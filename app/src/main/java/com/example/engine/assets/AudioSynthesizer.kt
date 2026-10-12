package com.example.engine.assets

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object AudioSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun play(soundName: String) {
        scope.launch {
            try {
                val samples = generateSamples(soundName.lowercase())
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
            } catch (_: Exception) {
                // Audio synthesis fallback
            }
        }
    }

    private fun generateSamples(name: String): ShortArray {
        return when (name) {
            "jump" -> {
                val duration = 0.18f
                val count = (SAMPLE_RATE * duration).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = i.toFloat() / count
                    val freq = 200f + progress * 400f
                    val amp = (1f - progress) * 0.7f
                    val sample = (sin(2.0 * PI * freq * t) * amp * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                buffer
            }
            "impact" -> {
                val duration = 0.12f
                val count = (SAMPLE_RATE * duration).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = i.toFloat() / count
                    val freq = 120f * (1f - progress * 0.5f)
                    val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.2f
                    val amp = (1f - progress) * (1f - progress) * 0.8f
                    val sample = ((sin(2.0 * PI * freq * t) + noise) * amp * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                buffer
            }
            "laser" -> {
                val duration = 0.15f
                val count = (SAMPLE_RATE * duration).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = i.toFloat() / count
                    val freq = 900f - progress * 700f
                    val amp = (1f - progress) * 0.6f
                    val sample = (sin(2.0 * PI * freq * t) * amp * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                buffer
            }
            "explosion" -> {
                val duration = 0.25f
                val count = (SAMPLE_RATE * duration).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val progress = i.toFloat() / count
                    val noise = (Math.random() * 2.0 - 1.0).toFloat()
                    val amp = (1f - progress) * 0.85f
                    val sample = (noise * amp * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                buffer
            }
            "coin" -> {
                val duration = 0.15f
                val count = (SAMPLE_RATE * duration).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = i.toFloat() / count
                    val freq = if (progress < 0.5f) 987f else 1318f
                    val amp = (1f - progress) * 0.5f
                    val sample = (sin(2.0 * PI * freq * t) * amp * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                buffer
            }
            else -> {
                // Default click
                val count = (SAMPLE_RATE * 0.05f).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val progress = i.toFloat() / count
                    val sample = (sin(2.0 * PI * 440.0 * (i.toDouble() / SAMPLE_RATE)) * (1f - progress) * Short.MAX_VALUE * 0.3f).toInt()
                    buffer[i] = sample.toShort()
                }
                buffer
            }
        }
    }
}
