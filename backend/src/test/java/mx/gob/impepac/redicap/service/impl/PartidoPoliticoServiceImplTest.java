package mx.gob.impepac.redicap.service.impl;

import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.PartidoPolitico;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.request.ActualizarPartidoRequest;
import mx.gob.impepac.redicap.dto.request.CrearPartidoRequest;
import mx.gob.impepac.redicap.dto.response.PartidoPoliticoResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.PartidoPoliticoRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre el catálogo de partidos políticos (DFR R6): alta con normalización, edición y activar/desactivar. */
@ExtendWith(MockitoExtension.class)
class PartidoPoliticoServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long PARTIDO_ID = 5L;

    @Mock private PartidoPoliticoRepository partidoRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;

    private PartidoPoliticoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PartidoPoliticoServiceImpl(partidoRepo, usuarioRepo, logRepo);
    }

    // ── crear ────────────────────────────────────────────────────────────────

    @Test
    void crear_normalizaSiglasAMayusculasYLasRecorta() {
        when(partidoRepo.existsBySiglas("MORENA")).thenReturn(false);
        prepararAdmin();

        PartidoPoliticoResponse response = service.crear(request("  morena ", "Movimiento Regeneración Nacional", null), ADMIN_ID);

        assertThat(response.getSiglas()).isEqualTo("MORENA");
        ArgumentCaptor<PartidoPolitico> captor = ArgumentCaptor.forClass(PartidoPolitico.class);
        verify(partidoRepo).save(captor.capture());
        assertThat(captor.getValue().getSiglas()).isEqualTo("MORENA");
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    void crear_siglasDuplicadas_lanzaConflict() {
        when(partidoRepo.existsBySiglas("PAN")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request("PAN", "Partido Acción Nacional", null), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(partidoRepo, never()).save(any());
    }

    @Test
    void crear_registraAuditoriaConAdminYTipoAccion() {
        when(partidoRepo.existsBySiglas("PAN")).thenReturn(false);
        prepararAdmin();

        service.crear(request("PAN", "Partido Acción Nacional", "#0047AB"), ADMIN_ID);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        assertThat(captor.getValue().getTipoAccion()).isEqualTo("PARTIDO_CREADO");
        assertThat(captor.getValue().getModulo()).isEqualTo("ADMINISTRACION");
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(ADMIN_ID);
    }

    // ── actualizar ───────────────────────────────────────────────────────────

    @Test
    void actualizar_modificaNombreYColor() {
        PartidoPolitico existente = PartidoPolitico.builder()
                .id(PARTIDO_ID).siglas("PAN").nombre("Viejo nombre").colorHex("#000000").activo(true).build();
        when(partidoRepo.findById(PARTIDO_ID)).thenReturn(Optional.of(existente));
        prepararAdmin();

        ActualizarPartidoRequest req = new ActualizarPartidoRequest();
        req.setNombre("Partido Acción Nacional");
        req.setColorHex("#0047AB");

        PartidoPoliticoResponse response = service.actualizar(PARTIDO_ID, req, ADMIN_ID);

        assertThat(response.getNombre()).isEqualTo("Partido Acción Nacional");
        assertThat(response.getColorHex()).isEqualTo("#0047AB");
        assertThat(response.getSiglas()).isEqualTo("PAN"); // las siglas no cambian
    }

    @Test
    void actualizar_partidoInexistente_lanzaNotFound() {
        when(partidoRepo.findById(PARTIDO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(PARTIDO_ID, new ActualizarPartidoRequest(), ADMIN_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── cambiarActivo ────────────────────────────────────────────────────────

    @Test
    void cambiarActivo_desactivaYRegistraAuditoriaCorrespondiente() {
        PartidoPolitico existente = PartidoPolitico.builder()
                .id(PARTIDO_ID).siglas("PAN").nombre("PAN").activo(true).build();
        when(partidoRepo.findById(PARTIDO_ID)).thenReturn(Optional.of(existente));
        prepararAdmin();

        PartidoPoliticoResponse response = service.cambiarActivo(PARTIDO_ID, false, ADMIN_ID);

        assertThat(response.getActivo()).isFalse();
        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        assertThat(captor.getValue().getTipoAccion()).isEqualTo("PARTIDO_DESACTIVADO");
    }

    @Test
    void cambiarActivo_reactivaYRegistraAuditoriaCorrespondiente() {
        PartidoPolitico existente = PartidoPolitico.builder()
                .id(PARTIDO_ID).siglas("PAN").nombre("PAN").activo(false).build();
        when(partidoRepo.findById(PARTIDO_ID)).thenReturn(Optional.of(existente));
        prepararAdmin();

        PartidoPoliticoResponse response = service.cambiarActivo(PARTIDO_ID, true, ADMIN_ID);

        assertThat(response.getActivo()).isTrue();
        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        assertThat(captor.getValue().getTipoAccion()).isEqualTo("PARTIDO_ACTIVADO");
    }

    // ── listar ───────────────────────────────────────────────────────────────

    @Test
    void listar_devuelveOrdenadoPorSiglasDesdeElRepositorio() {
        when(partidoRepo.findAllByOrderBySiglasAsc()).thenReturn(List.of(
                PartidoPolitico.builder().id(1L).siglas("MORENA").nombre("Morena").activo(true).build(),
                PartidoPolitico.builder().id(2L).siglas("PAN").nombre("PAN").activo(true).build()));

        List<PartidoPoliticoResponse> resultado = service.listar();

        assertThat(resultado).extracting(PartidoPoliticoResponse::getSiglas).containsExactly("MORENA", "PAN");
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private void prepararAdmin() {
        when(usuarioRepo.findById(eq(ADMIN_ID))).thenReturn(Optional.of(
                Usuario.builder().id(ADMIN_ID).username("admin").build()));
    }

    private CrearPartidoRequest request(String siglas, String nombre, String colorHex) {
        CrearPartidoRequest req = new CrearPartidoRequest();
        req.setSiglas(siglas);
        req.setNombre(nombre);
        req.setColorHex(colorHex);
        return req;
    }
}
