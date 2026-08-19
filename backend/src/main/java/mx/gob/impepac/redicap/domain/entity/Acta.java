package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import java.time.LocalDateTime;

/**
 * Acta de Escrutinio y Cómputo (núcleo del sistema – DFR R1-R9).
 * Contiene la imagen digitalizada, su hash de integridad y el estado
 * en la máquina de estados del flujo de captura.
 *
 * Cada casilla produce hasta 3 actas independientes, una por elección
 * (Gubernatura, Diputación Local, Ayuntamiento) — son documentos físicos
 * separados en la práctica real, no un acta combinada (DFR R4/R5/R6).
 */
@Entity @Table(name = "actas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"casilla_id", "tipo_eleccion"}),
    indexes = {
        @Index(name = "idx_actas_casilla", columnList = "casilla_id"),
        @Index(name = "idx_actas_estado",  columnList = "estado"),
        @Index(name = "idx_actas_tipo_eleccion", columnList = "tipo_eleccion")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Acta {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "casilla_id")
    private Casilla casilla;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_eleccion", nullable = false, length = 30)
    private TipoEleccion tipoEleccion;

    /** Ruta relativa encriptada al archivo de imagen en el servidor. */
    @Column(name = "ruta_imagen", length = 500)
    private String rutaImagen;

    /** SHA-256 calculado en el dispositivo móvil antes de transmitir. */
    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoActa estado = EstadoActa.RECIBIDA;

    /** Usuario digitalizador (CAE) que capturó la imagen en campo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "digitalizador_id")
    private Usuario digitalizador;

    /** Timestamp generado por el reloj del servidor. */
    @Column(name = "recibida_at", nullable = false, updatable = false)
    private LocalDateTime recibidaAt;

    @Column(name = "publicada_at")
    private LocalDateTime publicadaAt;

    // ── Flags de integridad aritmética ──────────────────────
    @Builder.Default
    @Column(name = "error_aritmetico", nullable = false)
    private Boolean errorAritmetico = false;

    @Builder.Default
    @Column(name = "excede_lista_nominal", nullable = false)
    private Boolean excedeListaNominal = false;

    @PrePersist
    void prePersist() { this.recibidaAt = LocalDateTime.now(); }
}
