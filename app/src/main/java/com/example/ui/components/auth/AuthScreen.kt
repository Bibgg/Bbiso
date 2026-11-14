package com.example.ui.components.auth

import android.app.Activity
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.HandleValidationState
import com.example.data.firebase.OmniAuthRepository
import com.example.data.firebase.OmniStorageRepository
import com.example.data.model.UserProfile
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=400&q=80",
    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80"
)

@Composable
fun AuthScreen(
    authRepository: OmniAuthRepository,
    onAuthSuccess: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageRepository = remember { OmniStorageRepository(context) }

    var signedInFirebaseUser by remember { mutableStateOf(authRepository.getCurrentUser()) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var authSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Auth tab selection: 0 = Google, 1 = Email Sign In, 2 = Email Sign Up
    var selectedAuthTab by remember { mutableIntStateOf(0) }

    // Email/Password fields
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Onboarding profile & unique handle validation state
    var rawHandleInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var dobInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var selectedAvatarUrl by remember { mutableStateOf(PRESET_AVATARS[0]) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var handleState by remember { mutableStateOf<HandleValidationState>(HandleValidationState.Idle) }
    var isSavingProfile by remember { mutableStateOf(false) }

    // Visual media picker for profile avatar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingAvatar = true
            coroutineScope.launch {
                val uploadResult = storageRepository.uploadAvatarSync(uri)
                isUploadingAvatar = false
                uploadResult.onSuccess { permanentUrl ->
                    selectedAvatarUrl = permanentUrl
                }.onFailure {
                    selectedAvatarUrl = uri.toString()
                }
            }
        }
    }

    // Real-time debounced handle checking against Firestore
    LaunchedEffect(rawHandleInput) {
        if (rawHandleInput.isBlank()) {
            handleState = HandleValidationState.Idle
            return@LaunchedEffect
        }
        delay(350)
        authRepository.validateHandleInRealTime(rawHandleInput).collect { state ->
            handleState = state
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceMedium)
                        .clickable { onDismiss() }
                        .testTag("auth_dismiss_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logo & Glow Header
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, CyberCyan))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = OledBlack,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "OmniBox Cloud Identity",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Persistent JWT Session Management, Real-Time Handle Uniqueness & Cloud Storage Backup",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // STEP 1: Authentication (Google Sign-In OR Email/Password)
            if (signedInFirebaseUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceLow)
                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Auth Method Tabs
                        TabRow(
                            selectedTabIndex = selectedAuthTab,
                            containerColor = DarkSurfaceMedium,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedAuthTab]),
                                    color = CyberCyan
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedAuthTab == 0,
                                onClick = { selectedAuthTab = 0; authErrorMessage = null },
                                text = { Text("Google", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedAuthTab == 1,
                                onClick = { selectedAuthTab = 1; authErrorMessage = null },
                                text = { Text("Email Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedAuthTab == 2,
                                onClick = { selectedAuthTab = 2; authErrorMessage = null },
                                text = { Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        when (selectedAuthTab) {
                            0 -> {
                                // Google Sign-In Tab
                                Text(
                                    text = "One-tap Google Play Services authentication with cryptographic token generation.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        isAuthenticating = true
                                        authErrorMessage = null
                                        coroutineScope.launch {
                                            val result = authRepository.signInWithGoogle(context as Activity)
                                            isAuthenticating = false
                                            result.onSuccess { user ->
                                                signedInFirebaseUser = user
                                                fullNameInput = user.displayName ?: ""
                                                val suggested = (user.email?.substringBefore("@") ?: user.displayName ?: "creator")
                                                    .replace(" ", "_")
                                                    .filter { it.isLetterOrDigit() || it == '_' }
                                                rawHandleInput = suggested
                                            }.onFailure { err ->
                                                authErrorMessage = err.localizedMessage ?: "Google Sign-In failed"
                                            }
                                        }
                                    },
                                    enabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("google_signin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPurple,
                                        contentColor = OledBlack
                                    )
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(color = OledBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                            1, 2 -> {
                                // Email/Password Sign In or Register Tab
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CyberCyan) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_email_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password (min 6 chars)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonPurple) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_password_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonPurple,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val isRegister = selectedAuthTab == 2
                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || passwordInput.length < 6) {
                                            authErrorMessage = "Please enter a valid email and password (min 6 characters)"
                                            return@Button
                                        }
                                        isAuthenticating = true
                                        authErrorMessage = null
                                        coroutineScope.launch {
                                            val result = if (isRegister) {
                                                authRepository.createUserWithEmailAndPassword(emailInput, passwordInput)
                                            } else {
                                                authRepository.signInWithEmailAndPassword(emailInput, passwordInput)
                                            }
                                            isAuthenticating = false
                                            result.onSuccess { user ->
                                                signedInFirebaseUser = user
                                                fullNameInput = user.displayName ?: emailInput.substringBefore("@")
                                                val suggested = emailInput.substringBefore("@")
                                                    .replace(".", "_")
                                                    .filter { it.isLetterOrDigit() || it == '_' }
                                                rawHandleInput = suggested
                                            }.onFailure { err ->
                                                authErrorMessage = err.localizedMessage ?: "Authentication failed"
                                            }
                                        }
                                    },
                                    enabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("auth_email_submit_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRegister) CyberCyan else NeonPurple,
                                        contentColor = OledBlack
                                    )
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(color = OledBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = if (isRegister) "Create OmniBox Account" else "Sign In with Email",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                if (!isRegister) {
                                    TextButton(
                                        onClick = {
                                            if (emailInput.isNotBlank()) {
                                                coroutineScope.launch {
                                                    authRepository.sendPasswordResetEmail(emailInput)
                                                    authSuccessMessage = "Password reset link sent to $emailInput"
                                                }
                                            } else {
                                                authErrorMessage = "Enter your email first to receive a reset link"
                                            }
                                        }
                                    ) {
                                        Text("Forgot Password?", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        authErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = err, color = LikeMagenta, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }

                        authSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = msg, color = RepostGreen, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                // STEP 2: Profile Onboarding Flow: Display Name, unique handle, DOB, Avatar, and Bio
                val user = signedInFirebaseUser!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceLow)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RepostGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Authenticated as ${user.email ?: user.uid.take(8)}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Set Up Your Creator Profile",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Choose your avatar, unique @handle, birth date, and bio. Everything syncs permanently to Cloud Firestore.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar Picker & Upload Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, NeonPurple, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedAvatarUrl,
                                    contentDescription = "Avatar Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isUploadingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .align(Alignment.BottomEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text("Profile Picture", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tap to upload custom photo or pick preset:", color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PRESET_AVATARS.take(4).forEach { url ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (selectedAvatarUrl == url) 2.dp else 1.dp,
                                                    color = if (selectedAvatarUrl == url) CyberCyan else DarkBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedAvatarUrl = url }
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real-Time Unique Handle Field
                        OutlinedTextField(
                            value = rawHandleInput,
                            onValueChange = { rawHandleInput = it },
                            label = { Text("Unique Handle (@username)") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = CyberCyan) },
                            trailingIcon = {
                                when (handleState) {
                                    is HandleValidationState.Checking -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberCyan, strokeWidth = 2.dp)
                                    is HandleValidationState.Available -> Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = RepostGreen)
                                    is HandleValidationState.Taken -> Icon(Icons.Default.Error, contentDescription = "Taken", tint = LikeMagenta)
                                    is HandleValidationState.Invalid -> Icon(Icons.Default.Error, contentDescription = "Invalid", tint = LikeMagenta)
                                    else -> {}
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("handle_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Handle status feedback
                        Spacer(modifier = Modifier.height(4.dp))
                        when (handleState) {
                            is HandleValidationState.Available -> {
                                Text("✓ @${(handleState as HandleValidationState.Available).handle} is available globally!", color = RepostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            is HandleValidationState.Taken -> {
                                Text("✕ @${(handleState as HandleValidationState.Taken).handle} is already taken in Firestore.", color = LikeMagenta, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            is HandleValidationState.Invalid -> {
                                Text((handleState as HandleValidationState.Invalid).reason, color = LikeMagenta, fontSize = 11.sp)
                            }
                            is HandleValidationState.Checking -> {
                                Text("Validating handle uniqueness in Cloud Firestore...", color = CyberCyan, fontSize = 11.sp)
                            }
                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full name input
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonPurple) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fullname_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date of birth
                        OutlinedTextField(
                            value = dobInput,
                            onValueChange = { dobInput = it },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            placeholder = { Text("2000-01-01", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CyberCyan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dob_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bio Field
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { if (it.length <= 160) bioInput = it },
                            label = { Text("Bio / Description") },
                            placeholder = { Text("Digital creator, filmmaker, tech enthusiast...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonPurple) },
                            supportingText = { Text("${bioInput.length}/160", color = TextMuted, fontSize = 10.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bio_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Complete Profile Registration Button
                        val canSave = handleState is HandleValidationState.Available && !isSavingProfile
                        Button(
                            onClick = {
                                isSavingProfile = true
                                coroutineScope.launch {
                                    val result = authRepository.registerUserProfileWithHandle(
                                        username = rawHandleInput,
                                        fullName = fullNameInput,
                                        dob = dobInput,
                                        avatarUrl = selectedAvatarUrl,
                                        bio = bioInput
                                    )
                                    isSavingProfile = false
                                    result.onSuccess { profile ->
                                        onAuthSuccess(profile)
                                    }.onFailure { e ->
                                        authErrorMessage = e.localizedMessage ?: "Failed to save profile"
                                    }
                                }
                            },
                            enabled = canSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_profile_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = OledBlack
                            )
                        ) {
                            if (isSavingProfile) {
                                CircularProgressIndicator(color = OledBlack, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("Complete Onboarding & Enter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = { onDismiss() }) {
                Text("Continue as Guest Preview", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}
