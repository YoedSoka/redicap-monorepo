package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Map;

/** Datos de una captura numérica de acta. */
@Data
public class CapturaRequest {

    /** Mapa de votos: clave = siglas partido o "NULOS" / "NO_REGISTRADOS". */
    @NotEmpty
    private Map<String, Integer> votos;

    /** Total escrito por los funcionarios en el acta (puede diferir del calculado). */
    private Integer totalVotosActa;
}
