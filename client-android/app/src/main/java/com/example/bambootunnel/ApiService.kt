package com.example.bambootunnel

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("browse")
    suspend fun browse(@Query("path") path: String): List<FileItem>
}
