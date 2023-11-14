package org.mg.iap

import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object SettingsManager {
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(ContextProvider.context)
    }
    private const val AUTH_STATUS_KEY = "key_auth_status"
    private const val SYNC_BLACK_TIME = "key_b_sync_tm"

    fun setAuthStatus(needAuth: Boolean) {
        LogUtils.d("setAuthStatus: $needAuth")
        val editor = preferences.edit()
        editor.putBoolean(AUTH_STATUS_KEY, needAuth)
        editor.apply()
    }

    fun getAuthStatus(): Boolean {
        return preferences.getBoolean(AUTH_STATUS_KEY, true)
    }

    fun getLastSyncTime(): Long {
        return preferences.getLong(SYNC_BLACK_TIME, 0)
    }

    fun setLastSyncTime(tm: Long) {
        val editor = preferences.edit()
        editor.putLong(SYNC_BLACK_TIME, tm)
        editor.apply()
    }
}