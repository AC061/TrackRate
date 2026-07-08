package com.example.trackrate.data.remote

class ApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
