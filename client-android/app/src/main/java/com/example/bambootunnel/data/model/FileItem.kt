package com.example.bambootunnel.data.model

data class FileItem(
    val name: String, // title of file/dir
    val path: String, // relative path of the file/dir
    val type: String, // "file" or "directory"
    val preview: String? = null  // nullable, default null
)