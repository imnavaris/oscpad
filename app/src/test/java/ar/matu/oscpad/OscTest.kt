package ar.matu.oscpad

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

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

    @Test
    fun encodeFloatsWritesTwoFloatsWithFfTag() {
        val bytes = Osc.encodeFloats("/xy", floatArrayOf(0.5f, 1.0f))
        assertArrayEquals(
            byteArrayOf(
                0x2F, 0x78, 0x79, 0x00,
                0x2C, 0x66, 0x66, 0x00,
                0x3F, 0x00, 0x00, 0x00,
                0x3F, 0x80.toByte(), 0x00, 0x00
            ),
            bytes
        )
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

    @Test
    fun encodeFloatsPadsThreeFloatTagsWithNull() {
        val bytes = Osc.encodeFloats("/ab", floatArrayOf(1f, 2f, 3f))
        assertEquals(24, bytes.size)
        assertArrayEquals(
            byteArrayOf(0x2F, 0x61, 0x62, 0x00, 0x2C, 0x66, 0x66, 0x66),
            bytes.copyOfRange(0, 8),
        )
    }

    @Test
    fun senderSkipsIdenticalDatagramsAndSendsDistinctOnes() {
        val listener = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        listener.soTimeout = 2000
        val sender = OscSender("127.0.0.1", listener.localPort)
        try {
            val first = Osc.encodeFloat("/fader", 0.5f)
            val second = Osc.encodeFloat("/fader", 0.25f)
            assertTrue(sender.send(first))
            assertTrue(sender.send(first))
            assertArrayEquals(first, receive(listener))
            listener.soTimeout = 200
            try {
                receive(listener)
                fail("identical datagram was sent twice")
            } catch (_: SocketTimeoutException) {
            }
            listener.soTimeout = 2000
            assertTrue(sender.send(second))
            assertArrayEquals(second, receive(listener))
        } finally {
            sender.close()
            listener.close()
        }
    }

    @Test
    fun senderReturnsFalseForUnresolvableHost() {
        val sender = OscSender("invalid.invalid", 9000)
        try {
            assertFalse(sender.send(Osc.encodeFloat("/fader", 1.0f)))
        } finally {
            sender.close()
        }
    }

    private fun receive(socket: DatagramSocket): ByteArray {
        val packet = DatagramPacket(ByteArray(1024), 1024)
        socket.receive(packet)
        return packet.data.copyOf(packet.length)
    }
}
