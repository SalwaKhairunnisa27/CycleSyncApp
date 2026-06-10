package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.repository.UserRepository
import java.util.Calendar

enum class PregnancyPhase {
    NOT_PREGNANT,
    FIRST_TRIMESTER,
    SECOND_TRIMESTER,
    THIRD_TRIMESTER
}

class PregnancyTransitionUseCase(
    private val userRepository: UserRepository
) {

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

    suspend fun switchToPregnancyMode(email: String? = null) {
        val user = if (email != null) userRepository.getUserByEmail(email) else userRepository.getUser()
        user?.let {
            userRepository.updateUser(it.copy(isPregnant = true))
        }
    }

    suspend fun switchToNormalCycle(email: String? = null) {
        val user = if (email != null) userRepository.getUserByEmail(email) else userRepository.getUser()
        user?.let {
            userRepository.updateUser(it.copy(isPregnant = false))
        }
    }
}
