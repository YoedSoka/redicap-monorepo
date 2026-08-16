package mx.gob.impepac.redicap.exception;

import org.springframework.http.HttpStatus;

public class RedicapException extends RuntimeException {

    private final HttpStatus status;

    public RedicapException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static RedicapException notFound(String entity, Object id) {
        return new RedicapException(entity + " no encontrado: " + id, HttpStatus.NOT_FOUND);
    }

    public static RedicapException forbidden(String message) {
        return new RedicapException(message, HttpStatus.FORBIDDEN);
    }

    public static RedicapException unauthorized(String message) {
        return new RedicapException(message, HttpStatus.UNAUTHORIZED);
    }

    public static RedicapException badRequest(String message) {
        return new RedicapException(message, HttpStatus.BAD_REQUEST);
    }

    public static RedicapException locked(String message) {
        return new RedicapException(message, HttpStatus.LOCKED);
    }

    public static RedicapException conflict(String message) {
        return new RedicapException(message, HttpStatus.CONFLICT);
    }
}
