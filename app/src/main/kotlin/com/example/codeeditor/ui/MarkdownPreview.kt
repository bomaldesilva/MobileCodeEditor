package com.example.codeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        if (content.isBlank()) {
            Text(
                text = "Empty Markdown Document",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val lines = content.lines()
            var inCodeBlock = false
            val codeBlockContent = mutableListOf<String>()

            lines.forEach { line ->
                when {
                    line.startsWith("```") -> {
                        if (inCodeBlock) {
                            // Render complete code block
                            CodeBlockItem(codeBlockContent.joinToString("\n"))
                            codeBlockContent.clear()
                            inCodeBlock = false
                        } else {
                            inCodeBlock = true
                        }
                    }
                    inCodeBlock -> {
                        codeBlockContent.add(line)
                    }
                    line.startsWith("# ") -> {
                        Text(
                            text = line.removePrefix("# "),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    line.startsWith("## ") -> {
                        Text(
                            text = line.removePrefix("## "),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    line.startsWith("### ") -> {
                        Text(
                            text = line.removePrefix("### "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    line.startsWith("> ") -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = line.removePrefix("> "),
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(parseFormattedInline(line.substring(2)))
                        }
                    }
                    else -> {
                        if (line.isNotBlank()) {
                            Text(
                                text = parseFormattedInline(line),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockItem(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun parseFormattedInline(text: String) = buildAnnotatedString {
    append(text)
    // Bold
    "\\*\\*.*?\\*\\*".toRegex().findAll(text).forEach { match ->
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
    }
    // Italic
    "\\*.*?\\*".toRegex().findAll(text).forEach { match ->
        if (!match.value.startsWith("**")) {
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
    }
}
