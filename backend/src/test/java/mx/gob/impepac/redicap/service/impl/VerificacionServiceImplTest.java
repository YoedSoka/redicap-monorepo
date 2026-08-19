package mx.gob.impepac.redicap.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.CapturaActa;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.MotivoDictamenVerificador;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.VerificacionDetalleResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CapturaActaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mesa de deliberación (DFR R3): elegir cuál de las 3 capturas coincide con el acta física,
 * o declararla ilegible. Ambas resoluciones exigen motivo de catálogo + justificación,
 * y quedan registradas en el log de auditoría (DFR R3 "Justificación Obligatoria" / "Trazabilidad de Dictamen").
 */
@ExtendWith(MockitoExtension.class)
class VerificacionServiceImplTest {

    private static final Long ACTA_ID = 1L;
    private static final Long VERIFICADOR_ID = 20L;

    @Mock private ActaRepository actaRepo;
    @Mock private CapturaActaRepository capturaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;

    private VerificacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VerificacionServiceImpl(actaRepo, capturaRepo, usuarioRepo, logRepo, new ObjectMapper());
    }

    // ── listarPendientes / obtenerDetalle ───────────────────────────────────

    @Test
    void listarPendientes_devuelveSoloActasEnMesaDeliberacion() {
        when(actaRepo.findByEstado(EstadoActa.MESA_DELIBERACION))
                .thenReturn(List.of(acta(ACTA_ID, EstadoActa.MESA_DELIBERACION)));

        List<ActaResponse> pendientes = service.listarPendientes();

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).getId()).isEqualTo(ACTA_ID);
    }

    @Test
    void obtenerDetalle_incluyeLasTresCapturas() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(acta(ACTA_ID, EstadoActa.MESA_DELIBERACION)));
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10}"), captura(2, "{\"PAN\":11}"), captura(3, "{\"PAN\":12}")));

        VerificacionDetalleResponse detalle = service.obtenerDetalle(ACTA_ID);

        assertThat(detalle.getCapturas()).hasSize(3);
    }

    // ── validar ──────────────────────────────────────────────────────────────

    @Test
    void validar_actaEnMesaDeliberacion_quedaValidadaPorVerificador() {
        prepararActaEnDeliberacion();
        prepararVerificador();
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10}"), captura(2, "{\"PAN\":11}"), captura(3, "{\"PAN\":12}")));

        ActaResponse response = service.validar(ACTA_ID, VERIFICADOR_ID, 2,
                MotivoDictamenVerificador.COINCIDENCIA_CLARA_CON_ACTA_FISICA, "Coincide con la foto.");

        assertThat(response.getEstado()).isEqualTo(EstadoActa.VALIDADA_VERIFICADOR);
    }

    @Test
    void validar_registraMotivoYJustificacionEnAuditoria() {
        prepararActaEnDeliberacion();
        prepararVerificador();
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10}"), captura(2, "{\"PAN\":11}")));

        service.validar(ACTA_ID, VERIFICADOR_ID, 1,
                MotivoDictamenVerificador.ERROR_DE_CAPTURA_EVIDENTE, "Las otras dos tienen un dígito de más.");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getTipoAccion()).isEqualTo("ACTA_VALIDADA_VERIFICADOR");
        assertThat(log.getValorDespues()).contains("ERROR_DE_CAPTURA_EVIDENTE");
        assertThat(log.getValorDespues()).contains("Las otras dos tienen un dígito de más.");
    }

    @Test
    void validar_numeroCapturaInexistente_lanzaBadRequest() {
        prepararActaEnDeliberacion();
        prepararVerificador();
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(captura(1, "{\"PAN\":10}")));

        assertThatThrownBy(() -> service.validar(ACTA_ID, VERIFICADOR_ID, 3,
                MotivoDictamenVerificador.OTRO, "justificación"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validar_actaNoEstaEnMesaDeliberacion_lanzaConflict() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(acta(ACTA_ID, EstadoActa.EN_CAPTURA_2)));

        assertThatThrownBy(() -> service.validar(ACTA_ID, VERIFICADOR_ID, 1,
                MotivoDictamenVerificador.OTRO, "justificación"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // ── marcarIlegible ───────────────────────────────────────────────────────

    @Test
    void marcarIlegible_actaEnMesaDeliberacion_quedaIlegible() {
        prepararActaEnDeliberacion();
        prepararVerificador();

        ActaResponse response = service.marcarIlegible(ACTA_ID, VERIFICADOR_ID,
                MotivoDictamenVerificador.IMAGEN_BORROSA_O_MOVIDA, "No se distinguen los números.");

        assertThat(response.getEstado()).isEqualTo(EstadoActa.ILEGIBLE);
    }

    @Test
    void marcarIlegible_registraMotivoYJustificacionEnAuditoria() {
        prepararActaEnDeliberacion();
        prepararVerificador();

        service.marcarIlegible(ACTA_ID, VERIFICADOR_ID,
                MotivoDictamenVerificador.OBSTRUCCION_O_DANO_FISICO, "El papel está roto en la sección de votos.");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getTipoAccion()).isEqualTo("ACTA_ILEGIBLE");
        assertThat(log.getValorDespues()).contains("OBSTRUCCION_O_DANO_FISICO");
        assertThat(log.getValorDespues()).contains("El papel está roto en la sección de votos.");
    }

    @Test
    void marcarIlegible_actaNoEstaEnMesaDeliberacion_lanzaConflict() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(acta(ACTA_ID, EstadoActa.VALIDADA)));

        assertThatThrownBy(() -> service.marcarIlegible(ACTA_ID, VERIFICADOR_ID,
                MotivoDictamenVerificador.OTRO, "justificación"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void marcarIlegible_actaInexistente_lanzaNotFound() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarIlegible(ACTA_ID, VERIFICADOR_ID,
                MotivoDictamenVerificador.OTRO, "justificación"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private void prepararActaEnDeliberacion() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(acta(ACTA_ID, EstadoActa.MESA_DELIBERACION)));
    }

    private void prepararVerificador() {
        lenient().when(usuarioRepo.findById(VERIFICADOR_ID)).thenReturn(Optional.of(
                Usuario.builder().id(VERIFICADOR_ID).username("verificador").rol(RolUsuario.VERIFICADOR).build()));
    }

    private Acta acta(Long id, EstadoActa estado) {
        return Acta.builder().id(id).estado(estado)
                .casilla(Casilla.builder().id(1L).listaNominal(1000).build())
                .build();
    }

    private CapturaActa captura(int numero, String votosJson) {
        return CapturaActa.builder()
                .numeroCaptura(numero)
                .datosVotosJson(votosJson)
                .capturista(Usuario.builder().id(100L + numero).username("cap" + numero).rol(RolUsuario.CAPTURISTA).build())
                .build();
    }
}
