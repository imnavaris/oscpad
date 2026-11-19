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

    @Test
    fun formatValueRoundsToTwoDecimals() {
        assertEquals("0.50", formatValue(0.5f))
        assertEquals("1.00", formatValue(1f))
        assertEquals("0.57", formatValue(0.574f))
    }

    @Test
    fun maskDefaultsToAllEnabled() {
        val ch = Channel(id = 1, name = "xy1", type = "xy")
        assertEquals(15, ch.mask)
        assertEquals(true, ch.compEnabled(0))
        assertEquals(true, ch.compEnabled(1))
    }

    @Test
    fun toggleCompFlipsOneBit() {
        val ch = Channel(id = 1, name = "rgb", type = "color")
        val off = ch.toggleComp(1)
        assertEquals(true, off.compEnabled(0))
        assertEquals(false, off.compEnabled(1))
        assertEquals(true, off.compEnabled(2))
        assertEquals(true, off.toggleComp(1).compEnabled(1))
    }

    @Test
    fun outputValuesZeroDisabledComponents() {
        val ch = Channel(
            id = 1,
            name = "rgb",
            type = "color",
            level = 0.2f,
            level2 = 0.4f,
            level3 = 0.6f,
            mask = 15 xor (1 shl 1),
        )
        val out = ch.outputValues()
        assertEquals(3, out.size)
        assertEquals(0.2f, out[0], 0.001f)
        assertEquals(0f, out[1], 0.001f)
        assertEquals(0.6f, out[2], 0.001f)
    }

    @Test
    fun outputValuesSensorHasFourComponents() {
        val ch = Channel(id = 1, name = "giro", type = "sensor", level4 = 0.9f)
        val out = ch.outputValues()
        assertEquals(4, out.size)
        assertEquals(0.9f, out[3], 0.001f)
    }

    @Test
    fun compLettersMatchComponentCount() {
        assertEquals("X", Channel(id = 1, name = "a", type = "xy").compLetter(0))
        assertEquals("B", Channel(id = 1, name = "a", type = "color").compLetter(2))
        assertEquals("M", Channel(id = 1, name = "a", type = "sensor").compLetter(3))
    }
}
