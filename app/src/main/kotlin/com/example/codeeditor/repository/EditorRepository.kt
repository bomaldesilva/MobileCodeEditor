package com.example.codeeditor.repository

import android.content.Context
import com.example.codeeditor.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EditorRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val fileDao = db.fileDao()
    private val versionDao = db.versionDao()
    private val recoveryDao = db.recoveryDao()
    private val recentFileDao = db.recentFileDao()

    /**
     * Creates a fresh unique file (e.g. Untitled.kt, Untitled_1.kt, Untitled_2.kt).
     */
    suspend fun createNewUniqueFile(baseName: String = "Untitled.kt"): FileEntity = withContext(Dispatchers.IO) {
        var candidateName = baseName
        var counter = 1
        val dotIndex = baseName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex != -1) baseName.substring(0, dotIndex) else baseName
        val ext = if (dotIndex != -1) baseName.substring(dotIndex) else ".kt"

        while (fileDao.getFileByName(candidateName) != null) {
            candidateName = "${nameWithoutExt}_$counter$ext"
            counter++
        }

        getOrCreateFile(candidateName, "")
    }

    /**
     * FIX: Upsert a file - always writes the given content to disk.
     * Unlike getOrCreateFile, this ALWAYS overwrites disk content with the provided content.
     * Used when opening external files from the file picker.
     */
    suspend fun upsertFile(fileName: String, content: String): FileEntity = withContext(Dispatchers.IO) {
        // Check if a DB record already exists for this filename
        var fileEntity = fileDao.getFileByName(fileName)
        val localFile = File(context.filesDir, fileName)

        if (fileEntity == null) {
            // Create new DB record
            localFile.createNewFile()
            val newFile = FileEntity(
                fileName = fileName,
                filePath = localFile.absolutePath,
                language = detectLanguage(fileName),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val fileId = fileDao.insertFile(newFile)
            fileEntity = newFile.copy(fileId = fileId)

            // Save Base Version (V1)
            versionDao.insertVersion(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = 1,
                    versionName = "Initial Version",
                    createdAt = System.currentTimeMillis(),
                    isBase = true,
                    patchContent = content
                )
            )
        } else {
            // Update existing record timestamp
            fileDao.updateFile(fileEntity.copy(updatedAt = System.currentTimeMillis()))
        }

        // ALWAYS write the new content to disk
        localFile.writeText(content, Charsets.UTF_8)

        // Add to recent files
        recentFileDao.insertRecent(
            RecentFileEntity(
                fileName = fileEntity.fileName,
                filePath = fileEntity.filePath,
                lastOpenedAt = System.currentTimeMillis()
            )
        )

        fileEntity
    }

    /**
     * Renames an active file and updates its path, extension, and language mode in DB.
     */
    suspend fun renameFile(fileId: Long, newFileName: String): FileEntity = withContext(Dispatchers.IO) {
        val fileEntity = fileDao.getFileById(fileId) ?: throw IllegalArgumentException("File not found")
        val newFileOnDisk = File(context.filesDir, newFileName)
        if (!newFileOnDisk.exists()) {
            newFileOnDisk.createNewFile()
        }
        val oldFileOnDisk = File(fileEntity.filePath)
        if (oldFileOnDisk.exists() && oldFileOnDisk.absolutePath != newFileOnDisk.absolutePath) {
            val content = oldFileOnDisk.readText()
            newFileOnDisk.writeText(content)
        }

        val updatedEntity = fileEntity.copy(
            fileName = newFileName,
            filePath = newFileOnDisk.absolutePath,
            language = detectLanguage(newFileName),
            updatedAt = System.currentTimeMillis()
        )
        fileDao.updateFile(updatedEntity)
        updatedEntity
    }

    /**
     * Initializes or creates a file record in DB and local storage.
     */
    suspend fun getOrCreateFile(fileName: String, initialContent: String = ""): FileEntity = withContext(Dispatchers.IO) {
        var fileEntity = fileDao.getFileByName(fileName)
        if (fileEntity == null) {
            val localFile = File(context.filesDir, fileName)
            if (!localFile.exists()) {
                localFile.createNewFile()
                if (initialContent.isNotEmpty()) {
                    localFile.writeText(initialContent)
                }
            }
            val newFile = FileEntity(
                fileName = fileName,
                filePath = localFile.absolutePath,
                language = detectLanguage(fileName),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val fileId = fileDao.insertFile(newFile)
            fileEntity = newFile.copy(fileId = fileId)

            // Save Base Version (V1)
            versionDao.insertVersion(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = 1,
                    versionName = "Initial Version",
                    createdAt = System.currentTimeMillis(),
                    isBase = true,
                    patchContent = initialContent
                )
            )
        }

        // Add to recent files
        recentFileDao.insertRecent(
            RecentFileEntity(
                fileName = fileEntity.fileName,
                filePath = fileEntity.filePath,
                lastOpenedAt = System.currentTimeMillis()
            )
        )

        fileEntity
    }

    /**
     * Saves a new incremental version delta for a file.
     * Stores ONLY the Unified Diff delta relative to previous version.
     */
    suspend fun saveVersion(fileId: Long, currentContent: String, versionName: String = ""): VersionEntity = withContext(Dispatchers.IO) {
        val versions = versionDao.getVersionsForFile(fileId)
        val latestVersion = versions.lastOrNull()

        // FIX: If V1 exists but its base content is empty, update V1 with initial code
        if (versions.size == 1 && latestVersion?.isBase == true && latestVersion.patchContent.isEmpty()) {
            val updatedV1 = latestVersion.copy(
                patchContent = currentContent,
                versionName = versionName.ifBlank { "Initial Version" },
                createdAt = System.currentTimeMillis()
            )
            versionDao.updateVersion(updatedV1)
            clearRecoveryDraft(fileId)
            return@withContext updatedV1
        }

        val nextVersionNum = (latestVersion?.versionNumber ?: 0) + 1
        val vName = versionName.ifBlank { "Version $nextVersionNum" }

        val previousText = if (latestVersion != null) {
            reconstructVersion(fileId, latestVersion.versionNumber)
        } else {
            ""
        }

        // Generate delta patch relative to previous version
        val patchString = DiffManager.createPatch(previousText, currentContent)

        val newVersion = VersionEntity(
            fileId = fileId,
            versionNumber = nextVersionNum,
            versionName = vName,
            createdAt = System.currentTimeMillis(),
            parentVersionId = latestVersion?.versionId ?: 0,
            isBase = versions.isEmpty(), // First version is Base, rest are deltas
            patchContent = if (versions.isEmpty()) currentContent else patchString
        )

        val versionId = versionDao.insertVersion(newVersion)

        // Update file timestamp
        fileDao.getFileById(fileId)?.let {
            fileDao.updateFile(it.copy(updatedAt = System.currentTimeMillis()))
        }

        // Clean up recovery draft on explicit save
        clearRecoveryDraft(fileId)

        newVersion.copy(versionId = versionId)
    }

    /**
     * Reconstructs file content for any version by walking the delta chain:
     * V1 (Base) -> V2 (Delta) -> ... -> Target Version.
     */
    suspend fun reconstructVersion(fileId: Long, targetVersionNumber: Int): String = withContext(Dispatchers.IO) {
        val versions = versionDao.getVersionsForFile(fileId).filter { it.versionNumber <= targetVersionNumber }
        if (versions.isEmpty()) return@withContext ""

        var currentText = ""
        for (ver in versions) {
            currentText = if (ver.isBase) {
                ver.patchContent
            } else {
                DiffManager.applyPatch(currentText, ver.patchContent)
            }
        }
        currentText
    }

    /**
     * Retrieves version history for a file.
     */
    suspend fun getVersionHistory(fileId: Long): List<VersionEntity> = withContext(Dispatchers.IO) {
        versionDao.getVersionsForFile(fileId)
    }

    /**
     * Saves content to physical disk file.
     */
    suspend fun writeToDisk(filePath: String, content: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) file.createNewFile()
        file.writeText(content)
    }

    /**
     * Reads content from physical disk file.
     */
    suspend fun readFromDisk(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.readText() else ""
    }

    /**
     * Crash Recovery: Saves recovery draft to DB table.
     */
    suspend fun saveRecoveryDraft(fileId: Long, content: String) = withContext(Dispatchers.IO) {
        recoveryDao.saveRecovery(
            RecoveryEntity(fileId = fileId, content = content, updatedAt = System.currentTimeMillis())
        )
    }

    /**
     * Crash Recovery: Reads recovery draft from DB table.
     */
    suspend fun getRecoveryDraft(fileId: Long): RecoveryEntity? = withContext(Dispatchers.IO) {
        recoveryDao.getRecovery(fileId)
    }

    /**
     * Crash Recovery: Clears obsolete recovery draft.
     */
    suspend fun clearRecoveryDraft(fileId: Long) = withContext(Dispatchers.IO) {
        recoveryDao.deleteRecovery(fileId)
    }

    /**
     * Recent Files list.
     */
    suspend fun getRecentFiles(): List<RecentFileEntity> = withContext(Dispatchers.IO) {
        recentFileDao.getRecentFiles()
    }

    /**
     * Load a file by its absolute path (used by Recent Files list).
     */
    suspend fun loadFileByPath(filePath: String): Pair<FileEntity, String> = withContext(Dispatchers.IO) {
        val fileEntity = fileDao.getFileByPath(filePath)
            ?: throw IllegalArgumentException("File not found for path: $filePath")
        val content = readFromDisk(filePath)
        recentFileDao.insertRecent(
            RecentFileEntity(
                fileName = fileEntity.fileName,
                filePath = fileEntity.filePath,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
        Pair(fileEntity, content)
    }

    private fun detectLanguage(fileName: String): String {
        return when {
            fileName.endsWith(".kt", ignoreCase = true) -> "kotlin"
            fileName.endsWith(".java", ignoreCase = true) -> "java"
            fileName.endsWith(".py", ignoreCase = true) -> "python"
            fileName.endsWith(".md", ignoreCase = true) -> "markdown"
            else -> "text"
        }
    }
}
