package com.example.cyclesyncapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_table")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: String,
    val endDate: String,
    val cycleLength: Int,
    val periodLength: Int,
    val notes: String? = null
)