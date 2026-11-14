package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.OmniAuthRepository
import com.example.data.firebase.OmniFirestoreRepository
import com.example.data.firebase.OmniSessionManager
import com.example.data.firebase.OmniStorageRepository
import com.example.data.firebase.MediaPipelineResult
import com.example.data.model.Community
import com.example.data.model.FeedPost
import com.example.data.model.OmniPostDocument
import com.example.data.model.PostFormat
import com.example.data.model.StoryItem
import com.example.data.model.User
import com.example.data.model.UserProfile
import com.example.data.repository.OmniRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OmniTab {
    HOME,
    TUBE,
    SNAP,
    CHATTER,
    FORUM,
    PROFILE
}

data class OmniUiState(
    val posts: List<FeedPost> = emptyList(),
    val stories: List<StoryItem> = emptyList(),
    val activeTab: OmniTab = OmniTab.HOME,
    val selectedFilter: PostFormat? = null, // null means "All"
    val activeStory: StoryItem? = null,
    val activeCommentPost: FeedPost? = null,
    val showCreateModal: Boolean = false,
    val showMediaUploadScreen: Boolean = false,
    val showSoundLab: Boolean = false,
    val showAuthScreen: Boolean = false,
    val showSettingsScreen: Boolean = false,
    val currentUser: User? = null,
    val userProfile: UserProfile? = null,
    val isLoggedIn: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val uploadProgressPercent: Int? = null,
    val communities: List<Community> = emptyList(),
    val forumSort: String = "Hot" // "Hot", "Top", "New"
)

sealed interface OmniEffect {
    data class ShowToast(val message: String) : OmniEffect
}

class OmniViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: OmniRepository = OmniRepository()
) : AndroidViewModel(application) {

    val sessionManager = OmniSessionManager(application)
    val authRepository = OmniAuthRepository(application)
    val firestoreRepository = OmniFirestoreRepository(application)
    val storageRepository = OmniStorageRepository(application)

    private val _activeTab = MutableStateFlow(OmniTab.HOME)
    private val _selectedFilter = MutableStateFlow<PostFormat?>(null)
    private val _activeStory = MutableStateFlow<StoryItem?>(null)
    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    private val _showCreateModal = MutableStateFlow(false)
    private val _showMediaUploadScreen = MutableStateFlow(false)
    private val _showSoundLab = MutableStateFlow(false)
    private val _showAuthScreen = MutableStateFlow(false)
    private val _showSettingsScreen = MutableStateFlow(false)
    private val _forumSort = MutableStateFlow("Hot")

    private val _cloudPosts = MutableStateFlow<List<FeedPost>>(emptyList())
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    private val _userBookmarkedPostIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isUploadingMedia = MutableStateFlow(false)
    private val _uploadProgressPercent = MutableStateFlow<Int?>(null)

    private val _effects = MutableSharedFlow<OmniEffect>()
    val effects: SharedFlow<OmniEffect> = _effects.asSharedFlow()

    init {
        // Observe Firebase Auth changes and sync profile and likes/bookmarks from Firestore
        viewModelScope.launch {
            sessionManager.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    val profile = firestoreRepository.getUserProfile(firebaseUser.uid)
                    _userProfile.value = profile
                    sessionManager.updateCachedProfile(profile)
                } else {
                    _userProfile.value = null
                    _userLikedPostIds.value = emptySet()
                    _userBookmarkedPostIds.value = emptySet()
                    sessionManager.updateCachedProfile(null)
                }
            }
        }

        // Real-time synchronization of user likes & bookmarks for anti-data loss
        viewModelScope.launch {
            sessionManager.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    launch {
                        try {
                            firestoreRepository.observeUserLikes(firebaseUser.uid).collect { likedSet ->
                                _userLikedPostIds.value = likedSet
                                repository.syncUserInteractions(likedSet, _userBookmarkedPostIds.value)
                            }
                        } catch (e: Exception) {}
                    }
                    launch {
                        try {
                            firestoreRepository.observeUserBookmarks(firebaseUser.uid).collect { bookmarkedSet ->
                                _userBookmarkedPostIds.value = bookmarkedSet
                                repository.syncUserInteractions(_userLikedPostIds.value, bookmarkedSet)
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // Real-time listener for Firestore posts
        viewModelScope.launch {
            try {
                firestoreRepository.observePosts(null).collect { postDocs ->
                    val feedPosts = postDocs.map { it.toFeedPost(likedPostIds = _userLikedPostIds.value) }
                    _cloudPosts.value = feedPosts
                }
            } catch (e: Exception) {
                // If offline or emulator cold start, continue with repository posts
            }
        }
    }

    private data class ExtraState(
        val story: StoryItem?,
        val commentPostId: String?,
        val showCreate: Boolean,
        val showMediaUpload: Boolean,
        val showSoundLab: Boolean,
        val showAuth: Boolean,
        val showSettings: Boolean,
        val userProfile: UserProfile?,
        val isLoggedIn: Boolean,
        val isUploading: Boolean,
        val uploadPercent: Int?,
        val sort: String
    )

    private val _extraState = combine(
        combine(_activeStory, _activeCommentPostId, _showCreateModal, _showMediaUploadScreen) { story, commentId, showCreate, showMedia ->
            listOf(story, commentId, showCreate, showMedia)
        },
        combine(_showSoundLab, _showAuthScreen, _showSettingsScreen, _userProfile, sessionManager.isLoggedIn) { showLab, showAuth, showSettings, profile, loggedIn ->
            listOf(showLab, showAuth, showSettings, profile, loggedIn)
        },
        combine(_isUploadingMedia, _uploadProgressPercent, _forumSort) { isUploading, percent, sort ->
            listOf(isUploading, percent, sort)
        }
    ) { group1, group2, group3 ->
        ExtraState(
            story = group1[0] as? StoryItem,
            commentPostId = group1[1] as? String,
            showCreate = group1[2] as Boolean,
            showMediaUpload = group1[3] as Boolean,
            showSoundLab = group2[0] as Boolean,
            showAuth = group2[1] as Boolean,
            showSettings = group2[2] as Boolean,
            userProfile = group2[3] as? UserProfile,
            isLoggedIn = group2[4] as Boolean,
            isUploading = group3[0] as Boolean,
            uploadPercent = group3[1] as? Int,
            sort = group3[2] as String
        )
    }

    private val _feedData = combine(repository.postsFlow, _cloudPosts, repository.storiesFlow) { localPosts, cloudPosts, stories ->
        Triple(localPosts, cloudPosts, stories)
    }

    val uiState: StateFlow<OmniUiState> = combine(
        _feedData,
        _activeTab,
        _selectedFilter,
        _extraState
    ) { feedData, tab, filter, extra ->
        val (localPosts, cloudPosts, stories) = feedData
        // Merge cloud posts with local repository posts, prioritizing cloud posts
        val allPosts = (cloudPosts + localPosts).distinctBy { it.id }
        val activeCommentPost = allPosts.find { it.id == extra.commentPostId }
        val effectiveUser = extra.userProfile?.toAppUser() ?: repository.getCurrentUser()

        OmniUiState(
            posts = allPosts,
            stories = stories,
            activeTab = tab,
            selectedFilter = filter,
            activeStory = extra.story,
            activeCommentPost = activeCommentPost,
            showCreateModal = extra.showCreate,
            showMediaUploadScreen = extra.showMediaUpload,
            showSoundLab = extra.showSoundLab,
            showAuthScreen = extra.showAuth,
            showSettingsScreen = extra.showSettings,
            currentUser = effectiveUser,
            userProfile = extra.userProfile,
            isLoggedIn = extra.isLoggedIn,
            isUploadingMedia = extra.isUploading,
            uploadProgressPercent = extra.uploadPercent,
            communities = repository.getCommunities(),
            forumSort = extra.sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OmniUiState(
            currentUser = repository.getCurrentUser(),
            communities = repository.getCommunities()
        )
    )

    fun onTabSelected(tab: OmniTab) {
        _activeTab.value = tab
    }

    fun onFilterSelected(filter: PostFormat?) {
        _selectedFilter.value = filter
    }

    fun onToggleLike(postId: String) {
        val currentlyLiked = _userLikedPostIds.value.contains(postId)
        val isLikedNow = !currentlyLiked
        val updatedSet = if (isLikedNow) _userLikedPostIds.value + postId else _userLikedPostIds.value - postId
        _userLikedPostIds.value = updatedSet

        repository.toggleLike(postId)
        viewModelScope.launch {
            try {
                firestoreRepository.toggleLike(postId, isLikedNow)
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    fun onCastVote(postId: String, direction: Int) {
        repository.castVote(postId, direction)
    }

    fun onToggleBookmark(postId: String) {
        val currentlyBookmarked = _userBookmarkedPostIds.value.contains(postId)
        val isBookmarkedNow = !currentlyBookmarked
        val updatedSet = if (isBookmarkedNow) _userBookmarkedPostIds.value + postId else _userBookmarkedPostIds.value - postId
        _userBookmarkedPostIds.value = updatedSet

        repository.toggleBookmark(postId)
        viewModelScope.launch {
            try {
                firestoreRepository.toggleBookmark(postId, isBookmarkedNow)
            } catch (e: Exception) {
                // Handled gracefully
            }
            _effects.emit(OmniEffect.ShowToast(if (isBookmarkedNow) "Saved to OmniBox bookmarks!" else "Removed from bookmarks"))
        }
    }

    fun onToggleVideoPlay(postId: String) {
        repository.toggleVideoPlay(postId)
    }

    fun onToggleVideoMute(postId: String) {
        repository.toggleVideoMute(postId)
    }

    fun onRepost(postId: String) {
        repository.repost(postId)
        viewModelScope.launch {
            _effects.emit(OmniEffect.ShowToast("Re-chattered to your followers!"))
        }
    }

    fun openStory(story: StoryItem) {
        _activeStory.value = story
    }

    fun closeStory() {
        _activeStory.value = null
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

    fun openCreateModal() {
        _showCreateModal.value = true
    }

    fun closeCreateModal() {
        _showCreateModal.value = false
    }

    fun openMediaUploadScreen() {
        _showMediaUploadScreen.value = true
    }

    fun closeMediaUploadScreen() {
        _showMediaUploadScreen.value = false
    }

    fun openSoundLab() {
        _showSoundLab.value = true
    }

    fun closeSoundLab() {
        _showSoundLab.value = false
    }

    fun openAuthScreen() {
        _showAuthScreen.value = true
    }

    fun closeAuthScreen() {
        _showAuthScreen.value = false
    }

    fun openSettingsScreen() {
        _showSettingsScreen.value = true
    }

    fun closeSettingsScreen() {
        _showSettingsScreen.value = false
    }

    fun onProfileUpdated(profile: UserProfile) {
        _userProfile.value = profile
        sessionManager.updateCachedProfile(profile)
        viewModelScope.launch {
            _effects.emit(OmniEffect.ShowToast("Profile updated and synced to Cloud Firestore!"))
        }
    }

    fun onAuthSuccess(profile: UserProfile) {
        _userProfile.value = profile
        sessionManager.updateCachedProfile(profile)
        _showAuthScreen.value = false
        viewModelScope.launch {
            _effects.emit(OmniEffect.ShowToast("Welcome back, @${profile.username}! Cloud session active."))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _userProfile.value = null
            sessionManager.updateCachedProfile(null)
            _effects.emit(OmniEffect.ShowToast("Signed out of OmniBox."))
        }
    }

    fun setForumSort(sort: String) {
        _forumSort.value = sort
    }

    fun submitComment(postId: String, parentId: String?, text: String) {
        if (text.isBlank()) return
        repository.addComment(postId, parentId, text.trim())
    }

    fun createNewPost(
        format: PostFormat,
        title: String?,
        body: String,
        mediaUrls: List<String>,
        community: Community?,
        tags: List<String>
    ) {
        // Save to local repository for immediate UI responsiveness
        repository.createPost(format, title, body, mediaUrls, community, tags)
        _showCreateModal.value = false

        // Persist to Cloud Firestore if user is authenticated
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            viewModelScope.launch {
                val profile = _userProfile.value
                val postDoc = OmniPostDocument(
                    uid = currentUser.uid,
                    authorName = profile?.full_name ?: currentUser.displayName ?: "Omni Creator",
                    authorAvatarUrl = profile?.avatar_url ?: currentUser.photoUrl?.toString() ?: "",
                    authorHandle = profile?.username ?: "creator",
                    mediaUrl = mediaUrls.firstOrNull() ?: "",
                    mediaType = if (format == PostFormat.TUBE) "video" else if (mediaUrls.isNotEmpty()) "photo" else "none",
                    caption = body,
                    title = title ?: "",
                    format = format.name,
                    tags = tags
                )
                firestoreRepository.createPost(postDoc)
                _effects.emit(OmniEffect.ShowToast("Published & synced permanently to Cloud Firestore!"))
            }
        } else {
            viewModelScope.launch {
                _effects.emit(OmniEffect.ShowToast("Published to local feed! Sign in for cloud sync."))
            }
        }
    }

    /**
     * Complete background media pipeline: uploads media to Cloud Storage with progress tracking
     * and publishes post to Cloud Firestore.
     */
    fun uploadMediaAndPublish(
        mediaUri: Uri?,
        isVideo: Boolean,
        format: PostFormat,
        title: String?,
        caption: String,
        tags: List<String>
    ) {
        _isUploadingMedia.value = true
        _uploadProgressPercent.value = 0

        val currentUser = authRepository.getCurrentUser()
        val profile = _userProfile.value
        val authorHandle = profile?.username ?: currentUser?.email?.substringBefore("@") ?: "creator"
        val authorName = profile?.full_name ?: currentUser?.displayName ?: "Omni Creator"
        val authorAvatar = profile?.avatar_url ?: currentUser?.photoUrl?.toString() ?: ""

        viewModelScope.launch {
            if (currentUser != null && mediaUri != null) {
                storageRepository.uploadAndPublishPost(
                    mediaUri = mediaUri,
                    isVideo = isVideo,
                    format = format,
                    title = title,
                    caption = caption,
                    authorHandle = authorHandle,
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    tags = tags
                ).collect { result ->
                    when (result) {
                        is MediaPipelineResult.Uploading -> {
                            _uploadProgressPercent.value = result.progressPercent
                        }
                        is MediaPipelineResult.SavingToFirestore -> {
                            _uploadProgressPercent.value = 100
                        }
                        is MediaPipelineResult.Success -> {
                            _isUploadingMedia.value = false
                            _uploadProgressPercent.value = null
                            _showMediaUploadScreen.value = false
                            _effects.emit(OmniEffect.ShowToast("Media uploaded to Cloudinary and post saved to Firestore!"))
                        }
                        is MediaPipelineResult.Failed -> {
                            _isUploadingMedia.value = false
                            _uploadProgressPercent.value = null
                            _effects.emit(OmniEffect.ShowToast("Upload failed: ${result.error}"))
                        }
                        else -> {}
                    }
                }
            } else {
                _isUploadingMedia.value = false
                _uploadProgressPercent.value = null
                _effects.emit(OmniEffect.ShowToast("Please sign in before publishing photos or videos."))
                _showAuthScreen.value = true
            }
        }
    }
}
