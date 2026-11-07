package vn.familytv.parentalcontrol.security

import java.security.MessageDigest
import java.security.SecureRandom

data class RecoveryCodeRecord(
    val hash: String,
    val used: Boolean = false,
    val usedAtEpochMs: Long? = null
)

data class GeneratedRecoverySet(
    val plainCodes: List<String>,
    val saltBase64: String,
    val records: List<RecoveryCodeRecord>
)

object RecoveryCodeManager {
    private val random = SecureRandom()

    fun generateSet(): GeneratedRecoverySet {
        val salt = ByteArray(16).apply { random.nextBytes(this) }
        val saltBase64 = Base32.encode(salt).replace("=", "")
        val codes = LinkedHashSet<String>()
        while (codes.size < 6) {
            val num = 100000 + random.nextInt(900000)
            codes.add(num.toString())
        }

        val plainCodes = codes.toList()
        val records = plainCodes.map { code ->
            RecoveryCodeRecord(hash = hash(code, saltBase64))
        }
        return GeneratedRecoverySet(plainCodes, saltBase64, records)
    }

    fun verifyAndConsume(
        inputCode: String,
        saltBase64: String,
        currentRecords: List<RecoveryCodeRecord>,
        nowEpochMs: Long
    ): Pair<Boolean, List<RecoveryCodeRecord>> {
        val clean = inputCode.trim()
        if (clean.length != 6 || !clean.all { it.isDigit() }) {
            return Pair(false, currentRecords)
        }
        val targetHash = hash(clean, saltBase64)
        val matchedIndex = currentRecords.indexOfFirst { it.hash == targetHash && !it.used }
        if (matchedIndex == -1) {
            return Pair(false, currentRecords)
        }

        val updated = currentRecords.toMutableList()
        val old = updated[matchedIndex]
        updated[matchedIndex] = old.copy(used = true, usedAtEpochMs = nowEpochMs)
        return Pair(true, updated)
    }

    private fun hash(code: String, saltBase64: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltBase64.toByteArray(Charsets.UTF_8))
        val digest = md.digest(code.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
