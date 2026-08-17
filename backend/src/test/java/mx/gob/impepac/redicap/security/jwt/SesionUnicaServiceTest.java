package mx.gob.impepac.redicap.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre la Política de Sesión Única Activa (DFR R8): un login nuevo debe invalidar
 * inmediatamente cualquier sesión previa del mismo usuario.
 */
@ExtendWith(MockitoExtension.class)
class SesionUnicaServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final String CLAVE = "sesion:activa:1";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private JwtService jwtService;

    private SesionUnicaService service;

    @BeforeEach
    void setUp() {
        service = new SesionUnicaService(redisTemplate, tokenBlacklistService, jwtService);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void sinSesionPrevia_soloRegistraLaNuevaSinInvalidarNada() {
        when(valueOps.get(CLAVE)).thenReturn(null);
        when(jwtService.remainingTtlMs("nuevo-token")).thenReturn(60_000L);

        service.registrarNuevaSesion(USUARIO_ID, "nuevo-token");

        verify(tokenBlacklistService, never()).blacklist(any());
        verify(valueOps).set(CLAVE, "nuevo-token", Duration.ofMillis(60_000L));
    }

    @Test
    void conSesionPreviaDistinta_invalidaElTokenAnterior() {
        when(valueOps.get(CLAVE)).thenReturn("token-viejo");
        when(jwtService.remainingTtlMs("token-nuevo")).thenReturn(60_000L);

        service.registrarNuevaSesion(USUARIO_ID, "token-nuevo");

        verify(tokenBlacklistService).blacklist("token-viejo");
        verify(valueOps).set(CLAVE, "token-nuevo", Duration.ofMillis(60_000L));
    }

    @Test
    void mismoTokenQueLaSesionActual_noSeAutoinvalida() {
        when(valueOps.get(CLAVE)).thenReturn("mismo-token");
        when(jwtService.remainingTtlMs("mismo-token")).thenReturn(60_000L);

        service.registrarNuevaSesion(USUARIO_ID, "mismo-token");

        verify(tokenBlacklistService, never()).blacklist(any());
    }

    @Test
    void redisFallaAlRegistrar_noPropagaExcepcion() {
        when(valueOps.get(CLAVE)).thenThrow(new RuntimeException("Redis no disponible"));

        assertThatCode(() -> service.registrarNuevaSesion(USUARIO_ID, "token"))
                .doesNotThrowAnyException();
    }

    @Test
    void limpiarSesion_borraElRegistroDelUsuario() {
        service.limpiarSesion(USUARIO_ID);

        verify(redisTemplate).delete(CLAVE);
    }

    @Test
    void redisFallaAlLimpiar_noPropagaExcepcion() {
        when(redisTemplate.delete(CLAVE)).thenThrow(new RuntimeException("Redis no disponible"));

        assertThatCode(() -> service.limpiarSesion(USUARIO_ID)).doesNotThrowAnyException();
    }
}
