package com.example.cyclesyncapp.domain.repository

import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    // Data Siklus
    fun getAllCycles(): Flow<List<CycleEntity>>
    suspend fun insertCycle(cycle: CycleEntity)
    suspend fun deleteCycle(cycle: CycleEntity)

    // Data Artikel Edukasi (Pre-populated dari education.db)
    fun getAllArticles(): Flow<List<EducationEntity>>
    fun getArticlesByPhase(phase: String): Flow<List<EducationEntity>>

    // Data Log Harian
    suspend fun insertDailyLog(log: DailyLogEntity)
    fun getLogByDate(date: String): Flow<DailyLogEntity?>
    fun getAllDailyLogs(): Flow<List<DailyLogEntity>>
}