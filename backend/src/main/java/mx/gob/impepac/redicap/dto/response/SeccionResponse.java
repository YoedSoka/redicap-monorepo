package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Seccion;

@Data
@Builder
public class SeccionResponse {
    private Long id;
    private Integer numeroSeccion;
    private Long municipioId;
    private String municipioNombre;
    private Long distritoId;
    private String distritoNombre;

    public static SeccionResponse from(Seccion s) {
        return SeccionResponse.builder()
                .id(s.getId())
                .numeroSeccion(s.getNumeroSeccion())
                .municipioId(s.getMunicipio().getId())
                .municipioNombre(s.getMunicipio().getNombre())
                .distritoId(s.getDistrito().getId())
                .distritoNombre(s.getDistrito().getNombre())
                .build();
    }
}
