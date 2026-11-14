package com.example.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AudioPreset(val displayName: String, val description: String) {
    CINEMA_DOLBY("Cinema Dolby 3D", "Expansive surround virtualizer with dynamic bass"),
    DEEP_BASS("Deep Bass Pro", "Ultra-low sub-harmonic punch for beats & music"),
    VOCAL_CLARITY("Vocal Clarity", "Boosts dialogue & speech frequencies in reels"),
    CYBER_DYNAMIC("Cyber Dynamic", "Futuristic high-fidelity holographic soundstage"),
    STUDIO_FLAT("Studio Flat", "Pure neutral reference audio reproduction")
}

data class AudioEngineState(
    val isSpatialAudioEnabled: Boolean = true,
    val spatialStrength: Int = 850, // 0 to 1000
    val isBassBoostEnabled: Boolean = true,
    val bassBoostStrength: Int = 750, // 0 to 1000
    val isLoudnessNormalizerEnabled: Boolean = true,
    val targetLoudnessGainMb: Int = 400, // mB boost for reels
    val currentPreset: AudioPreset = AudioPreset.CINEMA_DOLBY,
    val bandLevels: List<Int> = listOf(400, 200, 100, 300, 500), // 5-band offsets in mB (-1200 to +1200)
    val activeSessionId: Int? = null,
    val isMasterAudioEngineActive: Boolean = true
)

object OmniAudioEngine {
    private const val TAG = "OmniAudioEngine"

    private val _state = MutableStateFlow(AudioEngineState())
    val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    /**
     * Attach Dolby-grade audio effect processors to an active ExoPlayer audio session
     */
    @Synchronized
    fun attachAudioSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        releaseEffects()

        try {
            currentSessionId = sessionId
            _state.update { it.copy(activeSessionId = sessionId) }

            // 1. 3D Spatial Audio Virtualizer
            virtualizer = try {
                Virtualizer(0, sessionId).apply {
                    enabled = _state.value.isSpatialAudioEnabled
                    if (strengthSupported) {
                        setStrength(_state.value.spatialStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer init fallback: ${e.message}")
                null
            }

            // 2. Sub-Bass Boost Processor
            bassBoost = try {
                BassBoost(0, sessionId).apply {
                    enabled = _state.value.isBassBoostEnabled
                    if (strengthSupported) {
                        setStrength(_state.value.bassBoostStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost init fallback: ${e.message}")
                null
            }

            // 3. Automated Loudness Enhancer (Normalizes reels/shorts volume)
            loudnessEnhancer = try {
                LoudnessEnhancer(sessionId).apply {
                    enabled = _state.value.isLoudnessNormalizerEnabled
                    setTargetGain(_state.value.targetLoudnessGainMb)
                }
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer init fallback: ${e.message}")
                null
            }

            // 4. Multi-band Equalizer
            equalizer = try {
                Equalizer(0, sessionId).apply {
                    enabled = _state.value.isMasterAudioEngineActive
                    applyCurrentBandLevels()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Equalizer init fallback: ${e.message}")
                null
            }

            Log.d(TAG, "Attached Omni Dolby Engine to session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching audio effects to session $sessionId: ${e.message}")
        }
    }

    @Synchronized
    fun detachAudioSession(sessionId: Int) {
        if (currentSessionId == sessionId) {
            releaseEffects()
            currentSessionId = 0
            _state.update { it.copy(activeSessionId = null) }
        }
    }

    private fun releaseEffects() {
        try {
            virtualizer?.release()
            bassBoost?.release()
            loudnessEnhancer?.release()
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio effects: ${e.message}")
        } finally {
            virtualizer = null
            bassBoost = null
            loudnessEnhancer = null
            equalizer = null
        }
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        _state.update { it.copy(isSpatialAudioEnabled = enabled) }
        try {
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Error setting spatial enabled: ${e.message}")
        }
    }

    fun setSpatialStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { it.copy(spatialStrength = clamped) }
        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting spatial strength: ${e.message}")
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        _state.update { it.copy(isBassBoostEnabled = enabled) }
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Error setting bass boost enabled: ${e.message}")
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { it.copy(bassBoostStrength = clamped) }
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting bass boost strength: ${e.message}")
        }
    }

    fun setLoudnessNormalizerEnabled(enabled: Boolean) {
        _state.update { it.copy(isLoudnessNormalizerEnabled = enabled) }
        try {
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Error setting loudness normalizer enabled: ${e.message}")
        }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int) {
        val clamped = levelMb.coerceIn(-1200, 1200)
        val current = _state.value.bandLevels.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = clamped
            _state.update { it.copy(bandLevels = current) }
            try {
                equalizer?.let { eq ->
                    if (bandIndex < eq.numberOfBands) {
                        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error setting EQ band level: ${e.message}")
            }
        }
    }

    fun selectPreset(preset: AudioPreset) {
        val (spatial, bass, bands, normalizer) = when (preset) {
            AudioPreset.CINEMA_DOLBY -> Tuple4(850, 750, listOf(400, 200, 100, 350, 600), true)
            AudioPreset.DEEP_BASS -> Tuple4(400, 1000, listOf(900, 650, 100, 0, 200), true)
            AudioPreset.VOCAL_CLARITY -> Tuple4(300, 200, listOf(-200, 100, 700, 800, 400), true)
            AudioPreset.CYBER_DYNAMIC -> Tuple4(1000, 850, listOf(600, 300, 200, 500, 900), true)
            AudioPreset.STUDIO_FLAT -> Tuple4(0, 0, listOf(0, 0, 0, 0, 0), false)
        }

        _state.update {
            it.copy(
                currentPreset = preset,
                isSpatialAudioEnabled = spatial > 0,
                spatialStrength = spatial,
                isBassBoostEnabled = bass > 0,
                bassBoostStrength = bass,
                bandLevels = bands,
                isLoudnessNormalizerEnabled = normalizer
            )
        }

        // Apply to hardware effects
        try {
            virtualizer?.let {
                it.enabled = spatial > 0
                if (it.strengthSupported) it.setStrength(spatial.toShort())
            }
            bassBoost?.let {
                it.enabled = bass > 0
                if (it.strengthSupported) it.setStrength(bass.toShort())
            }
            loudnessEnhancer?.enabled = normalizer
            applyCurrentBandLevels()
        } catch (e: Exception) {
            Log.w(TAG, "Error applying preset: ${e.message}")
        }
    }

    private fun applyCurrentBandLevels() {
        val eq = equalizer ?: return
        val bands = _state.value.bandLevels
        for (i in bands.indices) {
            if (i < eq.numberOfBands) {
                try {
                    eq.setBandLevel(i.toShort(), bands[i].toShort())
                } catch (e: Exception) {
                    // Ignore band errors on devices with fewer bands
                }
            }
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
