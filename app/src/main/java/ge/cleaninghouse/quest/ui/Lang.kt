package ge.cleaninghouse.quest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalKa = staticCompositionLocalOf { true }

/** Inline bilingual text: English / Georgian. */
@Composable
@ReadOnlyComposable
fun tr(en: String, ka: String): String = if (LocalKa.current) ka else en
