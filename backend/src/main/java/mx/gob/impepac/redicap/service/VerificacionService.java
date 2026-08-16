package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.VerificacionDetalleResponse;

import java.util.List;

/** Mesa de deliberación para actas con 3 capturas divergentes (DFR R3). */
public interface VerificacionService {

    /** Actas pendientes de deliberación. */
    List<ActaResponse> listarPendientes();

    /** Acta + sus 3 capturas, para que la mesa delibere. */
    VerificacionDetalleResponse obtenerDetalle(Long actaId);

    /** La mesa determina que una de las 3 capturas coincide con el acta física. */
    ActaResponse validar(Long actaId, Long verificadorId, Integer numeroCapturaElegida);

    /** El acta física es ilegible o no permite determinar un resultado. */
    ActaResponse marcarIlegible(Long actaId, Long verificadorId, String motivo);
}
