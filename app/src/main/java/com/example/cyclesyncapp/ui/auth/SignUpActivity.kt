package com.example.cyclesyncapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.onboarding.PersonalDataActivity
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val tvTerms = findViewById<TextView>(R.id.tvTerms)

        // Styling "Sudah punya akun? Masuk di sini"
        val loginText = "Sudah punya akun? <font color='#D4607A'><u>Masuk di sini</u></font>"
        tvLoginLink.text = Html.fromHtml(loginText, Html.FROM_HTML_MODE_LEGACY)

        // Styling "Dengan mendaftar kamu setuju Syarat & Ketentuan dan Kebijakan Privasi"
        val termsText = "Dengan mendaftar kamu setuju <font color='#D4607A'>Syarat & Ketentuan</font> dan <font color='#D4607A'>Kebijakan Privasi</font>"
        tvTerms.text = Html.fromHtml(termsText, Html.FROM_HTML_MODE_LEGACY)

        btnRegister.setOnClickListener {
            startActivity(Intent(this, PersonalDataActivity::class.java))
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }
}
