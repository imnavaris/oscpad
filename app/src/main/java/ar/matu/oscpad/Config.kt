package ar.matu.oscpad

import android.content.Context

data class Config(
    val host: String = "192.168.0.112",
    val port: Int = 8000,
    val addrFader: String = "/fader",
    val addrButton: String = "/button",
    val min: Float = 0f,
    val max: Float = 1f,
)

object ConfigStore {
    private const val FILE = "oscpad"

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val defaults = Config()
        return Config(
            host = prefs.getString("host", defaults.host) ?: defaults.host,
            port = prefs.getInt("port", defaults.port),
            addrFader = prefs.getString("addrFader", defaults.addrFader) ?: defaults.addrFader,
            addrButton = prefs.getString("addrButton", defaults.addrButton) ?: defaults.addrButton,
            min = prefs.getFloat("min", defaults.min),
            max = prefs.getFloat("max", defaults.max),
        )
    }

    fun save(context: Context, config: Config) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString("host", config.host)
            .putInt("port", config.port)
            .putString("addrFader", config.addrFader)
            .putString("addrButton", config.addrButton)
            .putFloat("min", config.min)
            .putFloat("max", config.max)
            .apply()
    }
}
