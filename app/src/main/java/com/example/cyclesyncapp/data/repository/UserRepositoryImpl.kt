package com.example.cyclesyncapp.data.repository

import com.example.cyclesyncapp.data.local.dao.UserDao
import com.example.cyclesyncapp.data.local.entity.UserEntity
import com.example.cyclesyncapp.domain.repository.UserRepository

class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    override suspend fun getUser(): UserEntity? {
        return userDao.getUser()
    }

    override suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }
}
