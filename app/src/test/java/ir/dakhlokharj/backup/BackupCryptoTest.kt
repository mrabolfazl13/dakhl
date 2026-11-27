package ir.dakhlokharj.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun encryptDecryptRoundTrip() {
        val payload = BackupCrypto.encrypt("{\"version\":1}", "secret".toCharArray())
        assertThat(BackupCrypto.decrypt(payload, "secret".toCharArray())).isEqualTo("{\"version\":1}")
    }

    @Test
    fun wrongPasswordFailsToDecrypt() {
        val payload = BackupCrypto.encrypt("data", "secret".toCharArray())
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(payload, "wrong".toCharArray())
        }
    }

    @Test
    fun encryptionUsesFreshSaltAndIv() {
        val first = BackupCrypto.encrypt("data", "secret".toCharArray())
        val second = BackupCrypto.encrypt("data", "secret".toCharArray())
        assertThat(first.salt).isNotEqualTo(second.salt)
        assertThat(first.iv).isNotEqualTo(second.iv)
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
    }

    @Test
    fun emptyPasswordIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.encrypt("data", CharArray(0))
        }
    }

    @Test
    fun sha256IsDeterministic() {
        assertThat(BackupCrypto.sha256("abc")).isEqualTo(BackupCrypto.sha256("abc"))
        assertThat(BackupCrypto.sha256("abc")).hasLength(64)
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val payload = BackupCrypto.encrypt("payload", "secret".toCharArray())
        val tampered = payload.copy(ciphertext = payload.ciphertext.dropLast(2) + "AA")
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(tampered, "secret".toCharArray())
        }
    }
}
