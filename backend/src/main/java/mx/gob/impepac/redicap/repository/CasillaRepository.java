package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Casilla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CasillaRepository extends JpaRepository<Casilla, Long> {
    Optional<Casilla> findBySeccionNumeroSeccionAndTipoAndNumeroCasilla(
        Integer numeroSeccion, mx.gob.impepac.redicap.domain.enums.TipoCasilla tipo, Integer numeroCasilla);
    List<Casilla> findBySeccionId(Long seccionId);
    boolean existsBySeccionId(Long seccionId);
    boolean existsBySeccionIdAndTipoAndNumeroCasilla(
        Long seccionId, mx.gob.impepac.redicap.domain.enums.TipoCasilla tipo, Integer numeroCasilla);

    /** Las casillas ESPECIAL no participan en Ayuntamiento (DFR R5). */
    long countByTipoNot(mx.gob.impepac.redicap.domain.enums.TipoCasilla tipo);
}
