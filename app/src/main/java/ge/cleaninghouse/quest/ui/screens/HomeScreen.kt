package ge.cleaninghouse.quest.ui.screens

import ge.cleaninghouse.quest.ui.components.UpdateBanner
import ge.cleaninghouse.quest.ui.UpdateState
import ge.cleaninghouse.quest.ui.components.CoinBadge
import ge.cleaninghouse.quest.data.titleIn
import ge.cleaninghouse.quest.data.nameIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.data.CleaningSession
import ge.cleaninghouse.quest.data.Game
import ge.cleaninghouse.quest.data.Who
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.ui.Screen
import ge.cleaninghouse.quest.ui.components.GradientButton
import ge.cleaninghouse.quest.ui.components.PlayerCard
import ge.cleaninghouse.quest.ui.components.ProgressBar
import ge.cleaninghouse.quest.ui.components.SectionTitle
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(state: AppState, vm: AppViewModel, padding: PaddingValues) {
    val ka = LocalKa.current
    val project = state.currentProject ?: return
    val settings = state.settings
    var showProjects by remember { mutableStateOf(false) }
    var showNewSession by remember { mutableStateOf(false) }
    val active = project.sessions.filterNot { it.isComplete }
    val finished = project.sessions.filter { it.isComplete }
    val allTasks = project.sessions.flatMap { it.tasks }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { showProjects = true }
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(project.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        project.nameIn(ka), Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("▾", color = Purple, fontSize = 18.sp)
                }
            }
        }

        if (vm.update is UpdateState.Available || vm.update is UpdateState.Downloading || vm.update is UpdateState.Ready) {
            item { UpdateBanner(vm.update) { vm.installUpdate() } }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayerCard(settings.playerA, project.xpA, project.coinsA, Modifier.weight(1f))
                PlayerCard(settings.playerB, project.xpB, project.coinsB, Modifier.weight(1f))
            }
        }

        item {
            val house = Game.houseLevel(project.xpA + project.xpB)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏡", fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("House level ${house.level}", "სახლის დონე ${house.level}"),
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(Game.houseTitle(house.level, ka), color = Purple, fontSize = 13.sp)
                        }
                        Text("${house.intoLevel}/${house.needed}", color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressBar(house.progress)
                    Spacer(Modifier.height(10.dp))
                    val doneA = allTasks.count { it.doneBy == Who.A }
                    val doneB = allTasks.count { it.doneBy == Who.B }
                    val doneBoth = allTasks.count { it.doneBy == Who.BOTH }
                    Text(
                        "✅ ${settings.playerA.nameIn(ka)}: $doneA   ·   ${settings.playerB.nameIn(ka)}: $doneB   ·   🤝 $doneBoth",
                        color = Muted, fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            GradientButton(
                tr("🎥  Start new cleaning", "🎥  ახალი დალაგების დაწყება"),
                Modifier.fillMaxWidth(),
            ) { vm.screen = Screen.Capture }
            TextButton(onClick = { showNewSession = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("+ Create a quest list manually", "+ ქვესტების სიის ხელით შექმნა"))
            }
        }

        if (project.rewards.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(tr("🎁 Rewards to aim for", "🎁 ჯილდოები, რისთვისაც ღირს"), Modifier.weight(1f))
                    TextButton(onClick = { vm.screen = Screen.Store }) { Text(tr("All →", "ყველა →")) }
                }
            }
            item {
                val richest = maxOf(project.coinsA, project.coinsB)
                val goals = project.rewards.sortedBy { it.cost }.let { sorted ->
                    val next = sorted.indexOfFirst { it.cost > richest }.let { if (it < 0) sorted.size else it }
                    sorted.drop((next - 1).coerceAtLeast(0)).take(4).ifEmpty { sorted.takeLast(4) }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        goals.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.screen = Screen.Store }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(r.emoji, fontSize = 26.sp)
                                Spacer(Modifier.width(12.dp))
                                Text(r.titleIn(ka), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                CoinBadge(r.cost)
                            }
                        }
                    }
                }
            }
        }

        if (active.isNotEmpty()) {
            item { SectionTitle(tr("⚔️ Active quests", "⚔️ აქტიური ქვესტები")) }
            items(active, key = { it.id }) { s -> SessionCard(s) { vm.screen = Screen.Session(s.id) } }
        } else {
            item {
                Text(
                    tr(
                        "No active quests. Record a video of a room and the AI will turn the mess into quests!",
                        "აქტიური ქვესტები არ არის. ჩაწერე ოთახის ვიდეო და AI არეულობას ქვესტებად აქცევს!",
                    ),
                    color = Muted,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        if (finished.isNotEmpty()) {
            item { SectionTitle(tr("🏆 Completed", "🏆 დასრულებული")) }
            items(finished, key = { it.id }) { s -> SessionCard(s) { vm.screen = Screen.Session(s.id) } }
        }
    }

    if (showProjects) ProjectsDialog(state, vm) { showProjects = false }
    if (showNewSession) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewSession = false },
            title = { Text(tr("New quest list", "ახალი ქვესტების სია")) },
            text = {
                OutlinedTextField(
                    title, { title = it },
                    label = { Text(tr("Room or title", "ოთახი ან სათაური")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.createEmptySession(title); showNewSession = false }) { Text(tr("Create", "შექმნა")) }
            },
            dismissButton = { TextButton(onClick = { showNewSession = false }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
}

@Composable
private fun SessionCard(s: CleaningSession, onClick: () -> Unit) {
    val ka = LocalKa.current
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val left = s.tasks.filter { it.doneBy == null }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (s.isComplete) "🏆" else if (s.fromAi) "✨" else "📝", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.titleIn(ka), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fmt.format(Date(s.createdAt)), color = Muted, fontSize = 12.sp)
                }
                Text("${s.doneCount}/${s.tasks.size}", fontWeight = FontWeight.ExtraBold, color = Purple)
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(if (s.tasks.isEmpty()) 0f else s.doneCount.toFloat() / s.tasks.size, height = 8.dp)
            if (left.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ka) "დარჩა ~${left.sumOf { it.minutes }} წთ · ${left.sumOf { it.xp }} XP · ${left.sumOf { Game.coinsFor(it.xp) }} 🪙"
                    else "~${left.sumOf { it.minutes }} min left · ${left.sumOf { it.xp }} XP · ${left.sumOf { Game.coinsFor(it.xp) }} 🪙 to earn",
                    color = Muted, fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ProjectsDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    val ka = LocalKa.current
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏡") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) tr("New project", "ახალი პროექტი") else tr("Projects", "პროექტები")) },
        text = {
            if (creating) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        name, { name = it }, label = { Text(tr("Name (e.g. Summer house)", "სახელი (მაგ. აგარაკი)")) },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("🏡", "🏠", "🏢", "🏖️", "🚗", "🧰", "🏕️").forEach { e ->
                            Text(
                                e,
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (e == emoji) Color(0xFFE6E0FF) else Color.Transparent)
                                    .clickable { emoji = e }
                                    .padding(6.dp),
                                fontSize = 22.sp,
                            )
                        }
                    }
                }
            } else {
                Column {
                    state.projects.forEach { p ->
                        val selected = p.id == state.currentProject?.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (selected) Color(0xFFE6E0FF) else Color.Transparent)
                                .clickable { vm.selectProject(p.id); onDismiss() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(p.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.nameIn(ka), fontWeight = FontWeight.Bold)
                                Text(
                                    "🏡 Lv ${Game.houseLevel(p.xpA + p.xpB).level} · ${p.sessions.size} " +
                                        tr("sessions", "სესია"),
                                    color = Muted, fontSize = 12.sp,
                                )
                            }
                            if (selected) Text("✓", color = Purple, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFFF1EDFF))
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { vm.createProject(name, emoji); onDismiss() },
                ) { Text(tr("Create", "შექმნა")) }
            } else {
                TextButton(onClick = { creating = true }) { Text(tr("+ New project", "+ ახალი პროექტი")) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "დახურვა")) } },
    )
}
