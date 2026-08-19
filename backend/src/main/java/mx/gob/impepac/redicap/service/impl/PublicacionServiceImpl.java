package mx.gob.impepac.redicap.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.CapturaActa;
import mx.gob.impepac.redicap.domain.entity.CortePublicacion;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CapturaActaRepository;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.CortePublicacionRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.service.PublicacionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Genera snapshots de resultados cada 10 minutos (DFR R4). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PublicacionServiceImpl implements PublicacionService {

    private static final List<EstadoActa> ESTADOS_FINALIZADOS =
            List.of(EstadoActa.VALIDADA, EstadoActa.VALIDADA_VERIFICADOR, EstadoActa.PUBLICADA);
    private static final String PREFIJO_CACHE_ULTIMO_CORTE = "corte:ultimo:";

    private final ActaRepository actaRepo;
    private final CasillaRepository casillaRepo;
    private final CapturaActaRepository capturaRepo;
    private final LogAuditoriaRepository logRepo;
    private final CortePublicacionRepository corteRepo;
    private final CorteFallidoRecorder corteFallidoRecorder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<CortePublicacion> generarCorte() {
        List<CortePublicacion> cortes = new ArrayList<>();
        for (TipoEleccion tipoEleccion : TipoEleccion.values()) {
            try {
                cortes.add(generarCorteInterno(tipoEleccion));
            } catch (Exception e) {
                log.error("Error generando corte de publicación para {}", tipoEleccion, e);
                cortes.add(corteFallidoRecorder.registrarCorteFallido(tipoEleccion));
            }
        }
        return cortes;
    }

    private CortePublicacion generarCorteInterno(TipoEleccion tipoEleccion) {
        List<Acta> finalizadas = actaRepo.findByEstadoInAndTipoEleccion(ESTADOS_FINALIZADOS, tipoEleccion);

        Map<String, Integer> resultados = new TreeMap<>();
        int totalVotos = 0;
        int totalListaNominal = 0;
        for (Acta acta : finalizadas) {
            for (Map.Entry<String, Integer> voto : votosRepresentativos(acta).entrySet()) {
                resultados.merge(voto.getKey(), voto.getValue(), Integer::sum);
                totalVotos += voto.getValue();
            }
            totalListaNominal += acta.getCasilla().getListaNominal();
        }

        long totalActas = actaRepo.count();
        long sinCapturar = actaRepo.countByEstado(EstadoActa.RECIBIDA) + actaRepo.countByEstado(EstadoActa.EN_CAPTURA_1);
        // Las casillas ESPECIAL no participan en Ayuntamiento (DFR R5).
        long totalCasillas = tipoEleccion == TipoEleccion.AYUNTAMIENTO
                ? casillaRepo.countByTipoNot(mx.gob.impepac.redicap.domain.enums.TipoCasilla.ESPECIAL)
                : casillaRepo.count();
        Double participacion = totalListaNominal > 0
                ? Math.round(totalVotos * 10000.0 / totalListaNominal) / 100.0
                : null;

        List<Acta> nuevasPublicadas = finalizadas.stream()
                .filter(a -> a.getEstado() != EstadoActa.PUBLICADA)
                .toList();
        LocalDateTime ahora = LocalDateTime.now();
        nuevasPublicadas.forEach(a -> {
            a.setEstado(EstadoActa.PUBLICADA);
            a.setPublicadaAt(ahora);
        });
        actaRepo.saveAll(nuevasPublicadas);

        CortePublicacion corte = CortePublicacion.builder()
                .tipoEleccion(tipoEleccion)
                .totalActasCapturadas((int) (totalActas - sinCapturar))
                .totalActasValidadas(finalizadas.size())
                .totalCasillas((int) totalCasillas)
                .porcentajeParticipacion(participacion)
                .resultadosJson(serializar(resultados))
                .exitoso(true)
                .build();
        corteRepo.save(corte);
        cachearUltimoCorte(corte);

        log.info("Corte generado ({}): {} actas validadas de {} casillas ({} recién publicadas)",
                tipoEleccion, finalizadas.size(), totalCasillas, nuevasPublicadas.size());
        return corte;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CortePublicacion> obtenerUltimoCorte(TipoEleccion tipoEleccion) {
        try {
            String cacheado = redisTemplate.opsForValue().get(PREFIJO_CACHE_ULTIMO_CORTE + tipoEleccion);
            if (cacheado != null) {
                return Optional.of(objectMapper.readValue(cacheado, CortePublicacion.class));
            }
        } catch (Exception e) {
            log.warn("No se pudo leer el corte cacheado en Redis, se consulta la BD", e);
        }
        return corteRepo.findTopByTipoEleccionOrderByGeneradoAtDesc(tipoEleccion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CortePublicacion> obtenerHistorial(TipoEleccion tipoEleccion) {
        return corteRepo.findTop20ByTipoEleccionAndExitosoTrueOrderByGeneradoAtDesc(tipoEleccion);
    }

    /** El caché es solo una optimización de lectura; si Redis falla no debe tumbar el corte ya guardado en BD. */
    private void cachearUltimoCorte(CortePublicacion corte) {
        try {
            redisTemplate.opsForValue().set(
                    PREFIJO_CACHE_ULTIMO_CORTE + corte.getTipoEleccion(),
                    objectMapper.writeValueAsString(corte),
                    Duration.ofMinutes(15));
        } catch (Exception e) {
            log.warn("No se pudo cachear el corte en Redis", e);
        }
    }

    /**
     * Votos "oficiales" de un acta finalizada: si pasó por mesa de deliberación, la
     * captura que el verificador eligió (registrada en log_auditoria); si no, la
     * última captura guardada, que por diseño de la máquina de estados siempre
     * coincide con otra (única forma de llegar a VALIDADA sin verificador).
     */
    private Map<String, Integer> votosRepresentativos(Acta acta) {
        String entidad = "Acta#" + acta.getId();
        var veredicto = logRepo.findFirstByEntidadAfectadaAndTipoAccion(entidad, "ACTA_VALIDADA_VERIFICADOR");
        if (veredicto.isPresent()) {
            Map<String, Object> valor = deserializar(veredicto.get().getValorDespues(), new TypeReference<>() {});
            return deserializar((String) valor.get("votos"), new TypeReference<>() {});
        }

        List<CapturaActa> capturas = capturaRepo.findByActaIdOrderByNumeroCapturaAsc(acta.getId());
        CapturaActa ultima = capturas.get(capturas.size() - 1);
        return deserializar(ultima.getDatosVotosJson(), new TypeReference<>() {});
    }

    private <T> T deserializar(String json, TypeReference<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo JSON de resultados", e);
        }
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando resultados", e);
        }
    }
}
