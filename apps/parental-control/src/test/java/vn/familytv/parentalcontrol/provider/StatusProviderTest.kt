package vn.familytv.parentalcontrol.provider

import com.appletv.contract.StatusProviderContract
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeStateManager

class StatusProviderTest {

    private lateinit var stateManager: ModeStateManager
    private lateinit var provider: StatusProvider

    @Before
    fun setUp() {
        stateManager = ModeStateManager(sessionDurationSec = 300)
        stateManager.resetForTesting()
        provider = StatusProvider().apply {
            this.stateManager = this@StatusProviderTest.stateManager
        }
    }

    @Test
    fun testContractColumnsAlignment() {
        val cursor = StatusProvider.buildCursor(
            mode = Mode.KID,
            remainingSeconds = 600L,
            sessionLimitSeconds = 900L,
            whitelistPackages = "com.pkg.one,com.pkg.two"
        )
        assertNotNull(cursor)

        val expectedColumns = arrayOf(
            StatusProviderContract.COLUMN_CURRENT_MODE,
            StatusProviderContract.COLUMN_REMAINING_SECONDS,
            StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS,
            StatusProviderContract.COLUMN_WHITELIST_PACKAGES
        )
        assertArrayEquals(expectedColumns, cursor.columnNames)
        assertEquals(4, cursor.columnCount)
        assertEquals(1, cursor.count)

        assertEquals(StatusProviderContract.COLUMN_CURRENT_MODE, cursor.getColumnName(0))
        assertEquals(StatusProviderContract.COLUMN_REMAINING_SECONDS, cursor.getColumnName(1))
        assertEquals(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS, cursor.getColumnName(2))
        assertEquals(StatusProviderContract.COLUMN_WHITELIST_PACKAGES, cursor.getColumnName(3))
    }

    @Test
    fun testKidModeValuesAndColumnIndices() {
        val cursor = StatusProvider.buildCursor(
            mode = Mode.KID,
            remainingSeconds = 420L,
            sessionLimitSeconds = 900L,
            whitelistPackages = "com.google.android.youtube.tvkids,org.smarttube.stable"
        )

        assertTrue(cursor.moveToFirst())

        val modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE)
        val remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS)
        val limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS)
        val whiteIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_WHITELIST_PACKAGES)

        assertEquals("KID", cursor.getString(modeIdx))
        assertEquals(420L, cursor.getLong(remIdx))
        assertEquals(420, cursor.getInt(remIdx))
        assertEquals(900L, cursor.getLong(limitIdx))
        assertEquals(900, cursor.getInt(limitIdx))
        assertTrue(cursor.getString(whiteIdx).contains("org.smarttube.stable"))
        assertFalse(cursor.moveToNext())
        cursor.close()
    }

    @Test
    fun testParentModeValues() {
        val cursor = StatusProvider.buildCursor(
            mode = Mode.PARENT,
            remainingSeconds = 900L
        )

        assertTrue(cursor.moveToFirst())
        val modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE)
        val remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS)

        assertEquals("PARENT", cursor.getString(modeIdx))
        assertEquals(900L, cursor.getLong(remIdx))
    }

    @Test
    fun testDefaultValues() {
        val cursor = StatusProvider.buildCursor(mode = Mode.KID)
        assertTrue(cursor.moveToFirst())

        val remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS)
        val limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS)
        val whiteIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_WHITELIST_PACKAGES)

        assertEquals(900L, cursor.getLong(remIdx))
        assertEquals(900L, cursor.getLong(limitIdx))
        assertTrue(cursor.getString(whiteIdx).contains("vn.familytv.parentalcontrol"))
    }

    @Test
    fun testProviderQueryConnectsToModeStateManager() {
        stateManager.setScreenTimeRemainingSec(540)

        // Query provider with null projection
        val cursor = provider.queryStatus()
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())

        val modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE)
        val remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS)
        val limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS)

        assertEquals("KID", cursor.getString(modeIdx))
        assertEquals(540L, cursor.getLong(remIdx))
        assertEquals(900L, cursor.getLong(limitIdx))
    }

    @Test
    fun testProviderQueryWhenParentAuthenticated() {
        stateManager.onParentAuthenticated(System.currentTimeMillis())
        stateManager.setScreenTimeRemainingSec(300)

        val cursor = provider.queryStatus()
        assertTrue(cursor.moveToFirst())

        val modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE)
        val remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS)

        assertEquals("PARENT", cursor.getString(modeIdx))
        assertEquals(300L, cursor.getLong(remIdx))
    }

    @Test
    fun testQueryWithSpecificProjection() {
        stateManager.setScreenTimeRemainingSec(180)

        val projection = arrayOf(StatusProviderContract.COLUMN_REMAINING_SECONDS)
        val cursor = provider.queryStatus(projection)

        assertNotNull(cursor)
        assertEquals(1, cursor.columnCount)
        assertEquals(StatusProviderContract.COLUMN_REMAINING_SECONDS, cursor.getColumnName(0))
        assertTrue(cursor.moveToFirst())
        assertEquals(180L, cursor.getLong(0))
        assertEquals(0, cursor.getColumnIndex(StatusProviderContract.COLUMN_REMAINING_SECONDS))
        assertEquals(-1, cursor.getColumnIndex(StatusProviderContract.COLUMN_CURRENT_MODE))
    }

    @Test
    fun testColumnNotFoundThrowsException() {
        val cursor = StatusProvider.buildCursor(mode = Mode.KID)
        try {
            cursor.getColumnIndexOrThrow("non_existent_column")
            org.junit.Assert.fail("Expected IllegalArgumentException for unknown column")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
