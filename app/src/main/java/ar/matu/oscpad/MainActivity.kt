package ar.matu.oscpad

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val OscScheme = darkColorScheme(
    primary = Color(0xFF39FF14),
    onPrimary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFA6A6A6),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF1A1A1A),
    error = Color(0xFFFF5252),
    onError = Color(0xFF0A0A0A),
)

private val Ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = OscScheme, typography = appTypography()) {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var net by remember { mutableStateOf(NetStore.load(context)) }
    var channels by remember { mutableStateOf(ChannelStore.load(context)) }
    var presets by remember { mutableStateOf(PresetStore.load(context)) }
    var activePresetId by remember { mutableStateOf<Long?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var presetsOpen by remember { mutableStateOf(false) }
    var connOk by remember { mutableStateOf(true) }
    var connAt by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sender = remember(net.host, net.port) { OscSender(net.host, net.port) }

    DisposableEffect(sender) {
        onDispose { sender.close() }
    }

    fun replace(list: List<Channel>, next: Channel) =
        list.map { if (it.id == next.id) next else it }

    fun live(next: Channel) {
        channels = replace(channels, next)
    }

    fun done(next: Channel) {
        val updated = replace(channels, next)
        channels = updated
        ChannelStore.save(context, updated)
        activePresetId = null
    }

    fun remove(id: Long) {
        val idx = channels.indexOfFirst { it.id == id }
        if (idx == -1) return
        val removed = channels[idx]
        val updated = channels.filter { it.id != id }
        channels = updated
        ChannelStore.save(context, updated)
        activePresetId = null
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Canal borrado",
                actionLabel = "Deshacer",
            )
            if (result == SnackbarResult.ActionPerformed && channels.none { it.id == removed.id }) {
                val back = channels.toMutableList()
                back.add(idx.coerceIn(0, back.size), removed)
                channels = back
                ChannelStore.save(context, back)
            }
        }
    }

    fun markSent(ok: Boolean) {
        connOk = ok
        connAt = System.currentTimeMillis()
    }

    fun send(address: String, value: Float) {
        if (!address.startsWith('/')) return
        scope.launch(Dispatchers.IO) {
            val ok = sender.send(Osc.encodeFloat(address, value))
            launch(Dispatchers.Main) { markSent(ok) }
        }
    }

    fun sendMany(address: String, values: FloatArray) {
        if (!address.startsWith('/')) return
        scope.launch(Dispatchers.IO) {
            val ok = sender.send(Osc.encodeFloats(address, values))
            launch(Dispatchers.Main) { markSent(ok) }
        }
    }

    fun addChannel() {
        if (channels.size >= 8) return
        val id = ChannelStore.nextId(context)
        val updated = channels + Channel(id = id, name = "canal$id")
        channels = updated
        ChannelStore.save(context, updated)
        activePresetId = null
        scope.launch {
            listState.animateScrollToItem((updated.size - 1) * 2)
        }
    }

    val sensorSpecs = remember { listSensors(context) }
    val feeds = remember { mutableMapOf<Long, SensorFeed>() }
    val channelsState = rememberUpdatedState(channels)
    val sensorKeys = channels.filter { it.type == "sensor" }
        .map {
            Triple(it.id, it.name, Triple(it.sensorKind, it.sensorAxis, it.rateMs))
        }

    DisposableEffect(sensorKeys) {
        val wanted = sensorKeys.map { it.first to it.third }.toMap()
        wanted.forEach { (id, key) ->
            if (feeds[id] != null) return@forEach
            val channel = channelsState.value.first { it.id == id }
            val feed = SensorFeed(context, key.first, key.second, key.third) { raw ->
                channelsState.value.find { it.id == id }?.let { ch ->
                    val v = raw.coerceIn(ch.min, ch.max)
                    live(ch.copy(level = v))
                    send(ch.address(), v)
                }
            }
            feeds[id] = feed
            feed.start()
        }
        onDispose {
            feeds.values.forEach { it.stop() }
            feeds.clear()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val wifiLock = remember {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "oscpad:live")
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!wifiLock.isHeld) wifiLock.acquire()
                Lifecycle.Event.ON_PAUSE -> if (wifiLock.isHeld) wifiLock.release()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (wifiLock.isHeld) wifiLock.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (connOk) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${net.host}:${net.port}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = {
                        presetsOpen = !presetsOpen
                        if (presetsOpen) settingsOpen = false
                    }) {
                        LabIcons.Stack(
                            tint = if (presetsOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = {
                        settingsOpen = !settingsOpen
                        if (settingsOpen) presetsOpen = false
                    }) {
                        LabIcons.Gear(
                            tint = if (settingsOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (settingsOpen || presetsOpen) {
                Spacer(Modifier.height(16.dp))
                val reveal = remember { Animatable(0f) }
                LaunchedEffect(reveal) {
                    reveal.animateTo(1f, tween(240, easing = Ease))
                }
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = reveal.value
                        translationY = (1f - reveal.value) * 12.dp.toPx()
                    },
                ) {
                    if (presetsOpen) {
                        PresetsPanel(
                            presets = presets,
                            activeId = activePresetId,
                            onSelect = { preset ->
                                channels = preset.channels
                                net = preset.net
                                ChannelStore.save(context, preset.channels)
                                NetStore.save(context, preset.net)
                                activePresetId = preset.id
                            },
                            onSaveCurrent = { name ->
                                val preset = Preset(
                                    id = PresetStore.nextPresetId(context),
                                    name = name.trim(),
                                    channels = channels,
                                    net = net,
                                )
                                val updated = presets + preset
                                presets = updated
                                PresetStore.save(context, updated)
                                activePresetId = preset.id
                            },
                            onDelete = { id ->
                                val updated = presets.filter { it.id != id }
                                presets = updated
                                PresetStore.save(context, updated)
                                if (activePresetId == id) activePresetId = null
                            },
                        )
                    } else {
                        SettingsPanel(
                            net = net,
                            localIp = remember { localIpv4() ?: "?" },
                            onChange = {
                                net = it
                                NetStore.save(context, it)
                                activePresetId = null
                            },
                            onTestAll = {
                                channels.forEach { send(it.address(), it.current()) }
                            },
                            onClearAll = {
                                channels = emptyList()
                                ChannelStore.save(context, emptyList())
                                activePresetId = null
                            },
                        )
                    }
                }
            } else if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.clickable(onClick = ::addChannel),
                        contentAlignment = Alignment.Center,
                    ) {
                        LabIcons.Plus(
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(120.dp),
                        )
                    }
                }
            } else {
                val currentPage by remember(channels) {
                    derivedStateOf {
                        val key = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.key is Long }?.key
                        channels.indexOfFirst { it.id == key } + 1
                    }
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    state = listState,
                ) {
                    channels.forEachIndexed { i, channel ->
                        item(key = channel.id) {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .fillParentMaxHeight(),
                            ) {
                                ChannelModule(
                                    channel = channel,
                                    taken = channels.map { it.name }.toSet() - channel.name,
                                    sensors = sensorSpecs,
                                    onLive = ::live,
                                    onDone = ::done,
                                    onDelete = { remove(channel.id) },
                                    onSend = ::send,
                                    onSendMany = ::sendMany,
                                )
                            }
                        }
                        if (i < channels.lastIndex || channels.size < 8) {
                            item(key = "div_${channel.id}") {
                                DividerBar(modifier = Modifier.fillParentMaxHeight())
                            }
                        }
                    }
                    if (channels.size < 8) {
                        item(key = "add") {
                            AddPage(
                                count = channels.size,
                                onAdd = ::addChannel,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .fillParentMaxHeight(),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${currentPage.coerceAtLeast(1)}/${channels.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = Mono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SnackbarHost(snackbarHostState)
        }
    }
}

@Composable
private fun DividerBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(25.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .padding(vertical = 32.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun AddPage(count: Int, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                LabIcons.Plus(
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$count/8",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
