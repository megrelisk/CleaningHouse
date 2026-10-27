package ge.cleaninghouse.quest.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ge.cleaninghouse.quest.ai.AiResult
import ge.cleaninghouse.quest.ai.DemoTasks
import ge.cleaninghouse.quest.ai.GeminiClient
import ge.cleaninghouse.quest.ai.MediaPrep
import ge.cleaninghouse.quest.ai.ModelUnavailableException
import ge.cleaninghouse.quest.ai.buildPrompt
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.data.CleaningSession
import ge.cleaninghouse.quest.data.Game
import ge.cleaninghouse.quest.data.Project
import ge.cleaninghouse.quest.data.QuestTask
import ge.cleaninghouse.quest.data.Redemption
import ge.cleaninghouse.quest.data.Repository
import ge.cleaninghouse.quest.data.Reward
import ge.cleaninghouse.quest.data.Settings
import ge.cleaninghouse.quest.data.Who
import ge.cleaninghouse.quest.data.DEFAULT_MODEL
import ge.cleaninghouse.quest.data.REWARDS_VERSION
import ge.cleaninghouse.quest.data.RewardCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data object Capture : Screen
    data object Store : Screen
    data object Settings : Screen
    data class Session(val id: String) : Screen
}

sealed interface Analysis {
    data object Idle : Analysis
    data object Running : Analysis
    data class Failed(val message: String) : Analysis
}

