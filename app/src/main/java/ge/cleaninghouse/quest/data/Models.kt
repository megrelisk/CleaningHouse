package ge.cleaninghouse.quest.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Who completed a quest or bought a reward. */
@Serializable
enum class Who { A, B, BOTH }

@Serializable
data class PlayerProfile(
    val name: String,
    val isMale: Boolean,
    val hairColor: Long,
    val shirtColor: Long,
)

const val DEFAULT_MODEL = "gemini-3.8-flash"

@Serializable
data class Settings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val uiLang: String = "ka",
    val aiLang: String = "ka",
    val playerA: PlayerProfile = PlayerProfile("", true, 0xFF4E342E, 0xFF2979FF),
    val playerB: PlayerProfile = PlayerProfile("", false, 0xFFE65100, 0xFFE91E63),
)

@Serializable
data class QuestTask(
    val id: String = newId(),
    val title: String,
    val description: String = "",
    val titleEn: String = "",
    val descriptionEn: String = "",
    val minutes: Int,
    val xp: Int,
    val category: String = "other",
    val emoji: String = "🧹",
    val doneBy: Who? = null,
    val doneAt: Long? = null,
)

@Serializable
data class CleaningSession(
    val id: String = newId(),
    val createdAt: Long = System.currentTimeMillis(),
    val title: String,
    val summary: String = "",
    val titleEn: String = "",
    val summaryEn: String = "",
    val fromAi: Boolean = false,
    val tasks: List<QuestTask>,
) {
    val doneCount: Int get() = tasks.count { it.doneBy != null }
    val isComplete: Boolean get() = tasks.isNotEmpty() && doneCount == tasks.size
}

@Serializable
data class Reward(
    val id: String = newId(),
    val title: String,
    val emoji: String,
    val cost: Int,
    val key: String? = null,
)

/** [buyer] spent coins; the other player owes them the perk until [fulfilled]. */
@Serializable
data class Redemption(
    val id: String = newId(),
    val title: String,
    val emoji: String,
    val cost: Int,
    val buyer: Who,
    val key: String? = null,
    val at: Long = System.currentTimeMillis(),
    val fulfilled: Boolean = false,
)

@Serializable
data class Project(
    val id: String = newId(),
    val name: String,
    val emoji: String = "🏠",
    val createdAt: Long = System.currentTimeMillis(),
    val xpA: Int = 0,
    val xpB: Int = 0,
    val coinsA: Int = 0,
    val coinsB: Int = 0,
    val sessions: List<CleaningSession> = emptyList(),
    val rewards: List<Reward> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    val rewardsVersion: Int = 0,
)

@Serializable
data class AppState(
    val projects: List<Project> = emptyList(),
    val currentProjectId: String? = null,
    val settings: Settings = Settings(),
) {
    val currentProject: Project? get() = projects.firstOrNull { it.id == currentProjectId } ?: projects.firstOrNull()
}

const val REWARDS_VERSION = 2

/** Text in the requested language, falling back to whichever one exists. */
fun pick(ka: Boolean, kaText: String, enText: String): String =
    if (ka) kaText.ifBlank { enText } else enText.ifBlank { kaText }

fun QuestTask.titleIn(ka: Boolean) = pick(ka, title, titleEn)
fun QuestTask.descriptionIn(ka: Boolean) = pick(ka, description, descriptionEn)
fun CleaningSession.titleIn(ka: Boolean) = pick(ka, title, titleEn)
fun CleaningSession.summaryIn(ka: Boolean) = pick(ka, summary, summaryEn)
fun Reward.titleIn(ka: Boolean) = RewardCatalog.title(key, title, ka)
fun Redemption.titleIn(ka: Boolean) = RewardCatalog.title(key, title, ka)

private val defaultProjectNames = setOf("მთავარი ბინა", "Main apartment")
fun Project.nameIn(ka: Boolean) =
    if (name in defaultProjectNames) (if (ka) "მთავარი ბინა" else "Main apartment") else name

fun PlayerProfile.nameIn(ka: Boolean) = name.ifBlank {
    when {
        isMale && ka -> "ბიჭი"
        isMale -> "Boy"
        ka -> "გოგო"
        else -> "Girl"
    }
}
