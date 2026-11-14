package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.audio.OmniVoiceTransformer
import com.example.audio.VoiceFilterType
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostFormat
import com.example.ui.components.OmniInlineVideoPlayer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.LikeMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.RepostGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class UploadCategory(
    val label: String,
    val icon: ImageVector,
    val postFormat: PostFormat,
    val visualMediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType
) {
    TUBE(
        label = "Tube Video",
        icon = Icons.Default.VideoLibrary,
        postFormat = PostFormat.TUBE,
        visualMediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
    ),
    SNAP(
        label = "Snap Photo",
        icon = Icons.Default.Image,
        postFormat = PostFormat.SNAP,
        visualMediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
    ),
    STORY(
        label = "Story",
        icon = Icons.Default.PhotoCamera,
        postFormat = PostFormat.SNAP,
        visualMediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
    ),
    FILES(
        label = "File / Doc",
        icon = Icons.Default.InsertDriveFile,
        postFormat = PostFormat.FORUM,
        visualMediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
    )
}

sealed interface UploadPhase {
    data object Idle : UploadPhase
    data class Uploading(val progress: Float, val currentChunk: Int, val totalChunks: Int) : UploadPhase
    data class Processing(val stageText: String) : UploadPhase
    data object Completed : UploadPhase
}

