package com.example.codeeditor.storage

import androidx.room.*

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("SELECT * FROM files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: Long): FileEntity?

    @Query("SELECT * FROM files WHERE fileName = :fileName LIMIT 1")
    suspend fun getFileByName(fileName: String): FileEntity?

    @Query("SELECT * FROM files WHERE filePath = :filePath LIMIT 1")
    suspend fun getFileByPath(filePath: String): FileEntity?

    @Query("SELECT * FROM files ORDER BY updatedAt DESC")
    suspend fun getAllFiles(): List<FileEntity>

    @Delete
    suspend fun deleteFile(file: FileEntity)
}

@Dao
interface VersionDao {
    @Insert
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM file_versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getVersionsForFile(fileId: Long): List<VersionEntity>

    @Query("SELECT * FROM file_versions WHERE fileId = :fileId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersionForFile(fileId: Long): VersionEntity?

    @Query("SELECT * FROM file_versions WHERE fileId = :fileId AND isBase = 1 ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestBaseVersionForFile(fileId: Long): VersionEntity?

    @Query("DELETE FROM file_versions WHERE fileId = :fileId")
    suspend fun deleteVersionsForFile(fileId: Long)
}

@Dao
interface RecoveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecovery(recovery: RecoveryEntity)

    @Query("SELECT * FROM recovery_drafts WHERE fileId = :fileId")
    suspend fun getRecovery(fileId: Long): RecoveryEntity?

    @Query("DELETE FROM recovery_drafts WHERE fileId = :fileId")
    suspend fun deleteRecovery(fileId: Long)
}

@Dao
interface RecentFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentFileEntity)

    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC LIMIT 10")
    suspend fun getRecentFiles(): List<RecentFileEntity>
}
