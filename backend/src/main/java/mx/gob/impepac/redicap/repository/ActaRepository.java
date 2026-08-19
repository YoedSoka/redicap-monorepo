package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActaRepository extends JpaRepository<Acta, Long> {
    /** Una casilla puede tener hasta 3 actas: una por elección (DFR R4/R5). */
    Optional<Acta> findByCasillaIdAndTipoEleccion(Long casillaId, TipoEleccion tipoEleccion);
    boolean existsByCasillaId(Long casillaId);
    List<Acta> findByEstado(EstadoActa estado);
    List<Acta> findByEstadoIn(Collection<EstadoActa> estados);
    List<Acta> findByEstadoInAndTipoEleccion(Collection<EstadoActa> estados, TipoEleccion tipoEleccion);

    /** Siguiente acta disponible para captura (asignación aleatoria – DFR R2). */
    @Query(value = """
        SELECT * FROM actas
        WHERE estado = :estado
        AND id NOT IN (
            SELECT acta_id FROM capturas_acta WHERE capturista_id = :usuarioId
        )
        ORDER BY RANDOM()
        LIMIT 1
        """, nativeQuery = true)
    Optional<Acta> findSiguienteParaCaptura(String estado, Long usuarioId);

    long countByEstado(EstadoActa estado);
}
