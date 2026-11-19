package ar.matu.oscpad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("oscpad", MODE_PRIVATE)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                prefs.edit()
                    .putString("last_crash", android.util.Log.getStackTraceString(error).take(2000))
                    .apply()
            } catch (e: Exception) {
            }
            defaultHandler?.uncaughtException(thread, error)
        }
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
    var channels by remember {
        mutableStateOf(ChannelStore.load(context).let { loaded ->
            val prefs = context.getSharedPreferences("oscpad", Context.MODE_PRIVATE)
            if (prefs.getInt("order_version", 0) >= 3) {
                loaded
            } else {
                val sorted = loaded.sortedWith(
                    compareBy(
                        { channelSortKey(it.name).first },
                        { channelSortKey(it.name).second },
                        { it.id },
                    ),
                )
                if (sorted.map { it.id } != loaded.map { it.id }) {
                    ChannelStore.save(context, sorted)
                }
                prefs.edit().putInt("order_version", 3).apply()
                sorted
            }
        })
    }
    var localIp by remember { mutableStateOf(localIpv4() ?: "?") }
    var presets by remember { mutableStateOf(PresetStore.load(context)) }
    var activePresetId by remember { mutableStateOf<Long?>(null) }
    var tab by remember { mutableStateOf(1) }
    val history = remember { mutableStateListOf(1) }
    val activity = context as? Activity
    var rangeClip by remember { mutableStateOf<List<Float>?>(null) }
    val activePreset = presets.find { it.id == activePresetId }
    val activeDirty = activePreset != null &&
        (activePreset.channels != channels || activePreset.net != net)
    var animJob by remember { mutableStateOf<Job?>(null) }

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    var dim by remember {
        mutableStateOf(
            context.getSharedPreferences("oscpad", Context.MODE_PRIVATE).getBoolean("dim", false),
        )
    }

    DisposableEffect(dim) {
        val window = activity?.window
        if (window != null) {
            val attrs = window.attributes
            attrs.screenBrightness = if (dim) 0.05f else -1f
            window.attributes = attrs
        }
        onDispose { }
    }

    fun selectTab(i: Int) {
        if (i != tab) {
            tab = i
            history.add(i)
            if (history.size > 50) history.removeFirst()
        }
    }

    BackHandler {
        if (history.size > 1) {
            history.removeLast()
            tab = history.last()
        } else {
            activity?.finish()
        }
    }
    var connOk by remember { mutableStateOf(true) }
    var connAt by remember { mutableStateOf(0L) }
    var crash by remember {
        mutableStateOf(
            context.getSharedPreferences("oscpad", Context.MODE_PRIVATE)
                .getString("last_crash", null),
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sender = remember(net.host, net.port) { OscSender(net.host, net.port) }

    DisposableEffect(sender) {
        onDispose { sender.close() }
    }

    var importCandidate by remember { mutableStateOf<OscBackup?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val raw = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            } catch (e: Exception) {
                null
            }
            val backup = raw?.let { decodeBackup(it) }
            launch(Dispatchers.Main) {
                if (backup == null) {
                    snackbarHostState.showSnackbar(
                        message = "Archivo inválido",
                        duration = SnackbarDuration.Short,
                    )
                } else {
                    importCandidate = backup
                }
            }
        }
    }

    fun exportBackup() {
        scope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(context.cacheDir, "oscpad-backup.json")
                file.writeText(encodeBackup(OscBackup(channels, presets, net)))
                val uri = FileProvider.getUriForFile(
                    context,
                    "ar.matu.oscpad.fileprovider",
                    file,
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                launch(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Exportar backup"))
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        message = "No se pudo exportar",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
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
    }

    fun remove(id: Long) {
        val idx = channels.indexOfFirst { it.id == id }
        if (idx == -1) return
        val removed = channels[idx]
        val beforeIds = channels.map { it.id }
        val updated = channels.filter { it.id != id }
        channels = updated
        ChannelStore.save(context, updated)
        scope.launch {
            launch {
                delay(5000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = "Canal borrado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed && channels.none { it.id == removed.id }) {
                val back = channels.toMutableList()
                val at = if (channels.map { it.id } == beforeIds.filter { it != id }) {
                    idx.coerceIn(0, back.size)
                } else {
                    back.size
                }
                back.add(at, removed)
                channels = back
                ChannelStore.save(context, back)
            }
        }
    }

    fun markSent(ok: Boolean) {
        val now = System.currentTimeMillis()
        if (ok != connOk || now - connAt > 250) {
            connOk = ok
            connAt = now
        }
    }

    suspend fun sendNow(address: String, value: Float): Boolean {
        if (!address.startsWith('/')) return false
        val ok = kotlinx.coroutines.withContext(Dispatchers.IO) {
            sender.send(Osc.encodeFloat(address, value))
        }
        markSent(ok)
        return ok
    }

    suspend fun sendManyNow(address: String, values: FloatArray): Boolean {
        if (!address.startsWith('/')) return false
        val ok = kotlinx.coroutines.withContext(Dispatchers.IO) {
            sender.send(Osc.encodeFloats(address, values))
        }
        markSent(ok)
        return ok
    }

    fun send(address: String, value: Float) {
        if (!address.startsWith('/')) return
        scope.launch { sendNow(address, value) }
    }

    fun sendMany(address: String, values: FloatArray) {
        if (!address.startsWith('/')) return
        scope.launch { sendManyNow(address, values) }
    }

    fun addChannel() {
        if (channels.size >= 8) return
        val id = ChannelStore.nextId(context)
        val updated = channels + Channel(id = id, name = "canal$id")
        channels = updated
        ChannelStore.save(context, updated)
        scope.launch {
            listState.animateScrollToItem((updated.size - 1) * 2)
        }
    }

    fun addDemo() {
        if (channels.isNotEmpty()) return
        val demo = listOf(
            Channel(id = ChannelStore.nextId(context), name = "fader1"),
            Channel(id = ChannelStore.nextId(context), name = "boton1", type = "button"),
            Channel(id = ChannelStore.nextId(context), name = "xy1", type = "xy"),
        )
        channels = demo
        ChannelStore.save(context, demo)
        selectTab(1)
    }

    fun duplicate(channel: Channel) {
        if (channels.size >= 8) return
        var base = channel.name + "_2"
        var n = 2
        val names = channels.map { it.name }.toSet()
        while (base in names) {
            n++
            base = channel.name + "_$n"
        }
        val copy = channel.copy(id = ChannelStore.nextId(context), name = base)
        val updated = channels + copy
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
        .map { it.id to (it.sensorKind to it.rateMs) }

    DisposableEffect(sensorKeys) {
        val wanted = sensorKeys.toMap()
        feeds.keys.filter { it !in wanted }.forEach { feeds.remove(it)?.stop() }
        wanted.forEach { (id, key) ->
            if (feeds[id] == null) {
                val channel = channelsState.value.firstOrNull { it.id == id } ?: return@forEach
                val feed = SensorFeed(context, key.first, key.second) { norms ->
                    channelsState.value.find { it.id == id }?.let { ch ->
                        val mapped = norms.map { n ->
                            ch.min + (ch.max - ch.min) * n.coerceIn(0f, 1f)
                        }
                        live(
                            ch.copy(
                                level = mapped[0],
                                level2 = mapped[1],
                                level3 = mapped[2],
                                level4 = mapped[3],
                            ),
                        )
                        sendMany(ch.address(net.prefix), ch.copy(
                            level = mapped[0],
                            level2 = mapped[1],
                            level3 = mapped[2],
                            level4 = mapped[3],
                        ).outputValues())
                    }
                }
                feeds[id] = feed
                feed.start()
            }
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
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

    var lastSubnetWarn by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                    val ip = localIpv4() ?: "?"
                    localIp = ip
                    if (!sameSubnet(ip, net.host) && lastSubnetWarn != ip + net.host) {
                        lastSubnetWarn = ip + net.host
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "La red cambió: revisá la IP en Ajustes",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                }
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
                .safeDrawingPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "tab",
                ) { t ->
                when (t) {
                    2 -> {
                        PresetsPanel(
                            presets = presets,
                            activeId = activePresetId,
                            onSelect = { preset ->
                                animJob?.cancel()
                                val from = channels
                                val to = preset.channels
                                activePresetId = preset.id
                                if (from.map { it.id } == to.map { it.id }) {
                                    animJob = scope.launch {
                                        val steps = 20
                                        for (s in 1..steps) {
                                            val t = s / steps.toFloat()
                                            channels = from.mapIndexed { i, c ->
                                                val d = to[i]
                                                if (c.type == "sensor") {
                                                    d
                                                } else {
                                                    c.copy(
                                                        level = lerp(c.level, d.level, t),
                                                        level2 = lerp(c.level2, d.level2, t),
                                                        level3 = lerp(c.level3, d.level3, t),
                                                        level4 = lerp(c.level4, d.level4, t),
                                                        on = if (s == steps) d.on else c.on,
                                                    )
                                                }
                                            }
                                            channels.forEach {
                                                if (it.type == "sensor") return@forEach
                                                if (it.compCount() > 1) {
                                                    sendMany(it.address(net.prefix), it.outputValues())
                                                } else {
                                                    send(it.address(net.prefix), it.current())
                                                }
                                            }
                                            delay(15)
                                        }
                                        channels = to
                                        net = preset.net
                                        ChannelStore.save(context, to)
                                        NetStore.save(context, preset.net)
                                    }
                                } else {
                                    channels = to
                                    net = preset.net
                                    ChannelStore.save(context, to)
                                    NetStore.save(context, preset.net)
                                }
                            },
                            onDeselect = { activePresetId = null },
                            onSaveNew = { name ->
                                val preset = Preset(
                                    id = PresetStore.nextPresetId(context),
                                    name = name,
                                    channels = channels,
                                    net = net,
                                )
                                val updated = presets + preset
                                presets = updated
                                PresetStore.save(context, updated)
                                activePresetId = preset.id
                            },
                            onOverwrite = { preset ->
                                val updated = presets.map { p ->
                                    if (p.id == preset.id) p.copy(channels = channels, net = net) else p
                                }
                                presets = updated
                                PresetStore.save(context, updated)
                                activePresetId = preset.id
                            },
                            onDelete = { id ->
                                val idx = presets.indexOfFirst { it.id == id }
                                val target = presets.find { it.id == id }
                                if (target != null) {
                                    val beforeIds = presets.map { it.id }
                                    val updated = presets.filter { it.id != id }
                                    presets = updated
                                    PresetStore.save(context, updated)
                                    if (activePresetId == id) activePresetId = null
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Preset borrado",
                                            actionLabel = "Deshacer",
                                            duration = SnackbarDuration.Indefinite,
                                        )
                                        if (result == SnackbarResult.ActionPerformed &&
                                            presets.none { it.id == id }) {
                                            val back = presets.toMutableList()
                                            val at = if (presets.map { it.id } == beforeIds.filter { it != id }) {
                                                idx.coerceIn(0, back.size)
                                            } else {
                                                back.size
                                            }
                                            back.add(at, target)
                                            presets = back
                                            PresetStore.save(context, back)
                                        }
                                    }
                                }
                            },
                            onRename = { id, name ->
                                val updated = presets.map { p ->
                                    if (p.id == id) p.copy(name = name) else p
                                }
                                presets = updated
                                PresetStore.save(context, updated)
                            },
                            onReorder = { reordered ->
                                presets = reordered
                                PresetStore.save(context, reordered)
                                if (activePresetId != null && reordered.none { it.id == activePresetId }) {
                                    activePresetId = null
                                }
                            },
                            onGoChannels = {
                                selectTab(1)
                                if (channels.isEmpty()) addChannel()
                            },
                            activeDirty = activeDirty,
                        )
                    }
                    3 -> {
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SettingsPanel(
                            net = net,
                            localIp = localIp,
                            dim = dim,
                            onDimChange = {
                                dim = it
                                context.getSharedPreferences("oscpad", Context.MODE_PRIVATE)
                                    .edit().putBoolean("dim", it).apply()
                            },
                            onChange = {
                                net = it
                                NetStore.save(context, it)
                            },
                            onTestAll = {
                                scope.launch {
                                    var ok = 0
                                    channels.forEach {
                                        val sent = if (it.compCount() > 1) {
                                            sendManyNow(it.address(net.prefix), it.outputValues())
                                        } else {
                                            sendNow(it.address(net.prefix), it.current())
                                        }
                                        if (sent) ok++
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "$ok/${channels.size} enviados",
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                            onClearAll = {
                                channels = emptyList()
                                ChannelStore.save(context, emptyList())
                                activePresetId = null
                            },
                            onExport = ::exportBackup,
                            onImport = { importLauncher.launch("application/json") },
                        )
                        }
                    }
                    0 -> {
                        var gridDragId by remember { mutableStateOf<Long?>(null) }
                        var gridDragOffset by remember { mutableStateOf(Offset.Zero) }
                        var gridDragFrom by remember { mutableStateOf(-1) }
                        var gridSlots by remember { mutableStateOf(listOf<Rect>()) }
                        val gridRects = remember { mutableMapOf<Long, Rect>() }
                        var draggingCells by remember { mutableStateOf<List<Channel>?>(null) }
                        val shownChannels = draggingCells ?: channels

                        fun finishGridDrag(commit: Boolean) {
                            val order = draggingCells
                            draggingCells = null
                            gridDragId = null
                            gridDragOffset = Offset.Zero
                            gridDragFrom = -1
                            if (commit && order != null && order.map { it.id } != channels.map { it.id }) {
                                channels = order
                                ChannelStore.save(context, order)
                                activePresetId = null
                            }
                        }

                        fun moveGridDrag(amount: Offset) {
                            val list = draggingCells ?: return
                            val from = gridDragFrom
                            if (from == -1 || gridSlots.size != list.size) return
                            gridDragOffset += amount
                            val point = gridSlots[from].center + gridDragOffset
                            val target = gridSlots.indexOfFirst { it.contains(point) }
                            if (target != -1 && target != from) {
                                val next = list.toMutableList()
                                val item = next.removeAt(from)
                                next.add(target, item)
                                draggingCells = next
                                gridDragOffset -= (gridSlots[target].topLeft - gridSlots[from].topLeft)
                                gridDragFrom = target
                            }
                        }

                        if (channels.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Sin canales: creá uno en Canales",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = ::addDemo,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    ) {
                                        Text(
                                            text = "Crear demo",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "fader + botón + XY para probar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            val portrait = LocalConfiguration.current.orientation ==
                                Configuration.ORIENTATION_PORTRAIT
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(if (portrait) 2 else 4),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                userScrollEnabled = gridDragId == null,
                            ) {
                                items(shownChannels, key = { it.id }) { channel ->
                                    Column(
                                        modifier = Modifier
                                            .then(if (gridDragId == channel.id) Modifier else Modifier.animateItem())
                                            .onGloballyPositioned {
                                                gridRects[channel.id] = Rect(
                                                    it.positionInWindow(),
                                                    Size(
                                                        it.size.width.toFloat(),
                                                        it.size.height.toFloat(),
                                                    ),
                                                )
                                            }
                                            .graphicsLayer {
                                                if (gridDragId == channel.id) {
                                                    translationX = gridDragOffset.x
                                                    translationY = gridDragOffset.y
                                                }
                                            }
                                            .zIndex(if (gridDragId == channel.id) 1f else 0f)
                                            .pointerInput(channel.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        if (gridRects.size == channels.size) {
                                                            val slots = channels.mapNotNull { gridRects[it.id] }
                                                            if (slots.size == channels.size) {
                                                                gridSlots = slots
                                                                draggingCells = channels
                                                                gridDragId = channel.id
                                                                gridDragOffset = Offset.Zero
                                                                gridDragFrom = channels.indexOfFirst { it.id == channel.id }
                                                            }
                                                        } else {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(
                                                                    message = "Deslizá la grilla primero",
                                                                    duration = SnackbarDuration.Short,
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onDrag = { _, amount -> moveGridDrag(amount) },
                                                    onDragEnd = { finishGridDrag(true) },
                                                    onDragCancel = { finishGridDrag(false) },
                                                )
                                            }
                                            .clickable {
                                                selectTab(1)
                                                scope.launch {
                                                    val idx = shownChannels.indexOfFirst { it.id == channel.id }
                                                    listState.animateScrollToItem(idx * 2)
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = typeLabel(channel.type),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (channel.compCount() > 1) {
                                            val raws = when (channel.type) {
                                                "xy" -> channel.xyValues()
                                                "color" -> floatArrayOf(
                                                    channel.level,
                                                    channel.level2,
                                                    channel.level3,
                                                )
                                                else -> floatArrayOf(
                                                    channel.level,
                                                    channel.level2,
                                                    channel.level3,
                                                    channel.level4,
                                                )
                                            }
                                            raws.forEachIndexed { i, raw ->
                                                OverviewCompRow(
                                                    letter = channel.compLetter(i),
                                                    tint = axisTint(i),
                                                    value = raw,
                                                    enabled = channel.compEnabled(i),
                                                    onToggle = {
                                                        val next = channel.toggleComp(i)
                                                        channels = channels.map {
                                                            if (it.id == channel.id) next else it
                                                        }
                                                        ChannelStore.save(context, channels)
                                                    },
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = channelValueText(channel),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontFamily = Mono,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = channel.address(net.prefix),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = Mono,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        if (channels.isEmpty()) {
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
                                    onDuplicate = { duplicate(channel) },
                                    rangeClip = rangeClip,
                                    onCopyRange = { rangeClip = it },
                                    prefix = net.prefix,
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
                }
                }
                }
            }
            }

            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                )
            }

            importCandidate?.let { backup ->
                AlertDialog(
                    onDismissRequest = { importCandidate = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                channels = backup.channels
                                presets = backup.presets
                                net = backup.net
                                ChannelStore.save(context, backup.channels)
                                PresetStore.save(context, backup.presets)
                                NetStore.save(context, backup.net)
                                activePresetId = null
                                importCandidate = null
                            },
                        ) {
                            Text(
                                text = "Reemplazar",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { importCandidate = null }) {
                            Text(
                                text = "Cancelar",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    title = { Text("Importar backup") },
                    text = {
                        Text(
                            text = "${backup.channels.size} canales, " +
                                "${backup.presets.size} presets. " +
                                "Reemplaza todo lo actual.",
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }

            crash?.let { stack ->
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(
                            onClick = {
                                context.getSharedPreferences("oscpad", Context.MODE_PRIVATE)
                                    .edit().remove("last_crash").apply()
                                crash = null
                            },
                        ) {
                            Text(
                                text = "Borrar",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    title = { Text("La app se cerró sola") },
                    text = {
                        Text(
                            text = stack,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = Mono,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                TabItem(0, tab, ::selectTab, "Inicio") {
                    LabIcons.Grid(
                        tint = if (tab == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
                TabItem(1, tab, ::selectTab, "Canales") {
                    LabIcons.Sliders(
                        tint = if (tab == 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
                TabItem(2, tab, ::selectTab, "Presets") {
                    LabIcons.Stack(
                        tint = if (tab == 2) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
                TabItem(3, tab, ::selectTab, "Ajustes") {
                    LabIcons.Gear(
                        tint = if (tab == 3) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    index: Int,
    tab: Int,
    onSelect: (Int) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = tab == index,
        onClick = { onSelect(index) },
        icon = icon,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        ),
    )
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
                style = MaterialTheme.typography.titleMedium,
                fontFamily = Mono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Agregar nuevo canal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
