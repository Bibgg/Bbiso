package com.example.data.model

enum class PostFormat {
    TUBE,    // YouTube / TikTok video
    SNAP,    // Instagram carousel / photos
    CHATTER, // X / Twitter microblogging
    FORUM    // Reddit community discussion
}

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isVerified: Boolean = false,
    val karmaScore: Int = 1240
)

data class Community(
    val id: String,
    val slug: String,       // e.g. "c/androiddev"
    val name: String,
    val iconUrl: String,
    val memberCount: String = "240k"
)

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val timestamp: String,
    val upvotes: Int = 0,
    val userVote: Int = 0,
    val depth: Int = 0,
    val replies: List<Comment> = emptyList()
)

data class FeedPost(
    val id: String,
    val author: User,
    val format: PostFormat,
    val community: Community? = null,
    val title: String? = null,
    val body: String,
    val mediaUrls: List<String> = emptyList(),
    val videoDuration: String? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val upvoteCount: Int = 0,
    val downvoteCount: Int = 0,
    val commentCount: Int = 0,
    val repostCount: Int = 0,
    val viewCount: String = "12.4K",
    val isLiked: Boolean = false,
    val userVote: Int = 0, // +1, -1, 0
    val isBookmarked: Boolean = false,
    val isMuted: Boolean = true,
    val isPlaying: Boolean = false,
    val createdAt: String = "2h ago",
    val comments: List<Comment> = emptyList()
)

data class StoryItem(
    val id: String,
    val user: User,
    val previewImage: String,
    val caption: String = "",
    val hasUnseen: Boolean = true,
    val timestamp: String = "3h ago"
)
