package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.model.OmniPostDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediaUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MEDIA_URI = "media_uri"
        const val KEY_IS_VIDEO = "is_video"
        const val KEY_FORMAT = "format"
        const val KEY_TITLE = "title"
        const val KEY_CAPTION = "caption"
        const val KEY_AUTHOR_HANDLE = "author_handle"
        const val KEY_AUTHOR_NAME = "author_name"
        const val KEY_AUTHOR_AVATAR = "author_avatar"
        const val KEY_UID = "uid"

        const val PROGRESS_KEY = "progress"
        const val STATUS_KEY = "status"
        const val RESULT_POST_ID = "post_id"
        const val RESULT_DOWNLOAD_URL = "download_url"

        fun enqueueUpload(
            context: Context,
            mediaUri: Uri,
            isVideo: Boolean,
            format: String,
            title: String?,
            caption: String,
            authorHandle: String,
            authorName: String,
            authorAvatar: String,
            uid: String
        ): UUID {
            val inputData = Data.Builder()
                .putString(KEY_MEDIA_URI, mediaUri.toString())
                .putBoolean(KEY_IS_VIDEO, isVideo)
                .putString(KEY_FORMAT, format)
                .putString(KEY_TITLE, title ?: "")
                .putString(KEY_CAPTION, caption)
                .putString(KEY_AUTHOR_HANDLE, authorHandle)
                .putString(KEY_AUTHOR_NAME, authorName)
                .putString(KEY_AUTHOR_AVATAR, authorAvatar)
                .putString(KEY_UID, uid)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag("omni_media_upload")
                .build()

            WorkManager.getInstance(context).enqueue(uploadRequest)
            return uploadRequest.id
        }
    }

    override suspend fun doWork(): Result {
        val mediaUriString = inputData.getString(KEY_MEDIA_URI) ?: return Result.failure()
        val isVideo = inputData.getBoolean(KEY_IS_VIDEO, true)
        val format = inputData.getString(KEY_FORMAT) ?: if (isVideo) "TUBE" else "SNAP"
        val title = inputData.getString(KEY_TITLE)
        val caption = inputData.getString(KEY_CAPTION) ?: ""
        val authorHandle = inputData.getString(KEY_AUTHOR_HANDLE) ?: "creator"
        val authorName = inputData.getString(KEY_AUTHOR_NAME) ?: "Omni Creator"
        val authorAvatar = inputData.getString(KEY_AUTHOR_AVATAR) ?: ""
        val uid = inputData.getString(KEY_UID) ?: FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        val mediaUri = Uri.parse(mediaUriString)

        setProgress(workDataOf(PROGRESS_KEY to 5, STATUS_KEY to "Initializing 4K upload channel"))

        val storage = FirebaseStorage.getInstance()
        val folder = if (isVideo) "users/$uid/videos" else "users/$uid/photos"
        val extension = if (isVideo) "mp4" else "jpg"
        val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        val storageRef = storage.reference.child("$folder/$filename")

        val metadata = StorageMetadata.Builder()
            .setContentType(if (isVideo) "video/mp4" else "image/jpeg")
            .setCustomMetadata("uploadedBy", uid)
            .setCustomMetadata("authorHandle", authorHandle)
            .setCustomMetadata("format", format)
            .build()

        return try {
            val uploadTask = storageRef.putFile(mediaUri, metadata)

            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                val transferred = snapshot.bytesTransferred
                if (total > 0) {
                    val percent = ((transferred * 100) / total).toInt()
                    // Send WorkManager progress asynchronously
                    kotlinx.coroutines.runBlocking {
                        setProgress(workDataOf(PROGRESS_KEY to percent, STATUS_KEY to "Uploading 4K media: $percent%"))
                    }
                }
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            setProgress(workDataOf(PROGRESS_KEY to 95, STATUS_KEY to "Broadcasting post to Global Feed"))

            // Save post metadata to public Cloud Firestore posts collection
            val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            val newPostRef = db.collection("posts").document()

            val postDoc = OmniPostDocument(
                postId = newPostRef.id,
                uid = uid,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                authorHandle = authorHandle,
                mediaUrl = downloadUrl,
                mediaType = if (isVideo) "video" else "photo",
                caption = caption,
                title = title ?: "",
                format = format,
                likesCount = 0L,
                commentsCount = 0L,
                sharesCount = 0L
            )

            newPostRef.set(postDoc).await()

            setProgress(workDataOf(PROGRESS_KEY to 100, STATUS_KEY to "Published to Global Feed"))
            Result.success(
                workDataOf(
                    RESULT_POST_ID to newPostRef.id,
                    RESULT_DOWNLOAD_URL to downloadUrl
                )
            )
        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "Background media upload failed: ${e.message}", e)
            Result.failure(workDataOf("error" to (e.localizedMessage ?: "Upload failed")))
        }
    }
}
