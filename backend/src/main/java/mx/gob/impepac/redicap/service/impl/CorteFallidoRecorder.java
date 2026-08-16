package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import mx.gob.impepac.redicap.domain.entity.CortePublicacion;
import mx.gob.impepac.redicap.repository.CortePublicacionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja constancia de un corte fallido en su propia transacción (REQUIRES_NEW):
 * si PublicacionServiceImpl.generarCorte() falla a medio camino, su transacción
 * queda marcada rollback-only y no puede usarse para guardar nada más.
 */
@Component
@RequiredArgsConstructor
class CorteFallidoRecorder {

    private final CortePublicacionRepository corteRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CortePublicacion registrarCorteFallido() {
        return corteRepo.save(CortePublicacion.builder()
                .totalActasCapturadas(0)
                .totalActasValidadas(0)
                .totalCasillas(0)
                .exitoso(false)
                .build());
    }
}
