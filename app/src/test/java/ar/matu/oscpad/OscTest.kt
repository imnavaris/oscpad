package ar.matu.oscpad

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OscTest {
    @Test
    fun encodeFloatWritesPaddedAddressTypeTagAndBigEndianFloat() {
        val bytes = Osc.encodeFloat("/fader", 0.5f)
        assertArrayEquals(
            byteArrayOf(
                0x2F, 0x66, 0x61, 0x64, 0x65, 0x72, 0x00, 0x00,
                0x2C, 0x66, 0x00, 0x00,
                0x3F, 0x00, 0x00, 0x00
            ),
            bytes
        )
    }

    @Test
    fun encodeFloatOnePointZeroEndsWithBigEndianFloat() {
        val bytes = Osc.encodeFloat("/button", 1.0f)
        assertArrayEquals(byteArrayOf(0x3F, 0x80.toByte(), 0x00, 0x00), bytes.takeLast(4).toByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeFloatRejectsAddressWithoutSlash() {
        Osc.encodeFloat("fader", 0.5f)
    }

    @Test
    fun throttleAllowsFirstThenBlocksWithinInterval() {
        val throttle = Throttle(100)
        assertTrue(throttle.shouldSend(0))
        assertFalse(throttle.shouldSend(50))
        assertTrue(throttle.shouldSend(100))
    }
}
