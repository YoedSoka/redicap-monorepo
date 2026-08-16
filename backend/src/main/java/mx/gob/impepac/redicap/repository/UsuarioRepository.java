package mx.gob.impepac.redicap.repository;

import mx.gob.impepac.redicap.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);

    /** Verifica que el usuario NO tenga ya una captura sobre esta acta (DFR R2). */
    @Query("SELECT COUNT(c) > 0 FROM CapturaActa c WHERE c.acta.id = :actaId AND c.capturista.id = :usuarioId")
    boolean yaCapturadoPorUsuario(Long actaId, Long usuarioId);
}
