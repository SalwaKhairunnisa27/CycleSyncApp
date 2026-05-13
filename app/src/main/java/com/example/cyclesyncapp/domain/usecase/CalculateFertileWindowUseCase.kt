package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class CalculateFertileWindowUseCase {

    fun calculateFertileWindow(nextPeriod: Calendar): Pair<Calendar, Calendar> {
        // Ovulation biasanya 14 hari sebelum menstruasi berikutnya
        val ovulation = nextPeriod.clone() as Calendar
        ovulation.add(Calendar.DAY_OF_MONTH, -14)

        // Masa subur: 5 hari sebelum ovulasi
        val fertileStart = ovulation.clone() as Calendar
        fertileStart.add(Calendar.DAY_OF_MONTH, -5)

        // Sampai 1 hari setelah ovulasi
        val fertileEnd = ovulation.clone() as Calendar
        fertileEnd.add(Calendar.DAY_OF_MONTH, 1)

        return Pair(fertileStart, fertileEnd)
    }
}