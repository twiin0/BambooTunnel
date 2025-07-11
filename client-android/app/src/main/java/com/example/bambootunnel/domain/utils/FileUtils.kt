package com.example.bambootunnel.domain.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

object FileUtils {

    fun getMimeType(uri: Uri, contentResolver: ContentResolver): String {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT ->
                contentResolver.getType(uri) ?: "application/octet-stream"
            else -> {
                val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                    ?: "application/octet-stream"
            }
        }
    }

    fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        // Try content resolver first
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            try {
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst() && cursor.columnCount > 0) {
                        return cursor.getString(0) ?: generateFallbackName(uri)
                    }
                }
            } catch (e: Exception) {
            }
        }

        // Fallback approaches
        return generateFallbackName(uri)
    }

    private fun generateFallbackName(uri: Uri): String {
        // Try last path segment
        uri.lastPathSegment?.let {
            if (it.isNotBlank()) return it
        }

        // Try path parsing as last resort
        uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) return path.substring(cut + 1)
        }

        return "unnamed_${System.currentTimeMillis()}"
    }

    suspend fun prepareFilePart(
        partName: String,
        fileUri: Uri,
        contentResolver: ContentResolver
    ): MultipartBody.Part {
        val mimeType = getMimeType(fileUri, contentResolver)
        val fileName = getFileName(fileUri, contentResolver)
        val input: InputStream = contentResolver.openInputStream(fileUri)
            ?: error("Cannot open stream")
        val bytes = input.use { it.readBytes() }

        return MultipartBody.Part.createFormData(
            partName,
            fileName,
            bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        )
    }
}