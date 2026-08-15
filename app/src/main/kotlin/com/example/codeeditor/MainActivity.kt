package com.example.codeeditor

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codeeditor.ui.*
import com.example.codeeditor.ui.theme.CodeEditorTheme
import com.example.codeeditor.viewmodel.EditorViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val scope = rememberCoroutineScope()
            val viewModel: EditorViewModel = viewModel()

            val uiState by viewModel.uiState.collectAsState()

            // FIX: Derive syntaxRules reactively from uiState.currentLanguage
            // so it always updates when a file is opened, renamed, or language is switched.
            val syntaxRules by remember(uiState.currentLanguage) {
                mutableStateOf(
                    loadSyntaxRules(
                        context,
                        when (uiState.currentLanguage) {
                            "java"     -> "java.json"
                            "python"   -> "python.json"
                            "markdown" -> "markdown.json"
                            else       -> "kotlin.json"
                        }
                    )
                )
            }

            var showMiniToolbar by remember { mutableStateOf(false) }
            var showFindReplace by remember { mutableStateOf(false) }
            var showRenameDialog by remember { mutableStateOf(false) }
            var newFileNameInput by remember { mutableStateOf("") }
            var showCompileDialog by remember { mutableStateOf(false) }
            var compileOutput by remember { mutableStateOf("") }

            val drawerState = rememberDrawerState(DrawerValue.Closed)

            // ----------------------
            // Helper: resolve filename from URI
            // ----------------------
            fun getFileNameFromUri(uri: Uri): String {
                var name = "Untitled.kt"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index != -1) name = cursor.getString(index)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error resolving filename", e)
                }
                return name
            }

            // ----------------------
            // External Open Launcher
            // ----------------------
            val openExternalFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        // FIX: Read content directly from the URI stream, then pass to ViewModel
                        val content = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()?.use { it.readText() }.orEmpty()
                        val name = getFileNameFromUri(uri)
                        viewModel.openExternalFileContent(name, content)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // ----------------------
            // External Save Launcher
            // ----------------------
            val saveExternalFile = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("*/*")
            ) { uri ->
                if (uri != null) {
                    try {
                        val textToSave = uiState.editorText.text
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(textToSave.toByteArray(Charsets.UTF_8))
                        }
                        Toast.makeText(context, "Saved to External Storage", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // ----------------------
            // FIX: Sync text changes - use LaunchedEffect key on the text state
            // to avoid circular state loop that was overwriting displayFileName
            // ----------------------
            LaunchedEffect(Unit) {
                snapshotFlow { viewModel.textEditorState.textField.value }
                    .debounce(300)
                    .collect { viewModel.onTextChange(it) }
            }

            // ----------------------
            // 10-Second Auto Recovery Draft Timer
            // ----------------------
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(10000)
                    viewModel.autoSaveRecoveryDraft()
                }
            }

            // Status message toast observer
            LaunchedEffect(uiState.statusMessage) {
                uiState.statusMessage?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            // ----------------------
            // UI Render
            // ----------------------
            CodeEditorTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DrawerContent(
                                currentLanguage = uiState.currentLanguage,
                                onNewFile = {
                                    viewModel.createNewFile()
                                    scope.launch { drawerState.close() }
                                },
                                onOpenFile = {
                                    openExternalFile.launch(arrayOf("*/*"))
                                    scope.launch { drawerState.close() }
                                },
                                onSaveFile = {
                                    viewModel.saveFile()
                                    scope.launch { drawerState.close() }
                                },
                                onSaveAsFile = {
                                    val currentName = uiState.displayFileName
                                    saveExternalFile.launch(currentName)
                                    scope.launch { drawerState.close() }
                                },
                                onOpenVersionHistory = {
                                    viewModel.toggleVersionHistoryDialog(true)
                                    scope.launch { drawerState.close() }
                                },
                                recentFiles = uiState.recentFiles,
                                onSelectRecentFile = { filePath ->
                                    // FIX: load by filePath so correct file content is read
                                    viewModel.loadFileByPath(filePath)
                                    scope.launch { drawerState.close() }
                                },
                                onLanguageChange = { lang ->
                                    viewModel.setLanguage(lang)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            newFileNameInput = uiState.displayFileName
                                            showRenameDialog = true
                                        }
                                    ) {
                                        Text(
                                            text = uiState.displayFileName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename File",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        if (uiState.isReadOnly) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = "READ-ONLY",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    // Format Code Button (Kotlin only)
                                    if (uiState.currentLanguage == "kotlin") {
                                        IconButton(onClick = {
                                            val formatted = CodeFormatter.formatKotlinCode(
                                                viewModel.textEditorState.textField.value.text
                                            )
                                            viewModel.onTextChange(TextFieldValue(formatted))
                                            Toast.makeText(context, "Code Formatted", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Format Code")
                                        }
                                    }
                                    // Word Wrap Toggle
                                    IconButton(onClick = { viewModel.toggleWordWrap() }) {
                                        Icon(
                                            imageVector = if (uiState.isWordWrapEnabled)
                                                Icons.Default.WrapText else Icons.Default.Subject,
                                            contentDescription = "Word Wrap Toggle"
                                        )
                                    }
                                    // Version History Quick Action
                                    IconButton(onClick = { viewModel.toggleVersionHistoryDialog(true) }) {
                                        Icon(Icons.Default.History, contentDescription = "Version History")
                                    }
                                    // Read Only Toggle
                                    IconButton(onClick = { viewModel.toggleReadOnly() }) {
                                        Icon(
                                            imageVector = if (uiState.isReadOnly)
                                                Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Read Only Toggle"
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            ModernBottomBar(
                                editorState = viewModel.textEditorState,
                                showFindReplace = showFindReplace,
                                onToggleFindReplace = { showFindReplace = it },
                                currentFileName = uiState.displayFileName,
                                currentLanguage = uiState.currentLanguage,
                                fileManager = FileManager(context),
                                onCompileOutput = { output ->
                                    compileOutput = output
                                    showCompileDialog = true
                                },
                                clipboardManager = clipboardManager,
                                isMarkdown = uiState.currentLanguage == "markdown",
                                showPreview = uiState.isMarkdownPreview,
                                onTogglePreview = { viewModel.toggleMarkdownPreview() }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {

                            if (showFindReplace) {
                                FindReplaceBar(viewModel.textEditorState) { showFindReplace = false }
                            }

                            if (showMiniToolbar) {
                                MiniToolbar(
                                    onCut = {
                                        cutText(
                                            viewModel.textEditorState.textField.value,
                                            { viewModel.onTextChange(it) },
                                            clipboardManager
                                        )
                                    },
                                    onCopy = {
                                        copyText(viewModel.textEditorState.textField.value, clipboardManager)
                                    },
                                    onPaste = {
                                        pasteText(
                                            viewModel.textEditorState.textField.value,
                                            { viewModel.onTextChange(it) },
                                            clipboardManager
                                        )
                                    }
                                )
                            }

                            // Editor Area or Formatted Markdown Preview
                            if (uiState.isMarkdownPreview && uiState.currentLanguage == "markdown") {
                                MarkdownPreview(
                                    content = viewModel.textEditorState.textField.value.text,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                CodeEditor(
                                    modifier = Modifier.weight(1f),
                                    editorState = viewModel.textEditorState,
                                    syntaxRules = syntaxRules,
                                    isReadOnly = uiState.isReadOnly,
                                    isWordWrapEnabled = uiState.isWordWrapEnabled
                                )
                            }
                        }
                    }

                    // ---------------------------
                    // Dialogs & Prompts
                    // ---------------------------
                    if (uiState.showVersionHistoryDialog) {
                        VersionHistoryDialog(
                            versionHistory = uiState.versionHistory,
                            onCreateSnapshot = { versionName ->
                                viewModel.createVersionSnapshot(versionName)
                            },
                            onCompareVersion = { verA, verB ->
                                viewModel.compareVersions(verA, verB)
                            },
                            onRestoreVersion = { versionNum ->
                                viewModel.restoreVersion(versionNum)
                            },
                            onDismiss = { viewModel.toggleVersionHistoryDialog(false) }
                        )
                    }

                    if (uiState.showDiffDialog) {
                        DiffViewerDialog(
                            diffLines = uiState.diffComparisonLines,
                            onDismiss = { viewModel.dismissDiffDialog() }
                        )
                    }

                    if (uiState.showRecoveryPrompt) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissRecoveryDraft() },
                            title = { Text("Unsaved Crash Recovery Found") },
                            text = { Text("An unsaved auto-recovery draft was detected for this file. Would you like to restore it?") },
                            confirmButton = {
                                Button(onClick = { viewModel.acceptRecoveryDraft() }) {
                                    Text("Restore Draft")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissRecoveryDraft() }) {
                                    Text("Discard")
                                }
                            }
                        )
                    }

                    if (showRenameDialog) {
                        AlertDialog(
                            onDismissRequest = { showRenameDialog = false },
                            title = { Text("Rename File") },
                            text = {
                                Column {
                                    Text(
                                        "Enter new file name with extension (e.g., Main.java, script.py, App.kt):",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newFileNameInput,
                                        onValueChange = { newFileNameInput = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val trimmed = newFileNameInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        viewModel.renameActiveFile(trimmed)
                                    }
                                    showRenameDialog = false
                                }) {
                                    Text("Rename")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRenameDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showCompileDialog) {
                        CompilerInterface(
                            clipboardManager = clipboardManager,
                            compileOutput = compileOutput,
                            onClose = { showCompileDialog = false }
                        )
                    }
                }
            }
        }
    }
}
