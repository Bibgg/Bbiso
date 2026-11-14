package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PostFormat {
    TUBE,
    SNAP,
    CHATTER,
    FORUM
}

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val isVerified: Boolean = false,
    val karmaScore: Int = 1240
)

data class Community(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val memberCount: String = "240k"
)

data class Comment(
    val id: String = "",
    val author: User = User(),
    val text: String = "",
    val timestamp: String = "",
    val upvotes: Int = 0,
    val userVote: Int = 0,
    val depth: Int = 0,
    val replies: List<Comment> = emptyList()
)

data class StoryItem(
    val id: String = "",
    val user: User = User(),
    val previewImage: String = "",
    val caption: String = "",
    val hasUnseen: Boolean = true,
    val timestamp: String = "3h ago"
)

data class FeedPost(
    val id: String = "",
    val author: User = User(),
    val format: PostFormat = PostFormat.TUBE,
    val community: Community? = null,
    val title: String? = null,
    val body: String = "",
    val mediaUrls: List<String> = emptyList(),
    val videoDuration: String? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val upvoteCount: Int = 0,
    val downvoteCount: Int = 0,
    val commentCount: Int = 0,
    val repostCount: Int = 0,
    val viewCount: String = "12.4k",
    val isLiked: Boolean = false,
    val userVote: Int = 0,
    val isBookmarked: Boolean = false,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val createdAt: String = "Just now",
    val comments: List<Comment> = emptyList()
)

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val full_name: String = "",
    val display_name: String = "",
    val dob: String = "",
    val avatar_url: String = "",
    val bio: String = "",
    val phone_number: String = "",
    val phone_or_email: String = "",
    val private_account: Boolean = false,
    val posts_count: Int = 0,
    val followers_count: Int = 1240,
    val following_count: Int = 388,
    @ServerTimestamp
    val created_at: Timestamp? = null,
    @ServerTimestamp
    val updated_at: Timestamp? = null
) {
    fun getEffectiveDisplayName(): String {
        return display_name.ifBlank {
            full_name.ifBlank {
                username.ifBlank { "Omni Creator" }
            }
        }
    }

    fun getEffectivePhoneOrEmail(): String {
        return phone_or_email.ifBlank {
            phone_number.ifBlank { email }
        }
    }

    fun toAppUser(): User {
        val handle = if (username.startsWith("@")) username else "@$username"
        val avatar = avatar_url.ifBlank {
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
        }
        return User(
            id = uid,
            username = handle,
            displayName = getEffectiveDisplayName(),
            avatarUrl = avatar,
            isVerified = true,
            karmaScore = 1240
        )
    }
}

@IgnoreExtraProperties
data class OmniPostDocument(
    @DocumentId
    val postId: String = "",
    val uid: String = "",
    val author_id: String = "",
    val creator_uid: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val authorHandle: String = "",
    val mediaUrl: String = "",
    val video_url: String = "",
    val thumbnail_url: String = "",
    val mediaType: String = "video",
    val caption: String = "",
    val title: String = "",
    val format: String = "TUBE",
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val sharesCount: Long = 0,
    val views_count: Long = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun getEffectiveUid(): String {
        return uid.ifBlank { author_id.ifBlank { creator_uid } }
    }

    fun getEffectiveMediaUrl(): String {
        return mediaUrl.ifBlank { video_url }
    }

    fun toFeedPost(currentUserId: String? = null, likedPostIds: Set<String> = emptySet()): FeedPost {
        val effectiveMedia = getEffectiveMediaUrl()
        val mediaList = if (effectiveMedia.isNotBlank()) listOf(effectiveMedia) else emptyList()
        val parsedFormat = try {
            PostFormat.valueOf(format.uppercase())
        } catch (_: Exception) {
            PostFormat.TUBE
        }

        val effectiveAuthorId = getEffectiveUid()
        val authorUser = User(
            id = effectiveAuthorId,
            username = if (authorHandle.startsWith("@")) authorHandle else "@$authorHandle",
            displayName = authorName.ifBlank { "Omni Creator" },
            avatarUrl = authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80" },
            isVerified = true,
            karmaScore = 1240
        )

        val idToUse = postId.ifBlank { "post_${System.currentTimeMillis()}" }
        val isLiked = (currentUserId != null && likedPostIds.contains(idToUse))

        return FeedPost(
            id = idToUse,
            author = authorUser,
            format = parsedFormat,
            title = title.ifBlank { null },
            body = caption,
            mediaUrls = mediaList,
            videoDuration = if (parsedFormat == PostFormat.TUBE) "0:30" else null,
            likeCount = likesCount.toInt(),
            commentCount = commentsCount.toInt(),
            repostCount = sharesCount.toInt(),
            viewCount = if (views_count > 0) "${views_count}" else "1.2k",
            isLiked = isLiked,
            createdAt = "Recent"
        )
    }
}

@IgnoreExtraProperties
data class UserLikeDocument(
    val postId: String = "",
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBookmarkDocument(
    val postId: String = "",
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsernameReservation(
    val uid: String = "",
    val username: String = "",
    @ServerTimestamp
    val reserved_at: Timestamp? = null
)
