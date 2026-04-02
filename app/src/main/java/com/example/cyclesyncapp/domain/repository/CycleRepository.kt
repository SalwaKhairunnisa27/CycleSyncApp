package com.example.cyclesyncapp.domain.repository

import com.example.cyclesyncapp.data.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    // Fungsi untuk mengambil semua data haid (menggunakan Flow agar otomatis update ke UI)
    fun getAllCycles(): Flow<List<CycleEntity>>

    // Fungsi untuk menyimpan data haid baru (pakai suspend karena operasi database berat)
    suspend fun insertCycle(cycle: CycleEntity)

    // Fungsi untuk menghapus data haid
    suspend fun deleteCycle(cycle: CycleEntity)
}