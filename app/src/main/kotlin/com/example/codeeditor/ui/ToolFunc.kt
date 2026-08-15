package com.example.codeeditor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.TextRange

// Cuts the selected text, copies it to clipboard, and removes it from the edito
fun cutText(
    editorText: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    clipboardManager: ClipboardManager
) {
    val start = editorText.selection.start
    val end = editorText.selection.end

    if (start == end) return // nothing selected

    try {
        val selectedText = editorText.text.substring(start, end)
        clipboardManager.setText(AnnotatedString(selectedText))

        val newText = editorText.text.removeRange(start, end)
        onTextChange(
            editorText.copy(text = newText, selection = TextRange(start))
        )
    } catch (e: Exception) {
        // Log or notify user
        println("Cut failed: ${e.message}")
    }
}

// Copies the selected text to clipboard without removing it
fun copyText(
    editorText: TextFieldValue,
    clipboardManager: ClipboardManager
) {
    val start = editorText.selection.start
    val end = editorText.selection.end

    if (start == end) return // nothing selected

    try {
        val selectedText = editorText.text.substring(start, end)
        clipboardManager.setText(AnnotatedString(selectedText))
    } catch (e: Exception) {
        println("Copy failed: ${e.message}")
    }
}

// Pastes clipboard text into the editor at the current cursor/selection
fun pasteText(
    editorText: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    clipboardManager: ClipboardManager
) {
    val clipText = clipboardManager.getText()?.text
    if (clipText.isNullOrEmpty()) {
        println("Clipboard empty, nothing to paste")
        return
    }

    val start = editorText.selection.start
    val end = editorText.selection.end

    try {
        val newText = editorText.text.replaceRange(start, end, clipText)
        onTextChange(
            editorText.copy(
                text = newText,
                selection = TextRange(start + clipText.length)
            )
        )
    } catch (e: Exception) {
        println("Paste failed: ${e.message}")
    }
}
