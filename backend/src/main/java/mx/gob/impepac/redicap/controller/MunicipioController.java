package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.CrearMunicipioRequest;
import mx.gob.impepac.redicap.dto.response.MunicipioResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.CatalogoGeograficoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/municipios")
@RequiredArgsConstructor
@Tag(name = "Municipios", description = "Catálogo geográfico-electoral (DFR R1/R6)")
public class MunicipioController {

    private final CatalogoGeograficoService catalogoService;

    @Operation(summary = "Listar municipios")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MunicipioResponse>> listar() {
        return ResponseEntity.ok(catalogoService.listarMunicipios());
    }

    @Operation(summary = "Crear municipio")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MunicipioResponse> crear(@Valid @RequestBody CrearMunicipioRequest request,
                                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        MunicipioResponse creado = catalogoService.crearMunicipio(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Eliminar municipio (falla si tiene secciones asociadas)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        catalogoService.eliminarMunicipio(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
