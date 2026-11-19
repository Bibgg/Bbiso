package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostFormat
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Simple one-screen video upload: pick a clip, add an optional caption, publish. */
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

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Selected video"
        }
    }

    val progress = (uploadProgressPercent ?: 0).coerceIn(0, 100)

    Scaffold(
        modifier = modifier.fillMaxSize().background(OledBlack),
        containerColor = OledBlack,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, enabled = !isUploading, modifier = Modifier.testTag("creator_back")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Upload video", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceLow)
                    .clickable(enabled = !isUploading) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    .testTag("creator_pick_video"),
                contentAlignment = Alignment.Center
            ) {
                if (videoUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(58.dp).clip(CircleShape).background(NeonPurple), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = OledBlack, modifier = Modifier.size(30.dp))
                        }
                        Text("Choose video from gallery", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap here to select a video", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(38.dp))
                        Text("Video selected", color = CyberCyan, fontWeight = FontWeight.Bold)
                        Text(fileName, color = TextPrimary, fontSize = 12.sp, maxLines = 2)
                        Text("Tap to change video", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = { videoUri = null; fileName = "" },
                        enabled = !isUploading,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove video", tint = TextPrimary)
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(2000) },
                modifier = Modifier.fillMaxWidth().height(130.dp).testTag("creator_caption"),
                label = { Text("Caption (optional)", color = TextSecondary) },
                placeholder = { Text("Write something about your video…", color = TextMuted) },
                enabled = !isUploading,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium,
                    unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkSurfaceLow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            if (isUploading) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceMedium).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Uploading video…", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("$progress%", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = OledBlack,
                    disabledContainerColor = DarkSurfaceMedium,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(19.dp))
                Text(if (isUploading) "Uploading…" else "  Upload video", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
