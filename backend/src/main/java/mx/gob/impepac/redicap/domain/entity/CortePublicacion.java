package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Snapshot de resultados cada 10 minutos (DFR R4).
 * La réplica de publicación lee de aquí, nunca de las tablas transaccionales.
 */
@Entity @Table(name = "cortes_publicacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CortePublicacion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generado_at", nullable = false, updatable = false)
    private LocalDateTime generadoAt;

    @Column(name = "total_actas_capturadas", nullable = false)
    private Integer totalActasCapturadas;

    @Column(name = "total_actas_validadas", nullable = false)
    private Integer totalActasValidadas;

    @Column(name = "total_casillas", nullable = false)
    private Integer totalCasillas;

    /** Porcentaje de participación acumulada (DFR R4 dashboard). */
    @Column(name = "porcentaje_participacion")
    private Double porcentajeParticipacion;

    /** Resultados JSON consolidados por elección y partido. */
    @Column(name = "resultados_json", columnDefinition = "TEXT")
    private String resultadosJson;

    @Column(nullable = false)
    private Boolean exitoso = true;

    @PrePersist
    void prePersist() { this.generadoAt = LocalDateTime.now(); }
}
