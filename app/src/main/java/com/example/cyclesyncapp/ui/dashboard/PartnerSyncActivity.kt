package com.example.cyclesyncapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.security.EncryptionManager
import com.example.cyclesyncapp.databinding.ActivityPartnerSyncBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.URLEncoder

class PartnerSyncActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPartnerSyncBinding
    private var generatedMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartnerSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPartnerData()
        generatePreview()

        binding.btnSavePartner.setOnClickListener { savePartnerData() }
        binding.btnWhatsApp.setOnClickListener { shareViaWhatsApp() }
        binding.btnSMS.setOnClickListener { shareViaSMS() }
        binding.btnCopy.setOnClickListener { copyToClipboard() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadPartnerData() {
        val prefs = getSharedPreferences("partner_prefs", MODE_PRIVATE)
        binding.etPartnerName.setText(prefs.getString("name", ""))
        binding.etPartnerPhone.setText(prefs.getString("phone", ""))
    }

    private fun savePartnerData() {
        val name = binding.etPartnerName.text.toString()
        val phone = binding.etPartnerPhone.text.toString()
        getSharedPreferences("partner_prefs", MODE_PRIVATE).edit().apply {
            putString("name", name)
            putString("phone", phone)
            apply()
        }
        Toast.makeText(this, "Kontak Pasangan Tersimpan!", Toast.LENGTH_SHORT).show()
    }

    private fun generatePreview() {
        lifecycleScope.launch {
            val db = CycleDatabase.getDatabase(this@PartnerSyncActivity)
            val latestLog = db.dailyLogDao().getAllLogs().firstOrNull()?.firstOrNull()
            val user = db.userDao().getUser()

            val rawNote = latestLog?.encryptedNote?.let {
                try {
                    EncryptionManager.decrypt(it)
                } catch (e: Exception) {
                    ""
                }
            } ?: ""

            // Safe parsing of the encrypted note format: "Mood: ..., Gejala: ..., Energi: ..., Status: ..."
            val mood = if (rawNote.contains("Mood: ")) rawNote.substringAfter("Mood: ").substringBefore(",") else "Tidak dicatat"
            val energy = if (rawNote.contains("Energi: ")) rawNote.substringAfter("Energi: ").substringBefore(",") else "Sedang"

            generatedMessage = """
                CycleSync Update dari ${user?.name ?: "Aisyah"} 🌸
                
                Hai sayang, FYI aku lagi di fase ${latestLog?.phase ?: "Luteal"} nih. 
                Moodku lagi $mood, energi $energy.
                
                _Pesan otomatis dari CycleSync_
            """.trimIndent()

            binding.tvPhaseTitle.text = "Fase ${latestLog?.phase ?: "Luteal"}"
            binding.tvMoodSummary.text = "Mood: $mood • Energi: $energy"
            binding.tvPreviewMessage.text = generatedMessage
        }
    }

    private fun shareViaWhatsApp() {
        val phone = binding.etPartnerPhone.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "Isi No WhatsApp dulu", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            var cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
            if (cleanPhone.startsWith("0")) {
                cleanPhone = "62" + cleanPhone.substring(1)
            }
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=" + URLEncoder.encode(generatedMessage, "UTF-8")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareViaSMS() {
        val phone = binding.etPartnerPhone.text.toString()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
        intent.putExtra("sms_body", generatedMessage)
        startActivity(intent)
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CycleSync", generatedMessage))
        Toast.makeText(this, "Pesan disalin!", Toast.LENGTH_SHORT).show()
    }
}