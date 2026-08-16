package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VerificacionDetalleResponse {
    private ActaResponse acta;
    private List<CapturaResumenResponse> capturas;
}
