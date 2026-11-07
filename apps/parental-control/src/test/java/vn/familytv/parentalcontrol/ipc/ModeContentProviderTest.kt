package vn.familytv.parentalcontrol.ipc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModeContentProviderTest {
    @Test
    fun testProviderCursorColumnsAndValues() {
        val cursor = ModeContentProvider.buildCursor(
            mode = Mode.PARENT,
            sessionRemainingSec = 300,
            isQuizActive = true,
            screenTimeRemainingSec = 600
        )
        assertNotNull(cursor)

        val expectedColumns = arrayOf(
            ModeContentProvider.COL_CURRENT_MODE,
            ModeContentProvider.COL_SESSION_REMAINING_SEC,
            ModeContentProvider.COL_IS_QUIZ_ACTIVE,
            ModeContentProvider.COL_SCREENTIME_REMAINING_SEC
        )
        assertArrayEquals(expectedColumns, cursor.columnNames)
        assertEquals(4, cursor.columnCount)

        assertEquals("current_mode", cursor.getColumnName(0))
        assertEquals("session_remaining_sec", cursor.getColumnName(1))
        assertEquals("is_quiz_active", cursor.getColumnName(2))
        assertEquals("screentime_remaining_sec", cursor.getColumnName(3))

        cursor.moveToFirst()
        assertEquals("PARENT", cursor.getString(0))
        assertEquals(300, cursor.getInt(1))
        assertEquals(1, cursor.getInt(2))
        assertEquals(600, cursor.getInt(3))
    }

    @Test
    fun testProviderKidModeDefaults() {
        val cursor = ModeContentProvider.buildCursor(
            mode = Mode.KID,
            sessionRemainingSec = 0,
            isQuizActive = false,
            screenTimeRemainingSec = 900
        )
        assertNotNull(cursor)
        cursor.moveToFirst()
        assertEquals("KID", cursor.getString(0))
        assertEquals(0, cursor.getInt(1))
        assertEquals(0, cursor.getInt(2))
        assertEquals(900, cursor.getInt(3))
    }
}
