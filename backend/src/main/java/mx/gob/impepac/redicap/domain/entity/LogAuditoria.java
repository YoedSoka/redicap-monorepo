package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Bitácora de auditoría APPEND-ONLY (DFR R9).
 * Prohibido UPDATE/DELETE a nivel de BD.
 * Campos mínimos obligatorios definidos en el DFR.
 */
@Entity @Table(name = "log_auditoria",
    indexes = {
        @Index(name = "idx_log_usuario", columnList = "usuario_id"),
        @Index(name = "idx_log_timestamp", columnList = "timestamp_accion"),
        @Index(name = "idx_log_modulo", columnList = "modulo")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LogAuditoria {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "ip_origen", length = 45)    // IPv6 = hasta 45 chars
    private String ipOrigen;

    @Column(name = "tipo_accion", nullable = false, length = 30)
    private String tipoAccion;       // INSERT, UPDATE, LOGIN, LOGOUT, EXPORT…

    @Column(nullable = false, length = 50)
    private String modulo;           // DIGITALIZACION, CAPTURA, VERIFICACION…

    /** Entidad y ID afectados (ej. "Acta#1024"). */
    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    /** Valor anterior serializado como JSON (null para INSERTs). */
    @Column(name = "valor_antes", columnDefinition = "TEXT")
    private String valorAntes;

    /** Valor posterior serializado como JSON. */
    @Column(name = "valor_despues", columnDefinition = "TEXT")
    private String valorDespues;

    /** Precisión de milisegundos, generado por el servidor. */
    @Column(name = "timestamp_accion", nullable = false, updatable = false)
    private LocalDateTime timestampAccion;

    @PrePersist
    void prePersist() { this.timestampAccion = LocalDateTime.now(); }
}
