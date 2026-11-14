package com.example

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.OmniGreetingEngine
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CreatePostBottomSheet
import com.example.ui.components.OmniBottomBar
import com.example.ui.components.OmniTopBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.audio.OmniSoundLabSheet
import com.example.ui.components.auth.AuthScreen
import com.example.ui.screens.ChatterScreen
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaUploadScreen
import com.example.ui.screens.UploadCategory
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SnapScreen
import com.example.ui.screens.TubeScreen
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OmniBoxTheme
import com.example.ui.viewmodel.OmniEffect
import com.example.ui.viewmodel.OmniTab
import com.example.ui.viewmodel.OmniViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OmniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launch dynamic welcome experience (Harmonic Chime + Voice Greeting)
        OmniGreetingEngine.onAppLaunch(applicationContext)

        setContent {
            OmniBoxTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                var isTubeControlsVisible by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    viewModel.authRepository.attemptAutoSignIn()
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is OmniEffect.ShowToast -> {
                                snackbarHostState.showSnackbar(effect.message)
                            }
                        }
                    }
                }

                // Reset controls visibility when leaving or returning to other tabs
                LaunchedEffect(uiState.activeTab) {
                    if (uiState.activeTab != OmniTab.TUBE) {
                        isTubeControlsVisible = true
                    }
                }

                // Hardware back handler for auth screen
                BackHandler(enabled = uiState.showAuthScreen) {
                    viewModel.closeAuthScreen()
                }

                // Hardware back handler for settings screen
                BackHandler(enabled = !uiState.showAuthScreen && uiState.showSettingsScreen) {
                    viewModel.closeSettingsScreen()
                }

                // Hardware back handler for media upload screen
                BackHandler(enabled = !uiState.showAuthScreen && !uiState.showSettingsScreen && uiState.showMediaUploadScreen) {
                    viewModel.closeMediaUploadScreen()
                }

                // Hardware back handler: return to Home tab if on sub-tab
                BackHandler(enabled = !uiState.showAuthScreen && !uiState.showSettingsScreen && !uiState.showMediaUploadScreen && uiState.activeTab != OmniTab.HOME) {
                    viewModel.onTabSelected(OmniTab.HOME)
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBlack),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        // Top bar is hidden on Tube full-screen shorts tab
                        if (uiState.activeTab != OmniTab.TUBE) {
                            OmniTopBar(
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = { filter -> viewModel.onFilterSelected(filter) },
                                showFilterChips = uiState.activeTab == OmniTab.HOME,
                                onOpenSoundLab = { viewModel.openSoundLab() },
                                userProfile = uiState.userProfile,
                                isLoggedIn = uiState.isLoggedIn,
                                onOpenAuth = { viewModel.openAuthScreen() },
                                onOpenSettings = { viewModel.openSettingsScreen() },
                                onSignOut = { viewModel.signOut() }
                            )
                        }
                    },
                    bottomBar = {
                        // Bottom navigation bar toggles smoothly along with Tube reels fullscreen overlays
                        AnimatedVisibility(
                            visible = uiState.activeTab != OmniTab.TUBE || isTubeControlsVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it }
                        ) {
                            OmniBottomBar(
                                currentTab = uiState.activeTab,
                                onTabSelected = { tab -> viewModel.onTabSelected(tab) },
                                onCreateClick = { viewModel.openCreateModal() }
                            )
                        }
                    },
                    containerColor = OledBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (uiState.activeTab == OmniTab.TUBE) PaddingValues(0.dp) else innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = uiState.activeTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tabContent"
                        ) { targetTab ->
                            when (targetTab) {
                                OmniTab.HOME -> HomeScreen(
                                    posts = uiState.posts,
                                    stories = uiState.stories,
                                    currentUser = uiState.currentUser,
                                    selectedFilter = uiState.selectedFilter,
                                    onStoryClick = { story -> viewModel.openStory(story) },
                                    onAddStoryClick = { viewModel.openStoryUploadScreen() },
                                    onLikeClick = { id -> viewModel.onToggleLike(id) },
                                    onVoteClick = { id, dir -> viewModel.onCastVote(id, dir) },
                                    onCommentClick = { id -> viewModel.openComments(id) },
                                    onBookmarkClick = { id -> viewModel.onToggleBookmark(id) },
                                    onRepostClick = { id -> viewModel.onRepost(id) },
                                    onPlayToggle = { id -> viewModel.onToggleVideoPlay(id) },
                                    onMuteToggle = { id -> viewModel.onToggleVideoMute(id) }
                                )

                                OmniTab.TUBE -> TubeScreen(
                                    posts = uiState.posts,
                                    onLikeClick = { id -> viewModel.onToggleLike(id) },
                                    onCommentClick = { id -> viewModel.openComments(id) },
                                    onBookmarkClick = { id -> viewModel.onToggleBookmark(id) },
                                    onControlsVisibilityChanged = { visible -> isTubeControlsVisible = visible }
                                )

                                OmniTab.SNAP -> SnapScreen(
                                    posts = uiState.posts,
                                    stories = uiState.stories,
                                    currentUser = uiState.currentUser,
                                    onStoryClick = { story -> viewModel.openStory(story) },
                                    onAddStoryClick = { viewModel.openCreateModal() },
                                    onLikeClick = { id -> viewModel.onToggleLike(id) },
                                    onCommentClick = { id -> viewModel.openComments(id) },
                                    onBookmarkClick = { id -> viewModel.onToggleBookmark(id) }
                                )

                                OmniTab.CHATTER -> ChatterScreen(
                                    posts = uiState.posts,
                                    onLikeClick = { id -> viewModel.onToggleLike(id) },
                                    onCommentClick = { id -> viewModel.openComments(id) },
                                    onRepostClick = { id -> viewModel.onRepost(id) },
                                    onBookmarkClick = { id -> viewModel.onToggleBookmark(id) }
                                )

                                OmniTab.FORUM -> ForumScreen(
                                    posts = uiState.posts,
                                    communities = uiState.communities,
                                    currentSort = uiState.forumSort,
                                    onSortChange = { sort -> viewModel.setForumSort(sort) },
                                    onVote = { id, dir -> viewModel.onCastVote(id, dir) },
                                    onCommentClick = { id -> viewModel.openComments(id) },
                                    onBookmarkClick = { id -> viewModel.onToggleBookmark(id) }
                                )

                                OmniTab.PROFILE -> ProfileScreen(
                                    onOpenSettings = { viewModel.openSettingsScreen() }
                                )
                            }
                        }
                    }
                }

                // Comments Bottom Sheet
                uiState.activeCommentPost?.let { commentPost ->
                    CommentsBottomSheet(
                        post = commentPost,
                        onDismiss = { viewModel.closeComments() },
                        onSubmitComment = { text, parentId ->
                            viewModel.submitComment(commentPost.id, parentId, text)
                        }
                    )
                }

                // Create Post Modal Sheet
                if (uiState.showCreateModal) {
                    CreatePostBottomSheet(
                        communities = uiState.communities,
                        onDismiss = { viewModel.closeCreateModal() },
                        onSubmit = { format, title, body, mediaUrls, community, tags ->
                            viewModel.createNewPost(format, title, body, mediaUrls, community, tags)
                        },
                        onOpenStudioMediaUpload = {
                            viewModel.closeCreateModal()
                            viewModel.openMediaUploadScreen()
                        }
                    )
                }

                // Studio Media Upload Screen (Full native picker, preview & chunk progress)
                if (uiState.showMediaUploadScreen) {
                    MediaUploadScreen(
                        onDismiss = { viewModel.closeMediaUploadScreen() },
                        isUploading = uiState.isUploadingMedia,
                        uploadProgressPercent = uiState.uploadProgressPercent,
                        initialCategory = if (uiState.isStoryUploadMode) UploadCategory.STORY else UploadCategory.TUBE,
                        onPostCreated = { format, title, body, mediaUri, isVideo, isStory, tags ->
                            viewModel.uploadMediaAndPublish(
                                mediaUri = mediaUri,
                                isVideo = isVideo,
                                format = format,
                                title = title,
                                caption = body,
                                tags = tags,
                                isStory = isStory
                            )
                        }
                    )
                }

                // Fullscreen Story Viewer
                uiState.activeStory?.let { story ->
                    StoryViewerDialog(
                        story = story,
                        onDismiss = { viewModel.closeStory() }
                    )
                }

                // Dolby Cinema Sound Lab & Welcome Greeting Sheet
                if (uiState.showSoundLab) {
                    OmniSoundLabSheet(
                        onDismiss = { viewModel.closeSoundLab() }
                    )
                }

                // Firebase Authentication & Handle Onboarding Screen
                if (uiState.showAuthScreen) {
                    AuthScreen(
                        authRepository = viewModel.authRepository,
                        onAuthSuccess = { profile ->
                            viewModel.onAuthSuccess(profile)
                        },
                        onDismiss = { viewModel.closeAuthScreen() }
                    )
                }

                // Comprehensive App Settings & Account Control Screen
                if (uiState.showSettingsScreen) {
                    SettingsScreen(
                        userProfile = uiState.userProfile,
                        isLoggedIn = uiState.isLoggedIn,
                        sessionManager = viewModel.sessionManager,
                        authRepository = viewModel.authRepository,
                        onBack = { viewModel.closeSettingsScreen() },
                        onOpenAuth = {
                            viewModel.closeSettingsScreen()
                            viewModel.openAuthScreen()
                        },
                        onProfileUpdated = { updatedProfile ->
                            viewModel.onProfileUpdated(updatedProfile)
                        },
                        onSignOut = {
                            viewModel.signOut()
                            viewModel.closeSettingsScreen()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OmniGreetingEngine.destroy()
    }
}
