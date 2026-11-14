package com.example.data.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OmniStorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun uploadProfilePhoto(imageUri: Uri, uid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = storage.reference.child("users/$uid/profile.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            storageRef.putFile(imageUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("OmniStorageRepository", "Profile photo uploaded: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("OmniStorageRepository", "Profile photo upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadVideo(videoUri: Uri, uid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val filename = "video_${System.currentTimeMillis()}.mp4"
            val storageRef = storage.reference.child("users/$uid/videos/$filename")
            val metadata = StorageMetadata.Builder()
                .setContentType("video/mp4")
                .build()

            storageRef.putFile(videoUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("OmniStorageRepository", "Video upload failed", e)
            Result.failure(e)
        }
    }
}
