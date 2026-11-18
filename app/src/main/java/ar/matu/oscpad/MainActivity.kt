package ar.matu.oscpad

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val OscScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF0B0C10),
    background = Color(0xFF090A0F),
    onBackground = Color(0xFFE7E7EA),
    surface = Color(0xFF090A0F),
    onSurface = Color(0xFFE7E7EA),
    surfaceVariant = Color(0xFF15161D),
    onSurfaceVariant = Color(0xFF9A9AA5),
    outline = Color(0xFF262730),
    outlineVariant = Color(0xFF1B1C24),
    error = Color(0xFFF87171),
    onError = Color(0xFF0B0C10),
)

private val Ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = OscScheme) {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(ConfigStore.load(context)) }
    var fader by remember { mutableFloatStateOf(0.5f) }
    var toggle by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    val throttle = remember { Throttle() }
    val sender = remember(config.host, config.port) { OscSender(config.host, config.port) }

    DisposableEffect(sender) {
        onDispose { sender.close() }
    }

    fun send(address: String, value: Float) {
        if (!address.startsWith('/')) return
        scope.launch(Dispatchers.IO) { sender.send(Osc.encodeFloat(address, value)) }
    }

    fun scaled(level: Float) = config.min + (config.max - config.min) * level

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "oscpad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${config.host}:${config.port}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(if (settingsOpen) 16.dp else 0.dp))

            Box(
                modifier = if (settingsOpen) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                },
                contentAlignment = Alignment.Center,
            ) {
                Controls(
                    fader = fader,
                    toggle = toggle,
                    onFader = { level ->
                        fader = level
                        if (throttle.shouldSend()) send(config.addrFader, scaled(level))
                    },
                    onRelease = { send(config.addrFader, scaled(fader)) },
                    onToggle = { state ->
                        toggle = state
                        send(config.addrButton, if (state) 1f else 0f)
                    },
                )
            }

            TextButton(
                onClick = { settingsOpen = !settingsOpen },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (settingsOpen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (settingsOpen) {
                val reveal = remember { Animatable(0f) }
                LaunchedEffect(reveal) {
                    reveal.animateTo(1f, tween(240, easing = Ease))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            alpha = reveal.value
                            translationY = (1f - reveal.value) * 12.dp.toPx()
                        }
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsPanel(
                        config = config,
                        onChange = {
                            config = it
                            ConfigStore.save(context, it)
                        },
                        onTest = {
                            send(config.addrFader, scaled(fader))
                            send(config.addrButton, if (toggle) 1f else 0f)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Controls(
    fader: Float,
    toggle: Boolean,
    onFader: (Float) -> Unit,
    onRelease: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label("Fader")
            Spacer(Modifier.height(14.dp))
            Fader(value = fader, onChange = onFader, onRelease = onRelease)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label("Boton")
            Spacer(Modifier.height(14.dp))
            ToggleControl(on = toggle, onToggle = onToggle)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
