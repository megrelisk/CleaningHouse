package ge.cleaninghouse.quest.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Particle(
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val round: Boolean,
)

private val confettiColors = listOf(
    Color(0xFF6C4CF5), Color(0xFFFF7A59), Color(0xFF14C8A8),
    Color(0xFFFFC233), Color(0xFFFF4F8B), Color(0xFF2979FF),
)

/** A one-shot confetti burst from the center; restart it by changing [key]. */
@Composable
fun ConfettiBurst(key: Any, modifier: Modifier = Modifier) {
    val particles = remember(key) {
        List(110) {
            Particle(
                vx = Random.nextFloat() * 1.4f - 0.7f,
                vy = -(Random.nextFloat() * 1.1f + 0.3f),
                spin = Random.nextFloat() * 720f - 360f,
                size = Random.nextFloat() * 14f + 8f,
                color = confettiColors.random(),
                round = Random.nextBoolean(),
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(2200, easing = LinearEasing)) }

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val cx = size.width / 2
        val cy = size.height * 0.45f
        particles.forEach { p ->
            val x = cx + p.vx * t * size.width * 0.8f
            val y = cy + p.vy * t * size.height * 0.7f + 1.3f * t * t * size.height
            val color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f))
            if (p.round) {
                drawCircle(color, p.size / 2, Offset(x, y))
            } else {
                rotate(p.spin * t, Offset(x, y)) {
                    drawRect(color, Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
                }
            }
        }
    }
}
