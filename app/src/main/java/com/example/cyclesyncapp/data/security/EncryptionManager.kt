package com.example.cyclesyncapp.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {

    private const val PREF_NAME = "secure_cycle_sync_prefs"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "CycleSyncEncryptionKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    // --- BAGIAN 1: SharedPreferences Terenkripsi (Untuk Data Ringan/Key-Value) ---
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context, PREF_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- BAGIAN 2: Enkripsi String Umum (Untuk Database/Room) ---

    // Fungsi untuk mengubah teks biasa menjadi teks acak (Enkripsi)
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // Initialization Vector (kunci tambahan agar enkripsi unik)
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Gabungkan IV dan data terenkripsi lalu ubah ke Base64 agar bisa disimpan sebagai String
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    // Fungsi untuk mengubah teks acak kembali ke teks asli (Dekripsi)
    fun decrypt(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        val iv = combined.sliceArray(0 until 12) // GCM IV standar adalah 12 bytes
        val encryptedData = combined.sliceArray(12 until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        return String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }

    // Helper: Mengambil atau membuat kunci enkripsi di dalam Android Keystore (Sangat Aman)
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}