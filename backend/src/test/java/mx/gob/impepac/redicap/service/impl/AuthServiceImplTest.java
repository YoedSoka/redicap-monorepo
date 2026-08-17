package mx.gob.impepac.redicap.service.impl;

import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;
import mx.gob.impepac.redicap.dto.request.LoginRequest;
import mx.gob.impepac.redicap.dto.response.TokenResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.security.jwt.JwtService;
import mx.gob.impepac.redicap.security.jwt.SesionUnicaService;
import mx.gob.impepac.redicap.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre login/logout, en particular el enlace con la Política de Sesión Única Activa
 * (DFR R8): un login exitoso debe registrar la nueva sesión (lo que invalida la previa
 * si existía — ver SesionUnicaServiceTest para esa lógica), y logout debe limpiarla.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String USERNAME = "capturista1";
    private static final Long USUARIO_ID = 5L;

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private LoginIntentoRecorder loginIntentoRecorder;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private SesionUnicaService sesionUnicaService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(usuarioRepo, logRepo, passwordEncoder, jwtService,
                loginIntentoRecorder, tokenBlacklistService, sesionUnicaService);
        ReflectionTestUtils.setField(service, "maxIntentosFallidos", 5);
        ReflectionTestUtils.setField(service, "bloqueoMinutos", 15L);
    }

    @Test
    void loginExitoso_registraLaNuevaSesion() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepo.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("token-nuevo");

        TokenResponse response = service.login(request("clave-correcta"), "127.0.0.1");

        assertThat(response.getToken()).isEqualTo("token-nuevo");
        verify(sesionUnicaService).registrarNuevaSesion(USUARIO_ID, "token-nuevo");
    }

    @Test
    void loginConCredencialesInvalidas_noRegistraSesion() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepo.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(request("clave-mala"), "127.0.0.1"))
                .isInstanceOf(RedicapException.class);

        verify(sesionUnicaService, never()).registrarNuevaSesion(any(), any());
    }

    @Test
    void loginUsuarioBloqueado_noRegistraSesion() {
        Usuario usuario = usuarioActivo();
        usuario.setBloqueadoHasta(java.time.LocalDateTime.now().plusMinutes(10));
        when(usuarioRepo.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login(request("cualquier-clave"), "127.0.0.1"))
                .isInstanceOf(RedicapException.class);

        verify(sesionUnicaService, never()).registrarNuevaSesion(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void logout_limpiaLaSesionActivaDelUsuario() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepo.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));

        service.logout(USERNAME, "127.0.0.1", "token-a-cerrar");

        verify(tokenBlacklistService).blacklist("token-a-cerrar");
        verify(sesionUnicaService).limpiarSesion(USUARIO_ID);
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .username(USERNAME)
                .passwordHash("hash")
                .nombreCompleto("Capturista de Prueba")
                .rol(RolUsuario.CAPTURISTA)
                .intentosFallidos(0)
                .activo(true)
                .build();
    }

    private LoginRequest request(String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(USERNAME);
        req.setPassword(password);
        return req;
    }
}
