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

@Serializable
data class Settings(
    val apiKey: String = "",
    val model: String = "gemini-2.5-flash",
    val uiLang: String = "ka",
    val aiLang: String = "ka",
    val playerA: PlayerProfile = PlayerProfile("ბიჭი", true, 0xFF4E342E, 0xFF2979FF),
    val playerB: PlayerProfile = PlayerProfile("გოგო", false, 0xFFE65100, 0xFFE91E63),
)

@Serializable
data class QuestTask(
    val id: String = newId(),
    val title: String,
    val description: String = "",
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
)

/** [buyer] spent coins; the other player owes them the perk until [fulfilled]. */
@Serializable
data class Redemption(
    val id: String = newId(),
    val title: String,
    val emoji: String,
    val cost: Int,
    val buyer: Who,
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
)

@Serializable
data class AppState(
    val projects: List<Project> = emptyList(),
    val currentProjectId: String? = null,
    val settings: Settings = Settings(),
) {
    val currentProject: Project? get() = projects.firstOrNull { it.id == currentProjectId } ?: projects.firstOrNull()
}

fun defaultRewards(ka: Boolean): List<Reward> = listOf(
    Reward(title = if (ka) "მეორემ მიყიდოს კოკა-კოლა" else "Buy me a Coca-Cola", emoji = "🥤", cost = 50),
    Reward(title = if (ka) "10 წუთიანი ფეხის მასაჟი" else "10-minute foot massage", emoji = "💆", cost = 120),
    Reward(title = if (ka) "მე ვირჩევ დღევანდელ ფილმს" else "I pick tonight's movie", emoji = "🎬", cost = 80),
    Reward(title = if (ka) "საუზმე საწოლში" else "Breakfast in bed", emoji = "🥞", cost = 200),
    Reward(title = if (ka) "ერთი საქმის გამოტოვება" else "Skip one chore", emoji = "🛋️", cost = 150),
    Reward(title = if (ka) "შოკოლადი ან ნაყინი" else "Chocolate or ice cream", emoji = "🍫", cost = 60),
)
