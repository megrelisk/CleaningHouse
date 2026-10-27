package ge.cleaninghouse.quest.data

import kotlin.math.max

object Game {
    /** XP grows with the estimated effort of a task. */
    fun xpFor(minutes: Int): Int = 10 + minutes.coerceIn(1, 60) * 4

    fun coinsFor(xp: Int): Int = max(1, xp / 2)

    /** Each player gets 75% when a quest is done together: +50% for the team. */
    fun coopShare(value: Int): Int = (value * 3 + 3) / 4

    data class LevelInfo(val level: Int, val intoLevel: Int, val needed: Int) {
        val progress: Float get() = intoLevel.toFloat() / needed
    }

    private fun need(level: Int, scale: Int) = (100 + (level - 1) * 50) * scale

    private fun levelOf(xp: Int, scale: Int): LevelInfo {
        var level = 1
        var rest = xp
        while (rest >= need(level, scale)) {
            rest -= need(level, scale)
            level++
        }
        return LevelInfo(level, rest, need(level, scale))
    }

    fun playerLevel(xp: Int): LevelInfo = levelOf(xp, 1)

    fun houseLevel(totalXp: Int): LevelInfo = levelOf(totalXp, 2)

    private val titlesEn = listOf(
        "Dust Bunny", "Tidy Rookie", "Sponge Squire", "Broom Knight", "Mop Master",
        "Sparkle Mage", "Order Paladin", "Clean Legend",
    )
    private val titlesKa = listOf(
        "მტვრის კურდღელი", "დამწყები მოწესრიგებული", "ღრუბლის მეაბჯრე", "ცოცხის რაინდი",
        "იატაკის ოსტატი", "ბრწყინვალების ჯადოქარი", "წესრიგის პალადინი", "სისუფთავის ლეგენდა",
    )

    fun title(level: Int, ka: Boolean): String {
        val list = if (ka) titlesKa else titlesEn
        return list[((level - 1) / 2).coerceIn(0, list.lastIndex)]
    }

    val houseTitlesEn = listOf("Messy Den", "Cozy Nest", "Tidy Home", "Shiny Palace", "Spotless Castle")
    val houseTitlesKa = listOf("არეული ბუნაგი", "მყუდრო ბუდე", "მოწესრიგებული სახლი", "ბრწყინვალე სასახლე", "უნაკლო ციხესიმაგრე")

    fun houseTitle(level: Int, ka: Boolean): String {
        val list = if (ka) houseTitlesKa else houseTitlesEn
        return list[((level - 1) / 2).coerceIn(0, list.lastIndex)]
    }

    fun categoryEmoji(category: String): String = when (category) {
        "dishes" -> "🍽️"
        "laundry" -> "👕"
        "bed" -> "🛏️"
        "floor" -> "🧹"
        "vacuum" -> "🌀"
        "dust" -> "🪶"
        "surfaces" -> "🧽"
        "tidy" -> "📦"
        "trash" -> "🗑️"
        "bathroom" -> "🚿"
        "kitchen" -> "🍳"
        "windows" -> "🪟"
        "plants" -> "🪴"
        else -> "✨"
    }
}
