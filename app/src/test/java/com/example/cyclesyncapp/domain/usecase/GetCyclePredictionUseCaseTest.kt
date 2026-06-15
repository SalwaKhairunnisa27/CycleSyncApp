package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.model.HormonalPhase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GetCyclePredictionUseCaseTest {

    private val useCase = GetCyclePredictionUseCase()

    @Test
    fun `Hari ke-3 harus terdeteksi sebagai fase MENSTRUATION`() {
        // Setup: LMP 2 hari yang lalu = Hari ke-3
        val lmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }.timeInMillis
        val prevLmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis

        val result = useCase.execute(listOf(prevLmp, lmp), false)
        
        assertEquals(HormonalPhase.MENSTRUATION, result.currentPhase)
    }

    @Test
    fun `Hari ke-14 pada siklus 28 hari harus terdeteksi sebagai fase OVULATION`() {
        // Setup: LMP 13 hari yang lalu = Hari ke-14
        val lmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }.timeInMillis
        val prevLmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -41) }.timeInMillis

        val result = useCase.execute(listOf(prevLmp, lmp), false)
        
        assertEquals(HormonalPhase.OVULATION, result.currentPhase)
    }

    @Test
    fun `Siklus panjang 35 hari hari ke-21 harus terdeteksi sebagai fase OVULATION`() {
        // Setup: LMP 20 hari yang lalu = Hari ke-21 (35-14 = 21)
        val lmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -20) }.timeInMillis
        val prevLmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -55) }.timeInMillis

        val result = useCase.execute(listOf(prevLmp, lmp), false)
        
        assertEquals(HormonalPhase.OVULATION, result.currentPhase)
    }

    @Test
    fun `Mode kehamilan yang aktif harus mengembalikan fase PREGNANCY`() {
        val lmp = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -45) }.timeInMillis
        val result = useCase.execute(listOf(lmp), true)
        
        assertEquals(HormonalPhase.PREGNANCY, result.currentPhase)
    }
}

