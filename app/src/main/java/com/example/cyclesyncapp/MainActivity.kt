package com.example.cyclesyncapp

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.databinding.ActivityMainBinding
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity Utama untuk menampilkan Dashboard CycleSync.
 * Menggunakan pola MVVM dengan ViewModel untuk menangani logika perhitungan siklus.
 */
class MainActivity : AppCompatActivity() {

    // Inisialisasi ViewBinding untuk mengakses elemen UI di activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Inisialisasi ViewModel menggunakan delegasi 'viewModels()'
    private val viewModel: CycleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menyiapkan Observer untuk memantau perubahan data di ViewModel
        setupObservers()

        /**
         * Contoh pemanggilan kalkulasi prediksi menstruasi.
         * Parameter: (Tahun, Bulan, Tanggal, Riwayat Siklus)
         * Catatan: Di Android, index bulan dimulai dari 0 (Januari = 0, April = 3).
         */
        viewModel.calculatePrediction(
            year = 2025,
            month = 3,  // April
            day = 1,
            cycleHistory = listOf(28, 30, 27)
        )
    }

    /**
     * Fungsi untuk mengamati data dari ViewModel.
     * Setiap kali data di ViewModel berubah, UI akan otomatis terupdate.
     */
    private fun setupObservers() {
        // Format tanggal Indonesia (Contoh: 01 April 2025)
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        viewModel.predictionResult.observe(this) { result ->
            // 1. Update Tanggal Menstruasi Berikutnya
            // Mengubah tipe data Long (Timestamp) dari model menjadi format tanggal String
            binding.tvNextPeriod.text = dateFormat.format(Date(result.nextPeriodDate))

            // 2. Update Rentang Masa Subur
            val fertileStartStr = dateFormat.format(Date(result.fertileStart))
            val fertileEndStr = dateFormat.format(Date(result.fertileEnd))

            // Menampilkan rentang (Contoh: "10 April 2025 - 15 April 2025")
            binding.tvFertileWindow.text = "$fertileStartStr - $fertileEndStr"
        }
    }
}