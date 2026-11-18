package ar.matu.oscpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

fun formatValue(value: Float) = "%.2f".format(Locale.US, value)

private val types = listOf(
    "fader" to "Fader",
    "button" to "Botón",
    "xy" to "XY",
    "sensor" to "Sensor",
    "color" to "Color",
)

@Composable
fun ChannelModule(
    channel: Channel,
    taken: Set<String>,
    sensors: List<SensorSpec>,
    onLive: (Channel) -> Unit,
    onDone: (Channel) -> Unit,
    onDelete: () -> Unit,
    onSend: (String, Float) -> Unit,
    onSendMany: (String, FloatArray) -> Unit,
) {
    val throttle = remember(channel.id) { Throttle() }
    var name by remember(channel.id) { mutableStateOf(channel.name) }
    var minText by remember(channel.id) { mutableStateOf(channel.min.toString()) }
    var maxText by remember(channel.id) { mutableStateOf(channel.max.toString()) }
    var onText by remember(channel.id) { mutableStateOf(channel.onValue.toString()) }
    var offText by remember(channel.id) { mutableStateOf(channel.offValue.toString()) }
    var rateText by remember(channel.id) { mutableStateOf(channel.rateMs.toString()) }
    var editing by remember(channel.id) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    val type = channel.type
    val clean = sanitizeName(name)
    val minValue = minText.toFloatOrNull()
    val maxValue = maxText.toFloatOrNull()
    val onValue = onText.toFloatOrNull()
    val offValue = offText.toFloatOrNull()
    val rateValue = rateText.toLongOrNull()
    val nameOk = clean != null && clean !in taken
    val minOk = minValue != null && (maxValue == null || minValue < maxValue)
    val maxOk = maxValue != null && (minValue == null || minValue < maxValue)
    val binaryOk = onValue != null && offValue != null
    val rateOk = rateValue != null && rateValue >= 16L
    val range = channel.max - channel.min

    fun pushName() {
        val c = sanitizeName(name) ?: return
        if (c in taken) return
        name = c
        onDone(channel.copy(name = c))
    }

    fun pushRange() {
        val mn = minText.toFloatOrNull() ?: return
        val mx = maxText.toFloatOrNull() ?: return
        if (mn >= mx) return
        onDone(channel.copy(min = mn, max = mx))
    }

    fun pushBinary() {
        val on = onText.toFloatOrNull() ?: return
        val off = offText.toFloatOrNull() ?: return
        onDone(channel.copy(onValue = on, offValue = off))
    }

    fun pushRate() {
        val ms = rateText.toLongOrNull() ?: return
        if (ms < 16L) return
        onDone(channel.copy(rateMs = ms))
    }

    fun closeEditor() {
        if (!nameOk) name = channel.name
        editing = false
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(48.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            pushName()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        isError = !nameOk,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { closeEditor() },
                        ),
                    )
                } else {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            name = channel.name
                            editing = true
                        },
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                LabIcons.X(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (type) {
                "fader" -> Fader(
                    value = channel.level,
                    onChange = { level ->
                        val next = channel.copy(level = level)
                        onLive(next)
                        if (throttle.shouldSend()) onSend(next.address(), next.current())
                    },
                    onRelease = { onDone(channel) },
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight(),
                )

                "button" -> ToggleControl(
                    on = channel.on,
                    onToggle = { state ->
                        val next = channel.copy(on = state)
                        onDone(next)
                        onSend(next.address(), next.current())
                    },
                    label = formatValue(channel.current()),
                    diameter = 120.dp,
                )

                "xy" -> XyPad(
                    x = channel.level,
                    y = channel.level2,
                    onChange = { x, y ->
                        val next = channel.copy(level = x, level2 = y)
                        onLive(next)
                        if (throttle.shouldSend()) {
                            onSendMany(
                                next.address(),
                                floatArrayOf(
                                    next.min + (next.max - next.min) * x,
                                    next.min + (next.max - next.min) * y,
                                ),
                            )
                        }
                    },
                    onRelease = { onDone(channel) },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(),
                )

                "sensor" -> {
                    val spec = sensors.firstOrNull { it.kind == channel.sensorKind }
                        ?: sensors.firstOrNull()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            sensors.forEach { s ->
                                TypeChip(
                                    label = s.label,
                                    selected = s.kind == channel.sensorKind,
                                    onClick = {
                                        onDone(
                                            channel.copy(
                                                sensorKind = s.kind,
                                                sensorAxis = s.axes.first(),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            (spec?.axes ?: emptyList()).forEach { axis ->
                                TypeChip(
                                    label = axis,
                                    selected = axis == channel.sensorAxis,
                                    onClick = { onDone(channel.copy(sensorAxis = axis)) },
                                )
                            }
                        }
                    }
                }

                "color" -> {
                    val r = channel.level
                    val g = channel.level2
                    val b = channel.level3
                    fun push(next: Channel) {
                        onLive(next)
                        if (throttle.shouldSend()) {
                            onSendMany(
                                next.address(),
                                floatArrayOf(next.level, next.level2, next.level3),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(r, g, b), RoundedCornerShape(12.dp)),
                        )
                        ColorSlider(
                            label = "R",
                            value = r,
                            onChange = { push(channel.copy(level = it)) },
                        )
                        ColorSlider(
                            label = "G",
                            value = g,
                            onChange = { push(channel.copy(level2 = it)) },
                        )
                        ColorSlider(
                            label = "B",
                            value = b,
                            onChange = { push(channel.copy(level3 = it)) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (type) {
                "xy" -> "${formatValue(channel.min + range * channel.level)}  " +
                    formatValue(channel.min + range * channel.level2)
                "color" -> "${formatValue(channel.level)}  " +
                    "${formatValue(channel.level2)}  ${formatValue(channel.level3)}"
                else -> formatValue(channel.current())
            },
            style = MaterialTheme.typography.displaySmall,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = channel.address(),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(12.dp))
        when (type) {
            "fader", "xy" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    label = "Mín.",
                    value = minText,
                    isError = !minOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        minText = it
                        pushRange()
                    },
                )
                Field(
                    label = "Máx.",
                    value = maxText,
                    isError = !maxOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        maxText = it
                        pushRange()
                    },
                )
            }

            "button" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    label = "Encendido",
                    value = onText,
                    isError = !binaryOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        onText = it
                        pushBinary()
                    },
                )
                Field(
                    label = "Apagado",
                    value = offText,
                    isError = !binaryOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        offText = it
                        pushBinary()
                    },
                )
            }

            "sensor" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    label = "Intervalo ms",
                    value = rateText,
                    isError = !rateOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        rateText = it
                        pushRate()
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            types.forEach { (key, label) ->
                TypeChip(
                    label = label,
                    selected = type == key,
                    onClick = { onDone(channel.copy(type = key)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp),
        )
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
