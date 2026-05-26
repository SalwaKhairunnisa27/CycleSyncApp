package com.example.cyclesyncapp.domain.repository

import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun getRecommendationsByPhase(phase: String): Flow<List<RecommendationEntity>>
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)
    suspend fun clearRecommendations()
    suspend fun seedDatabase() // Tambahkan fungsi ini
}