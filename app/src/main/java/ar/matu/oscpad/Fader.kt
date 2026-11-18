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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun Fader(value: Float, onChange: (Float) -> Unit, onRelease: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val current by rememberUpdatedState(value)
    val emit by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)

    Canvas(
        modifier = Modifier
            .width(64.dp)
            .height(320.dp)
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
        val trackWidth = 6.dp.toPx()
        val handleWidth = size.width - 8.dp.toPx()
        val handleHeight = 26.dp.toPx()
        val corner = handleHeight / 2
        val top = corner + (1f - level) * (size.height - handleHeight)

        drawRoundRect(
            color = colors.surfaceVariant,
            topLeft = Offset(middle - trackWidth / 2, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.primary, colors.primary.copy(alpha = 0.4f)),
                startY = top,
                endY = size.height,
            ),
            topLeft = Offset(middle - trackWidth / 2, top),
            size = Size(trackWidth, size.height - top),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        drawRoundRect(
            color = Color(0xFF1A1B23),
            topLeft = Offset(middle - handleWidth / 2, top - corner),
            size = Size(handleWidth, handleHeight),
            cornerRadius = CornerRadius(corner),
        )
        drawRoundRect(
            color = Color(0x2EFFFFFF),
            topLeft = Offset(middle - handleWidth / 2, top - corner),
            size = Size(handleWidth, handleHeight),
            cornerRadius = CornerRadius(corner),
            style = Stroke(1.dp.toPx()),
        )
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(middle - 9.dp.toPx(), top - 1.dp.toPx()),
            size = Size(18.dp.toPx(), 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}
