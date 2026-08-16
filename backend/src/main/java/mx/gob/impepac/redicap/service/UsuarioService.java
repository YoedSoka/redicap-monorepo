package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.ActualizarUsuarioRequest;
import mx.gob.impepac.redicap.dto.request.CrearUsuarioRequest;
import mx.gob.impepac.redicap.dto.response.UsuarioResponse;

import java.util.List;

/** Gestión de usuarios del sistema por el ADMINISTRADOR (DFR R8). */
public interface UsuarioService {
    List<UsuarioResponse> listar();
    UsuarioResponse obtenerPropio(Long usuarioId);
    UsuarioResponse crear(CrearUsuarioRequest request, Long adminId);
    UsuarioResponse actualizar(Long usuarioId, ActualizarUsuarioRequest request, Long adminId);
    UsuarioResponse desbloquear(Long usuarioId, Long adminId);
    UsuarioResponse cambiarActivo(Long usuarioId, boolean activo, Long adminId);
}
