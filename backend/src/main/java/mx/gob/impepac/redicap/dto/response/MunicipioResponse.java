package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Municipio;

@Data
@Builder
public class MunicipioResponse {
    private Long id;
    private String clave;
    private String nombre;

    public static MunicipioResponse from(Municipio m) {
        return MunicipioResponse.builder()
                .id(m.getId())
                .clave(m.getClave())
                .nombre(m.getNombre())
                .build();
    }
}
