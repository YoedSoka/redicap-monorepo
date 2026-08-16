package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.CapturaActa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CapturaActaRepository extends JpaRepository<CapturaActa, Long> {
    List<CapturaActa> findByActaIdOrderByNumeroCapturaAsc(Long actaId);
    boolean existsByActaIdAndCapturistaId(Long actaId, Long capturistaId);
    int countByActaId(Long actaId);
}
