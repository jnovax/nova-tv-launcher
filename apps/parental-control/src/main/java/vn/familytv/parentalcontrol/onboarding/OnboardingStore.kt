package vn.familytv.parentalcontrol.onboarding

import android.content.Context

class OnboardingStore(context: Context) {
    private val preferences = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun load(): InitialSetupStep = InitialSetupStep.fromStored(preferences.getString("step", null))

    fun save(step: InitialSetupStep) {
        preferences.edit().putString("step", step.name).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
