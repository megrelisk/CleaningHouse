package ge.cleaninghouse.quest.data

/** Built-in rewards; their titles follow the app language. */
object RewardCatalog {
    class Def(val key: String, val emoji: String, val cost: Int, val en: String, val ka: String)

    val all = listOf(
        Def("cola", "🥤", 50, "Buy me a Coca-Cola", "მიყიდე კოკა-კოლა"),
        Def("foot_massage", "💆", 120, "10-minute foot massage", "10 წუთიანი ფეხის მასაჟი"),
        Def("movie", "🎬", 80, "I pick tonight's movie", "დღევანდელ ფილმს მე ვირჩევ"),
        Def("breakfast", "🥞", 200, "Breakfast in bed", "საუზმე საწოლში"),
        Def("skip_chore", "🛋️", 150, "Skip one chore", "ერთი საქმის გამოტოვება"),
        Def("sweets", "🍫", 60, "Chocolate or ice cream", "შოკოლადი ან ნაყინი"),
        Def("hug", "🤗", 20, "Long hug on demand", "ხანგრძლივი ჩახუტება მოთხოვნისთანავე"),
        Def("music", "🎵", 30, "I control the music all evening", "მთელი საღამო მუსიკას მე ვრთავ"),
        Def("coffee", "☕", 40, "Coffee made and brought to me", "ყავა მომიდუღე და მომიტანე"),
        Def("popcorn", "🍿", 50, "Popcorn and snacks for movie night", "პოპკორნი და სნექები ფილმისთვის"),
        Def("walk", "🚶", 60, "Evening walk together", "საღამოს ერთად გასეირნება"),
        Def("remote", "📺", 70, "The TV remote is mine all evening", "მთელი საღამო პულტი ჩემია"),
        Def("make_bed", "🛏️", 70, "You make the bed for a week", "ერთი კვირა საწოლს შენ ასწორებ"),
        Def("trash_week", "🗑️", 80, "You take out the trash for a week", "ერთი კვირა ნაგავს შენ გაიტან"),
        Def("dessert", "🍰", 90, "Bake or buy me a dessert", "დესერტი გამიცხე ან მიყიდე"),
        Def("my_dishes", "🍽️", 100, "You wash my dishes for a day", "ერთი დღე ჩემს ჭურჭელს შენ რეცხავ"),
        Def("gaming", "🎮", 120, "One hour of gaming, no complaints", "ერთი საათი თამაში, პრეტენზიების გარეშე"),
        Def("fold_laundry", "🧺", 130, "You fold my laundry", "ჩემს სარეცხს შენ დაკეცავ"),
        Def("back_massage", "💆‍♂️", 150, "15-minute back massage", "15 წუთიანი ზურგის მასაჟი"),
        Def("sleep_in", "😴", 180, "Sleep in, you handle the morning", "დილით ვიძინებ, დილის საქმეებს შენ აკეთებ"),
        Def("flowers", "💐", 220, "Surprise flowers", "სიურპრიზად ყვავილები"),
        Def("pizza", "🍕", 250, "Pizza night, your treat", "პიცის საღამო შენი ხარჯით"),
        Def("dinner", "🍝", 300, "You cook my favorite dinner", "ჩემს საყვარელ ვახშამს შენ ამზადებ"),
        Def("spa", "🛁", 350, "Home spa evening", "სახლის სპა საღამო"),
        Def("date", "🌃", 400, "Date night out, you plan it", "პაემანი, შენ გეგმავ"),
        Def("wish", "✨", 500, "One wish of my choice", "ერთი სურვილი ჩემი არჩევით"),
    )

    private val byKey = all.associateBy { it.key }

    fun title(key: String?, fallback: String, ka: Boolean): String =
        byKey[key]?.let { if (ka) it.ka else it.en } ?: fallback

    fun rewards(ka: Boolean): List<Reward> =
        all.map { Reward(title = if (ka) it.ka else it.en, emoji = it.emoji, cost = it.cost, key = it.key) }

    /** Tags the six original defaults with keys and adds the newer built-ins. */
    fun upgrade(existing: List<Reward>, ka: Boolean): List<Reward> {
        val tagged = existing.map { r ->
            if (r.key != null) r
            else all.take(6).firstOrNull { it.emoji == r.emoji && it.cost == r.cost }?.let { r.copy(key = it.key) } ?: r
        }
        val have = tagged.mapNotNull { it.key }.toSet()
        return tagged + rewards(ka).filter { it.key !in have }
    }
}
