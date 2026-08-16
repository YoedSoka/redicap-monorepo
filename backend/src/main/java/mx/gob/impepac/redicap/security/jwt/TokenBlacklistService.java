package mx.gob.impepac.redicap.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Lista negra de JWT invalidados por logout (DFR R8). Cada entrada expira sola en Redis
 * al mismo tiempo que el propio token, así que no requiere limpieza manual.
 * Si Redis no está disponible, falla abierto (no bloquea login/requests) y solo loguea
 * advertencia — la firma/expiración del JWT sigue siendo la validación primaria.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String PREFIJO = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklist(String token) {
        try {
            long ttlMs = jwtService.remainingTtlMs(token);
            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(PREFIJO + hash(token), "1", Duration.ofMillis(ttlMs));
            }
        } catch (Exception e) {
            log.warn("No se pudo invalidar el token en Redis (logout seguirá funcionando del lado del cliente)", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIJO + hash(token)));
        } catch (Exception e) {
            log.warn("Redis no disponible para verificar blacklist; se permite la request", e);
            return false;
        }
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
