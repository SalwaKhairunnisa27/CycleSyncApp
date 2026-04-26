package com.example.cyclesyncapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,    // Format: YYYY-MM-DD
    val mood: String,
    val symptoms: String,
    val notes: String?
)