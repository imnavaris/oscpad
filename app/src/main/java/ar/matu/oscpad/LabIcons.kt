package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object LabIcons {
    @Composable
    fun X(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val pad = size.minDimension * 0.22f
            drawLine(
                color = tint,
                start = Offset(pad, pad),
                end = Offset(size.width - pad, size.height - pad),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width - pad, pad),
                end = Offset(pad, size.height - pad),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Gear(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val c = center
            val r = size.minDimension * 0.24f
            drawCircle(
                color = tint,
                radius = r,
                center = c,
                style = Stroke(w),
            )
            for (i in 0 until 8) {
                val a = i * (Math.PI.toFloat() / 4f)
                val ca = cos(a)
                val sa = sin(a)
                drawLine(
                    color = tint,
                    start = Offset(c.x + ca * r, c.y + sa * r),
                    end = Offset(c.x + ca * r * 1.55f, c.y + sa * r * 1.55f),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    @Composable
    fun Plus(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val p = size.minDimension * 0.25f
            drawLine(
                color = tint,
                start = Offset(p, size.height / 2),
                end = Offset(size.width - p, size.height / 2),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width / 2, p),
                end = Offset(size.width / 2, size.height - p),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }
}
