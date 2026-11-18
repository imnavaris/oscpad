package ar.matu.oscpad

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Osc {
    fun encodeFloat(address: String, value: Float): ByteArray =
        encodeFloats(address, floatArrayOf(value))

    fun encodeFloats(address: String, values: FloatArray): ByteArray {
        require(address.startsWith('/')) { "OSC address must start with '/': $address" }
        val addressBytes = address.toByteArray(Charsets.US_ASCII)
        val padded = ((addressBytes.size + 1 + 3) / 4) * 4
        val tags = ByteArray(values.size + 1) { if (it == 0) 0x2C else 0x66 }
        val tagPadded = ((tags.size + 3) / 4) * 4
        val buffer = ByteBuffer.allocate(padded + tagPadded + 4 * values.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(addressBytes)
        buffer.put(ByteArray(padded - addressBytes.size))
        buffer.put(tags)
        buffer.put(ByteArray(tagPadded - tags.size))
        values.forEach { buffer.putFloat(it) }
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
