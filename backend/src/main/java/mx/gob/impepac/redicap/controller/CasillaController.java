package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.CrearCasillaRequest;
import mx.gob.impepac.redicap.dto.response.CasillaResponse;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.CatalogoGeograficoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/casillas")
@RequiredArgsConstructor
@Tag(name = "Casillas", description = "Catálogo de casillas electorales (DFR R1/R6)")
public class CasillaController {

    private final CasillaRepository casillaRepo;
    private final CatalogoGeograficoService catalogoService;

    @Operation(summary = "Listar casillas, opcionalmente filtradas por sección")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CasillaResponse>> listar(@RequestParam(required = false) Long seccionId) {
        List<CasillaResponse> casillas = (seccionId != null
                ? casillaRepo.findBySeccionId(seccionId)
                : casillaRepo.findAll())
                .stream().map(CasillaResponse::from).toList();
        return ResponseEntity.ok(casillas);
    }

    @Operation(summary = "Obtener una casilla por id (para resolver su jerarquía distrito/municipio/sección)")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CasillaResponse> obtener(@PathVariable Long id) {
        return casillaRepo.findById(id)
                .map(CasillaResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear casilla")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CasillaResponse> crear(@Valid @RequestBody CrearCasillaRequest request,
                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        CasillaResponse creada = catalogoService.crearCasilla(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
