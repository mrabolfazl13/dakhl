package ir.dakhlokharj.security

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class PinHasherTest {

    @Test
    fun correctPinVerifies() {
        val stored = PinHasher.hash("1234")
        assertThat(PinHasher.verify("1234", stored)).isTrue()
    }

    @Test
    fun wrongPinDoesNotVerify() {
        val stored = PinHasher.hash("1234")
        assertThat(PinHasher.verify("4321", stored)).isFalse()
    }

    @Test
    fun hashesUseDifferentSalts() {
        val first = PinHasher.hash("1234")
        val second = PinHasher.hash("1234")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun blankStoredHashIsRejected() {
        assertThat(PinHasher.verify("1234", null)).isFalse()
        assertThat(PinHasher.verify("1234", "")).isFalse()
    }

    @Test
    fun malformedStoredHashIsRejected() {
        assertThat(PinHasher.verify("1234", "not-a-hash")).isFalse()
        assertThat(PinHasher.verify("1234", "a:b:c")).isFalse()
        assertThat(PinHasher.verify("1234", "zz:zz")).isFalse()
    }

    @Test
    fun shortPinIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { PinHasher.hash("12") }
    }
}
