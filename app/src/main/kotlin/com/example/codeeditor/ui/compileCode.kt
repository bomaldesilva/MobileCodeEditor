package com.example.codeeditor.network

import com.example.codeeditor.FileManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class CompileResponse(
    val output: String? = null,
    val statusCode: Int? = null,
    val memory: String? = null,
    val cpuTime: String? = null,
    val error: String? = null
)

class CompilerClient() {

    // IMPORTANT: Replace these with your actual JDoodle credentials
    private val clientId = "YOUR_CLIENT_ID"
    private val clientSecret = "YOUR_CLIENT_SECRET"
    private val endpoint = "https://api.jdoodle.com/v1/execute"

    private fun postOnce(code: String, language: String): CompileResponse {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")

            // Map internal language names to JDoodle language codes
            val jdoodleLanguage = when (language.lowercase()) {
                "java" -> "java"
                "python" -> "python3"
                else -> "kotlin"
            }

            // Map version indices (e.g., JDK 17 for Java/Kotlin)
            val versionIndex = when (jdoodleLanguage) {
                "python3" -> "4" // Python 3.10
                else -> "4"      // JDK 17
            }

            val jsonBody = """
                {
                    "clientId": "$clientId",
                    "clientSecret": "$clientSecret",
                    "script": ${Gson().toJson(code)},
                    "language": "$jdoodleLanguage",
                    "versionIndex": "$versionIndex"
                }
            """.trimIndent()

            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream
            else (conn.errorStream ?: conn.inputStream)

            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            try {
                Gson().fromJson(body, CompileResponse::class.java) ?: CompileResponse(
                    error = "Server returned empty response (Code $responseCode)"
                )
            } catch (e: Exception) {
                CompileResponse(
                    error = "Parsing Error (Code $responseCode):\n$body"
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun compile(code: String, language: String): CompileResponse = withContext(Dispatchers.IO) {
        try {
            postOnce(code, language)
        } catch (e: java.net.ConnectException) {
            CompileResponse(
                error = "Failed to connect to JDoodle API.\nCheck your internet connection.\nError: ${e.message}"
            )
        } catch (e: Exception) {
            CompileResponse(
                error = "Network Error:\n${e.message ?: "Unknown error"}"
            )
        }
    }

    fun saveCodeLocally(fileManager: FileManager, fileName: String, code: String) {
        fileManager.saveFile(fileName, code)
    }
}
