package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.FeedPost
import com.example.ui.components.OmniInlineVideoPlayer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkSurfaceLow
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProfileMediaTab
import com.example.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // System PhotoPicker launcher for selecting a new avatar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadProfilePhoto(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 1. Profile Header Span
            item(span = { GridItemSpan(3) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Bar inside Profile: Username title + Settings Gear Icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.userProfile.username.startsWith("@")) uiState.userProfile.username else "@${uiState.userProfile.username}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.testTag("profile_username_title")
                        )

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("profile_settings_gear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings & Controls",
                                tint = CyberCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Avatar + Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Large Avatar with PhotoPicker Click Action
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_tap"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glowing border
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NeonPurple, CyberCyan))
                                    )
                                    .padding(2.5.dp)
                            ) {
                                AsyncImage(
                                    model = uiState.userProfile.avatar_url.ifBlank {
                                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
                                    },
                                    contentDescription = "Profile Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(DarkSurfaceHigh)
                                )
                            }

                            // Camera badge overlay
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(NeonPurple)
                                    .border(2.dp, OledBlack, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = OledBlack
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = OledBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Stats: Posts, Followers, Following
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ProfileStatColumn(
                                count = "${uiState.postsCount}",
                                label = "Posts"
                            )
                            ProfileStatColumn(
                                count = "1.2k",
                                label = "Followers"
                            )
                            ProfileStatColumn(
                                count = "388",
                                label = "Following"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Display Name & Handle
                    Text(
                        text = uiState.userProfile.display_name.ifBlank { "bishal gc" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.testTag("profile_display_name")
                    )

                    // Bio
                    Text(
                        text = uiState.userProfile.bio.ifBlank { "Digital Creator & Filmmaker 🎬 | Exploring AI & OmniBox" },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("profile_bio")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row (Edit Profile + Change Photo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleEditProfile(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("edit_profile_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DarkSurfaceMedium,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = NeonPurple
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("change_photo_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceHigh,
                                contentColor = CyberCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Photo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Structured Media Tabs: "My Videos", "Photos", "Saved"
                    TabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = OledBlack,
                        contentColor = NeonPurple,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                                color = NeonPurple,
                                height = 2.5.dp
                            )
                        },
                        divider = {
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(DarkBorder))
                        }
                    ) {
                        Tab(
                            selected = uiState.selectedTab == ProfileMediaTab.MY_VIDEOS,
                            onClick = { viewModel.onTabSelected(ProfileMediaTab.MY_VIDEOS) },
                            icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            text = { Text("My Videos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = NeonPurple,
                            unselectedContentColor = TextMuted,
                            modifier = Modifier.testTag("tab_my_videos")
                        )
                        Tab(
                            selected = uiState.selectedTab == ProfileMediaTab.PHOTOS,
                            onClick = { viewModel.onTabSelected(ProfileMediaTab.PHOTOS) },
                            icon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            text = { Text("Photos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = NeonPurple,
                            unselectedContentColor = TextMuted,
                            modifier = Modifier.testTag("tab_photos")
                        )
                        Tab(
                            selected = uiState.selectedTab == ProfileMediaTab.SAVED,
                            onClick = { viewModel.onTabSelected(ProfileMediaTab.SAVED) },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            text = { Text("Saved", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = NeonPurple,
                            unselectedContentColor = TextMuted,
                            modifier = Modifier.testTag("tab_saved")
                        )
                    }
                }
            }

            // 2. Media Grid Items (3-column Instagram-style grid)
            val currentList = when (uiState.selectedTab) {
                ProfileMediaTab.MY_VIDEOS -> uiState.userVideos
                ProfileMediaTab.PHOTOS -> uiState.userPhotos
                ProfileMediaTab.SAVED -> uiState.savedPosts
            }

            if (currentList.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (uiState.selectedTab) {
                                    ProfileMediaTab.MY_VIDEOS -> Icons.Default.Videocam
                                    ProfileMediaTab.PHOTOS -> Icons.Default.GridOn
                                    ProfileMediaTab.SAVED -> Icons.Default.Bookmark
                                },
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No media in this category yet",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(currentList, key = { it.id }) { post ->
                    ProfileMediaGridItem(
                        post = post,
                        onClick = { viewModel.openVideoPlayer(post) }
                    )
                }
            }
        }

        // Full Screen Video Player Dialog when a video is clicked
        uiState.activePlayingVideo?.let { playingPost ->
            Dialog(
                onDismissRequest = { viewModel.closeVideoPlayer() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBlack)
                ) {
                    val mediaUrl = playingPost.mediaUrls.firstOrNull() ?: ""
                    var isPlaying by remember { mutableStateOf(true) }
                    var isMuted by remember { mutableStateOf(false) }
                    OmniInlineVideoPlayer(
                        videoUrl = mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onToggleMute = { isMuted = !isMuted }
                    )

                    // Close button top-left
                    IconButton(
                        onClick = { viewModel.closeVideoPlayer() },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(DarkSurfaceLow.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Video",
                            tint = Color.White
                        )
                    }

                    // Bottom info overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = playingPost.title ?: playingPost.body,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playingPost.viewCount} views",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Edit Profile Modal
        if (uiState.isEditingProfile) {
            EditProfileDialog(
                currentDisplayName = uiState.userProfile.display_name,
                currentBio = uiState.userProfile.bio,
                onDismiss = { viewModel.toggleEditProfile(false) },
                onSave = { name, bio -> viewModel.updateProfileDetails(name, bio) }
            )
        }
    }
}

@Composable
private fun ProfileStatColumn(
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = count,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ProfileMediaGridItem(
    post: FeedPost,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square grid like Instagram
            .background(DarkSurfaceMedium)
            .clickable { onClick() }
            .testTag("profile_grid_item_${post.id}")
    ) {
        // Thumbnail image
        val thumbnail = post.mediaUrls.firstOrNull() ?: ""
        AsyncImage(
            model = thumbnail,
            contentDescription = post.title ?: "Post Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient shadow at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // View count and Play Icon indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = post.viewCount,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentDisplayName: String,
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentDisplayName) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(displayName, bio) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Save", color = OledBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceLow
    )
}
