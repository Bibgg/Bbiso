package com.example.ui.components.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPreset
import com.example.audio.GreetingLanguage
import com.example.audio.OmniAudioEngine
import com.example.audio.OmniGreetingEngine
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.LikeMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniSoundLabSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioState by OmniAudioEngine.state.collectAsStateWithLifecycle()
    val greetingState by OmniGreetingEngine.settings.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledBlack,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPurple, CyberCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = "Dolby Audio Engine",
                            tint = OledBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Omni Sound Lab",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonPurple.copy(alpha = 0.2f))
                                    .border(1.dp, NeonPurple, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DOLBY 3D",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Cinema-Grade Audio & Voice Greeting Ecosystem",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("sound_lab_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sound Lab",
                        tint = TextSecondary
                    )
                }
            }

            // Live Spectrum Visualizer Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceLow)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "LIVE AUDIO SPECTRUM",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Preset: ${audioState.currentPreset.displayName}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AudioSpectrumVisualizer(
                        isPlaying = true,
                        barCount = 12,
                        maxHeight = 24.dp,
                        barWidth = 4.dp,
                        spacing = 3.dp
                    )
                }
            }

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = OledBlack,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonPurple
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dolby Equalizer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Welcome Voice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Scrollable Tab Contents
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // TAB 1: DOLBY EQUALIZER & SPATIAL ENGINE
                    SpatialAudioEngineSection(audioState)
                } else {
                    // TAB 2: WELCOME VOICE GREETING & CHIME
                    WelcomeVoiceGreetingSection(greetingState)
                }
            }
        }
    }
}

