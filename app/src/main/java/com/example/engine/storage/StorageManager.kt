package com.example.engine.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeString: String,
    val dateString: String,
    val extension: String
)

object StorageManager {

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getManageStorageIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else null
    }

    fun getAppProjectsDir(context: Context): File {
        val dir = File(context.filesDir, "Vortex3D/Projects")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDefaultStartDirectory(context: Context): File {
        val ext = Environment.getExternalStorageDirectory()
        return if (ext != null && ext.exists() && ext.canRead()) {
            ext
        } else {
            getAppProjectsDir(context)
        }
    }

    fun getQuickLocations(context: Context): List<Pair<String, File>> {
        val list = mutableListOf<Pair<String, File>>()

        val root = Environment.getExternalStorageDirectory()
        if (root != null && root.exists()) {
            list.add("Storage" to root)
        }

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.exists()) {
            list.add("Downloads" to downloads)
        }

        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents != null && documents.exists()) {
            list.add("Documents" to documents)
        }

        list.add("Projects" to getAppProjectsDir(context))
        return list
    }

    fun listDirectory(dir: File): List<FileItem> {
        val files = dir.listFiles() ?: return emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        return files.map { file ->
            val ext = file.extension.lowercase()
            val sizeStr = if (file.isDirectory) {
                "${file.list()?.size ?: 0} items"
            } else {
                formatFileSize(file.length())
            }
            val dateStr = dateFormat.format(Date(file.lastModified()))

            FileItem(
                file = file,
                name = file.name,
                isDirectory = file.isDirectory,
                sizeString = sizeStr,
                dateString = dateStr,
                extension = ext
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun createFolder(parent: File, folderName: String): Boolean {
        val newFolder = File(parent, folderName.trim())
        return if (!newFolder.exists()) newFolder.mkdirs() else false
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
        }
    }
}
