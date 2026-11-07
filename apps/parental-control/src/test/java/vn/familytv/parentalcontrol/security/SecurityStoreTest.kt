package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityStoreTest {
    private val memoryStorage = mutableMapOf<String, Any?>()

    private val fakePrefs = object : SecurityStore.SimpleKeyValueStore {
        override fun getString(key: String): String? = memoryStorage[key] as? String
        override fun putString(key: String, value: String) { memoryStorage[key] = value }
        override fun getBoolean(key: String, def: Boolean): Boolean = (memoryStorage[key] as? Boolean) ?: def
        override fun putBoolean(key: String, value: Boolean) { memoryStorage[key] = value }
        override fun getInt(key: String, def: Int): Int = (memoryStorage[key] as? Int) ?: def
        override fun putInt(key: String, value: Int) { memoryStorage[key] = value }
        override fun getLong(key: String, def: Long): Long = (memoryStorage[key] as? Long) ?: def
        override fun putLong(key: String, value: Long) { memoryStorage[key] = value }
    }

    private val fakeCipher = object : VaultCipher {
        override fun encrypt(plainText: String): String = "ENC:$plainText"
        override fun decrypt(cipherText: String): String = cipherText.removePrefix("ENC:")
    }

    private val store = SecurityStore(fakePrefs, fakeCipher)

    @Test
    fun testStoreAndRetrieveSecret() {
        assertFalse(store.isEnrolled)
        store.saveTotpSecret("JBSWY3DPEHPK3PXP")
        assertEquals("JBSWY3DPEHPK3PXP", store.getTotpSecret())
        assertTrue(store.isEnrolled)
    }

    @Test
    fun testStoreAndConsumeRecoveryCodes() {
        val records = listOf(
            RecoveryCodeRecord(hash = "hash1", used = false),
            RecoveryCodeRecord(hash = "hash2", used = false)
        )
        store.saveRecoverySet("salt123", records)
        assertEquals("salt123", store.getRecoverySalt())
        assertEquals(2, store.getRecoveryRecords().size)

        store.markRecoveryRecordUsed("hash1", 1700000000L)
        val updated = store.getRecoveryRecords()
        assertTrue(updated.first { it.hash == "hash1" }.used)
        assertFalse(updated.first { it.hash == "hash2" }.used)
    }

    @Test
    fun testDefaultPinAuthModeAndVerification() {
        assertEquals(AuthMode.PIN, store.getAuthMode())
        assertFalse(store.isCustomPin())
        assertTrue(store.verifyPin("000000"))
        assertFalse(store.verifyPin("123456"))
    }

    @Test
    fun testSaveCustomPin() {
        store.saveCustomPin("123456")
        assertEquals(AuthMode.PIN, store.getAuthMode())
        assertTrue(store.isCustomPin())
        assertTrue(store.verifyPin("123456"))
        assertFalse(store.verifyPin("000000"))
    }

    @Test
    fun testEnableAndDisableTotp() {
        val records = listOf(
            RecoveryCodeRecord(hash = "hash1", used = false),
            RecoveryCodeRecord(hash = "hash2", used = false)
        )
        store.enableTotp("JBSWY3DPEHPK3PXP", "salt123", records)
        assertEquals(AuthMode.TOTP, store.getAuthMode())
        assertEquals("JBSWY3DPEHPK3PXP", store.getTotpSecret())
        assertEquals("salt123", store.getRecoverySalt())
        assertEquals(2, store.getRecoveryRecords().size)

        store.disableTotp("654321")
        assertEquals(AuthMode.PIN, store.getAuthMode())
        assertNull(store.getTotpSecret())
        assertNull(store.getRecoverySalt())
        assertTrue(store.getRecoveryRecords().isEmpty())
        assertTrue(store.isCustomPin())
        assertTrue(store.verifyPin("654321"))
    }
}
