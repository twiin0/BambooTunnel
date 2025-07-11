package com.example.bambootunnel.presentation.handler

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import com.example.bambootunnel.data.model.FileItem
import com.example.bambootunnel.data.repository.FileRepository
import com.example.bambootunnel.domain.utils.FileUtils
import java.net.URLEncoder

class FileOperationsHandler(
    private val context: Context,
    private val fileRepository: FileRepository
) {
    fun downloadFile(file: FileItem) {
        val downloadUrl = fileRepository.getDownloadUrl(file)

        DownloadManager.Request(downloadUrl.toUri()).apply {
            setTitle(file.name)
            setDescription("Downloading ${file.name}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.name)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }.also { request ->
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
                .enqueue(request)
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun uploadFiles(uris: List<Uri>, contentResolver: ContentResolver) {
        uris.forEach { uri ->
            try {
                val part = FileUtils.prepareFilePart("file", uri, contentResolver)
                fileRepository.uploadFile(part)
                Toast.makeText(context, "File uploaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}