package mx.gob.impepac.redicap.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/** Generación y validación de JWT. */
@Service @Slf4j
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${redicap.jwt.secret}") String secret,
            @Value("${redicap.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                // jti: sin esto, dos logins del mismo usuario en el mismo segundo generan el
                // mismo token (iat/exp truncan a segundos), y SesionUnicaService no podría
                // distinguir la sesión nueva de la vieja para invalidarla (DFR R8).
                .id(UUID.randomUUID().toString())
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("userId", usuario.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /** Milisegundos restantes hasta la expiración del token (puede ser negativo si ya expiró). */
    public long remainingTtlMs(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
