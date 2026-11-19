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
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Premium dark, single-screen video upload. Reuses the existing secure publish pipeline. */
@Composable
fun VideoCreatorStudioScreen(
    onDismiss: () -> Unit,
    onPostCreated: (PostFormat, String?, String, Uri, Boolean, Boolean, List<String>) -> Unit,
    isUploading: Boolean = false,
    uploadProgressPercent: Int? = null,
    modifier: Modifier = Modifier
) {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Selected video"
            isPlaying = true
        }
    }
    val progress = (uploadProgressPercent ?: 0).coerceIn(0, 100)
    val gradient = Brush.linearGradient(listOf(NeonPurple, Color(0xFFFF4E9B), Color(0xFFFFA43A)))

    Scaffold(
        modifier = modifier.fillMaxSize().background(OledBlack),
        containerColor = OledBlack,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, enabled = !isUploading, modifier = Modifier.testTag("creator_back")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("Bbiso", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Create • Share • Enjoy", color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("✦", color = Color(0xFFFFC857), fontSize = 23.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text("Upload Video", color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("Turn your moments into amazing content ✨", color = TextSecondary, fontSize = 12.sp)

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceLow)
                    .border(1.dp, gradient, RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (videoUri == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(145.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF281449), Color(0xFF17152D), Color(0xFF382044))
                                )
                            )
                            .clickable(enabled = !isUploading) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                            .testTag("creator_pick_video"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(42.dp))
                            Text("Choose a video", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Select from your gallery", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 12f)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.Black)
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
                            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "VIDEO PREVIEW",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Text(
                                "Change",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(Color(0xAA000000))
                                    .clickable(enabled = !isUploading) {
                                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    }.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Text(fileName, color = TextSecondary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Text("Selected", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { videoUri = null; fileName = "" },
                            enabled = !isUploading,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove video", tint = TextSecondary, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("creator_choose_video"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB735E8), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (videoUri == null) "  Choose Video" else "  Choose Another Video", fontWeight = FontWeight.Bold)
                }
                Text("From Gallery  •  MP4, MOV and other supported formats", color = TextSecondary, fontSize = 10.sp)
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(2000) },
                modifier = Modifier.fillMaxWidth().height(112.dp).testTag("creator_caption"),
                label = { Text("Add Caption", color = TextSecondary) },
                placeholder = { Text("Write a caption… ❤️", color = TextMuted) },
                enabled = !isUploading,
                maxLines = 4,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium,
                    unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            if (isUploading) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceMedium).padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Uploading your video…", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("$progress%", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = NeonPurple,
                        trackColor = DarkSurfaceLow
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val uri = videoUri ?: return@Button
                    onPostCreated(PostFormat.TUBE, null, caption.trim(), uri, true, false, emptyList())
                },
                enabled = videoUri != null && !isUploading,
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("creator_publish"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB735E8),
                    contentColor = Color.White,
                    disabledContainerColor = DarkSurfaceMedium,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(19.dp))
                Text(if (isUploading) "Uploading…" else "  Upload", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Text("Fast • Easy • Secure", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
