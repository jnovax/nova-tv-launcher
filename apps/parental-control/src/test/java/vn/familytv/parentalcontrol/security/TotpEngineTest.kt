package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpEngineTest {
    // Secret: "12345678901234567890" in ASCII -> Base32: GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
    private val rfcSecretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun testGenerateCodeRfc6238Vectors() {
        // T0 = 59s -> step = 1 -> code 287082
        assertEquals("287082", TotpEngine.generateCode(rfcSecretBase32, 59L))
        // T0 = 1111111109s -> step = 37037036 -> code 081804
        assertEquals("081804", TotpEngine.generateCode(rfcSecretBase32, 1111111109L))
        // T0 = 1234567890s -> step = 41152263 -> code 005924
        assertEquals("005924", TotpEngine.generateCode(rfcSecretBase32, 1234567890L))
    }

    @Test
    fun testVerifyCodeWindowTolerance() {
        val epochNow = 1000000L
        val currentCode = TotpEngine.generateCode(rfcSecretBase32, epochNow)
        val prevCode = TotpEngine.generateCode(rfcSecretBase32, epochNow - 30L)
        val nextCode = TotpEngine.generateCode(rfcSecretBase32, epochNow + 30L)
        val tooOldCode = TotpEngine.generateCode(rfcSecretBase32, epochNow - 60L)
        val tooFutureCode = TotpEngine.generateCode(rfcSecretBase32, epochNow + 60L)

        assertTrue(TotpEngine.verifyCode(rfcSecretBase32, currentCode, epochNow, windowSteps = 1))
        assertTrue(TotpEngine.verifyCode(rfcSecretBase32, prevCode, epochNow, windowSteps = 1))
        assertTrue(TotpEngine.verifyCode(rfcSecretBase32, nextCode, epochNow, windowSteps = 1))
        assertFalse(TotpEngine.verifyCode(rfcSecretBase32, tooOldCode, epochNow, windowSteps = 1))
        assertFalse(TotpEngine.verifyCode(rfcSecretBase32, tooFutureCode, epochNow, windowSteps = 1))
        assertFalse(TotpEngine.verifyCode(rfcSecretBase32, "000000", epochNow, windowSteps = 1))
    }

    @Test
    fun testGenerateSecretAndUri() {
        val secret = TotpEngine.generateSecret(20)
        assertEquals(32, secret.length)
        val uri = TotpEngine.generateOtpAuthUri(secret, "TCL TV", "FamilyTime")
        assertTrue(uri.startsWith("otpauth://totp/FamilyTime:TCL%20TV?secret=$secret"))
        assertTrue(uri.contains("issuer=FamilyTime"))
        assertTrue(uri.contains("algorithm=SHA1"))
        assertTrue(uri.contains("digits=6"))
        assertTrue(uri.contains("period=30"))
    }
}
