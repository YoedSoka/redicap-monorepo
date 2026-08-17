package mx.gob.impepac.redicap.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Política de Sesión Única Activa (DFR R8): un mismo usuario no puede mantener dos sesiones
 * válidas a la vez. Guarda en Redis el último token emitido por usuario; si llega un login
 * nuevo mientras había uno vigente, el anterior se manda a la blacklist de inmediato — la
 * próxima request de esa sesión vieja la rechaza {@link JwtAuthenticationFilter}, sin tocarlo.
 * Igual que TokenBlacklistService, si Redis no está disponible falla abierto (solo advierte)
 * para no tumbar el login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SesionUnicaService {

    private static final String PREFIJO = "sesion:activa:";

    private final StringRedisTemplate redisTemplate;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;

    /** Invalida la sesión previa del usuario (si había una) y registra el nuevo token como la activa. */
    public void registrarNuevaSesion(Long usuarioId, String nuevoToken) {
        try {
            String key = PREFIJO + usuarioId;
            String tokenAnterior = redisTemplate.opsForValue().get(key);
            if (tokenAnterior != null && !tokenAnterior.equals(nuevoToken)) {
                tokenBlacklistService.blacklist(tokenAnterior);
                log.info("Sesión previa del usuario {} invalidada por nuevo inicio de sesión", usuarioId);
            }
            long ttlMs = jwtService.remainingTtlMs(nuevoToken);
            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(key, nuevoToken, Duration.ofMillis(ttlMs));
            }
        } catch (Exception e) {
            log.warn("No se pudo aplicar la política de sesión única (Redis no disponible); el login continúa", e);
        }
    }

    /** Limpia el registro de sesión activa del usuario (logout explícito). */
    public void limpiarSesion(Long usuarioId) {
        try {
            redisTemplate.delete(PREFIJO + usuarioId);
        } catch (Exception e) {
            log.warn("No se pudo limpiar el registro de sesión activa", e);
        }
    }
}
