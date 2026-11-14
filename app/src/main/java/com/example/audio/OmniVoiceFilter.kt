package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class VoiceFilterType(
    val title: String,
    val iconEmoji: String,
    val description: String,
    val basePitchFactor: Float,
    val modulationRateHz: Double
) {
    STUDIO_CLEAN(
        title = "Studio Clean",
        iconEmoji = "🎙️",
        description = "Neutral studio condenser sound with zero coloring",
        basePitchFactor = 1.0f,
        modulationRateHz = 0.0
    ),
    CYBER_AI_ROBOT(
        title = "Cyber-AI Robot",
        iconEmoji = "🤖",
        description = "Neural vocoder with metallic ring modulation & harmonic carrier",
        basePitchFactor = 0.82f,
        modulationRateHz = 35.0
    ),
    DEEP_BASS_GOD(
        title = "Deep Bass God",
        iconEmoji = "⚡",
        description = "Sub-octave resonance with dramatic cinema trailer rumble",
        basePitchFactor = 0.65f,
        modulationRateHz = 0.0
    ),
    HELIUM_CHIPMUNK(
        title = "Helium High",
        iconEmoji = "🐿️",
        description = "Ultra-bright up-shifted formant with energetic cadence",
        basePitchFactor = 1.60f,
        modulationRateHz = 0.0
    ),
    COSMIC_REVERB(
        title = "Cosmic Reverb",
        iconEmoji = "🌌",
        description = "Expansive 3D cathedral delay with decaying spatial reflections",
        basePitchFactor = 1.0f,
        modulationRateHz = 2.5
    ),
    VINTAGE_RADIO(
        title = "Lo-Fi Radio",
        iconEmoji = "📻",
        description = "300Hz-3kHz bandpass distortion with vintage transceiver warmth",
        basePitchFactor = 0.95f,
        modulationRateHz = 8.0
    )
}

object OmniVoiceTransformer {
    private const val TAG = "OmniVoiceTransformer"
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a live synthesized preview of the voice filter using real-time audio synthesis.
     */
    fun playVoiceFilterPreview(filter: VoiceFilterType, isNoiseSuppressionActive: Boolean) {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 700
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                val baseFreq = 260.0 * filter.basePitchFactor
                val noiseLevel = if (isNoiseSuppressionActive) 0.02 else 0.12

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = (t / 0.05).coerceAtMost(1.0) * exp(-t * 2.8)

                    // Base speech formant
                    var sample = sin(2.0 * PI * baseFreq * t) +
                            0.5 * sin(2.0 * PI * (baseFreq * 2.0) * t) +
                            0.25 * sin(2.0 * PI * (baseFreq * 3.0) * t)

                    // Apply filter-specific acoustic transformations
                    when (filter) {
                        VoiceFilterType.CYBER_AI_ROBOT -> {
                            val modulator = sin(2.0 * PI * filter.modulationRateHz * t)
                            sample = sample * (0.6 + 0.4 * modulator)
                        }
                        VoiceFilterType.DEEP_BASS_GOD -> {
                            val subHarmonic = sin(2.0 * PI * (baseFreq * 0.5) * t)
                            sample = sample * 0.7 + subHarmonic * 0.8
                        }
                        VoiceFilterType.HELIUM_CHIPMUNK -> {
                            sample = sin(2.0 * PI * baseFreq * t) * 0.9
                        }
                        VoiceFilterType.COSMIC_REVERB -> {
                            // Simulated decaying delay echo
                            val echo1 = if (i > 3000) sin(2.0 * PI * baseFreq * (t - 0.07)) * 0.4 else 0.0
                            val echo2 = if (i > 6000) sin(2.0 * PI * baseFreq * (t - 0.14)) * 0.2 else 0.0
                            sample = sample + echo1 + echo2
                        }
                        VoiceFilterType.VINTAGE_RADIO -> {
                            // Bandpass clipping
                            sample = sample.coerceIn(-0.6, 0.6) * 1.4
                        }
                        VoiceFilterType.STUDIO_CLEAN -> {
                            // Clean untouched
                        }
                    }

                    // Ambient noise reduction effect
                    val ambientHiss = (Math.random() * 2.0 - 1.0) * noiseLevel
                    val finalVal = ((sample + ambientHiss) * envelope * 0.45 * 32767.0).toInt()
                    buffer[i] = finalVal.coerceIn(-32768, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()

                delay(durationMs.toLong() + 50)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Ignore
                }
            } catch (e: Exception) {
                Log.w(TAG, "Voice filter preview playback error: ${e.message}")
            }
        }
    }
}
