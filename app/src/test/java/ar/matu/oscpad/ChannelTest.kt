package ar.matu.oscpad

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun addressPrefixesNameWithSlash() {
        assertEquals("/canal1", Channel(id = 1, name = "canal1").address())
    }

    @Test
    fun faderCurrentScalesLevelIntoRange() {
        val channel = Channel(id = 1, name = "luz", min = 100f, max = 200f, level = 0.25f)
        assertEquals(125f, channel.current(), 0.001f)
    }

    @Test
    fun buttonCurrentSendsConfiguredBinaryValues() {
        val base = Channel(id = 1, name = "play", type = "button", onValue = 10f, offValue = 2f)
        assertEquals(10f, base.copy(on = true).current(), 0.001f)
        assertEquals(2f, base.copy(on = false).current(), 0.001f)
    }

    @Test
    fun buttonDefaultsToOneAndZero() {
        val base = Channel(id = 1, name = "play", type = "button")
        assertEquals(1f, base.copy(on = true).current(), 0.001f)
        assertEquals(0f, base.copy(on = false).current(), 0.001f)
    }

    @Test
    fun buttonDefaultsToMomentary() {
        assertEquals(false, Channel(id = 1, name = "t").hold)
    }

    @Test
    fun normalizeAccelerometerCentersFlatAtHalf() {
        assertEquals(0.5f, normalizeSensor("accelerometer", "x", floatArrayOf(0f, 0f, 0f), 0f), 0.001f)
        assertEquals(1f, normalizeSensor("accelerometer", "x", floatArrayOf(19.6f, 0f, 0f), 0f), 0.001f)
        assertEquals(0f, normalizeSensor("accelerometer", "x", floatArrayOf(-19.6f, 0f, 0f), 0f), 0.001f)
    }

    @Test
    fun normalizeLightUsesLogScale() {
        assertEquals(0f, normalizeSensor("light", "valor", floatArrayOf(0f), 40000f), 0.01f)
        assertEquals(1f, normalizeSensor("light", "valor", floatArrayOf(40000f), 40000f), 0.01f)
    }

    @Test
    fun cleanIntInputKeepsOnlyDigits() {
        assertEquals("50", cleanIntInput("50"))
        assertEquals("50", cleanIntInput(" 50 ms"))
        assertEquals("", cleanIntInput("abc"))
    }

    @Test
    fun cleanFloatInputKeepsNumericChars() {
        assertEquals("0.5", cleanFloatInput("0.5"))
        assertEquals("-1.5", cleanFloatInput(" -1.5 "))
        assertEquals("1.0", cleanFloatInput("1.0x"))
    }

    @Test
    fun xyValuesMapEachAxisToItsRange() {
        val channel = Channel(id = 1, name = "pad", type = "xy", min = 0f, max = 10f, min2 = 100f, max2 = 200f)
        val mapped = channel.xyValues(0.5f, 0.25f)
        assertEquals(5f, mapped[0], 0.001f)
        assertEquals(125f, mapped[1], 0.001f)
    }

    @Test
    fun sanitizeNameReplacesWhitespaceAndRejectsEmpty() {
        assertEquals("mis_luces", sanitizeName("mis luces"))
        assertEquals("abc", sanitizeName("  abc  "))
        assertNull(sanitizeName("   "))
        assertNull(sanitizeName("!!!"))
    }

    @Test
    fun channelDefaultsForExtendedControls() {
        val channel = Channel(id = 1, name = "luz")
        assertEquals(0.5f, channel.level2, 0.001f)
        assertEquals(0.5f, channel.level3, 0.001f)
        assertEquals("accelerometer", channel.sensorKind)
        assertEquals("x", channel.sensorAxis)
        assertEquals(50L, channel.rateMs)
    }

    @Test
    fun legacyChannelJsonFallsBackToDefaults() {
        val decoded = json.decodeFromString<Channel>("""{"id":1,"name":"luz"}""")
        assertEquals(0.5f, decoded.level, 0.001f)
        assertEquals(0.5f, decoded.level2, 0.001f)
        assertEquals(0.5f, decoded.level3, 0.001f)
        assertEquals("accelerometer", decoded.sensorKind)
        assertEquals("x", decoded.sensorAxis)
        assertEquals(50L, decoded.rateMs)
    }

    @Test
    fun presetRoundTripsThroughJson() {
        val preset = Preset(
            id = 7,
            name = "escena",
            channels = listOf(
                Channel(id = 1, name = "luz", level2 = 0.25f, rateMs = 120L),
                Channel(id = 2, name = "play", type = "button", on = true),
            ),
            net = NetConfig(host = "10.0.0.2", port = 9000),
        )
        val decoded = json.decodeFromString<Preset>(json.encodeToString(preset))
        assertEquals(preset, decoded)
    }
}
