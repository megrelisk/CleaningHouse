package ge.cleaninghouse.quest.ai

/** Offline quest generator used when no Gemini API key is configured. */
object DemoTasks {
    private data class D(val en: String, val ka: String, val dEn: String, val dKa: String, val min: Int, val cat: String, val emoji: String)

    private val pool = listOf(
        D("Make the bed", "საწოლის გასწორება", "Straighten sheets, fluff pillows.", "გაასწორე ზეწარი, გაბერე ბალიშები.", 3, "bed", "🛏️"),
        D("Wash the dishes in the sink", "ჭურჭლის გარეცხვა", "Clear everything from the sink.", "ნიჟარაში დარჩენილი ყველაფერი გარეცხე.", 10, "dishes", "🍽️"),
        D("Load the washing machine", "სარეცხის მანქანაში ჩადება", "Collect dirty clothes and start a wash.", "შეაგროვე ჭუჭყიანი ტანსაცმელი და ჩართე.", 4, "laundry", "👕"),
        D("Hang the clean laundry", "სარეცხის გაფენა", "Hang clothes from the machine to dry.", "მანქანიდან ამოღებული ტანსაცმელი გააშრე.", 6, "laundry", "🧺"),
        D("Fold dry clothes", "მშრალი ტანსაცმლის დაკეცვა", "Fold and put them in the wardrobe.", "დაკეცე და კარადაში ჩააწყვე.", 8, "laundry", "👚"),
        D("Take dry laundry down", "სარეცხის ჩამოხსნა", "Take dry clothes off the rack.", "გამშრალი ტანსაცმელი ჩამოხსენი საშრობიდან.", 3, "laundry", "🧦"),
        D("Wipe the coffee table glass", "ჟურნალის მაგიდის მინის გაწმენდა", "Spray and wipe smudges away.", "მოასხი და მოწმინდე ლაქები.", 2, "surfaces", "🪟"),
        D("Vacuum the living room", "მისაღების მტვერსასრუტით გაწმენდა", "Don't forget under the sofa.", "დივნის ქვეშაც გაიარე.", 10, "vacuum", "🌀"),
        D("Take out the trash", "ნაგვის გატანა", "Replace the bag with a new one.", "ახალი პარკი ჩადე.", 2, "trash", "🗑️"),
        D("Dust the shelves", "თაროების მტვრისგან გაწმენდა", "Wipe shelves and the TV stand.", "მოწმინდე თაროები და ტელევიზორის სადგამი.", 5, "dust", "🪶"),
        D("Put shoes on the rack", "ფეხსაცმლის დალაგება", "Line up shoes by the door.", "კართან ფეხსაცმელი მოაწესრიგე.", 2, "tidy", "👟"),
        D("Clear the desk", "სამუშაო მაგიდის დალაგება", "Put papers and cables in place.", "ქაღალდები და კაბელები ადგილზე დააბრუნე.", 5, "tidy", "🗂️"),
        D("Wipe the kitchen counter", "სამზარეულოს დახლის გაწმენდა", "Remove crumbs and sticky spots.", "მოაშორე ნამცეცები და წებოვანი ლაქები.", 3, "kitchen", "🧽"),
        D("Clean the stovetop", "ქურის გაწმენდა", "Degrease burners and the surface.", "ცხიმი მოაშორე ზედაპირს.", 7, "kitchen", "🍳"),
        D("Clean the bathroom mirror", "აბაზანის სარკის გაწმენდა", "Wipe water spots off.", "წყლის ლაქები მოწმინდე.", 2, "bathroom", "🪞"),
        D("Scrub the bathroom sink", "პირსაბანის გახეხვა", "Scrub and rinse the basin.", "გახეხე და ჩამორეცხე.", 4, "bathroom", "🚿"),
        D("Mop the kitchen floor", "სამზარეულოს იატაკის მოწმენდა", "Mop from far corner to door.", "შორეული კუთხიდან კარისკენ მოწმინდე.", 8, "floor", "🧹"),
        D("Water the plants", "ყვავილების მორწყვა", "Check every pot.", "ყველა ქოთანი შეამოწმე.", 3, "plants", "🪴"),
        D("Put clothes from chair away", "სკამზე დაყრილი ტანსაცმლის დალაგება", "Hang or fold what's clean.", "სუფთა ჩამოკიდე ან დაკეცე.", 4, "tidy", "🪑"),
        D("Empty the dishwasher", "ჭურჭლის სარეცხი მანქანის დაცლა", "Put dishes back in cupboards.", "ჭურჭელი კარადებში დააბრუნე.", 5, "dishes", "🥣"),
        D("Fluff and arrange cushions", "ბალიშების მოწესრიგება", "Arrange sofa cushions neatly.", "დივნის ბალიშები ლამაზად დააწყვე.", 1, "tidy", "🛋️"),
        D("Clean the fridge shelves", "მაცივრის თაროების გაწმენდა", "Throw away expired food.", "ვადაგასული საკვები გადააგდე.", 12, "kitchen", "🧊"),
        D("Wipe door handles", "კარის სახელურების გაწმენდა", "Disinfect all handles.", "ყველა სახელური გაასუფთავე.", 3, "surfaces", "🚪"),
        D("Change the bed sheets", "თეთრეულის გამოცვლა", "Put on fresh bedding.", "სუფთა თეთრეული გადააკარი.", 10, "bed", "🛌"),
        D("Clean the window glass", "ფანჯრის მინის გაწმენდა", "Spray and wipe in circles.", "მოასხი და წრიულად მოწმინდე.", 6, "windows", "🪟"),
    )

    fun generate(count: Int?): AiResult {
        val n = (count ?: (6..12).random()).coerceAtMost(pool.size)
        val tasks = pool.shuffled().take(n).sortedBy { it.min }.map {
            AiTask(
                titleKa = it.ka,
                titleEn = it.en,
                descriptionKa = it.dKa,
                descriptionEn = it.dEn,
                minutes = it.min,
                category = it.cat,
                emoji = it.emoji,
            )
        }
        return AiResult(
            roomKa = "დემო ოთახი",
            roomEn = "Demo room",
            summaryKa = "დემო რეჟიმი: დაამატე Gemini API გასაღები პარამეტრებში, რომ AI-მ ვიდეო გააანალიზოს.",
            summaryEn = "Demo mode: add a Gemini API key in Settings so the AI can analyze your video.",
            tasks = tasks,
        )
    }
}
