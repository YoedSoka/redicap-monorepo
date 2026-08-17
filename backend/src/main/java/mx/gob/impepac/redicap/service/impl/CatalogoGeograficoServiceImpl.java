package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.Distrito;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Municipio;
import mx.gob.impepac.redicap.domain.entity.Seccion;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.request.CrearCasillaRequest;
import mx.gob.impepac.redicap.dto.request.CrearDistritoRequest;
import mx.gob.impepac.redicap.dto.request.CrearMunicipioRequest;
import mx.gob.impepac.redicap.dto.request.CrearSeccionRequest;
import mx.gob.impepac.redicap.dto.response.CasillaResponse;
import mx.gob.impepac.redicap.dto.response.DistritoResponse;
import mx.gob.impepac.redicap.dto.response.MunicipioResponse;
import mx.gob.impepac.redicap.dto.response.SeccionResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.DistritoRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.MunicipioRepository;
import mx.gob.impepac.redicap.repository.SeccionRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.service.CatalogoGeograficoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CatalogoGeograficoServiceImpl implements CatalogoGeograficoService {

    private static final String MODULO = "ADMINISTRACION";

    private final DistritoRepository distritoRepo;
    private final MunicipioRepository municipioRepo;
    private final SeccionRepository seccionRepo;
    private final CasillaRepository casillaRepo;
    private final ActaRepository actaRepo;
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;

    @Override
    @Transactional(readOnly = true)
    public List<DistritoResponse> listarDistritos() {
        return distritoRepo.findAll().stream().map(DistritoResponse::from).toList();
    }

    @Override
    public DistritoResponse crearDistrito(CrearDistritoRequest request, Long adminId) {
        String clave = request.getClave().trim().toUpperCase();
        if (distritoRepo.existsByClave(clave)) {
            throw RedicapException.conflict("Ya existe un distrito con esa clave");
        }
        Distrito distrito = Distrito.builder()
                .clave(clave)
                .nombre(request.getNombre())
                .cabeceraDistrital(request.getCabeceraDistrital())
                .build();
        distritoRepo.save(distrito);
        registrarAuditoria(adminId, "DISTRITO_CREADO", "Distrito#" + distrito.getId());
        return DistritoResponse.from(distrito);
    }

    @Override
    public void eliminarDistrito(Long id, Long adminId) {
        if (!distritoRepo.existsById(id)) {
            throw RedicapException.notFound("Distrito", id);
        }
        if (seccionRepo.existsByDistritoId(id)) {
            throw RedicapException.conflict("No se puede eliminar: hay secciones que pertenecen a este distrito");
        }
        distritoRepo.deleteById(id);
        registrarAuditoria(adminId, "DISTRITO_ELIMINADO", "Distrito#" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipioResponse> listarMunicipios() {
        return municipioRepo.findAll().stream().map(MunicipioResponse::from).toList();
    }

    @Override
    public MunicipioResponse crearMunicipio(CrearMunicipioRequest request, Long adminId) {
        String clave = request.getClave().trim().toUpperCase();
        if (municipioRepo.existsByClave(clave)) {
            throw RedicapException.conflict("Ya existe un municipio con esa clave");
        }
        Municipio municipio = Municipio.builder()
                .clave(clave)
                .nombre(request.getNombre())
                .build();
        municipioRepo.save(municipio);
        registrarAuditoria(adminId, "MUNICIPIO_CREADO", "Municipio#" + municipio.getId());
        return MunicipioResponse.from(municipio);
    }

    @Override
    public void eliminarMunicipio(Long id, Long adminId) {
        if (!municipioRepo.existsById(id)) {
            throw RedicapException.notFound("Municipio", id);
        }
        if (seccionRepo.existsByMunicipioId(id)) {
            throw RedicapException.conflict("No se puede eliminar: hay secciones que pertenecen a este municipio");
        }
        municipioRepo.deleteById(id);
        registrarAuditoria(adminId, "MUNICIPIO_ELIMINADO", "Municipio#" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeccionResponse> listarSecciones(Long municipioId, Long distritoId) {
        List<Seccion> secciones;
        if (municipioId != null) {
            secciones = seccionRepo.findByMunicipioId(municipioId);
        } else if (distritoId != null) {
            secciones = seccionRepo.findByDistritoId(distritoId);
        } else {
            secciones = seccionRepo.findAll();
        }
        return secciones.stream().map(SeccionResponse::from).toList();
    }

    @Override
    public SeccionResponse crearSeccion(CrearSeccionRequest request, Long adminId) {
        if (seccionRepo.existsByNumeroSeccion(request.getNumeroSeccion())) {
            throw RedicapException.conflict("Ya existe una sección con ese número");
        }
        Municipio municipio = municipioRepo.findById(request.getMunicipioId())
                .orElseThrow(() -> RedicapException.notFound("Municipio", request.getMunicipioId()));
        Distrito distrito = distritoRepo.findById(request.getDistritoId())
                .orElseThrow(() -> RedicapException.notFound("Distrito", request.getDistritoId()));

        Seccion seccion = Seccion.builder()
                .numeroSeccion(request.getNumeroSeccion())
                .municipio(municipio)
                .distrito(distrito)
                .build();
        seccionRepo.save(seccion);
        registrarAuditoria(adminId, "SECCION_CREADA", "Seccion#" + seccion.getId());
        return SeccionResponse.from(seccion);
    }

    @Override
    public void eliminarSeccion(Long id, Long adminId) {
        if (!seccionRepo.existsById(id)) {
            throw RedicapException.notFound("Seccion", id);
        }
        if (casillaRepo.existsBySeccionId(id)) {
            throw RedicapException.conflict("No se puede eliminar: hay casillas que pertenecen a esta sección");
        }
        seccionRepo.deleteById(id);
        registrarAuditoria(adminId, "SECCION_ELIMINADA", "Seccion#" + id);
    }

    @Override
    public CasillaResponse crearCasilla(CrearCasillaRequest request, Long adminId) {
        Seccion seccion = seccionRepo.findById(request.getSeccionId())
                .orElseThrow(() -> RedicapException.notFound("Seccion", request.getSeccionId()));
        if (casillaRepo.existsBySeccionIdAndTipoAndNumeroCasilla(
                request.getSeccionId(), request.getTipo(), request.getNumeroCasilla())) {
            throw RedicapException.conflict("Ya existe una casilla con esa clave (sección + tipo + número)");
        }

        Casilla casilla = Casilla.builder()
                .seccion(seccion)
                .tipo(request.getTipo())
                .numeroCasilla(request.getNumeroCasilla())
                .listaNominal(request.getListaNominal())
                .activa(true)
                .build();
        casillaRepo.save(casilla);
        registrarAuditoria(adminId, "CASILLA_CREADA", "Casilla#" + casilla.getId());
        log.info("Casilla {} creada por admin {}", casilla.getId(), adminId);
        return CasillaResponse.from(casilla);
    }

    @Override
    public void eliminarCasilla(Long id, Long adminId) {
        if (!casillaRepo.existsById(id)) {
            throw RedicapException.notFound("Casilla", id);
        }
        if (actaRepo.findByCasillaId(id).isPresent()) {
            throw RedicapException.conflict("No se puede eliminar: esta casilla ya tiene un acta digitalizada");
        }
        casillaRepo.deleteById(id);
        registrarAuditoria(adminId, "CASILLA_ELIMINADA", "Casilla#" + id);
        log.info("Casilla {} eliminada por admin {}", id, adminId);
    }

    private void registrarAuditoria(Long adminId, String tipoAccion, String entidadAfectada) {
        Usuario admin = usuarioRepo.findById(adminId).orElse(null);
        logRepo.save(LogAuditoria.builder()
                .usuario(admin)
                .tipoAccion(tipoAccion)
                .modulo(MODULO)
                .entidadAfectada(entidadAfectada)
                .build());
    }
}
