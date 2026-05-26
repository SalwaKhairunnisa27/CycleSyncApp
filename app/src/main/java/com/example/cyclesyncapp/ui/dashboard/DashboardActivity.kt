package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cyclesyncapp.databinding.ActivityDashboardBinding
import com.example.cyclesyncapp.ui.adapter.RecommendationAdapter
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.example.cyclesyncapp.ui.viewmodel.RecommendationViewModel
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val recommendationViewModel: RecommendationViewModel by viewModels()
    private val cycleViewModel: CycleViewModel by viewModels()
    private val recommendationAdapter = RecommendationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupObservers()

        // Ambil data awal
        recommendationViewModel.loadRecommendations("LUTEAL")
    }

    private fun setupUI() {
        binding.cardLogHarian.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        binding.navSiklus.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        binding.navLog.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        binding.rvRecommendations.apply {
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Observasi List Rekomendasi (Arin)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                recommendationViewModel.recommendations.collect { recommendations ->
                    recommendationAdapter.submitList(recommendations)
                }
            }
        }

        // Observasi Perubahan Siklus (Aisyah & Arin)
        cycleViewModel.predictionResult.observe(this) { result ->
            // Update Rekomendasi berdasarkan fase otomatis
            // Ganti baris ini:
            recommendationViewModel.loadRecommendations(result.currentPhase.name)

            // Update Badge Fase di UI
            binding.tvPhaseBadge.text = "🌸 Fase ${result.currentPhase}"
        }
    }
}