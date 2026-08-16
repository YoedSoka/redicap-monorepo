package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.response.ActaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DigitalizacionService {

    /**
     * Recibe la imagen de un acta digitalizada en campo (DFR R1).
     *
     * @param casillaId        casilla a la que corresponde el acta
     * @param digitalizadorId  usuario DIGITALIZADOR autenticado
     * @param imagen           archivo de imagen capturado en el dispositivo móvil
     * @param hashSha256Cliente SHA-256 calculado en el dispositivo antes de transmitir
     */
    ActaResponse recibirActa(Long casillaId, Long digitalizadorId, MultipartFile imagen, String hashSha256Cliente);
}
