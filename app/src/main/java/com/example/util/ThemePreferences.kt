package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "netkas_ui_prefs")

enum class AppThemeMode(val code: String, val labelId: String) {
    SYSTEM("system", "Otomatis"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap");

    fun nextMode(): AppThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }

    companion object {
        fun fromCode(code: String?): AppThemeMode = when (code) {
            LIGHT.code -> LIGHT
            DARK.code -> DARK
            else -> SYSTEM
        }
    }
}

object ThemePreferences {
    private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")

    fun themeModeFlow(context: Context): Flow<AppThemeMode> =
        context.applicationContext.themeDataStore.data.map { prefs ->
            AppThemeMode.fromCode(prefs[KEY_THEME_MODE])
        }

    suspend fun setThemeMode(context: Context, mode: AppThemeMode) {
        context.applicationContext.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.code
        }
    }
}
