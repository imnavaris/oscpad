package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun Fader(
    value: Float,
    onChange: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
        .width(64.dp)
        .height(320.dp),
) {
    val colors = MaterialTheme.colorScheme
    val current by rememberUpdatedState(value)
    val emit by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                var base = 0f
                var travel = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        base = current
                        travel = 0f
                    },
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { pointer, delta ->
                    pointer.consume()
                    travel += delta
                    emit((base - travel / size.height).coerceIn(0f, 1f))
                }
            },
    ) {
        val level = value.coerceIn(0f, 1f)
        val middle = size.width / 2
        val trackWidth = 2.dp.toPx()
        val dotRadius = 3.dp.toPx()
        val cursorHalf = size.width * 0.36f
        val top = dotRadius + (1f - level) * (size.height - dotRadius * 2)

        drawRoundRect(
            color = colors.surfaceVariant,
            topLeft = Offset(middle - trackWidth / 2, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.primary, colors.primary.copy(alpha = 0.3f)),
                startY = top,
                endY = size.height,
            ),
            topLeft = Offset(middle - trackWidth / 2, top),
            size = Size(trackWidth, size.height - top),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        drawLine(
            color = colors.primary,
            start = Offset(middle - cursorHalf, top),
            end = Offset(middle + cursorHalf, top),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = Color(0x334ADE80),
            radius = dotRadius * 2.4f,
            center = Offset(middle, top),
        )
        drawCircle(
            color = Color(0xFF4ADE80),
            radius = dotRadius,
            center = Offset(middle, top),
        )
    }
}
