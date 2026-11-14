package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostFormat
import com.example.ui.components.OmniInlineVideoPlayer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.RepostGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated short-video creator. It deliberately reuses the existing publish
 * callback so upload signing, Cloudinary verification and Firestore publishing
 * remain owned by the established pipeline.
 */
@Composable
fun VideoCreatorStudioScreen(
    onDismiss: () -> Unit,
    onPostCreated: (PostFormat, String?, String, Uri, Boolean, Boolean, List<String>) -> Unit,
    isUploading: Boolean = false,
    uploadProgressPercent: Int? = null,
    modifier: Modifier = Modifier
) {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) videoUri = uri }

    val progress = (uploadProgressPercent ?: 0).coerceIn(0, 100)

    Scaffold(
        modifier = modifier.fillMaxSize().background(OledBlack),
        containerColor = OledBlack,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, enabled = !isUploading, modifier = Modifier.testTag("creator_back")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("VIDEO CREATOR", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("Create • Preview • Publish", color = TextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("BBISO STUDIO", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding()
                .padding(horizontal = 16.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(DarkSurfaceLow).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(NeonPurple), contentAlignment = Alignment.Center) {
                    Text("1", color = OledBlack, fontWeight = FontWeight.Black)
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("Choose your video", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Select a clip from your phone gallery", color = TextSecondary, fontSize = 11.sp)
                }
                Text("MP4 • MOV • WEBM", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            if (videoUri == null) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(9f / 10f).clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceMedium)
                        .border(1.dp, Brush.linearGradient(listOf(NeonPurple, CyberCyan)), RoundedCornerShape(24.dp))
                        .clickable(enabled = !isUploading) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }.testTag("creator_pick_video"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(76.dp).clip(CircleShape).background(NeonPurple.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(38.dp))
                        }
                        Text("Select a video", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to open your device gallery", color = TextSecondary, fontSize = 12.sp)
                        Text("Your original video stays on your phone", color = RepostGreen, fontSize = 11.sp)
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(9f / 13f).clip(RoundedCornerShape(22.dp))
                        .background(Color.Black).border(1.dp, DarkBorder, RoundedCornerShape(22.dp))
                        .testTag("creator_video_preview")
                ) {
                    OmniInlineVideoPlayer(
                        videoUrl = videoUri.toString(),
                        thumbnailUrl = null,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onToggleMute = { isMuted = !isMuted },
                        modifier = Modifier.fillMaxSize()
                    )
                    Row(
                        Modifier.align(Alignment.TopStart).fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 6.dp))
                        Text("Change video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xAA000000))
                                .clickable(enabled = !isUploading) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(NeonPurple), contentAlignment = Alignment.Center) {
                    Text("2", color = OledBlack, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text("  Add your story", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it.take(120) },
                modifier = Modifier.fillMaxWidth().testTag("creator_title"),
                label = { Text("Video title", color = TextSecondary) },
                placeholder = { Text("Give your video a great title", color = TextMuted) },
                singleLine = true, enabled = !isUploading,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium, unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = NeonPurple, unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = caption, onValueChange = { caption = it.take(2000) },
                modifier = Modifier.fillMaxWidth().height(112.dp).testTag("creator_caption"),
                label = { Text("Caption", color = TextSecondary) },
                placeholder = { Text("Tell people what this video is about…", color = TextMuted) },
                maxLines = 4, enabled = !isUploading, shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium, unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = NeonPurple, unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = hashtags, onValueChange = { hashtags = it.take(300) },
                modifier = Modifier.fillMaxWidth().testTag("creator_hashtags"),
                label = { Text("Hashtags", color = TextSecondary) },
                placeholder = { Text("#travel #music #daily", color = TextMuted) },
                singleLine = true, enabled = !isUploading, shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium, unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            if (isUploading) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurfaceMedium).padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Uploading your video", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("$progress%", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(7.dp).clip(CircleShape),
                        color = NeonPurple, trackColor = DarkSurfaceLow
                    )
                    Text("Keep Bbiso open until publishing is complete.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }

            Button(
                onClick = {
                    val uri = videoUri ?: return@Button
                    val tags = hashtags.split(",", " ", "\n").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct().take(20)
                    onPostCreated(PostFormat.TUBE, title.trim().ifBlank { null }, caption.trim(), uri, true, false, tags)
                },
                enabled = videoUri != null && title.isNotBlank() && !isUploading,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("creator_publish"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = OledBlack, disabledContainerColor = DarkSurfaceMedium, disabledContentColor = TextMuted)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(19.dp))
                Text(if (isUploading) "Publishing…" else "  Publish video", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Text("Your upload is sent through Bbiso’s existing secure media pipeline.", color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
