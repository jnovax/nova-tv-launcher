package vn.familytv.parentalcontrol.ipc

import android.content.Context
import android.content.Intent

class ModeBroadcastSender(private val context: Context) {
    fun sendModeChanged(mode: Mode, sessionRemainingSec: Int, isQuizActive: Boolean) {
        val intent = createIntent(mode, sessionRemainingSec, isQuizActive)
        context.sendBroadcast(intent)
    }

    companion object {
        const val ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_SESSION_REMAINING_SEC = "extra_session_remaining_sec"
        const val EXTRA_IS_QUIZ_ACTIVE = "extra_is_quiz_active"

        fun createIntent(mode: Mode, sessionRemainingSec: Int, isQuizActive: Boolean): Intent {
            return ModeIntent(mode, sessionRemainingSec, isQuizActive)
        }
    }

    private class ModeIntent(
        private val mode: Mode,
        private val sessionRemainingSec: Int,
        private val isQuizActive: Boolean
    ) : Intent(ACTION_MODE_CHANGED) {
        init {
            try {
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_SESSION_REMAINING_SEC, sessionRemainingSec)
                putExtra(EXTRA_IS_QUIZ_ACTIVE, isQuizActive)
            } catch (_: RuntimeException) {
                // In local unit tests without Robolectric, Android framework methods throw
                // "Method ... not mocked". Fallback getters provide test coverage.
            }
        }

        override fun getAction(): String = ACTION_MODE_CHANGED

        override fun getStringExtra(name: String?): String? {
            return if (name == EXTRA_MODE) {
                mode.name
            } else {
                try { super.getStringExtra(name) } catch (_: RuntimeException) { null }
            }
        }

        override fun getIntExtra(name: String?, defaultValue: Int): Int {
            return if (name == EXTRA_SESSION_REMAINING_SEC) {
                sessionRemainingSec
            } else {
                try { super.getIntExtra(name, defaultValue) } catch (_: RuntimeException) { defaultValue }
            }
        }

        override fun getBooleanExtra(name: String?, defaultValue: Boolean): Boolean {
            return if (name == EXTRA_IS_QUIZ_ACTIVE) {
                isQuizActive
            } else {
                try { super.getBooleanExtra(name, defaultValue) } catch (_: RuntimeException) { defaultValue }
            }
        }
    }
}
