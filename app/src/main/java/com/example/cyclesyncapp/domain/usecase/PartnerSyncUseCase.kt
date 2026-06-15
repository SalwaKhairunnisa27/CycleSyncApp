package com.example.cyclesyncapp.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri

class PartnerSyncUseCase {
    fun generateShareMessage(partnerName: String, phase: String, dayOfCycle: Int, symptoms: String?): String {
        val partnerStr = if (partnerName.trim().isNotEmpty()) "Halo ${partnerName.trim()}, " else "Halo, "
        val symptomsStr = if (!symptoms.isNullOrEmpty() && symptoms.trim().isNotEmpty()) {
            "Kondisiku hari ini: $symptoms."
        } else {
            "Hari ini aku merasa cukup baik."
        }
        return if (phase.contains("Kehamilan")) {
            "${partnerStr}sekadar update hari ini aku sedang di $phase. $symptomsStr Terima kasih ya atas pengertian dan dukungannya! 💖"
        } else {
            "${partnerStr}sekadar update hari ini aku sedang di $phase (Hari ke-$dayOfCycle dari siklus). $symptomsStr Terima kasih ya atas pengertian dan dukungannya! 💖"
        }
    }

    fun shareStatusToWhatsApp(context: Context, phone: String, message: String) {
        val formattedPhone = if (phone.isNotEmpty()) {
            var cleaned = phone.replace(Regex("[^0-9+]"), "")
            if (cleaned.startsWith("0")) {
                cleaned = "62" + cleaned.substring(1)
            } else if (cleaned.startsWith("+")) {
                cleaned = cleaned.substring(1)
            }
            cleaned
        } else {
            ""
        }
        
        val url = if (formattedPhone.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    }
}
