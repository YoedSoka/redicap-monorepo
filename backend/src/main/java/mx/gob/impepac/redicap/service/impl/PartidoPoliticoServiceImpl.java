package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.PartidoPolitico;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.request.ActualizarPartidoRequest;
import mx.gob.impepac.redicap.dto.request.CrearPartidoRequest;
import mx.gob.impepac.redicap.dto.response.PartidoPoliticoResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.PartidoPoliticoRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.service.PartidoPoliticoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartidoPoliticoServiceImpl implements PartidoPoliticoService {

    private static final String MODULO = "ADMINISTRACION";

    private final PartidoPoliticoRepository partidoRepo;
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;

    @Override
    @Transactional(readOnly = true)
    public List<PartidoPoliticoResponse> listar() {
        return partidoRepo.findAllByOrderBySiglasAsc().stream().map(PartidoPoliticoResponse::from).toList();
    }

    @Override
    public PartidoPoliticoResponse crear(CrearPartidoRequest request, Long adminId) {
        String siglas = request.getSiglas().trim().toUpperCase();
        if (partidoRepo.existsBySiglas(siglas)) {
            throw RedicapException.conflict("Ya existe un partido con esas siglas");
        }

        PartidoPolitico partido = PartidoPolitico.builder()
                .siglas(siglas)
                .nombre(request.getNombre())
                .colorHex(request.getColorHex())
                .activo(true)
                .build();
        partidoRepo.save(partido);

        registrarAuditoria(adminId, "PARTIDO_CREADO", partido.getId());
        log.info("Partido {} creado por admin {}", siglas, adminId);
        return PartidoPoliticoResponse.from(partido);
    }

    @Override
    public PartidoPoliticoResponse actualizar(Long id, ActualizarPartidoRequest request, Long adminId) {
        PartidoPolitico partido = obtener(id);
        partido.setNombre(request.getNombre());
        partido.setColorHex(request.getColorHex());
        partidoRepo.save(partido);

        registrarAuditoria(adminId, "PARTIDO_ACTUALIZADO", partido.getId());
        return PartidoPoliticoResponse.from(partido);
    }

    @Override
    public PartidoPoliticoResponse cambiarActivo(Long id, boolean activo, Long adminId) {
        PartidoPolitico partido = obtener(id);
        partido.setActivo(activo);
        partidoRepo.save(partido);

        registrarAuditoria(adminId, activo ? "PARTIDO_ACTIVADO" : "PARTIDO_DESACTIVADO", partido.getId());
        return PartidoPoliticoResponse.from(partido);
    }

    private PartidoPolitico obtener(Long id) {
        return partidoRepo.findById(id)
                .orElseThrow(() -> RedicapException.notFound("Partido", id));
    }

    private void registrarAuditoria(Long adminId, String tipoAccion, Long partidoId) {
        Usuario admin = usuarioRepo.findById(adminId).orElse(null);
        logRepo.save(LogAuditoria.builder()
                .usuario(admin)
                .tipoAccion(tipoAccion)
                .modulo(MODULO)
                .entidadAfectada("Partido#" + partidoId)
                .build());
    }
}
