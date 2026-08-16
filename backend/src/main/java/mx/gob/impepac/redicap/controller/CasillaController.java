package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.response.CasillaResponse;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/casillas")
@RequiredArgsConstructor
@Tag(name = "Casillas", description = "Catálogo de casillas electorales (DFR R6)")
public class CasillaController {

    private final CasillaRepository casillaRepo;

    @Operation(summary = "Listar casillas")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<CasillaResponse>> listar() {
        return ResponseEntity.ok(casillaRepo.findAll().stream().map(CasillaResponse::from).toList());
    }
}
