/**
 * DeviceIdentityManager.kt
 *
 * Purpose: Manages unique device identification without requiring username or password authentication.
 * Guarantees persistent device identifier across app launches and sessions.
 */
package com.example.solace_temp.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceIdentityManager {

    private const val PREFS_NAME = "solace_device_identity"
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * Retrieves or generates a persistent unique device identifier.
     * Uses [Settings.Secure.ANDROID_ID] if available, or a persisted UUID in SharedPreferences as fallback.
     *
     * @param context The application context.
     * @return The unique device identifier.
     */
    @SuppressLint("HardwareIds")
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_DEVICE_ID, null)
        if (!savedId.isNullOrBlank()) {
            return savedId
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            "device_${UUID.randomUUID()}"
        }

        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    /**
     * Clears stored device identity (used primarily in testing to simulate fresh devices).
     *
     * @param context The application context.
     */
    fun clearDeviceId(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DEVICE_ID).apply()
    }
}
