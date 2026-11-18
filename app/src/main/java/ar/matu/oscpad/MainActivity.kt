package ar.matu.oscpad

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val OscScheme = darkColorScheme(
    primary = Color(0xFF39FF14),
    onPrimary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF8A8A8A),
    outline = Color(0xFF242424),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dividerPx = with(density) { 25.dp.toPx() }
    var net by remember { mutableStateOf(NetStore.load(context)) }
    var channels by remember { mutableStateOf(ChannelStore.load(context)) }
    var settingsOpen by remember { mutableStateOf(false) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOrigIdx by remember { mutableStateOf(0) }
    var dragTotalX by remember { mutableStateOf(0f) }
    var cellWpx by remember { mutableStateOf(0f) }
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
    }

    fun remove(id: Long) {
        val updated = channels.filter { it.id != id }
        channels = updated
        ChannelStore.save(context, updated)
    }

    fun send(address: String, value: Float) {
        if (!address.startsWith('/')) return
        scope.launch(Dispatchers.IO) { sender.send(Osc.encodeFloat(address, value)) }
    }

    fun addChannel() {
        if (channels.size >= 8) return
        val id = ChannelStore.nextId(context)
        val updated = channels + Channel(id = id, name = "canal$id")
        channels = updated
        ChannelStore.save(context, updated)
    }

    fun moveDragged(targetIdx: Int) {
        val id = dragId ?: return
        val list = channels.toMutableList()
        val cur = list.indexOfFirst { it.id == id }
        val target = targetIdx.coerceIn(0, list.lastIndex)
        if (cur == -1 || cur == target) return
        list.add(target, list.removeAt(cur))
        channels = list
    }

    fun onTitleDragStart(channel: Channel) {
        dragId = channel.id
        dragOrigIdx = channels.indexOfFirst { it.id == channel.id }
        dragTotalX = 0f
    }

    fun onTitleDrag(dx: Float) {
        if (cellWpx <= 0f) return
        dragTotalX += dx
        moveDragged(dragOrigIdx + (dragTotalX / (cellWpx + dividerPx)).roundToInt())
    }

    fun onTitleDragEnd() {
        dragId = null
        dragTotalX = 0f
        ChannelStore.save(context, channels)
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
                    Text(
                        text = "${net.host}:${net.port}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { settingsOpen = !settingsOpen }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Ajustes",
                            tint = if (settingsOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            if (settingsOpen) {
                Spacer(Modifier.height(16.dp))
                val reveal = remember { Animatable(0f) }
                LaunchedEffect(reveal) {
                    reveal.animateTo(1f, tween(240, easing = Ease))
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer {
                            alpha = reveal.value
                            translationY = (1f - reveal.value) * 12.dp.toPx()
                        },
                ) {
                    SettingsPanel(
                        net = net,
                        onChange = {
                            net = it
                            NetStore.save(context, it)
                        },
                        onTestAll = {
                            channels.forEach { send(it.address(), it.current()) }
                        },
                        onClearAll = {
                            channels = emptyList()
                            ChannelStore.save(context, emptyList())
                        },
                    )
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
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar canal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(120.dp),
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
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
                    userScrollEnabled = dragId == null,
                ) {
                    channels.forEachIndexed { i, channel ->
                        item(key = channel.id) {
                            val idx = channels.indexOfFirst { it.id == channel.id }
                            val active = dragId == channel.id
                            val tx = if (active) {
                                dragTotalX - (idx - dragOrigIdx) * (cellWpx + dividerPx)
                            } else {
                                0f
                            }
                            Box(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .fillParentMaxHeight()
                                    .onSizeChanged { if (it.width > 0) cellWpx = it.width.toFloat() }
                                    .graphicsLayer { translationX = tx }
                                    .zIndex(if (active) 1f else 0f)
                                    .then(if (dragId != null) Modifier.animateItem() else Modifier),
                            ) {
                                ChannelModule(
                                    channel = channel,
                                    taken = channels.map { it.name }.toSet() - channel.name,
                                    onLive = ::live,
                                    onDone = ::done,
                                    onDelete = { remove(channel.id) },
                                    onSend = ::send,
                                    onTitleDragStart = { onTitleDragStart(channel) },
                                    onTitleDrag = ::onTitleDrag,
                                    onTitleDragEnd = ::onTitleDragEnd,
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
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Agregar canal",
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
