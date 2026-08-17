package mx.gob.impepac.redicap.service.impl;

import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la recepción de actas digitalizadas (DFR R1), en particular el cambio de esta
 * sesión: ya no se exige que la casilla coincida con la asignada al digitalizador (solo
 * que esté activa), para permitir digitalizar cualquier casilla activa del catálogo.
 */
@ExtendWith(MockitoExtension.class)
class DigitalizacionServiceImplTest {

    private static final Long CASILLA_ID = 100L;
    private static final Long DIGITALIZADOR_ID = 10L;

    @Mock private CasillaRepository casillaRepo;
    @Mock private ActaRepository actaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private LogAuditoriaRepository logRepo;

    private DigitalizacionServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new DigitalizacionServiceImpl(casillaRepo, actaRepo, usuarioRepo, logRepo);
        ReflectionTestUtils.setField(service, "actasPath", tempDir.toString());
        lenient().when(usuarioRepo.findById(DIGITALIZADOR_ID)).thenReturn(Optional.of(
                Usuario.builder().id(DIGITALIZADOR_ID).username("digitalizador").build()));
        // almacenar() registra un TransactionSynchronization; en producción lo provee el
        // proxy @Transactional, aquí lo activamos a mano para poder probar esa rama.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void imagenVacia_lanzaBadRequest() {
        MockMultipartFile vacia = new MockMultipartFile("imagen", "acta.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, vacia, "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(actaRepo, never()).save(any());
    }

    @Test
    void tipoNoSoportado_lanzaBadRequest() {
        MockMultipartFile pdf = new MockMultipartFile("imagen", "acta.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, pdf, "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void casillaInexistente_lanzaNotFound() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagenValida(), "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void digitalizadorInexistente_lanzaNotFound() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(usuarioRepo.findById(DIGITALIZADOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagenValida(), "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── El cambio de esta sesión: casilla activa basta, ya no hace falta que sea la asignada ──

    @Test
    void casillaActivaPeroNoAsignadaAlDigitalizador_seAcepta() {
        // Antes de esta sesión esto lanzaba FORBIDDEN si la casilla no era la asignada al
        // digitalizador; ahora cualquier digitalizador puede subir a cualquier casilla activa.
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(actaRepo.findByCasillaId(CASILLA_ID)).thenReturn(Optional.empty());
        MockMultipartFile imagen = imagenValida();

        ActaResponse response = service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagen, hashDe(imagen));

        assertThat(response.getCasillaId()).isEqualTo(CASILLA_ID);
        verify(actaRepo).save(any());
    }

    @Test
    void casillaInactiva_lanzaBadRequest() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(false)));

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagenValida(), "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(actaRepo, never()).save(any());
    }

    // ── Resto de reglas de negocio ───────────────────────────────────────────

    @Test
    void casillaYaTieneActa_lanzaConflict() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(actaRepo.findByCasillaId(CASILLA_ID)).thenReturn(Optional.of(Acta.builder().id(1L).build()));

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagenValida(), "hash"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(actaRepo, never()).save(any());
    }

    @Test
    void hashNoCoincideConElContenido_lanzaBadRequest() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(actaRepo.findByCasillaId(CASILLA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagenValida(), "hash-incorrecto"))
                .isInstanceOf(RedicapException.class)
                .extracting(e -> ((RedicapException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(actaRepo, never()).save(any());
    }

    // ── Éxito: hash correcto, archivo almacenado, folio generado ────────────

    @Test
    void exito_almacenaElArchivoYGeneraFolioConElIdDelActa() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(actaRepo.findByCasillaId(CASILLA_ID)).thenReturn(Optional.empty());
        when(actaRepo.save(any())).thenAnswer(inv -> {
            Acta a = inv.getArgument(0);
            a.setId(123L);
            return a;
        });
        MockMultipartFile imagen = imagenValida();

        ActaResponse response = service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagen, hashDe(imagen));

        assertThat(response.getFolio()).isEqualTo("RDCP-00000123");

        ArgumentCaptor<Acta> captor = ArgumentCaptor.forClass(Acta.class);
        verify(actaRepo).save(captor.capture());
        String rutaGuardada = captor.getValue().getRutaImagen();
        assertThat(rutaGuardada).startsWith(CASILLA_ID + "/");
        assertThat(Files.exists(tempDir.resolve(rutaGuardada))).isTrue();
    }

    @Test
    void exito_registraAuditoriaDeDigitalizacion() {
        when(casillaRepo.findById(CASILLA_ID)).thenReturn(Optional.of(casilla(true)));
        when(actaRepo.findByCasillaId(CASILLA_ID)).thenReturn(Optional.empty());
        MockMultipartFile imagen = imagenValida();

        service.recibirActa(CASILLA_ID, DIGITALIZADOR_ID, imagen, hashDe(imagen));

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepo).save(captor.capture());
        assertThat(captor.getValue().getTipoAccion()).isEqualTo("ACTA_DIGITALIZADA");
        assertThat(captor.getValue().getModulo()).isEqualTo("DIGITALIZACION");
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Casilla casilla(boolean activa) {
        return Casilla.builder().id(CASILLA_ID).activa(activa).build();
    }

    private MockMultipartFile imagenValida() {
        return new MockMultipartFile("imagen", "acta.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5});
    }

    private String hashDe(MockMultipartFile archivo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(archivo.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
