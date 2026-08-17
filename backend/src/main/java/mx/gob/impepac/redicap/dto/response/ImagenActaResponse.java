package mx.gob.impepac.redicap.dto.response;

/** Contenido crudo de la imagen digitalizada de un acta, con su content-type real. */
public record ImagenActaResponse(byte[] contenido, String contentType) {
}
