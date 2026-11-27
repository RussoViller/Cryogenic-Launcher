/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.ui.screens.game.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSegmentedTabs
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.screens.game.elements.log_parser.LogHighlighter
import com.russoviller.cryogenic.utils.copyText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

/** 游戏日志的最大保留行数 */
private const val MAX_LOG_LINES = 2048

internal const val TAB_ALL = 0
internal const val TAB_IMPORTANT = 1
internal const val TAB_WARN = 2
internal const val TAB_ERROR = 3

private val WarnColor = Color(0xFFFFC857)

/**
 * 单条游戏日志
 */
internal class LogLine(
    val id: Long,
    val text: AnnotatedString,
    val raw: String,
    val kind: LogClassifier.Kind,
    val headline: Boolean
) {
    val important: Boolean get() = kind == LogClassifier.Kind.IMPORTANT || headline
    fun inTab(tab: Int): Boolean = when (tab) {
        TAB_IMPORTANT -> important
        TAB_WARN -> kind == LogClassifier.Kind.WARN
        TAB_ERROR -> kind == LogClassifier.Kind.ERROR
        else -> true
    }
}

/** Estado da caixa de log (linhas, contadores, aba e busca). Só é alterado na thread principal. */
internal class LogBoxState {
    val logList = mutableStateListOf<LogLine>()
    var tab by mutableIntStateOf(TAB_ALL)
    var query by mutableStateOf("")
    var autoScrollDown by mutableStateOf(true)
    var importantCount by mutableIntStateOf(0)
    var warnCount by mutableIntStateOf(0)
    var errorCount by mutableIntStateOf(0)

    val visible: List<LogLine> by derivedStateOf {
        val q = query.trim()
        if (tab == TAB_ALL && q.isEmpty()) logList
        else logList.filter { it.inTab(tab) && (q.isEmpty() || it.raw.contains(q, ignoreCase = true)) }
    }

    private fun count(line: LogLine, delta: Int) {
        if (line.important) importantCount += delta
        if (line.kind == LogClassifier.Kind.WARN) warnCount += delta
        if (line.headline) errorCount += delta
    }

    fun append(lines: List<LogLine>) {
        logList.addAll(lines)
        lines.forEach { count(it, 1) }
        val overflow = logList.size - MAX_LOG_LINES
        if (overflow > 0) {
            val removed = logList.subList(0, overflow)
            removed.forEach { count(it, -1) }
            removed.clear()
        }
    }

    fun clear() {
        logList.clear()
        importantCount = 0
        warnCount = 0
        errorCount = 0
    }
}

