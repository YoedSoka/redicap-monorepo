package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.request.CapturaRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.CapturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/capturas")
@RequiredArgsConstructor
@Tag(name = "Captura", description = "Flujo de doble ciego – DFR R2")
public class CapturaController {

    private final CapturaService capturaService;

    @Operation(summary = "Obtener siguiente acta disponible")
    @GetMapping("/siguiente")
    @PreAuthorize("hasRole('CAPTURISTA')")
    public ResponseEntity<ActaResponse> obtenerSiguiente(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(capturaService.obtenerSiguienteActa(principal.getId()));
    }

    @Operation(summary = "Registrar captura de votos")
    @PostMapping("/{actaId}")
    @PreAuthorize("hasRole('CAPTURISTA')")
    public ResponseEntity<ActaResponse> registrar(
            @PathVariable Long actaId,
            @Valid @RequestBody CapturaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(capturaService.registrarCaptura(actaId, principal.getId(), request));
    }
}
