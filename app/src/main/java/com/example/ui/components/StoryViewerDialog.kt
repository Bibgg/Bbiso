package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoryItem
import com.example.ui.components.OmniInlineVideoPlayer
import com.example.ui.components.audio.AudioSpectrumVisualizer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.LikeMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun StoryViewerDialog(
    story: StoryItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progressStarted by remember(story.id) { mutableStateOf(false) }
    val storyDurationMs = ((story.durationSeconds?.times(1000.0)?.toLong()) ?: if (story.isVideo) 15000L else 5000L).coerceIn(5000L, 30000L)
    var replyText by remember { mutableStateOf("") }

    val progress by animateFloatAsState(
        targetValue = if (progressStarted) 1f else 0f,
        animationSpec = tween(durationMillis = storyDurationMs.toInt(), easing = LinearEasing),
        label = "storyTimer"
    )

    LaunchedEffect(story.id) {
        progressStarted = true
        delay(storyDurationMs)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Fullscreen story background: videos play from Cloudinary; photos use the thumbnail.
            if (story.isVideo && story.mediaUrl.isNotBlank()) {
                OmniInlineVideoPlayer(
                    videoUrl = story.mediaUrl,
                    thumbnailUrl = story.previewImage,
                    isPlaying = true,
                    isMuted = true,
                    onTogglePlay = {},
                    onToggleMute = {},
                    modifier = Modifier.fillMaxSize(),
                    isControlsVisible = false
                )
            } else {
                AsyncImage(
                    model = story.previewImage,
                    contentDescription = story.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top gradient for status bar and header visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color.Transparent)
                        )
                    )
            )

            // Bottom gradient for caption and reply bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xEE000000))
                        )
                    )
            )

            // Header Elements (Timer Bar + User Info + Close)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonPurple,
                    trackColor = Color(0x66FFFFFF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = story.user.avatarUrl,
                        contentDescription = story.user.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.user.displayName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = story.timestamp,
                            color = Color(0xBBFFFFFF),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_story_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Bottom Caption & Quick Reply
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to ${story.user.displayName}...", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceMedium,
                            unfocusedContainerColor = DarkSurfaceMedium,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            replyText = ""
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LikeMagenta)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart React",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
