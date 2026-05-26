package com.example.cyclesyncapp.data.repository

import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import com.example.cyclesyncapp.data.security.EncryptionManager // Import ini
import com.example.cyclesyncapp.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow

class CycleRepositoryImpl(
    private val database: CycleDatabase
) : CycleRepository {

    override fun getAllCycles(): Flow<List<CycleEntity>> = database.cycleDao().getAllCycles()
    override suspend fun insertCycle(cycle: CycleEntity) = database.cycleDao().insertCycle(cycle)
    override suspend fun deleteCycle(cycle: CycleEntity) = database.cycleDao().deleteCycle(cycle)

    override fun getAllArticles(): Flow<List<EducationEntity>> = database.educationDao().getAllArticles()
    override fun getArticlesByPhase(phase: String): Flow<List<EducationEntity>> = database.educationDao().getArticlesByPhase(phase)

    override suspend fun insertDailyLog(log: DailyLogEntity) = database.dailyLogDao().insertLog(log)
    override fun getLogByDate(date: String): Flow<DailyLogEntity?> = database.dailyLogDao().getLogByDate(date)
    override fun getAllDailyLogs(): Flow<List<DailyLogEntity>> = database.dailyLogDao().getAllLogs()

    // --- LOGIKA ENKRIPSI ARINI ---
    override suspend fun insertEncryptedLog(date: String, note: String, phase: String) {
        // 1. Enkripsi teks catatan (note) menjadi teks acak
        val secureNote = EncryptionManager.encrypt(note)

        // 2. Bungkus ke dalam Entity
        val logEntity = DailyLogEntity(
            date = date,
            encryptedNote = secureNote,
            phase = phase
        )

        // 3. Simpan ke Database via Dao
        database.dailyLogDao().insertLog(logEntity)
    }
}
