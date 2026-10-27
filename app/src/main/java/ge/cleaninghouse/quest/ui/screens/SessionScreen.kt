package ge.cleaninghouse.quest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.data.QuestTask
import ge.cleaninghouse.quest.data.Settings
import ge.cleaninghouse.quest.data.Who
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.ui.Screen
import ge.cleaninghouse.quest.ui.components.Avatar
import ge.cleaninghouse.quest.ui.components.DuoAvatar
import ge.cleaninghouse.quest.ui.components.EmojiBubble
import ge.cleaninghouse.quest.ui.components.Pill
import ge.cleaninghouse.quest.ui.components.ProgressBar
import ge.cleaninghouse.quest.ui.components.WhoPickerDialog
import ge.cleaninghouse.quest.ui.theme.Coral
import ge.cleaninghouse.quest.ui.theme.HeroGradient
import ge.cleaninghouse.quest.ui.theme.Mint
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.theme.categoryColor
import ge.cleaninghouse.quest.ui.tr

private enum class Filter { TODO, ALL, DONE }

@Composable
fun SessionScreen(state: AppState, vm: AppViewModel, sessionId: String) {
    val ka = LocalKa.current
    val project = state.currentProject
    val session = project?.sessions?.firstOrNull { it.id == sessionId }
    BackHandler { vm.screen = Screen.Home }
    if (session == null) {
        LaunchedEffect(Unit) { vm.screen = Screen.Home }
        return
    }

    var filter by remember { mutableStateOf(Filter.TODO) }
    var randomIds by remember(sessionId) { mutableStateOf<Set<String>?>(null) }
    var picking by remember { mutableStateOf<QuestTask?>(null) }
    var undoing by remember { mutableStateOf<QuestTask?>(null) }
    var showDice by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val pending = session.tasks.filter { it.doneBy == null }
    val visible = when {
        randomIds != null -> session.tasks.filter { it.id in randomIds!! }
        filter == Filter.TODO -> pending
        filter == Filter.DONE -> session.tasks.filter { it.doneBy != null }
        else -> session.tasks
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { vm.screen = Screen.Home }) { Text("←", fontSize = 22.sp) }
            Text(
                session.title, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { confirmDelete = true }) { Text("🗑️", fontSize = 18.sp) }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (session.summary.isNotBlank()) {
                            Text(session.summary, color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("${session.doneCount} of ${session.tasks.size} quests", "${session.doneCount} / ${session.tasks.size} ქვესტი"),
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (ka) "⏱ ~${pending.sumOf { it.minutes }} წთ" else "⏱ ~${pending.sumOf { it.minutes }} min",
                                color = Muted, fontSize = 13.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressBar(if (session.tasks.isEmpty()) 0f else session.doneCount.toFloat() / session.tasks.size)
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tr("🎲 Random", "🎲 შემთხვევითი"),
                        Modifier
                            .clip(CircleShape)
                            .background(HeroGradient)
                            .clickable(enabled = pending.isNotEmpty()) { showDice = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White, fontWeight = FontWeight.Bold,
                    )
                    FilterChip(randomIds == null && filter == Filter.TODO, { randomIds = null; filter = Filter.TODO }, { Text(tr("To do", "გასაკეთებელი")) })
                    FilterChip(randomIds == null && filter == Filter.ALL, { randomIds = null; filter = Filter.ALL }, { Text(tr("All", "ყველა")) })
                    FilterChip(randomIds == null && filter == Filter.DONE, { randomIds = null; filter = Filter.DONE }, { Text(tr("Done", "შესრულებული")) })
                    TextButton(onClick = { showAdd = true }) { Text(tr("+ Add", "+ დამატება")) }
                }
            }

            randomIds?.let { ids ->
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFFFFEDE6))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val left = session.tasks.count { it.id in ids && it.doneBy == null }
                        Text(
                            tr("🎲 Random run: $left of ${ids.size} left", "🎲 შემთხვევითი რაუნდი: დარჩა $left / ${ids.size}"),
                            Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Coral,
                        )
                        TextButton(onClick = { randomIds = null }) { Text("✕") }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (session.isComplete) "🏆" else "📝", fontSize = 56.sp)
                        Text(
                            if (session.isComplete) tr("Everything is sparkling!", "ყველაფერი ბრწყინავს!")
                            else tr("Nothing here yet", "აქ ჯერ არაფერია"),
                            color = Muted,
                        )
                    }
                }
            }

            items(visible, key = { it.id }) { task ->
                TaskCard(
                    task, state.settings,
                    Modifier.animateItem(),
                    onCheck = { if (task.doneBy == null) picking = task else undoing = task },
                    onDelete = { vm.deleteTask(session.id, task.id) },
                )
            }
        }
    }

    picking?.let { t ->
        WhoPickerDialog(t.title, state.settings, t.xp, onDismiss = { picking = null }) { who ->
            picking = null
            vm.completeTask(session.id, t.id, who)
        }
    }
    undoing?.let { t ->
        AlertDialog(
            onDismissRequest = { undoing = null },
            title = { Text(tr("Undo this quest?", "გავაუქმოთ შესრულება?")) },
            text = { Text(tr("XP and coins will be taken back.", "XP და მონეტები უკან დაბრუნდება.")) },
            confirmButton = { TextButton(onClick = { vm.undoTask(session.id, t.id); undoing = null }) { Text(tr("Undo", "გაუქმება")) } },
            dismissButton = { TextButton(onClick = { undoing = null }) { Text(tr("Keep", "დატოვება")) } },
        )
    }
    if (showDice) {
        AlertDialog(
            onDismissRequest = { showDice = false },
            title = { Text(tr("🎲 Roll random quests", "🎲 შემთხვევითი ქვესტები")) },
            text = {
                Column {
                    Text(tr("How many quests for a quick run?", "რამდენი ქვესტი სწრაფი რაუნდისთვის?"), color = Muted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 5, 10).forEach { n ->
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Purple, CircleShape)
                                    .clickable {
                                        randomIds = pending.shuffled().take(n).map { it.id }.toSet()
                                        showDice = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text("$n", fontWeight = FontWeight.ExtraBold, color = Purple, fontSize = 18.sp) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDice = false }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var minutes by remember { mutableStateOf("5") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(tr("Add a quest", "ქვესტის დამატება")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text(tr("Task", "დავალება")) }, singleLine = true)
                    OutlinedTextField(
                        minutes, { minutes = it.filter(Char::isDigit).take(3) },
                        label = { Text(tr("Minutes", "წუთი")) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    vm.addTask(session.id, title, minutes.toIntOrNull() ?: 5)
                    showAdd = false
                }) { Text(tr("Add", "დამატება")) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Delete this quest list?", "წავშალოთ ეს სია?")) },
            text = { Text(tr("Earned XP and coins are kept.", "მოპოვებული XP და მონეტები შენარჩუნდება.")) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteSession(session.id) }) { Text(tr("Delete", "წაშლა")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
}

@Composable
private fun TaskCard(
    task: QuestTask,
    settings: Settings,
    modifier: Modifier = Modifier,
    onCheck: () -> Unit,
    onDelete: () -> Unit,
) {
    val ka = LocalKa.current
    val done = task.doneBy != null
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier
            .fillMaxWidth()
            .alpha(if (done) 0.65f else 1f),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(if (done) 0.dp else 2.dp),
    ) {
        Column {
            Row(
                Modifier
                    .clickable { menu = !menu }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmojiBubble(task.emoji, categoryColor(task.category))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                    )
                    if (task.description.isNotBlank()) {
                        Text(task.description, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(if (ka) "⏱ ${task.minutes} წთ" else "⏱ ${task.minutes} min")
                        Pill("⭐ ${task.xp} XP", bg = Color(0xFFE6E0FF), fg = Purple)
                    }
                }
                Spacer(Modifier.width(8.dp))
                CheckButton(task.doneBy, settings, onCheck)
            }
            AnimatedVisibility(menu) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { menu = false; onDelete() }) { Text(tr("🗑️ Remove quest", "🗑️ ქვესტის წაშლა"), color = Coral) }
                }
            }
        }
    }
}

@Composable
private fun CheckButton(doneBy: Who?, settings: Settings, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (doneBy) {
            null -> Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(3.dp, Mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Who.A -> Avatar(settings.playerA, 44.dp)
            Who.B -> Avatar(settings.playerB, 44.dp)
            Who.BOTH -> DuoAvatar(settings.playerA, settings.playerB, 32.dp)
        }
    }
}
