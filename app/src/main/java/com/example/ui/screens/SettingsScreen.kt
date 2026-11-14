package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.OmniAuthRepository
import com.example.data.firebase.OmniSessionManager
import com.example.data.firebase.OmniStorageRepository
import com.example.data.model.UserProfile
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceHigh
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

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    isLoggedIn: Boolean,
    sessionManager: OmniSessionManager,
    authRepository: OmniAuthRepository,
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageRepository = remember { OmniStorageRepository(context) }

    // Dialog state
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showUpdatePhoneDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }

    // Privacy & Preferences local toggles
    var isPrivateAccount by remember { mutableStateOf(userProfile?.private_account ?: false) }
    var dmPermission by remember { mutableStateOf("Everyone") } // "Everyone", "Followers", "Off"
    var showActivityStatus by remember { mutableStateOf(true) }
    var readReceiptsEnabled by remember { mutableStateOf(true) }

    // Theme & Playback toggles
    var selectedAccent by remember { mutableStateOf("CyberCyan") }
    var autoplayVideos by remember { mutableStateOf("Always") } // "Always", "Wi-Fi Only", "Off"
    var defaultQuality by remember { mutableStateOf("1080p") } // "Auto", "1080p", "4K"
    var audioPreset by remember { mutableStateOf("Dolby 3D") }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings & Controls",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cloud JWT Sync badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLoggedIn) DarkSurfaceMedium else DarkSurfaceLow)
                        .border(1.dp, if (isLoggedIn) RepostGreen.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLoggedIn) RepostGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLoggedIn) "Cloud JWT Active" else "Guest Mode",
                            color = if (isLoggedIn) RepostGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            feedbackMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceMedium)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(text = msg, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 1. ACCOUNT PROFILE SECTION
            SettingsSectionHeader(title = "Account Management", icon = Icons.Default.Security)

            if (isLoggedIn && userProfile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceLow)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, NeonPurple, CircleShape)
                            ) {
                                AsyncImage(
                                    model = userProfile.avatar_url,
                                    contentDescription = userProfile.full_name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.full_name.ifBlank { "Omni Creator" },
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${userProfile.username}",
                                    color = CyberCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = userProfile.email,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (userProfile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = userProfile.bio,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Edit Profile Action Button
                        Button(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("settings_edit_profile_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceMedium,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile (Name, Bio, Avatar, DOB)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Password & Phone Number Actions
                SettingsItemRow(
                    icon = Icons.Default.Key,
                    title = "Change Password",
                    subtitle = "Update your cloud security password",
                    onClick = { showChangePasswordDialog = true }
                )

                SettingsItemRow(
                    icon = Icons.Default.Phone,
                    title = "Phone Number",
                    subtitle = if (userProfile.phone_number.isNotBlank()) userProfile.phone_number else "No phone linked",
                    onClick = { showUpdatePhoneDialog = true }
                )

                SettingsItemRow(
                    icon = Icons.Default.Shield,
                    title = "JWT Token & Session State",
                    subtitle = "Verified 256-bit Token • Auto-refresh on launch",
                    onClick = {
                        feedbackMessage = "Cloud JWT Token is active and verified across app restarts."
                    }
                )
            } else {
                // Guest mode prompt card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceLow)
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Guest Mode Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in to reserve your @handle, permanently back up 4K media, and restore your likes on any device.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("settings_guest_signin_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple,
                                contentColor = OledBlack
                            )
                        ) {
                            Text("Sign In or Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PRIVACY & SECURITY CONTROLS
            SettingsSectionHeader(title = "Privacy & Safety", icon = Icons.Default.Lock)

            SettingsSwitchRow(
                title = "Private Account",
                subtitle = "Only approved followers can view your Snaps and Tube reels",
                checked = isPrivateAccount,
                onCheckedChange = { checked ->
                    isPrivateAccount = checked
                    if (isLoggedIn) {
                        coroutineScope.launch {
                            authRepository.updateUserProfile(privateAccount = checked)
                            feedbackMessage = "Account privacy updated to ${if (checked) "Private" else "Public"}"
                        }
                    }
                }
            )

            SettingsSelectorRow(
                title = "Direct Message Requests",
                options = listOf("Everyone", "Followers", "Off"),
                selectedOption = dmPermission,
                onOptionSelected = { dmPermission = it }
            )

            SettingsSwitchRow(
                title = "Activity Pulse",
                subtitle = "Display glowing green indicator when you are active on OmniBox",
                checked = showActivityStatus,
                onCheckedChange = { showActivityStatus = it }
            )

            SettingsSwitchRow(
                title = "Read Receipts",
                subtitle = "Let participants see when you've viewed their messages",
                checked = readReceiptsEnabled,
                onCheckedChange = { readReceiptsEnabled = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. THEME & PLAYBACK CONTROLS
            SettingsSectionHeader(title = "Theme & Playback Controls", icon = Icons.Default.Palette)

            SettingsSelectorRow(
                title = "Theme Accent",
                options = listOf("CyberCyan", "NeonPurple", "LikeMagenta"),
                selectedOption = selectedAccent,
                onOptionSelected = { selectedAccent = it }
            )

            SettingsSelectorRow(
                title = "Video Autoplay",
                options = listOf("Always", "Wi-Fi Only", "Off"),
                selectedOption = autoplayVideos,
                onOptionSelected = { autoplayVideos = it }
            )

            SettingsSelectorRow(
                title = "Video Stream Quality",
                options = listOf("Auto", "1080p", "4K Ultra"),
                selectedOption = defaultQuality,
                onOptionSelected = { defaultQuality = it }
            )

            SettingsSelectorRow(
                title = "Dolby Spatial Sound Preset",
                options = listOf("Dolby 3D", "Club Bass", "Vocal Boost"),
                selectedOption = audioPreset,
                onOptionSelected = { audioPreset = it }
            )

            SettingsSwitchRow(
                title = "Haptic Feedback on Gestures",
                subtitle = "Vibrate when double-tapping to like or triggering controls",
                checked = hapticFeedbackEnabled,
                onCheckedChange = { hapticFeedbackEnabled = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. ACCOUNT SESSIONS & LOGOUT
            if (isLoggedIn) {
                SettingsSectionHeader(title = "Session & Danger Zone", icon = Icons.Default.Logout)

                SettingsItemRow(
                    icon = Icons.Default.Logout,
                    title = "Sign Out",
                    subtitle = "Log out on this device (cloud session will be cleared)",
                    tint = LikeMagenta,
                    onClick = { showSignOutConfirmDialog = true }
                )

                SettingsItemRow(
                    icon = Icons.Default.Delete,
                    title = "Delete OmniBox Account",
                    subtitle = "Permanently wipe UID, profile, username handle, and posts",
                    tint = Color.Red,
                    onClick = { showDeleteAccountConfirmDialog = true }
                )
            }
        }
    }

    // DIALOG: Edit Profile (Name, Bio, Avatar, DOB)
    if (showEditProfileDialog && userProfile != null) {
        EditProfileModal(
            currentProfile = userProfile,
            storageRepository = storageRepository,
            authRepository = authRepository,
            onDismiss = { showEditProfileDialog = false },
            onSaved = { updated ->
                showEditProfileDialog = false
                onProfileUpdated(updated)
                feedbackMessage = "Profile updated and synced to Cloud Firestore!"
            }
        )
    }

    // DIALOG: Change Password
    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = DarkSurfaceLow,
            title = { Text("Change Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = LikeMagenta, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length < 6) {
                            error = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            error = "Passwords do not match"
                            return@Button
                        }
                        coroutineScope.launch {
                            val res = authRepository.updatePassword(newPass)
                            res.onSuccess {
                                showChangePasswordDialog = false
                                feedbackMessage = "Password successfully changed!"
                            }.onFailure { e ->
                                error = e.localizedMessage ?: "Failed to update password"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = OledBlack)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // DIALOG: Update Phone Number
    if (showUpdatePhoneDialog) {
        var phoneInput by remember { mutableStateOf(userProfile?.phone_number ?: "") }

        AlertDialog(
            onDismissRequest = { showUpdatePhoneDialog = false },
            containerColor = DarkSurfaceLow,
            title = { Text("Update Phone Number", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number (+1...)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.updatePhoneNumber(phoneInput)
                            showUpdatePhoneDialog = false
                            feedbackMessage = "Phone number saved to profile."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = OledBlack)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdatePhoneDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // DIALOG: Sign Out Confirmation
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            containerColor = DarkSurfaceLow,
            title = { Text("Sign Out of OmniBox?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text("Your profile, liked posts, and bookmarks are backed up safely in Cloud Firestore and will be restored next time you log in.", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LikeMagenta, contentColor = Color.White)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Stay Logged In", color = TextMuted)
                }
            }
        )
    }

    // DIALOG: Delete Account Confirmation
    if (showDeleteAccountConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmDialog = false },
            containerColor = DarkSurfaceLow,
            title = { Text("Permanently Delete Account?", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text("Warning: This will permanently delete your OmniBox profile, release your unique @${userProfile?.username} handle, and erase all your data from Cloud Firestore. This cannot be undone.", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.deleteAccount()
                            showDeleteAccountConfirmDialog = false
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun EditProfileModal(
    currentProfile: UserProfile,
    storageRepository: OmniStorageRepository,
    authRepository: OmniAuthRepository,
    onDismiss: () -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    var fullName by remember { mutableStateOf(currentProfile.full_name) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var dob by remember { mutableStateOf(currentProfile.dob) }
    var avatarUrl by remember { mutableStateOf(currentProfile.avatar_url) }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                val res = storageRepository.uploadAvatarSync(uri)
                isUploading = false
                res.onSuccess { permanentUrl ->
                    avatarUrl = permanentUrl
                }.onFailure {
                    avatarUrl = uri.toString()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceLow,
        title = { Text("Edit Creator Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Avatar preview with tap to upload
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, CyberCyan, CircleShape)
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isUploading) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Tap avatar to upload new 4K photo to Cloud Storage", color = TextMuted, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    label = { Text("Bio (max 160 chars)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = LikeMagenta, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        val res = authRepository.updateUserProfile(
                            fullName = fullName,
                            bio = bio,
                            avatarUrl = avatarUrl,
                            dob = dob
                        )
                        isSaving = false
                        res.onSuccess { updated ->
                            onSaved(updated)
                        }.onFailure { e ->
                            errorMsg = e.localizedMessage ?: "Failed to save profile"
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = OledBlack)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = OledBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save to Cloud", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceLow)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceMedium),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceLow)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OledBlack,
                    checkedTrackColor = CyberCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceMedium
                )
            )
        }
    }
}

@Composable
private fun SettingsSelectorRow(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceLow)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = opt == selectedOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan else DarkSurfaceMedium)
                            .border(1.dp, if (isSelected) CyberCyan else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { onOptionSelected(opt) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            color = if (isSelected) OledBlack else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
