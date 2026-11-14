package com.example.data.firebase

import android.util.Log
import com.example.data.model.FeedPost
import com.example.data.model.OmniPostDocument
import com.example.data.model.UserBookmarkDocument
import com.example.data.model.UserLikeDocument
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OmniFirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun observePosts(): Flow<List<FeedPost>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OmniFirestoreRepository", "Error observing posts: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        val postDoc = doc.toObject(OmniPostDocument::class.java)
                        postDoc?.toFeedPost(currentUserId = auth.currentUser?.uid)
                    }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeUserPosts(uid: String): Flow<List<FeedPost>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("posts")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OmniFirestoreRepository", "Error observing user posts: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        val postDoc = doc.toObject(OmniPostDocument::class.java)
                        postDoc?.toFeedPost(currentUserId = uid)
                    }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OmniFirestoreRepository", "Error observing user profile: ${error.message}")
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserLikes(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("likes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val likedIds = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
                trySend(likedIds)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserBookmarks(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val bookmarkIds = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
                trySend(bookmarkIds)
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleLike(postId: String, isLikedNow: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        val postRef = db.collection("posts").document(postId)
        return try {
            val delta = if (isLikedNow) 1L else -1L
            postRef.update("likesCount", FieldValue.increment(delta)).await()

            if (uid != null) {
                val userLikeRef = db.collection("users").document(uid).collection("likes").document(postId)
                if (isLikedNow) {
                    userLikeRef.set(UserLikeDocument(postId = postId, userId = uid, createdAt = Timestamp.now())).await()
                } else {
                    userLikeRef.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OmniFirestoreRepository", "Error updating like: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(postId: String, isBookmarkedNow: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        val bookmarkRef = db.collection("users").document(uid).collection("bookmarks").document(postId)
        return try {
            if (isBookmarkedNow) {
                bookmarkRef.set(UserBookmarkDocument(postId = postId, userId = uid, createdAt = Timestamp.now())).await()
            } else {
                bookmarkRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(post: OmniPostDocument): Result<String> {
        return try {
            val docRef = if (post.postId.isNotBlank()) {
                db.collection("posts").document(post.postId)
            } else {
                db.collection("posts").document()
            }
            val finalPost = post.copy(postId = docRef.id, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
            docRef.set(finalPost).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("OmniFirestoreRepository", "Error creating post: ${e.message}")
            Result.failure(e)
        }
    }
}
