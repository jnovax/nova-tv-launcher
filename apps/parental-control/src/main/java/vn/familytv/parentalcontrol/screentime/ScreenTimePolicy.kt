package vn.familytv.parentalcontrol.screentime

import java.util.Calendar

data class ScreenTimePolicy(
    val defaultWeekdayHours: Int = 2,
    val defaultWeekendHours: Int = 3,
    val defaultLimitSec: Int = 7200,
    val excludedPackages: Set<String> = setOf(
        "com.appletv.launcher",
        "vn.familytv.parentalcontrol",
        "vn.familytv.audit",
        "com.android.tv.settings",
        "com.android.systemui"
    )
) {
    fun isWeekend(calendar: Calendar = Calendar.getInstance()): Boolean {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    fun getLimitSecForDay(
        isWeekend: Boolean,
        weekdayHours: Int = defaultWeekdayHours,
        weekendHours: Int = defaultWeekendHours
    ): Int {
        val hours = if (isWeekend) weekendHours else weekdayHours
        return hours.coerceIn(1, 4) * 3600
    }
}
