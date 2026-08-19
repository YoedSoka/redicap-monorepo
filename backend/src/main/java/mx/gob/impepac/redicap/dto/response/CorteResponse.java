package mx.gob.impepac.redicap.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.CortePublicacion;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Data
@Builder
public class CorteResponse {
    private Long id;
    private TipoEleccion tipoEleccion;
    private LocalDateTime generadoAt;
    private Integer totalActasCapturadas;
    private Integer totalActasValidadas;
    private Integer totalCasillas;
    private Double porcentajeParticipacion;
    private Map<String, Integer> resultados;

    public static CorteResponse from(CortePublicacion c, ObjectMapper mapper) {
        return CorteResponse.builder()
                .id(c.getId())
                .tipoEleccion(c.getTipoEleccion())
                .generadoAt(c.getGeneradoAt())
                .totalActasCapturadas(c.getTotalActasCapturadas())
                .totalActasValidadas(c.getTotalActasValidadas())
                .totalCasillas(c.getTotalCasillas())
                .porcentajeParticipacion(c.getPorcentajeParticipacion())
                .resultados(parsear(c.getResultadosJson(), mapper))
                .build();
    }

    private static Map<String, Integer> parsear(String json, ObjectMapper mapper) {
        if (json == null) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
