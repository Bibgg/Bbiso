package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Community
import com.example.data.model.PostFormat
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UpvoteOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    communities: List<Community>,
    onDismiss: () -> Unit,
    onSubmit: (PostFormat, String?, String, List<String>, Community?, List<String>) -> Unit,
    onOpenStudioMediaUpload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember { mutableStateOf(PostFormat.CHATTER) }
    var titleText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }
    var selectedCommunity by remember { mutableStateOf(communities.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceLow,
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Create on OmniBox",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Pillar Format Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceMedium)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FormatPill(
                    label = "Tube",
                    icon = Icons.Default.PlayCircle,
                    isSelected = selectedFormat == PostFormat.TUBE,
                    onClick = { selectedFormat = PostFormat.TUBE },
                    modifier = Modifier.weight(1f)
                )
                FormatPill(
                    label = "Snap",
                    icon = Icons.Default.PhotoLibrary,
                    isSelected = selectedFormat == PostFormat.SNAP,
                    onClick = { selectedFormat = PostFormat.SNAP },
                    modifier = Modifier.weight(1f)
                )
                FormatPill(
                    label = "Chatter",
                    icon = Icons.Default.Chat,
                    isSelected = selectedFormat == PostFormat.CHATTER,
                    onClick = { selectedFormat = PostFormat.CHATTER },
                    modifier = Modifier.weight(1f)
                )
                FormatPill(
                    label = "Forum",
                    icon = Icons.Default.Forum,
                    isSelected = selectedFormat == PostFormat.FORUM,
                    onClick = { selectedFormat = PostFormat.FORUM },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Community Selector for Forum
                if (selectedFormat == PostFormat.FORUM) {
                    Text(
                        text = "Choose Sub-Community",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(communities) { comm ->
                            val isSelected = selectedCommunity?.id == comm.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) UpvoteOrange else DarkSurfaceMedium)
                                    .clickable { selectedCommunity = comm }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = comm.slug,
                                    color = if (isSelected) OledBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Title Input (For Tube & Forum)
                if (selectedFormat == PostFormat.TUBE || selectedFormat == PostFormat.FORUM) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Title", color = TextSecondary) },
                        placeholder = {
                            Text(
                                if (selectedFormat == PostFormat.TUBE) "e.g., Unreal Engine 5.5 City Showcase" else "e.g., [Discussion] Clean Architecture in Compose",
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceMedium,
                            unfocusedContainerColor = DarkSurfaceMedium,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Body / Caption Input
                OutlinedTextField(
                    value = bodyText,
                    onValueChange = {
                        if (selectedFormat == PostFormat.CHATTER && it.length <= 280) {
                            bodyText = it
                        } else if (selectedFormat != PostFormat.CHATTER) {
                            bodyText = it
                        }
                    },
                    label = {
                        Text(
                            when (selectedFormat) {
                                PostFormat.TUBE -> "Video Description"
                                PostFormat.SNAP -> "Write a Caption"
                                PostFormat.CHATTER -> "What's happening?"
                                PostFormat.FORUM -> "Post Content (Markdown supported)"
                            },
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("create_body_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceMedium,
                        unfocusedContainerColor = DarkSurfaceMedium,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 6
                )

                // Chatter character counter
                if (selectedFormat == PostFormat.CHATTER) {
                    Text(
                        text = "${280 - bodyText.length} characters remaining",
                        color = if (bodyText.length > 260) UpvoteOrange else TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                // Tags Input
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("Tags (comma separated)", color = TextSecondary) },
                    placeholder = { Text("Compose, Android, Tech", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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

                // Attached media indicator + Studio Media Uploader button
                if (selectedFormat == PostFormat.TUBE || selectedFormat == PostFormat.SNAP) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceMedium)
                            .clickable {
                                onDismiss()
                                onOpenStudioMediaUpload()
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedFormat == PostFormat.TUBE) Icons.Default.VideoLibrary else Icons.Default.Image,
                                contentDescription = "Media Attached",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedFormat == PostFormat.TUBE) "1 Video Ready (1080p 60fps)" else "2 High-Res Photos Selected",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to open Studio Media Uploader 🚀",
                                    color = NeonPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Publish Button
            val isValid = bodyText.isNotBlank() && (selectedFormat != PostFormat.TUBE || titleText.isNotBlank())
            Button(
                onClick = {
                    if (isValid) {
                        val tags = tagText.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                        val title = if (selectedFormat == PostFormat.TUBE || selectedFormat == PostFormat.FORUM) titleText.trim() else null
                        if (selectedFormat == PostFormat.TUBE || selectedFormat == PostFormat.SNAP) {
                            onDismiss()
                            onOpenStudioMediaUpload()
                        } else {
                            onSubmit(selectedFormat, title, bodyText.trim(), emptyList(), selectedCommunity, tags)
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_post_btn"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = OledBlack,
                    disabledContainerColor = DarkSurfaceMedium,
                    disabledContentColor = TextMuted
                )
            ) {
                Text(
                    text = "Publish to OmniBox",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormatPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonPurple else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) OledBlack else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) OledBlack else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
