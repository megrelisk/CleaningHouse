package ge.cleaninghouse.quest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.cleaninghouse.quest.ui.UpdateState
import ge.cleaninghouse.quest.ui.theme.Mint
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.tr

/** Shown on Home while a newer build is available, downloading or ready to install. */
@Composable
fun UpdateBanner(state: UpdateState, onInstall: () -> Unit) {
    val (title, sub, progress) = when (state) {
        is UpdateState.Available -> Triple(
            tr("🚀 New version available (build ${state.release.build})", "🚀 ხელმისაწვდომია ახალი ვერსია (build ${state.release.build})"),
            tr("Tap to update. Your progress stays.", "დააჭირე განახლებისთვის. პროგრესი შენარჩუნდება."),
            null,
        )
        is UpdateState.Downloading -> Triple(
            tr("⬇️ Downloading update… ${(state.progress * 100).toInt()}%", "⬇️ განახლება იტვირთება… ${(state.progress * 100).toInt()}%"),
            tr("Almost there", "ცოტაც და მზადაა"),
            state.progress,
        )
        is UpdateState.Ready -> Triple(
            tr("✅ Update downloaded", "✅ განახლება ჩამოიტვირთა"),
            tr("Tap, then press Install", "დააჭირე და შემდეგ „Install“"),
            null,
        )
        else -> return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Mint, Purple)))
            .clickable(enabled = state !is UpdateState.Downloading, onClick = onInstall)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            if (state !is UpdateState.Downloading) {
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("Update", "განახლება"),
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Purple, fontWeight = FontWeight.Bold,
                )
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress, brush = Brush.linearGradient(listOf(Color.White, Color.White)), height = 8.dp)
        }
    }
}
