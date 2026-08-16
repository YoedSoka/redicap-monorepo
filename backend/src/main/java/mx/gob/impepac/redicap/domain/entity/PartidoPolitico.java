package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/** Partido político participante (DFR R6). */
@Entity @Table(name = "partidos_politicos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartidoPolitico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String siglas;          // ej. "MORENA", "PAN", "PRI"

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "color_hex", length = 7)
    private String colorHex;        // para la plantilla de publicación

    @Column(nullable = false)
    private Boolean activo = true;
}
