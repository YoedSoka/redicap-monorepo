package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;

@Data @Builder
public class ActaResponse {
    private Long id;
    private Long casillaId;
    private TipoEleccion tipoEleccion;
    private EstadoActa estado;
    private String rutaImagen;
    private Boolean errorAritmetico;
    private Boolean excedeListaNominal;
    private String folio;

    public static ActaResponse from(Acta a) {
        return ActaResponse.builder()
                .id(a.getId())
                .casillaId(a.getCasilla().getId())
                .tipoEleccion(a.getTipoEleccion())
                .estado(a.getEstado())
                .rutaImagen(a.getRutaImagen())
                .errorAritmetico(a.getErrorAritmetico())
                .excedeListaNominal(a.getExcedeListaNominal())
                .folio("RDCP-" + String.format("%08d", a.getId()))
                .build();
    }
}
