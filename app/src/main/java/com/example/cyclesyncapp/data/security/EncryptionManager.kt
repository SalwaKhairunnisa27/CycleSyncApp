package com.example.cyclesyncapp.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

object EncryptionManager {
    private const val PREFS_NAME = "cyclesync_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_key"

    fun getPassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context, PREFS_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existingKey = prefs.getString(KEY_DB_PASSPHRASE, null)
        return if (existingKey != null) {
            Base64.decode(existingKey, Base64.DEFAULT)
        } else {
            val newKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            val encoded = Base64.encodeToString(newKey, Base64.DEFAULT)
            prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
            newKey
        }
    }
}