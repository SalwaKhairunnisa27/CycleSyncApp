package com.example.cyclesyncapp // Sesuaikan dengan folder filemu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.databinding.ActivityMainBinding
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import java.text.SimpleDateFormat
import java.util.Date // Tambahkan import ini
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CycleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        // Memanggil kalkulasi (Contoh: 1 April 2025)
        // Note: Bulan di Android mulai dari 0 (Januari = 0, April = 3)
        viewModel.calculatePrediction(2025, 3, 1, listOf(28, 30, 27))
    }

    private fun setupObservers() {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        viewModel.predictionResult.observe(this) { result ->
            // ✅ Perbaikan: Gunakan Date(result.nextPeriodDate)
            binding.tvNextPeriod.text = dateFormat.format(Date(result.nextPeriodDate))

            // ✅ Perbaikan: Nama variabel sesuai Model (fertileStart & fertileEnd)
            val fertileStartStr = dateFormat.format(Date(result.fertileStart))
            val fertileEndStr = dateFormat.format(Date(result.fertileEnd))

            binding.tvFertileWindow.text = "$fertileStartStr - $fertileEndStr"
        }
    }
}