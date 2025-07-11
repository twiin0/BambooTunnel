package com.example.bambootunnel.data.api

import com.example.bambootunnel.data.model.FileItem
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @GET("browse")
    suspend fun browse(@Query("path") path: String): List<FileItem>

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): ResponseBody
}