package mx.gob.impepac.redicap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.response.CorteResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.service.PublicacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publicacion")
@RequiredArgsConstructor
@Tag(name = "Publicación", description = "Consulta de resultados publicados – DFR R4")
@PreAuthorize("hasRole('CONSULTOR_PUBLICO')")
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Último corte de resultados publicado")
    @GetMapping("/ultimo-corte")
    public ResponseEntity<CorteResponse> ultimoCorte() {
        var corte = publicacionService.obtenerUltimoCorte()
                .orElseThrow(() -> new RedicapException("Aún no se ha generado ningún corte", org.springframework.http.HttpStatus.NO_CONTENT));
        return ResponseEntity.ok(CorteResponse.from(corte, objectMapper));
    }

    @Operation(summary = "Historial de cortes publicados")
    @GetMapping("/historial")
    public ResponseEntity<List<CorteResponse>> historial() {
        List<CorteResponse> historial = publicacionService.obtenerHistorial().stream()
                .map(c -> CorteResponse.from(c, objectMapper))
                .toList();
        return ResponseEntity.ok(historial);
    }
}
