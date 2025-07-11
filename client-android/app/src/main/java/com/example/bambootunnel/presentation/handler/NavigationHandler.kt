package com.example.bambootunnel.presentation.handler

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class NavigationHandler(
    private val activity: AppCompatActivity,
    private val loadFiles: (String) -> Unit
) {
    private val pathStack = mutableListOf("")

    init {
        setupBackPressHandler()
    }

    fun navigateTo(path: String) {
        pathStack.add(path)
        loadFiles(path)
    }

    private fun setupBackPressHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pathStack.size > 1) {
                    pathStack.removeAt(pathStack.lastIndex)
                    loadFiles(pathStack.last())
                } else {
                    isEnabled = false
                    activity.onBackPressed()
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
    }

    fun getCurrentPath(): String = pathStack.last()
}