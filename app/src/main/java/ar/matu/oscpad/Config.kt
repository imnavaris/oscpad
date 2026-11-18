package ar.matu.oscpad

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetConfig(
    val host: String = "192.168.0.112",
    val port: Int = 8000,
)

object NetStore {
    private const val FILE = "oscpad"

    fun load(context: Context): NetConfig {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return NetConfig(
            host = prefs.getString("host", NetConfig().host) ?: NetConfig().host,
            port = prefs.getInt("port", NetConfig().port),
        )
    }

    fun save(context: Context, net: NetConfig) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString("host", net.host)
            .putInt("port", net.port)
            .apply()
    }
}

@Serializable
data class Channel(
    val id: Long,
    val name: String,
    val type: String = "fader",
    val min: Float = 0f,
    val max: Float = 1f,
    val onValue: Float = 1f,
    val offValue: Float = 0f,
    val level: Float = 0.5f,
    val on: Boolean = false,
    val hold: Boolean = false,
    val level2: Float = 0.5f,
    val level3: Float = 0.5f,
    val min2: Float = 0f,
    val max2: Float = 1f,
    val sensorKind: String = "accelerometer",
    val sensorAxis: String = "x",
    val rateMs: Long = 50,
)

@Serializable
data class Preset(
    val id: Long,
    val name: String,
    val channels: List<Channel>,
    val net: NetConfig,
)

private val json = Json { ignoreUnknownKeys = true }

fun Channel.xyValues(x: Float = level, y: Float = level2) = floatArrayOf(
    min + (max - min) * x,
    min2 + (max2 - min2) * y,
)

fun Channel.address() = "/$name"

fun Channel.current() = if (type == "button") {
    if (on) onValue else offValue
} else {
    min + (max - min) * level
}

fun localIpv4(): String? {
    return try {
        java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            .flatMap { java.util.Collections.list(it.inetAddresses) }
            .firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}

fun sameSubnet(a: String, b: String): Boolean {
    fun prefix(ip: String) = ip.trim().split(".").take(3)
    val pa = prefix(a)
    val pb = prefix(b)
    return pa.size == 3 && pa == pb
}
fun sanitizeName(raw: String): String? {
    val clean = raw.trim()
        .replace(Regex("\\s+"), "_")
        .filter { it.isLetterOrDigit() || it == '_' }
        .take(24)
    if (clean.isEmpty()) return null
    return clean
}

object ChannelStore {
    private const val FILE = "oscpad"

    fun load(context: Context): List<Channel> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString("channels", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Channel>>(raw).map {
                val t = it.type
                it.copy(type = if (t == "button" || t == "xy" || t == "sensor" || t == "color") t else "fader")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, channels: List<Channel>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString("channels", json.encodeToString(channels))
            .apply()
    }

    fun nextId(context: Context): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val id = prefs.getLong("next_id", 1)
        prefs.edit().putLong("next_id", id + 1).apply()
        return id
    }
}

object PresetStore {
    private const val FILE = "oscpad"

    fun load(context: Context): List<Preset> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString("presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Preset>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, presets: List<Preset>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString("presets", json.encodeToString(presets))
            .apply()
    }

    fun nextPresetId(context: Context): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val id = prefs.getLong("preset_next_id", 1)
        prefs.edit().putLong("preset_next_id", id + 1).apply()
        return id
    }
}
