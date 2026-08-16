package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Registro individual de una captura numérica de acta (DFR R2).
 * Cada acta puede tener hasta 3 registros: C1, C2, C3.
 * NUNCA se actualiza: append-only por diseño.
 */
@Entity @Table(name = "capturas_acta",
    uniqueConstraints = @UniqueConstraint(columnNames = {"acta_id", "numero_captura"}),
    indexes = {
        @Index(name = "idx_capturas_acta", columnList = "acta_id"),
        @Index(name = "idx_capturas_usuario", columnList = "capturista_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CapturaActa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "acta_id")
    private Acta acta;

    /** 1, 2 o 3. */
    @Column(name = "numero_captura", nullable = false)
    private Integer numeroCaptura;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "capturista_id")
    private Usuario capturista;

    /** JSON con los votos: {"PAN":12,"MORENA":45,"NULOS":3,...} */
    @Column(name = "datos_votos", nullable = false, columnDefinition = "TEXT")
    private String datosVotosJson;

    /** Total de votos anotado en el acta por los funcionarios de casilla. */
    @Column(name = "total_votos_acta")
    private Integer totalVotosActa;

    /** Total calculado por el sistema (suma de datosVotos). */
    @Column(name = "total_votos_calculado")
    private Integer totalVotosCalculado;

    /** Timestamp del servidor al momento de guardar. */
    @Column(name = "capturado_at", nullable = false, updatable = false)
    private LocalDateTime capturadoAt;

    @PrePersist
    void prePersist() { this.capturadoAt = LocalDateTime.now(); }
}
