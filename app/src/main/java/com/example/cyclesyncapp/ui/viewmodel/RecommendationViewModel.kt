package com.example.cyclesyncapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import com.example.cyclesyncapp.data.repository.RecommendationRepositoryImpl
import com.example.cyclesyncapp.domain.usecase.GetRecommendationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel buat mengelola data Smart Food & Exercise.
 */
class RecommendationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecommendationRepositoryImpl
    private val getRecommendationsUseCase: GetRecommendationsUseCase

    // StateFlow untuk daftar rekomendasi agar UI bisa memantau perubahan secara real-time
    private val _recommendations = MutableStateFlow<List<RecommendationEntity>>(emptyList())
    val recommendations: StateFlow<List<RecommendationEntity>> = _recommendations.asStateFlow()

    // State untuk fase yang sedang aktif (Default: MENSTRUATION)
    private val _currentPhase = MutableStateFlow("MENSTRUATION")
    val currentPhase: StateFlow<String> = _currentPhase.asStateFlow()

    init {
        // 1. Inisialisasi Database
        val database = CycleDatabase.getDatabase(application)

        // 2. Inisialisasi Repository
        repository = RecommendationRepositoryImpl(database.recommendationDao())

        // 3. Inisialisasi UseCase
        getRecommendationsUseCase = GetRecommendationsUseCase(repository)

        // 4. Jalankan Seed Database (Mengisi data awal otomatis)
        viewModelScope.launch {
            repository.seedDatabase()
            // Muat data default untuk fase awal
            loadRecommendations("MENSTRUATION")
        }
    }

    /**
     * ini buat mmperbarui daftar rekomendasi berdasarkan fase hormonal.
     */
    fun loadRecommendations(phase: String) {
        _currentPhase.value = phase
        viewModelScope.launch {
            getRecommendationsUseCase(phase).collect { result ->
                _recommendations.value = result
            }
        }
    }

    /**
     * Helper buat memfilter data berdasarkan kategori (FOOD / EXERCISE)
     */
    fun getFilteredRecommendations(type: String): List<RecommendationEntity> {
        return _recommendations.value.filter { it.type == type }
    }
}
