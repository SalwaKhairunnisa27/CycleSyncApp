package com.example.cyclesyncapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("SELECT * FROM recommendations WHERE phase = :phase AND type = :type")
    fun getRecommendationsByPhaseAndType(phase: String, type: String): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE phase = :phase")
    fun getAllRecommendationsByPhase(phase: String): Flow<List<RecommendationEntity>>

    @Query("DELETE FROM recommendations")
    suspend fun clearRecommendations()
}