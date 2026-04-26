package com.example.cyclesyncapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menggunakan ViewBinding untuk menghubungkan XML
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}