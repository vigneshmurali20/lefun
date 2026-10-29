package app.lefun.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HexTest {
    @Test fun roundTrip() {
        val bytes = byteArrayOf(0x01, 0xAB.toByte(), 0xFF.toByte())
        assertEquals("01 AB FF", bytes.toHex())
        assertArrayEquals(bytes, "01 ab ff".hexToBytes())
    }
}
