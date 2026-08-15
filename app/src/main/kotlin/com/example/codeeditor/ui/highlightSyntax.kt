package com.example.codeeditor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.codeeditor.ui.theme.*

fun highlightSyntax(text: String, rules: SyntaxRules): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        // 🔹 First, mark comments
        val commentRanges = mutableListOf<IntRange>()

        // Handle multi-line comments first
        if (rules.comments.contains("/*")) {
            val multilinePattern = Regex("/\\*[\\s\\S]*?\\*/")
            multilinePattern.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
                commentRanges.add(match.range)
            }
        }

        // Handle Python docstrings
        if (rules.comments.contains("\"\"\"")) {
            val docstringPattern = Regex("\"\"\"[\\s\\S]*?\"\"\"")
            docstringPattern.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
                commentRanges.add(match.range)
            }
        }

        // Handle single-line comments
        rules.comments.filter { it != "/*" && it != "\"\"\"" }.forEach { commentSymbol ->
            val pattern = Regex("${Regex.escape(commentSymbol)}.*")
            pattern.findAll(text).forEach { match ->
                if (commentRanges.none { range -> range.contains(match.range.first) }) {
                    addStyle(
                        SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic),
                        match.range.first,
                        match.range.last + 1
                    )
                    commentRanges.add(match.range)
                }
            }
        }

        // 🔹 Keywords
        rules.keywords.forEach { keyword ->
            "\\b$keyword\\b".toRegex().findAll(text).forEach { match ->
                if (commentRanges.none { it.contains(match.range.first) }) {
                    addStyle(
                        SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }

        // 🔹 Strings
        val stringPattern = Regex("(\"[^\"]*\")|('[^']*')")
        stringPattern.findAll(text).forEach { match ->
            if (commentRanges.none { it.contains(match.range.first) }) {
                addStyle(
                    SpanStyle(color = SyntaxString),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        // 🔹 Annotations
        rules.annotations.forEach { annotation ->
            "\\b$annotation\\b".toRegex().findAll(text).forEach { match ->
                if (commentRanges.none { it.contains(match.range.first) }) {
                    addStyle(
                        SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.Medium),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }

        // 🔹 Markdown Specific (Headers, Lists, Bold, Italic)
        // Note: Headers
        "^#+ .*".toRegex(RegexOption.MULTILINE).findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = MidnightPrimary, fontWeight = FontWeight.ExtraBold),
                match.range.first,
                match.range.last + 1
            )
        }
        // Bold
        "\\*\\*.*?\\*\\*".toRegex().findAll(text).forEach { match ->
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
        // Links
        "\\[.*?\\]\\(.*?\\)".toRegex().findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = MidnightPrimary),
                match.range.first,
                match.range.last + 1
            )
        }
    }
}
