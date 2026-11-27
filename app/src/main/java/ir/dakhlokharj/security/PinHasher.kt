package ir.dakhlokharj.security

import java.security.MessageDigest
import java.security.SecureRandom

/** Salted SHA-256 PIN storage. The plain PIN is never persisted. */
object PinHasher {

    private val random = SecureRandom()

    fun newSalt(): String = randomBytes(16).toHex()

    fun hash(pin: String, saltHex: String = newSalt()): String {
        require(pin.length >= 4) { "رمز باید حداقل ۴ رقم باشد" }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.hexToBytes())
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return "$saltHex:${digest.digest().toHex()}"
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val expected = parts[1]
        return runCatching {
            val actual = hash(pin, salt).substringAfter(":")
            MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
        }.getOrDefault(false)
    }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
