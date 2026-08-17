package mx.gob.impepac.redicap.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.CapturaActa;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.RolUsuario;
import mx.gob.impepac.redicap.dto.request.CapturaRequest;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.dto.response.ImagenActaResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CapturaActaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la máquina de estados del doble ciego (DFR R2):
 *   C1 guardada            → EN_CAPTURA_2
 *   C2 == C1               → VALIDADA
 *   C2 != C1               → EN_CAPTURA_3
 *   C3 == C1 o C3 == C2    → VALIDADA
 *   C3 != C1 y C3 != C2    → MESA_DELIBERACION
 */
@ExtendWith(MockitoExtension.class)
class CapturaServiceImplTest {

    private static final Long ACTA_ID = 1L;
    private static final Long USUARIO_ID = 10L;

    @Mock private ActaRepository actaRepo;
    @Mock private CapturaActaRepository capturaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;

    private CapturaServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new CapturaServiceImpl(actaRepo, capturaRepo, usuarioRepo, logRepo, new ObjectMapper());
        ReflectionTestUtils.setField(service, "actasPath", tempDir.toString());
    }

    // ── Máquina de estados ──────────────────────────────────────────────────

    @Test
    void primeraCaptura_avanzaAEnCaptura2() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(0);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of());

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.EN_CAPTURA_2);
        verify(capturaRepo).save(argCapturaConNumero(1));
    }

    @Test
    void segundaCapturaCoincideConPrimera_validaElActa() {
        prepararActa(EstadoActa.EN_CAPTURA_2, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(1);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10,\"MORENA\":5}"),
                captura(2, "{\"PAN\":10,\"MORENA\":5}")));

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.VALIDADA);
    }

    @Test
    void segundaCapturaDifiereDePrimera_pasaATerceraCaptura() {
        prepararActa(EstadoActa.EN_CAPTURA_2, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(1);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10,\"MORENA\":5}"),
                captura(2, "{\"PAN\":10,\"MORENA\":9}")));

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 9), 19));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.EN_CAPTURA_3);
    }

    @Test
    void terceraCapturaCoincideConPrimera_validaElActa() {
        prepararActa(EstadoActa.EN_CAPTURA_3, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(2);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10,\"MORENA\":5}"),
                captura(2, "{\"PAN\":10,\"MORENA\":9}"),
                captura(3, "{\"PAN\":10,\"MORENA\":5}")));

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.VALIDADA);
    }

    @Test
    void terceraCapturaCoincideConSegunda_validaElActa() {
        prepararActa(EstadoActa.EN_CAPTURA_3, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(2);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10,\"MORENA\":5}"),
                captura(2, "{\"PAN\":10,\"MORENA\":9}"),
                captura(3, "{\"PAN\":10,\"MORENA\":9}")));

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 9), 19));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.VALIDADA);
    }

    @Test
    void terceraCapturaNoCoincideConNinguna_vaAMesaDeliberacion() {
        prepararActa(EstadoActa.EN_CAPTURA_3, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(2);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of(
                captura(1, "{\"PAN\":10,\"MORENA\":5}"),
                captura(2, "{\"PAN\":10,\"MORENA\":9}"),
                captura(3, "{\"PAN\":10,\"MORENA\":7}")));

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 7), 17));

        assertThat(response.getEstado()).isEqualTo(EstadoActa.MESA_DELIBERACION);
    }

    // ── Reglas de negocio ────────────────────────────────────────────────────

    @Test
    void usuarioYaCapturoEstaActa_lanzaForbidden() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(1000));
        prepararUsuario();
        when(capturaRepo.existsByActaIdAndCapturistaId(ACTA_ID, USUARIO_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15)))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(capturaRepo, never()).save(any());
    }

    @Test
    void actaConTresCapturas_rechazaUnaCuarta() {
        prepararActa(EstadoActa.MESA_DELIBERACION, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(3);

        assertThatThrownBy(() -> service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15)))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(capturaRepo, never()).save(any());
    }

    @Test
    void actaInexistente_lanzaNotFound() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15)))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void usuarioInexistente_lanzaNotFound() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(1000));
        when(usuarioRepo.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15)))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Integridad aritmética (DFR R7) ──────────────────────────────────────

    @Test
    void totalActaNoCoincideConSuma_marcaErrorAritmetico() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(0);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of());

        // suma real = 15, pero el acta física dice 20
        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 20));

        assertThat(response.getErrorAritmetico()).isTrue();
    }

    @Test
    void votosSumanIgualQueTotalActa_noMarcaErrorAritmetico() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(1000));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(0);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of());

        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15));

        assertThat(response.getErrorAritmetico()).isFalse();
    }

    @Test
    void votosExcedenListaNominal_marcaExcedente() {
        prepararActa(EstadoActa.EN_CAPTURA_1, casilla(12));
        prepararUsuario();
        when(capturaRepo.countByActaId(ACTA_ID)).thenReturn(0);
        when(capturaRepo.findByActaIdOrderByNumeroCapturaAsc(ACTA_ID)).thenReturn(List.of());

        // lista nominal = 12, suma de votos = 15
        ActaResponse response = service.registrarCaptura(ACTA_ID, USUARIO_ID, request(votos(10, 5), 15));

        assertThat(response.getExcedeListaNominal()).isTrue();
    }

    // ── obtenerSiguienteActa ─────────────────────────────────────────────────

    @Test
    void obtenerSiguienteActa_priorizaEnCaptura1() {
        Acta acta = acta(ACTA_ID, EstadoActa.EN_CAPTURA_1, casilla(1000));
        when(actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_1.name(), USUARIO_ID))
                .thenReturn(Optional.of(acta));

        ActaResponse response = service.obtenerSiguienteActa(USUARIO_ID);

        assertThat(response.getId()).isEqualTo(ACTA_ID);
        verify(actaRepo, never()).findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_2.name(), USUARIO_ID);
    }

    @Test
    void obtenerSiguienteActa_caeAEnCaptura2SiNoHayEnCaptura1() {
        Acta acta = acta(2L, EstadoActa.EN_CAPTURA_2, casilla(1000));
        when(actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_1.name(), USUARIO_ID))
                .thenReturn(Optional.empty());
        when(actaRepo.findSiguienteParaCaptura(EstadoActa.EN_CAPTURA_2.name(), USUARIO_ID))
                .thenReturn(Optional.of(acta));

        ActaResponse response = service.obtenerSiguienteActa(USUARIO_ID);

        assertThat(response.getId()).isEqualTo(2L);
    }

    @Test
    void obtenerSiguienteActa_sinActasDisponibles_lanzaNoContent() {
        when(actaRepo.findSiguienteParaCaptura(any(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSiguienteActa(USUARIO_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── Imagen de referencia ─────────────────────────────────────────────────

    @Test
    void obtenerImagen_devuelveContenidoYContentTypeCorrectos() throws Exception {
        byte[] bytesImagen = {1, 2, 3, 4};
        Files.createDirectories(tempDir.resolve("100"));
        Files.write(tempDir.resolve("100/foto.jpg"), bytesImagen);
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(
                Acta.builder().id(ACTA_ID).rutaImagen("100/foto.jpg").build()));

        ImagenActaResponse imagen = service.obtenerImagen(ACTA_ID);

        assertThat(imagen.contenido()).isEqualTo(bytesImagen);
        assertThat(imagen.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void obtenerImagen_actaSinImagen_lanzaNotFound() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(Acta.builder().id(ACTA_ID).rutaImagen(null).build()));

        assertThatThrownBy(() -> service.obtenerImagen(ACTA_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerImagen_actaInexistente_lanzaNotFound() {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerImagen(ACTA_ID))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private void prepararActa(EstadoActa estado, Casilla casilla) {
        when(actaRepo.findById(ACTA_ID)).thenReturn(Optional.of(acta(ACTA_ID, estado, casilla)));
    }

    private void prepararUsuario() {
        when(usuarioRepo.findById(USUARIO_ID)).thenReturn(Optional.of(
                Usuario.builder().id(USUARIO_ID).username("capturista").rol(RolUsuario.CAPTURISTA).build()));
    }

    private Acta acta(Long id, EstadoActa estado, Casilla casilla) {
        return Acta.builder().id(id).casilla(casilla).estado(estado).build();
    }

    private Casilla casilla(int listaNominal) {
        return Casilla.builder().id(1L).listaNominal(listaNominal).build();
    }

    private CapturaActa captura(int numero, String votosJson) {
        return CapturaActa.builder().numeroCaptura(numero).datosVotosJson(votosJson).build();
    }

    private Map<String, Integer> votos(int pan, int morena) {
        return Map.of("PAN", pan, "MORENA", morena);
    }

    private CapturaRequest request(Map<String, Integer> votos, int totalVotosActa) {
        CapturaRequest req = new CapturaRequest();
        req.setVotos(votos);
        req.setTotalVotosActa(totalVotosActa);
        return req;
    }

    private CapturaActa argCapturaConNumero(int numero) {
        return org.mockito.ArgumentMatchers.argThat(c -> c != null && c.getNumeroCaptura() == numero);
    }
}