@Composable
fun LogBox(
    enableLog: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    val state = remember { LogBoxState() }
    val buffer = remember { Collections.synchronizedList(mutableListOf<String>()) }
    //元素表示该次滚动是否使用动画，自动跟随日志的滚动不带动画
    val scrollChannel = remember { mutableStateOf<Channel<Boolean>?>(null) }
    val lineId = remember { AtomicLong(0) }

    val logHighlighter = remember { LogHighlighter() }
    val classifier = remember { LogClassifier() }

    val config = remember {
        object {
            /** 缓冲区刷新间隔，单位：ms */
            val BUFFER_FLUSH_INTERVAL: Long = AllSettings.logBufferFlushInterval.getValue().toLong()
        }
    }

    LaunchedEffect(enableLog) {
        if (enableLog) {
            scrollChannel.value = Channel(capacity = 100)

            //O log nativo tem um único ouvinte (GameLogHub), que repassa para a caixa de log
            com.russoviller.cryogenic.bridge.GameLogHub.setConsole { log ->
                synchronized(buffer) {
                    buffer.add(log)
                }
            }

            launch(Dispatchers.Default) {
                val mutex = Mutex()
                while (isActive) {
                    try {
                        ensureActive()
                        delay(config.BUFFER_FLUSH_INTERVAL.milliseconds)
                        val pending = mutableListOf<String>()

                        mutex.withLock {
                            synchronized(buffer) {
                                if (buffer.isNotEmpty()) {
                                    pending.addAll(buffer)
                                    buffer.clear()
                                }
                            }
                        }
                        if (pending.isNotEmpty()) {
                            val lines = pending.map { raw ->
                                val result = classifier.classify(raw)
                                LogLine(lineId.incrementAndGet(), logHighlighter.highlight(raw), raw, result.kind, result.headline)
                            }
                            withContext(Dispatchers.Main) {
                                state.append(lines)
                                if (state.autoScrollDown) {
                                    //尝试进行滚动
                                    scrollChannel.value?.trySend(false)
                                }
                            }
                        }
                    } catch (_: CancellationException) {
                        break
                    }
                }
            }

            //自动滚动部分
            launch(Dispatchers.Main) {
                scrollChannel.value?.consumeAsFlow()?.collect { animated ->
                    runCatching {
                        val targetIndex = state.visible.lastIndex
                        if (targetIndex >= 0) {
                            if (animated) scrollState.animateScrollToItem(targetIndex)
                            else scrollState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
        } else {
            scrollChannel.value = null
            com.russoviller.cryogenic.bridge.GameLogHub.setConsole(null)
            synchronized(buffer) { buffer.clear() }
            state.clear()
            classifier.reset()
        }
    }

    //trocar de aba ou buscar leva para o fim da lista filtrada
    LaunchedEffect(state.tab, state.query) {
        scrollChannel.value?.trySend(false)
    }

    if (enableLog) {
        LogBoxBody(
            state = state,
            scrollState = scrollState,
            modifier = modifier,
            onClear = {
                synchronized(buffer) { buffer.clear() }
                state.clear()
            },
            onScrollDown = { scrollChannel.value?.trySend(true) },
            onClose = onClose
        )
    }
}

/** Prévia da caixa de log com linhas fixas (usada nos testes de captura de tela). */
@Composable
internal fun LogBoxPreviewHost(lines: List<String>, tab: Int = TAB_ALL) {
    val state = remember {
        LogBoxState().apply {
            val highlighter = LogHighlighter()
            val classifier = LogClassifier()
            append(lines.mapIndexed { i, raw ->
                val r = classifier.classify(raw)
                LogLine(i.toLong(), highlighter.highlight(raw), raw, r.kind, r.headline)
            })
            this.tab = tab
        }
    }
    LogBoxBody(
        state = state,
        scrollState = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        onClear = { state.clear() },
        onScrollDown = {},
        onClose = {}
    )
}

@Composable
private fun LogBoxBody(
    state: LogBoxState,
    scrollState: LazyListState,
    modifier: Modifier,
    onClear: () -> Unit,
    onScrollDown: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val tab = state.tab
    val visible = state.visible
    val tabTitles = listOf(
        stringResource(R.string.cryo_log_tab_all),
        stringResource(R.string.cryo_log_tab_important) + countSuffix(state.importantCount),
        stringResource(R.string.cryo_log_tab_warn) + countSuffix(state.warnCount),
        stringResource(R.string.cryo_log_tab_error) + countSuffix(state.errorCount)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        //barra superior
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(CryoGlassColors.Deep.copy(alpha = 0.85f))
                .drawBehind {
                    drawRect(
                        color = CryoGlassColors.Ice.copy(alpha = 0.35f),
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx())
                    )
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val wide = maxWidth >= 640.dp
            val tools: @Composable () -> Unit = {
                LogSearchField(
                    modifier = Modifier.width(if (wide) 170.dp else 150.dp),
                    value = state.query,
                    onValueChange = { state.query = it },
                    onDone = { focusManager.clearFocus() }
                )
                LogToolButton(R.drawable.ic_copy_all_outlined) {
                    val text = state.visible.joinToString("\n") { it.raw }
                    copyText("log", text, context)
                }
                LogToolButton(R.drawable.ic_delete_outlined, onClick = onClear)
                LogToolButton(R.drawable.ic_list_down, active = state.autoScrollDown) {
                    val value = !state.autoScrollDown
                    state.autoScrollDown = value
                    if (value) onScrollDown()
                }
                LogToolButton(R.drawable.ic_arrow_cool_down, onClick = onScrollDown)
                LogToolButton(R.drawable.ic_close, danger = true, onClick = onClose)
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CryoTag(text = "LOG", icon = painterResource(R.drawable.ic_terminal_outlined))
                    CryoSegmentedTabs(
                        modifier = Modifier.weight(1f),
                        titles = tabTitles,
                        selected = tab,
                        onSelect = { state.tab = it }
                    )
                    tools()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CryoTag(text = "LOG", icon = painterResource(R.drawable.ic_terminal_outlined))
                        Spacer(Modifier.weight(1f))
                        tools()
                    }
                    CryoSegmentedTabs(titles = tabTitles, selected = tab, onSelect = { state.tab = it })
                }
            }
        }

        //explicação curta da aba (para quem não entende log)
        val (hintRes, hintColor) = when (tab) {
            TAB_IMPORTANT -> R.string.cryo_log_hint_important to CryoGlassColors.Ice
            TAB_WARN -> R.string.cryo_log_hint_warn to WarnColor
            TAB_ERROR -> R.string.cryo_log_hint_error to CryoDanger
            else -> R.string.cryo_log_hint_all to CryoGlassColors.Frost
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(hintColor.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(
                    when (tab) {
                        TAB_WARN, TAB_ERROR -> R.drawable.ic_warning_outlined
                        else -> R.drawable.ic_info_outlined
                    }
                ),
                contentDescription = null,
                tint = hintColor
            )
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val textSize = AllSettings.logTextSize.state
        val fontSize = remember(textSize) { TextUnit(textSize.toFloat(), TextUnitType.Sp) }
        val lineHeight = remember(textSize) { TextUnit(textSize.toFloat() * 1.1f, TextUnitType.Sp) }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (visible.isEmpty()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    text = stringResource(if (state.logList.isEmpty()) R.string.cryo_log_empty_wait else R.string.cryo_log_empty_tab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState
            ) {
                items(visible, key = { it.id }) { line ->
                    val accent = when (line.kind) {
                        LogClassifier.Kind.ERROR -> CryoDanger
                        LogClassifier.Kind.WARN -> WarnColor
                        LogClassifier.Kind.IMPORTANT -> CryoGlassColors.Ice
                        LogClassifier.Kind.NORMAL -> Color.Transparent
                    }
                    Text(
                        text = line.text,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .drawBehind {
                                if (accent != Color.Transparent) {
                                    drawRect(color = accent.copy(alpha = 0.10f))
                                    drawRect(color = accent, size = Size(3.dp.toPx(), size.height))
                                }
                            }
                            .padding(start = 9.dp, end = 6.dp),
                        color = Color.White,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }
            }
        }
    }
}

private fun countSuffix(count: Int): String = if (count > 0) " · ${if (count > 999) "999+" else count}" else ""

@Composable
private fun LogToolButton(iconRes: Int, active: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) CryoDanger else CryoGlassColors.Ice
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) tint.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = if (active) 0.95f else 0.4f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(modifier = Modifier.size(18.dp), painter = painterResource(iconRes), contentDescription = null, tint = tint)
    }
}

@Composable
private fun LogSearchField(modifier: Modifier, value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(modifier = Modifier.size(15.dp), painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = CryoGlassColors.Ice)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.cryo_log_search),
                    style = MaterialTheme.typography.labelMedium,
                    color = CryoGlassColors.Frost.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(CryoGlassColors.Ice),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() })
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onValueChange("") },
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = CryoGlassColors.Frost
            )
        }
    }
}
