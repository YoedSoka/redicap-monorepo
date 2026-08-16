package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
    List<LogAuditoria> findByEntidadAfectadaOrderByTimestampAccionAsc(String entidadAfectada);
    Optional<LogAuditoria> findFirstByEntidadAfectadaAndTipoAccion(String entidadAfectada, String tipoAccion);
}
