package com.example.codeeditor


import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue

class TextEditorState(initialText: TextFieldValue = TextFieldValue("")) {
    // Current text state
    var textField = mutableStateOf(initialText)
        private set

    private val undoStack = ArrayDeque<TextFieldValue>() // history for undo
    private val redoStack = ArrayDeque<TextFieldValue>() // history for redo
    private var lastCommittedText = initialText

    // Update text when user types
    fun onTextChange(newValue: TextFieldValue) {
        textField.value = newValue
    }

    // Save current change to history
    fun commitChange() {
        if (textField.value.text != lastCommittedText.text) {
            undoStack.addLast(lastCommittedText)
            redoStack.clear()
            lastCommittedText = textField.value
        }
    }

    // Undo last change
    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(textField.value)
            textField.value = undoStack.removeLast()
            lastCommittedText = textField.value
        }
    }

    // Redo undone change
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(textField.value)
            textField.value = redoStack.removeLast()
            lastCommittedText = textField.value
        }
    }

    // Replace first match
    fun replace(find: String, replace: String) {
        val text = textField.value.text
        val index = text.indexOf(find, ignoreCase = true)
        if (index >= 0) {
            val newText = text.replaceFirst(find, replace, ignoreCase = true)
            onTextChange(TextFieldValue(newText))
            commitChange()
        }
    }

    // Replace all matches
    fun replaceAll(find: String, replace: String) {
        val text = textField.value.text
        if (text.contains(find, ignoreCase = true)) {
            val newText = text.replace(find, replace, ignoreCase = true)
            onTextChange(TextFieldValue(newText))
            commitChange()
        }
    }
}

