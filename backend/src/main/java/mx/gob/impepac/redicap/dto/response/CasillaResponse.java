package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;

@Data
@Builder
public class CasillaResponse {
    private Long id;
    private Integer numeroSeccion;
    private TipoCasilla tipo;
    private Integer numeroCasilla;

    public static CasillaResponse from(Casilla c) {
        return CasillaResponse.builder()
                .id(c.getId())
                .numeroSeccion(c.getSeccion().getNumeroSeccion())
                .tipo(c.getTipo())
                .numeroCasilla(c.getNumeroCasilla())
                .build();
    }
}
