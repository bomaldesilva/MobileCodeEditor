package com.example.codeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.storage.DiffLine
import com.example.codeeditor.storage.DiffType

@Composable
fun DiffViewerDialog(
    diffLines: List<DiffLine>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Version Line-by-Line Diff Comparison",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Red = Removed | Green = Added", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (diffLines.isEmpty()) {
                    Text("No differences detected between selected versions.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                    ) {
                        items(diffLines) { line ->
                            val bgColor = when (line.type) {
                                DiffType.INSERTED -> Color(0xFF1B5E20).copy(alpha = 0.3f)
                                DiffType.DELETED -> Color(0xFFB71C1C).copy(alpha = 0.3f)
                                DiffType.UNCHANGED -> Color.Transparent
                            }
                            val prefix = when (line.type) {
                                DiffType.INSERTED -> "+ "
                                DiffType.DELETED -> "- "
                                DiffType.UNCHANGED -> "  "
                            }
                            val textColor = when (line.type) {
                                DiffType.INSERTED -> Color(0xFF81C784)
                                DiffType.DELETED -> Color(0xFFE57373)
                                DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "${line.oldLineNum?.toString() ?: ""} / ${line.newLineNum?.toString() ?: ""}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = "$prefix${line.text}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Diff")
            }
        }
    )
}
