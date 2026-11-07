package vn.familytv.parentalcontrol.screentime

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class ScreenTimeStoreTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val storage = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(storage)

        override fun getString(key: String?, defValue: String?): String? =
            (storage[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (storage[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (storage[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (storage[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (storage[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (storage[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = storage.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val toRemove = mutableSetOf<String>()
            private var shouldClear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                key?.let { pending[it] = values }
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                key?.let { toRemove.add(it) }
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                shouldClear = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (shouldClear) target.clear()
                for (key in toRemove) {
                    target.remove(key)
                }
                target.putAll(pending)
            }
        }
    }

    private lateinit var store: ScreenTimeStore
    private lateinit var prefs: FakeSharedPreferences
    private val policy = ScreenTimePolicy()

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = ScreenTimeStore(prefs)
    }

    @Test
    fun testGetRemainingSecReturnsDefaultWhenUnset() {
        val weekdayCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4) }
        val weekendCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 7) }
        assertEquals(7200, store.getRemainingSec(weekdayCal, policy))
        assertEquals(10800, store.getRemainingSec(weekendCal, policy))
    }

    @Test
    fun testSetAndGetRemainingSec() {
        store.setRemainingSec(450)
        assertEquals(450, store.getRemainingSec(7200))
    }

    @Test
    fun testAddRemainingSecIncrements() {
        store.setRemainingSec(100)
        store.addRemainingSec(1800) // +30 minutes
        assertEquals(1900, store.getRemainingSec(0))
    }

    @Test
    fun testWeekdayAndWeekendHoursConfiguration() {
        assertEquals(2, store.getWeekdayHours())
        assertEquals(3, store.getWeekendHours())

        store.setWeekdayHours(1)
        store.setWeekendHours(4)

        assertEquals(1, store.getWeekdayHours())
        assertEquals(4, store.getWeekendHours())

        // Coerce 1..4 bounds
        store.setWeekdayHours(0)
        store.setWeekendHours(5)
        assertEquals(1, store.getWeekdayHours())
        assertEquals(4, store.getWeekendHours())

        // Test getDailyLimitHours
        val weekdayCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4) }
        val weekendCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 7) }
        assertEquals(1, store.getDailyLimitHours(weekdayCal, policy))
        assertEquals(4, store.getDailyLimitHours(weekendCal, policy))
    }

    @Test
    fun testCheckAndResetDayRollover() {
        val calendar = Calendar.getInstance()
        // Wednesday (weekday)
        calendar.set(2026, Calendar.NOVEMBER, 4) // Wednesday
        store.setWeekdayHours(2) // 2h = 7200s

        val reset = store.checkAndResetDay(calendar, policy)
        assertTrue(reset)
        assertEquals(7200, store.getRemainingSec(calendar, policy))
        assertEquals("2026-11-04", store.getLastTrackedDate())

        // Calling again on the same day should NOT reset
        val secondCall = store.checkAndResetDay(calendar, policy)
        assertFalse(secondCall)

        // Advance to Saturday (weekend)
        calendar.set(2026, Calendar.NOVEMBER, 7) // Saturday
        store.setWeekendHours(4) // 4h = 14400s
        val weekendReset = store.checkAndResetDay(calendar, policy)
        assertTrue(weekendReset)
        assertEquals(14400, store.getRemainingSec(calendar, policy))
        assertEquals("2026-11-07", store.getLastTrackedDate())
    }

    @Test
    fun testChangingLimitPreservesUsedTimeAndRecalculatesRemaining() {
        val calendar = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4) } // Wednesday (weekday)
        store.setWeekdayHours(1)
        store.setUsedSec(1800) // 30 minutes used

        // 1h limit -> 30m used -> 30m remaining
        assertEquals(1800, store.getUsedSec())
        assertEquals(3600, store.getTotalLimitSec(calendar, policy))
        assertEquals(1800, store.getRemainingSec(calendar, policy))

        // Change to 2h -> used remains 30m -> remaining becomes 90m (5400s)
        store.setWeekdayHours(2)
        assertEquals(1800, store.getUsedSec())
        assertEquals(7200, store.getTotalLimitSec(calendar, policy))
        assertEquals(5400, store.getRemainingSec(calendar, policy))

        // Change to 3h -> used remains 30m -> remaining becomes 150m (9000s)
        store.setWeekdayHours(3)
        assertEquals(1800, store.getUsedSec())
        assertEquals(10800, store.getTotalLimitSec(calendar, policy))
        assertEquals(9000, store.getRemainingSec(calendar, policy))

        // Change to 4h -> used remains 30m -> remaining becomes 210m (12600s)
        store.setWeekdayHours(4)
        assertEquals(1800, store.getUsedSec())
        assertEquals(14400, store.getTotalLimitSec(calendar, policy))
        assertEquals(12600, store.getRemainingSec(calendar, policy))
    }

    @Test
    fun testClearRemovesPersistedData() {
        val weekdayCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4) }
        store.setRemainingSec(450)
        store.setWeekdayHours(1)
        store.setLastTrackedDate("2026-11-06")

        store.clear()

        assertEquals(7200, store.getRemainingSec(weekdayCal, policy))
        assertEquals(2, store.getWeekdayHours())
    }
}
