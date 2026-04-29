package com.example.cyclesyncapp.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.databinding.ActivityMainBinding
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: CycleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Init ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Init ViewModel (WAJIB pakai ini, bukan constructor langsung)
        viewModel = ViewModelProvider(this)[CycleViewModel::class.java]

        // 🔹 Jalankan logic (dibungkus try biar aman dari crash)
        try {
            val result = viewModel.getPrediction()

            Log.d("CycleDebug", "Next Period: ${result.nextPeriod.time}")
            Log.d("CycleDebug", "Ovulation Day: ${result.ovulationDay.time}")

        } catch (e: Exception) {
            Log.e("CycleDebug", "Error saat hitung: ${e.message}")
        }

        // 🔹 Setup Navigation (punyamu tetap)
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment

        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
    }
}