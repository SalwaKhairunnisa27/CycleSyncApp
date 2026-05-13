package com.example.cyclesyncapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val age: Int?,
    val weight: Double?,
    val height: Double?,
    val cycleLength: Int? // Lama siklus (rata-rata 28 hari)
)