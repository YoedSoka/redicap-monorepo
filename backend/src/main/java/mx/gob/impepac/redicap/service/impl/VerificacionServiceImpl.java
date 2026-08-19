package mx.gob.impepac.redicap.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.CapturaActa;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.MotivoDictamenVerificador;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.CapturaResumenResponse;
import mx.gob.impepac.redicap.dto.response.VerificacionDetalleResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CapturaActaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.service.VerificacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificacionServiceImpl implements VerificacionService {

    private final ActaRepository actaRepo;
    private final CapturaActaRepository capturaRepo;
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ActaResponse> listarPendientes() {
        return actaRepo.findByEstado(EstadoActa.MESA_DELIBERACION).stream()
                .map(ActaResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionDetalleResponse obtenerDetalle(Long actaId) {
        Acta acta = actaRepo.findById(actaId)
                .orElseThrow(() -> RedicapException.notFound("Acta", actaId));

        List<CapturaResumenResponse> capturas = capturaRepo.findByActaIdOrderByNumeroCapturaAsc(actaId).stream()
                .map(this::toResumen)
                .toList();

        return VerificacionDetalleResponse.builder()
                .acta(ActaResponse.from(acta))
                .capturas(capturas)
                .build();
    }

    @Override
    public ActaResponse validar(Long actaId, Long verificadorId, Integer numeroCapturaElegida,
                                 MotivoDictamenVerificador motivoCatalogo, String justificacion) {
        Acta acta = obtenerActaEnDeliberacion(actaId);
        Usuario verificador = usuarioRepo.findById(verificadorId)
                .orElseThrow(() -> RedicapException.notFound("Usuario", verificadorId));

        CapturaActa elegida = capturaRepo.findByActaIdOrderByNumeroCapturaAsc(actaId).stream()
                .filter(c -> c.getNumeroCaptura().equals(numeroCapturaElegida))
                .findFirst()
                .orElseThrow(() -> RedicapException.badRequest("No existe la captura número " + numeroCapturaElegida));

        acta.setEstado(EstadoActa.VALIDADA_VERIFICADOR);
        actaRepo.save(acta);

        registrarAuditoria(verificador, "ACTA_VALIDADA_VERIFICADOR", acta.getId(), Map.of(
                "numeroCapturaElegida", numeroCapturaElegida,
                "votos", elegida.getDatosVotosJson(),
                "motivoCatalogo", motivoCatalogo.name(),
                "justificacion", justificacion));

        log.info("Acta {} validada por verificador {} eligiendo captura {} (motivo: {})",
                actaId, verificadorId, numeroCapturaElegida, motivoCatalogo);
        return ActaResponse.from(acta);
    }

    @Override
    public ActaResponse marcarIlegible(Long actaId, Long verificadorId,
                                        MotivoDictamenVerificador motivoCatalogo, String justificacion) {
        Acta acta = obtenerActaEnDeliberacion(actaId);
        Usuario verificador = usuarioRepo.findById(verificadorId)
                .orElseThrow(() -> RedicapException.notFound("Usuario", verificadorId));

        acta.setEstado(EstadoActa.ILEGIBLE);
        actaRepo.save(acta);

        registrarAuditoria(verificador, "ACTA_ILEGIBLE", acta.getId(), Map.of(
                "motivoCatalogo", motivoCatalogo.name(),
                "justificacion", justificacion));

        log.info("Acta {} marcada ILEGIBLE por verificador {} (motivo: {})", actaId, verificadorId, motivoCatalogo);
        return ActaResponse.from(acta);
    }

    private Acta obtenerActaEnDeliberacion(Long actaId) {
        Acta acta = actaRepo.findById(actaId)
                .orElseThrow(() -> RedicapException.notFound("Acta", actaId));
        if (acta.getEstado() != EstadoActa.MESA_DELIBERACION) {
            throw RedicapException.conflict("El acta no está en mesa de deliberación (estado actual: " + acta.getEstado() + ")");
        }
        return acta;
    }

    private CapturaResumenResponse toResumen(CapturaActa c) {
        return CapturaResumenResponse.builder()
                .numeroCaptura(c.getNumeroCaptura())
                .capturistaId(c.getCapturista().getId())
                .votos(deserializarVotos(c.getDatosVotosJson()))
                .totalVotosActa(c.getTotalVotosActa())
                .totalVotosCalculado(c.getTotalVotosCalculado())
                .build();
    }

    private Map<String, Integer> deserializarVotos(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RedicapException("Error leyendo votos de captura", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void registrarAuditoria(Usuario verificador, String tipoAccion, Long actaId, Map<String, Object> valorDespues) {
        try {
            logRepo.save(LogAuditoria.builder()
                    .usuario(verificador)
                    .tipoAccion(tipoAccion)
                    .modulo("VERIFICACION")
                    .entidadAfectada("Acta#" + actaId)
                    .valorDespues(objectMapper.writeValueAsString(valorDespues))
                    .build());
        } catch (Exception e) {
            throw new RedicapException("Error registrando auditoría", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
