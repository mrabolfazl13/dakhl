package ir.dakhlokharj.backup

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String,
    val salt: String,
    val iv: String,
)

/** PBKDF2-SHA256 + AES-256-GCM encryption for backups. No keys are stored anywhere. */
object BackupCrypto {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val TAG_LENGTH = 128
    private val random = SecureRandom()

    fun encrypt(plaintext: String, password: CharArray): EncryptedPayload {
        require(password.isNotEmpty()) { "رمز عبور پشتیبان نمی‌تواند خالی باشد" }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return EncryptedPayload(
            ciphertext = encoder.encodeToString(ciphertext),
            salt = encoder.encodeToString(salt),
            iv = encoder.encodeToString(iv),
        )
    }

    fun decrypt(payload: EncryptedPayload, password: CharArray): String {
        val decoder = Base64.getDecoder()
        val key = deriveKey(password, decoder.decode(payload.salt))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, decoder.decode(payload.iv)))
        val plaintext = cipher.doFinal(decoder.decode(payload.ciphertext))
        return String(plaintext, Charsets.UTF_8)
    }

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(key, "AES")
    }
}
