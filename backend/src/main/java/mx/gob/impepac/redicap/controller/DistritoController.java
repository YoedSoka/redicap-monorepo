package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.CrearDistritoRequest;
import mx.gob.impepac.redicap.dto.response.DistritoResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.CatalogoGeograficoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distritos")
@RequiredArgsConstructor
@Tag(name = "Distritos", description = "Catálogo geográfico-electoral (DFR R1/R6)")
public class DistritoController {

    private final CatalogoGeograficoService catalogoService;

    @Operation(summary = "Listar distritos")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DistritoResponse>> listar() {
        return ResponseEntity.ok(catalogoService.listarDistritos());
    }

    @Operation(summary = "Crear distrito")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DistritoResponse> crear(@Valid @RequestBody CrearDistritoRequest request,
                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        DistritoResponse creado = catalogoService.crearDistrito(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Eliminar distrito (falla si tiene secciones asociadas)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        catalogoService.eliminarDistrito(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
