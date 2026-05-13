package com.example.cyclesyncapp.data.repository

import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import com.example.cyclesyncapp.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow

class CycleRepositoryImpl(
    private val database: CycleDatabase
) : CycleRepository {

    // Implementasi Siklus
    override fun getAllCycles(): Flow<List<CycleEntity>> = database.cycleDao().getAllCycles()
    override suspend fun insertCycle(cycle: CycleEntity) = database.cycleDao().insertCycle(cycle)
    override suspend fun deleteCycle(cycle: CycleEntity) = database.cycleDao().deleteCycle(cycle)

    // Implementasi Artikel (Mengambil dari EducationDao)
    override fun getAllArticles(): Flow<List<EducationEntity>> =
        database.educationDao().getAllArticles()

    override fun getArticlesByPhase(phase: String): Flow<List<EducationEntity>> =
        database.educationDao().getArticlesByPhase(phase)

    // Implementasi Log Harian (Mengambil dari DailyLogDao)
    override suspend fun insertDailyLog(log: DailyLogEntity) =
        database.dailyLogDao().insertLog(log)

    override fun getLogByDate(date: String): Flow<DailyLogEntity?> =
        database.dailyLogDao().getLogByDate(date)

    override fun getAllDailyLogs(): Flow<List<DailyLogEntity>> =
        database.dailyLogDao().getAllLogs()
}