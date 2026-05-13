package com.example.tugaspab1

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Inisialisasi TextView dari XML
        val tvHasil = findViewById<TextView>(R.id.tvHasilStruk)

        // Data Pembeli
        val pembeli = "Andi"

        // Data Barang & Harga
        val barang1 = "Buku Tulis"; val harga1 = 5000; val jumlah1 = 3
        val barang2 = "Pensil";     val harga2 = 2500; val jumlah2 = 2
        val barang3 = "Penghapus";  val harga3 = 1500; val jumlah3 = 1

        // Hitung Total tiap barang
        val total1 = harga1 * jumlah1
        val total2 = harga2 * jumlah2
        val total3 = harga3 * jumlah3

        // Hitung Subtotal
        val subtotal = total1 + total2 + total3
        var diskon = 0.0
        var labelDiskon = "0%"

        if (subtotal > 20000) {
            diskon = subtotal * 0.10
            labelDiskon = "10%"
        } else if (subtotal > 10000) {
            diskon = subtotal * 0.05
            labelDiskon = "5%"
        }
        val totalBayar = subtotal - diskon

        // --- Bagian Tampilan (SUDAH DIRAPIKAN TITIK DUANYA) ---
        val strukText = """
            ***** STRUK BELANJA *****
            Pembeli    : $pembeli
            -------------------------
            
            $barang1    x$jumlah1 = Rp$total1
            $barang2            x$jumlah2 = Rp$total2
            $barang3  x$jumlah3 = Rp$total3
            
            -------------------------
            Subtotal         : Rp$subtotal
            Diskon $labelDiskon   : Rp${diskon.toInt()}
            Total Bayar    : Rp${totalBayar.toInt()}
            
            =========================
        """.trimIndent()

        // Tampilkan hasil ke layar HP
        tvHasil.text = strukText
    }
}