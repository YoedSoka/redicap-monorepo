package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.domain.entity.CortePublicacion;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;

import java.util.List;
import java.util.Optional;

public interface PublicacionService {
    /**
     * Genera un snapshot de resultados por cada elección (DFR R4/R5): agrega votos de
     * las actas finalizadas (VALIDADA, VALIDADA_VERIFICADOR, PUBLICADA) de cada
     * TipoEleccion por separado, y publica las que aún no lo estaban.
     */
    List<CortePublicacion> generarCorte();

    /** Último corte generado para una elección; lee de caché (Redis) y cae a BD si no está cacheado. */
    Optional<CortePublicacion> obtenerUltimoCorte(TipoEleccion tipoEleccion);

    /** Historial de los últimos cortes exitosos de una elección, más reciente primero. */
    List<CortePublicacion> obtenerHistorial(TipoEleccion tipoEleccion);
}
