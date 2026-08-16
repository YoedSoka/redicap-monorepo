package mx.gob.impepac.redicap.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;
import java.time.LocalDateTime;

/** Usuario del sistema REDICAP (DFR R8). */
@Entity @Table(name = "usuarios",
    indexes = @Index(name = "idx_usuarios_username", columnList = "username"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador único personal. */
    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, length = 200)
    private String passwordHash;    // BCrypt

    @Column(nullable = false, length = 200)
    private String nombreCompleto;

    @Column(length = 18)
    private String curp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    /** Para DIGITALIZADOR: la casilla asignada en campo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casilla_asignada_id")
    private Casilla casillaAsignada;

    /** Intentos fallidos de login. */
    @Column(nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
