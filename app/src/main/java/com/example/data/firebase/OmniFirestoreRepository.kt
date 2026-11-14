package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.OmniChatDocument
import com.example.data.model.OmniChatMessageDocument
import com.example.data.model.OmniPostDocument
import com.example.data.model.OmniStoryDocument
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class UserRestoreData(
    val profile: UserProfile?,
    val posts: List<OmniPostDocument>,
    val likedPostIds: Set<String>,
    val bookmarkedPostIds: Set<String>
)

class OmniFirestoreRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing Firestore operation")
    }

    /**
     * Real-time observation of user profile
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Fetch user profile once
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    /**
     * Real-time observation of feed posts across formats (Tube, Snap, Chatter, Forum)
     */
    fun observePosts(formatFilter: String? = null): Flow<List<OmniPostDocument>> {
        val collection = db.collection("posts")
        val query = if (!formatFilter.isNullOrBlank()) {
            collection.whereEqualTo("format", formatFilter)
        } else {
            collection
        }

        return query
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(OmniPostDocument::class.java) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "posts")
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Observe or query all posts created by a specific user UID (for Instagram/TikTok style anti-data loss sync)
     */
    fun observeUserPosts(userId: String): Flow<List<OmniPostDocument>> {
        return db.collection("posts")
            .whereEqualTo("uid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(OmniPostDocument::class.java) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "posts?uid=$userId")
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /** Observe only stories whose 24-hour visibility window has not expired. */
    fun observeStories(): Flow<List<OmniStoryDocument>> {
        return db.collection("stories")
            .whereGreaterThan("expiresAt", com.google.firebase.Timestamp.now())
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(OmniStoryDocument::class.java) }
                    .filter { it.expiresAt?.toDate()?.time?.let { expiry -> expiry > System.currentTimeMillis() } == true }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "stories")
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /** Publish a story document after Cloudinary confirms the media upload. */
    suspend fun createStory(story: OmniStoryDocument): Result<String> {
        val uid = requireUserId()
        val storiesRef = db.collection("stories")
        val newDocRef = if (story.storyId.isNotBlank()) storiesRef.document(story.storyId) else storiesRef.document()
        return try {
            newDocRef.set(story.copy(storyId = newDocRef.id, uid = uid)).await()
            Result.success(newDocRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, newDocRef.path)
            Result.failure(e)
        }
    }

    /**
     * Comprehensive Anti-Data Loss Restore:
     * When logging back in after app data wipe or device switch, queries:
     * 1. Profile metadata
     * 2. All user-created posts/videos
     * 3. Backed up user likes
     * 4. Backed up user bookmarks
     */
    suspend fun restoreUserData(userId: String): UserRestoreData {
        val profile = getUserProfile(userId)

        val userPosts = try {
            val snapshot = db.collection("posts").whereEqualTo("uid", userId).get().await()
            snapshot.documents.mapNotNull { it.toObject(OmniPostDocument::class.java) }
        } catch (e: Exception) {
            emptyList()
        }

        val likes = try {
            val snapshot = db.collection("users").document(userId).collection("likes").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val bookmarks = try {
            val snapshot = db.collection("users").document(userId).collection("bookmarks").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        return UserRestoreData(
            profile = profile,
            posts = userPosts,
            likedPostIds = likes,
            bookmarkedPostIds = bookmarks
        )
    }

    /**
     * Create a new post in Cloud Firestore
     */
    suspend fun createPost(post: OmniPostDocument): Result<String> {
        val uid = requireUserId()
        val postsRef = db.collection("posts")
        val newDocRef = if (post.postId.isNotBlank()) postsRef.document(post.postId) else postsRef.document()

        val postToSave = post.copy(
            postId = newDocRef.id,
            uid = uid
        )

        return try {
            newDocRef.set(postToSave).await()
            Result.success(newDocRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, newDocRef.path)
            Result.failure(e)
        }
    }

    /**
     * Increment or decrement likes count on post, and back up like permanently under users/{uid}/likes/{postId}
     */
    suspend fun toggleLike(postId: String, isLikedNow: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docRef = db.collection("posts").document(postId)
        return try {
            val delta = if (isLikedNow) 1L else -1L
            docRef.update("likesCount", FieldValue.increment(delta)).await()

            // Anti-data loss backup under user's permanent UID
            if (uid != null) {
                val userLikeRef = db.collection("users").document(uid).collection("likes").document(postId)
                if (isLikedNow) {
                    userLikeRef.set(mapOf("postId" to postId, "likedAt" to FieldValue.serverTimestamp())).await()
                } else {
                    userLikeRef.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Permanent anti-data-loss bookmark backup under users/{uid}/bookmarks/{postId}
     */
    suspend fun toggleBookmark(postId: String, isBookmarkedNow: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val bookmarkRef = db.collection("users").document(uid).collection("bookmarks").document(postId)
        return try {
            if (isBookmarkedNow) {
                bookmarkRef.set(mapOf("postId" to postId, "bookmarkedAt" to FieldValue.serverTimestamp())).await()
            } else {
                bookmarkRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, bookmarkRef.path)
            Result.failure(e)
        }
    }

    /** Record one view per signed-in viewer for an active story. */
    suspend fun markStoryViewed(storyId: String): Result<Unit> {
        val uid = requireUserId()
        val viewRef = db.collection("stories").document(storyId).collection("views").document(uid)
        return try {
            viewRef.set(mapOf("viewerUid" to uid, "viewedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, viewRef.path)
            Result.failure(e)
        }
    }

    /**
     * Real-time observation of user's backed-up liked post IDs
     */
    fun observeUserLikes(userId: String): Flow<Set<String>> {
        return db.collection("users").document(userId).collection("likes")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.id }.toSet()
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "users/$userId/likes")
                emit(emptySet())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Real-time observation of user's backed-up bookmarked post IDs
     */
    fun observeUserBookmarks(userId: String): Flow<Set<String>> {
        return db.collection("users").document(userId).collection("bookmarks")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.id }.toSet()
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "users/$userId/bookmarks")
                emit(emptySet())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Real-time observation of direct message chats for current user
     */
    fun observeUserChats(): Flow<List<OmniChatDocument>> {
        val uid = requireUserId()
        return db.collection("chats")
            .whereArrayContains("participants", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(OmniChatDocument::class.java) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "chats")
                throw e
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Real-time observation of messages in a chat thread
     */
    fun observeMessages(chatId: String): Flow<List<OmniChatMessageDocument>> {
        return db.collection("chats").document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(OmniChatMessageDocument::class.java) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "chats/$chatId/messages")
                throw e
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Send message in chat thread
     */
    suspend fun sendMessage(chatId: String, text: String, mediaUrl: String = ""): Result<Unit> {
        val uid = requireUserId()
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()

        val message = OmniChatMessageDocument(
            messageId = msgRef.id,
            chatId = chatId,
            senderUid = uid,
            text = text,
            mediaUrl = mediaUrl
        )

        return try {
            val batch = db.batch()
            batch.set(msgRef, message)
            batch.update(chatRef, mapOf(
                "lastMessage" to text.ifBlank { "Attachment" },
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path)
            Result.failure(e)
        }
    }
}
