package com.example.trackrate.data.repository

import com.example.trackrate.data.local.TokenStore
import com.example.trackrate.data.remote.ApiException
import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.domain.model.SessionStatus
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: TrackRateApi,
    private val tokenStore: TokenStore
) {

    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    val currentUserId: String?
        get() = tokenStore.getUserId()

    suspend fun bootstrap() {
        if (_sessionStatus.value != SessionStatus.Initializing) return
        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) {
            _sessionStatus.value = SessionStatus.NotAuthenticated
            return
        }
        try {
            val user = api.me()
            tokenStore.saveSession(token, user.id)
            _sessionStatus.value = SessionStatus.Authenticated(user.id)
        } catch (_: Exception) {
            tokenStore.clear()
            _sessionStatus.value = SessionStatus.NotAuthenticated
        }
    }

    suspend fun signIn(identifier: String, password: String) {
        val response = api.login(identifier.trim(), password.trim())
        tokenStore.saveSession(response.accessToken, response.user.id)
        _sessionStatus.value = SessionStatus.Authenticated(response.user.id)
    }

    suspend fun signUp(email: String, password: String) {
        val response = api.register(email.trim(), password.trim())
        tokenStore.saveSession(response.accessToken, response.user.id)
        _sessionStatus.value = SessionStatus.Authenticated(response.user.id)
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        api.changePassword(
            currentPassword = currentPassword.trim(),
            newPassword = newPassword.trim(),
            confirmPassword = confirmPassword.trim()
        )
    }

    suspend fun signOut() {
        tokenStore.clear()
        _sessionStatus.value = SessionStatus.NotAuthenticated
    }

    fun isUnauthorized(error: Throwable): Boolean =
        error is ApiException && error.statusCode == HttpStatusCode.Unauthorized.value
}
