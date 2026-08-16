package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Registra intentos de login fallidos en su propia transacción (REQUIRES_NEW).
 * AuthServiceImpl.login() lanza RedicapException en el camino de credenciales inválidas,
 * y con @Transactional normal eso hace rollback de todo el método, incluyendo el contador
 * de intentos y el bloqueo — por eso el registro necesita confirmarse independientemente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class LoginIntentoRecorder {

    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(Long usuarioId, String ipOrigen, int maxIntentosFallidos, long bloqueoMinutos) {
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();

        boolean bloqueoExpirado = usuario.getBloqueadoHasta() != null
                && !usuario.getBloqueadoHasta().isAfter(LocalDateTime.now());
        int base = bloqueoExpirado ? 0 : usuario.getIntentosFallidos();
        int intentos = base + 1;

        usuario.setIntentosFallidos(intentos);
        if (intentos >= maxIntentosFallidos) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(bloqueoMinutos));
            usuario.setIntentosFallidos(0);
            log.warn("Usuario {} bloqueado {} minutos por {} intentos fallidos",
                    usuario.getUsername(), bloqueoMinutos, maxIntentosFallidos);
        } else if (bloqueoExpirado) {
            usuario.setBloqueadoHasta(null);
        }

        usuarioRepo.save(usuario);
        logRepo.save(LogAuditoria.builder()
                .usuario(usuario)
                .ipOrigen(ipOrigen)
                .tipoAccion("LOGIN_FALLIDO")
                .modulo("AUTH")
                .entidadAfectada("Usuario#" + usuario.getId())
                .build());
    }
}
