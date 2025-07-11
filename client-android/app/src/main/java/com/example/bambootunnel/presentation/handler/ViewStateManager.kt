package com.example.bambootunnel.presentation.handler

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bambootunnel.R
import com.google.android.material.button.MaterialButton

class ViewStateManager(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val uploadSection: LinearLayout,
    private val btnDownload: MaterialButton,
    private val btnUpload: MaterialButton,
) {
    private var isUploadMode = false

    fun showDownloadSection() {
        recyclerView.visibility = View.VISIBLE
        uploadSection.visibility = View.GONE
        btnDownload.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
        btnUpload.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
        isUploadMode = false
    }

    fun showUploadSection() {
        recyclerView.visibility = View.GONE
        uploadSection.visibility = View.VISIBLE
        btnUpload.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
        btnDownload.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
        isUploadMode = true
    }
}