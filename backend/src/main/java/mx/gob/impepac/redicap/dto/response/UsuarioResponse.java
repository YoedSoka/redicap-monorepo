package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String username;
    private String nombreCompleto;
    private String curp;
    private RolUsuario rol;
    private Long casillaAsignadaId;
    private Boolean activo;
    private Integer intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private LocalDateTime createdAt;

    public static UsuarioResponse from(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nombreCompleto(u.getNombreCompleto())
                .curp(u.getCurp())
                .rol(u.getRol())
                .casillaAsignadaId(u.getCasillaAsignada() != null ? u.getCasillaAsignada().getId() : null)
                .activo(u.getActivo())
                .intentosFallidos(u.getIntentosFallidos())
                .bloqueadoHasta(u.getBloqueadoHasta())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
