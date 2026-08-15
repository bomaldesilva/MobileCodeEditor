package com.example.codeeditor.storage

import android.content.Context
import androidx.room.*

@Entity(tableName = "file_versions")
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val versionName: String,
    val timestamp: Long,
    val isBase: Boolean, // True if this is the full original file content
    val content: String // Either the full content (if isBase) or a Diff Patch string
)

@Dao
interface VersionDao {
    @Insert
    suspend fun insertVersion(version: VersionEntity)

    @Query("SELECT * FROM file_versions WHERE fileName = :fileName ORDER BY timestamp ASC")
    suspend fun getVersionsForFile(fileName: String): List<VersionEntity>

    @Query("SELECT * FROM file_versions WHERE fileName = :fileName AND isBase = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBaseForFile(fileName: String): VersionEntity?

    @Query("DELETE FROM file_versions WHERE fileName = :fileName")
    suspend fun deleteVersionsForFile(fileName: String)
}

@Database(entities = [VersionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun versionDao(): VersionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "code_editor_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
