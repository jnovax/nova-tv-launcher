package vn.familytv.parentalcontrol.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.appletv.contract.StatusProviderContract
import java.lang.reflect.Proxy
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeStateManager

class StatusProvider : ContentProvider() {

    internal var stateManager: ModeStateManager = ModeStateManager.instance

    companion object {
        const val AUTHORITY = StatusProviderContract.AUTHORITY
        const val STATUS_PATH = StatusProviderContract.STATUS_PATH
        const val CONTENT_URI_STRING = StatusProviderContract.CONTENT_URI_STRING

        val CONTENT_URI: Uri by lazy {
            try {
                Uri.parse(CONTENT_URI_STRING)
            } catch (_: Throwable) {
                // Fallback in unmocked local unit tests
                Uri.EMPTY
            }
        }

        const val DEFAULT_SESSION_LIMIT_SECONDS: Long = 900L
        const val DEFAULT_WHITELIST_PACKAGES: String =
            "com.google.android.youtube.tvkids,org.smarttube.stable,org.schabi.newpipe,org.videolan.vlc,org.khanacademy.android,com.duolingo,vn.familytv.parentalcontrol"

        val COLUMNS = arrayOf(
            StatusProviderContract.COLUMN_CURRENT_MODE,
            StatusProviderContract.COLUMN_REMAINING_SECONDS,
            StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS,
            StatusProviderContract.COLUMN_WHITELIST_PACKAGES
        )

        fun buildCursor(
            mode: Mode,
            remainingSeconds: Long = DEFAULT_SESSION_LIMIT_SECONDS,
            sessionLimitSeconds: Long = DEFAULT_SESSION_LIMIT_SECONDS,
            whitelistPackages: String = DEFAULT_WHITELIST_PACKAGES,
            projection: Array<out String>? = null
        ): Cursor {
            return buildCursor(
                mode = mode.name,
                remainingSeconds = remainingSeconds,
                sessionLimitSeconds = sessionLimitSeconds,
                whitelistPackages = whitelistPackages,
                projection = projection
            )
        }

        fun buildCursor(
            mode: String,
            remainingSeconds: Long = DEFAULT_SESSION_LIMIT_SECONDS,
            sessionLimitSeconds: Long = DEFAULT_SESSION_LIMIT_SECONDS,
            whitelistPackages: String = DEFAULT_WHITELIST_PACKAGES,
            projection: Array<out String>? = null
        ): Cursor {
            val requestedCols = if (projection != null && projection.isNotEmpty()) {
                projection.map { it }.toTypedArray()
            } else {
                COLUMNS
            }

            val values: Array<Any?> = requestedCols.map<String, Any?> { col ->
                when (col) {
                    StatusProviderContract.COLUMN_CURRENT_MODE -> mode
                    StatusProviderContract.COLUMN_REMAINING_SECONDS -> remainingSeconds
                    StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS -> sessionLimitSeconds
                    StatusProviderContract.COLUMN_WHITELIST_PACKAGES -> whitelistPackages
                    else -> ""
                }
            }.toTypedArray()

            return try {
                MatrixCursor(requestedCols).apply { addRow(values) }
            } catch (_: Throwable) {
                StatusCursor(requestedCols, values)
            }
        }
    }

    override fun onCreate(): Boolean = true

    fun queryStatus(projection: Array<out String>? = null): Cursor {
        val manager = stateManager
        val mode = manager.getMode()
        val remainingSeconds = manager.getScreenTimeRemainingSec().toLong()
        return buildCursor(
            mode = mode,
            remainingSeconds = remainingSeconds,
            sessionLimitSeconds = DEFAULT_SESSION_LIMIT_SECONDS,
            whitelistPackages = DEFAULT_WHITELIST_PACKAGES,
            projection = projection
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor = queryStatus(projection)

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.familytv.status"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private class StatusCursor(
        private val cols: Array<String>,
        private val vals: Array<Any?>
    ) : Cursor by (Proxy.newProxyInstance(
        Cursor::class.java.classLoader,
        arrayOf(Cursor::class.java)
    ) { proxy, method, args ->
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
            "getString" -> vals[args[0] as Int]?.toString()
            "getInt" -> {
                val v = vals[args[0] as Int]
                when (v) {
                    is Number -> v.toInt()
                    else -> v?.toString()?.toIntOrNull() ?: 0
                }
            }
            "getLong" -> {
                val v = vals[args[0] as Int]
                when (v) {
                    is Number -> v.toLong()
                    else -> v?.toString()?.toLongOrNull() ?: 0L
                }
            }
            "isNull" -> vals[args[0] as Int] == null
            "close" -> Unit
            "isClosed" -> false
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> args?.get(0) === proxy
            "toString" -> "StatusCursor(cols=${cols.contentToString()})"
            else -> null
        }
    } as Cursor)
}
