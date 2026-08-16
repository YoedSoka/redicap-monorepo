package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.request.ActualizarUsuarioRequest;
import mx.gob.impepac.redicap.dto.request.CrearUsuarioRequest;
import mx.gob.impepac.redicap.dto.response.UsuarioResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private static final String MODULO = "ADMINISTRACION";

    private final UsuarioRepository usuarioRepo;
    private final CasillaRepository casillaRepo;
    private final LogAuditoriaRepository logRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepo.findAll().stream().map(UsuarioResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPropio(Long usuarioId) {
        return UsuarioResponse.from(obtener(usuarioId));
    }

    @Override
    public UsuarioResponse crear(CrearUsuarioRequest request, Long adminId) {
        if (usuarioRepo.existsByUsername(request.getUsername())) {
            throw RedicapException.conflict("Ya existe un usuario con ese nombre de usuario");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombreCompleto(request.getNombreCompleto())
                .curp(request.getCurp())
                .rol(request.getRol())
                .casillaAsignada(resolverCasilla(request.getCasillaAsignadaId()))
                .intentosFallidos(0)
                .activo(true)
                .build();
        usuarioRepo.save(usuario);

        registrarAuditoria(adminId, "USUARIO_CREADO", usuario.getId());
        log.info("Usuario {} creado por admin {}", usuario.getUsername(), adminId);
        return UsuarioResponse.from(usuario);
    }

    @Override
    public UsuarioResponse actualizar(Long usuarioId, ActualizarUsuarioRequest request, Long adminId) {
        Usuario usuario = obtener(usuarioId);
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setCurp(request.getCurp());
        usuario.setRol(request.getRol());
        usuario.setCasillaAsignada(resolverCasilla(request.getCasillaAsignadaId()));
        usuarioRepo.save(usuario);

        registrarAuditoria(adminId, "USUARIO_ACTUALIZADO", usuario.getId());
        return UsuarioResponse.from(usuario);
    }

    @Override
    public UsuarioResponse desbloquear(Long usuarioId, Long adminId) {
        Usuario usuario = obtener(usuarioId);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepo.save(usuario);

        registrarAuditoria(adminId, "USUARIO_DESBLOQUEADO", usuario.getId());
        log.info("Usuario {} desbloqueado por admin {}", usuario.getUsername(), adminId);
        return UsuarioResponse.from(usuario);
    }

    @Override
    public UsuarioResponse cambiarActivo(Long usuarioId, boolean activo, Long adminId) {
        Usuario usuario = obtener(usuarioId);
        usuario.setActivo(activo);
        usuarioRepo.save(usuario);

        registrarAuditoria(adminId, activo ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", usuario.getId());
        return UsuarioResponse.from(usuario);
    }

    private Usuario obtener(Long usuarioId) {
        return usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> RedicapException.notFound("Usuario", usuarioId));
    }

    private Casilla resolverCasilla(Long casillaId) {
        if (casillaId == null) {
            return null;
        }
        return casillaRepo.findById(casillaId)
                .orElseThrow(() -> RedicapException.notFound("Casilla", casillaId));
    }

    private void registrarAuditoria(Long adminId, String tipoAccion, Long usuarioAfectadoId) {
        Usuario admin = usuarioRepo.findById(adminId).orElse(null);
        logRepo.save(LogAuditoria.builder()
                .usuario(admin)
                .tipoAccion(tipoAccion)
                .modulo(MODULO)
                .entidadAfectada("Usuario#" + usuarioAfectadoId)
                .build());
    }
}
