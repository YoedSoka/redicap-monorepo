package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;

@Data @Builder
public class ActaResponse {
    private Long id;
    private Long casillaId;
    private EstadoActa estado;
    private String rutaImagen;
    private Boolean errorAritmetico;
    private Boolean excedeListaNominal;

    public static ActaResponse from(Acta a) {
        return ActaResponse.builder()
                .id(a.getId())
                .casillaId(a.getCasilla().getId())
                .estado(a.getEstado())
                .rutaImagen(a.getRutaImagen())
                .errorAritmetico(a.getErrorAritmetico())
                .excedeListaNominal(a.getExcedeListaNominal())
                .build();
    }
}
