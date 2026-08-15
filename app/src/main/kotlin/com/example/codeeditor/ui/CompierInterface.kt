package com.example.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompilerInterface(
    clipboardManager: ClipboardManager,
    compileOutput: String,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = { onClose() },
        title = { Text("Compiler Result") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp)
                    .background(backgroundColor)
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        compileOutput.lines().forEach { line ->
                            when {
                                line.contains("error", ignoreCase = true) -> append(
                                    AnnotatedString(line + "\n", SpanStyle(color = Color(0xFFFF6B6B))) // Light red for dark theme
                                )
                                line.contains("warning", ignoreCase = true) -> append(
                                    AnnotatedString(line + "\n", SpanStyle(color = Color(0xFFFFD93D))) // Light yellow for dark theme
                                )
                                else -> append(
                                    AnnotatedString(line + "\n", SpanStyle(color = textColor))
                                )
                            }
                        }
                    },
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (compileOutput.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(compileOutput))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Copy")
                }

                Button(
                    onClick = { onClose() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("OK")
                }
            }
        },
        containerColor = backgroundColor
    )
}
