package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

enum class PregnancyPhase {
    NOT_PREGNANT,
    FIRST_TRIMESTER,
    SECOND_TRIMESTER,
    THIRD_TRIMESTER
}

class PregnancyTransitionUseCase {

    fun getPregnancyPhase(lmp: Calendar, currentDate: Calendar): PregnancyPhase {

        val diffInMillis = currentDate.timeInMillis - lmp.timeInMillis
        val weeks = (diffInMillis / (1000 * 60 * 60 * 24)) / 7

        return when {
            weeks < 0 -> PregnancyPhase.NOT_PREGNANT
            weeks <= 12 -> PregnancyPhase.FIRST_TRIMESTER
            weeks <= 27 -> PregnancyPhase.SECOND_TRIMESTER
            weeks <= 40 -> PregnancyPhase.THIRD_TRIMESTER
            else -> PregnancyPhase.NOT_PREGNANT
        }
    }
}