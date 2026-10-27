package ge.cleaninghouse.quest.data

import kotlinx.serialization.json.Json
import java.io.File

class Repository(private val file: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppState? = runCatching {
        if (file.exists()) json.decodeFromString<AppState>(file.readText()) else null
    }.getOrNull()

    fun save(state: AppState) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppState.serializer(), state))
        tmp.renameTo(file)
    }
}
