package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeManagerTest {
    @Test
    fun testGenerateSetCreates6Distinct6DigitCodes() {
        val set = RecoveryCodeManager.generateSet()
        assertEquals(6, set.plainCodes.size)
        assertEquals(6, set.records.size)
        assertEquals(6, set.plainCodes.distinct().size)
        set.plainCodes.forEach { code ->
            assertEquals(6, code.length)
            assertTrue(code.all { it.isDigit() })
        }
        set.records.forEach { record ->
            assertFalse(record.used)
            assertEquals(null, record.usedAtEpochMs)
        }
    }

    @Test
    fun testVerifyAndConsumeSuccessAndSingleUse() {
        val set = RecoveryCodeManager.generateSet()
        val firstCode = set.plainCodes[0]
        val (success1, updatedRecords1) = RecoveryCodeManager.verifyAndConsume(
            firstCode, set.saltBase64, set.records, 1700000000L
        )
        assertTrue(success1)
        val usedRecord = updatedRecords1.first { it.used }
        assertEquals(1700000000L, usedRecord.usedAtEpochMs)

        // Try using the exact same code a second time
        val (success2, _) = RecoveryCodeManager.verifyAndConsume(
            firstCode, set.saltBase64, updatedRecords1, 1700000010L
        )
        assertFalse(success2)
    }

    @Test
    fun testVerifyRejectsInvalidCode() {
        val set = RecoveryCodeManager.generateSet()
        val (success, _) = RecoveryCodeManager.verifyAndConsume(
            "000000", set.saltBase64, set.records, 1700000000L
        )
        assertFalse(success)
    }
}
