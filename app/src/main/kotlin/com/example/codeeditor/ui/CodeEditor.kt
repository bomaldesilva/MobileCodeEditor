package com.example.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -------------------------
// Code Editor Composable
// -------------------------
@Composable
fun CodeEditor(
    modifier: Modifier,
    editorState: TextEditorState,
    syntaxRules: SyntaxRules,
    isReadOnly: Boolean = false,
    isWordWrapEnabled: Boolean = true
) {
    val scrollState = rememberScrollState()
    val editorText = editorState.textField.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (!isWordWrapEnabled) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        Row {
            // Line numbers
            val lines = editorText.text.lines().ifEmpty { listOf("") }
            Column(modifier = Modifier.width(50.dp).padding(end = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                lines.forEachIndexed { i, _ ->
                    Text(
                        text = "${i + 1}.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Highlighted editor
            BasicTextField(
                value = editorText,
                onValueChange = { if (!isReadOnly) editorState.onTextChange(it) },
                readOnly = isReadOnly,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        // Highlighted text
                        Text(
                            text = highlightSyntax(editorText.text, syntaxRules),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // Editable overlay
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
