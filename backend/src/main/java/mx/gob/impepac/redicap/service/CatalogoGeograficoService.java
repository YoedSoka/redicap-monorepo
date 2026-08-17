package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.CrearCasillaRequest;
import mx.gob.impepac.redicap.dto.request.CrearDistritoRequest;
import mx.gob.impepac.redicap.dto.request.CrearMunicipioRequest;
import mx.gob.impepac.redicap.dto.request.CrearSeccionRequest;
import mx.gob.impepac.redicap.dto.response.CasillaResponse;
import mx.gob.impepac.redicap.dto.response.DistritoResponse;
import mx.gob.impepac.redicap.dto.response.MunicipioResponse;
import mx.gob.impepac.redicap.dto.response.SeccionResponse;

import java.util.List;

/** Catálogo geográfico-electoral mínimo (Distrito/Municipio/Sección/Casilla) — DFR R1/R6. */
public interface CatalogoGeograficoService {
    List<DistritoResponse> listarDistritos();
    DistritoResponse crearDistrito(CrearDistritoRequest request, Long adminId);
    void eliminarDistrito(Long id, Long adminId);

    List<MunicipioResponse> listarMunicipios();
    MunicipioResponse crearMunicipio(CrearMunicipioRequest request, Long adminId);
    void eliminarMunicipio(Long id, Long adminId);

    List<SeccionResponse> listarSecciones(Long municipioId, Long distritoId);
    SeccionResponse crearSeccion(CrearSeccionRequest request, Long adminId);
    void eliminarSeccion(Long id, Long adminId);

    CasillaResponse crearCasilla(CrearCasillaRequest request, Long adminId);
    void eliminarCasilla(Long id, Long adminId);
}
