package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistritoRepository extends JpaRepository<Distrito, Long> {
    boolean existsByClave(String clave);
}
