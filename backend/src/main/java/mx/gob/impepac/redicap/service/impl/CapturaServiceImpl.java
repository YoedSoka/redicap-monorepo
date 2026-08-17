package mx.gob.impepac.redicap.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.*;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.dto.request.CapturaRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.ImagenActaResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.*;
import mx.gob.impepac.redicap.service.CapturaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Implementa el flujo de doble ciego con hasta 3 capturas (DFR R2).
 *
 * Reglas de estado:
 *   C1 guardada            → EN_CAPTURA_2
 *   C2 == C1               → VALIDADA
 *   C2 != C1               → EN_CAPTURA_3
 *   C3 == C1 o C3 == C2    → VALIDADA
 *   C3 != C1 y C3 != C2    → MESA_DELIBERACION
 */
@Service @RequiredArgsConstructor @Slf4j
@Transactional
public class CapturaServiceImpl implements CapturaService {

    private final ActaRepository actaRepo;
    private final CapturaActaRepository capturaRepo;
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;
    private final ObjectMapper objectMapper;

    @Value("${redicap.storage.actas-path}")
    private String actasPath;

    private static final Map<String, String> CONTENT_TYPE_POR_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    @Override
    @Transactional(readOnly = true)
    public ActaResponse obtenerSiguienteActa(Long usuarioId) {
        // El estado depende de si ya hay C1: busca EN_CAPTURA_1 o EN_CAPTURA_2
        Acta acta = actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_1.name(), usuarioId)
                .or(() -> actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_2.name(), usuarioId))
                .or(() -> actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_3.name(), usuarioId))
                .orElseThrow(() -> new RedicapException("No hay actas disponibles", org.springframework.http.HttpStatus.NO_CONTENT));
        return ActaResponse.from(acta);
    }

    @Override
    public ActaResponse registrarCaptura(Long actaId, Long usuarioId, CapturaRequest req) {
        Acta acta = actaRepo.findById(actaId)
                .orElseThrow(() -> RedicapException.notFound("Acta", actaId));
        Usuario capturista = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> RedicapException.notFound("Usuario", usuarioId));

        // Regla: un usuario NO puede capturar la misma acta dos veces (DFR R2)
        if (capturaRepo.existsByActaIdAndCapturistaId(actaId, usuarioId)) {
            throw RedicapException.forbidden("Ya realizaste una captura de esta acta");
        }

        int numeroCapturas = capturaRepo.countByActaId(actaId);
        int nuevaCaptura   = numeroCapturas + 1;

        if (nuevaCaptura > 3) {
            throw RedicapException.conflict("Esta acta ya tiene 3 capturas registradas");
        }

        // Guardar la captura
        String datosJson = serializarVotos(req.getVotos());
        CapturaActa captura = CapturaActa.builder()
                .acta(acta)
                .numeroCaptura(nuevaCaptura)
                .capturista(capturista)
                .datosVotosJson(datosJson)
                .totalVotosActa(req.getTotalVotosActa())
                .totalVotosCalculado(req.getVotos().values().stream().mapToInt(Integer::intValue).sum())
                .build();
        capturaRepo.save(captura);

        // Actualizar flags de integridad (DFR R7)
        validarIntegridad(acta, captura, req);

        // Avanzar la máquina de estados
        avanzarEstado(acta, nuevaCaptura);
        actaRepo.save(acta);

        log.info("Captura {} registrada para acta {} por usuario {}", nuevaCaptura, actaId, usuarioId);
        return ActaResponse.from(acta);
    }

    // ── Máquina de estados ──────────────────────────────────────────────────
    private void avanzarEstado(Acta acta, int capturasTotal) {
        List<CapturaActa> capturas = capturaRepo.findByActaIdOrderByNumeroCapturaAsc(acta.getId());

        switch (capturasTotal) {
            case 1 -> acta.setEstado(EstadoActa.EN_CAPTURA_2);
            case 2 -> {
                if (capturasCoinciden(capturas.get(0), capturas.get(1))) {
                    acta.setEstado(EstadoActa.VALIDADA);
                } else {
                    acta.setEstado(EstadoActa.EN_CAPTURA_3);
                }
            }
            case 3 -> {
                CapturaActa c1 = capturas.get(0);
                CapturaActa c2 = capturas.get(1);
                CapturaActa c3 = capturas.get(2);
                if (capturasCoinciden(c3, c1) || capturasCoinciden(c3, c2)) {
                    acta.setEstado(EstadoActa.VALIDADA);
                } else {
                    acta.setEstado(EstadoActa.MESA_DELIBERACION);
                }
            }
        }
    }

    private boolean capturasCoinciden(CapturaActa a, CapturaActa b) {
        return a.getDatosVotosJson().equals(b.getDatosVotosJson());
    }

    // ── Validación de integridad (DFR R7) ───────────────────────────────────
    private void validarIntegridad(Acta acta, CapturaActa captura, CapturaRequest req) {
        int totalCalculado = captura.getTotalVotosCalculado();

        // Error aritmético: total del acta ≠ suma calculada
        if (req.getTotalVotosActa() != null && !req.getTotalVotosActa().equals(totalCalculado)) {
            acta.setErrorAritmetico(true);
            log.warn("Error aritmético en acta {}: acta={} calculado={}",
                     acta.getId(), req.getTotalVotosActa(), totalCalculado);
        }
        // Excedente de lista nominal
        if (totalCalculado > acta.getCasilla().getListaNominal()) {
            acta.setExcedeListaNominal(true);
            log.warn("Excedente lista nominal en acta {}: votos={} lista={}",
                     acta.getId(), totalCalculado, acta.getCasilla().getListaNominal());
        }
    }

    private String serializarVotos(Map<String, Integer> votos) {
        try {
            return objectMapper.writeValueAsString(votos);
        } catch (Exception e) {
            throw new RedicapException("Error serializando votos", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── Imagen de referencia ─────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ImagenActaResponse obtenerImagen(Long actaId) {
        Acta acta = actaRepo.findById(actaId)
                .orElseThrow(() -> RedicapException.notFound("Acta", actaId));
        if (acta.getRutaImagen() == null) {
            throw RedicapException.notFound("Imagen de acta", actaId);
        }
        try {
            byte[] contenido = Files.readAllBytes(Path.of(actasPath, acta.getRutaImagen()));
            String extension = acta.getRutaImagen()
                    .substring(acta.getRutaImagen().lastIndexOf('.') + 1)
                    .toLowerCase();
            String contentType = CONTENT_TYPE_POR_EXTENSION.getOrDefault(extension, "application/octet-stream");
            return new ImagenActaResponse(contenido, contentType);
        } catch (IOException e) {
            throw new RedicapException("No se pudo leer la imagen del acta", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
