package com.example.cyclesyncapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cyclesyncapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    data class PhaseInfo(
        val name: String, val color: String, val desc: String,
        val day: String, val next: String, val progress: Int,
        val progressLabel: String, val tip: String, val nextPhase: String
    )

    private val phases = mapOf(
        "menstruation" to PhaseInfo("Menstruation", "#E53935", "Your period phase. Rest and nourish your body.", "Day 3", "25 days", 10, "Day 3 of 28", "Your body is working hard. Rest is productive too. Honor your need for recovery.", "Follicular phase in 2 days"),
        "follicular" to PhaseInfo("Follicular", "#1E88E5", "Energy is building! Estrogen rises.", "Day 9", "19 days", 32, "Day 9 of 28", "Fresh start phase. Set new goals, try new activities, embrace creativity.", "Ovulation phase in 3 days"),
        "ovulation" to PhaseInfo("Ovulation", "#43A047", "Peak energy! Most energetic and confident.", "Day 14", "14 days", 50, "Day 14 of 28", "Peak confidence and communication. Great time for meetings and social events!", "Luteal phase in 1 day"),
        "luteal" to PhaseInfo("Luteal", "#8E24AA", "Winding down. Focus and prepare.", "Day 22", "6 days", 79, "Day 22 of 28", "Use introspective time for planning and deep focus tasks.", "Menstruation phase in 6 days")
    )

    private var currentPhase = "menstruation"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePhase("menstruation")

        binding.chipMenstruation.setOnClickListener { updatePhase("menstruation") }
        binding.chipFollicular.setOnClickListener { updatePhase("follicular") }
        binding.chipOvulation.setOnClickListener { updatePhase("ovulation") }
        binding.chipLuteal.setOnClickListener { updatePhase("luteal") }
    }

    private fun updatePhase(phase: String) {
        currentPhase = phase
        val p = phases[phase] ?: return
        val color = Color.parseColor(p.color)

        binding.phaseIndicator.setBackgroundColor(color)
        binding.tvPhaseName.text = p.name
        binding.tvPhaseDesc.text = p.desc
        binding.tvCurrentDay.text = p.day
        binding.tvNextPeriod.text = p.next
        binding.progressCycle.progress = p.progress
        binding.tvProgressLabel.text = p.progressLabel
        binding.tvNextPhase.text = "Next: ${p.nextPhase}"
        binding.tvTip.text = p.tip
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}