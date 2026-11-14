package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.OmniPostDocument
import com.example.data.model.PostFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface UploadProgressState {
    object Idle : UploadProgressState
    data class Uploading(val progressPercent: Int, val bytesTransferred: Long, val totalBytes: Long) : UploadProgressState
    data class Completed(val downloadUrl: String, val mediaType: String, val storagePath: String) : UploadProgressState
    data class Error(val message: String) : UploadProgressState
}

sealed interface MediaPipelineResult {
    object Idle : MediaPipelineResult
    data class Uploading(val progressPercent: Int) : MediaPipelineResult
    object SavingToFirestore : MediaPipelineResult
    data class Success(val postId: String, val permanentMediaUrl: String) : MediaPipelineResult
    data class Failed(val error: String) : MediaPipelineResult
}

class OmniStorageRepository(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestoreRepository: OmniFirestoreRepository = OmniFirestoreRepository(context),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before uploading media to Cloud Storage")
    }

    /**
     * Uploads media file to Firebase Cloud Storage under user-specific folders:
     * - Videos: users/{uid}/videos/{timestamp}_{uuid}
     * - Photos: users/{uid}/photos/{timestamp}_{uuid}
     *
     * Streams continuous progress percentage (0 - 100%) so UI stays responsive.
     */
    fun uploadMediaWithProgress(
        fileUri: Uri,
        isVideo: Boolean,
        customFilename: String? = null
    ): Flow<UploadProgressState> = callbackFlow {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            trySend(UploadProgressState.Error(e.localizedMessage ?: "Unauthenticated"))
            close(e)
            return@callbackFlow
        }

        val folder = if (isVideo) "users/$uid/videos" else "users/$uid/photos"
        val extension = if (isVideo) "mp4" else "jpg"
        val uniqueName = customFilename ?: "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        val storagePath = "$folder/$uniqueName"
        val storageRef: StorageReference = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(if (isVideo) "video/mp4" else "image/jpeg")
            .setCustomMetadata("uploadedBy", uid)
            .setCustomMetadata("mediaCategory", if (isVideo) "reel" else "snap")
            .build()

        val uploadTask = storageRef.putFile(fileUri, metadata)

        uploadTask.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val percent = if (total > 0) ((transferred * 100) / total).toInt() else 0
            trySend(UploadProgressState.Uploading(percent, transferred, total))
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri.toString()
                trySend(
                    UploadProgressState.Completed(
                        downloadUrl = downloadUrl,
                        mediaType = if (isVideo) "video" else "photo",
                        storagePath = storagePath
                    )
                )
                close()
            }.addOnFailureListener { error ->
                trySend(UploadProgressState.Error(error.localizedMessage ?: "Failed to retrieve download URL"))
                close(error)
            }
        }.addOnFailureListener { error ->
            Log.e("OmniStorage", "Upload failed: ${error.message}", error)
            trySend(UploadProgressState.Error(error.localizedMessage ?: "Upload failed"))
            close(error)
        }

        awaitClose {
            if (uploadTask.isInProgress) {
                uploadTask.pause()
            }
        }
    }

    /**
     * Uploads avatar image specifically to users/{uid}/avatars/{timestamp}.jpg
     */
    suspend fun uploadAvatarSync(imageUri: Uri): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val filename = "${System.currentTimeMillis()}_avatar.jpg"
        val storagePath = "users/$uid/avatars/$filename"
        val storageRef = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("uploadedBy", uid)
            .setCustomMetadata("type", "avatar")
            .build()

        return try {
            storageRef.putFile(imageUri, metadata).await()
            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("OmniStorage", "Avatar upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads avatar image specifically to users/{uid}/profile.jpg
     */
    suspend fun uploadProfilePhoto(imageUri: Uri, uid: String): Result<String> {
        val targetUid = uid.ifBlank {
            try {
                requireUserId()
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        val storagePath = "users/$targetUid/profile.jpg"
        val storageRef = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("uploadedBy", targetUid)
            .setCustomMetadata("type", "profile_avatar")
            .build()

        return try {
            storageRef.putFile(imageUri, metadata).await()
            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("OmniStorage", "Profile photo upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Complete End-to-End Media Pipeline:
     * 1. Uploads media to Firebase Cloud Storage (tracks 0-100% progress)
     * 2. Gets permanent download URL
     * 3. Persists post metadata permanently into Cloud Firestore
     */
    fun uploadAndPublishPost(
        mediaUri: Uri?,
        isVideo: Boolean,
        format: PostFormat,
        title: String?,
        caption: String,
        authorHandle: String,
        authorName: String,
        authorAvatar: String
    ): Flow<MediaPipelineResult> = callbackFlow {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            trySend(MediaPipelineResult.Failed(e.localizedMessage ?: "Authentication required"))
            close()
            return@callbackFlow
        }

        if (mediaUri == null) {
            // Text-only post
            trySend(MediaPipelineResult.SavingToFirestore)
            val postDoc = OmniPostDocument(
                uid = uid,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                authorHandle = authorHandle,
                mediaUrl = "",
                mediaType = "none",
                caption = caption,
                title = title ?: "",
                format = format.name
            )
            val result = firestoreRepository.createPost(postDoc)
            if (result.isSuccess) {
                trySend(MediaPipelineResult.Success(result.getOrThrow(), ""))
            } else {
                trySend(MediaPipelineResult.Failed(result.exceptionOrNull()?.message ?: "Failed to save post"))
            }
            close()
            return@callbackFlow
        }

        // Upload media first with progress
        val folder = if (isVideo) "users/$uid/videos" else "users/$uid/photos"
        val extension = if (isVideo) "mp4" else "jpg"
        val uniqueName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        val storageRef = storage.reference.child("$folder/$uniqueName")

        val metadata = StorageMetadata.Builder()
            .setContentType(if (isVideo) "video/mp4" else "image/jpeg")
            .setCustomMetadata("uploadedBy", uid)
            .build()

        val uploadTask = storageRef.putFile(mediaUri, metadata)

        uploadTask.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val percent = if (total > 0) ((transferred * 100) / total).toInt() else 0
            trySend(MediaPipelineResult.Uploading(percent))
        }.addOnSuccessListener {
            trySend(MediaPipelineResult.SavingToFirestore)
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val permanentUrl = downloadUri.toString()
                val postDoc = OmniPostDocument(
                    uid = uid,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatar,
                    authorHandle = authorHandle,
                    mediaUrl = permanentUrl,
                    mediaType = if (isVideo) "video" else "photo",
                    caption = caption,
                    title = title ?: "",
                    format = format.name
                )
                // Save into Firestore
                storageRef.storage.app // verify storage
                CoroutineScope(Dispatchers.IO).launch {
                    val createResult = firestoreRepository.createPost(postDoc)
                    if (createResult.isSuccess) {
                        trySend(MediaPipelineResult.Success(createResult.getOrThrow(), permanentUrl))
                    } else {
                        trySend(MediaPipelineResult.Failed(createResult.exceptionOrNull()?.message ?: "Failed to save post in Firestore"))
                    }
                    close()
                }
            }.addOnFailureListener { error ->
                trySend(MediaPipelineResult.Failed("Failed to get download URL: ${error.message}"))
                close(error)
            }
        }.addOnFailureListener { error ->
            trySend(MediaPipelineResult.Failed("Cloud Storage upload failed: ${error.message}"))
            close(error)
        }

        awaitClose {
            if (uploadTask.isInProgress) {
                uploadTask.cancel()
            }
        }
    }
}
