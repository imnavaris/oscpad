package ar.matu.oscpad

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.log10
import kotlin.math.sqrt

data class SensorSpec(
    val kind: String,
    val label: String,
    val axes: List<String>,
)

private val vectorAxes = listOf("x", "y", "z", "magnitud")
private val scalarAxes = listOf("valor")

private val catalog = listOf(
    Triple(Sensor.TYPE_ACCELEROMETER, "accelerometer", "Acelerómetro"),
    Triple(Sensor.TYPE_GYROSCOPE, "gyroscope", "Giroscopio"),
    Triple(Sensor.TYPE_MAGNETIC_FIELD, "magnetic_field", "Magnetómetro"),
    Triple(Sensor.TYPE_LIGHT, "light", "Luz"),
    Triple(Sensor.TYPE_PRESSURE, "pressure", "Presión"),
)

private fun isVector(type: Int) =
    type == Sensor.TYPE_ACCELEROMETER ||
        type == Sensor.TYPE_GYROSCOPE ||
        type == Sensor.TYPE_MAGNETIC_FIELD

private fun sensorType(kind: String): Int =
    catalog.firstOrNull { it.second == kind }?.first ?: Sensor.TYPE_ACCELEROMETER

fun listSensors(context: Context): List<SensorSpec> {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: return emptyList()
    return catalog
        .filter { manager.getDefaultSensor(it.first) != null }
        .map { SensorSpec(it.second, it.third, if (isVector(it.first)) vectorAxes else scalarAxes) }
}

class SensorFeed(
    context: Context,
    private val kind: String,
    private val rateMs: Long,
    private val onValues: (FloatArray) -> Unit,
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = manager?.getDefaultSensor(sensorType(kind))
    private var lastMs = 0L

    fun start() {
        val s = sensor ?: return
        manager?.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        manager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now - lastMs < rateMs.coerceAtLeast(16L)) return
        lastMs = now
        val maxRange = sensor?.maximumRange ?: 0f
        onValues(
            floatArrayOf(
                normalizeSensor(kind, "x", event.values, maxRange),
                normalizeSensor(kind, "y", event.values, maxRange),
                normalizeSensor(kind, "z", event.values, maxRange),
                normalizeSensor(kind, "magnitud", event.values, maxRange),
            ),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

fun normalizeSensor(kind: String, axis: String, v: FloatArray, maxRange: Float): Float {
    fun raw(): Float = when (axis) {
        "y" -> v.getOrElse(1) { 0f }
        "z" -> v.getOrElse(2) { 0f }
        "magnitud" -> sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        else -> v.getOrElse(0) { 0f }
    }
    val r = raw()
    return when (kind) {
        "accelerometer" -> if (axis == "magnitud") {
            (r / 19.6f).coerceIn(0f, 1f)
        } else {
            ((r / 19.6f).coerceIn(-1f, 1f) + 1f) / 2f
        }
        "gyroscope" -> if (axis == "magnitud") {
            (r / 8.7f).coerceIn(0f, 1f)
        } else {
            ((r / 8.7f).coerceIn(-1f, 1f) + 1f) / 2f
        }
        "magnetic_field" -> if (axis == "magnitud") {
            (r / 100f).coerceIn(0f, 1f)
        } else {
            ((r / 100f).coerceIn(-1f, 1f) + 1f) / 2f
        }
        "light" -> (log10(1f + r) / log10(40001f)).coerceIn(0f, 1f)
        "proximity" -> if (maxRange > 0f) {
            (r / maxRange).coerceIn(0f, 1f)
        } else if (r > 0f) {
            1f
        } else {
            0f
        }
        "pressure" -> ((r - 300f) / 800f).coerceIn(0f, 1f)
        else -> r.coerceIn(0f, 1f)
    }
}