@Composable
fun MediaUploadScreen(
    onDismiss: () -> Unit,
    onPostCreated: (PostFormat, String?, String, Uri, Boolean, Boolean, List<String>) -> Unit,
    isUploading: Boolean = false,
    uploadProgressPercent: Int? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(UploadCategory.TUBE) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var titleText by remember { mutableStateOf("") }
    var captionText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var uploadPhase by remember { mutableStateOf<UploadPhase>(UploadPhase.Idle) }
    var isPreviewPlaying by remember { mutableStateOf(true) }
    var isPreviewMuted by remember { mutableStateOf(false) }
    var selectedVoiceFilter by remember { mutableStateOf(VoiceFilterType.STUDIO_CLEAN) }
    var isNoiseSuppressionEnabled by remember { mutableStateOf(true) }

    // Native Android Zero-Permission Photo/Video Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedFileName = uri.lastPathSegment ?: "Selected Media"
        }
    }

    // Native Android System File/Document Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedFileName = uri.lastPathSegment?.split("/")?.lastOrNull() ?: "Document_${System.currentTimeMillis()}"
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledBlack)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("upload_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "New Upload",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (selectedMediaUri != null && uploadPhase is UploadPhase.Idle) {
                        Text(
                            text = "Reset",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { selectedMediaUri = null }
                                .padding(end = 16.dp)
                        )
                    }
                }

                // Category Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceMedium)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UploadCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else Color.Transparent)
                                .clickable(enabled = uploadPhase is UploadPhase.Idle) {
                                    if (selectedCategory != category) {
                                        selectedCategory = category
                                        selectedMediaUri = null // Reset selection on tab switch
                                    }
                                }
                                .padding(vertical = 8.dp)
                                .testTag("upload_tab_${category.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.label,
                                    tint = if (isSelected) OledBlack else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = category.label,
                                    color = if (isSelected) OledBlack else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = OledBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Instant Preview Card or Media Picker Drop Zone
            if (selectedMediaUri == null) {
                // Dropzone: triggers native picker based on selected category
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceLow)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(NeonPurple, CyberCyan)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (selectedCategory == UploadCategory.FILES) {
                                filePickerLauncher.launch("*/*")
                            } else {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(selectedCategory.visualMediaType)
                                )
                            }
                        }
                        .testTag("launch_media_picker_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedCategory == UploadCategory.FILES) Icons.Default.FolderOpen else Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                tint = CyberCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Select ${selectedCategory.label}",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (selectedCategory) {
                                UploadCategory.TUBE -> "Filter: MP4, MOV, WebM • Up to 4K 60fps"
                                UploadCategory.SNAP -> "Filter: JPG, PNG, WebP • Auto HDR"
                                UploadCategory.STORY -> "Filter: 9:16 Full-bleed Photos or Videos"
                                UploadCategory.FILES -> "Filter: PDF, ZIP, APK, Audio, Docs (*/*)"
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dual quick-picker actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceMedium)
                                    .clickable {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(selectedCategory.visualMediaType)
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Gallery",
                                        tint = NeonPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Gallery Picker",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceMedium)
                                    .clickable {
                                        filePickerLauncher.launch("*/*")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Device Files",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Browse Files (*/*)",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Instant Preview: File Document Card OR Media Image/Video Viewport
                if (selectedCategory == UploadCategory.FILES) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceMedium)
                            .border(1.dp, CyberCyan, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                            .testTag("file_preview_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = "File Attached",
                                    tint = OledBlack,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFileName ?: "Attached Document",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ready to upload • Merkle Checksum Verified",
                                    color = CyberCyan,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceLow)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Change File",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Instant Visual Media Preview Box with metadata tags
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (selectedCategory == UploadCategory.STORY) 9f / 16f else 16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .testTag("media_preview_box")
                    ) {
                        if (selectedCategory == UploadCategory.TUBE) {
                            OmniInlineVideoPlayer(
                                videoUrl = selectedMediaUri.toString(),
                                thumbnailUrl = null,
                                isPlaying = isPreviewPlaying,
                                isMuted = isPreviewMuted,
                                onTogglePlay = { isPreviewPlaying = !isPreviewPlaying },
                                onToggleMute = { isPreviewMuted = !isPreviewMuted },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Selected Media Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Top Media Spec Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xCC000000))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${selectedCategory.label} • Ready",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(selectedCategory.visualMediaType)
                                    )
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Change media",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Bottom Privacy & Edge Attestation Chip
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Edge Guard",
                                tint = RepostGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NPU Privacy Screen Passed",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. Metadata Inputs (Title, Caption, Tags)
            if (selectedCategory == UploadCategory.TUBE) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Video Title", color = TextSecondary) },
                    placeholder = { Text("Enter a catchy title...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_title_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceMedium,
                        unfocusedContainerColor = DarkSurfaceMedium,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    enabled = uploadPhase is UploadPhase.Idle
                )
            }

            OutlinedTextField(
                value = captionText,
                onValueChange = { captionText = it },
                label = { Text(if (selectedCategory == UploadCategory.TUBE) "Description" else "Caption", color = TextSecondary) },
                placeholder = { Text("Add details, mentions, and thoughts...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("upload_caption_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium,
                    unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4,
                enabled = uploadPhase is UploadPhase.Idle
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Hashtags", color = TextSecondary) },
                placeholder = { Text("e.g. #android, #compose, #future", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_tags_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceMedium,
                    unfocusedContainerColor = DarkSurfaceMedium,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                enabled = uploadPhase is UploadPhase.Idle
            )

            // Creator Audio Engine: Voice Transformers & AI Noise Suppression
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceLow)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Overlay & Audio Filters",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Preview Voice Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(NeonPurple.copy(alpha = 0.2f))
                                .border(1.dp, NeonPurple, RoundedCornerShape(14.dp))
                                .clickable {
                                    OmniVoiceTransformer.playVoiceFilterPreview(
                                        selectedVoiceFilter,
                                        isNoiseSuppressionEnabled
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("preview_voice_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Test Audio",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test Audio 🔊",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Voice Filter Chips Carousel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VoiceFilterType.entries.size) { idx ->
                            val filter = VoiceFilterType.entries[idx]
                            val isSelected = selectedVoiceFilter == filter

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NeonPurple else DarkSurfaceMedium)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedVoiceFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("voice_filter_${filter.name}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = filter.iconEmoji, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = filter.title,
                                        color = if (isSelected) OledBlack else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Selected filter description
                    Text(
                        text = selectedVoiceFilter.description,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    // AI Background Noise Cancellation Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceMedium)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isNoiseSuppressionEnabled) RepostGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AI Noise Suppression",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isNoiseSuppressionEnabled) "Active (-18dB ambient gate)" else "Bypassed",
                                    color = if (isNoiseSuppressionEnabled) RepostGreen else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Switch(
                            checked = isNoiseSuppressionEnabled,
                            onCheckedChange = { isNoiseSuppressionEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = RepostGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceLow
                            )
                        )
                    }
                }
            }

            if (selectedCategory == UploadCategory.STORY) {
                Text(
                    text = "Stories disappear from the feed after 24 hours.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 3. Upload Progress & State Card (YouTube/WhatsApp style)
            AnimatedVisibility(visible = uploadPhase !is UploadPhase.Idle || isUploading) {
                UploadProgressOverlay(phase = if (isUploading) UploadPhase.Uploading((uploadProgressPercent ?: 0) / 100f, 1, 1) else uploadPhase)
            }

            // 4. Primary Upload / Publish Button
            val isReady = selectedMediaUri != null &&
                    (selectedCategory != UploadCategory.TUBE || titleText.isNotBlank()) &&
                    uploadPhase is UploadPhase.Idle && !isUploading

            Button(
                onClick = {
                    if (isReady) {
                        coroutineScope.launch {
                            val uri = selectedMediaUri ?: return@launch
                            val tagsList = tagsText.split(",", " ")
                                .map { it.trim().removePrefix("#") }
                                .filter { it.isNotBlank() }
                            val mimeType = context.contentResolver.getType(uri).orEmpty()
                            val isVideo = mimeType.startsWith("video/")
                            onPostCreated(
                                selectedCategory.postFormat,
                                if (selectedCategory == UploadCategory.TUBE) titleText else if (selectedCategory == UploadCategory.FILES) (titleText.ifBlank { selectedFileName ?: "Attached File" }) else null,
                                if (captionText.isNotBlank()) captionText else if (selectedCategory == UploadCategory.FILES) "Attached file: ${selectedFileName ?: "file"}" else "",
                                uri,
                                isVideo,
                                selectedCategory == UploadCategory.STORY,
                                tagsList
                            )
                        }
                    }
                },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_upload_btn"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = OledBlack,
                    disabledContainerColor = DarkSurfaceMedium,
                    disabledContentColor = TextMuted
                )
            ) {
                Text(
                    text = when (uploadPhase) {
                        is UploadPhase.Idle -> if (selectedCategory == UploadCategory.STORY) "Share Story" else "Upload to OmniBox"
                        is UploadPhase.Uploading -> "Streaming Binary Chunks..."
                        is UploadPhase.Processing -> "Transcoding..."
                        is UploadPhase.Completed -> "Published!"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun UploadProgressOverlay(
    phase: UploadPhase,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceMedium)
            .border(1.dp, NeonPurple, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (phase) {
                is UploadPhase.Uploading -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = phase.progress,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "uploadPercent"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Uploading chunk ${phase.currentChunk} of ${phase.totalChunks}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            color = NeonPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonPurple,
                        trackColor = DarkSurfaceLow
                    )
                }

                is UploadPhase.Processing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = phase.stageText,
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberCyan,
                        trackColor = DarkSurfaceLow
                    )
                }

                is UploadPhase.Completed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = RepostGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Media Transcoded & Indexed!",
                            color = RepostGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is UploadPhase.Idle -> {}
            }
        }
    }
}
