package com.example.cyclesyncapp.domain.repository

import com.example.cyclesyncapp.data.local.entity.UserEntity

interface UserRepository {
    suspend fun insertUser(user: UserEntity)
    suspend fun getUser(): UserEntity?
    suspend fun getUserByEmail(email: String): UserEntity?
    suspend fun updateUser(user: UserEntity)
}
