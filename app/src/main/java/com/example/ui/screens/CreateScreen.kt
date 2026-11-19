package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostFormat
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** The five entries on the Create screen. */
enum class CreateKind(
    val label: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val accent: Color,
    val cardColor: Color,
    val borderColor: Color,
    val format: PostFormat,
    val isStory: Boolean,
    val mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType
) {
    PHOTO(
        label = "Upload Photo",
        icon = Icons.Default.Image,
        accent = Color(0xFFA855F7),
        cardColor = Color(0xFF111827),
        borderColor = Color(0xFF1F2937),
        format = PostFormat.SNAP,
        isStory = false,
        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
    ),
    VIDEO_POST(
        label = "Upload Video Post",
        icon = Icons.Default.PhotoCamera,
        accent = Color(0xFF3B82F6),
        cardColor = Color(0xFF111827),
        borderColor = Color(0xFF1F2937),
        format = PostFormat.TUBE,
        isStory = false,
        mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
    ),
    REEL(
        label = "Upload Reel",
        icon = Icons.Default.Movie,
        accent = Color(0xFFEC4899),
        cardColor = Color(0xFF3B0A1A),
        borderColor = Color(0xFF7F1D3A),
        format = PostFormat.TUBE,
        isStory = false,
        mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
    ),
    STORY(
        label = "Upload Story",
        icon = Icons.Default.AddCircleOutline,
        accent = Color(0xFF22D3EE),
        cardColor = Color(0xFF0B1B33),
        borderColor = Color(0xFF123A5C),
        format = PostFormat.SNAP,
        isStory = true,
        mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
    ),
    GALLERY(
        label = "Open Gallery",
        subtitle = "Photos & Videos",
        icon = Icons.Default.PhotoLibrary,
        accent = Color(0xFF22C55E),
        cardColor = Color(0xFF1F2937),
        borderColor = Color(0xFF374151),
        format = PostFormat.SNAP,
        isStory = false,
        mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
    )
}

/**
 * Full-screen Create screen: pick what to upload, choose the media, then Post.
 *
 * [onPost] receives (format, mediaUri, isVideo, isStory, caption).
 */
@Composable
fun CreateScreen(
    onDismiss: () -> Unit,
    isUploading: Boolean,
    uploadProgressPercent: Int?,
    initialKind: CreateKind? = null,
    onPost: (PostFormat, Uri, Boolean, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(initialKind) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    // Opened from a story shortcut: go straight to the picker.
    LaunchedEffect(initialKind) {
        initialKind?.let { picker.launch(PickVisualMediaRequest(it.mediaType)) }
    }

    fun choose(selected: CreateKind) {
        kind = selected
        picker.launch(PickVisualMediaRequest(selected.mediaType))
    }

    val isVideo = pickedUri?.let { context.contentResolver.getType(it).orEmpty().startsWith("video/") } == true
    val canPost = pickedUri != null && !isUploading

    Surface(modifier = Modifier.fillMaxSize(), color = OledBlack) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header: Cancel | Create | Post
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(enabled = !isUploading) { onDismiss() }
                )
                Text(
                    text = "Create",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Post",
                    color = if (canPost) TextPrimary else TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = canPost) {
                        val selected = kind ?: CreateKind.GALLERY
                        onPost(selected.format, pickedUri!!, isVideo, selected.isStory, caption.trim())
                    }
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))

            if (pickedUri == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CreateKind.entries.forEach { entry ->
                        if (entry == CreateKind.GALLERY) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
                        }
                        CreateOptionCard(entry) { choose(entry) }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceLow),
                        contentAlignment = Alignment.Center
                    ) {
                        // Coil also renders a video frame for video Uris when a video decoder is installed.
                        AsyncImage(
                            model = pickedUri,
                            contentDescription = "Selected media",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideo) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        text = "Change media",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(enabled = !isUploading) {
                            picker.launch(PickVisualMediaRequest((kind ?: CreateKind.GALLERY).mediaType))
                        }
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Write a caption...", color = TextMuted) },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DarkBorder,
                            unfocusedBorderColor = DarkBorder
                        )
                    )
                    if (isUploading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Uploading${uploadProgressPercent?.let { " $it%" } ?: "..."}",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateOptionCard(entry: CreateKind, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(16.dp),
        color = entry.cardColor,
        border = BorderStroke(1.dp, entry.borderColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.label,
                color = when (entry) {
                    CreateKind.REEL -> Color(0xFFFBCFE8)
                    CreateKind.STORY -> Color(0xFFA5F3FC)
                    else -> TextPrimary
                },
                fontSize = 15.sp,
                fontWeight = if (entry == CreateKind.REEL || entry == CreateKind.STORY || entry == CreateKind.GALLERY) FontWeight.Bold else FontWeight.Normal
            )
            entry.subtitle?.let {
                Text(text = it, color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}
