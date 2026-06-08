package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class CalculateHPLUseCase {

    /**
     * Menghitung Hari Perkiraan Lahir menggunakan Rumus Naegele:
     * (HPHT + 7 Hari) - 3 Bulan + 1 Tahun
     */
    fun execute(lastMenstrualDate: Calendar): Long {
        val hpl = lastMenstrualDate.clone() as Calendar

        hpl.add(Calendar.DAY_OF_MONTH, 7)
        hpl.add(Calendar.MONTH, -3)
        hpl.add(Calendar.YEAR, 1)

        return hpl.timeInMillis
    }
}