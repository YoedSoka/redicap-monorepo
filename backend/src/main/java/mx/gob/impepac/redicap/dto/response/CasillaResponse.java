package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;

@Data
@Builder
public class CasillaResponse {
    private Long id;
    private Long seccionId;
    private Integer numeroSeccion;
    private TipoCasilla tipo;
    private Integer numeroCasilla;
    private Integer listaNominal;
    private Boolean activa;
    private String municipioNombre;
    private String distritoNombre;

    public static CasillaResponse from(Casilla c) {
        return CasillaResponse.builder()
                .id(c.getId())
                .seccionId(c.getSeccion().getId())
                .numeroSeccion(c.getSeccion().getNumeroSeccion())
                .tipo(c.getTipo())
                .numeroCasilla(c.getNumeroCasilla())
                .listaNominal(c.getListaNominal())
                .activa(c.getActiva())
                .municipioNombre(c.getSeccion().getMunicipio().getNombre())
                .distritoNombre(c.getSeccion().getDistrito().getNombre())
                .build();
    }
}
