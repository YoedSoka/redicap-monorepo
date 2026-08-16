package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Distrito;

@Data
@Builder
public class DistritoResponse {
    private Long id;
    private String clave;
    private String nombre;
    private String cabeceraDistrital;

    public static DistritoResponse from(Distrito d) {
        return DistritoResponse.builder()
                .id(d.getId())
                .clave(d.getClave())
                .nombre(d.getNombre())
                .cabeceraDistrital(d.getCabeceraDistrital())
                .build();
    }
}
