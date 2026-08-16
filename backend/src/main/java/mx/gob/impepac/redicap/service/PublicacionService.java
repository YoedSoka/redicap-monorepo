package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.domain.entity.CortePublicacion;

import java.util.List;
import java.util.Optional;

public interface PublicacionService {
    /**
     * Genera un snapshot de resultados (DFR R4): agrega votos de todas las actas
     * finalizadas (VALIDADA, VALIDADA_VERIFICADOR, PUBLICADA) y publica las que
     * aún no lo estaban (VALIDADA/VALIDADA_VERIFICADOR → PUBLICADA).
     */
    CortePublicacion generarCorte();

    /** Último corte generado; lee de caché (Redis) y cae a BD si no está cacheado. */
    Optional<CortePublicacion> obtenerUltimoCorte();

    /** Historial de los últimos cortes exitosos, más reciente primero. */
    List<CortePublicacion> obtenerHistorial();
}
