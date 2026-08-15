package com.example.codeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.storage.VersionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersionHistoryDialog(
    versionHistory: List<VersionEntity>,
    onCreateSnapshot: (String) -> Unit,
    onCompareVersion: (Int, Int) -> Unit,
    onRestoreVersion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showSnapshotInput by remember { mutableStateOf(false) }
    var newSnapshotName by remember { mutableStateOf("") }

    // FIX: Arbitrary diff selection — user picks any two versions
    var showDiffPicker by remember { mutableStateOf(false) }
    var diffVersionA by remember { mutableStateOf(1) }
    var diffVersionB by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Version History & Delta Control",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Snapshot creation input
                if (showSnapshotInput) {
                    OutlinedTextField(
                        value = newSnapshotName,
                        onValueChange = { newSnapshotName = it },
                        label = { Text("Snapshot Name / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSnapshotInput = false }) { Text("Cancel") }
                        Button(onClick = {
                            onCreateSnapshot(newSnapshotName)
                            newSnapshotName = ""
                            showSnapshotInput = false
                        }) { Text("Save Snapshot") }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                } else if (showDiffPicker && versionHistory.size >= 2) {
                    // Arbitrary version comparison picker
                    Text(
                        "Compare any two versions:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val versionNumbers = versionHistory.map { it.versionNumber }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Version A picker
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From (A):", fontSize = 12.sp)
                            DropdownVersionPicker(
                                versions = versionHistory,
                                selected = diffVersionA,
                                onSelect = { diffVersionA = it }
                            )
                        }
                        // Version B picker
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To (B):", fontSize = 12.sp)
                            DropdownVersionPicker(
                                versions = versionHistory,
                                selected = diffVersionB,
                                onSelect = { diffVersionB = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDiffPicker = false }) { Text("Cancel") }
                        Button(onClick = {
                            if (diffVersionA != diffVersionB) {
                                onCompareVersion(
                                    minOf(diffVersionA, diffVersionB),
                                    maxOf(diffVersionA, diffVersionB)
                                )
                            }
                            showDiffPicker = false
                        }) { Text("Compare") }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSnapshotInput = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Snapshot", fontSize = 12.sp)
                        }
                        if (versionHistory.size >= 2) {
                            OutlinedButton(
                                onClick = {
                                    diffVersionA = versionHistory.first().versionNumber
                                    diffVersionB = versionHistory.last().versionNumber
                                    showDiffPicker = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Compare Versions", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (versionHistory.isEmpty()) {
                    Text(
                        text = "No version snapshots recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(versionHistory.reversed()) { version ->
                            VersionItemCard(
                                version = version,
                                onCompare = {
                                    val prevVersion = (version.versionNumber - 1).coerceAtLeast(1)
                                    onCompareVersion(prevVersion, version.versionNumber)
                                },
                                onRestore = { onRestoreVersion(version.versionNumber) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DropdownVersionPicker(
    versions: List<VersionEntity>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVersion = versions.find { it.versionNumber == selected }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("V$selected: ${selectedVersion?.versionName ?: ""}", fontSize = 11.sp, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { v ->
                DropdownMenuItem(
                    text = { Text("V${v.versionNumber}: ${v.versionName}", fontSize = 12.sp) },
                    onClick = {
                        onSelect(v.versionNumber)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VersionItemCard(
    version: VersionEntity,
    onCompare: () -> Unit,
    onRestore: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(version.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "V${version.versionNumber}: ${version.versionName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (version.isBase) "BASE" else "DELTA",
                    fontSize = 10.sp,
                    color = if (version.isBase) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier
                        .background(
                            if (version.isBase) Color(0xFF1B5E20) else Color(0xFFE65100),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saved: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (version.versionNumber > 1) {
                    OutlinedButton(
                        onClick = onCompare,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("vs Prev", fontSize = 12.sp)
                    }
                }
                Button(onClick = onRestore) {
                    Text("Restore", fontSize = 12.sp)
                }
            }
        }
    }
}
