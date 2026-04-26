package com.example.cyclesyncapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import kotlinx.coroutines.flow.Flow // Tambahkan import ini

@Dao
interface EducationDao {
    @Query("SELECT * FROM articles")
    fun getAllArticles(): Flow<List<EducationEntity>> // Ubah jadi Flow

    @Query("SELECT * FROM articles WHERE phase_recom = :phase")
    fun getArticlesByPhase(phase: String): Flow<List<EducationEntity>> // Ubah jadi Flow
}