package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/** Distrito electoral local de Morelos (12 distritos). */
@Entity @Table(name = "distritos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Distrito {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String clave;           // ej. "D01"

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "cabecera_municipal", length = 100)
    private String cabeceraDistrital;

    @OneToMany(mappedBy = "distrito", fetch = FetchType.LAZY)
    private List<Seccion> secciones;
}
