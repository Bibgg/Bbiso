package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OmniSessionManager(
    private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("omni_session_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _jwtToken = MutableStateFlow<String?>(prefs.getString("jwt_token", null))
    val jwtToken: StateFlow<String?> = _jwtToken.asStateFlow()

    private val _sessionStatus = MutableStateFlow("Initialized")
    val sessionStatus: StateFlow<String> = _sessionStatus.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isLoggedIn.value = user != null
            if (user != null) {
                scope.launch {
                    refreshJwtToken(false)
                }
            } else {
                clearCachedSession()
            }
        }

        // Check if user was already cached or auto-refresh
        if (auth.currentUser != null) {
            scope.launch {
                refreshJwtToken(false)
            }
        }
    }

    /**
     * Refreshes JWT token from Firebase Auth and persists it to secure preferences.
     */
    suspend fun refreshJwtToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            val tokenResult = user.getIdToken(forceRefresh).await()
            val token = tokenResult.token
            if (!token.isNullOrBlank()) {
                _jwtToken.value = token
                _sessionStatus.value = "Verified Cloud JWT Active"
                prefs.edit()
                    .putString("jwt_token", token)
                    .putString("user_uid", user.uid)
                    .putString("user_email", user.email ?: "")
                    .putLong("last_token_refresh", System.currentTimeMillis())
                    .apply()
            }
            token
        } catch (e: Exception) {
            Log.w("OmniSession", "Could not refresh JWT token: ${e.message}")
            prefs.getString("jwt_token", null)
        }
    }

    fun updateCachedProfile(profile: UserProfile?) {
        _userProfile.value = profile
        if (profile != null) {
            prefs.edit()
                .putString("cached_username", profile.username)
                .putString("cached_full_name", profile.full_name)
                .putString("cached_avatar_url", profile.avatar_url)
                .apply()
        }
    }

    fun clearCachedSession() {
        _jwtToken.value = null
        _userProfile.value = null
        _sessionStatus.value = "Logged Out"
        prefs.edit().clear().apply()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserSignedIn(): Boolean = auth.currentUser != null
}
