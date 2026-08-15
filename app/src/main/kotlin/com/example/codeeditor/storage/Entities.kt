package com.example.codeeditor.storage

import androidx.room.*

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val fileId: Long = 0,
    val fileName: String,
    val filePath: String,
    val encoding: String = "UTF-8",
    val language: String = "kotlin",
    val isReadOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "file_versions",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["fileId"])]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val versionId: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val versionName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val parentVersionId: Long = 0,
    val isBase: Boolean, // True if full base content, false if unified diff patch
    val patchContent: String, // Full text if isBase, else Unified Diff format string
    val checksum: String = ""
)

@Entity(tableName = "recovery_drafts")
data class RecoveryEntity(
    @PrimaryKey val fileId: Long,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
