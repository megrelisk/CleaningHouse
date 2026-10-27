package ge.cleaninghouse.quest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ge.cleaninghouse.quest.data.PlayerProfile
import kotlin.math.PI
import kotlin.math.sin

private val Skin = Color(0xFFFFD9BC)
private val SkinShade = Color(0xFFF5C3A0)
private val EyeColor = Color(0xFF2B2440)
private val Cheek = Color(0xFFFF8FA3)

/** A cute animated character: idles with a gentle bob and blinks; jumps and cheers when [celebrate]. */
@Composable
fun Avatar(profile: PlayerProfile, size: Dp, modifier: Modifier = Modifier, celebrate: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "avatar")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (celebrate) 520 else 2200, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "bob",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = if (profile.isMale) 3900 else 3400
                1f at 0
                1f at durationMillis - 260
                0.1f at durationMillis - 160
                1f at durationMillis - 40
            }
        ),
        label = "blink",
    )
    val shirt = Color(profile.shirtColor)
    val hair = Color(profile.hairColor)

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(shirt.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension
            val wave = sin(bob * 2 * PI).toFloat()
            val dy = if (celebrate) -kotlin.math.abs(wave) * s * 0.08f else wave * s * 0.02f
            translate(top = dy) {
                drawCharacter(s, profile.isMale, hair, shirt, blink, celebrate, wave)
            }
        }
    }
}

