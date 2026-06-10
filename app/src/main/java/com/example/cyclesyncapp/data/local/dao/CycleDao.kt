package com.example.cyclesyncapp.data.local.dao

import androidx.room.*
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycles(cycles: List<CycleEntity>)

    @Query("SELECT * FROM cycle_table ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycle_table ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestCycle(): CycleEntity?

    @Delete
    suspend fun deleteCycle(cycle: CycleEntity)

    @Query("DELETE FROM cycle_table")
    suspend fun clearCycles()
}