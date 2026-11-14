package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UsernameReservation
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

sealed interface HandleValidationState {
    object Idle : HandleValidationState
    object Checking : HandleValidationState
    data class Available(val handle: String) : HandleValidationState
    data class Taken(val handle: String) : HandleValidationState
    data class Invalid(val reason: String) : HandleValidationState
}

class OmniAuthRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getWebClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Interactive Google Sign-In with Jetpack CredentialManager
     */
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        val clientId = getWebClientId()
            ?: return Result.failure(IllegalStateException("Google Web Client ID missing in google-services.json"))

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase user is null after sign-in"))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential response received"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled by user: ${e.message}", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    /**
     * Silent background auto-sign-in for returning users
     */
    suspend fun attemptAutoSignIn(): Result<FirebaseUser> {
        if (auth.currentUser != null) {
            return Result.success(auth.currentUser!!)
        }
        val clientId = getWebClientId()
            ?: return Result.failure(IllegalStateException("Client ID missing"))

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase user is null after auto-sign-in"))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No auto-sign-in credential"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time unique username (@handle) validation engine
     * Checks format & checks against Firestore /usernames/{handle}
     */
    fun validateHandleInRealTime(rawHandle: String): Flow<HandleValidationState> = flow {
        val clean = rawHandle.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) {
            emit(HandleValidationState.Idle)
            return@flow
        }

        // Format rules: 3 to 30 characters, letters, numbers, and underscores only
        val regex = Regex("^[a-z0-9_]{3,30}$")
        if (!regex.matches(clean)) {
            emit(HandleValidationState.Invalid("Must be 3-30 letters, numbers, or underscores (no spaces)"))
            return@flow
        }

        emit(HandleValidationState.Checking)

        try {
            val snapshot = db.collection("usernames").document(clean).get().await()
            val currentUid = auth.currentUser?.uid
            if (snapshot.exists()) {
                val ownerUid = snapshot.getString("uid")
                if (ownerUid == currentUid) {
                    emit(HandleValidationState.Available(clean)) // already owned by this user
                } else {
                    emit(HandleValidationState.Taken(clean))
                }
            } else {
                emit(HandleValidationState.Available(clean))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$clean")
            emit(HandleValidationState.Invalid("Could not verify handle: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Authenticate using Email and Password
     */
    suspend fun signInWithEmailAndPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("User is null after sign in"))
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Email sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Create account with Email and Password
     */
    suspend fun createUserWithEmailAndPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("User is null after account creation"))
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Email account creation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically registers the user profile and reserves their unique @handle in Firestore
     */
    suspend fun registerUserProfileWithHandle(
        username: String,
        fullName: String,
        dob: String = "",
        avatarUrl: String = "",
        bio: String = ""
    ): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User must be authenticated before registering profile"))

        val cleanHandle = username.trim().removePrefix("@").lowercase()
        val email = user.email ?: "user@omnisocial.box"
        val resolvedAvatar = avatarUrl.ifBlank {
            user.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
        }

        return try {
            val batch = db.batch()

            // 1. Reserve username document
            val usernameRef = db.collection("usernames").document(cleanHandle)
            batch.set(
                usernameRef,
                mapOf(
                    "uid" to user.uid,
                    "username" to cleanHandle
                )
            )

            // 2. Create User profile document
            val userRef = db.collection("users").document(user.uid)
            val profile = UserProfile(
                uid = user.uid,
                email = email,
                username = cleanHandle,
                full_name = fullName.ifBlank { user.displayName ?: cleanHandle },
                dob = dob,
                avatar_url = resolvedAvatar,
                bio = bio
            )
            batch.set(userRef, profile)

            batch.commit().await()
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
            Result.failure(e)
        }
    }

    /**
     * Update existing user profile (Name, Bio, Avatar, DOB, Phone, Privacy)
     */
    suspend fun updateUserProfile(
        fullName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        dob: String? = null,
        phoneNumber: String? = null,
        privateAccount: Boolean? = null
    ): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val userRef = db.collection("users").document(user.uid)
        return try {
            val snapshot = userRef.get().await()
            val existing = snapshot.toObject(UserProfile::class.java)
                ?: UserProfile(uid = user.uid, email = user.email ?: "", username = "user")

            val updated = existing.copy(
                full_name = fullName ?: existing.full_name,
                bio = bio ?: existing.bio,
                avatar_url = avatarUrl ?: existing.avatar_url,
                dob = dob ?: existing.dob,
                phone_number = phoneNumber ?: existing.phone_number,
                private_account = privateAccount ?: existing.private_account
            )

            userRef.set(updated).await()
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
            Result.failure(e)
        }
    }

    /**
     * Change user account password
     */
    suspend fun updatePassword(newPass: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Change user account phone number in profile
     */
    suspend fun updatePhoneNumber(newPhone: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        val userRef = db.collection("users").document(user.uid)
        return try {
            userRef.update("phone_number", newPhone.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
            Result.failure(e)
        }
    }

    /**
     * Delete user account and corresponding Firestore records
     */
    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val uid = user.uid
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val handle = snapshot.getString("username")

            val batch = db.batch()
            batch.delete(userRef)
            if (!handle.isNullOrBlank()) {
                batch.delete(db.collection("usernames").document(handle))
            }
            batch.commit().await()

            user.delete().await()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out user and clear Credential Manager state
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "Sign out error", e)
            Result.failure(e)
        }
    }
}
