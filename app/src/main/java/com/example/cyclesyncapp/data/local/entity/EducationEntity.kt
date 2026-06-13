package com.example.cyclesyncapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class EducationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // Tambah ? supaya NotNull=false
    val title: String?,
    val content: String?,
    val category: String?,
    @ColumnInfo(name = "phase_recom")
    val phaseRecom: String?
)