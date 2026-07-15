package com.example.trackrate.data.remote

import com.example.trackrate.data.repository.AuthRepository
import dagger.Lazy
import io.ktor.http.HttpStatusCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnauthorizedSessionHandler @Inject constructor(
    private val authRepository: Lazy<AuthRepository>
) {
    suspend fun handleIfUnauthorized(statusCode: Int) {
        if (statusCode == HttpStatusCode.Unauthorized.value) {
            authRepository.get().signOut()
        }
    }
}
