package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class CalculateHPLUseCase {

    fun calculateHPL(lastMenstrualDate: Calendar): Calendar {
        val result = lastMenstrualDate.clone() as Calendar

        result.add(Calendar.DAY_OF_MONTH, 7)
        result.add(Calendar.MONTH, -3)
        result.add(Calendar.YEAR, 1)

        return result
    }
}