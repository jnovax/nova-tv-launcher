package vn.familytv.parentalcontrol.screentime

import java.util.Calendar

data class ScreenTimePolicy(
    val defaultWeekdayHours: Int = 2,
    val defaultWeekendHours: Int = 3,
    val defaultLimitSec: Int = 7200,
    val trackedPackages: Set<String> = DEFAULT_TRACKED_PACKAGES,
    val excludedPackages: Set<String> = DEFAULT_EXCLUDED_PACKAGES
) {
    companion object {
        val DEFAULT_TRACKED_PACKAGES: Set<String> = setOf(
            // YouTube variants (Stock YouTube, YouTube Kids, SmartTube, TizenTube Cobalt, NewPipe)
            "io.gh.reisxd.tizentube.cobalt",
            "org.smarttube.stable",
            "org.smarttube.beta",
            "com.teamsmart.videomanager.tv",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.videomanager",
            "com.google.android.youtube.tv",
            "com.google.android.youtube.tvkids",
            "com.google.android.youtube",
            "org.schabi.newpipe",
            // MyTV variants
            "vn.mytvnet.b2cott",
            "vn.mytvnet.b2c",
            "vn.vnpt.media.mytv",
            // Nova Video Player
            "org.courville.nova",
            // Spotify variants
            "com.spotify.tv.android",
            "com.spotify.music"
        )

        val DEFAULT_EXCLUDED_PACKAGES: Set<String> = setOf(
            "com.appletv.launcher",
            "vn.familytv.parentalcontrol",
            "vn.familytv.audit",
            "com.android.tv.settings",
            "com.android.systemui",
            "com.google.android.backdrop",
            "com.google.android.apps.tv.dreamx",
            "com.tcl.ambient"
        )
    }

    /**
     * Determines whether the foreground package should count toward daily screen time.
     * Only designated entertainment apps (YouTube, MyTV, Nova Video Player, Spotify) are tracked.
     * Screen savers, launchers, system UI, and other tools are never tracked.
     */
    fun isTrackedPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val pkg = packageName.trim()
        if (trackedPackages.contains(pkg)) return true
        if (pkg.startsWith("vn.mytvnet.")) return true
        if (pkg.startsWith("com.spotify.")) return true
        return false
    }

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
