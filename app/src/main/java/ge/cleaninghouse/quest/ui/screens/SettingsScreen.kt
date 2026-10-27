package ge.cleaninghouse.quest.ui.screens

import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.data.nameIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.data.PlayerProfile
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.components.Avatar
import ge.cleaninghouse.quest.ui.components.SectionTitle
import ge.cleaninghouse.quest.ui.theme.Coral
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.tr

private val hairColors = listOf(0xFF2B1B14, 0xFF4E342E, 0xFF8D5524, 0xFFE65100, 0xFFF9C74F, 0xFF9E9E9E, 0xFF7B1FA2, 0xFF1565C0)
private val shirtColors = listOf(0xFF2979FF, 0xFFE91E63, 0xFF14C8A8, 0xFFFF7A59, 0xFF6C4CF5, 0xFFFFC233, 0xFF43A047, 0xFF37474F)

@Composable
fun SettingsScreen(state: AppState, vm: AppViewModel, padding: PaddingValues) {
    val s = state.settings
    val ka = LocalKa.current
    val project = state.currentProject ?: return
    val uri = LocalUriHandler.current
    var showKey by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var projectName by remember(project.id, ka) { mutableStateOf(project.nameIn(ka)) }
    var projectEmoji by remember(project.id) { mutableStateOf(project.emoji) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(tr("⚙️ Settings", "⚙️ პარამეტრები"), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) }

        item {
            SettingsCard(tr("Language", "ენა")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(s.uiLang == "ka", { vm.updateSettings { it.copy(uiLang = "ka") } }, { Text("ქართული") })
                    FilterChip(s.uiLang == "en", { vm.updateSettings { it.copy(uiLang = "en") } }, { Text("English") })
                }
            }
        }

        item {
            SettingsCard(tr("🤖 AI (Google Gemini)", "🤖 AI (Google Gemini)")) {
                OutlinedTextField(
                    s.apiKey, { v -> vm.updateSettings { it.copy(apiKey = v.trim()) } },
                    Modifier.fillMaxWidth(),
                    label = { Text("Gemini API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "🙈" else "👁️") }
                    },
                )
                OutlinedTextField(
                    s.model, { v -> vm.updateSettings { it.copy(model = v.trim()) } },
                    Modifier.fillMaxWidth(),
                    label = { Text(tr("Model", "მოდელი")) },
                    singleLine = true,
                )
                Text(
                    tr(
                        "Get a free key at aistudio.google.com → \"Get API key\". The key is stored only on this phone.",
                        "უფასო გასაღები აიღე aistudio.google.com-ზე → \"Get API key\". გასაღები ინახება მხოლოდ ამ ტელეფონში.",
                    ),
                    color = Muted, fontSize = 12.sp,
                )
                TextButton(onClick = { uri.openUri("https://aistudio.google.com/apikey") }) {
                    Text(tr("Open Google AI Studio ↗", "Google AI Studio-ს გახსნა ↗"))
                }
            }
        }

        item {
            SettingsCard(tr("🎮 Players", "🎮 მოთამაშეები")) {
                PlayerEditor(s.playerA, ka) { p -> vm.updateSettings { it.copy(playerA = p) } }
                Spacer(Modifier.height(8.dp))
                PlayerEditor(s.playerB, ka) { p -> vm.updateSettings { it.copy(playerB = p) } }
            }
        }

        item {
            SettingsCard(tr("${project.emoji} Current project", "${project.emoji} მიმდინარე პროექტი")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(projectEmoji, { projectEmoji = it.take(4) }, Modifier.width(80.dp), singleLine = true)
                    OutlinedTextField(projectName, { projectName = it }, Modifier.weight(1f), singleLine = true)
                }
                OutlinedButton(onClick = { vm.renameProject(projectName, projectEmoji) }) { Text(tr("Save name", "სახელის შენახვა")) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { confirm = "reset" }) { Text(tr("Reset progress", "პროგრესის განულება"), color = Coral) }
                    TextButton(onClick = { confirm = "delete" }) { Text(tr("Delete project", "პროექტის წაშლა"), color = Coral) }
                }
            }
        }

        item {
            Text(
                tr(
                    "XP: 10 + 4 per estimated minute. Coins: half of XP. Together: 75% each (+50% team bonus).",
                    "XP: 10 + 4 ყოველ სავარაუდო წუთზე. მონეტები: XP-ის ნახევარი. ერთად: 75% თითოეულს (+50% გუნდური ბონუსი).",
                ),
                color = Muted, fontSize = 12.sp,
            )
        }
    }

    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(
                    if (what == "delete") tr("Delete \"${project.nameIn(ka)}\"?", "წავშალოთ \"${project.nameIn(ka)}\"?")
                    else tr("Reset all progress?", "განვანულოთ პროგრესი?")
                )
            },
            text = { Text(tr("This can't be undone.", "ამის დაბრუნება შეუძლებელია.")) },
            confirmButton = {
                TextButton(onClick = {
                    if (what == "delete") vm.deleteCurrentProject() else vm.resetProgress()
                    confirm = null
                }) { Text(tr("Yes", "დიახ"), color = Coral) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun PlayerEditor(p: PlayerProfile, ka: Boolean, onChange: (PlayerProfile) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(p, 72.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                p.name, { onChange(p.copy(name = it.take(20))) }, singleLine = true,
                label = { Text(tr("Name", "სახელი")) }, placeholder = { Text(p.nameIn(ka)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(p.isMale, { onChange(p.copy(isMale = true)) }, { Text(tr("👦 Boy", "👦 ბიჭი")) })
                FilterChip(!p.isMale, { onChange(p.copy(isMale = false)) }, { Text(tr("👧 Girl", "👧 გოგო")) })
            }
        }
    }
    Text(tr("Hair", "თმა"), color = Muted, fontSize = 12.sp)
    ColorRow(hairColors, p.hairColor) { onChange(p.copy(hairColor = it)) }
    Text(tr("Outfit", "ტანსაცმელი"), color = Muted, fontSize = 12.sp)
    ColorRow(shirtColors, p.shirtColor) { onChange(p.copy(shirtColor = it)) }
}

@Composable
private fun ColorRow(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            Spacer(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(3.dp, if (c == selected) Purple else Color.Transparent, CircleShape)
                    .clickable { onPick(c) }
            )
        }
    }
}
