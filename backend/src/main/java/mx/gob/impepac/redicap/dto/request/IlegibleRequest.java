package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IlegibleRequest {
    @NotBlank
    private String motivo;
}
