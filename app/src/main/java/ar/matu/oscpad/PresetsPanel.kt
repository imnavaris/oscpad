package ar.matu.oscpad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun PresetsPanel(
    presets: List<Preset>,
    activeId: Long?,
    onSelect: (Preset) -> Unit,
    onDeselect: () -> Unit,
    onSaveNew: (String) -> Unit,
    onOverwrite: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onReorder: (List<Preset>) -> Unit,
    onGoChannels: () -> Unit,
) {
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }
    var renamingId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Preset?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val heights = remember { mutableMapOf<Long, Int>() }
    var dragging by remember { mutableStateOf<List<Preset>?>(null) }
    val shown = dragging ?: presets

    fun finishDrag(commit: Boolean) {
        val order = dragging
        dragging = null
        dragId = null
        dragOffsetY = 0f
        if (commit && order != null && order.map { it.id } != presets.map { it.id }) {
            onReorder(order + presets.filter { p -> order.none { it.id == p.id } })
        }
    }

    fun moveDrag(id: Long, dy: Float) {
        val list = dragging ?: return
        var from = list.indexOfFirst { it.id == id }
        if (from == -1) return
        dragOffsetY += dy
        val fb = heights[id] ?: 200
        var top = 0
        for (i in 0 until from) top += heights[list[i].id] ?: fb
        val center = top + fb / 2 + dragOffsetY
        var y = 0
        var target = from
        for (i in list.indices) {
            val h = if (i == from) fb else heights[list[i].id] ?: fb
            val mid = y + h / 2
            if (i < from && center < mid) {
                target = i
                break
            }
            if (i > from && center > mid) target = i
            y += h
        }
        if (target != from) {
            val next = list.toMutableList()
            val item = next.removeAt(from)
            next.add(target, item)
            dragging = next
            var newTop = 0
            for (i in 0 until target) newTop += heights[next[i].id] ?: fb
            dragOffsetY -= (newTop - top).toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Presets",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        if (presets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onGoChannels,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    LabIcons.Plus(
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Creá tu primer preset",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                userScrollEnabled = dragId == null,
            ) {
                items(shown, key = { it.id }) { preset ->
                    val active = preset.id == activeId
                    val expanded = preset.id in expandedIds
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = tween(200),
                        label = "chevron",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .onSizeChanged { heights[preset.id] = it.height }
                            .graphicsLayer {
                                translationY = if (dragId == preset.id) dragOffsetY else 0f
                            }
                            .zIndex(if (dragId == preset.id) 1f else 0f)
                            .pointerInput(preset.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragging = presets
                                        dragId = preset.id
                                        dragOffsetY = 0f
                                        if (preset.id in expandedIds) {
                                            expandedIds = expandedIds - preset.id
                                        }
                                    },
                                    onDrag = { _, amount -> moveDrag(preset.id, amount.y) },
                                    onDragEnd = { finishDrag(true) },
                                    onDragCancel = { finishDrag(false) },
                                )
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    expandedIds = if (expanded) {
                                        expandedIds - preset.id
                                    } else {
                                        expandedIds + preset.id
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                LabIcons.ChevronDown(
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                )
                            }
                            if (renamingId == preset.id) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                IconButton(
                                    onClick = {
                                        if (renameText.trim().isNotEmpty()) {
                                            onRename(preset.id, renameText.trim())
                                            renamingId = null
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    LabIcons.Check(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (active) {
                                                onDeselect()
                                            } else {
                                                onSelect(preset)
                                            }
                                        },
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (active) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        text = "${preset.channels.size} canales",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { onOverwrite(preset) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    LabIcons.Save(
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        renamingId = preset.id
                                        renameText = preset.name
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    LabIcons.Pencil(
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { deleteTarget = preset },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    LabIcons.X(
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        if (expanded) {
                            preset.channels.forEach { channel ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 48.dp, top = 2.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(3.dp),
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = typeLabel(channel.type),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            TextButton(
                onClick = {
                    nameText = ""
                    showNameDialog = true
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                LabIcons.Plus(
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Nuevo preset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Borrar preset",
            body = "Se borra «${target.name}» para siempre.",
            confirm = "Borrar",
            onConfirm = {
                onDelete(target.id)
                expandedIds = expandedIds - target.id
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveNew(nameText.trim())
                        nameText = ""
                        showNameDialog = false
                    },
                    enabled = nameText.isNotBlank(),
                ) {
                    Text(
                        text = "Guardar",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            title = { Text("Nuevo preset") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "nombre",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(12.dp),
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirm,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        title = { Text(title) },
        text = { Text(body) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
    )
}
