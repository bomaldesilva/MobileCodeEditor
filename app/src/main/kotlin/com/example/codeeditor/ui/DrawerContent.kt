package com.example.codeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.storage.RecentFileEntity

@Composable
fun DrawerContent(
    currentLanguage: String = "kotlin",
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onOpenVersionHistory: () -> Unit,
    recentFiles: List<RecentFileEntity>,
    onSelectRecentFile: (String) -> Unit,   // FIX: passes filePath not fileName
    onLanguageChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Text(
            text = "Mobile Code IDE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // File Actions
        SidebarItem(icon = Icons.Default.Add,        label = "New File")           { onNewFile() }
        SidebarItem(icon = Icons.Default.FolderOpen, label = "Open File")          { onOpenFile() }
        SidebarItem(icon = Icons.Default.Save,       label = "Save")               { onSaveFile() }
        SidebarItem(icon = Icons.Default.SaveAs,     label = "Save As")            { onSaveAsFile() }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Version Control
        SidebarItem(icon = Icons.Default.History, label = "Version History & Diff") { onOpenVersionHistory() }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Recent Files
        if (recentFiles.isNotEmpty()) {
            Text(
                text = "Recent Files",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                items(recentFiles) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // FIX: pass filePath so loadOrCreateFile reads correct file
                            .clickable { onSelectRecentFile(recent.filePath) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            recent.fileName.endsWith(".kt")   -> "🟣"
                            recent.fileName.endsWith(".java") -> "🟠"
                            recent.fileName.endsWith(".py")   -> "🔵"
                            recent.fileName.endsWith(".md")   -> "⚪"
                            else                              -> "📄"
                        }
                        Text(text = icon, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recent.fileName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }

        // Language Switcher with active indicator (FIX: selected chip highlights)
        Text(
            text = "Language & Syntax",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("kotlin" to "Kotlin", "java" to "Java", "python" to "Python", "markdown" to "MD").forEach { (lang, label) ->
                FilterChip(
                    selected = currentLanguage == lang,
                    onClick = { onLanguageChange(lang) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
