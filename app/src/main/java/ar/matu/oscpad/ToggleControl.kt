package ar.matu.oscpad

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun ToggleControl(on: Boolean, onToggle: (Boolean) -> Unit, label: String, diameter: Dp = 120.dp) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(140, easing = Ease),
        label = "scale",
    )
    val fill by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(180, easing = Ease),
        label = "fill",
    )

    Box(
        modifier = Modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = { onToggle(!on) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2 - 1.dp.toPx()
            drawCircle(
                color = lerp(colors.surfaceVariant, colors.primary, fill),
                radius = radius,
            )
            drawCircle(
                color = lerp(colors.outline, colors.primary.copy(alpha = 0.55f), fill),
                radius = radius,
                style = Stroke(1.dp.toPx()),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = Mono,
            color = lerp(colors.onSurfaceVariant, colors.background, fill),
        )
    }
}
