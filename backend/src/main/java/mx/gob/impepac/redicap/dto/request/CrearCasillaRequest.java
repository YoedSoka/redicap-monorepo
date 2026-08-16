package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;

@Data
public class CrearCasillaRequest {
    @NotNull
    private Long seccionId;

    @NotNull
    private TipoCasilla tipo;

    @NotNull @Positive
    private Integer numeroCasilla;

    @NotNull @Positive
    private Integer listaNominal;
}
