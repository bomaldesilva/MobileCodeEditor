package com.example.codeeditor.viewmodel

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.codeeditor.TextEditorState
import com.example.codeeditor.repository.EditorRepository
import com.example.codeeditor.storage.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val activeFile: FileEntity? = null,
    val displayFileName: String = "Untitled.kt",
    val editorText: TextFieldValue = TextFieldValue(""),
    val currentLanguage: String = "kotlin",
    val isReadOnly: Boolean = false,
    val isWordWrapEnabled: Boolean = true,
    val isMarkdownPreview: Boolean = false,
    val versionHistory: List<VersionEntity> = emptyList(),
    val recentFiles: List<RecentFileEntity> = emptyList(),
    val diffComparisonLines: List<DiffLine> = emptyList(),
    val selectedVersionForDiff: VersionEntity? = null,
    val showVersionHistoryDialog: Boolean = false,
    val showDiffDialog: Boolean = false,
    val showRecoveryPrompt: Boolean = false,
    val recoveryDraftText: String? = null,
    val statusMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EditorRepository(application)
    val textEditorState = TextEditorState()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadOrCreateFile("Untitled.kt")
    }

    /**
     * FIX: Rename active file. Does NOT call onTextChange or touch textEditorState
     * to prevent the debounce collector in MainActivity from overwriting state.
     */
    fun renameActiveFile(newFileName: String) {
        viewModelScope.launch {
            try {
                val active = _uiState.value.activeFile
                    ?: repository.getOrCreateFile("Untitled.kt")
                val updatedFile = repository.renameFile(active.fileId, newFileName)
                val recent = repository.getRecentFiles()
                _uiState.update {
                    it.copy(
                        activeFile = updatedFile,
                        displayFileName = updatedFile.fileName,
                        currentLanguage = updatedFile.language,
                        recentFiles = recent,
                        statusMessage = "Renamed to ${updatedFile.fileName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Rename Error: ${e.message}") }
            }
        }
    }

    /**
     * Changes language by renaming extension only. Does NOT re-load or clear editor content.
     */
    fun setLanguage(language: String) {
        val currentName = _uiState.value.displayFileName
        val dotIdx = currentName.lastIndexOf('.')
        val baseName = if (dotIdx != -1) currentName.substring(0, dotIdx) else currentName
        val newExt = when (language.lowercase()) {
            "java"     -> ".java"
            "python"   -> ".py"
            "markdown" -> ".md"
            else       -> ".kt"
        }
        renameActiveFile("$baseName$newExt")
    }

    /**
     * FIX: Create a fresh new unique file. Always starts with empty content.
     * Ensures a totally blank editor state.
     */
    fun createNewFile(baseName: String = "Untitled.kt") {
        viewModelScope.launch {
            try {
                val newFile = repository.createNewUniqueFile(baseName)
                val versions = repository.getVersionHistory(newFile.fileId)
                val recent = repository.getRecentFiles()

                // Update textEditorState FIRST, then update UI state
                val emptyValue = TextFieldValue("")
                textEditorState.onTextChange(emptyValue)

                _uiState.update {
                    it.copy(
                        activeFile = newFile,
                        displayFileName = newFile.fileName,
                        editorText = emptyValue,
                        currentLanguage = newFile.language,
                        isReadOnly = false,
                        versionHistory = versions,
                        recentFiles = recent,
                        showRecoveryPrompt = false,
                        statusMessage = "New file: ${newFile.fileName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Error creating file: ${e.message}") }
            }
        }
    }

    /**
     * FIX: Open an externally picked file. ALWAYS uses the passed content directly,
     * never re-reads from disk. This ensures the picked file's content is shown.
     */
    fun openExternalFileContent(fileName: String, content: String) {
        viewModelScope.launch {
            try {
                // Force-create or update the file record and write content to disk
                val fileEntity = repository.upsertFile(fileName, content)
                val versions = repository.getVersionHistory(fileEntity.fileId)
                val recent = repository.getRecentFiles()

                val tfv = TextFieldValue(content)
                textEditorState.onTextChange(tfv)

                _uiState.update {
                    it.copy(
                        activeFile = fileEntity,
                        displayFileName = fileEntity.fileName,
                        editorText = tfv,
                        currentLanguage = fileEntity.language,
                        isReadOnly = fileEntity.isReadOnly,
                        versionHistory = versions,
                        recentFiles = recent,
                        showRecoveryPrompt = false,
                        statusMessage = "Opened ${fileEntity.fileName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Error opening file: ${e.message}") }
            }
        }
    }

    /**
     * Load an existing file from recent files list by fileName.
     * Reads the actual content from disk.
     */
    fun loadOrCreateFile(fileName: String, initialContent: String = "") {
        viewModelScope.launch {
            try {
                val fileEntity = repository.getOrCreateFile(fileName, initialContent)
                val content = repository.readFromDisk(fileEntity.filePath)
                val versions = repository.getVersionHistory(fileEntity.fileId)
                val recent = repository.getRecentFiles()

                val tfv = TextFieldValue(content)
                textEditorState.onTextChange(tfv)

                _uiState.update {
                    it.copy(
                        activeFile = fileEntity,
                        displayFileName = fileEntity.fileName,
                        editorText = tfv,
                        currentLanguage = fileEntity.language,
                        isReadOnly = fileEntity.isReadOnly,
                        versionHistory = versions,
                        recentFiles = recent,
                        showRecoveryPrompt = false,
                        statusMessage = "Loaded ${fileEntity.fileName}"
                    )
                }

                checkRecoveryDraft(fileEntity.fileId)
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Error loading file: ${e.message}") }
            }
        }
    }

    fun loadFileByPath(filePath: String) {
        viewModelScope.launch {
            try {
                val (fileEntity, content) = repository.loadFileByPath(filePath)
                val versions = repository.getVersionHistory(fileEntity.fileId)
                val recent = repository.getRecentFiles()

                val tfv = TextFieldValue(content)
                textEditorState.onTextChange(tfv)

                _uiState.update {
                    it.copy(
                        activeFile = fileEntity,
                        displayFileName = fileEntity.fileName,
                        editorText = tfv,
                        currentLanguage = fileEntity.language,
                        isReadOnly = fileEntity.isReadOnly,
                        versionHistory = versions,
                        recentFiles = recent,
                        showRecoveryPrompt = false,
                        statusMessage = "Opened ${fileEntity.fileName}"
                    )
                }
                checkRecoveryDraft(fileEntity.fileId)
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Error opening: ${e.message}") }
            }
        }
    }

    fun onTextChange(newValue: TextFieldValue) {
        if (_uiState.value.isReadOnly) return
        textEditorState.onTextChange(newValue)
        // Commit to undo stack so Undo/Redo work during typing
        textEditorState.commitChange()
        _uiState.update { it.copy(editorText = newValue) }
    }

    /**
     * Explicit Save: Saves content to disk and clears recovery draft.
     */
    fun saveFile() {
        val active = _uiState.value.activeFile ?: return
        val text = _uiState.value.editorText.text
        viewModelScope.launch {
            try {
                repository.writeToDisk(active.filePath, text)
                repository.clearRecoveryDraft(active.fileId)
                _uiState.update { it.copy(statusMessage = "Saved ${active.fileName}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Save Error: ${e.message}") }
            }
        }
    }

    /**
     * Incremental Version Snapshot: Saves delta patch in Room DB.
     */
    fun createVersionSnapshot(versionName: String = "") {
        val active = _uiState.value.activeFile ?: return
        val currentText = _uiState.value.editorText.text
        viewModelScope.launch {
            try {
                val newVer = repository.saveVersion(active.fileId, currentText, versionName)
                val updatedVersions = repository.getVersionHistory(active.fileId)
                _uiState.update {
                    it.copy(
                        versionHistory = updatedVersions,
                        statusMessage = "Snapshot: ${newVer.versionName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Version error: ${e.message}") }
            }
        }
    }

    /**
     * Reconstructs text from version chain and updates editor (Rollback/Restore).
     */
    fun restoreVersion(versionNumber: Int) {
        val active = _uiState.value.activeFile ?: return
        viewModelScope.launch {
            try {
                val reconstructed = repository.reconstructVersion(active.fileId, versionNumber)
                val tfv = TextFieldValue(reconstructed)
                textEditorState.onTextChange(tfv)
                _uiState.update {
                    it.copy(
                        editorText = tfv,
                        showVersionHistoryDialog = false,
                        statusMessage = "Restored to V$versionNumber"
                    )
                }
                createVersionSnapshot("Restored from V$versionNumber")
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Restore error: ${e.message}") }
            }
        }
    }

    /**
     * Compares active editor text or version A against version B.
     */
    fun compareVersions(versionA: Int, versionB: Int) {
        val active = _uiState.value.activeFile ?: return
        viewModelScope.launch {
            try {
                val textA = repository.reconstructVersion(active.fileId, versionA)
                val textB = repository.reconstructVersion(active.fileId, versionB)
                val diffLines = DiffManager.generateLineDiff(textA, textB)
                _uiState.update {
                    it.copy(diffComparisonLines = diffLines, showDiffDialog = true)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Diff error: ${e.message}") }
            }
        }
    }

    /**
     * Crash Recovery: Auto-saves current editor draft every 10 seconds.
     */
    fun autoSaveRecoveryDraft() {
        val active = _uiState.value.activeFile ?: return
        val text = _uiState.value.editorText.text
        if (text.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.saveRecoveryDraft(active.fileId, text)
                } catch (e: Exception) {
                    // Silent
                }
            }
        }
    }

    private fun checkRecoveryDraft(fileId: Long) {
        viewModelScope.launch {
            val draft = repository.getRecoveryDraft(fileId)
            if (draft != null && draft.content != _uiState.value.editorText.text) {
                _uiState.update {
                    it.copy(showRecoveryPrompt = true, recoveryDraftText = draft.content)
                }
            }
        }
    }

    fun acceptRecoveryDraft() {
        val draftText = _uiState.value.recoveryDraftText ?: return
        val tfv = TextFieldValue(draftText)
        textEditorState.onTextChange(tfv)
        _uiState.update {
            it.copy(
                editorText = tfv,
                showRecoveryPrompt = false,
                statusMessage = "Restored unsaved draft"
            )
        }
    }

    fun dismissRecoveryDraft() {
        val active = _uiState.value.activeFile ?: return
        viewModelScope.launch {
            repository.clearRecoveryDraft(active.fileId)
            _uiState.update { it.copy(showRecoveryPrompt = false) }
        }
    }

    fun toggleReadOnly() {
        _uiState.update { it.copy(isReadOnly = !it.isReadOnly) }
    }

    fun toggleWordWrap() {
        _uiState.update { it.copy(isWordWrapEnabled = !it.isWordWrapEnabled) }
    }

    fun toggleMarkdownPreview() {
        _uiState.update { it.copy(isMarkdownPreview = !it.isMarkdownPreview) }
    }

    fun toggleVersionHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showVersionHistoryDialog = show) }
    }

    fun dismissDiffDialog() {
        _uiState.update { it.copy(showDiffDialog = false) }
    }
}
