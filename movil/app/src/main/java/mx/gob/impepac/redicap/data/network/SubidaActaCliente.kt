package mx.gob.impepac.redicap.data.network

import mx.gob.impepac.redicap.data.model.ActaResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Construye el multipart y sube el acta a partir de bytes ya en memoria (descifrados). Se usa
 * tanto para el intento síncrono al confirmar en CapturaScreen como para el reintento del
 * worker offline — un solo lugar para no duplicar la construcción del request.
 */
suspend fun subirActaBytes(
    api: ApiService,
    casillaId: Long,
    hashSha256: String,
    bytes: ByteArray,
    nombreArchivo: String,
): ActaResponse {
    val casillaBody = casillaId.toString().toRequestBody("text/plain".toMediaType())
    val hashBody = hashSha256.toRequestBody("text/plain".toMediaType())
    val imagenPart = MultipartBody.Part.createFormData(
        "imagen", nombreArchivo, bytes.toRequestBody("image/jpeg".toMediaType())
    )
    return api.subirActa(casillaBody, hashBody, imagenPart)
}
