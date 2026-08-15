package com.example.codeeditor


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.codeeditor.network.CompilerClient

@Composable
fun ModernBottomBar(
    editorState: TextEditorState,
    showFindReplace: Boolean,
    onToggleFindReplace: (Boolean) -> Unit,
    currentFileName: String,
    currentLanguage: String,
    fileManager: FileManager,
    onCompileOutput: (String) -> Unit,
    clipboardManager: ClipboardManager,
    isMarkdown: Boolean,
    showPreview: Boolean,
    onTogglePreview: (Boolean) -> Unit
) {
    // Compute word and character count
    val textValue = editorState.textField.value.text
    val wordCount = textValue.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    val charCount = textValue.length

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Word & Character Count Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Words: $wordCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Chars: $charCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo
                ModernIconButton(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    onClick = { editorState.undo() }
                )

                // Redo
                ModernIconButton(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    onClick = { editorState.redo() }
                )

                // Cut
                ModernIconButton(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Cut",
                    onClick = {
                        val currentValue = editorState.textField.value
                        if (currentValue.hasSelection()) {
                            val selectedText = currentValue.getSelectedText()
                            clipboardManager.setText(AnnotatedString(selectedText))
                            // Remove selected text
                            val newText = currentValue.text.removeRange(
                                currentValue.selection.start,
                                currentValue.selection.end
                            )
                            editorState.textField.value = currentValue.copy(
                                text = newText,
                                selection = TextRange(currentValue.selection.start)
                            )
                        }
                    }
                )

                // Copy
                ModernIconButton(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    onClick = {
                        val currentValue = editorState.textField.value
                        if (currentValue.hasSelection()) {
                            val selectedText = currentValue.getSelectedText()
                            clipboardManager.setText(AnnotatedString(selectedText))
                        }
                    }
                )

                // Paste
                ModernIconButton(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    onClick = {
                        val clipboardText = clipboardManager.getText()?.text ?: ""
                        if (clipboardText.isNotEmpty()) {
                            val currentValue = editorState.textField.value
                            val newText = currentValue.text.substring(0, currentValue.selection.start) +
                                    clipboardText +
                                    currentValue.text.substring(currentValue.selection.end)
                            editorState.textField.value = currentValue.copy(
                                text = newText,
                                selection = TextRange(currentValue.selection.start + clipboardText.length)
                            )
                        }
                    }
                )

                // Find/Replace
                ModernIconButton(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Find",
                    onClick = { onToggleFindReplace(!showFindReplace) }
                )

                // Markdown Preview Toggle
                if (isMarkdown) {
                    ModernIconButton(
                        imageVector = if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Preview",
                        onClick = { onTogglePreview(!showPreview) }
                    )
                }

                // Compile
                var isCompiling by remember { mutableStateOf(false) }

                Box(contentAlignment = Alignment.Center) {
                    ModernIconButton(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Compile",
                        onClick = {
                            if (isCompiling) return@ModernIconButton
                            isCompiling = true

                            val compiler = CompilerClient()
                            val code = editorState.textField.value.text

                            // Launch coroutine to compile
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    // Optional: save file locally
                                    compiler.saveCodeLocally(fileManager, currentFileName, code)

                                    // Compile code on server
                                    val result = compiler.compile(code, currentLanguage)

                                    // Combine stdout, stderr, compile_output for full visibility
                                    val output = buildString {
                                        if (result.error != null) {
                                            append(result.error)
                                        } else {
                                            if (!result.stdout.isNullOrEmpty()) {
                                                append(result.stdout)
                                            }
                                            if (!result.stderr.isNullOrEmpty()) {
                                                if (isNotEmpty()) append("\n--- Standard Error ---\n")
                                                append(result.stderr)
                                            }
                                            if (!result.compile_output.isNullOrEmpty()) {
                                                if (isNotEmpty()) append("\n--- Compiler Log ---\n")
                                                append(result.compile_output)
                                            }
                                            if (result.stdout.isNullOrEmpty() && result.stderr.isNullOrEmpty() && result.compile_output.isNullOrEmpty()) {
                                                append("Execution finished (no output).")
                                            }
                                            if (result.time != null || result.memory != null) {
                                                append("\n\n-------------------------------")
                                                append("\nProcess finished with exit code ${result.status?.id ?: 0}")
                                                if (result.memory != null) append("\nMemory: ${result.memory / 1024} KB")
                                                if (result.time != null) append("\nCPU Time: ${result.time}s")
                                            }
                                        }
                                    }

                                    // Send output to screen
                                    onCompileOutput(output)
                                } catch (e: Exception) {
                                    onCompileOutput("App Error: ${e.localizedMessage ?: "Unknown error occurred"}")
                                } finally {
                                    isCompiling = false
                                }
                            }
                        }
                    )

                    if (isCompiling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun ModernIconButton(
    contentDescription: String,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
    ) {
        if (painter != null) {
            Icon(painter = painter, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary)
        } else if (imageVector != null) {
            Icon(imageVector = imageVector, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// Helper extension function to get selected text
fun TextFieldValue.getSelectedText(): String {
    return if (selection.collapsed) {
        ""
    } else {
        text.substring(selection.start, selection.end)
    }
}

// Helper extension function to check if text is selected
fun TextFieldValue.hasSelection(): Boolean {
    return !selection.collapsed
}
