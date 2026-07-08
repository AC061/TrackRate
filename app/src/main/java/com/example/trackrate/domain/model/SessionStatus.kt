package com.example.trackrate.domain.model

sealed interface SessionStatus {
    data object Initializing : SessionStatus
    data object NotAuthenticated : SessionStatus
    data class Authenticated(val userId: String) : SessionStatus
}
