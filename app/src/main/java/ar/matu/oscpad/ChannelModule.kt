package ar.matu.oscpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

fun formatValue(value: Float) = "%.2f".format(Locale.US, value)

@Composable
fun ChannelModule(
    channel: Channel,
    taken: Set<String>,
    onUpdate: (Channel) -> Unit,
    onDelete: () -> Unit,
    onSend: (String, Float) -> Unit,
) {
    val throttle = remember(channel.id) { Throttle() }
    var name by remember(channel.id) { mutableStateOf(channel.name) }
    var minText by remember(channel.id) { mutableStateOf(channel.min.toString()) }
    var maxText by remember(channel.id) { mutableStateOf(channel.max.toString()) }

    val clean = sanitizeName(name)
    val minValue = minText.toFloatOrNull()
    val maxValue = maxText.toFloatOrNull()
    val nameOk = clean != null && clean !in taken
    val minOk = minValue != null && (maxValue == null || minValue < maxValue)
    val maxOk = maxValue != null && (minValue == null || minValue < maxValue)

    fun push() {
        val c = sanitizeName(name) ?: return
        if (c in taken) return
        val mn = minText.toFloatOrNull() ?: return
        val mx = maxText.toFloatOrNull() ?: return
        if (mn >= mx) return
        name = c
        onUpdate(channel.copy(name = c, min = mn, max = mx))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(channel.copy(expanded = !channel.expanded)) },
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!channel.expanded) {
                        Text(
                            text = "${channel.address()}  ${formatValue(channel.current())}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (channel.expanded) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (channel.type == "fader") {
                        Fader(
                            value = channel.level,
                            onChange = { level ->
                                val next = channel.copy(level = level)
                                onUpdate(next)
                                if (throttle.shouldSend()) onSend(next.address(), next.current())
                            },
                            onRelease = { onSend(channel.address(), channel.current()) },
                            modifier = Modifier
                                .width(64.dp)
                                .height(200.dp),
                        )
                    } else {
                        ToggleControl(
                            on = channel.on,
                            onToggle = { state ->
                                val next = channel.copy(on = state)
                                onUpdate(next)
                                onSend(next.address(), next.current())
                            },
                            diameter = 96.dp,                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${channel.address()}  ${formatValue(channel.current())}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                Field(
                    label = "Nombre",
                    value = name,
                    isError = !nameOk,
                    modifier = Modifier.fillMaxWidth(),
                    onChange = {
                        name = it
                        push()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(
                        label = "Min",
                        value = minText,
                        isError = !minOk,
                        modifier = Modifier.weight(1f),
                        numeric = true,
                        onChange = {
                            minText = it
                            push()
                        },
                    )
                    Field(
                        label = "Max",
                        value = maxText,
                        isError = !maxOk,
                        modifier = Modifier.weight(1f),
                        numeric = true,
                        onChange = {
                            maxText = it
                            push()
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(
                        label = "Fader",
                        selected = channel.type == "fader",
                        modifier = Modifier.weight(1f),
                        onClick = { onUpdate(channel.copy(type = "fader")) },
                    )
                    TypeChip(
                        label = "Botón",
                        selected = channel.type == "button",
                        modifier = Modifier.weight(1f),
                        onClick = { onUpdate(channel.copy(type = "button")) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
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
                .padding(vertical = 10.dp),
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
