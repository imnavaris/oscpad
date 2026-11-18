package ar.matu.oscpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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

fun typeLabel(type: String) = when (type) {
    "fader" -> "Fader"
    "button" -> "Botón"
    "xy" -> "XY"
    "sensor" -> "Sensor"
    "color" -> "Color"
    else -> type
}

fun cleanIntInput(raw: String) = raw.filter { it.isDigit() }

fun cleanFloatInput(raw: String) = raw.filter { it.isDigit() || it == '.' || it == '-' }

fun channelValueText(channel: Channel) = when (channel.type) {
    "xy" -> channel.xyValues().let { "X ${formatValue(it[0])}  Y ${formatValue(it[1])}" }
    "sensor" -> formatValue(channel.level)
    else -> formatValue(channel.current())
}

private fun axisGloss(kind: String) = when (kind) {
    "accelerometer" -> "x: inclinar costados · y: inclinar frente · magnitud: sacudida"
    "gyroscope" -> "x/y/z: giro por eje · magnitud: giro total"
    "magnetic_field" -> "x/y/z: campo por eje · magnitud: intensidad"
    else -> ""
}

private fun sensorHint(kind: String) = when (kind) {
    "gyroscope" -> "Velocidad de giro por eje"
    "magnetic_field" -> "Campo magnético (brújula)"
    "light" -> "Luz ambiente (lux)"
    "proximity" -> "Cerca o lejos (cm)"
    "pressure" -> "Presión atmosférica (hPa)"
    else -> "Inclinación y movimiento del celu"
}

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
    var min2Text by remember(channel.id) { mutableStateOf(channel.min2.toString()) }
    var max2Text by remember(channel.id) { mutableStateOf(channel.max2.toString()) }
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
    val min2Value = min2Text.toFloatOrNull()
    val max2Value = max2Text.toFloatOrNull()
    val onValue = onText.toFloatOrNull()
    val offValue = offText.toFloatOrNull()
    val rateValue = rateText.toLongOrNull()
    val nameOk = clean != null && clean !in taken
    val minOk = minValue != null && (maxValue == null || minValue < maxValue)
    val maxOk = maxValue != null && (minValue == null || minValue < maxValue)
    val min2Ok = min2Value != null && (max2Value == null || min2Value < max2Value)
    val max2Ok = max2Value != null && (min2Value == null || min2Value < max2Value)
    val binaryOk = onValue != null && offValue != null
    val rateOk = rateValue != null && rateValue >= 16L

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

    fun pushRangeY() {
        val mn = min2Text.toFloatOrNull() ?: return
        val mx = max2Text.toFloatOrNull() ?: return
        if (mn >= mx) return
        onDone(channel.copy(min2 = mn, max2 = mx))
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

                "button" -> {
                    ToggleControl(
                        on = channel.on,
                        hold = channel.hold,
                        onLabel = formatValue(channel.onValue),
                        offLabel = formatValue(channel.offValue),
                        onToggle = { state ->
                            val next = channel.copy(on = state)
                            onDone(next)
                            onSend(next.address(), next.current())
                        },
                        onDown = {
                            val next = channel.copy(on = true)
                            onLive(next)
                            onSend(next.address(), channel.onValue)
                        },
                        onUp = {
                            val next = channel.copy(on = false)
                            onLive(next)
                            onSend(next.address(), channel.offValue)
                        },
                        diameter = 120.dp,
                    )
                }

                "xy" -> XyPad(
                    x = channel.level,
                    y = channel.level2,
                    onChange = { x, y ->
                        val next = channel.copy(level = x, level2 = y)
                        onLive(next)
                        if (throttle.shouldSend()) {
                            onSendMany(next.address(), next.xyValues())
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
                        Text(
                            text = sensorHint(channel.sensorKind),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        sensors.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                row.forEach { s ->
                                    val selected = s.kind == channel.sensorKind
                                    Column(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clickable {
                                                onDone(
                                                    channel.copy(
                                                        sensorKind = s.kind,
                                                        sensorAxis = s.axes.first(),
                                                    ),
                                                )
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        SensorGlyph(
                                            kind = s.kind,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = s.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if ((spec?.axes ?: emptyList()).size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (spec?.axes ?: emptyList()).forEach { axis ->
                                    TypeChip(
                                        label = axis.replaceFirstChar { it.uppercase() },
                                        selected = axis == channel.sensorAxis,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onDone(channel.copy(sensorAxis = axis)) },
                                    )
                                }
                            }
                            val gloss = axisGloss(channel.sensorKind)
                            if (gloss.isNotEmpty()) {
                                Text(
                                    text = gloss,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(Color(r, g, b), RoundedCornerShape(20.dp))
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(20.dp),
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                        ColorSlider(
                            tint = Color(0xFFFF5252),
                            value = r,
                            onChange = { push(channel.copy(level = it)) },
                            onDone = { onDone(channel) },
                        )
                        ColorSlider(
                            tint = MaterialTheme.colorScheme.primary,
                            value = g,
                            onChange = { push(channel.copy(level2 = it)) },
                            onDone = { onDone(channel) },
                        )
                        ColorSlider(
                            tint = Color(0xFF5AA9FF),
                            value = b,
                            onChange = { push(channel.copy(level3 = it)) },
                            onDone = { onDone(channel) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            listOf(
                                Triple("R", r, Color(0xFFFF5252)),
                                Triple("G", g, MaterialTheme.colorScheme.primary),
                                Triple("B", b, Color(0xFF5AA9FF)),
                            ).forEach { (letter, v, tint) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tint,
                                    )
                                    Text(
                                        text = formatValue(v),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontFamily = Mono,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (type == "xy") {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val v = channel.xyValues()
                listOf("X" to v[0], "Y" to v[1]).forEach { (letter, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Mono,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatValue(value),
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = Mono,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        } else if (type != "color") {
            Text(
                text = channelValueText(channel),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = Mono,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = channel.address(),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (type == "button") {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Mantener apretado",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = channel.hold,
                    onCheckedChange = { onDone(channel.copy(hold = it)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when (type) {
            "fader" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    label = "Mín.",
                    value = minText,
                    isError = !minOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        minText = cleanFloatInput(it)
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
                        maxText = cleanFloatInput(it)
                        pushRange()
                    },
                )
            }

            "xy" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(
                        label = "Mín. X",
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
                        label = "Máx. X",
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(
                        label = "Mín. Y",
                        value = min2Text,
                        isError = !min2Ok,
                        modifier = Modifier.weight(1f),
                        numeric = true,
                        onChange = {
                            min2Text = cleanFloatInput(it)
                            pushRangeY()
                        },
                    )
                    Field(
                        label = "Máx. Y",
                        value = max2Text,
                        isError = !max2Ok,
                        modifier = Modifier.weight(1f),
                        numeric = true,
                        onChange = {
                            max2Text = cleanFloatInput(it)
                            pushRangeY()
                        },
                    )
                }
            }

            "button" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    label = "Encendido",
                    value = onText,
                    isError = !binaryOk,
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    onChange = {
                        onText = cleanFloatInput(it)
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
                        offText = cleanFloatInput(it)
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
                        rateText = cleanIntInput(it)
                        pushRate()
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            types.forEach { (key, label) ->
                TypeChip(
                    label = label,
                    selected = type == key,
                    modifier = Modifier.weight(1f),
                    onClick = { onDone(channel.copy(type = key)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SensorGlyph(kind: String, tint: Color) {
    val modifier = Modifier.size(40.dp)
    when (kind) {
        "gyroscope" -> LabIcons.Gyro(tint = tint, modifier = modifier)
        "magnetic_field" -> LabIcons.Magnet(tint = tint, modifier = modifier)
        "light" -> LabIcons.Light(tint = tint, modifier = modifier)
        "proximity" -> LabIcons.Prox(tint = tint, modifier = modifier)
        "pressure" -> LabIcons.Pressure(tint = tint, modifier = modifier)
        else -> LabIcons.Accel(tint = tint, modifier = modifier)
    }
}

@Composable
private fun ColorSlider(
    tint: Color,
    value: Float,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = onChange,
        onValueChangeFinished = onDone,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = tint,
            activeTrackColor = tint.copy(alpha = 0.55f),
            inactiveTrackColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
fun OverviewXyRow(letter: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatValue(value),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = Mono,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    RoundedCornerShape(1.dp),
                ),
        )
    }
}
