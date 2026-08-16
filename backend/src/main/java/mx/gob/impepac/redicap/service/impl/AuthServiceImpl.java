package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.request.LoginRequest;
import mx.gob.impepac.redicap.dto.response.TokenResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.security.jwt.JwtService;
import mx.gob.impepac.redicap.security.jwt.TokenBlacklistService;
import mx.gob.impepac.redicap.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Login/logout con bloqueo por intentos fallidos (DFR R8) y bitácora de auditoría (DFR R9). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String MODULO = "AUTH";

    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginIntentoRecorder loginIntentoRecorder;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${redicap.security.max-intentos-fallidos}")
    private int maxIntentosFallidos;

    @Value("${redicap.security.bloqueo-minutos}")
    private long bloqueoMinutos;

    @Override
    public TokenResponse login(LoginRequest request, String ipOrigen) {
        Usuario usuario = usuarioRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> RedicapException.unauthorized("Usuario o contraseña incorrectos"));

        if (!usuario.getActivo()) {
            throw RedicapException.forbidden("Usuario inactivo");
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw RedicapException.locked("Usuario bloqueado hasta " + usuario.getBloqueadoHasta());
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            // Transacción propia: si no, el rollback de esta excepción borraría el registro del intento.
            loginIntentoRecorder.registrarIntentoFallido(usuario.getId(), ipOrigen, maxIntentosFallidos, bloqueoMinutos);
            throw RedicapException.unauthorized("Usuario o contraseña incorrectos");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepo.save(usuario);
        registrarAuditoria(usuario, ipOrigen, "LOGIN_EXITOSO");

        return TokenResponse.builder()
                .token(jwtService.generateToken(usuario))
                .username(usuario.getUsername())
                .rol(usuario.getRol().name())
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }

    @Override
    public void logout(String username, String ipOrigen, String token) {
        tokenBlacklistService.blacklist(token);
        usuarioRepo.findByUsername(username)
                .ifPresent(usuario -> registrarAuditoria(usuario, ipOrigen, "LOGOUT"));
    }

    private void registrarAuditoria(Usuario usuario, String ipOrigen, String tipoAccion) {
        logRepo.save(LogAuditoria.builder()
                .usuario(usuario)
                .ipOrigen(ipOrigen)
                .tipoAccion(tipoAccion)
                .modulo(MODULO)
                .entidadAfectada("Usuario#" + usuario.getId())
                .build());
    }
}
