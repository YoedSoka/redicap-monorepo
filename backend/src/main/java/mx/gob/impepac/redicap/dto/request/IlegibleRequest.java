package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.gob.impepac.redicap.domain.enums.MotivoDictamenVerificador;

@Data
public class IlegibleRequest {
    @NotNull
    private MotivoDictamenVerificador motivoCatalogo;

    @NotBlank
    private String justificacion;
}
