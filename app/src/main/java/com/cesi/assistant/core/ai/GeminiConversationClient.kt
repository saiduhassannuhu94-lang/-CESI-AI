package com.cesi.assistant.core.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GeminiConversationClient(
    private val apiKey: String,
    private val model: String = "gemini-3.8-flash"
) {
    private val history = JSONArray()
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    @Synchronized
    fun ask(userText: String): String? {
        if (apiKey.isBlank()) {
            return "Ba a saita Gemini API key na CESI ba tukuna."
        }

        history.put(
            JSONObject()
                .put("role", "user")
                .put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", userText))
                )
        )

        // Keep the conversation lightweight on-device.
        while (history.length() > 12) {
            history.remove(0)
        }

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            "Kai ne CESI, mataimakin murya na Android mai magana da Hausa da English. " +
                                "Ka kasance friendly, concise, natural, kuma ka fi Hausa idan mai amfani ya yi Hausa. " +
                                "Kada ka yi ikirarin cewa ka yi action a waya idan ba CESI ya yi action ɗin ba."
                        )
                    )
                )
            )
            .put("contents", history)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 500)
            )

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 25_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) {
                return "Akwai matsala wajen haɗa CESI da AI yanzu."
            }

            val response = JSONObject(responseBody)
            val text = response
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.let { parts ->
                    buildString {
                        for (i in 0 until parts.length()) {
                            val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                            if (partText.isNotBlank()) append(partText)
                        }
                    }
                }
                ?.trim()
                .orEmpty()

            if (text.isBlank()) {
                "Ban samu amsa daga AI ba."
            } else {
                history.put(
                    JSONObject()
                        .put("role", "model")
                        .put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", text))
                        )
                )
                text
            }
        } catch (_: Exception) {
            "Ban iya haɗawa da AI yanzu ba. Ka duba internet ɗin ka sake gwadawa."
        } finally {
            connection?.disconnect()
        }
    }

    @Synchronized
    fun reset() {
        while (history.length() > 0) history.remove(0)
    }
}
