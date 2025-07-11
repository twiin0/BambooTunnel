package com.example.bambootunnel.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.bambootunnel.data.api.ApiService
import com.example.bambootunnel.data.model.FileItem
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class FileRepository(context: Context) {
    private var api: ApiService
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private var baseUrl: String = "http://dummy-url/"

    init {
        api = createApiService(baseUrl)
    }

    private fun createApiService(url: String): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    fun setBaseUrl(ip: String) {
        baseUrl = "http://$ip:8022"
        api = createApiService(baseUrl)
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun browseFiles(path: String): List<FileItem> {
        return api.browse(path)
    }

    suspend fun uploadFile(part: MultipartBody.Part) {
        api.uploadFile(part)
    }

    fun getDownloadUrl(file: FileItem): String {
        val encodedPath = file.path.split("/")
            .joinToString("/") { URLEncoder.encode(it, "UTF-8") }
        return "${baseUrl.trimEnd('/')}/download/$encodedPath"
    }

    fun getStoredIp(): String? = prefs.getString("ip_address", null)

    fun saveIp(ip: String) {
        prefs.edit { putString("ip_address", ip) }
    }
}