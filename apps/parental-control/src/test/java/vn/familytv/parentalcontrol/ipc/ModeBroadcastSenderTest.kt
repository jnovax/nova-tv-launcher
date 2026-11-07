package vn.familytv.parentalcontrol.ipc

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class ModeBroadcastSenderTest {
    @Test
    fun testBuildBroadcastIntent() {
        val intent = ModeBroadcastSender.createIntent(
            mode = Mode.PARENT,
            sessionRemainingSec = 180,
            isQuizActive = true
        )
        assertEquals("vn.familytv.parentalcontrol.ACTION_MODE_CHANGED", intent.action)
        assertEquals("PARENT", intent.getStringExtra("extra_mode"))
        assertEquals(180, intent.getIntExtra("extra_session_remaining_sec", -1))
        assertTrue(intent.getBooleanExtra("extra_is_quiz_active", false))
    }
}
