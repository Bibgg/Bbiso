package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UsernameReservation
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface HandleValidationState {
    data object Idle : HandleValidationState
    data object Checking : HandleValidationState
    data class Available(val handle: String) : HandleValidationState
    data class Taken(val handle: String) : HandleValidationState
    data class Invalid(val reason: String) : HandleValidationState
}

class OmniAuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val credentialManager = CredentialManager.create(context)

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun validateHandleInRealTime(handle: String): Flow<HandleValidationState> = flow {
        val sanitized = handle.trim().removePrefix("@").lowercase()
        if (sanitized.isEmpty()) {
            emit(HandleValidationState.Idle)
            return@flow
        }
        if (sanitized.length < 3) {
            emit(HandleValidationState.Invalid("Username must be at least 3 characters"))
            return@flow
        }
        if (!sanitized.matches(Regex("^[a-z0-9_.]+$"))) {
            emit(HandleValidationState.Invalid("Only letters, numbers, underscores, and dots allowed"))
            return@flow
        }

        emit(HandleValidationState.Checking)
        try {
            val doc = firestore.collection("usernames").document(sanitized).get().await()
            if (doc.exists()) {
                val ownerUid = doc.getString("uid")
                if (ownerUid != null && ownerUid == auth.currentUser?.uid) {
                    emit(HandleValidationState.Available(sanitized))
                } else {
                    emit(HandleValidationState.Taken(sanitized))
                }
            } else {
                emit(HandleValidationState.Available(sanitized))
            }
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Handle validation error: ${e.message}")
            emit(HandleValidationState.Invalid("Error checking handle. Please retry."))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun signInWithGoogle(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user is null")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("OmniAuthRepository", "Credential Manager failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Email sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("User creation failed")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Email registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun registerUserProfileWithHandle(
        username: String,
        fullName: String,
        dob: String = "",
        avatarUrl: String = "",
        bio: String = "",
        phoneNumber: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        val sanitizedHandle = username.trim().removePrefix("@").lowercase()

        val profile = UserProfile(
            uid = user.uid,
            email = user.email ?: "",
            username = sanitizedHandle,
            full_name = fullName.trim(),
            display_name = fullName.trim().ifBlank { sanitizedHandle },
            dob = dob.trim(),
            avatar_url = avatarUrl.trim(),
            bio = bio.trim(),
            phone_number = phoneNumber.trim(),
            phone_or_email = user.email ?: phoneNumber.trim(),
            created_at = Timestamp.now(),
            updated_at = Timestamp.now()
        )

        try {
            firestore.runBatch { batch ->
                val handleRef = firestore.collection("usernames").document(sanitizedHandle)
                batch.set(handleRef, UsernameReservation(uid = user.uid, username = sanitizedHandle, reserved_at = Timestamp.now()))

                val userRef = firestore.collection("users").document(user.uid)
                batch.set(userRef, profile)
            }.await()

            Result.success(profile)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Failed to register handle and profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val userRef = firestore.collection("users").document(user.uid)
            userRef.set(profile.copy(updated_at = Timestamp.now())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Failed to update profile", e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
        try {
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OmniAuthRepository", "Sign out error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            firestore.collection("users").document(user.uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
