package com.example.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CodeEditor Composable
 *
 * Supports lag-free syntax highlighting, full touch selection/long-press word selection,
 * cursor navigation, line numbers, word wrap toggle, and read-only mode.
 */
@Composable
fun CodeEditor(
    modifier: Modifier,
    editorState: TextEditorState,
    syntaxRules: SyntaxRules,
    isReadOnly: Boolean = false,
    isWordWrapEnabled: Boolean = true
) {
    val scrollState = rememberScrollState()
    val rawValue = editorState.textField.value

    // FIX: Key remember on both rawValue.text and rawValue.selection
    // so selection handles update dynamically on long-press or touch drag
    val highlightedValue = remember(rawValue.text, rawValue.selection, syntaxRules) {
        TextFieldValue(
            annotatedString = highlightSyntax(rawValue.text, syntaxRules),
            selection = rawValue.selection,
            composition = rawValue.composition
        )
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (!isWordWrapEnabled) Modifier.horizontalScroll(rememberScrollState())
                else Modifier
            )
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // Line numbers column
        val lines = rawValue.text.lines().ifEmpty { listOf("") }
        Column(
            modifier = Modifier
                .width(40.dp)
                .padding(end = 4.dp)
        ) {
            lines.forEachIndexed { i, _ ->
                Text(
                    text = "${i + 1}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        // BasicTextField supporting touch selection and typing
        BasicTextField(
            value = highlightedValue,
            onValueChange = { newVal ->
                if (isReadOnly) {
                    // In read-only mode, allow cursor & selection movement, but keep text unchanged
                    editorState.onTextChange(
                        rawValue.copy(
                            selection = newVal.selection,
                            composition = newVal.composition
                        )
                    )
                } else {
                    // In edit mode, propagate full text + selection updates
                    editorState.onTextChange(
                        TextFieldValue(
                            text = newVal.text,
                            selection = newVal.selection,
                            composition = newVal.composition
                        )
                    )
                }
            },
            readOnly = isReadOnly,
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
