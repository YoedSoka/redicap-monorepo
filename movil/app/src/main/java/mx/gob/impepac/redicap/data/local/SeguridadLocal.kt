package mx.gob.impepac.redicap.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom

private const val PREFS_NAME = "redicap_seguridad"
private const val CLAVE_PASSPHRASE = "db_passphrase"

/**
 * Cifrado en reposo para el modo offline (DFR R1): la passphrase de SQLCipher y las fotos
 * de actas pendientes de subir se protegen con Android Keystore (vía Jetpack Security).
 */
object SeguridadLocal {

    private fun masterKey(context: Context) =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    /** Genera una passphrase aleatoria la primera vez que se necesita y la reutiliza después. */
    fun obtenerPassphrase(context: Context): ByteArray {
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.getString(CLAVE_PASSPHRASE, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }

        val nueva = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(CLAVE_PASSPHRASE, Base64.encodeToString(nueva, Base64.NO_WRAP)).apply()
        return nueva
    }

    /** Escribe [bytes] cifrados en [destino] (lo reemplaza si ya existía). */
    fun escribirCifrado(context: Context, destino: File, bytes: ByteArray) {
        if (destino.exists()) destino.delete()
        encryptedFile(context, destino).openFileOutput().use { it.write(bytes) }
    }

    /** Descifra y devuelve el contenido completo de [origen]. */
    fun leerCifrado(context: Context, origen: File): ByteArray =
        encryptedFile(context, origen).openFileInput().use { it.readBytes() }

    private fun encryptedFile(context: Context, archivo: File): EncryptedFile =
        EncryptedFile.Builder(
            context,
            archivo,
            masterKey(context),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
}
