package ge.cleaninghouse.quest.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class AiTask(
    val title: String,
    val description: String = "",
    val minutes: Int = 5,
    val category: String = "other",
    val emoji: String = "",
)

@Serializable
data class AiResult(
    val room: String = "",
    val summary: String = "",
    val tasks: List<AiTask> = emptyList(),
)

val CATEGORIES = listOf(
    "dishes", "laundry", "bed", "floor", "vacuum", "dust", "surfaces", "tidy",
    "trash", "bathroom", "kitchen", "windows", "plants", "other",
)

class GeminiClient(private val apiKey: String, private val model: String) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val schema: JsonElement = Json.parseToJsonElement(
        """
        {
          "type": "OBJECT",
          "properties": {
            "room": {"type": "STRING"},
            "summary": {"type": "STRING"},
            "tasks": {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "title": {"type": "STRING"},
                  "description": {"type": "STRING"},
                  "minutes": {"type": "INTEGER"},
                  "category": {"type": "STRING", "enum": ${CATEGORIES.joinToString(",", "[", "]") { "\"$it\"" }}},
                  "emoji": {"type": "STRING"}
                },
                "required": ["title", "description", "minutes", "category", "emoji"],
                "propertyOrdering": ["title", "description", "minutes", "category", "emoji"]
              }
            }
          },
          "required": ["room", "summary", "tasks"],
          "propertyOrdering": ["room", "summary", "tasks"]
        }
        """.trimIndent()
    )

    suspend fun generateTasks(media: List<JsonObject>, prompt: String): AiResult = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        media.forEach { add(it) }
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                put("responseSchema", schema)
                put("temperature", 0.7)
            }
        }.toString().toByteArray(Charsets.UTF_8)

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${model.trim()}:generateContent")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30_000
            conn.readTimeout = 240_000
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(body.size)
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", apiKey.trim())
            conn.outputStream.use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Gemini HTTP $code: ${errorMessage(text)}")

            val root = Json.parseToJsonElement(text).jsonObject
            val out = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
            if (out.isNullOrBlank()) throw IOException("Gemini returned no tasks (${errorMessage(text)})")
            json.decodeFromString<AiResult>(out)
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(text: String): String = runCatching {
        val obj = Json.parseToJsonElement(text).jsonObject
        obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: obj["promptFeedback"]?.toString()
    }.getOrNull() ?: text.take(300)
}

fun buildPrompt(count: Int?, ka: Boolean, hint: String): String {
    val language = if (ka) "Georgian (ქართული ენა)" else "English"
    val amount = if (count == null) {
        "Decide the number of tasks yourself from how messy the space is: between 5 (almost clean) and 50 (very messy)."
    } else {
        "Return exactly $count tasks (if there is truly less to do, split jobs into smaller steps or add quick maintenance tasks)."
    }
    val extra = if (hint.isBlank()) "" else "\nUser note about this space: \"$hint\"."
    return """
        You are the quest master of a gamified house-cleaning app for a couple.
        The attached video and/or photos were recorded by walking through a room or home.
        Look carefully at everything visible and find what needs to be cleaned, tidied, washed, folded, hung, put away, vacuumed, wiped or thrown out
        (e.g. unmade bed, clothes to wash / hang / fold, dirty dishes, dusty shelves, crumbs, items lying around, full trash bin, smudged glass).

        Rules:
        - Each task is ONE concrete micro-quest about something you actually see, naming the object and place.
        - title: short imperative, max 6 words, no filler (e.g. "Wipe the glass coffee table").
        - description: one short sentence with the specific detail (what/where), max 15 words.
        - minutes: realistic estimate 1-30. Split big jobs into several smaller tasks.
        - category: one of the allowed values. emoji: one fitting emoji.
        - Order tasks from quick wins to bigger jobs.
        - $amount
        - room: short name of the space. summary: one encouraging sentence about the state of the space.
        - Write room, summary, title and description in $language.$extra
    """.trimIndent()
}
