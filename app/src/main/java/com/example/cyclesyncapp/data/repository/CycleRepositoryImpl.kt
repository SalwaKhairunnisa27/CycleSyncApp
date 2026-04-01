package com.example.cyclesyncapp.data.repository

import com.example.cyclesyncapp.data.local.dao.CycleDao
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow

class CycleRepositoryImpl(
    private val cycleDao: CycleDao
) : CycleRepository {

    override fun getAllCycles(): Flow<List<CycleEntity>> {
        return cycleDao.getAllCycles()
    }

    override suspend fun insertCycle(cycle: CycleEntity) {
        cycleDao.insertCycle(cycle)
    }

    override suspend fun deleteCycle(cycle: CycleEntity) {
        cycleDao.deleteCycle(cycle)
    }
}