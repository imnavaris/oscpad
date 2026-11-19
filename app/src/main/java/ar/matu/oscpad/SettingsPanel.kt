package ar.matu.oscpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPanel(
    net: NetConfig,
    localIp: String,
    dim: Boolean,
    decComma: Boolean,
    theme: String,
    channelCount: Int,
    onDimChange: (Boolean) -> Unit,
    onDecCommaChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onChange: (NetConfig) -> Unit,
    onTestAll: () -> Unit,
    onClearAll: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var host by remember { mutableStateOf(net.host) }
    var port by remember { mutableStateOf(net.port.toString()) }
    var prefixText by remember { mutableStateOf(net.prefix) }
    var showClearDialog by remember { mutableStateOf(false) }
    var hostTouched by remember { mutableStateOf(false) }
    var portTouched by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val portValue = port.toIntOrNull()
    val hostOk = host.isNotBlank()
    val portOk = portValue != null && portValue in 1..65535

    fun push() {
        val parsedPort = port.toIntOrNull() ?: return
        if (host.isBlank()) return
        if (parsedPort !in 1..65535) return
        onChange(net.copy(host = host.trim(), port = parsedPort, prefix = prefixText.trim()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Field(
            label = "Este celular",
            value = localIp,
            isError = false,
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            onChange = {},
        )
        SectionTitle("Conexión")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(
                label = "IP de la PC",
                value = host,
                isError = !hostOk,
                modifier = Modifier
                    .weight(1.5f)
                    .onFocusChanged { state ->                        if (state.isFocused) {
                            hostTouched = true
                        } else if (hostTouched) {
                            push()
                        }
                    },
                onChange = { host = it },
            )
            Field(
                label = "Puerto",
                value = port,
                isError = !portOk,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            portTouched = true
                        } else if (portTouched) {
                            push()
                        }
                    },
                numeric = true,
                onChange = { port = it.filter(Char::isDigit).take(5) },
            )
        }
        Field(
            label = "Prefijo de las direcciones (opcional)",
            value = prefixText,
            isError = false,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) push()
                },
            onChange = {
                prefixText = sanitizeName(it) ?: ""
                push()
            },
        )
        Button(
            onClick = onTestAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "Probar canales",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        SectionTitle("Datos")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onExport,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Exportar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                onClick = onImport,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Importar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TextButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Borrar canales",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            onClearAll()
                        },
                    ) {
                        Text(
                            text = "Borrar todo",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(
                            text = "Cancelar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                title = { Text("Borrar canales") },
                text = {
                    Text(
                        text = "Se borran $channelCount canales. Podés deshacerlo después.",
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        val context = LocalContext.current
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        SectionTitle("Pantalla")
        Text(
            text = "Tema",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Sistema", "dark" to "Oscuro", "light" to "Claro").forEach { (key, label) ->
                val selected = theme == key
                TextButton(onClick = { onThemeChange(key) }) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Text(
            text = "Decimales",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Punto (0.50)", true to "Coma (0,50)").forEach { (key, label) ->
                TextButton(onClick = { onDecCommaChange(key) }) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (decComma == key) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Brillo bajo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = dim, onCheckedChange = onDimChange)
        }
        Text(
            text = "Baja la pantalla al 5% para no encandilarte en el escenario.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "oscpad v$version",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    readOnly: Boolean = false,
    next: FocusRequester? = null,
    onChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        readOnly = readOnly,
        enabled = !readOnly,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = if (next != null) ImeAction.Next else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onNext = { next?.requestFocus() },
            onDone = { focusManager.clearFocus() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            errorTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor = MaterialTheme.colorScheme.error,
        ),
    )
}
