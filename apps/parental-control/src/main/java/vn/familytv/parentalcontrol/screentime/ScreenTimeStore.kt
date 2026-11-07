package vn.familytv.parentalcontrol.screentime

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScreenTimeStore(private val prefs: SharedPreferences) {
    companion object {
        private const val KEY_USED_SEC = "key_used_sec"
        private const val KEY_EXTENSION_SEC = "key_extension_sec"
        private const val KEY_WEEKDAY_HOURS = "key_weekday_hours"
        private const val KEY_WEEKEND_HOURS = "key_weekend_hours"
        private const val KEY_LAST_TRACKED_DATE = "key_last_tracked_date"

        fun formatDate(date: Date = Date()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(date)
        }
    }

    fun getWeekdayHours(): Int = prefs.getInt(KEY_WEEKDAY_HOURS, 2)
    fun setWeekdayHours(hours: Int) {
        prefs.edit().putInt(KEY_WEEKDAY_HOURS, hours.coerceIn(1, 4)).apply()
    }

    fun getWeekendHours(): Int = prefs.getInt(KEY_WEEKEND_HOURS, 3)
    fun setWeekendHours(hours: Int) {
        prefs.edit().putInt(KEY_WEEKEND_HOURS, hours.coerceIn(1, 4)).apply()
    }

    fun getDailyLimitHours(
        calendar: Calendar = Calendar.getInstance(),
        policy: ScreenTimePolicy = ScreenTimePolicy()
    ): Int {
        val isWeekend = policy.isWeekend(calendar)
        return if (isWeekend) getWeekendHours() else getWeekdayHours()
    }

    fun getLastTrackedDate(): String? = prefs.getString(KEY_LAST_TRACKED_DATE, null)
    fun setLastTrackedDate(dateStr: String) {
        prefs.edit().putString(KEY_LAST_TRACKED_DATE, dateStr).apply()
    }

    fun getUsedSec(): Int = prefs.getInt(KEY_USED_SEC, 0)
    fun setUsedSec(sec: Int) {
        prefs.edit().putInt(KEY_USED_SEC, sec.coerceAtLeast(0)).apply()
    }

    fun getExtensionSec(): Int = prefs.getInt(KEY_EXTENSION_SEC, 0)
    fun setExtensionSec(sec: Int) {
        prefs.edit().putInt(KEY_EXTENSION_SEC, sec.coerceAtLeast(0)).apply()
    }

    fun addExtensionSec(addedSec: Int) {
        setExtensionSec(getExtensionSec() + addedSec)
    }

    /**
     * Total allowed seconds for today = base daily limit + parent extension
     */
    fun getTotalLimitSec(
        calendar: Calendar = Calendar.getInstance(),
        policy: ScreenTimePolicy = ScreenTimePolicy()
    ): Int {
        return getDailyLimitHours(calendar, policy) * 3600 + getExtensionSec()
    }

    /**
     * Remaining seconds = (Total Limit - Used).coerceAtLeast(0)
     */
    fun getRemainingSec(
        calendar: Calendar = Calendar.getInstance(),
        policy: ScreenTimePolicy = ScreenTimePolicy()
    ): Int {
        return (getTotalLimitSec(calendar, policy) - getUsedSec()).coerceAtLeast(0)
    }

    // Overload with defaultSec for backward compatibility
    fun getRemainingSec(defaultSec: Int): Int {
        return (getTotalLimitSec() - getUsedSec()).coerceAtLeast(0)
    }

    fun setRemainingSec(remainingSec: Int) {
        val total = getTotalLimitSec()
        setUsedSec((total - remainingSec).coerceAtLeast(0))
    }

    fun addRemainingSec(addedSec: Int) {
        addExtensionSec(addedSec)
    }

    fun checkAndResetDay(calendar: Calendar, policy: ScreenTimePolicy): Boolean {
        val todayStr = formatDate(calendar.time)
        val lastDate = getLastTrackedDate()
        if (lastDate != todayStr) {
            setLastTrackedDate(todayStr)
            setUsedSec(0)
            setExtensionSec(0)
            return true
        }
        return false
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
