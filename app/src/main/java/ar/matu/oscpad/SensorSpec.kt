package ar.matu.oscpad

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    Triple(Sensor.TYPE_PROXIMITY, "proximity", "Proximidad"),
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
    private val axis: String,
    private val rateMs: Long,
    private val onValue: (Float) -> Unit,
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
        val v = event.values
        val value = when (axis) {
            "y" -> v.getOrElse(1) { 0f }
            "z" -> v.getOrElse(2) { 0f }
            "magnitud" -> sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            else -> v.getOrElse(0) { 0f }
        }
        onValue(value)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
