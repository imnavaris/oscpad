package ar.matu.oscpad

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    @Composable
    fun Check(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            drawLine(
                color = tint,
                start = Offset(size.width * 0.24f, size.height * 0.54f),
                end = Offset(size.width * 0.45f, size.height * 0.72f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.45f, size.height * 0.72f),
                end = Offset(size.width * 0.78f, size.height * 0.28f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Accel(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val mid = size.height / 2
            drawLine(
                color = tint,
                start = Offset(size.width * 0.14f, mid),
                end = Offset(size.width * 0.72f, mid),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.56f, mid - size.height * 0.16f),
                end = Offset(size.width * 0.72f, mid),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.56f, mid + size.height * 0.16f),
                end = Offset(size.width * 0.72f, mid),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.14f, mid - size.height * 0.24f),
                end = Offset(size.width * 0.32f, mid - size.height * 0.24f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.14f, mid + size.height * 0.24f),
                end = Offset(size.width * 0.32f, mid + size.height * 0.24f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Gyro(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val c = center
            val r = size.minDimension * 0.3f
            val side = Size(r * 2, r * 2)
            drawArc(
                color = tint,
                startAngle = -30f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = side,
                style = Stroke(w, cap = StrokeCap.Round),
            )
            val a = Math.PI.toFloat() * 1.1f
            val tip = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
            drawLine(
                color = tint,
                start = Offset(tip.x - r * 0.45f, tip.y - r * 0.1f),
                end = tip,
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(tip.x - r * 0.1f, tip.y - r * 0.45f),
                end = tip,
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Magnet(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val r = size.minDimension * 0.28f
            val c = Offset(size.width / 2, size.height * 0.42f)
            drawArc(
                color = tint,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(w, cap = StrokeCap.Round),
            )
            drawLine(
                color = tint,
                start = Offset(c.x - r, c.y),
                end = Offset(c.x - r, c.y + r * 0.9f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(c.x + r, c.y),
                end = Offset(c.x + r, c.y + r * 0.9f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Light(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val c = center
            drawCircle(
                color = tint,
                radius = size.minDimension * 0.14f,
                center = c,
                style = Stroke(w),
            )
            for (i in 0 until 8) {
                val a = i * (Math.PI.toFloat() / 4f)
                val ca = cos(a)
                val sa = sin(a)
                drawLine(
                    color = tint,
                    start = Offset(c.x + ca * size.minDimension * 0.24f, c.y + sa * size.minDimension * 0.24f),
                    end = Offset(c.x + ca * size.minDimension * 0.36f, c.y + sa * size.minDimension * 0.36f),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    @Composable
    fun Prox(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val c = Offset(size.width * 0.3f, size.height / 2)
            drawCircle(color = tint, radius = w * 0.9f, center = c)
            listOf(0.22f, 0.34f).forEach { f ->
                val r = size.minDimension * f
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
    }

    @Composable
    fun Grid(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val s = size.minDimension * 0.32f
            val o = size.minDimension * 0.12f
            val cr = CornerRadius(size.minDimension * 0.06f)
            listOf(Offset(o, o), Offset(size.minDimension - o - s, o), Offset(o, size.minDimension - o - s), Offset(size.minDimension - o - s, size.minDimension - o - s)).forEach { top ->
                drawRoundRect(
                    color = tint,
                    topLeft = top,
                    size = Size(s, s),
                    cornerRadius = cr,
                    style = Stroke(w),
                )
            }
        }
    }

    @Composable
    fun Sliders(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val x1 = size.width * 0.32f
            val x2 = size.width * 0.68f
            drawLine(
                color = tint,
                start = Offset(x1, size.height * 0.12f),
                end = Offset(x1, size.height * 0.88f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(x2, size.height * 0.12f),
                end = Offset(x2, size.height * 0.88f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawCircle(color = tint, radius = w * 1.6f, center = Offset(x1, size.height * 0.38f))
            drawCircle(color = tint, radius = w * 1.6f, center = Offset(x2, size.height * 0.62f))
        }
    }

    @Composable
    fun Pencil(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.6.dp.toPx()
            drawLine(
                color = tint,
                start = Offset(size.width * 0.26f, size.height * 0.72f),
                end = Offset(size.width * 0.6f, size.height * 0.38f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.6f, size.height * 0.38f),
                end = Offset(size.width * 0.78f, size.height * 0.2f),
                strokeWidth = w * 0.62f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = tint,
                radius = w * 0.32f,
                center = Offset(size.width * 0.79f, size.height * 0.19f),
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.22f, size.height * 0.62f),
                end = Offset(size.width * 0.3f, size.height * 0.76f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun ChevronDown(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.6.dp.toPx()
            drawLine(
                color = tint,
                start = Offset(size.width * 0.28f, size.height * 0.4f),
                end = Offset(size.width * 0.5f, size.height * 0.62f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.5f, size.height * 0.62f),
                end = Offset(size.width * 0.72f, size.height * 0.4f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Save(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val left = size.width * 0.22f
            val top = size.height * 0.12f
            val right = size.width * 0.78f
            val bottom = size.height * 0.88f
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(w),
            )
            val slotH = (bottom - top) * 0.3f
            drawRoundRect(
                color = tint,
                topLeft = Offset(left + (right - left) * 0.24f, top + w / 2),
                size = Size((right - left) * 0.52f, slotH),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(left + (right - left) * 0.16f, bottom - (bottom - top) * 0.34f),
                size = Size((right - left) * 0.68f, (bottom - top) * 0.22f),
                cornerRadius = CornerRadius(1.dp.toPx()),
                style = Stroke(w * 0.8f),
            )
        }
    }

    @Composable
    fun Stack(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val s = size.minDimension * 0.52f
            val o = size.minDimension * 0.15f
            val cr = CornerRadius(size.minDimension * 0.08f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(o * 2, 0f),
                size = Size(s, s),
                cornerRadius = cr,
                style = Stroke(w),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(o, o),
                size = Size(s, s),
                cornerRadius = cr,
                style = Stroke(w),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(0f, o * 2),
                size = Size(s, s),
                cornerRadius = cr,
                style = Stroke(w),
            )
        }
    }

    @Composable
    fun Pressure(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val w = 2.dp.toPx()
            val c = Offset(size.width / 2, size.height * 0.62f)
            val r = size.minDimension * 0.32f
            drawArc(
                color = tint,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(w, cap = StrokeCap.Round),
            )
            val a = -Math.PI.toFloat() * 0.25f
            drawLine(
                color = tint,
                start = c,
                end = Offset(c.x + cos(a) * r * 0.8f, c.y + sin(a) * r * 0.8f),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }
}
