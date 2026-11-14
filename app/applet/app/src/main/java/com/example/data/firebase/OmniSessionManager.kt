package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OmniSessionManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("omni_session_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _jwtToken = MutableStateFlow<String?>(prefs.getString("cached_jwt_token", null))
    val jwtToken: StateFlow<String?> = _jwtToken.asStateFlow()

    private val _isTokenValid = MutableStateFlow(false)
    val isTokenValid: StateFlow<Boolean> = _isTokenValid.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                scope.launch {
                    refreshJwtToken(false)
                }
            } else {
                clearSession()
            }
        }
    }

    suspend fun refreshJwtToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            val tokenResult = user.getIdToken(forceRefresh).await()
            val token = tokenResult.token
            if (!token.isNullOrBlank()) {
                _jwtToken.value = token
                _isTokenValid.value = true
                prefs.edit()
                    .putString("cached_jwt_token", token)
                    .putLong("token_cached_at", System.currentTimeMillis())
                    .apply()
                Log.d("OmniSessionManager", "JWT token successfully secured & refreshed")
            }
            token
        } catch (e: Exception) {
            Log.e("OmniSessionManager", "Failed to get JWT Token: ${e.message}")
            _isTokenValid.value = false
            null
        }
    }

    fun updateUserProfile(profile: UserProfile?) {
        _userProfile.value = profile
    }

    fun clearSession() {
        _jwtToken.value = null
        _isTokenValid.value = false
        _userProfile.value = null
        prefs.edit().clear().apply()
    }
}
