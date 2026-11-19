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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPanel(
    net: NetConfig,
    localIp: String,
    onChange: (NetConfig) -> Unit,
    onTestAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    var host by remember { mutableStateOf(net.host) }
    var port by remember { mutableStateOf(net.port.toString()) }
    var confirmClear by remember { mutableStateOf(false) }
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
        onChange(net.copy(host = host.trim(), port = parsedPort))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Field(
            label = "Este celu",
            value = localIp,
            isError = false,
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            onChange = {},
        )
        if (!sameSubnet(localIp, host)) {
            Text(
                text = "Red distinta al destino: no va a llegar nada",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(
                label = "IP",
                value = host,
                isError = !hostOk,
                modifier = Modifier
                    .weight(1.5f)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
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
        Button(
            onClick = onTestAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
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
        TextButton(
            onClick = {
                if (confirmClear) {
                    confirmClear = false
                    onClearAll()
                } else {
                    confirmClear = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (confirmClear) "Tocar de nuevo para borrar canales" else "Borrar canales",
                style = MaterialTheme.typography.labelLarge,
                color = if (confirmClear) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    readOnly: Boolean = false,
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
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