data class Celebration(
    val who: Who,
    val xp: Int,
    val coins: Int,
    val levelUpA: Int? = null,
    val levelUpB: Int? = null,
    val houseLevelUp: Int? = null,
    val sessionCleared: Boolean = false,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(File(app.filesDir, "state.json"))
    private val saveLock = Mutex()

    private val _state = MutableStateFlow(migrate(ensureProject(repo.load() ?: AppState())))
    val state: StateFlow<AppState> = _state

    var screen by mutableStateOf<Screen>(Screen.Home)
    var analysis by mutableStateOf<Analysis>(Analysis.Idle)
        private set
    var celebration by mutableStateOf<Celebration?>(null)

    private var analysisJob: Job? = null

    val captureDir: File get() = File(getApplication<Application>().cacheDir, "capture").apply { mkdirs() }

    /** Brings state saved by older versions up to date. */
    private fun migrate(s: AppState): AppState {
        val ka = s.settings.uiLang == "ka"
        val oldNames = setOf("ბიჭი", "გოგო")
        val settings = s.settings.copy(
            model = if (s.settings.model in setOf("gemini-2.5-flash", "")) DEFAULT_MODEL else s.settings.model,
            playerA = s.settings.playerA.let { if (it.name in oldNames) it.copy(name = "") else it },
            playerB = s.settings.playerB.let { if (it.name in oldNames) it.copy(name = "") else it },
        )
        val projects = s.projects.map { p ->
            if (p.rewardsVersion >= REWARDS_VERSION) p
            else p.copy(rewards = RewardCatalog.upgrade(p.rewards, ka), rewardsVersion = REWARDS_VERSION)
        }
        return s.copy(settings = settings, projects = projects)
    }

    private fun ensureProject(s: AppState): AppState {
        if (s.projects.isNotEmpty()) {
            return if (s.currentProjectId == null) s.copy(currentProjectId = s.projects.first().id) else s
        }
        val ka = s.settings.uiLang == "ka"
        val p = Project(
            name = if (ka) "მთავარი ბინა" else "Main apartment", emoji = "🏠",
            rewards = RewardCatalog.rewards(ka), rewardsVersion = REWARDS_VERSION,
        )
        return s.copy(projects = listOf(p), currentProjectId = p.id)
    }

    private fun update(f: (AppState) -> AppState) {
        _state.update(f)
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { repo.save(_state.value) }
        }
    }

    private fun updateProject(f: (Project) -> Project) = update { s ->
        val id = s.currentProject?.id
        s.copy(projects = s.projects.map { if (it.id == id) f(it) else it })
    }

    private fun updateSession(sessionId: String, f: (CleaningSession) -> CleaningSession) = updateProject { p ->
        p.copy(sessions = p.sessions.map { if (it.id == sessionId) f(it) else it })
    }

    // --- Projects ---

    fun createProject(name: String, emoji: String) = update { s ->
        val p = Project(
            name = name.trim().ifEmpty { "🏠" },
            emoji = emoji.trim().ifEmpty { "🏠" },
            rewards = RewardCatalog.rewards(s.settings.uiLang == "ka"),
            rewardsVersion = REWARDS_VERSION,
        )
        s.copy(projects = s.projects + p, currentProjectId = p.id)
    }

    fun selectProject(id: String) {
        update { it.copy(currentProjectId = id) }
        screen = Screen.Home
    }

    fun renameProject(name: String, emoji: String) = updateProject {
        it.copy(name = name.trim().ifEmpty { it.name }, emoji = emoji.trim().ifEmpty { it.emoji })
    }

    fun deleteCurrentProject() {
        update { s ->
            val id = s.currentProject?.id
            ensureProject(s.copy(projects = s.projects.filterNot { it.id == id }, currentProjectId = null))
        }
        screen = Screen.Home
    }

    fun resetProgress() = updateProject {
        it.copy(xpA = 0, xpB = 0, coinsA = 0, coinsB = 0, sessions = emptyList(), redemptions = emptyList())
    }

    // --- Settings ---

    fun updateSettings(f: (Settings) -> Settings) = update { it.copy(settings = f(it.settings)) }

    // --- Quests ---

    fun completeTask(sessionId: String, taskId: String, who: Who) {
        val project = _state.value.currentProject ?: return
        val session = project.sessions.firstOrNull { it.id == sessionId } ?: return
        val task = session.tasks.firstOrNull { it.id == taskId } ?: return
        if (task.doneBy != null) return

        val coins = Game.coinsFor(task.xp)
        val (xpA, xpB, cA, cB) = when (who) {
            Who.A -> listOf(task.xp, 0, coins, 0)
            Who.B -> listOf(0, task.xp, 0, coins)
            Who.BOTH -> listOf(Game.coopShare(task.xp), Game.coopShare(task.xp), Game.coopShare(coins), Game.coopShare(coins))
        }
        val beforeA = Game.playerLevel(project.xpA).level
        val beforeB = Game.playerLevel(project.xpB).level
        val beforeHouse = Game.houseLevel(project.xpA + project.xpB).level
        val afterA = Game.playerLevel(project.xpA + xpA).level
        val afterB = Game.playerLevel(project.xpB + xpB).level
        val afterHouse = Game.houseLevel(project.xpA + xpA + project.xpB + xpB).level

        updateProject { p ->
            p.copy(
                xpA = p.xpA + xpA, xpB = p.xpB + xpB,
                coinsA = p.coinsA + cA, coinsB = p.coinsB + cB,
                sessions = p.sessions.map { s ->
                    if (s.id != sessionId) s else s.copy(tasks = s.tasks.map {
                        if (it.id == taskId) it.copy(doneBy = who, doneAt = System.currentTimeMillis()) else it
                    })
                },
            )
        }
        celebration = Celebration(
            who = who,
            xp = if (who == Who.BOTH) xpA else task.xp,
            coins = if (who == Who.BOTH) cA else coins,
            levelUpA = afterA.takeIf { it > beforeA },
            levelUpB = afterB.takeIf { it > beforeB },
            houseLevelUp = afterHouse.takeIf { it > beforeHouse },
            sessionCleared = session.doneCount + 1 == session.tasks.size,
        )
    }

    fun undoTask(sessionId: String, taskId: String) {
        val project = _state.value.currentProject ?: return
        val task = project.sessions.firstOrNull { it.id == sessionId }?.tasks?.firstOrNull { it.id == taskId } ?: return
        val who = task.doneBy ?: return
        val coins = Game.coinsFor(task.xp)
        val share = Game.coopShare(task.xp)
        val coinShare = Game.coopShare(coins)
        updateProject { p ->
            p.copy(
                xpA = (p.xpA - when (who) { Who.A -> task.xp; Who.BOTH -> share; Who.B -> 0 }).coerceAtLeast(0),
                xpB = (p.xpB - when (who) { Who.B -> task.xp; Who.BOTH -> share; Who.A -> 0 }).coerceAtLeast(0),
                coinsA = (p.coinsA - when (who) { Who.A -> coins; Who.BOTH -> coinShare; Who.B -> 0 }).coerceAtLeast(0),
                coinsB = (p.coinsB - when (who) { Who.B -> coins; Who.BOTH -> coinShare; Who.A -> 0 }).coerceAtLeast(0),
                sessions = p.sessions.map { s ->
                    if (s.id != sessionId) s else s.copy(tasks = s.tasks.map {
                        if (it.id == taskId) it.copy(doneBy = null, doneAt = null) else it
                    })
                },
            )
        }
    }

    fun deleteTask(sessionId: String, taskId: String) = updateSession(sessionId) { s ->
        s.copy(tasks = s.tasks.filterNot { it.id == taskId })
    }

    fun addTask(sessionId: String, title: String, minutes: Int) = updateSession(sessionId) { s ->
        val m = minutes.coerceIn(1, 120)
        s.copy(tasks = s.tasks + QuestTask(title = title.trim(), minutes = m, xp = Game.xpFor(m), emoji = "📝"))
    }

    fun deleteSession(sessionId: String) {
        updateProject { p -> p.copy(sessions = p.sessions.filterNot { it.id == sessionId }) }
        screen = Screen.Home
    }

    fun createEmptySession(title: String) {
        val session = CleaningSession(title = title.trim().ifEmpty { "📝" }, tasks = emptyList())
        updateProject { p -> p.copy(sessions = listOf(session) + p.sessions) }
        screen = Screen.Session(session.id)
    }

    // --- AI analysis ---

    fun analyze(video: File?, photos: List<File>, count: Int?, hint: String, demo: Boolean) {
        val settings = _state.value.settings
        val ka = settings.uiLang == "ka"
        analysisJob?.cancel()
        analysis = Analysis.Running
        analysisJob = viewModelScope.launch {
            try {
                val result: AiResult = if (demo) {
                    kotlinx.coroutines.delay(1500)
                    DemoTasks.generate(count)
                } else {
                    val parts = withContext(Dispatchers.Default) { MediaPrep.buildParts(video, photos) }
                    if (parts.isEmpty()) throw IllegalStateException(
                        if (settings.uiLang == "ka") "ჯერ ჩაწერე ვიდეო ან გადაიღე ფოტო." else "Record a video or take a photo first."
                    )
                    generateWithFallback(settings.apiKey, settings.model, parts, buildPrompt(count, hint))
                }
                val tasks = result.tasks.filter { it.titleKa.isNotBlank() || it.titleEn.isNotBlank() }.map {
                    val minutes = it.minutes.coerceIn(1, 60)
                    QuestTask(
                        title = it.titleKa.trim(),
                        description = it.descriptionKa.trim(),
                        titleEn = it.titleEn.trim(),
                        descriptionEn = it.descriptionEn.trim(),
                        minutes = minutes,
                        xp = Game.xpFor(minutes),
                        category = it.category,
                        emoji = it.emoji.trim().ifEmpty { Game.categoryEmoji(it.category) },
                    )
                }
                if (tasks.isEmpty()) throw IllegalStateException(
                    if (settings.uiLang == "ka") "AI-მ დავალებები ვერ იპოვა. სცადე სხვა კადრი." else "The AI found no tasks. Try another recording."
                )
                val session = CleaningSession(
                    title = result.roomKa.ifBlank { hint }.ifBlank { "დალაგება" },
                    titleEn = result.roomEn.ifBlank { hint }.ifBlank { "Cleaning" },
                    summary = result.summaryKa,
                    summaryEn = result.summaryEn,
                    fromAi = !demo,
                    tasks = tasks,
                )
                updateProject { p -> p.copy(sessions = listOf(session) + p.sessions) }
                analysis = Analysis.Idle
                screen = Screen.Session(session.id)
                withContext(Dispatchers.IO) { captureDir.listFiles()?.forEach { it.delete() } }
            } catch (e: kotlinx.coroutines.CancellationException) {
                analysis = Analysis.Idle
                throw e
            } catch (e: Exception) {
                analysis = Analysis.Failed(e.message ?: e.toString())
            }
        }
    }

    /** Tries the configured model, then newer defaults if Google has retired it. */
    private suspend fun generateWithFallback(
        apiKey: String,
        model: String,
        parts: List<kotlinx.serialization.json.JsonObject>,
        prompt: String,
    ): AiResult {
        val client = GeminiClient(apiKey)
        var lastError: Exception? = null
        for (m in listOf(model, DEFAULT_MODEL, "gemini-flash-latest").filter { it.isNotBlank() }.distinct()) {
            try {
                val result = client.generateTasks(m, parts, prompt)
                if (m != model) updateSettings { it.copy(model = m) }
                return result
            } catch (e: ModelUnavailableException) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No model available")
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysis = Analysis.Idle
    }

    fun dismissAnalysisError() {
        analysis = Analysis.Idle
    }

    // --- Rewards ---

    fun addReward(title: String, emoji: String, cost: Int) = updateProject { p ->
        p.copy(rewards = p.rewards + Reward(title = title.trim(), emoji = emoji.trim().ifEmpty { "🎁" }, cost = cost.coerceAtLeast(1)))
    }

    fun deleteReward(id: String) = updateProject { p -> p.copy(rewards = p.rewards.filterNot { it.id == id }) }

    /** Returns false when the buyer can't afford it. */
    fun redeem(reward: Reward, buyer: Who): Boolean {
        val p = _state.value.currentProject ?: return false
        val wallet = if (buyer == Who.A) p.coinsA else p.coinsB
        if (buyer == Who.BOTH || wallet < reward.cost) return false
        updateProject {
            it.copy(
                coinsA = if (buyer == Who.A) it.coinsA - reward.cost else it.coinsA,
                coinsB = if (buyer == Who.B) it.coinsB - reward.cost else it.coinsB,
                redemptions = listOf(
                    Redemption(title = reward.title, emoji = reward.emoji, cost = reward.cost, buyer = buyer, key = reward.key)
                ) + it.redemptions,
            )
        }
        return true
    }

    fun toggleFulfilled(id: String) = updateProject { p ->
        p.copy(redemptions = p.redemptions.map { if (it.id == id) it.copy(fulfilled = !it.fulfilled) else it })
    }

    fun deleteRedemption(id: String) = updateProject { p -> p.copy(redemptions = p.redemptions.filterNot { it.id == id }) }
}
