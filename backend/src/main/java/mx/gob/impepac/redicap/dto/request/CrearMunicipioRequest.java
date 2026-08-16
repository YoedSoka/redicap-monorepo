package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearMunicipioRequest {
    @NotBlank @Size(max = 10)
    private String clave;

    @NotBlank @Size(max = 100)
    private String nombre;
}
