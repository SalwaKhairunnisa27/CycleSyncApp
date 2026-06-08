package com.example.cyclesyncapp.domain.usecase

enum class UserStatus {
    NORMAL, PREGNANT, POSTPARTUM
}

class PregnancyTransitionUseCase {

    /**
     * Mengelola transisi status pengguna dari hamil kembali ke normal.
     */
    fun transitionStatus(
        currentStatus: UserStatus,
        isPregnancyEnded: Boolean,
        isFirstPeriodOccurred: Boolean
    ): UserStatus {
        return when (currentStatus) {
            UserStatus.PREGNANT -> {
                // Jika hamil selesai, pindah ke masa nifas/pemulihan (Postpartum)
                if (isPregnancyEnded) UserStatus.POSTPARTUM else UserStatus.PREGNANT
            }
            UserStatus.POSTPARTUM -> {
                // Kembali ke NORMAL hanya jika haid pertama sudah muncul
                if (isFirstPeriodOccurred) UserStatus.NORMAL else UserStatus.POSTPARTUM
            }
            else -> UserStatus.NORMAL
        }
    }
}