package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;

/**
 * Casilla electoral (DFR R6).
 * Clave única = Distrito + Municipio + Sección + Tipo.
 */
@Entity @Table(name = "casillas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_id", "tipo", "numero_casilla"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Casilla {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCasilla tipo;

    @Column(name = "numero_casilla", nullable = false)
    private Integer numeroCasilla;

    /** Total de ciudadanos con derecho a voto en esta casilla. */
    @Column(name = "lista_nominal", nullable = false)
    private Integer listaNominal;

    @Column(nullable = false)
    private Boolean activa = true;
}
