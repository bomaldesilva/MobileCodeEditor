package com.example.codeeditor

import android.content.Context
import android.util.Log
import com.example.codeeditor.repository.EditorRepository
import com.example.codeeditor.storage.DiffManager
import java.io.File

class FileManager(private val context: Context) {
    private val repository = EditorRepository(context)

    // Save a new version using unified diff deltas
    suspend fun saveVersion(fileName: String, content: String, versionName: String) {
        val fileEntity = repository.getOrCreateFile(fileName, content)
        repository.saveVersion(fileEntity.fileId, content, versionName)
    }

    suspend fun getVersions(fileName: String) = repository.getVersionHistory(
        repository.getOrCreateFile(fileName).fileId
    )

    // Reconstruct file from unified diff deltas
    fun applyPatch(base: String, patchString: String): String {
        return DiffManager.applyPatch(base, patchString)
    }

    // Create a new file (if not exists) and return its name
    fun createNewFile(fileName: String): String {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file.name
    }

    // Save text content to a file (creates it if missing)
    fun saveFile(fileName: String, content: String) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(content)
        Log.d("FileManager", "Saved to ${file.absolutePath}")
    }

    // Open a file and return its content (empty if not exists)
    fun openFile(fileName: String): String {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readText() else ""
    }
}
