package vn.familytv.parentalcontrol.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

enum class AuthMode {
    PIN,
    TOTP
}

class SecurityStore(
    private val storage: SimpleKeyValueStore,
    private val cipher: VaultCipher
) {
    interface SimpleKeyValueStore {
        fun getString(key: String): String?
        fun putString(key: String, value: String)
        fun getBoolean(key: String, def: Boolean): Boolean
        fun putBoolean(key: String, value: Boolean)
        fun getInt(key: String, def: Int): Int
        fun putInt(key: String, value: Int)
        fun getLong(key: String, def: Long): Long
        fun putLong(key: String, value: Long)
    }

    class SharedPrefsStorage(private val prefs: SharedPreferences) : SimpleKeyValueStore {
        override fun getString(key: String): String? = prefs.getString(key, null)
        override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
        override fun getBoolean(key: String, def: Boolean): Boolean = prefs.getBoolean(key, def)
        override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
        override fun getInt(key: String, def: Int): Int = prefs.getInt(key, def)
        override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
        override fun getLong(key: String, def: Long): Long = prefs.getLong(key, def)
        override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    }

    companion object {
        private const val PREFS_NAME = "family_time_security"
        private const val KEY_ENCRYPTED_SECRET = "encrypted_totp_secret"
        private const val KEY_IS_ENROLLED = "is_enrolled"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_RECOVERY_RECORDS = "recovery_records"
        private const val KEY_AUTH_MODE = "auth_mode"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_IS_CUSTOM_PIN = "is_custom_pin"
        private const val DEFAULT_PIN = "000000"
        private const val DEFAULT_PIN_SALT = "DEFAULT_SALT_FAMILY_TIME"

        fun fromContext(context: Context): SecurityStore {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SecurityStore(SharedPrefsStorage(prefs), AndroidKeyStoreCipher())
        }
    }

    val isEnrolled: Boolean
        get() = storage.getBoolean(KEY_IS_ENROLLED, false)

    fun getAuthMode(): AuthMode {
        val modeStr = storage.getString(KEY_AUTH_MODE)
        if (modeStr != null) {
            return try { AuthMode.valueOf(modeStr) } catch (_: Exception) { AuthMode.PIN }
        }
        return if (storage.getBoolean(KEY_IS_ENROLLED, false) && storage.getString(KEY_ENCRYPTED_SECRET) != null) {
            AuthMode.TOTP
        } else {
            AuthMode.PIN
        }
    }

    fun isCustomPin(): Boolean = storage.getBoolean(KEY_IS_CUSTOM_PIN, false)

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(candidatePin: String): Boolean {
        var salt = storage.getString(KEY_PIN_SALT)
        var hash = storage.getString(KEY_PIN_HASH)
        if (salt == null || hash == null) {
            salt = DEFAULT_PIN_SALT
            hash = hashPin(DEFAULT_PIN, salt)
            storage.putString(KEY_PIN_SALT, salt)
            storage.putString(KEY_PIN_HASH, hash)
            storage.putBoolean(KEY_IS_CUSTOM_PIN, false)
        }
        return hash == hashPin(candidatePin, salt)
    }

    fun saveCustomPin(newPin: String) {
        val randomBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val salt = Base32.encode(randomBytes).take(16)
        val hash = hashPin(newPin, salt)
        storage.putString(KEY_PIN_SALT, salt)
        storage.putString(KEY_PIN_HASH, hash)
        storage.putBoolean(KEY_IS_CUSTOM_PIN, true)
        storage.putString(KEY_AUTH_MODE, AuthMode.PIN.name)
        storage.putBoolean(KEY_IS_ENROLLED, true)
    }

    fun saveTotpSecret(secretBase32: String) {
        val encrypted = cipher.encrypt(secretBase32)
        storage.putString(KEY_ENCRYPTED_SECRET, encrypted)
        storage.putBoolean(KEY_IS_ENROLLED, true)
    }

    fun enableTotp(secretBase32: String, salt: String, records: List<RecoveryCodeRecord>) {
        saveTotpSecret(secretBase32)
        saveRecoverySet(salt, records)
        storage.putString(KEY_AUTH_MODE, AuthMode.TOTP.name)
        storage.putBoolean(KEY_IS_ENROLLED, true)
    }

    fun disableTotp(fallbackPin: String) {
        storage.putString(KEY_ENCRYPTED_SECRET, "")
        storage.putString(KEY_RECOVERY_SALT, "")
        storage.putString(KEY_RECOVERY_RECORDS, "")
        saveCustomPin(fallbackPin)
        storage.putString(KEY_AUTH_MODE, AuthMode.PIN.name)
    }

    fun getTotpSecret(): String? {
        val encrypted = storage.getString(KEY_ENCRYPTED_SECRET)?.takeIf { it.isNotBlank() } ?: return null
        return try {
            cipher.decrypt(encrypted)
        } catch (_: Exception) {
            null
        }
    }

    fun saveRecoverySet(saltBase64: String, records: List<RecoveryCodeRecord>) {
        storage.putString(KEY_RECOVERY_SALT, saltBase64)
        val serialized = records.joinToString(";") { "${it.hash},${it.used},${it.usedAtEpochMs ?: 0}" }
        storage.putString(KEY_RECOVERY_RECORDS, serialized)
    }

    fun getRecoverySalt(): String? = storage.getString(KEY_RECOVERY_SALT)?.takeIf { it.isNotBlank() }

    fun getRecoveryRecords(): List<RecoveryCodeRecord> {
        val raw = storage.getString(KEY_RECOVERY_RECORDS)?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 3) {
                RecoveryCodeRecord(
                    hash = parts[0],
                    used = parts[1].toBoolean(),
                    usedAtEpochMs = parts[2].toLongOrNull()?.takeIf { it > 0 }
                )
            } else null
        }
    }

    fun markRecoveryRecordUsed(hash: String, epochMs: Long) {
        val records = getRecoveryRecords().map {
            if (it.hash == hash) it.copy(used = true, usedAtEpochMs = epochMs) else it
        }
        val salt = getRecoverySalt() ?: ""
        saveRecoverySet(salt, records)
    }
}
