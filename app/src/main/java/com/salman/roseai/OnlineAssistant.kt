package com.salman.roseai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenRouter-compatible online chat. The API key is supplied by the user and never logged. */
object OnlineAssistant {
    fun reply(apiKey: String, conversation: List<Pair<String, String>>): String {
        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("HTTP-Referer", "https://github.com/roopanaaz2-code/ROSE-AI")
            connection.setRequestProperty("X-Title", "ROSE AI")
            val messages = JSONArray().put(
                JSONObject().put("role", "system")
                    .put("content", "You are ROSE, a warm, capable Android voice assistant. Be concise, safe, and honest about device actions. Never claim an action happened unless Android confirms it. Explain when a phone feature or permission is unavailable.")
            )
            conversation.takeLast(20).forEach { (role, content) ->
                if ((role == "user" || role == "assistant") && content.isNotBlank()) {
                    messages.put(JSONObject().put("role", role).put("content", content.take(4000)))
                }
            }
            val body = JSONObject().put("model", "openrouter/free")
                .put("messages", messages).put("temperature", 0.7)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = try { JSONObject(response).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                return "Online service error ($code): " + (message?.take(240) ?: "Please check your API key and connection.")
            }
            val content = JSONObject(response).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            return content?.takeIf { it.isNotBlank() } ?: "The online service returned an empty answer."
        } finally {
            connection.disconnect()
        }
    }
}
