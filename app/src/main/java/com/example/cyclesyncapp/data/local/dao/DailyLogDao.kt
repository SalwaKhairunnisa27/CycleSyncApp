package com.example.cyclesyncapp.data.local.dao

import androidx.room.*
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLogEntity)

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun getLogByDate(date: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE flowLevel IS NOT NULL AND flowLevel != 'None' ORDER BY date DESC")
    suspend fun getPeriodLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    suspend fun getAllLogsList(): List<DailyLogEntity>

    @Delete
    suspend fun deleteLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs")
    suspend fun clearLogs()
}