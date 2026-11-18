package ar.matu.oscpad

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    onGoChannels: () -> Unit,
) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var renamingId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Preset?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }

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
                Spacer(Modifier.height(8.dp))
                NewPresetButton(onClick = {
                    nameText = ""
                    showNameDialog = true
                })
            }
        } else {
            presets.forEach { preset ->
                val active = preset.id == activeId
                val expanded = expandedId == preset.id
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (active) {
                                    onDeselect()
                                    if (expanded) expandedId = null
                                } else {
                                    onSelect(preset)
                                    expandedId = preset.id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                            Column(modifier = Modifier.weight(1f)) {
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
                                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
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
            TextButton(
                onClick = {
                    nameText = ""
                    showNameDialog = true
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                NewPresetContent()
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
                if (expandedId == target.id) expandedId = null
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
private fun NewPresetButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        NewPresetContent()
    }
}

@Composable
private fun NewPresetContent() {
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
