package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/** Municipio del estado de Morelos (33 municipios). */
@Entity @Table(name = "municipios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Municipio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String clave;           // ej. "MUN001"

    @Column(nullable = false, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "municipio", fetch = FetchType.LAZY)
    private List<Seccion> secciones;
}
