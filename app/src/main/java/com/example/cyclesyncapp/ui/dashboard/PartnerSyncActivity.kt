package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.preferences.UserPreferences

class PartnerSyncActivity : AppCompatActivity() {

    private lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_sync)

        prefs = UserPreferences(this)

        val etPartnerName = findViewById<EditText>(R.id.etPartnerName)
        val etPartnerPhone = findViewById<EditText>(R.id.etPartnerPhone)
        val btnSaveContact = findViewById<Button>(R.id.btnSaveContact)
        val btnSendWhatsApp = findViewById<Button>(R.id.btnSendWhatsApp)
        val btnShareOther = findViewById<Button>(R.id.btnShareOther)
        val tvSavedContact = findViewById<TextView>(R.id.tvSavedContact)
        val layoutNoContact = findViewById<LinearLayout>(R.id.layoutNoContact)
        val layoutHasContact = findViewById<LinearLayout>(R.id.layoutHasContact)
        val tvPreviewMessage = findViewById<TextView>(R.id.tvPreviewMessage)
        val btnEditContact = findViewById<TextView>(R.id.btnEditContact)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        // Load saved partner data
        val savedName = prefs.getPartnerName()
        val savedPhone = prefs.getPartnerPhone()

        // Build share message from current cycle status
        val nickname = prefs.getNickname() ?: "Saya"
        val shareMessage = buildShareMessage(nickname)

        // Show preview
        tvPreviewMessage.text = shareMessage

        // Show correct state
        if (!savedName.isNullOrEmpty() && !savedPhone.isNullOrEmpty()) {
            showHasContact(layoutNoContact, layoutHasContact, tvSavedContact, savedName, savedPhone)
        } else {
            showNoContact(layoutNoContact, layoutHasContact)
        }

        btnSaveContact.setOnClickListener {
            val name = etPartnerName.text.toString().trim()
            val phone = etPartnerPhone.text.toString().trim().replace("-", "").replace(" ", "")
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Isi nama dan nomor WA pasangan dulu ya 💕", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.savePartnerContact(name, phone)
            showHasContact(layoutNoContact, layoutHasContact, tvSavedContact, name, phone)
            Toast.makeText(this, "Kontak $name berhasil disimpan 💾", Toast.LENGTH_SHORT).show()
        }

        btnSendWhatsApp.setOnClickListener {
            val phone = prefs.getPartnerPhone()
            if (phone.isNullOrEmpty()) {
                Toast.makeText(this, "Simpan kontak pasangan dulu ya!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Format phone: remove leading 0, add 62
            val formattedPhone = if (phone.startsWith("0")) "62${phone.substring(1)}" else "62$phone"
            val encodedMsg = Uri.encode(shareMessage)
            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=$encodedMsg"))
            try {
                startActivity(waIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp tidak ditemukan, coba bagikan via lainnya", Toast.LENGTH_SHORT).show()
            }
        }

        btnShareOther.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Update Kondisi Siklus - CycleSync")
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan kondisi via"))
        }

        btnEditContact.setOnClickListener {
            prefs.clearPartnerContact()
            showNoContact(layoutNoContact, layoutHasContact)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun buildShareMessage(nickname: String): String {
        val prefs = UserPreferences(this)
        val goal = prefs.getGoal()
        val currentPhase = prefs.getCurrentPhase()  // ✅ Fase nyata dari kalkulasi Dashboard
        val daysToNext = prefs.getNextPeriodDays()

        val phaseInfo = when {
            goal == "promil" && currentPhase.contains("Subur") ->
                "🌟 Aku lagi di MASA SUBUR nih! Ini adalah waktu yang penting buat kita. Jangan lupa ya sayang!"
            goal == "promil" ->
                "🌸 Fase: $currentPhase — Aku terus memantau siklus untuk program hamil kita."
            currentPhase.contains("Menstruasi") ->
                "🩸 Aku lagi haid nih, mungkin agak kurang nyaman beberapa hari ini. Butuh supportmu ya 💕"
            currentPhase.contains("Subur") ->
                "🌟 Aku lagi di masa subur. Kalau ada perubahan mood, itu wajar secara hormonal!"
            currentPhase.contains("Luteal") ->
                "🌙 Aku di fase Luteal nih. Kalau aku agak sensitif, itu pengaruh hormon ya — bukan marah sama kamu!"
            else ->
                "🌿 Aku di fase Folikuler — energi lagi bagus dan mood oke hari ini!"
        }

        val daysInfo = if (daysToNext > 0) "\n📅 Haid berikutnya diperkirakan sekitar $daysToNext hari lagi." else ""

        return "CycleSync Update dari $nickname 🌸\n\nHei sayang! $phaseInfo$daysInfo\n\n💕 Makasih udah selalu support aku ya!\n\n_Dikirim via CycleSync ❤️_"
    }

    private fun showNoContact(layoutNoContact: LinearLayout, layoutHasContact: LinearLayout) {
        layoutNoContact.visibility = android.view.View.VISIBLE
        layoutHasContact.visibility = android.view.View.GONE
    }

    private fun showHasContact(layoutNoContact: LinearLayout, layoutHasContact: LinearLayout, tvSaved: TextView, name: String, phone: String) {
        layoutNoContact.visibility = android.view.View.GONE
        layoutHasContact.visibility = android.view.View.VISIBLE
        tvSaved.text = "$name · +62$phone"
    }
}
