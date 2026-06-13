package com.example.cyclesyncapp.ui.dashboard

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R

class ArticleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val title = intent.getStringExtra("title") ?: "Mengapa Kamu Lebih Emosional di Akhir Siklus?"
        val category = intent.getStringExtra("category") ?: "Hormon"
        val content = intent.getStringExtra("content") ?: "Setelah ovulasi, kadar progesteron meningkat tajam sementara estrogen menurun. Perubahan ini berdampak langsung pada kadar serotonin — neurotransmitter yang mengatur suasana hati."
        val phase = intent.getStringExtra("phase") ?: "LUTEAL"

        findViewById<TextView>(R.id.tvArticleTitle).text = title
        findViewById<TextView>(R.id.tvArticleSub).text = "$category · 5 menit baca · Literasi Ilmiah"
        findViewById<TextView>(R.id.tvArticleTag).text = "📌 Sesuai Fase $phase Aktif"
        findViewById<TextView>(R.id.tvArticleContent).text = content
    }
}