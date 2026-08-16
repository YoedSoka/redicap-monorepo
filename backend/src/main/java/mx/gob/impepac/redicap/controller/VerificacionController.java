package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.IlegibleRequest;
import mx.gob.impepac.redicap.dto.request.ValidarVerificacionRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.VerificacionDetalleResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.VerificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/verificaciones")
@RequiredArgsConstructor
@Tag(name = "Verificación", description = "Mesa de deliberación – DFR R3")
@PreAuthorize("hasRole('VERIFICADOR')")
public class VerificacionController {

    private final VerificacionService verificacionService;

    @Operation(summary = "Actas pendientes de deliberación")
    @GetMapping("/pendientes")
    public ResponseEntity<List<ActaResponse>> pendientes() {
        return ResponseEntity.ok(verificacionService.listarPendientes());
    }

    @Operation(summary = "Detalle de un acta y sus 3 capturas")
    @GetMapping("/{actaId}")
    public ResponseEntity<VerificacionDetalleResponse> detalle(@PathVariable Long actaId) {
        return ResponseEntity.ok(verificacionService.obtenerDetalle(actaId));
    }

    @Operation(summary = "La mesa determina que una de las 3 capturas coincide con el acta física")
    @PostMapping("/{actaId}/validar")
    public ResponseEntity<ActaResponse> validar(@PathVariable Long actaId,
                                                 @Valid @RequestBody ValidarVerificacionRequest request,
                                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(verificacionService.validar(actaId, principal.getId(), request.getNumeroCapturaElegida()));
    }

    @Operation(summary = "El acta física es ilegible o no permite determinar un resultado")
    @PostMapping("/{actaId}/ilegible")
    public ResponseEntity<ActaResponse> ilegible(@PathVariable Long actaId,
                                                  @Valid @RequestBody IlegibleRequest request,
                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(verificacionService.marcarIlegible(actaId, principal.getId(), request.getMotivo()));
    }
}
