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
    val level4: Float = 0.5f,
    val mask: Int = 15,
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

fun channelSortKey(name: String): Pair<String, Int> {
    val m = Regex("^(.*?)(\\d+)$").find(name.trim())
    return if (m != null) {
        Pair(m.groupValues[1], m.groupValues[2].toIntOrNull() ?: 0)
    } else {
        Pair(name, 0)
    }
}

fun Channel.compCount() = when (type) {
    "xy" -> 2
    "color" -> 3
    "sensor" -> 4
    else -> 1
}

fun Channel.compLetter(i: Int) = when (type) {
    "xy" -> if (i == 0) "X" else "Y"
    "color" -> listOf("R", "G", "B").getOrElse(i) { "" }
    "sensor" -> listOf("X", "Y", "Z", "M").getOrElse(i) { "" }
    else -> ""
}

fun Channel.compEnabled(i: Int) = mask and (1 shl i) != 0

fun Channel.toggleComp(i: Int) = copy(mask = mask xor (1 shl i))

fun Channel.outputValues(): FloatArray {
    fun masked(i: Int, v: Float) = if (compEnabled(i)) v else 0f
    return when (type) {
        "xy" -> {
            val v = xyValues()
            floatArrayOf(masked(0, v[0]), masked(1, v[1]))
        }
        "color" -> floatArrayOf(masked(0, level), masked(1, level2), masked(2, level3))
        "sensor" -> floatArrayOf(masked(0, level), masked(1, level2), masked(2, level3), masked(3, level4))
        else -> floatArrayOf(current())
    }
}

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
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString("channels", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Channel>>(raw).map {
                val t = it.type
                it.copy(type = if (t == "button" || t == "xy" || t == "sensor" || t == "color") t else "fader")
            }
        } catch (e: Exception) {
            try {
                json.decodeFromString<List<Channel>>(
                    prefs.getString("channels.bak", null) ?: return emptyList(),
                )
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    fun save(context: Context, channels: List<Channel>) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs.getString("channels", null)?.let { prev ->
            prefs.edit().putString("channels.bak", prev).apply()
        }
        prefs.edit().putString("channels", json.encodeToString(channels)).apply()
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
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString("presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Preset>>(raw)
        } catch (e: Exception) {
            try {
                json.decodeFromString<List<Preset>>(
                    prefs.getString("presets.bak", null) ?: return emptyList(),
                )
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    fun save(context: Context, presets: List<Preset>) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs.getString("presets", null)?.let { prev ->
            prefs.edit().putString("presets.bak", prev).apply()
        }
        prefs.edit().putString("presets", json.encodeToString(presets)).apply()
    }

    fun nextPresetId(context: Context): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val id = prefs.getLong("preset_next_id", 1)
        prefs.edit().putLong("preset_next_id", id + 1).apply()
        return id
    }
}
