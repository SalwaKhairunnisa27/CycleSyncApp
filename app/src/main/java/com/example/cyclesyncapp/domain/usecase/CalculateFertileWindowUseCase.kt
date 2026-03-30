package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class CalculateFertileWindowUseCase {

    fun calculateFertileWindow(nextPeriod: Calendar): Pair<Calendar, Calendar> {

        val ovulation = nextPeriod.clone() as Calendar
        ovulation.add(Calendar.DAY_OF_MONTH, -14)

        val fertileStart = ovulation.clone() as Calendar
        fertileStart.add(Calendar.DAY_OF_MONTH, -2)

        val fertileEnd = ovulation.clone() as Calendar
        fertileEnd.add(Calendar.DAY_OF_MONTH, 2)

        return Pair(fertileStart, fertileEnd)
    }
}