package ar.matu.oscpad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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

data class Channel(
    val id: Long,
    val name: String,
    val type: String = "fader",
    val min: Float = 0f,
    val max: Float = 1f,
    val level: Float = 0.5f,
    val on: Boolean = false,
    val expanded: Boolean = true,
)

fun Channel.address() = "/$name"

fun Channel.current() = if (type == "button") {
    if (on) max else min
} else {
    min + (max - min) * level
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
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val type = o.optString("type", "fader")
                Channel(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    type = if (type == "button") "button" else "fader",
                    min = o.optDouble("min", 0.0).toFloat(),
                    max = o.optDouble("max", 1.0).toFloat(),
                    level = o.optDouble("level", 0.5).toFloat(),
                    on = o.optBoolean("on", false),
                    expanded = o.optBoolean("expanded", true),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, channels: List<Channel>) {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(
                JSONObject()
                    .put("id", ch.id)
                    .put("name", ch.name)
                    .put("type", ch.type)
                    .put("min", ch.min.toDouble())
                    .put("max", ch.max.toDouble())
                    .put("level", ch.level.toDouble())
                    .put("on", ch.on)
                    .put("expanded", ch.expanded),
            )
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString("channels", arr.toString())
            .apply()
    }

    fun nextId(context: Context): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val id = prefs.getLong("next_id", 1)
        prefs.edit().putLong("next_id", id + 1).apply()
        return id
    }
}
