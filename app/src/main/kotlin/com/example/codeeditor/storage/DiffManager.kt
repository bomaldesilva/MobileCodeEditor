package com.example.codeeditor.storage

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch

object DiffManager {

    /**
     * Generates a standard Unified Diff patch string between [baseText] and [newText].
     */
    fun createPatch(baseText: String, newText: String, fileName: String = "file.txt"): String {
        val baseLines = baseText.lines()
        val newLines = newText.lines()
        val patch: Patch<String> = DiffUtils.diff(baseLines, newLines)
        val unifiedDiffLines = UnifiedDiffUtils.generateUnifiedDiff(fileName, fileName, baseLines, patch, 3)
        return unifiedDiffLines.joinToString("\n")
    }

    /**
     * Reconstructs text by applying a Unified Diff [patchString] to [baseText].
     */
    fun applyPatch(baseText: String, patchString: String): String {
        if (patchString.isBlank()) return baseText
        val baseLines = baseText.lines()
        val patchLines = patchString.lines()
        return try {
            val patch: Patch<String> = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
            val reconstructedLines = DiffUtils.patch(baseLines, patch)
            reconstructedLines.joinToString("\n")
        } catch (e: Exception) {
            baseText
        }
    }

    /**
     * Generates line-by-line comparison between two texts for UI diff display.
     */
    fun generateLineDiff(text1: String, text2: String): List<DiffLine> {
        val lines1 = text1.lines()
        val lines2 = text2.lines()
        val patch: Patch<String> = DiffUtils.diff(lines1, lines2)
        
        val result = mutableListOf<DiffLine>()
        var line1Idx = 0
        var line2Idx = 0

        for (delta in patch.deltas) {
            val targetPos = delta.source.position
            while (line1Idx < targetPos) {
                result.add(DiffLine(DiffType.UNCHANGED, lines1[line1Idx], line1Idx + 1, line2Idx + 1))
                line1Idx++
                line2Idx++
            }

            for (sourceLine in delta.source.lines) {
                result.add(DiffLine(DiffType.DELETED, sourceLine, line1Idx + 1, null))
                line1Idx++
            }

            for (targetLine in delta.target.lines) {
                result.add(DiffLine(DiffType.INSERTED, targetLine, null, line2Idx + 1))
                line2Idx++
            }
        }

        while (line1Idx < lines1.size) {
            result.add(DiffLine(DiffType.UNCHANGED, lines1[line1Idx], line1Idx + 1, line2Idx + 1))
            line1Idx++
            line2Idx++
        }

        return result
    }
}

enum class DiffType { UNCHANGED, INSERTED, DELETED }

data class DiffLine(
    val type: DiffType,
    val text: String,
    val oldLineNum: Int?,
    val newLineNum: Int?
)
