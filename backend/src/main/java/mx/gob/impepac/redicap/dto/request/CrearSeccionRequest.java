package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CrearSeccionRequest {
    @NotNull @Positive
    private Integer numeroSeccion;

    @NotNull
    private Long municipioId;

    @NotNull
    private Long distritoId;
}
