package com.example.bambootunnel

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var api: ApiService

    private var ipAddress: String? = null
    private lateinit var baseUrl: String
    private val pathStack = mutableListOf("")  // root = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // 2. Handle system back to navigate up folders
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pathStack.size > 1) {
                    pathStack.removeAt(pathStack.lastIndex)
                    loadFiles(pathStack.last())
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // 3. Kick off IP prompt (which will initialize adapter & load data)
        getOrPromptIp()
    }

    // prompts user for ip (reusable for different circumstances)
    private fun promptForIp(message: String = "Enter IP Address") {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastIp = prefs.getString("ip_address", "") ?: ""

        val input = EditText(this).apply {
            setText(lastIp)
            hint = "IP Address"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSelection(text.length)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(message)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Connect", null) // override click listener below
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    prefs.edit().putString("ip_address", ip).apply()
                    ipAddress = ip
                    baseUrl = "http://$ipAddress:8022"
                    setupRetrofitAndLoadFiles()
                    dialog.dismiss()
                } else {
                    input.error = "IP address required"
                }
            }
        }
        dialog.show()
    }

    // gets last used ip, i
    private fun getOrPromptIp() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        ipAddress = prefs.getString("ip_address", null)

        if (ipAddress.isNullOrEmpty()) {
            promptForIp()
        } else {
            baseUrl = "http://$ipAddress:8022"
            setupRetrofitAndLoadFiles()
        }
    }

    // Configures the network client, sets up your file list UI, and loads the initial file list from the backend.
    private fun setupRetrofitAndLoadFiles() {
        // Build Retrofit once baseUrl is known
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiService::class.java)

        // Initialize adapter with now-known baseUrl
        fileAdapter = FileAdapter(baseUrl) { file ->
            if (file.type == "directory") {
                pathStack.add(file.path)
                loadFiles(file.path)
            } else {
                downloadFile(file)
            }
        }
        recyclerView.adapter = fileAdapter

        // Load root folder
        loadFiles(pathStack.last())
    }

    // load files so viewable except on error, re-prompt for ip
    private fun loadFiles(path: String) {
        lifecycleScope.launch {
            try {
                val list = api.browse(path)
                fileAdapter.submitList(list)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load files", Toast.LENGTH_SHORT).show()
                promptForIp("Could not connect to server. Please check IP.")
            }
        }
    }

    // decodes path of desired file and makes download request
    private fun downloadFile(file: FileItem) {
        // URL‑encode each path segment to support spaces & special chars
        val encodedPath = file.path
            .split("/")
            .joinToString("/") { segment -> URLEncoder.encode(segment, "UTF-8") }
        val downloadUrl = "${baseUrl.trimEnd('/')}/download/$encodedPath"

        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle(file.name)
            .setDescription("Downloading ${file.name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.name)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }
}
