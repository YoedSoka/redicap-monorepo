package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.PartidoPolitico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartidoPoliticoRepository extends JpaRepository<PartidoPolitico, Long> {
    boolean existsBySiglas(String siglas);
    List<PartidoPolitico> findAllByOrderBySiglasAsc();
}
