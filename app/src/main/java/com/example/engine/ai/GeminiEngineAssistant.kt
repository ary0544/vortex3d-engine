package com.example.engine.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiAssistantResponse(
    val thinkingContent: String?,
    val answerContent: String,
    val extractedCode: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object GeminiEngineAssistant {
    private const val MODEL_NAME = "gemini-3.1-pro-preview"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val ENGINE_API_RULES = """
        RULES & SPECIFICATION FOR VORTEX3D LUA SCRIPTS:
        1. Lifecycle Hooks:
           - function init() : called once on spawn.
           - function update(dt) : called every frame (dt in seconds).
           - function on_collision(other) : called when colliding with another entity name string.
        2. Entity API:
           - entity:rotate(rx, ry, rz)
           - entity:translate(dx, dy, dz)
           - entity:set_position(x, y, z) or entity:set_pos(x, y, z)
        3. Physics API:
           - physics:apply_force(fx, fy, fz)
           - physics:apply_impulse(ix, iy, iz)
        4. Audio & Logging:
           - engine:play_sound("jump" | "impact" | "laser" | "explosion" | "coin")
           - engine:log(message) or print(message)
        5. Virtual Controls:
           - input:get_axis("Horizontal") -> [-1..1]
           - input:get_axis("Vertical") -> [-1..1]
           - input:is_action_pressed("Jump") -> boolean
           - input:is_action_pressed("Fire") -> boolean

        INSTRUCTIONS:
        - Provide production-grade, bug-free Lua code inside ```lua ... ``` blocks.
        - When repairing errors, diagnose the exact cause (nil value, missing parameter, syntax error) and return the corrected complete script.
        - Zero conversational fluff.
    """

    suspend fun queryFixError(
        failingScript: String,
        errorMessage: String,
        sceneContext: String
    ): AiAssistantResponse {
        val prompt = """
            PERBAIKI ERROR SCRIPT LUA BERIKUT:
            Pesan Error: $errorMessage
            
            Script yang Bermasalah:
            ```lua
            $failingScript
            ```
            
            Konteks Scene:
            $sceneContext
            
            Instruksi: Analisis penyebab error dan berikan script Lua yang sudah diperbaiki sepenuhnya di dalam blok ```lua ... ```.
        """.trimIndent()

        return executeGeminiRequest(prompt, "fix_error")
    }

    suspend fun queryGenerateScript(
        userPrompt: String,
        sceneContext: String,
        taskType: String = "generate_lua"
    ): AiAssistantResponse {
        val prompt = """
            BUATKAN SCRIPT LUA DENGAN PERINTAH DAN ATURAN BERIKUT:
            Perintah User: $userPrompt
            
            Konteks Scene:
            $sceneContext
            
            Tipe Task: $taskType
            Instruksi: Tulis script lengkap yang siap dijalankan di Vortex3D di dalam blok ```lua ... ```.
        """.trimIndent()

        return executeGeminiRequest(prompt, taskType)
    }

    private suspend fun executeGeminiRequest(
        userPrompt: String,
        taskType: String
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiAssistantResponse(
                thinkingContent = "API Key belum dikonfigurasi.",
                answerContent = "Silakan masukkan Gemini API Key di Secrets panel AI Studio untuk mengaktifkan perbaikan error dan pembuatan script otomatis via Gemini 3.1 Pro Preview.",
                extractedCode = null,
                isSuccess = false,
                errorMessage = "API Key missing"
            )
        }

        try {
            val jsonRoot = JSONObject()

            // Contents array
            val contentsArray = JSONArray()
            val userContent = JSONObject()
            userContent.put("role", "user")
            val partsArray = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", userPrompt)
            partsArray.put(textPart)
            userContent.put("parts", partsArray)
            contentsArray.put(userContent)
            jsonRoot.put("contents", contentsArray)

            // System Instruction
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", ENGINE_API_RULES)
            sysParts.put(sysPart)
            systemInstruction.put("parts", sysParts)
            jsonRoot.put("systemInstruction", systemInstruction)

            // Thinking Config: thinkingLevel = "HIGH", no maxOutputTokens
            val generationConfig = JSONObject()
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", "HIGH")
            generationConfig.put("thinkingConfig", thinkingConfig)
            jsonRoot.put("generationConfig", generationConfig)

            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AiAssistantResponse(
                    thinkingContent = null,
                    answerContent = "Gagal memanggil Gemini ($response.code): $responseBody",
                    extractedCode = null,
                    isSuccess = false,
                    errorMessage = response.message
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext AiAssistantResponse(
                    thinkingContent = null,
                    answerContent = "Tidak ada jawaban yang dikembalikan.",
                    extractedCode = null,
                    isSuccess = false
                )
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")

            var thinkingText: String? = null
            val answerBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.optBoolean("thought", false)) {
                        thinkingText = (thinkingText ?: "") + p.optString("text", "")
                    } else {
                        val text = p.optString("text", "")
                        if (text.isNotBlank()) {
                            answerBuilder.append(text)
                        }
                    }
                }
            }

            val finalAnswer = if (answerBuilder.isNotEmpty()) answerBuilder.toString() else "Jawaban kosong."
            val extracted = extractCodeBlock(finalAnswer)

            AiAssistantResponse(
                thinkingContent = thinkingText,
                answerContent = finalAnswer,
                extractedCode = extracted,
                isSuccess = true
            )
        } catch (e: Exception) {
            AiAssistantResponse(
                thinkingContent = null,
                answerContent = "Terjadi kesalahan: ${e.message}",
                extractedCode = null,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    private fun extractCodeBlock(markdown: String): String? {
        val luaRegex = Regex("```(?:lua|glsl)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = luaRegex.find(markdown)
        return match?.groupValues?.getOrNull(1)?.trim()
    }
}
