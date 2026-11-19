package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun Fader(
    value: Float,
    onChange: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
        .width(64.dp)
        .height(320.dp),
    label: String = "Fader",
) {
    val colors = MaterialTheme.colorScheme
    val current by rememberUpdatedState(value)
    val emit by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)
    val haptics = LocalHapticFeedback.current

    Canvas(
        modifier = modifier
            .semantics { contentDescription = "$label, valor ${formatValue(value)}" }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { emit((1f - it.y / size.height).coerceIn(0f, 1f)) },
                    onDoubleTap = { emit(0.5f) },
                )
            }
            .pointerInput(Unit) {
                var base = 0f
                var travel = 0f
                fun done() {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    release()
                }
                detectVerticalDragGestures(
                    onDragStart = {
                        base = current
                        travel = 0f
                    },
                    onDragEnd = { done() },
                    onDragCancel = { done() },
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
        val handleW = size.width * 0.9f
        val handleH = 40.dp.toPx()
        val corner = 8.dp.toPx()
        val top = handleH / 2 + (1f - level) * (size.height - handleH)

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
        drawRoundRect(
            color = colors.outline,
            topLeft = Offset(middle - handleW / 2, top - handleH / 2),
            size = Size(handleW, handleH),
            cornerRadius = CornerRadius(corner),
        )
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(middle - handleW / 2, top - handleH / 2),
            size = Size(handleW, handleH),
            cornerRadius = CornerRadius(corner),
            style = Stroke(1.5.dp.toPx()),
        )
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(middle - 9.dp.toPx(), top - 1.dp.toPx()),
            size = Size(18.dp.toPx(), 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}
