package com.example.cyclesyncapp.ui.dashboard

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.domain.usecase.PregnancyTransitionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

class PostpartumActivity : AppCompatActivity() {

    private lateinit var pregnancyTransitionUseCase: PregnancyTransitionUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_postpartum)

        val database = CycleDatabase.getDatabase(this)
        val userRepository = UserRepositoryImpl(database.userDao())
        pregnancyTransitionUseCase = PregnancyTransitionUseCase(userRepository)

        findViewById<Button>(R.id.btnEndPregnancy).setOnClickListener {
            endPregnancy()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }

        // Pre-initialize in case it hasn't been initialized yet
        preInitialize(this)

        // Show congrats popup and play fanfare sound
        showCongratsDialog()
    }

    private fun showCongratsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_congratulations, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        dialogView.findViewById<Button>(R.id.btnDismissCongrats).setOnClickListener {
            dialog.dismiss()
            // Speak immediately on button click using pre-initialized TTS
            val safeTts = preInitializedTts
            if (safeTts != null) {
                safeTts.speak("Congratulations, welcome baby!", TextToSpeech.QUEUE_FLUSH, null, "congrats_msg")
            } else {
                // Fallback: If not ready yet, initialize and speak
                var fallbackTts: TextToSpeech? = null
                fallbackTts = TextToSpeech(applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        fallbackTts?.let { safeTts ->
                            val result = safeTts.setLanguage(Locale.US)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                safeTts.language = Locale.getDefault()
                            }
                            safeTts.setPitch(1.7f)        // Cute, squeaky baby-like pitch
                            safeTts.setSpeechRate(0.75f)  // Slower, baby-like speaking rate
                            safeTts.speak("Congratulations, welcome baby!", TextToSpeech.QUEUE_FLUSH, null, "congrats_msg")
                            preInitializedTts = safeTts
                        }
                    }
                }
            }
        }
        
        dialog.show()

        // Play fanfare sound chime programmatically
        playCongratulationsSound()
    }

    private fun endPregnancy() {
        lifecycleScope.launch {
            pregnancyTransitionUseCase.switchToNormalCycle()
            Toast.makeText(this@PostpartumActivity, "Mode Siklus Normal Aktif!", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this@PostpartumActivity, PostpartumDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    private fun playCongratulationsSound() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val durationMs = 2500
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            
            val notes = listOf(
                Triple(523.25f, 0.0f, 0.15f), // C5
                Triple(659.25f, 0.15f, 0.15f), // E5
                Triple(783.99f, 0.30f, 0.15f), // G5
                Triple(1046.50f, 0.45f, 0.30f), // C6
                
                Triple(880.00f, 0.80f, 0.15f), // A5
                Triple(987.77f, 0.95f, 0.15f), // B5
                Triple(1174.66f, 1.10f, 0.40f), // D6
                
                Triple(1318.51f, 1.60f, 0.60f)  // E6
            )
            
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var value = 0.0
                var envelope = 0.0
                
                for (note in notes) {
                    val noteStart = note.second
                    val noteDuration = note.third
                    if (t >= noteStart && t < noteStart + noteDuration) {
                        val tLocal = t - noteStart
                        val freq = note.first
                        
                        var noteVal = sin(2.0 * PI * freq * tLocal)
                        noteVal += 0.3 * sin(2.0 * PI * (freq * 2) * tLocal)
                        
                        val noteEnv = exp(-4.0 * tLocal.toDouble())
                        
                        value = noteVal / 1.3
                        envelope = noteEnv
                        break
                    }
                }
                
                if (t > 0.4f) {
                    val sparkleStart = 0.4f
                    val sparkleDuration = 2.0f
                    if (t < sparkleStart + sparkleDuration) {
                        val tSparkle = t - sparkleStart
                        val fSparkle = 1500f + 500f * sin(2.0 * PI * 8.0 * tSparkle).toFloat()
                        val sparkleVal = sin(2.0 * PI * fSparkle * tSparkle)
                        val sparkleEnv = 0.15 * exp(-12.0 * (tSparkle % 0.15f).toDouble())
                        value += sparkleVal * sparkleEnv
                    }
                }
                
                val pcm = (value * envelope * 32767.0 * 0.7f).toInt().coerceIn(-32768, 32767)
                buffer[i] = pcm.toShort()
            }
            
            try {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(durationMs.toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        @Volatile
        var preInitializedTts: TextToSpeech? = null

        fun preInitialize(context: Context) {
            if (preInitializedTts == null) {
                val appCtx = context.applicationContext
                var tempTts: TextToSpeech? = null
                tempTts = TextToSpeech(appCtx) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tempTts?.let { safeTts ->
                            val result = safeTts.setLanguage(Locale.US)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                safeTts.language = Locale.getDefault()
                            }
                            safeTts.setPitch(1.7f)        // Cute, squeaky baby-like pitch
                            safeTts.setSpeechRate(0.75f)  // Slower, baby-like speaking rate
                            preInitializedTts = safeTts
                        }
                    }
                }
            }
        }
    }
}