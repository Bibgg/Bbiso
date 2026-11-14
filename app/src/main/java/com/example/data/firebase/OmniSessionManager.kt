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
    context: Context
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

    // Firebase ID tokens stay in memory only. Firebase Auth refreshes them when needed.
    private val _jwtToken = MutableStateFlow<String?>(null)
    val jwtToken: StateFlow<String?> = _jwtToken.asStateFlow()

    private val _sessionStatus = MutableStateFlow("Initialized")
    val sessionStatus: StateFlow<String> = _sessionStatus.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isLoggedIn.value = user != null
            if (user != null) {
                scope.launch { refreshJwtToken(false) }
            } else {
                clearCachedSession()
            }
        }

        if (auth.currentUser != null) {
            scope.launch { refreshJwtToken(false) }
        }
    }

    /**
     * Gets a Firebase ID token for authenticated API requests.
     * The token is held in memory only and is never written to SharedPreferences.
     */
    suspend fun refreshJwtToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            val token = user.getIdToken(forceRefresh).await().token
            if (!token.isNullOrBlank()) {
                _jwtToken.value = token
                _sessionStatus.value = "Firebase ID token ready"
            }
            token
        } catch (e: Exception) {
            Log.w("OmniSession", "Could not refresh Firebase ID token: ${e.message}")
            _jwtToken.value = null
            _sessionStatus.value = "Token refresh failed"
            null
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
