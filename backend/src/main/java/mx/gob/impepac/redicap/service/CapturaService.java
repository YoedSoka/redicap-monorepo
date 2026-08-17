package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.CapturaRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.ImagenActaResponse;

public interface CapturaService {
    /** Obtiene el siguiente acta disponible para captura (asignación aleatoria). */
    ActaResponse obtenerSiguienteActa(Long usuarioId);
    /** Guarda la captura y avanza la máquina de estados del acta. */
    ActaResponse registrarCaptura(Long actaId, Long usuarioId, CapturaRequest request);
    /** Imagen digitalizada del acta, para usarla de referencia visual al capturar. */
    ImagenActaResponse obtenerImagen(Long actaId);
}
