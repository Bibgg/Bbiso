package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.OmniAuthRepository
import com.example.data.firebase.OmniFirestoreRepository
import com.example.data.firebase.OmniStorageRepository
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProfileMediaTab {
    MY_VIDEOS,
    PHOTOS,
    SAVED
}

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(
        uid = "default_user",
        display_name = "bishal gc",
        full_name = "bishal gc",
        username = "bishal",
        bio = "Digital Creator & Filmmaker 🎬 | Exploring AI & OmniBox",
        avatar_url = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
    ),
    val selectedTab: ProfileMediaTab = ProfileMediaTab.MY_VIDEOS,
    val userVideos: List<FeedPost> = emptyList(),
    val userPhotos: List<FeedPost> = emptyList(),
    val savedPosts: List<FeedPost> = emptyList(),
    val postsCount: Int = 12,
    val followersCount: String = "1.2k",
    val followingCount: String = "388",
    val isUploadingAvatar: Boolean = false,
    val avatarUploadSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val activePlayingVideo: FeedPost? = null,
    val isEditingProfile: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepo = OmniFirestoreRepository(application)
    private val storageRepo = OmniStorageRepository(application)
    private val authRepo = OmniAuthRepository(application)

    private val _selectedTab = MutableStateFlow(ProfileMediaTab.MY_VIDEOS)
    private val _isUploadingAvatar = MutableStateFlow(false)
    private val _avatarUploadSuccessMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _activePlayingVideo = MutableStateFlow<FeedPost?>(null)
    private val _isEditingProfile = MutableStateFlow(false)

    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    // Real-time user profile
    private val _profileFlow = firestoreRepo.observeUserProfile(currentUid)

    // Real-time user posts from Firestore
    private val _userPostsFlow = firestoreRepo.observeUserPosts(currentUid)

    // All posts
    private val _allPostsFlow = firestoreRepo.observePosts()

    data class SecondaryProfileState(
        val isUploading: Boolean,
        val activeVideo: FeedPost?,
        val isEditing: Boolean
    )

    private val _secondaryStateFlow = combine(
        _isUploadingAvatar,
        _activePlayingVideo,
        _isEditingProfile
    ) { isUploading, activeVideo, isEditing ->
        SecondaryProfileState(isUploading, activeVideo, isEditing)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        _selectedTab,
        _profileFlow,
        _userPostsFlow,
        _allPostsFlow,
        _secondaryStateFlow
    ) { selectedTab, profile, userPostDocs, allPostDocs, secondaryState ->
        val isUploading = secondaryState.isUploading
        val activeVideo = secondaryState.activeVideo
        val isEditing = secondaryState.isEditing
        val defaultProfile = UserProfile(
            uid = currentUid.ifBlank { "bishal_uid" },
            display_name = "bishal gc",
            full_name = "bishal gc",
            username = "bishal",
            bio = "Digital Creator & Filmmaker 🎬 | Exploring AI & OmniBox",
            avatar_url = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
        )

        val activeProfile = profile ?: defaultProfile
        val mappedUserPosts = userPostDocs.map { it.toFeedPost(currentUserId = currentUid) }
        val mappedAllPosts = allPostDocs.map { it.toFeedPost(currentUserId = currentUid) }

        // Filter user uploaded videos
        val videos = mappedUserPosts.filter { post ->
            post.format == PostFormat.TUBE || post.videoDuration != null || post.mediaUrls.any { url ->
                url.endsWith(".mp4", ignoreCase = true) || url.contains("video", ignoreCase = true)
            }
        }.ifEmpty {
            listOf(
                FeedPost(
                    id = "bishal_vid_1",
                    author = activeProfile.toAppUser(),
                    format = PostFormat.TUBE,
                    title = "Neon Cyberpunk Nights in Tokyo",
                    body = "Shooting with anamorphic lenses and dynamic neon lighting ✨",
                    mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                    videoDuration = "0:30",
                    likeCount = 342,
                    commentCount = 48,
                    viewCount = "14.8k"
                ),
                FeedPost(
                    id = "bishal_vid_2",
                    author = activeProfile.toAppUser(),
                    format = PostFormat.TUBE,
                    title = "Cinematic Drone Reel 2026",
                    body = "Mountain ranges and ocean vistas in 4K HDR",
                    mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
                    videoDuration = "0:45",
                    likeCount = 512,
                    commentCount = 64,
                    viewCount = "22.3k"
                ),
                FeedPost(
                    id = "bishal_vid_3",
                    author = activeProfile.toAppUser(),
                    format = PostFormat.TUBE,
                    title = "SoundLab 3D Audio Demo",
                    body = "Testing spatial binaural audio synthesizer in OmniBox",
                    mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
                    videoDuration = "0:25",
                    likeCount = 189,
                    commentCount = 23,
                    viewCount = "8.1k"
                )
            )
        }

        // Filter user photos
        val photos = mappedUserPosts.filter { it.format == PostFormat.SNAP }.ifEmpty {
            listOf(
                FeedPost(
                    id = "bishal_photo_1",
                    author = activeProfile.toAppUser(),
                    format = PostFormat.SNAP,
                    title = "Golden Hour Portrait",
                    body = "Natural light shot on 85mm f/1.4",
                    mediaUrls = listOf("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=600&q=80"),
                    likeCount = 280,
                    commentCount = 31,
                    viewCount = "5.4k"
                ),
                FeedPost(
                    id = "bishal_photo_2",
                    author = activeProfile.toAppUser(),
                    format = PostFormat.SNAP,
                    title = "Cyber Setup 2026",
                    body = "Triple OLED monitor minimal workspace",
                    mediaUrls = listOf("https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=600&q=80"),
                    likeCount = 412,
                    commentCount = 57,
                    viewCount = "11.2k"
                )
            )
        }

        // Saved bookmarks
        val saved = mappedAllPosts.take(4)

        ProfileUiState(
            userProfile = activeProfile.copy(
                display_name = activeProfile.display_name.ifBlank { "bishal gc" },
                full_name = activeProfile.full_name.ifBlank { "bishal gc" },
                username = activeProfile.username.ifBlank { "bishal" },
                bio = activeProfile.bio.ifBlank { "Digital Creator & Filmmaker 🎬 | Exploring AI & OmniBox" }
            ),
            selectedTab = selectedTab,
            userVideos = videos,
            userPhotos = photos,
            savedPosts = saved,
            postsCount = (videos.size + photos.size).coerceAtLeast(1),
            followersCount = "1.2k",
            followingCount = "388",
            isUploadingAvatar = isUploading,
            activePlayingVideo = activeVideo,
            isEditingProfile = isEditing
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfileUiState()
    )

    fun onTabSelected(tab: ProfileMediaTab) {
        _selectedTab.value = tab
    }

    fun openVideoPlayer(post: FeedPost) {
        _activePlayingVideo.value = post
    }

    fun closeVideoPlayer() {
        _activePlayingVideo.value = null
    }

    fun toggleEditProfile(show: Boolean) {
        _isEditingProfile.value = show
    }

    /**
     * Uploads new profile photo directly to Firebase Cloud Storage at `users/{uid}/profile.jpg`
     * and immediately updates Firestore document `users/{uid}`.
     */
    fun uploadProfilePhoto(uri: Uri) {
        val uid = currentUid.ifBlank { "bishal_uid" }
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _errorMessage.value = null
            _avatarUploadSuccessMessage.value = null

            try {
                // 1. Upload to Firebase Storage: users/{uid}/profile.jpg
                val uploadResult = storageRepo.uploadProfilePhoto(uri, uid)
                val downloadUrl = uploadResult.getOrNull()

                if (downloadUrl != null) {
                    // 2. Update Firestore immediately
                    authRepo.updateUserProfile(avatarUrl = downloadUrl)
                    _avatarUploadSuccessMessage.value = "Profile photo updated successfully!"
                } else {
                    _errorMessage.value = "Upload failed: ${uploadResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile photo: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to upload photo"
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun updateProfileDetails(displayName: String, bio: String) {
        viewModelScope.launch {
            authRepo.updateUserProfile(fullName = displayName.trim(), bio = bio.trim())
            _isEditingProfile.value = false
        }
    }
}