private fun DrawScope.drawCharacter(
    s: Float,
    male: Boolean,
    hair: Color,
    shirt: Color,
    blink: Float,
    celebrate: Boolean,
    wave: Float,
) {
    val headC = Offset(s * 0.5f, s * 0.42f)
    val headR = s * 0.21f

    // Long hair behind the head
    if (!male) {
        drawRoundRect(
            hair,
            topLeft = Offset(s * 0.25f, s * 0.24f),
            size = Size(s * 0.5f, s * 0.5f),
            cornerRadius = CornerRadius(s * 0.2f),
        )
    }

    // Arms
    val armStroke = s * 0.07f
    if (celebrate) {
        val lift = s * 0.04f * wave
        drawLine(shirt, Offset(s * 0.32f, s * 0.74f), Offset(s * 0.16f, s * 0.52f + lift), armStroke, StrokeCap.Round)
        drawLine(shirt, Offset(s * 0.68f, s * 0.74f), Offset(s * 0.84f, s * 0.52f - lift), armStroke, StrokeCap.Round)
        drawCircle(Skin, s * 0.045f, Offset(s * 0.16f, s * 0.50f + lift))
        drawCircle(Skin, s * 0.045f, Offset(s * 0.84f, s * 0.50f - lift))
    }

    // Body + neck
    drawRoundRect(
        shirt,
        topLeft = Offset(s * 0.27f, s * 0.67f),
        size = Size(s * 0.46f, s * 0.42f),
        cornerRadius = CornerRadius(s * 0.18f),
    )
    drawRect(SkinShade, topLeft = Offset(s * 0.45f, s * 0.58f), size = Size(s * 0.1f, s * 0.1f))
    if (male) {
        // collar
        val collar = Path().apply {
            moveTo(s * 0.42f, s * 0.67f); lineTo(s * 0.5f, s * 0.75f); lineTo(s * 0.58f, s * 0.67f); close()
        }
        drawPath(collar, Color.White.copy(alpha = 0.85f))
    } else {
        drawCircle(Color.White.copy(alpha = 0.85f), s * 0.025f, Offset(s * 0.5f, s * 0.76f))
    }

    // Head + ears
    drawCircle(Skin, headR * 0.28f, Offset(headC.x - headR, headC.y + headR * 0.1f))
    drawCircle(Skin, headR * 0.28f, Offset(headC.x + headR, headC.y + headR * 0.1f))
    drawCircle(Skin, headR, headC)

    // Hair on top
    drawArc(
        hair,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headC.x - headR * 1.06f, headC.y - headR * 1.12f),
        size = Size(headR * 2.12f, headR * 1.7f),
    )
    if (male) {
        val spikes = Path().apply {
            moveTo(headC.x - headR * 0.9f, headC.y - headR * 0.35f)
            lineTo(headC.x - headR * 0.55f, headC.y - headR * 1.25f)
            lineTo(headC.x - headR * 0.25f, headC.y - headR * 0.6f)
            lineTo(headC.x + headR * 0.1f, headC.y - headR * 1.32f)
            lineTo(headC.x + headR * 0.35f, headC.y - headR * 0.6f)
            lineTo(headC.x + headR * 0.75f, headC.y - headR * 1.15f)
            lineTo(headC.x + headR * 0.95f, headC.y - headR * 0.3f)
            close()
        }
        drawPath(spikes, hair)
    } else {
        // side fringe + bow
        drawCircle(hair, headR * 0.45f, Offset(headC.x - headR * 0.55f, headC.y - headR * 0.55f))
        val bowC = Offset(headC.x + headR * 0.7f, headC.y - headR * 0.85f)
        val bow = Color(0xFFFF4F8B)
        val left = Path().apply {
            moveTo(bowC.x, bowC.y); lineTo(bowC.x - s * 0.08f, bowC.y - s * 0.05f); lineTo(bowC.x - s * 0.08f, bowC.y + s * 0.05f); close()
        }
        val right = Path().apply {
            moveTo(bowC.x, bowC.y); lineTo(bowC.x + s * 0.08f, bowC.y - s * 0.05f); lineTo(bowC.x + s * 0.08f, bowC.y + s * 0.05f); close()
        }
        drawPath(left, bow)
        drawPath(right, bow)
        drawCircle(bow, s * 0.022f, bowC)
    }

    // Eyes
    val eyeY = headC.y + headR * 0.08f
    val eyeW = s * 0.036f
    val eyeH = s * 0.052f * blink
    listOf(-1f, 1f).forEach { side ->
        val cx = headC.x + side * headR * 0.42f
        if (celebrate) {
            // happy closed eyes ^ ^
            drawArc(
                EyeColor, 200f, 140f, false,
                topLeft = Offset(cx - eyeW, eyeY - eyeW * 0.6f),
                size = Size(eyeW * 2, eyeW * 1.6f),
                style = Stroke(width = s * 0.018f, cap = StrokeCap.Round),
            )
        } else {
            drawOval(EyeColor, topLeft = Offset(cx - eyeW / 2, eyeY - eyeH / 2), size = Size(eyeW, eyeH))
            if (blink > 0.6f) drawCircle(Color.White, eyeW * 0.18f, Offset(cx + eyeW * 0.15f, eyeY - eyeH * 0.2f))
        }
        if (!male) {
            // lashes
            drawLine(EyeColor, Offset(cx + side * eyeW * 0.5f, eyeY - eyeH * 0.4f), Offset(cx + side * eyeW * 0.95f, eyeY - eyeH * 0.7f), s * 0.01f, StrokeCap.Round)
        }
        drawCircle(Cheek.copy(alpha = 0.45f), s * 0.03f, Offset(headC.x + side * headR * 0.62f, headC.y + headR * 0.42f))
    }

    // Mouth
    val mouthTop = Offset(headC.x - headR * 0.25f, headC.y + headR * 0.28f)
    if (celebrate) {
        drawArc(
            Color(0xFFB0304A), 0f, 180f, true,
            topLeft = mouthTop, size = Size(headR * 0.5f, headR * 0.42f),
        )
    } else {
        drawArc(
            EyeColor, 20f, 140f, false,
            topLeft = mouthTop, size = Size(headR * 0.5f, headR * 0.3f),
            style = Stroke(width = s * 0.016f, cap = StrokeCap.Round),
        )
    }
}

/** Both players side by side, slightly overlapping. */
@Composable
fun DuoAvatar(a: PlayerProfile, b: PlayerProfile, size: Dp, celebrate: Boolean = false) {
    Box(Modifier.size(width = size * 1.6f, height = size)) {
        Avatar(a, size, Modifier.align(Alignment.CenterStart), celebrate)
        Avatar(b, size, Modifier.align(Alignment.CenterEnd).offset(x = 0.dp), celebrate)
    }
}
