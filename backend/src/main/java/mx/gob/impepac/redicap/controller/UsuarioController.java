package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.ActualizarUsuarioRequest;
import mx.gob.impepac.redicap.dto.request.CrearUsuarioRequest;
import mx.gob.impepac.redicap.dto.response.UsuarioResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios por el administrador – DFR R8")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Perfil del usuario autenticado")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponse> obtenerPropio(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.obtenerPropio(principal.getId()));
    }

    @Operation(summary = "Listar usuarios")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @Operation(summary = "Crear usuario")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request,
                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        UsuarioResponse creado = usuarioService.crear(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Actualizar datos de un usuario")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ActualizarUsuarioRequest request,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request, principal.getId()));
    }

    @Operation(summary = "Desbloquear usuario (resetea intentos fallidos)")
    @PostMapping("/{id}/desbloquear")
    public ResponseEntity<UsuarioResponse> desbloquear(@PathVariable Long id,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.desbloquear(id, principal.getId()));
    }

    @Operation(summary = "Activar usuario")
    @PostMapping("/{id}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id,
                                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.cambiarActivo(id, true, principal.getId()));
    }

    @Operation(summary = "Desactivar usuario")
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.cambiarActivo(id, false, principal.getId()));
    }
}
