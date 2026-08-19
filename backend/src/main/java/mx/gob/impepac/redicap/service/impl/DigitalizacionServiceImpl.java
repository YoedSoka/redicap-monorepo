package mx.gob.impepac.redicap.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.domain.entity.Acta;
import mx.gob.impepac.redicap.domain.entity.Casilla;
import mx.gob.impepac.redicap.domain.entity.LogAuditoria;
import mx.gob.impepac.redicap.domain.entity.Usuario;
import mx.gob.impepac.redicap.domain.enums.EstadoActa;
import mx.gob.impepac.redicap.domain.enums.TipoCasilla;
import mx.gob.impepac.redicap.domain.enums.TipoEleccion;
import mx.gob.impepac.redicap.dto.response.ActaResponse;
import mx.gob.impepac.redicap.exception.RedicapException;
import mx.gob.impepac.redicap.repository.ActaRepository;
import mx.gob.impepac.redicap.repository.CasillaRepository;
import mx.gob.impepac.redicap.repository.LogAuditoriaRepository;
import mx.gob.impepac.redicap.repository.UsuarioRepository;
import mx.gob.impepac.redicap.service.DigitalizacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Recepción de imágenes de actas digitalizadas en campo (DFR R1). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DigitalizacionServiceImpl implements DigitalizacionService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");
    private static final Map<String, String> EXTENSION_POR_TIPO = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png");

    private final CasillaRepository casillaRepo;
    private final ActaRepository actaRepo;
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaRepository logRepo;

    @Value("${redicap.storage.actas-path}")
    private String actasPath;

    @Override
    public ActaResponse recibirActa(Long casillaId, TipoEleccion tipoEleccion, Long digitalizadorId,
                                     MultipartFile imagen, String hashSha256Cliente) {
        if (imagen == null || imagen.isEmpty()) {
            throw RedicapException.badRequest("La imagen del acta es obligatoria");
        }
        String tipo = imagen.getContentType();
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw RedicapException.badRequest("Formato de imagen no soportado: " + tipo);
        }

        Casilla casilla = casillaRepo.findById(casillaId)
                .orElseThrow(() -> RedicapException.notFound("Casilla", casillaId));
        Usuario digitalizador = usuarioRepo.findById(digitalizadorId)
                .orElseThrow(() -> RedicapException.notFound("Usuario", digitalizadorId));

        if (!Boolean.TRUE.equals(casilla.getActiva())) {
            throw RedicapException.badRequest("La casilla no está activa en el catálogo");
        }
        // Las casillas ESPECIAL solo reciben votos de Gubernatura y Diputación (DFR R5).
        if (casilla.getTipo() == TipoCasilla.ESPECIAL && tipoEleccion == TipoEleccion.AYUNTAMIENTO) {
            throw RedicapException.badRequest("Las casillas especiales no participan en la elección de Ayuntamiento");
        }
        if (actaRepo.findByCasillaIdAndTipoEleccion(casillaId, tipoEleccion).isPresent()) {
            throw RedicapException.conflict("Esta casilla ya tiene un acta digitalizada para esa elección");
        }

        byte[] contenido = leerBytes(imagen);
        String hashCalculado = sha256Hex(contenido);
        if (!hashCalculado.equalsIgnoreCase(hashSha256Cliente)) {
            throw RedicapException.badRequest("El hash SHA-256 no coincide con la imagen recibida");
        }

        String rutaRelativa = almacenar(casillaId, tipoEleccion, contenido, EXTENSION_POR_TIPO.get(tipo));

        Acta acta = Acta.builder()
                .casilla(casilla)
                .tipoEleccion(tipoEleccion)
                .rutaImagen(rutaRelativa)
                .hashSha256(hashCalculado)
                .estado(EstadoActa.EN_CAPTURA_1)
                .digitalizador(digitalizador)
                .build();
        actaRepo.save(acta);

        logRepo.save(LogAuditoria.builder()
                .usuario(digitalizador)
                .tipoAccion("ACTA_DIGITALIZADA")
                .modulo("DIGITALIZACION")
                .entidadAfectada("Acta#" + acta.getId())
                .build());

        log.info("Acta {} digitalizada para casilla {} ({}) por usuario {}",
                acta.getId(), casillaId, tipoEleccion, digitalizadorId);
        return ActaResponse.from(acta);
    }

    private byte[] leerBytes(MultipartFile imagen) {
        try {
            return imagen.getBytes();
        } catch (IOException e) {
            throw new RedicapException("Error al leer la imagen recibida", HttpStatus.BAD_REQUEST);
        }
    }

    private String sha256Hex(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new RedicapException("SHA-256 no disponible en este servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String almacenar(Long casillaId, TipoEleccion tipoEleccion, byte[] contenido, String extension) {
        try {
            Path directorio = Path.of(actasPath, casillaId.toString(), tipoEleccion.name());
            Files.createDirectories(directorio);
            String nombreArchivo = UUID.randomUUID() + "." + extension;
            Path archivo = directorio.resolve(nombreArchivo);
            Files.write(archivo, contenido);
            // Si la transacción hace rollback después de escribir el archivo (p. ej. el save falla),
            // no debe quedar un archivo huérfano sin Acta asociada.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(archivo);
                        } catch (IOException e) {
                            log.warn("No se pudo eliminar archivo huérfano {}", archivo, e);
                        }
                    }
                }
            });
            return casillaId + "/" + tipoEleccion.name() + "/" + nombreArchivo;
        } catch (IOException e) {
            throw new RedicapException("Error al almacenar la imagen", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
