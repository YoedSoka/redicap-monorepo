package mx.gob.impepac.redicap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.security.userdetails.UsuarioPrincipal;
import mx.gob.impepac.redicap.service.DigitalizacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/digitalizacion")
@RequiredArgsConstructor
@Tag(name = "Digitalización", description = "Recepción de imágenes de actas desde campo – DFR R1")
public class DigitalizacionController {

    private final DigitalizacionService digitalizacionService;

    @Operation(summary = "Subir imagen de acta digitalizada")
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DIGITALIZADOR')")
    public ResponseEntity<ActaResponse> recibir(
            @RequestParam Long casillaId,
            @RequestParam String hashSha256,
            @RequestParam MultipartFile imagen,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        ActaResponse response = digitalizacionService.recibirActa(casillaId, principal.getId(), imagen, hashSha256);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
