package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ToggleControl(
    on: Boolean,
    hold: Boolean,
    onLabel: String,
    offLabel: String,
    onToggle: (Boolean) -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    diameter: Dp = 120.dp,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val latestOn by rememberUpdatedState(on)
    val latestToggle by rememberUpdatedState(onToggle)
    val latestDown by rememberUpdatedState(onDown)
    val latestUp by rememberUpdatedState(onUp)
    val active = if (hold) on else pressed

    Box(
        modifier = Modifier
            .size(diameter)
            .pointerInput(hold) {
                detectTapGestures(
                    onPress = {
                        val press = PressInteraction.Press(it)
                        interaction.emit(press)
                        try {
                            if (!hold) latestDown()
                            awaitRelease()
                        } finally {
                            if (!hold) latestUp()
                            interaction.emit(PressInteraction.Release(press))
                        }
                    },
                    onTap = { if (hold) latestToggle(!latestOn) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(color = colors.outline, radius = r)
            drawCircle(color = colors.background, radius = r - 3.dp.toPx())
            val capR = r - 12.dp.toPx()
            val push = if (active) 5.dp.toPx() else 0f
            val capC = center + Offset(0f, push)
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = capR,
                center = center + Offset(0f, 7.dp.toPx()),
            )
            drawCircle(
                color = if (active) colors.primary else colors.surfaceVariant,
                radius = capR,
                center = capC,
            )
            drawCircle(
                color = if (active) {
                    colors.primary.copy(alpha = 0.5f)
                } else {
                    colors.outline
                },
                radius = capR,
                center = capC,
                style = Stroke(2.dp.toPx()),
            )
            drawArc(
                color = Color.White.copy(alpha = if (active) 0.35f else 0.15f),
                startAngle = 200f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(capC.x - capR, capC.y - capR),
                size = Size(capR * 2, capR * 2),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(
            text = if (active) onLabel else offLabel,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            color = if (active) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}
