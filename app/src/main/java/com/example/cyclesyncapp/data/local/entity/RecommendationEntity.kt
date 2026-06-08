package com.example.cyclesyncapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phase: String,       // MENSTRUATION, FOLLICULAR, OVULATION, LUTEAL
    val category: String,    // Contoh: "Sayuran", "Kardio", "Nutrisi"
    val title: String,       // Buat Nama makanan/olahraga
    val description: String, // Penjelasan singkat
    val benefit: String,     // Manfaat untuk fase tersebut
    val type: String         // FOOD atau EXERCISE
)