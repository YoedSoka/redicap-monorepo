package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CapturaResumenResponse {
    private Integer numeroCaptura;
    private Long capturistaId;
    private Map<String, Integer> votos;
    private Integer totalVotosActa;
    private Integer totalVotosCalculado;
}
