package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.CortePublicacion;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CortePublicacionRepository extends JpaRepository<CortePublicacion, Long> {
    Optional<CortePublicacion> findTopByTipoEleccionOrderByGeneradoAtDesc(TipoEleccion tipoEleccion);
    List<CortePublicacion> findTop20ByTipoEleccionAndExitosoTrueOrderByGeneradoAtDesc(TipoEleccion tipoEleccion);
}
