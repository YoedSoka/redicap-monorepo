package mx.gob.impepac.redicap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;

@Data
public class ActualizarUsuarioRequest {
    @NotBlank
    @Size(max = 200)
    private String nombreCompleto;

    @Size(max = 18)
    private String curp;

    @NotNull
    private RolUsuario rol;

    private Long casillaAsignadaId;
}
