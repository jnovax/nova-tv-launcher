package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Base32Test {
    @Test
    fun testEncodeAndDecodeAscii() {
        val input = "Hello World".toByteArray(Charsets.UTF_8)
        val encoded = Base32.encode(input)
        val decoded = Base32.decode(encoded)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun testRfc4648TestVectors() {
        assertEquals("", Base32.encode("".toByteArray()))
        assertEquals("MY======", Base32.encode("f".toByteArray()))
        assertEquals("MZXQ====", Base32.encode("fo".toByteArray()))
        assertEquals("MZXW6===", Base32.encode("foo".toByteArray()))
        assertEquals("MZXW6YQ=", Base32.encode("foob".toByteArray()))
        assertEquals("MZXW6YTB", Base32.encode("fooba".toByteArray()))
        assertEquals("MZXW6YTBOI======", Base32.encode("foobar".toByteArray()))
    }
}
