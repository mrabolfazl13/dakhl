package ir.dakhlokharj.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dakhlo_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.CURRENCY] = updated.currencyUnit.name
            prefs[Keys.HIDE_BALANCES] = updated.hideBalances
            prefs[Keys.APP_LOCK] = updated.appLockEnabled
            prefs[Keys.BIOMETRIC] = updated.biometricEnabled
            updated.pinHash?.let { prefs[Keys.PIN_HASH] = it } ?: prefs.remove(Keys.PIN_HASH)
            prefs[Keys.SCREENSHOT_PROTECTION] = updated.screenshotProtection
            prefs[Keys.REMINDER_ENABLED] = updated.reminderEnabled
            prefs[Keys.REMINDER_HOUR] = updated.dailyReminderHour
            prefs[Keys.REMINDER_MINUTE] = updated.dailyReminderMinute
            updated.defaultAccountId?.let { prefs[Keys.DEFAULT_ACCOUNT] = it }
                ?: prefs.remove(Keys.DEFAULT_ACCOUNT)
            prefs[Keys.DASHBOARD_ORDER] = updated.dashboardOrder
            prefs[Keys.ONBOARDING] = updated.onboardingDone
            prefs[Keys.LANGUAGE] = updated.languageTag
        }
    }

    suspend fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    suspend fun setCurrency(unit: CurrencyUnit) = update { it.copy(currencyUnit = unit) }

    suspend fun setHideBalances(hidden: Boolean) = update { it.copy(hideBalances = hidden) }

    suspend fun setScreenshotProtection(enabled: Boolean) = update { it.copy(screenshotProtection = enabled) }

    suspend fun setReminder(enabled: Boolean, hour: Int = 21, minute: Int = 0) =
        update { it.copy(reminderEnabled = enabled, dailyReminderHour = hour, dailyReminderMinute = minute) }

    suspend fun setDefaultAccount(id: Long?) = update { it.copy(defaultAccountId = id) }

    suspend fun setDashboardOrder(order: String) = update { it.copy(dashboardOrder = order) }

    suspend fun setPinHash(hash: String?) = update { it.copy(pinHash = hash) }

    suspend fun setAppLock(enabled: Boolean) = update { it.copy(appLockEnabled = enabled) }

    suspend fun setBiometric(enabled: Boolean) = update { it.copy(biometricEnabled = enabled) }

    suspend fun setOnboardingDone(done: Boolean) = update { it.copy(onboardingDone = done) }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            themeMode = this[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            currencyUnit = this[Keys.CURRENCY]?.let { runCatching { CurrencyUnit.valueOf(it) }.getOrNull() } ?: defaults.currencyUnit,
            hideBalances = this[Keys.HIDE_BALANCES] ?: defaults.hideBalances,
            appLockEnabled = this[Keys.APP_LOCK] ?: defaults.appLockEnabled,
            biometricEnabled = this[Keys.BIOMETRIC] ?: defaults.biometricEnabled,
            pinHash = this[Keys.PIN_HASH],
            screenshotProtection = this[Keys.SCREENSHOT_PROTECTION] ?: defaults.screenshotProtection,
            reminderEnabled = this[Keys.REMINDER_ENABLED] ?: defaults.reminderEnabled,
            dailyReminderHour = this[Keys.REMINDER_HOUR] ?: defaults.dailyReminderHour,
            dailyReminderMinute = this[Keys.REMINDER_MINUTE] ?: defaults.dailyReminderMinute,
            defaultAccountId = this[Keys.DEFAULT_ACCOUNT],
            dashboardOrder = this[Keys.DASHBOARD_ORDER] ?: defaults.dashboardOrder,
            onboardingDone = this[Keys.ONBOARDING] ?: defaults.onboardingDone,
            languageTag = this[Keys.LANGUAGE] ?: defaults.languageTag,
        )
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency_unit")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val DEFAULT_ACCOUNT = longPreferencesKey("default_account")
        val DASHBOARD_ORDER = stringPreferencesKey("dashboard_order")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
    }
}
