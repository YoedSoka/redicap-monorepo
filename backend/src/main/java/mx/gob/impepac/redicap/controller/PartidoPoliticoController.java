package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.ActualizarPartidoRequest;
import mx.gob.impepac.redicap.dto.request.CrearPartidoRequest;
import mx.gob.impepac.redicap.dto.response.PartidoPoliticoResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.PartidoPoliticoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partidos")
@RequiredArgsConstructor
@Tag(name = "Partidos", description = "Catálogo de partidos políticos (DFR R6)")
public class PartidoPoliticoController {

    private final PartidoPoliticoService partidoService;

    @Operation(summary = "Listar partidos (incluye inactivos; el cliente filtra según el uso)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PartidoPoliticoResponse>> listar() {
        return ResponseEntity.ok(partidoService.listar());
    }

    @Operation(summary = "Crear partido")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PartidoPoliticoResponse> crear(@Valid @RequestBody CrearPartidoRequest request,
                                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        PartidoPoliticoResponse creado = partidoService.crear(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Actualizar nombre/color de un partido")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PartidoPoliticoResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody ActualizarPartidoRequest request,
                                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(partidoService.actualizar(id, request, principal.getId()));
    }

    @Operation(summary = "Activar partido")
    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PartidoPoliticoResponse> activar(@PathVariable Long id,
                                                            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(partidoService.cambiarActivo(id, true, principal.getId()));
    }

    @Operation(summary = "Desactivar partido")
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PartidoPoliticoResponse> desactivar(@PathVariable Long id,
                                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(partidoService.cambiarActivo(id, false, principal.getId()));
    }
}
