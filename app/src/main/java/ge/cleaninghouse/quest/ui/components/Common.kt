package ge.cleaninghouse.quest.ui.components

import ge.cleaninghouse.quest.data.nameIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ge.cleaninghouse.quest.data.Game
import ge.cleaninghouse.quest.data.PlayerProfile
import ge.cleaninghouse.quest.data.Settings
import ge.cleaninghouse.quest.data.Who
import ge.cleaninghouse.quest.ui.Celebration
import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.ui.theme.CoinGradient
import ge.cleaninghouse.quest.ui.theme.HeroGradient
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.tr
import kotlinx.coroutines.delay

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, brush: Brush = HeroGradient, height: Dp = 10.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color(0xFFE9E4FF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(brush)
        )
    }
}

@Composable
fun CoinBadge(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(CoinGradient)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text("$coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, bg: Color = MaterialTheme.colorScheme.surfaceVariant, fg: Color = Muted) {
    Text(
        text,
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun PlayerCard(profile: PlayerProfile, xp: Int, coins: Int, modifier: Modifier = Modifier) {
    val ka = LocalKa.current
    val info = Game.playerLevel(xp)
    Card(
        modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(profile, 76.dp)
            Spacer(Modifier.height(6.dp))
            Text(profile.nameIn(ka), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "Lv ${info.level} · ${Game.title(info.level, ka)}",
                color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            ProgressBar(info.progress, height = 8.dp)
            Spacer(Modifier.height(2.dp))
            Text("${info.intoLevel}/${info.needed} XP", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            CoinBadge(coins)
        }
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) HeroGradient else Brush.linearGradient(listOf(Color.LightGray, Color.Gray)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp, bottom = 4.dp), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
}

/** "Who did it?" — pick player A, player B or together. */
@Composable
fun WhoPickerDialog(
    title: String,
    settings: Settings,
    xp: Int,
    onDismiss: () -> Unit,
    onPick: (Who) -> Unit,
) {
    val ka = LocalKa.current
    val coins = Game.coinsFor(xp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = Color.White) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Who completed it?", "ვინ შეასრულა?"), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(title, color = Muted, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WhoOption(Modifier.weight(1f), settings.playerA.nameIn(ka), "+$xp XP · +$coins 🪙", onClick = { onPick(Who.A) }) {
                        Avatar(settings.playerA, 72.dp)
                    }
                    WhoOption(Modifier.weight(1f), settings.playerB.nameIn(ka), "+$xp XP · +$coins 🪙", onClick = { onPick(Who.B) }) {
                        Avatar(settings.playerB, 72.dp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                WhoOption(
                    Modifier.fillMaxWidth(),
                    tr("Together 🤝", "ერთად 🤝"),
                    tr("+${Game.coopShare(xp)} XP · +${Game.coopShare(coins)} 🪙 each · team bonus", "+${Game.coopShare(xp)} XP · +${Game.coopShare(coins)} 🪙 თითოეულს · გუნდური ბონუსი"),
                    onClick = { onPick(Who.BOTH) },
                ) {
                    DuoAvatar(settings.playerA, settings.playerB, 64.dp)
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel", "გაუქმება")) }
            }
        }
    }
}

@Composable
private fun WhoOption(
    modifier: Modifier,
    label: String,
    sub: String,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(2.dp, Color(0xFFE6E0FF)), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        avatar()
        Spacer(Modifier.height(6.dp))
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Full-screen reward moment after a quest is checked off. */
@Composable
fun CelebrationOverlay(c: Celebration, settings: Settings, onDone: () -> Unit) {
    val ka = LocalKa.current
    val pop = remember(c) { Animatable(0.4f) }
    LaunchedEffect(c) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    val big = c.levelUpA != null || c.levelUpB != null || c.houseLevelUp != null || c.sessionCleared
    LaunchedEffect(c) {
        delay(if (big) 3800 else 2200)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA140F2E))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiBurst(c)
        Surface(
            Modifier
                .padding(32.dp)
                .scale(pop.value),
            shape = MaterialTheme.shapes.large,
            color = Color.White,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (c.who) {
                    Who.A -> Avatar(settings.playerA, 110.dp, celebrate = true)
                    Who.B -> Avatar(settings.playerB, 110.dp, celebrate = true)
                    Who.BOTH -> DuoAvatar(settings.playerA, settings.playerB, 96.dp, celebrate = true)
                }
                Spacer(Modifier.height(12.dp))
                val name = when (c.who) {
                    Who.A -> settings.playerA.nameIn(ka)
                    Who.B -> settings.playerB.nameIn(ka)
                    Who.BOTH -> if (ka) "გუნდი" else "Team"
                }
                Text(
                    if (c.sessionCleared) tr("All quests cleared! 🏆", "ყველა ქვესტი შესრულდა! 🏆")
                    else tr("Quest complete! 🎉", "ქვესტი შესრულდა! 🎉"),
                    fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, textAlign = TextAlign.Center,
                )
                Text(name, color = Muted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        if (c.who == Who.BOTH) "+${c.xp} XP ×2" else "+${c.xp} XP",
                        bg = Color(0xFFE6E0FF), fg = Purple,
                    )
                    CoinBadge(c.coins)
                }
                c.levelUpA?.let { LevelUpLine("${settings.playerA.nameIn(ka)}: Lv $it · ${Game.title(it, ka)}") }
                c.levelUpB?.let { LevelUpLine("${settings.playerB.nameIn(ka)}: Lv $it · ${Game.title(it, ka)}") }
                c.houseLevelUp?.let { LevelUpLine("🏡 ${tr("House", "სახლი")} Lv $it · ${Game.houseTitle(it, ka)}") }
            }
        }
    }
}

@Composable
private fun LevelUpLine(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(
        tr("⬆️ LEVEL UP! ", "⬆️ ახალი დონე! ") + text,
        Modifier
            .clip(CircleShape)
            .background(HeroGradient)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun EmojiBubble(emoji: String, bg: Color, size: Dp = 48.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.48f).sp)
    }
}
