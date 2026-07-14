package com.example.trackrate.util

import coil.intercept.Interceptor
import coil.request.ImageResult

class MediaUrlInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data is String) {
            val resolved = MediaUrlResolver.resolve(data)
            if (resolved != null && resolved != data) {
                return chain.proceed(request.newBuilder().data(resolved).build())
            }
        }
        return chain.proceed(request)
    }
}
