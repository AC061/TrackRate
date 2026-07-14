package com.example.trackrate.util

import android.net.Uri
import com.example.trackrate.BuildConfig

object MediaUrlResolver {

    private val apiHost: String by lazy {
        Uri.parse(BuildConfig.API_BASE_URL).host ?: "10.0.2.2"
    }

    private val unreachableHosts = setOf("localhost", "127.0.0.1", "minio")

    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val uri = Uri.parse(url)
        val host = uri.host ?: return url
        if (host !in unreachableHosts) return url

        val port = uri.port.takeIf { it != -1 } ?: 9000
        return uri.buildUpon()
            .encodedAuthority("$apiHost:$port")
            .build()
            .toString()
    }
}
