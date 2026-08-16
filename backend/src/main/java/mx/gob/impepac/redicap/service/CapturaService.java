package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.CapturaRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;

public interface CapturaService {
    /** Obtiene el siguiente acta disponible para captura (asignación aleatoria). */
    ActaResponse obtenerSiguienteActa(Long usuarioId);
    /** Guarda la captura y avanza la máquina de estados del acta. */
    ActaResponse registrarCaptura(Long actaId, Long usuarioId, CapturaRequest request);
}
