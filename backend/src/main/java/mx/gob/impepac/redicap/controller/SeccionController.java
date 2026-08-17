package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.CrearSeccionRequest;
import mx.gob.impepac.redicap.dto.response.SeccionResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.CatalogoGeograficoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
@Tag(name = "Secciones", description = "Catálogo geográfico-electoral (DFR R1/R6)")
public class SeccionController {

    private final CatalogoGeograficoService catalogoService;

    @Operation(summary = "Listar secciones, opcionalmente filtradas por municipio o distrito")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeccionResponse>> listar(
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) Long distritoId) {
        return ResponseEntity.ok(catalogoService.listarSecciones(municipioId, distritoId));
    }

    @Operation(summary = "Crear sección")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SeccionResponse> crear(@Valid @RequestBody CrearSeccionRequest request,
                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        SeccionResponse creada = catalogoService.crearSeccion(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @Operation(summary = "Eliminar sección (falla si tiene casillas asociadas)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        catalogoService.eliminarSeccion(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
