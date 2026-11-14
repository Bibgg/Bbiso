package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GreetingLanguage(
    val id: String,
    val title: String,
    val subtitle: String,
    val sampleText: String,
    val locale: Locale,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f
) {
    CYBER_AI(
        id = "cyber_ai",
        title = "Cyber-AI Voice",
        subtitle = "Futuristic Neural Engine",
        sampleText = "Welcome to OmniBox. Neural audio processors online. Feeds ready.",
        locale = Locale.US,
        pitch = 0.80f,
        speechRate = 1.08f
    ),
    HINDI(
        id = "hi",
        title = "Hindi (हिंदी)",
        subtitle = "नमस्ते! आपका स्वागत है",
        sampleText = "नमस्ते! ऑम्नीबॉक्स में आपका स्वागत है।",
        locale = Locale("hi", "IN"),
        pitch = 1.0f,
        speechRate = 1.0f
    ),
    SANSKRIT(
        id = "sa",
        title = "Sanskrit (संस्कृतम्)",
        subtitle = "स्वागतम्! भवतां स्वागतम् अस्ति",
        sampleText = "स्वागतम्! ओम्नीबॉक्से भवतां हार्दिकं स्वागतम् अस्ति।",
        locale = Locale("sa", "IN"),
        pitch = 1.05f,
        speechRate = 0.95f
    ),
    ENGLISH(
        id = "en",
        title = "Global English",
        subtitle = "Cinema-Grade Welcome",
        sampleText = "Welcome to OmniBox! Explore Tube, Snap, Chatter, and Forums.",
        locale = Locale.US,
        pitch = 1.0f,
        speechRate = 1.0f
    ),
    JAPANESE(
        id = "ja",
        title = "Japanese (日本語)",
        subtitle = "オムニボックスへようこそ",
        sampleText = "オムニボックスへようこそ！",
        locale = Locale.JAPAN,
        pitch = 1.15f,
        speechRate = 1.05f
    ),
    SPANISH(
        id = "es",
        title = "Spanish (Español)",
        subtitle = "¡Bienvenido a OmniBox!",
        sampleText = "¡Bienvenido a OmniBox! Tu centro social unificado.",
        locale = Locale("es", "ES"),
        pitch = 1.0f,
        speechRate = 1.02f
    )
}

data class GreetingSettings(
    val isStartupChimeEnabled: Boolean = true,
    val isVoiceGreetingEnabled: Boolean = true,
    val selectedVoice: GreetingLanguage = GreetingLanguage.CYBER_AI,
    val volume: Float = 0.85f,
    val isPlayingPreview: Boolean = false,
    val lastGreetingPlayed: String = ""
)

object OmniGreetingEngine {
    private const val TAG = "OmniGreetingEngine"
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _settings = MutableStateFlow(GreetingSettings())
    val settings: StateFlow<GreetingSettings> = _settings.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var hasPlayedOnStartup = false

    fun init(context: Context) {
        if (textToSpeech != null) return

        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                Log.d(TAG, "TTS initialized successfully")
                applyCurrentVoiceLocale()
            } else {
                Log.w(TAG, "TTS initialization failed with code $status")
            }
        }
    }

    private fun applyCurrentVoiceLocale() {
        val tts = textToSpeech ?: return
        if (!isTtsReady) return

        val voice = _settings.value.selectedVoice
        val res = tts.setLanguage(voice.locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback gracefully to default locale
            Log.w(TAG, "Locale ${voice.locale} not supported, falling back to default")
            tts.language = Locale.US
        }
        tts.setPitch(voice.pitch)
        tts.setSpeechRate(voice.speechRate)
    }

    /**
     * Plays the signature startup chime and personalized voice greeting on app launch.
     */
    fun onAppLaunch(context: Context) {
        if (hasPlayedOnStartup) return
        hasPlayedOnStartup = true

        init(context)

        scope.launch {
            if (_settings.value.isStartupChimeEnabled) {
                playSignatureChime()
            }
            if (_settings.value.isVoiceGreetingEnabled) {
                speakGreeting(_settings.value.selectedVoice)
            }
        }
    }

    /**
     * Preview voice greeting from settings or sound lab
     */
    fun previewGreeting(language: GreetingLanguage) {
        _settings.update { it.copy(selectedVoice = language, isPlayingPreview = true) }
        applyCurrentVoiceLocale()

        scope.launch {
            playSignatureChime()
            speakGreeting(language) {
                _settings.update { it.copy(isPlayingPreview = false) }
            }
        }
    }

    fun toggleStartupChime(enabled: Boolean) {
        _settings.update { it.copy(isStartupChimeEnabled = enabled) }
    }

    fun toggleVoiceGreeting(enabled: Boolean) {
        _settings.update { it.copy(isVoiceGreetingEnabled = enabled) }
    }

    fun setVoiceLanguage(language: GreetingLanguage) {
        _settings.update { it.copy(selectedVoice = language) }
        applyCurrentVoiceLocale()
    }

    fun setVolume(volume: Float) {
        _settings.update { it.copy(volume = volume.coerceIn(0.0f, 1.0f)) }
    }

    /**
     * Synthesizes a signature high-fidelity futuristic startup chime with harmonics
     * (C5 523Hz -> E5 659Hz -> G5 784Hz -> C6 1046Hz) using AudioTrack.
     */
    fun playSignatureChime() {
        try {
            val sampleRate = 44100
            val durationMs = 650
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            // Frequencies for futuristic Omni chord: C5, E5, G5, C6
            val freq1 = 523.25
            val freq2 = 659.25
            val freq3 = 783.99
            val freq4 = 1046.50

            val vol = _settings.value.volume

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Exponential decay envelope with smooth 30ms attack
                val attack = (t / 0.03).coerceAtMost(1.0)
                val decay = exp(-t * 5.2)
                val envelope = attack * decay

                // Harmonic synthesis
                val wave1 = sin(2.0 * PI * freq1 * t)
                val wave2 = 0.7 * sin(2.0 * PI * freq2 * t)
                val wave3 = 0.5 * sin(2.0 * PI * freq3 * t)
                val wave4 = 0.3 * sin(2.0 * PI * freq4 * t)

                val sample = ((wave1 + wave2 + wave3 + wave4) * 0.35 * envelope * vol * 32767.0).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Release after completion
            scope.launch {
                kotlinx.coroutines.delay(durationMs.toLong() + 100)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore release
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack chime playback error: ${e.message}")
        }
    }

    private fun speakGreeting(language: GreetingLanguage, onComplete: (() -> Unit)? = null) {
        val tts = textToSpeech ?: run {
            onComplete?.invoke()
            return
        }

        if (!isTtsReady) {
            onComplete?.invoke()
            return
        }

        try {
            val res = tts.setLanguage(language.locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
            tts.setPitch(language.pitch)
            tts.setSpeechRate(language.speechRate)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _settings.value.volume)
            }

            val utteranceId = "OmniGreeting_${System.currentTimeMillis()}"

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _settings.update { it.copy(lastGreetingPlayed = language.sampleText) }
                }

                override fun onDone(utteranceId: String?) {
                    onComplete?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onComplete?.invoke()
                }
            })

            tts.speak(language.sampleText, TextToSpeech.QUEUE_ADD, params, utteranceId)
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak failed: ${e.message}")
            onComplete?.invoke()
        }
    }

    fun destroy() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "TTS shutdown error: ${e.message}")
        } finally {
            textToSpeech = null
            isTtsReady = false
        }
    }
}
