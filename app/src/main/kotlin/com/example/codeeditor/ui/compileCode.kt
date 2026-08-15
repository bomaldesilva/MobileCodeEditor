package com.example.codeeditor.network

import com.example.codeeditor.FileManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class StatusInfo(
    val id: Int? = null,
    val description: String? = null
)

data class CompileResponse(
    val stdout: String? = null,
    val stderr: String? = null,
    val compile_output: String? = null,
    val time: String? = null,
    val memory: Long? = null,
    val status: StatusInfo? = null,
    val error: String? = null
)

class CompilerClient() {

    private val endpoint = "https://ce.judge0.com/submissions?wait=true"

    private fun postOnce(code: String, language: String): CompileResponse {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")

            val languageId = when {
                language.equals("java", ignoreCase = true) || code.contains("public class ") || code.contains("System.out.print") -> 62 // Java
                language.equals("python", ignoreCase = true) || (code.contains("def ") && !code.contains("fun ")) || (code.contains("print(") && !code.contains(";") && !code.contains("{")) -> 71 // Python
                else -> 78 // Kotlin
            }

            val requestMap = mapOf(
                "language_id" to languageId,
                "source_code" to code
            )
            val jsonBody = Gson().toJson(requestMap)

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
                error = "Failed to connect to compiler server.\nCheck your internet connection."
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

