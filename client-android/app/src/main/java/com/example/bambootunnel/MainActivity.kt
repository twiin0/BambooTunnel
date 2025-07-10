package com.example.bambootunnel

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.net.URLEncoder
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var api: ApiService

    private var ipAddress: String? = null
    private lateinit var baseUrl: String
    private val pathStack = mutableListOf("")

    private lateinit var btnDownload: Button
    private lateinit var btnUpload: Button
    private lateinit var uploadSection: LinearLayout
    private lateinit var btnSelectFiles: Button

    private val filePickCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnDownload = findViewById(R.id.btnDownload)
        btnUpload = findViewById(R.id.btnUpload)
        uploadSection = findViewById(R.id.uploadSection)
        btnSelectFiles = findViewById(R.id.btnSelectFiles)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        btnDownload.setOnClickListener { showDownloadSection() }
        btnUpload.setOnClickListener { showUploadSection() }
        btnSelectFiles.setOnClickListener { openFilePicker() }

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

        getOrPromptIp()
        showDownloadSection()
    }

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
            .setPositiveButton("Connect", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    prefs.edit { putString("ip_address", ip) }
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

    private fun setupRetrofitAndLoadFiles() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiService::class.java)

        fileAdapter = FileAdapter(baseUrl) { file ->
            if (file.type == "directory") {
                pathStack.add(file.path)
                loadFiles(file.path)
            } else {
                downloadFile(file)
            }
        }
        recyclerView.adapter = fileAdapter
        loadFiles(pathStack.last())
    }

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

    private fun downloadFile(file: FileItem) {
        val encodedPath = file.path
            .split("/")
            .joinToString("/") { URLEncoder.encode(it, "UTF-8") }
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

    private fun showDownloadSection() {
        recyclerView.visibility = RecyclerView.VISIBLE
        uploadSection.visibility = LinearLayout.GONE

        btnDownload.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        btnUpload.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
    }

    private fun showUploadSection() {
        recyclerView.visibility = View.GONE
        uploadSection.visibility = View.VISIBLE

        btnUpload.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        btnDownload.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, filePickCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == filePickCode && resultCode == Activity.RESULT_OK) {
            data?.let {
                val uris = mutableListOf<Uri>()
                it.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
                } ?: it.data?.let { uri -> uris.add(uri) }
                uploadFiles(uris)
            }
        }
    }

    private fun uploadFiles(uris: List<Uri>) {
        lifecycleScope.launch {
            for (uri in uris) {
                try {
                    val part = prepareFilePart("file", uri)
                    api.uploadFile(part)
                    Toast.makeText(this@MainActivity, "Uploaded: ${getFileName(uri)}", Toast.LENGTH_SHORT).show()
                } catch (e: HttpException) {
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message()}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Upload error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val resolver: ContentResolver = applicationContext.contentResolver
            val mimeType = getMimeType(fileUri)
            val input: InputStream = resolver.openInputStream(fileUri) ?: error("Cannot open stream")
            val bytes = input.readBytes().also { input.close() }
            val fileName = getFileName(fileUri)
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, fileName, requestBody)
        }

    private fun getMimeType(uri: Uri): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: "application/octet-stream"
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        }
        if (name == null) uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) name = path.substring(cut + 1)
        }
        return name ?: "file"
    }
}
