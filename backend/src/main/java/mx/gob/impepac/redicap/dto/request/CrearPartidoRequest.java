package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearPartidoRequest {
    @NotBlank
    @Size(max = 20)
    private String siglas;

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hexadecimal, ej. #DC2597")
    private String colorHex;
}
