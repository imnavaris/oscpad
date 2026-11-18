package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun XyPad(
    x: Float,
    y: Float,
    onChange: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val emit by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)
    val dot = Color(0xFF4ADE80)

    Box(modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { change, _ ->
                    change.consume()
                    emit(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        (1f - change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            },
        ) {
            val inset = 1.dp.toPx()
            for (i in 1..3) {
                val fx = size.width * i / 4
                val fy = size.height * i / 4
                drawLine(
                    color = colors.outline.copy(alpha = 0.25f),
                    start = Offset(fx, 0f),
                    end = Offset(fx, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = colors.outline.copy(alpha = 0.25f),
                    start = Offset(0f, fy),
                    end = Offset(size.width, fy),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawRect(
                color = colors.outline,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(1.5.dp.toPx()),
            )
            val px = x.coerceIn(0f, 1f) * size.width
            val py = (1f - y.coerceIn(0f, 1f)) * size.height
            drawLine(
                color = colors.primary.copy(alpha = 0.6f),
                start = Offset(px, 0f),
                end = Offset(px, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = colors.primary.copy(alpha = 0.6f),
                start = Offset(0f, py),
                end = Offset(size.width, py),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = dot.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = Offset(px, py))
            drawCircle(color = dot, radius = 3.dp.toPx(), center = Offset(px, py))
        }
        Text(
            text = "Y",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
        )
        Text(
            text = "X",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}
