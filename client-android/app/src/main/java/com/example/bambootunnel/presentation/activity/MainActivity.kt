package com.example.bambootunnel.presentation.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bambootunnel.R
import com.example.bambootunnel.data.repository.FileRepository
import com.example.bambootunnel.presentation.adapter.FileAdapter
import com.example.bambootunnel.presentation.handler.FileOperationsHandler
import com.example.bambootunnel.presentation.handler.NavigationHandler
import com.example.bambootunnel.presentation.handler.ViewStateManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Component declarations
    private lateinit var fileRepository: FileRepository
    private lateinit var fileOperations: FileOperationsHandler
    private lateinit var navigation: NavigationHandler
    private lateinit var viewState: ViewStateManager
    private lateinit var fileAdapter: FileAdapter

    private val filePickCode = 1001 // Request code for file picker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize core components
        fileRepository = FileRepository(this)
        fileOperations = FileOperationsHandler(this, fileRepository)
        viewState = ViewStateManager(
            this,
            findViewById(R.id.recyclerView),
            findViewById(R.id.uploadSection),
            findViewById(R.id.btnDownload),
            findViewById(R.id.btnUpload),
        )

        // Set up UI event handlers
        findViewById<MaterialButton>(R.id.btnUpload).setOnClickListener {
            viewState.showUploadSection() // Switch to upload view
        }

        findViewById<MaterialButton>(R.id.btnDownload).setOnClickListener {
            viewState.showDownloadSection() // Switch to download view
        }

        findViewById<Button>(R.id.btnSelectFiles).setOnClickListener {
            openFilePicker() // Launch file selection
        }

        // Initialize remaining components
        navigation = NavigationHandler(this) { path -> loadFiles(path) }
        setupRecyclerView()
        initializeAppState() // Load initial data
    }

    // Opens system file picker for document selection
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  // Allow all file types
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Enable multi-select
        }
        startActivityForResult(intent, filePickCode)
    }

    // Handles results from file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == filePickCode && resultCode == RESULT_OK) {
            data?.let {
                val uris = mutableListOf<Uri>()
                // Handle both single and multiple file selections
                it.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
                } ?: it.data?.let { uri -> uris.add(uri) }

                lifecycleScope.launch {
                    fileOperations.uploadFiles(uris, contentResolver)
                    viewState.showDownloadSection() // Return to download view
                    loadFiles(navigation.getCurrentPath()) // Refresh file list
                }
            }
        }
    }

    // Configures the RecyclerView for file display
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2-column grid
        fileAdapter = FileAdapter(fileRepository.getBaseUrl()) { file ->
            // Handle file/directory clicks
            if (file.type == "directory") {
                navigation.navigateTo(file.path) // Enter directory
            } else {
                fileOperations.downloadFile(file) // Download file
            }
        }
        recyclerView.adapter = fileAdapter
    }

    // Initializes app with saved IP or prompts for new one
    private fun initializeAppState() {
        fileRepository.getStoredIp()?.let { ip ->
            fileRepository.setBaseUrl(ip)
            loadFiles(navigation.getCurrentPath()) // Load files if IP exists
        } ?: promptForIp() // Otherwise prompt for IP
        viewState.showDownloadSection() // Default to download view
    }

    // Shows IP input dialog
    private fun promptForIp(message: String = "Enter IP Address") {
        val input = EditText(this).apply {
            setText(fileRepository.getStoredIp().orEmpty())
            hint = "IP Address (e.g., 192.168.0.100)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        AlertDialog.Builder(this)
            .setTitle(message)
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isEmpty()) {
                    input.error = "IP address cannot be empty"
                    return@setPositiveButton
                }
                fileRepository.saveIp(ip)
                fileRepository.setBaseUrl(ip)
                loadFiles(navigation.getCurrentPath())
            }
            .setCancelable(false)
            .show()
    }

    // Loads files for given path
    private fun loadFiles(path: String) {
        lifecycleScope.launch {
            try {
                val list = fileRepository.browseFiles(path)
                fileAdapter.submitList(list) // Update RecyclerView
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load files", Toast.LENGTH_SHORT).show()
                promptForIp("Could not connect to server. Please check IP.")
            }
        }
    }
}