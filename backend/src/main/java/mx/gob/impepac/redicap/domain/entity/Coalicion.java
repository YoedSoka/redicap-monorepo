package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import java.util.List;

/**
 * Coalición: alianza de partidos para una elección específica (DFR R6).
 * Los votos se pueden emitir cruzando uno o más partidos de la coalición.
 */
@Entity @Table(name = "coaliciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coalicion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;          // ej. "Sigamos Haciendo Historia en Morelos"

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_eleccion", nullable = false, length = 30)
    private TipoEleccion tipoEleccion;

    @ManyToMany
    @JoinTable(name = "coalicion_partidos",
        joinColumns = @JoinColumn(name = "coalicion_id"),
        inverseJoinColumns = @JoinColumn(name = "partido_id"))
    private List<PartidoPolitico> partidos;

    @Column(nullable = false)
    private Boolean activa = true;
}
