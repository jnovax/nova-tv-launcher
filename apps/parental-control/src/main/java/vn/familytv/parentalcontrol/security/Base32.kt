package vn.familytv.parentalcontrol.security

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE_TABLE = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
            this[ALPHABET[i].lowercaseChar().code] = i
        }
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val output = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                output.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            output.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        while (output.length % 8 != 0) {
            output.append('=')
        }
        return output.toString()
    }

    fun decode(input: String): ByteArray {
        val clean = input.trimEnd('=').uppercase()
        val output = ArrayList<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (char in clean) {
            val code = char.code
            if (code >= DECODE_TABLE.size || DECODE_TABLE[code] == -1) continue
            buffer = (buffer shl 5) or DECODE_TABLE[code]
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }
}
