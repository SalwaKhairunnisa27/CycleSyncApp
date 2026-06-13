package com.example.cyclesyncapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,      // Format: yyyy-MM-dd
    val flowLevel: String? = null, // "None", "Light", "Medium", "Heavy"
    val symptoms: String? = null,   // Comma separated symptoms
    val encryptedNote: String, // Teks acak hasil enkripsi
    val phase: String      // Fase saat catatan dibuat
)