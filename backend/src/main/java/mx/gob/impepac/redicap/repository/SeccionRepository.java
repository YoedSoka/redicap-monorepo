package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    boolean existsByNumeroSeccion(Integer numeroSeccion);
    List<Seccion> findByMunicipioId(Long municipioId);
    List<Seccion> findByDistritoId(Long distritoId);
}
