package com.example.data.firebase

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Call
import okhttp3.Callback
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String,
    val thumbnailUrl: String,
    val bytes: Long,
    val format: String,
    val durationSeconds: Double?
)

/**
 * Requests a UID-scoped upload signature from Bbiso API, then sends the media
 * bytes directly from the Android device to Cloudinary. The API secret is
 * never present in this class or in the APK.
 */
class CloudinaryUploadRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val apiBaseUrl: String = "https://bbiso-api.onrender.com"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .callTimeout(12, TimeUnit.MINUTES)
        .build()

    suspend fun upload(
        uri: Uri,
        isVideo: Boolean,
        purpose: String = "post",
        onProgress: (Int) -> Unit = {}
    ): CloudinaryUploadResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: throw IOException("Please sign in before uploading media")
        val idToken = user.getIdToken(true).await().token
            ?: throw IOException("Could not refresh the secure sign-in token")

        val mediaType = if (isVideo) "video" else "image"
        val ticketJson = JSONObject()
            .put("mediaType", mediaType)
            .put("purpose", purpose)
        val ticketRequest = Request.Builder()
            .url("$apiBaseUrl/api/media/upload-signature")
            .header("Authorization", "Bearer $idToken")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), ticketJson.toString()))
            .build()

        val ticket = executeJsonSuspend(ticketRequest, "Could not prepare secure media upload")
        val cloudName = ticket.getString("cloudName")
        val apiKey = ticket.getString("apiKey")
        val timestamp = ticket.getLong("timestamp")
        val signature = ticket.getString("signature")
        val folder = ticket.getString("folder")
        val publicId = ticket.getString("publicId")
        val resourceType = ticket.getString("resourceType")
        val allowedFormats = ticket.getString("allowedFormats")
        val maxBytes = ticket.getLong("maxBytes")

        val fileSize = getFileSize(uri)
        if (fileSize <= 0L) throw IOException("Could not read the selected file size")
        if (fileSize > maxBytes) {
            val maxMb = maxBytes / (1024 * 1024)
            throw IOException("This file is too large. Maximum allowed size is $maxMb MB.")
        }

        val mime = context.contentResolver.getType(uri)
            ?: if (isVideo) "video/mp4" else "image/jpeg"
        if (isVideo && !mime.startsWith("video/")) throw IOException("Please select a video file")
        if (!isVideo && !mime.startsWith("image/")) throw IOException("Please select an image file")
        val fileName = getFileName(uri) ?: if (isVideo) "upload.mp4" else "upload.jpg"

        val fileBody = object : RequestBody() {
            override fun contentType() = mime.toMediaTypeOrNull()
            override fun contentLength() = fileSize

            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open selected media")
                input.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    var transferred = 0L
                    while (true) {
                        val count = stream.read(buffer)
                        if (count == -1) break
                        sink.write(buffer, 0, count)
                        transferred += count
                        onProgress(((transferred * 100) / fileSize).toInt().coerceIn(0, 99))
                    }
                }
            }
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("timestamp", timestamp.toString())
            .addFormDataPart("folder", folder)
            .addFormDataPart("public_id", publicId)
            .addFormDataPart("allowed_formats", allowedFormats)
            .addFormDataPart("signature", signature)
            .build()

        val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"
        val uploadRequest = Request.Builder().url(uploadUrl).post(multipart).build()
        val uploaded = executeJsonSuspend(uploadRequest, "Cloudinary upload failed")

        val secureUrl = uploaded.optString("secure_url")
        val returnedPublicId = uploaded.optString("public_id")
        if (!secureUrl.startsWith("https://") || !(returnedPublicId == publicId || returnedPublicId.endsWith("/$publicId"))) {
            throw IOException("Cloudinary returned an invalid media response")
        }

        val thumbnail = if (isVideo) {
            secureUrl.replace("/upload/", "/upload/so_0,w_720,c_limit,q_auto,f_jpg/")
                .replace(Regex("\\.(mp4|mov|m4v|webm)(\\?.*)?$", RegexOption.IGNORE_CASE), ".jpg")
        } else {
            secureUrl.replace("/upload/", "/upload/w_720,c_limit,q_auto,f_auto/")
        }

        onProgress(100)
        CloudinaryUploadResult(
            secureUrl = secureUrl,
            publicId = returnedPublicId,
            thumbnailUrl = thumbnail,
            bytes = uploaded.optLong("bytes", fileSize),
            format = uploaded.optString("format"),
            durationSeconds = if (uploaded.has("duration")) uploaded.optDouble("duration") else null
        )
    }

    private suspend fun executeJsonSuspend(request: Request, fallbackMessage: String): JSONObject =
        suspendCancellableCoroutine { continuation ->
            val call: Call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            val raw = it.body?.string().orEmpty()
                            if (!it.isSuccessful) {
                                val message = try { JSONObject(raw).optString("error").ifBlank { fallbackMessage } }
                                    catch (_: Exception) { fallbackMessage }
                                throw IOException(message)
                            }
                            val json = try { JSONObject(raw) } catch (_: Exception) {
                                throw IOException("The media service returned an invalid response")
                            }
                            if (continuation.isActive) continuation.resume(json)
                        } catch (error: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    }
                }
            })
        }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor: Cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) return cursor.getLong(index)
            }
        }
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
