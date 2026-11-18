package ar.matu.oscpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelTest {
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
    fun buttonCurrentSendsMaxWhenOnAndMinWhenOff() {
        val base = Channel(id = 1, name = "play", type = "button", min = 0f, max = 10f)
        assertEquals(10f, base.copy(on = true).current(), 0.001f)
        assertEquals(0f, base.copy(on = false).current(), 0.001f)
    }

    @Test
    fun sanitizeNameReplacesWhitespaceAndRejectsEmpty() {
        assertEquals("mis_luces", sanitizeName("mis luces"))
        assertEquals("abc", sanitizeName("  abc  "))
        assertNull(sanitizeName("   "))
        assertNull(sanitizeName("!!!"))
    }
}
