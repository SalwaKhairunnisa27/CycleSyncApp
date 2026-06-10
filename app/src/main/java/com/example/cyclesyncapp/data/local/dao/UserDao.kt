package com.example.cyclesyncapp.data.local.dao

import androidx.room.*
import com.example.cyclesyncapp.data.local.entity.UserEntity

@Dao
interface UserDao {
    // Digunakan saat user pertama kali daftar (Onboarding)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Digunakan untuk menampilkan nama user di Dashboard
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Digunakan jika user ingin edit profil
    @Update
    suspend fun updateUser(user: UserEntity)
}