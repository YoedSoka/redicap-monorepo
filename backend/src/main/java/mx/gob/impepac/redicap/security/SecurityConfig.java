package mx.gob.impepac.redicap.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/** RBAC vía JWT (DFR R8): sin sesión de servidor, un rol por usuario. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/actuator/health",
            "/error"
    };

    /**
     * Swagger y el resto de actuator (env, metrics, etc.) exponen el catálogo completo de
     * endpoints y datos internos del sistema. Solo se abren cuando se activa explícitamente
     * en un ambiente de desarrollo controlado; nunca por omisión (DFR R11) — importa sobre
     * todo en cuanto el backend deja de estar solo en la LAN (ej. detrás de un túnel).
     */
    private static final String[] RUTAS_DOCUMENTACION = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${redicap.security.exponer-documentacion:false}")
    private boolean exponerDocumentacion;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers(RUTAS_PUBLICAS).permitAll();
                        if (exponerDocumentacion) {
                            auth.requestMatchers(RUTAS_DOCUMENTACION).permitAll();
                        }
                        auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::onAuthError)
                        .accessDeniedHandler(this::onAccessDenied))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private void onAuthError(HttpServletRequest request, HttpServletResponse response,
                              AuthenticationException ex) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado");
    }

    private void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                 AccessDeniedException ex) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"message\":\"%s\"}".formatted(status.value(), message));
    }
}
