package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/** Sección electoral: unidad territorial mínima (DFR Glosario). */
@Entity @Table(name = "secciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seccion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numeroSeccion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "distrito_id")
    private Distrito distrito;

    @OneToMany(mappedBy = "seccion", fetch = FetchType.LAZY)
    private List<Casilla> casillas;
}
