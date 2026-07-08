package com.example.trackrate.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

data class ImagePayload(
    val bytes: ByteArray,
    val contentType: String,
    val fileName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImagePayload
        return bytes.contentEquals(other.bytes) &&
            contentType == other.contentType &&
            fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

object ImageUriReader {

    private val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")

    fun read(context: Context, uri: Uri): ImagePayload {
        val resolver = context.contentResolver
        val contentType = resolver.getType(uri)?.lowercase()
            ?.takeIf { it in allowedTypes }
            ?: guessFromUri(uri)
            ?: "image/jpeg"

        val extension = when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer la imagen")

        if (bytes.isEmpty()) error("La imagen está vacía")
        if (bytes.size > MAX_BYTES) error("La imagen supera el límite de 10 MB")

        return ImagePayload(
            bytes = bytes,
            contentType = contentType,
            fileName = "upload.$extension"
        )
    }

    private fun guessFromUri(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString()) ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    private const val MAX_BYTES = 10 * 1024 * 1024
}
