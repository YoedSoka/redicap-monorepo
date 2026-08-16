package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Decisión de la mesa de deliberación: cuál de las 3 capturas coincide con el acta física. */
@Data
public class ValidarVerificacionRequest {
    @NotNull @Min(1) @Max(3)
    private Integer numeroCapturaElegida;
}
