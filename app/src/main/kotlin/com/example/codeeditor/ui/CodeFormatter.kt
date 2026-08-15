package com.example.codeeditor.ui

object CodeFormatter {

    /**
     * Automated Kotlin Code Formatter.
     * Formats raw Kotlin code according to standard indentation and spacing rules.
     */
    fun formatKotlinCode(rawCode: String): String {
        if (rawCode.isBlank()) return rawCode

        val lines = rawCode.lines()
        val formattedLines = mutableListOf<String>()
        var indentLevel = 0
        val indentUnit = "    " // 4 spaces

        for (line in lines) {
            var trimmed = line.trim()
            if (trimmed.isEmpty()) {
                formattedLines.add("")
                continue
            }

            // Adjust indent level for closing braces
            val closingBraces = trimmed.takeWhile { it == '}' || it == ')' }.length
            if (closingBraces > 0 && trimmed.startsWith("}")) {
                indentLevel = (indentLevel - 1).coerceAtLeast(0)
            }

            // Format spacing around operators and keywords
            trimmed = formatSpacing(trimmed)

            // Add indented line
            val currentIndent = indentUnit.repeat(indentLevel)
            formattedLines.add("$currentIndent$trimmed")

            // Adjust indent level for opening braces
            val openCount = trimmed.count { it == '{' }
            val closeCount = trimmed.count { it == '}' }
            val netOpen = openCount - closeCount
            if (netOpen > 0) {
                indentLevel += netOpen
            } else if (netOpen < 0) {
                indentLevel = (indentLevel + netOpen).coerceAtLeast(0)
            }
        }

        return formattedLines.joinToString("\n")
    }

    private fun formatSpacing(line: String): String {
        var result = line
        // Ensure space after commas
        result = result.replace(Regex(",(?=\\S)"), ", ")
        // Ensure space around binary operators like = + - * / == !=
        result = result.replace(Regex("(?<=\\w)=(?=\\w)"), " = ")
        result = result.replace(Regex("(?<=\\w)\\+(?=\\w)"), " + ")
        return result
    }
}
