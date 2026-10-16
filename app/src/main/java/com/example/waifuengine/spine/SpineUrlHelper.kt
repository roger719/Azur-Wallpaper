package com.example.waifuengine.spine

import android.content.Context
import android.net.Uri

object SpineUrlHelper {
    fun convertToAssetUrl(context: Context, absolutePath: String): String {
        if (absolutePath.isEmpty()) return ""
        val cleanPath = if (absolutePath.startsWith("file://")) {
            absolutePath.removePrefix("file://")
        } else {
            absolutePath
        }
        val filesDirStr = context.filesDir.absolutePath
        return if (cleanPath.startsWith(filesDirStr)) {
            val relativePath = cleanPath.substring(filesDirStr.length).replace("\\", "/").trimStart('/')
            val encodedPath = relativePath.split("/").filter { it.isNotEmpty() }.joinToString("/") { Uri.encode(it) }
            "https://appassets.androidview.local/files/$encodedPath"
        } else {
            cleanPath
        }
    }
}
