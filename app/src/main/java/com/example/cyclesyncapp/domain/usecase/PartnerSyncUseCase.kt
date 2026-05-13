package com.example.cyclesyncapp.domain.usecase

import java.util.UUID

class PartnerSyncUseCase {

    fun generateSyncCode(userId: String): String {
        // Generate a simple 6 character alphanumeric code
        val uuid = UUID.randomUUID().toString().replace("-", "").uppercase()
        return uuid.substring(0, 6)
    }

    fun syncWithPartner(partnerCode: String): Boolean {
        // Mock logic for syncing with a partner
        val regex = "^[A-Z0-9]{6}$".toRegex()
        return regex.matches(partnerCode)
    }
}