@Composable
private fun SpatialAudioEngineSection(audioState: com.example.audio.AudioEngineState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Presets Carousel
        Text(
            text = "AUDIO PRESETS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AudioPreset.entries.size) { index ->
                val preset = AudioPreset.entries[index]
                val isSelected = audioState.currentPreset == preset

                val bg by animateColorAsState(
                    targetValue = if (isSelected) NeonPurple else DarkSurfaceMedium,
                    label = "presetBg"
                )
                val textCol by animateColorAsState(
                    targetValue = if (isSelected) OledBlack else TextPrimary,
                    label = "presetText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, if (isSelected) CyberCyan else DarkBorder, RoundedCornerShape(10.dp))
                        .clickable { OmniAudioEngine.selectPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("preset_${preset.name}")
                ) {
                    Column {
                        Text(
                            text = preset.displayName,
                            color = textCol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.description,
                            color = if (isSelected) OledBlack.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Feature Switches & Sliders
        // 1. Spatial Virtualizer (3D Dolby)
        AudioControlCard(
            title = "Cinema Spatial Virtualizer",
            subtitle = "Widens soundstage for 3D binaural headphone audio",
            icon = Icons.Default.SpatialAudio,
            iconTint = CyberCyan,
            isChecked = audioState.isSpatialAudioEnabled,
            onCheckedChange = { OmniAudioEngine.setSpatialAudioEnabled(it) }
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Spatial Width", color = TextSecondary, fontSize = 12.sp)
                    Text("${audioState.spatialStrength / 10}%", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.spatialStrength.toFloat(),
                    onValueChange = { OmniAudioEngine.setSpatialStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkSurfaceMedium
                    )
                )
            }
        }

        // 2. Bass Boost Pro
        AudioControlCard(
            title = "Sub-Bass Harmonic Boost",
            subtitle = "Deep acoustic bass resonance without clipping",
            icon = Icons.Default.Headphones,
            iconTint = LikeMagenta,
            isChecked = audioState.isBassBoostEnabled,
            onCheckedChange = { OmniAudioEngine.setBassBoostEnabled(it) }
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Strength", color = TextSecondary, fontSize = 12.sp)
                    Text("${audioState.bassBoostStrength / 10}%", color = LikeMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.bassBoostStrength.toFloat(),
                    onValueChange = { OmniAudioEngine.setBassBoostStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = LikeMagenta,
                        activeTrackColor = LikeMagenta,
                        inactiveTrackColor = DarkSurfaceMedium
                    )
                )
            }
        }

        // 3. Automated Loudness Enhancer
        AudioControlCard(
            title = "Smart Volume Normalizer",
            subtitle = "Prevents sudden volume spikes across Tube, Snap & Chatter reels",
            icon = Icons.Default.VolumeUp,
            iconTint = NeonPurple,
            isChecked = audioState.isLoudnessNormalizerEnabled,
            onCheckedChange = { OmniAudioEngine.setLoudnessNormalizerEnabled(it) }
        )

        // 4. 5-Band Equalizer Sliders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceLow)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5-BAND EQUALIZER",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "±12 dB Precision",
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OmniAudioEngine.bandFrequencies.forEachIndexed { index, freqLabel ->
                    val bandLevel = audioState.bandLevels.getOrElse(index) { 0 }
                    val dbValue = bandLevel / 100.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(55.dp)
                        )
                        Slider(
                            value = bandLevel.toFloat(),
                            onValueChange = { OmniAudioEngine.setEqualizerBandLevel(index, it.toInt()) },
                            valueRange = -1200f..1200f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = DarkSurfaceMedium
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%+.1f dB", dbValue),
                            color = if (bandLevel >= 0) CyberCyan else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeVoiceGreetingSection(greetingState: com.example.audio.GreetingSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Master Switches
        AudioControlCard(
            title = "Startup Audio Chime",
            subtitle = "Play dynamic multi-harmonic cyber chord upon launching OmniBox",
            icon = Icons.Default.Audiotrack,
            iconTint = CyberCyan,
            isChecked = greetingState.isStartupChimeEnabled,
            onCheckedChange = { OmniGreetingEngine.toggleStartupChime(it) }
        )

        AudioControlCard(
            title = "Personalized Voice Greeting",
            subtitle = "Welcome prompt spoken in your chosen language personality",
            icon = Icons.Default.RecordVoiceOver,
            iconTint = NeonPurple,
            isChecked = greetingState.isVoiceGreetingEnabled,
            onCheckedChange = { OmniGreetingEngine.toggleVoiceGreeting(it) }
        )

        // Voice Language Personas
        Text(
            text = "VOICE GREETING PERSONALITY",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GreetingLanguage.entries.forEach { lang ->
                val isSelected = greetingState.selectedVoice == lang

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) DarkSurfaceMedium else DarkSurfaceLow)
                        .border(
                            1.dp,
                            if (isSelected) NeonPurple else DarkBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { OmniGreetingEngine.setVoiceLanguage(lang) }
                        .padding(14.dp)
                        .testTag("voice_${lang.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = lang.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "“${lang.sampleText}”",
                                color = if (isSelected) CyberCyan else TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { OmniGreetingEngine.previewGreeting(lang) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Preview Voice",
                                tint = NeonPurple
                            )
                        }
                    }
                }
            }
        }

        // Volume Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceLow)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Greeting Volume", color = TextSecondary, fontSize = 13.sp)
                    Text("${(greetingState.volume * 100).toInt()}%", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = greetingState.volume,
                    onValueChange = { OmniGreetingEngine.setVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkSurfaceMedium
                    )
                )
            }
        }

        // Test Experience Button
        Button(
            onClick = {
                OmniGreetingEngine.previewGreeting(greetingState.selectedVoice)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_welcome_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = OledBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (greetingState.isPlayingPreview) "Playing Welcome Audio..." else "Test Welcome Experience 🔊",
                    color = OledBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AudioControlCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceLow)
            .border(1.dp, if (isChecked) iconTint.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OledBlack,
                        checkedTrackColor = iconTint,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceMedium
                    )
                )
            }

            if (isChecked && content != null) {
                content()
            }
        }
    }
}
