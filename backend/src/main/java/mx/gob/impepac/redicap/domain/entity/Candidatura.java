package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;

/** Candidatura vinculada a un partido/coalición y demarcación (DFR R6). */
@Entity @Table(name = "candidaturas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candidatura {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_eleccion", nullable = false, length = 30)
    private TipoEleccion tipoEleccion;

    @Column(nullable = false, length = 200)
    private String nombreCandidato;

    /** Partido que postula (null si es coalición). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id")
    private PartidoPolitico partido;

    /** Coalición que postula (null si es partido solo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coalicion_id")
    private Coalicion coalicion;

    /** Para Diputaciones: el distrito. Para Ayuntamientos: el municipio. Para Gubernatura: null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distrito_id")
    private Distrito distrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    @Column(nullable = false)
    private Boolean activa = true;
}
