package com.example.cyclesyncapp.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptionManager {

    private const val PREF_NAME = "secure_cycle_sync_prefs"

    // Fungsi untuk membuat SharedPreferences yang terenkripsi otomatis
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Contoh cara simpan data sensitif (misal: Token haid)
    fun saveSecretData(context: Context, key: String, value: String) {
        getEncryptedPrefs(context).edit().putString(key, value).apply()
    }

    // Contoh cara ambil data sensitif
    fun getSecretData(context: Context, key: String): String? {
        return getEncryptedPrefs(context).getString(key, null)
    }
}