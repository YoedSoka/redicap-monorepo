package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipioRepository extends JpaRepository<Municipio, Long> {
    boolean existsByClave(String clave);
}
