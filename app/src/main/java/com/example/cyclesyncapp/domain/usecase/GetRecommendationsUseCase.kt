package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import com.example.cyclesyncapp.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow

class GetRecommendationsUseCase(
    private val repository: RecommendationRepository
) {
    operator fun invoke(phase: String): Flow<List<RecommendationEntity>> {
        // Di sini kamu bisa menambahkan logika tambahan jika perlu,
        // tapi untuk sekarang kita ambil langsung dari repository berdasarkan fase.
        return repository.getRecommendationsByPhase(phase)
    }
}
