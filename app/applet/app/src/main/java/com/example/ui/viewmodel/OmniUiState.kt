package com.example.ui.viewmodel

import com.example.data.model.Comment
import com.example.data.model.Community
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.data.model.StoryItem
import com.example.data.model.User
import com.example.data.model.UserProfile

data class OmniUiState(
    val posts: List<FeedPost> = emptyList(),
    val stories: List<StoryItem> = emptyList(),
    val activeTab: OmniTab = OmniTab.HOME,
    val selectedFilter: PostFormat? = null,
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
    val forumSort: String = "hot",
    val likedPostIds: Set<String> = emptySet(),
    val bookmarkedPostIds: Set<String> = emptySet()
)
