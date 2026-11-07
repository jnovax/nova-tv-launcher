package vn.familytv.parentalcontrol.ipc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import java.lang.reflect.Proxy

class ModeContentProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "vn.familytv.parentalcontrol.provider"
        val CONTENT_URI: Uri by lazy {
            try {
                Uri.parse("content://$AUTHORITY/status")
            } catch (_: RuntimeException) {
                // Fallback in unmocked local unit tests
                Uri.EMPTY
            }
        }

        const val COL_CURRENT_MODE = "current_mode"
        const val COL_SESSION_REMAINING_SEC = "session_remaining_sec"
        const val COL_IS_QUIZ_ACTIVE = "is_quiz_active"
        const val COL_SCREENTIME_REMAINING_SEC = "screentime_remaining_sec"

        val COLUMNS = arrayOf(
            COL_CURRENT_MODE,
            COL_SESSION_REMAINING_SEC,
            COL_IS_QUIZ_ACTIVE,
            COL_SCREENTIME_REMAINING_SEC
        )

        fun buildCursor(
            mode: Mode,
            sessionRemainingSec: Int,
            isQuizActive: Boolean,
            screenTimeRemainingSec: Int
        ): Cursor {
            val values = arrayOf<Any>(
                mode.name,
                sessionRemainingSec,
                if (isQuizActive) 1 else 0,
                screenTimeRemainingSec
            )
            return try {
                MatrixCursor(COLUMNS).apply { addRow(values) }
            } catch (_: RuntimeException) {
                StatusCursor(COLUMNS, values)
            }
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val manager = ModeStateManager.instance
        val now = System.currentTimeMillis()
        return buildCursor(
            mode = manager.getMode(),
            sessionRemainingSec = manager.getSessionRemainingSec(now),
            isQuizActive = manager.isQuizActive(),
            screenTimeRemainingSec = manager.getScreenTimeRemainingSec()
        )
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.familytv.status"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private class StatusCursor(
        private val cols: Array<String>,
        private val vals: Array<Any>
    ) : Cursor by (Proxy.newProxyInstance(
        Cursor::class.java.classLoader,
        arrayOf(Cursor::class.java)
    ) { _, method, args ->
        when (method.name) {
            "getColumnNames" -> cols
            "getColumnName" -> cols[args[0] as Int]
            "getColumnCount" -> cols.size
            "getColumnIndex" -> cols.indexOf(args[0] as String)
            "getColumnIndexOrThrow" -> {
                val idx = cols.indexOf(args[0] as String)
                if (idx < 0) throw IllegalArgumentException("Column not found: ${args[0]}")
                idx
            }
            "getCount" -> 1
            "moveToFirst" -> true
            "moveToNext" -> false
            "getString" -> vals[args[0] as Int].toString()
            "getInt" -> (vals[args[0] as Int] as Number).toInt()
            "getLong" -> (vals[args[0] as Int] as Number).toLong()
            "close" -> Unit
            "isClosed" -> false
            else -> null
        }
    } as Cursor)
}
