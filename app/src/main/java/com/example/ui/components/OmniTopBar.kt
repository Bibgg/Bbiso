package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.PostFormat
import com.example.ui.components.audio.InteractiveSoundPill
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

@Composable
fun OmniTopBar(
    selectedFilter: PostFormat?,
    onFilterSelected: (PostFormat?) -> Unit,
    showFilterChips: Boolean = true,
    onOpenSoundLab: () -> Unit = {},
    userProfile: com.example.data.model.UserProfile? = null,
    isLoggedIn: Boolean = false,
    onOpenAuth: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showUserMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledBlack)
            .statusBarsPadding()
    ) {
        // App Header: OmniBox Logo + Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // OmniBox Gradient Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glowing Cube Mark
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonPurple, CyberCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OledBlack)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "OmniBox",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
            }

            // Right icons (Sound Lab Pill + Auth Profile Button + Direct Messages)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InteractiveSoundPill(
                    isPlaying = true,
                    presetName = "Dolby 3D",
                    onClick = onOpenSoundLab
                )

                // If guest, show Sign In button
                if (!isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(NeonPurple, CyberCyan)))
                            .clickable { onOpenAuth() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("top_bar_signin_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = OledBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Direct Messages",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Format Filter Chips: All, Tube 🎥, Snap 📸, Chatter 💬, Forum 🌐
        if (showFilterChips) {
            val filters = listOf(
                null to "All",
                PostFormat.TUBE to "Tube 🎥",
                PostFormat.SNAP to "Snap 📸",
                PostFormat.CHATTER to "Chatter 💬",
                PostFormat.FORUM to "Forum 🌐"
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters.size) { index ->
                    val (format, label) = filters[index]
                    val isSelected = selectedFilter == format

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) NeonPurple else DarkSurfaceMedium,
                        label = "chipBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) OledBlack else TextSecondary,
                        label = "chipText"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonPurple else DarkBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onFilterSelected(format) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("filter_chip_${format?.name ?: "ALL"}")
                    ) {
                        Text(
                            text = label,
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
