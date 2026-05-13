package com.example.cyclesyncapp.data.local.dao

import androidx.room.*
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity)

    @Query("SELECT * FROM cycle_table ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    @Delete
    suspend fun deleteCycle(cycle: CycleEntity)
}