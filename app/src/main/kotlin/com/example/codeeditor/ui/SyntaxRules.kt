package com.example.codeeditor

import android.content.Context
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class SyntaxRules(
    val keywords: List<String>,
    val comments: List<String>,
    val strings: List<String>,
    val annotations: List<String> = emptyList()
)

fun loadSyntaxRules(context: Context, filename: String): SyntaxRules {
    return try {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        Json.decodeFromString<SyntaxRules>(jsonString)
    } catch (e: Exception) {
        SyntaxRules(keywords = emptyList(), comments = emptyList(), strings = emptyList())
    }
}
