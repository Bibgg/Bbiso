package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
    @ServerTimestamp val created_at: Timestamp? = null,
    @ServerTimestamp val updated_at: Timestamp? = null
) {
    fun getEffectiveDisplayName(): String = display_name.ifBlank { full_name.ifBlank { username.ifBlank { "Omni Creator" } } }
    fun getEffectivePhoneOrEmail(): String = phone_or_email.ifBlank { phone_number.ifBlank { email } }

    fun toAppUser(): User {
        return User(
            id = uid,
            username = if (username.startsWith("@")) username else "@$username",
            displayName = getEffectiveDisplayName(),
            avatarUrl = avatar_url.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80" },
            isVerified = true
        )
    }
}

@IgnoreExtraProperties
data class UsernameReservation(
    val uid: String = "",
    val username: String = "",
    @ServerTimestamp val created_at: Timestamp? = null
)

@IgnoreExtraProperties
data class UserLikeDocument(
    @DocumentId val postId: String = "",
    @ServerTimestamp val likedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBookmarkDocument(
    @DocumentId val postId: String = "",
    @ServerTimestamp val bookmarkedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OmniPostDocument(
    @DocumentId val postId: String = "",
    val uid: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val authorHandle: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "photo", // "video", "photo", "none"
    val caption: String = "",
    val title: String = "",
    val format: String = "SNAP", // "TUBE", "SNAP", "CHATTER", "FORUM"
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFeedPost(currentUserId: String? = null, likedPostIds: Set<String> = emptySet()): FeedPost {
        val parsedFormat = when (format.uppercase()) {
            "TUBE" -> PostFormat.TUBE
            "CHATTER" -> PostFormat.CHATTER
            "FORUM" -> PostFormat.FORUM
            else -> PostFormat.SNAP
        }

        return FeedPost(
            id = postId,
            author = User(
                id = uid,
                username = if (authorHandle.startsWith("@")) authorHandle else "@$authorHandle",
                displayName = authorName.ifBlank { authorHandle.ifBlank { "Omni User" } },
                avatarUrl = authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80" },
                isVerified = true
            ),
            format = parsedFormat,
            title = title.ifBlank { null },
            body = caption,
            mediaUrls = if (mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList(),
            likeCount = likesCount.toInt(),
            commentCount = commentsCount.toInt(),
            repostCount = sharesCount.toInt(),
            isLiked = likedPostIds.contains(postId),
            createdAt = "Just now"
        )
    }
}

@IgnoreExtraProperties
data class OmniChatDocument(
    @DocumentId val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OmniChatMessageDocument(
    @DocumentId val messageId: String = "",
    val chatId: String = "",
    val senderUid: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
