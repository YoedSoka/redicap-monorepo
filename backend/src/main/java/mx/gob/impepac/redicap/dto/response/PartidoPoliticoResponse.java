package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.PartidoPolitico;

@Data
@Builder
public class PartidoPoliticoResponse {
    private Long id;
    private String siglas;
    private String nombre;
    private String colorHex;
    private Boolean activo;

    public static PartidoPoliticoResponse from(PartidoPolitico p) {
        return PartidoPoliticoResponse.builder()
                .id(p.getId())
                .siglas(p.getSiglas())
                .nombre(p.getNombre())
                .colorHex(p.getColorHex())
                .activo(p.getActivo())
                .build();
    }
}
