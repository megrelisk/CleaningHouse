package ge.cleaninghouse.quest.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import ge.cleaninghouse.quest.ui.LocalKa
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.data.Reward
import ge.cleaninghouse.quest.data.Who
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.components.Avatar
import ge.cleaninghouse.quest.ui.components.CoinBadge
import ge.cleaninghouse.quest.ui.components.SectionTitle
import ge.cleaninghouse.quest.ui.theme.CoinGradient
import ge.cleaninghouse.quest.ui.theme.Coral
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.tr

@Composable
fun StoreScreen(state: AppState, vm: AppViewModel, padding: PaddingValues) {
    val project = state.currentProject ?: return
    val s = state.settings
    val ka = LocalKa.current
    var buying by remember { mutableStateOf<Reward?>(null) }
    var deleting by remember { mutableStateOf<Reward?>(null) }
    var adding by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(tr("🎁 Reward store", "🎁 ჯილდოების მაღაზია"), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text(
                tr(
                    "Spend coins from quests on real-life perks. The other player makes it happen!",
                    "ქვესტებით მოპოვებული მონეტები დახარჯე რეალურ ჯილდოებზე. მეორე მოთამაშე ასრულებს!",
                ),
                color = Muted, fontSize = 13.sp,
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(s.playerA, 44.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.playerA.nameIn(ka), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        CoinBadge(project.coinsA)
                    }
                    Avatar(s.playerB, 44.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.playerB.nameIn(ka), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        CoinBadge(project.coinsB)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(tr("Rewards", "ჯილდოები"), Modifier.weight(1f))
                TextButton(onClick = { adding = true }) { Text(tr("+ New reward", "+ ახალი ჯილდო")) }
            }
        }
        items(project.rewards.chunked(2)) { row ->
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { r ->
                    RewardCard(r, ka, Modifier.weight(1f).fillMaxHeight(), onBuy = { buying = r }, onLongDelete = { deleting = r })
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (project.redemptions.isNotEmpty()) {
            item { SectionTitle(tr("📜 Owed perks", "📜 ვალში მყოფი ჯილდოები")) }
            items(project.redemptions, key = { it.id }) { r ->
                val buyer = if (r.buyer == Who.A) s.playerA.nameIn(ka) else s.playerB.nameIn(ka)
                val giver = if (r.buyer == Who.A) s.playerB.nameIn(ka) else s.playerA.nameIn(ka)
                Card(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (r.fulfilled) 0.6f else 1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.titleIn(ka), fontWeight = FontWeight.Bold,
                                textDecoration = if (r.fulfilled) TextDecoration.LineThrough else null,
                            )
                            Text(
                                tr("$giver → $buyer · 🪙 ${r.cost}", "$giver → $buyer-ს · 🪙 ${r.cost}"),
                                color = Muted, fontSize = 12.sp,
                            )
                        }
                        Checkbox(checked = r.fulfilled, onCheckedChange = { vm.toggleFulfilled(r.id) })
                    }
                }
            }
        }
    }

    buying?.let { r ->
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text("${r.emoji} ${r.titleIn(ka)}") },
            text = {
                Column {
                    Text(tr("Who is spending ${r.cost} 🪙?", "ვინ ხარჯავს ${r.cost} 🪙-ს?"))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Who.A to project.coinsA, Who.B to project.coinsB).forEach { (who, coins) ->
                            val p = if (who == Who.A) s.playerA else s.playerB
                            val ok = coins >= r.cost
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFFF1EDFF))
                                    .clickable(enabled = ok) {
                                        vm.redeem(r, who)
                                        buying = null
                                    }
                                    .alpha(if (ok) 1f else 0.4f)
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Avatar(p, 56.dp)
                                Text(p.nameIn(ka), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (ok) "🪙 $coins" else tr("needs ${r.cost - coins} more", "აკლია ${r.cost - coins}"),
                                    fontSize = 12.sp, color = if (ok) Muted else Coral, textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { buying = null }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Remove reward?", "წავშალოთ ჯილდო?")) },
            text = { Text("${r.emoji} ${r.titleIn(ka)}") },
            confirmButton = { TextButton(onClick = { vm.deleteReward(r.id); deleting = null }) { Text(tr("Remove", "წაშლა")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
    if (adding) {
        var title by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🎁") }
        var cost by remember { mutableStateOf("50") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(tr("New reward", "ახალი ჯილდო")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        title, { title = it }, singleLine = true,
                        label = { Text(tr("Reward (e.g. Buy me a Coca-Cola)", "ჯილდო (მაგ. მიყიდე კოკა-კოლა)")) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(emoji, { emoji = it.take(4) }, Modifier.weight(1f), singleLine = true, label = { Text("Emoji") })
                        OutlinedTextField(
                            cost, { cost = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), singleLine = true,
                            label = { Text(tr("Cost 🪙", "ფასი 🪙")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("🎁", "🥤", "🍕", "💆", "🎬", "🍫", "☕", "🌹", "🎮").forEach { e ->
                            Text(e, Modifier.clickable { emoji = e }.padding(4.dp), fontSize = 22.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0, onClick = {
                    vm.addReward(title, emoji, cost.toInt())
                    adding = false
                }) { Text(tr("Add", "დამატება")) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(tr("Cancel", "გაუქმება")) } },
        )
    }
}

@Composable
private fun RewardCard(r: Reward, ka: Boolean, modifier: Modifier, onBuy: () -> Unit, onLongDelete: () -> Unit) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.fillMaxHeight().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("✕", Modifier.clickable(onClick = onLongDelete).padding(2.dp), color = Muted, fontSize = 12.sp)
            }
            Text(r.emoji, fontSize = 40.sp)
            Spacer(Modifier.height(6.dp))
            Text(r.titleIn(ka), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            Text(
                "🪙 ${r.cost}",
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(CoinGradient)
                    .clickable(onClick = onBuy)
                    .padding(vertical = 8.dp),
                color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
            )
        }
    }
}
