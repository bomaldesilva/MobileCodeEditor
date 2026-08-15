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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.example.codeeditor.ui.theme.CodeEditorTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val editorState = TextEditorState()
    private var currentFileName by mutableStateOf("Untitled.kt")

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val scope = rememberCoroutineScope()

            var syntaxRules by remember { mutableStateOf(loadSyntaxRules(context, "kotlin.json")) }
            var currentLanguage by remember { mutableStateOf("kotlin") }
            var showMiniToolbar by remember { mutableStateOf(false) }
            var showFindReplace by remember { mutableStateOf(false) }
            var showMarkdownPreview by remember { mutableStateOf(false) }
            var isReadOnly by remember { mutableStateOf(false) }
            var isWordWrapEnabled by remember { mutableStateOf(true) }

            // Compiler dialog states
            var showCompileDialog by remember { mutableStateOf(false) }
            var compileOutput by remember { mutableStateOf("") }

            val drawerState = rememberDrawerState(DrawerValue.Closed)

            // ----------------------
            // Helpers for file name
            // ----------------------
            fun getFileNameFromUri(uri: Uri): String {
                var name = "Untitled.kt"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) name = cursor.getString(index)
                    }
                }
                return name
            }

            // ----------------------
            // External Open
            // ----------------------
            val openExternalFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use {
                        val loaded = it?.readText().orEmpty()
                        editorState.textField.value = TextFieldValue(loaded)
                        currentFileName = getFileNameFromUri(uri)
                        Toast.makeText(context, "File Opened", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // ----------------------
            // External Save
            // ----------------------
            val saveExternalFile = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("*/*")
            ) { uri ->
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(editorState.textField.value.text.toByteArray())
                    }
                    currentFileName = getFileNameFromUri(uri)
                    Toast.makeText(context, "File Saved", Toast.LENGTH_SHORT).show()
                }
            }

            // ----------------------
            // Auto-save draft in memory (UI sync)
            // ----------------------
            LaunchedEffect(editorState.textField.value) {
                snapshotFlow { editorState.textField.value }
                    .debounce(500)
                    .collect { editorState.commitChange() }
            }

            // ----------------------
            // Crash Prevention: Periodic Auto-save to Disk (10 seconds)
            // ----------------------
            val fileManager = remember { FileManager(context) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(10000)
                    val content = editorState.textField.value.text
                    if (content.isNotEmpty()) {
                        fileManager.saveFile("recovery_draft.tmp", content)
                        Log.d("CrashPrevention", "Auto-saved to recovery_draft.tmp")
                    }
                }
            }

            // ----------------------
            // Restore from recovery on startup
            // ----------------------
            LaunchedEffect(Unit) {
                val recovered = fileManager.openFile("recovery_draft.tmp")
                if (recovered.isNotEmpty()) {
                    // In a real app, we might ask the user. Here we just notify.
                    Toast.makeText(context, "Recovery draft loaded", Toast.LENGTH_LONG).show()
                }
            }

            // ----------------------
            // UI
            // ----------------------
            CodeEditorTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNewFile = {
                                editorState.textField.value = TextFieldValue("")
                                currentFileName = "Untitled.kt"
                            },
                            onOpenFile = { openExternalFile.launch(arrayOf("*/*")) },
                            onSaveFile = {
                                val suggestedName =
                                    if (currentFileName.contains(".")) currentFileName else "$currentFileName.kt"
                                saveExternalFile.launch(suggestedName)
                            },
                            onLanguageChange = { languageFile ->
                                currentLanguage = languageFile.removeSuffix(".json")
                                syntaxRules = loadSyntaxRules(context, languageFile)
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Modern Editor - $currentFileName") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            ModernBottomBar(
                                editorState = editorState,
                                showFindReplace = showFindReplace,
                                onToggleFindReplace = { showFindReplace = it },
                                currentFileName = currentFileName,
                                currentLanguage = currentLanguage,
                                fileManager = fileManager,
                                onCompileOutput = { output ->
                                    compileOutput = output
                                    showCompileDialog = true
                                },
                                clipboardManager = clipboardManager,
                                isMarkdown = currentLanguage == "markdown",
                                showPreview = showMarkdownPreview,
                                onTogglePreview = { showMarkdownPreview = it }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {

                            if (showFindReplace) {
                                FindReplaceBar(editorState) { showFindReplace = false }
                            }

                            if (showMiniToolbar) {
                                MiniToolbar(
                                    onCut = {
                                        cutText(
                                            editorState.textField.value,
                                            { editorState.onTextChange(it) },
                                            clipboardManager
                                        )
                                    },
                                    onCopy = {
                                        copyText(editorState.textField.value, clipboardManager)
                                    },
                                    onPaste = {
                                        pasteText(
                                            editorState.textField.value,
                                            { editorState.onTextChange(it) },
                                            clipboardManager
                                        )
                                    }
                                )
                            }

                            // ----------------------
                            // Code Editor or Preview
                            // ----------------------
                            if (showMarkdownPreview && currentLanguage == "markdown") {
                                com.example.codeeditor.ui.MarkdownPreview(
                                    content = editorState.textField.value.text,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                CodeEditor(
                                    modifier = Modifier.weight(1f),
                                    editorState = editorState,
                                    syntaxRules = syntaxRules,
                                    isReadOnly = isReadOnly,
                                    isWordWrapEnabled = isWordWrapEnabled
                                )
                            }
                        }
                    }

                    // ---------------------------
                    // Show Compiler Dialog
                    // ---------------------------
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
