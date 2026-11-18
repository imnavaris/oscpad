package ar.matu.oscpad

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Osc {
    fun encodeFloat(address: String, value: Float): ByteArray {
        require(address.startsWith('/')) { "OSC address must start with '/': $address" }
        val addressBytes = address.toByteArray(Charsets.US_ASCII)
        val padded = ((addressBytes.size + 1 + 3) / 4) * 4
        val buffer = ByteBuffer.allocate(padded + 4 + 4).order(ByteOrder.BIG_ENDIAN)
        buffer.put(addressBytes)
        buffer.put(ByteArray(padded - addressBytes.size))
        buffer.put(byteArrayOf(0x2C, 0x66, 0x00, 0x00))
        buffer.putFloat(value)
        return buffer.array()
    }
}

class OscSender(private val host: String, private val port: Int) {
    private val socket = DatagramSocket()

    fun send(bytes: ByteArray): Boolean = try {
        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port))
        true
    } catch (e: IOException) {
        false
    }

    fun close() {
        socket.close()
    }
}

class Throttle(private val minIntervalMs: Long = 16) {
    private var lastMs: Long? = null

    fun shouldSend(nowMs: Long = System.currentTimeMillis()): Boolean {
        val last = lastMs
        if (last != null && nowMs - last < minIntervalMs) return false
        lastMs = nowMs
        return true
    }
}
