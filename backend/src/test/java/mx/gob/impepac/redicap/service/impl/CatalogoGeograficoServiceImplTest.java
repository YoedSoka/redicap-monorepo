package mx.gob.impepac.redicap.service.impl;

import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.Distrito;
import mx.gob.impepac.redicap.domain.entity.Municipio;
import mx.gob.impepac.redicap.domain.entity.Seccion;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;
import mx.gob.impepac.redicap.dto.request.CrearCasillaRequest;
import mx.gob.impepac.redicap.dto.request.CrearDistritoRequest;
import mx.gob.impepac.redicap.dto.request.CrearMunicipioRequest;
import mx.gob.impepac.redicap.dto.request.CrearSeccionRequest;
import mx.gob.impepac.redicap.dto.response.CasillaResponse;
import mx.gob.impepac.redicap.dto.response.DistritoResponse;
import mx.gob.impepac.redicap.dto.response.MunicipioResponse;
import mx.gob.impepac.redicap.dto.response.SeccionResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.DistritoRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.MunicipioRepository;
import mx.gob.impepac.redicap.repository.SeccionRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el catálogo geográfico-electoral (DFR R1/R6): Distrito y Municipio son listas
 * independientes; Sección une un número con un Municipio y un Distrito; Casilla cuelga
 * de una Sección con clave única sección+tipo+número.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoGeograficoServiceImplTest {

    private static final Long ADMIN_ID = 1L;

    @Mock private DistritoRepository distritoRepo;
    @Mock private MunicipioRepository municipioRepo;
    @Mock private SeccionRepository seccionRepo;
    @Mock private CasillaRepository casillaRepo;
    @Mock private ActaRepository actaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;

    private CatalogoGeograficoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CatalogoGeograficoServiceImpl(distritoRepo, municipioRepo, seccionRepo, casillaRepo, actaRepo, usuarioRepo, logRepo);
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(Usuario.builder().id(ADMIN_ID).username("admin").build()));
    }

    // ── Distrito ─────────────────────────────────────────────────────────────

    @Test
    void crearDistrito_normalizaClaveAMayusculas() {
        when(distritoRepo.existsByClave("D01")).thenReturn(false);

        DistritoResponse response = service.crearDistrito(distritoRequest(" d01 ", "Distrito Uno", null), ADMIN_ID);

        assertThat(response.getClave()).isEqualTo("D01");
    }

    @Test
    void crearDistrito_claveDuplicada_lanzaConflict() {
        when(distritoRepo.existsByClave("D01")).thenReturn(true);

        assertThatThrownBy(() -> service.crearDistrito(distritoRequest("D01", "Distrito Uno", null), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(distritoRepo, never()).save(any());
    }

    @Test
    void eliminarDistrito_sinSecciones_seElimina() {
        when(distritoRepo.existsById(2L)).thenReturn(true);
        when(seccionRepo.existsByDistritoId(2L)).thenReturn(false);

        service.eliminarDistrito(2L, ADMIN_ID);

        verify(distritoRepo).deleteById(2L);
    }

    @Test
    void eliminarDistrito_conSecciones_lanzaConflictYNoElimina() {
        when(distritoRepo.existsById(2L)).thenReturn(true);
        when(seccionRepo.existsByDistritoId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarDistrito(2L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(distritoRepo, never()).deleteById(any());
    }

    @Test
    void eliminarDistrito_inexistente_lanzaNotFound() {
        when(distritoRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarDistrito(99L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Municipio ────────────────────────────────────────────────────────────

    @Test
    void crearMunicipio_normalizaClaveAMayusculas() {
        when(municipioRepo.existsByClave("MUN001")).thenReturn(false);

        MunicipioResponse response = service.crearMunicipio(municipioRequest(" mun001 ", "Cuernavaca"), ADMIN_ID);

        assertThat(response.getClave()).isEqualTo("MUN001");
    }

    @Test
    void crearMunicipio_claveDuplicada_lanzaConflict() {
        when(municipioRepo.existsByClave("MUN001")).thenReturn(true);

        assertThatThrownBy(() -> service.crearMunicipio(municipioRequest("MUN001", "Cuernavaca"), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(municipioRepo, never()).save(any());
    }

    @Test
    void eliminarMunicipio_sinSecciones_seElimina() {
        when(municipioRepo.existsById(1L)).thenReturn(true);
        when(seccionRepo.existsByMunicipioId(1L)).thenReturn(false);

        service.eliminarMunicipio(1L, ADMIN_ID);

        verify(municipioRepo).deleteById(1L);
    }

    @Test
    void eliminarMunicipio_conSecciones_lanzaConflictYNoElimina() {
        when(municipioRepo.existsById(1L)).thenReturn(true);
        when(seccionRepo.existsByMunicipioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarMunicipio(1L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(municipioRepo, never()).deleteById(any());
    }

    // ── Sección ──────────────────────────────────────────────────────────────

    @Test
    void crearSeccion_numeroDuplicado_lanzaConflict() {
        when(seccionRepo.existsByNumeroSeccion(1007)).thenReturn(true);

        assertThatThrownBy(() -> service.crearSeccion(seccionRequest(1007, 1L, 1L), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(seccionRepo, never()).save(any());
    }

    @Test
    void crearSeccion_municipioInexistente_lanzaNotFound() {
        when(seccionRepo.existsByNumeroSeccion(1007)).thenReturn(false);
        when(municipioRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSeccion(seccionRequest(1007, 1L, 1L), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crearSeccion_distritoInexistente_lanzaNotFound() {
        when(seccionRepo.existsByNumeroSeccion(1007)).thenReturn(false);
        when(municipioRepo.findById(1L)).thenReturn(Optional.of(Municipio.builder().id(1L).clave("MUN001").nombre("Cuernavaca").build()));
        when(distritoRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSeccion(seccionRequest(1007, 1L, 1L), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crearSeccion_exitoUneMunicipioYDistritoElegidos() {
        when(seccionRepo.existsByNumeroSeccion(1007)).thenReturn(false);
        when(municipioRepo.findById(1L)).thenReturn(Optional.of(Municipio.builder().id(1L).clave("MUN001").nombre("Cuernavaca").build()));
        when(distritoRepo.findById(2L)).thenReturn(Optional.of(Distrito.builder().id(2L).clave("D01").nombre("Distrito Uno").build()));

        SeccionResponse response = service.crearSeccion(seccionRequest(1007, 1L, 2L), ADMIN_ID);

        assertThat(response.getNumeroSeccion()).isEqualTo(1007);
        assertThat(response.getMunicipioNombre()).isEqualTo("Cuernavaca");
        assertThat(response.getDistritoNombre()).isEqualTo("Distrito Uno");
    }

    @Test
    void listarSecciones_filtraPorMunicipioCuandoSeProporciona() {
        when(seccionRepo.findByMunicipioId(1L)).thenReturn(List.of(seccion(1007)));

        List<SeccionResponse> resultado = service.listarSecciones(1L, 9L);

        assertThat(resultado).hasSize(1);
        verify(seccionRepo, never()).findByDistritoId(any());
        verify(seccionRepo, never()).findAll();
    }

    @Test
    void listarSecciones_filtraPorDistritoCuandoNoHayMunicipio() {
        when(seccionRepo.findByDistritoId(2L)).thenReturn(List.of(seccion(1007)));

        List<SeccionResponse> resultado = service.listarSecciones(null, 2L);

        assertThat(resultado).hasSize(1);
        verify(seccionRepo, never()).findAll();
    }

    @Test
    void listarSecciones_devuelveTodasSinFiltros() {
        when(seccionRepo.findAll()).thenReturn(List.of(seccion(1007), seccion(1008)));

        List<SeccionResponse> resultado = service.listarSecciones(null, null);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void eliminarSeccion_sinCasillas_seElimina() {
        when(seccionRepo.existsById(1L)).thenReturn(true);
        when(casillaRepo.existsBySeccionId(1L)).thenReturn(false);

        service.eliminarSeccion(1L, ADMIN_ID);

        verify(seccionRepo).deleteById(1L);
    }

    @Test
    void eliminarSeccion_conCasillas_lanzaConflictYNoElimina() {
        when(seccionRepo.existsById(1L)).thenReturn(true);
        when(casillaRepo.existsBySeccionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarSeccion(1L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(seccionRepo, never()).deleteById(any());
    }

    // ── Casilla ──────────────────────────────────────────────────────────────

    @Test
    void crearCasilla_seccionInexistente_lanzaNotFound() {
        when(seccionRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearCasilla(casillaRequest(1L, TipoCasilla.BASICA, 1, 750), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(casillaRepo, never()).save(any());
    }

    @Test
    void crearCasilla_claveDuplicada_lanzaConflict() {
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion(1007)));
        when(casillaRepo.existsBySeccionIdAndTipoAndNumeroCasilla(1L, TipoCasilla.BASICA, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.crearCasilla(casillaRequest(1L, TipoCasilla.BASICA, 1, 750), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(casillaRepo, never()).save(any());
    }

    @Test
    void crearCasilla_exitoQuedaActivaPorDefecto() {
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion(1007)));
        when(casillaRepo.existsBySeccionIdAndTipoAndNumeroCasilla(1L, TipoCasilla.BASICA, 1)).thenReturn(false);

        CasillaResponse response = service.crearCasilla(casillaRequest(1L, TipoCasilla.BASICA, 1, 750), ADMIN_ID);

        assertThat(response.getTipo()).isEqualTo(TipoCasilla.BASICA);
        assertThat(response.getListaNominal()).isEqualTo(750);
        ArgumentCaptor<Casilla> captor = ArgumentCaptor.forClass(Casilla.class);
        verify(casillaRepo).save(captor.capture());
        assertThat(captor.getValue().getActiva()).isTrue();
    }

    @Test
    void eliminarCasilla_sinActa_seElimina() {
        when(casillaRepo.existsById(5L)).thenReturn(true);
        when(actaRepo.existsByCasillaId(5L)).thenReturn(false);

        service.eliminarCasilla(5L, ADMIN_ID);

        verify(casillaRepo).deleteById(5L);
    }

    @Test
    void eliminarCasilla_conActaDigitalizada_lanzaConflictYNoElimina() {
        when(casillaRepo.existsById(5L)).thenReturn(true);
        when(actaRepo.existsByCasillaId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarCasilla(5L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(casillaRepo, never()).deleteById(any());
    }

    @Test
    void eliminarCasilla_inexistente_lanzaNotFound() {
        when(casillaRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarCasilla(99L, ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Seccion seccion(int numero) {
        Municipio municipio = Municipio.builder().id(1L).clave("MUN001").nombre("Cuernavaca").build();
        Distrito distrito = Distrito.builder().id(2L).clave("D01").nombre("Distrito Uno").build();
        return Seccion.builder().id(1L).numeroSeccion(numero).municipio(municipio).distrito(distrito).build();
    }

    private CrearDistritoRequest distritoRequest(String clave, String nombre, String cabecera) {
        CrearDistritoRequest req = new CrearDistritoRequest();
        req.setClave(clave);
        req.setNombre(nombre);
        req.setCabeceraDistrital(cabecera);
        return req;
    }

    private CrearMunicipioRequest municipioRequest(String clave, String nombre) {
        CrearMunicipioRequest req = new CrearMunicipioRequest();
        req.setClave(clave);
        req.setNombre(nombre);
        return req;
    }

    private CrearSeccionRequest seccionRequest(int numero, Long municipioId, Long distritoId) {
        CrearSeccionRequest req = new CrearSeccionRequest();
        req.setNumeroSeccion(numero);
        req.setMunicipioId(municipioId);
        req.setDistritoId(distritoId);
        return req;
    }

    private CrearCasillaRequest casillaRequest(Long seccionId, TipoCasilla tipo, int numero, int listaNominal) {
        CrearCasillaRequest req = new CrearCasillaRequest();
        req.setSeccionId(seccionId);
        req.setTipo(tipo);
        req.setNumeroCasilla(numero);
        req.setListaNominal(listaNominal);
        return req;
    }
}
