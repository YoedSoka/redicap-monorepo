package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.ActualizarPartidoRequest;
import mx.gob.impepac.redicap.dto.request.CrearPartidoRequest;
import mx.gob.impepac.redicap.dto.response.PartidoPoliticoResponse;

import java.util.List;

/** Catálogo de partidos políticos (DFR R6). */
public interface PartidoPoliticoService {
    List<PartidoPoliticoResponse> listar();
    PartidoPoliticoResponse crear(CrearPartidoRequest request, Long adminId);
    PartidoPoliticoResponse actualizar(Long id, ActualizarPartidoRequest request, Long adminId);
    PartidoPoliticoResponse cambiarActivo(Long id, boolean activo, Long adminId);
}
