package com.lifeos.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * PIN storage. Only a salted PBKDF2 hash is persisted; the PIN itself never is.
 * Kept free of Android APIs so it can be unit tested on the JVM.
 */
object PinHasher {
    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, saltBase64: String, iterations: Int = ITERATIONS): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(key)
    }

    /** Constant-time comparison, so a wrong PIN cannot be probed by timing. */
    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        if (saltBase64.isEmpty() || expectedHash.isEmpty()) return false
        val actual = runCatching { hash(pin, saltBase64) }.getOrNull() ?: return false
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}

@Serializable
data class EncryptedEnvelope(
    val format: String = EncryptedBackup.FORMAT,
    val version: Int = EncryptedBackup.VERSION,
    val salt: String,
    val iv: String,
    val data: String
)

/**
 * Password protected backup file. Uses PBKDF2 to derive an AES-256-GCM key, so
 * the exported JSON cannot be read without the password. JVM testable.
 */
object EncryptedBackup {
    const val FORMAT = "lifeos-backup-enc"
    const val VERSION = 1
    private const val ITERATIONS = 100_000
    private const val TAG_BITS = 128

    // encodeDefaults keeps the format marker in the file, which is what
    // isEncrypted() looks for; the default Json instance omits it.
    private val envelopeJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun isEncrypted(raw: String): Boolean =
        raw.trimStart().startsWith("{") && raw.contains("\"$FORMAT\"")

    fun encrypt(plainJson: String, password: String): String {
        require(password.length >= 4) { "La password deve avere almeno 4 caratteri" }
        val salt = PinHasher.newSalt()
        val key = deriveKey(password, salt)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return envelopeJson.encodeToString(
            EncryptedEnvelope(
                salt = salt,
                iv = encoder.encodeToString(iv),
                data = encoder.encodeToString(encrypted)
            )
        )
    }

    fun decrypt(raw: String, password: String): Result<String> = runCatching {
        val envelope = envelopeJson.decodeFromString<EncryptedEnvelope>(raw)
        require(envelope.format == FORMAT) { "Il file non è un backup cifrato di LifeOS" }
        require(envelope.version <= VERSION) { "Backup creato con una versione più recente" }
        val decoder = Base64.getDecoder()
        val key = deriveKey(password, envelope.salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, decoder.decode(envelope.iv)))
        String(cipher.doFinal(decoder.decode(envelope.data)), Charsets.UTF_8)
    }.recoverCatching { error ->
        throw IllegalArgumentException(
            if (error is IllegalArgumentException && error.message?.contains("backup") == true) error.message!!
            else "Password errata o file danneggiato"
        )
    }

    private fun deriveKey(password: String, saltBase64: String): SecretKey {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(saltBase64),
            ITERATIONS,
            256
        )
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }
}

/**
 * Encrypts vault secrets with a non-exportable Android Keystore key. The clear
 * text is never written to DataStore, and the key cannot leave the device.
 */
class VaultCrypto(context: Context) {
    private val appContext = context.applicationContext

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Returns Base64 cipher text and Base64 IV. */
    fun encrypt(plain: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(encrypted) to encoder.encodeToString(cipher.iv)
    }

    /** Null when the key was invalidated or the payload was tampered with. */
    fun decrypt(cipherText: String, iv: String): String? = runCatching {
        val decoder = Base64.getDecoder()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, decoder.decode(iv)))
        String(cipher.doFinal(decoder.decode(cipherText)), Charsets.UTF_8)
    }.getOrNull()

    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(ALIAS)
        }
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "lifeos_vault_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
