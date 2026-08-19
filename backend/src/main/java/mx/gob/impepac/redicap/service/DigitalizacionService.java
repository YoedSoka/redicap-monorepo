package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DigitalizacionService {

    /**
     * Recibe la imagen de un acta digitalizada en campo (DFR R1).
     * Una casilla produce hasta 3 actas independientes, una por elección (DFR R4/R5).
     *
     * @param casillaId        casilla a la que corresponde el acta
     * @param tipoEleccion     elección a la que pertenece esta acta
     * @param digitalizadorId  usuario DIGITALIZADOR autenticado
     * @param imagen           archivo de imagen capturado en el dispositivo móvil
     * @param hashSha256Cliente SHA-256 calculado en el dispositivo antes de transmitir
     */
    ActaResponse recibirActa(Long casillaId, TipoEleccion tipoEleccion, Long digitalizadorId,
                              MultipartFile imagen, String hashSha256Cliente);
}
