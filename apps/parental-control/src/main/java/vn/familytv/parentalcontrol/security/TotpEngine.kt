package vn.familytv.parentalcontrol.security

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpEngine {
    private const val HMAC_ALGORITHM = "HmacSHA1"
    private const val PERIOD_SECONDS = 30L
    private const val DIGITS = 6
    private val MODULO = Math.pow(10.0, DIGITS.toDouble()).toInt()
    private val random = SecureRandom()

    fun generateSecret(byteLength: Int = 20): String {
        val bytes = ByteArray(byteLength)
        random.nextBytes(bytes)
        return Base32.encode(bytes).replace("=", "")
    }

    fun generateOtpAuthUri(
        secretBase32: String,
        accountName: String = "TCL TV",
        issuer: String = "FamilyTime"
    ): String {
        val encodedAccount = URLEncoder.encode(accountName, "UTF-8").replace("+", "%20")
        val encodedIssuer = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20")
        return "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secretBase32&issuer=$encodedIssuer&algorithm=SHA1&digits=$DIGITS&period=$PERIOD_SECONDS"
    }

    fun generateCode(secretBase32: String, epochSeconds: Long): String {
        val timeStep = epochSeconds / PERIOD_SECONDS
        return generateCodeForStep(secretBase32, timeStep)
    }

    fun verifyCode(
        secretBase32: String,
        candidateCode: String,
        epochSeconds: Long,
        windowSteps: Int = 1
    ): Boolean {
        val cleaned = candidateCode.trim()
        if (cleaned.length != DIGITS || !cleaned.all { it.isDigit() }) return false
        val currentStep = epochSeconds / PERIOD_SECONDS
        for (stepOffset in -windowSteps..windowSteps) {
            val validCode = generateCodeForStep(secretBase32, currentStep + stepOffset)
            if (validCode == cleaned) return true
        }
        return false
    }

    private fun generateCodeForStep(secretBase32: String, timeStep: Long): String {
        val keyBytes = Base32.decode(secretBase32)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(keyBytes, HMAC_ALGORITHM))

        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncatedHash = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val pin = truncatedHash % MODULO
        return pin.toString().padStart(DIGITS, '0')
    }
}
