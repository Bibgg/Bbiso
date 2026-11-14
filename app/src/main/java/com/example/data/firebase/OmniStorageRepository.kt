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
        authorAvatar: String,
        tags: List<String> = emptyList()
    ): Flow<MediaPipelineResult> = callbackFlow {
        try {
            val uid = requireUserId()
            if (mediaUri == null) {
                trySend(MediaPipelineResult.SavingToFirestore)
                val textPost = OmniPostDocument(
                    uid = uid,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatar,
                    authorHandle = authorHandle,
                    mediaUrl = "",
                    mediaType = "none",
                    caption = caption,
                    title = title ?: "",
                    format = format.name,
                    tags = tags
                )
                val saved = firestoreRepository.createPost(textPost)
                if (saved.isSuccess) {
                    trySend(MediaPipelineResult.Success(saved.getOrThrow(), ""))
                } else {
                    trySend(MediaPipelineResult.Failed(saved.exceptionOrNull()?.localizedMessage ?: "Could not publish post"))
                }
            } else {
                trySend(MediaPipelineResult.Uploading(0))
                val uploaded = CloudinaryUploadRepository(context, auth).upload(
                    uri = mediaUri,
                    isVideo = isVideo,
                    purpose = "post",
                    onProgress = { percent -> trySend(MediaPipelineResult.Uploading(percent)) }
                )
                trySend(MediaPipelineResult.SavingToFirestore)
                val post = OmniPostDocument(
                    uid = uid,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatar,
                    authorHandle = authorHandle,
                    mediaUrl = uploaded.secureUrl,
                    mediaType = if (isVideo) "video" else "photo",
                    thumbnailUrl = uploaded.thumbnailUrl,
                    cloudinaryPublicId = uploaded.publicId,
                    mediaBytes = uploaded.bytes,
                    durationSeconds = uploaded.durationSeconds,
                    caption = caption,
                    title = title ?: "",
                    format = format.name,
                    tags = tags
                )
                val saved = firestoreRepository.createPost(post)
                if (saved.isSuccess) {
                    trySend(MediaPipelineResult.Success(saved.getOrThrow(), uploaded.secureUrl))
                } else {
                    trySend(MediaPipelineResult.Failed(saved.exceptionOrNull()?.localizedMessage ?: "Media uploaded, but post metadata could not be saved"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("OmniStorage", "Cloudinary media pipeline failed: ${e.message}", e)
            trySend(MediaPipelineResult.Failed(e.localizedMessage ?: "Media upload failed"))
        } finally {
            close()
        }
    }

}
