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
        val lmp = Calendar.getInstance()
        lmp.add(Calendar.DAY_OF_YEAR, -2)

        val result = useCase.execute(lmp, listOf(28, 28))
        
        assertEquals(HormonalPhase.MENSTRUATION, result.currentPhase)
    }

    @Test
    fun `Hari ke-14 pada siklus 28 hari harus terdeteksi sebagai fase OVULATION`() {
        // Setup: LMP 13 hari yang lalu = Hari ke-14
        val lmp = Calendar.getInstance()
        lmp.add(Calendar.DAY_OF_YEAR, -13)

        val result = useCase.execute(lmp, listOf(28, 28))
        
        assertEquals(HormonalPhase.OVULATION, result.currentPhase)
    }

    @Test
    fun `Siklus panjang 35 hari hari ke-21 harus terdeteksi sebagai fase OVULATION`() {
        // Setup: LMP 20 hari yang lalu = Hari ke-21 (35-14 = 21)
        val lmp = Calendar.getInstance()
        lmp.add(Calendar.DAY_OF_YEAR, -20)

        val result = useCase.execute(lmp, listOf(35, 35))
        
        assertEquals(HormonalPhase.OVULATION, result.currentPhase)
    }

    @Test
    fun `Telat haid lebih dari 14 hari harus otomatis mode PREGNANCY`() {
        // Setup: LMP sudah lewat 45 hari (Siklus 28 + 14 = 42)
        val lmp = Calendar.getInstance()
        lmp.add(Calendar.DAY_OF_YEAR, -45)

        val result = useCase.execute(lmp, listOf(28, 28))
        
        assertEquals(HormonalPhase.PREGNANCY, result.currentPhase)
    }
}
