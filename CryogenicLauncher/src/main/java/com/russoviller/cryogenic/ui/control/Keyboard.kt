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

package com.russoviller.cryogenic.ui.control

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.inputmap.keycodes.ControlEventKeycode
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.AutoSizeText
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.control.gamepad.SPECIAL_KEY_MOUSE_SCROLL_DOWN
import com.russoviller.cryogenic.ui.control.gamepad.SPECIAL_KEY_MOUSE_SCROLL_UP
import com.russoviller.cryogenic.ui.screens.main.control_editor.InfoLayoutTextItem
import kotlinx.coroutines.launch

private data class TabItem(val title: String)

private data class KeySpec(
    val label: String,
    val code: String = "",
    val weight: Float = 1f,
    val aspect: Float = 1f,
    val isSpacer: Boolean = false
)

private val MAIN_LAYOUT: List<List<KeySpec>> = listOf(
    //1
    listOf(
        KeySpec(label = "Esc", code = ControlEventKeycode.GLFW_KEY_ESCAPE),
        KeySpec(label = "", code = "", weight = 0.6f, isSpacer = true),
        KeySpec(label = "F1", code = ControlEventKeycode.GLFW_KEY_F1),
        KeySpec(label = "F2", code = ControlEventKeycode.GLFW_KEY_F2),
        KeySpec(label = "F3", code = ControlEventKeycode.GLFW_KEY_F3),
        KeySpec(label = "F4", code = ControlEventKeycode.GLFW_KEY_F4),
        KeySpec(label = "", code = "", weight = 0.1f, isSpacer = true),
        KeySpec(label = "F5", code = ControlEventKeycode.GLFW_KEY_F5),
        KeySpec(label = "F6", code = ControlEventKeycode.GLFW_KEY_F6),
        KeySpec(label = "F7", code = ControlEventKeycode.GLFW_KEY_F7),
        KeySpec(label = "F8", code = ControlEventKeycode.GLFW_KEY_F8),
        KeySpec(label = "", code = "", weight = 0.1f, isSpacer = true),
        KeySpec(label = "F9", code = ControlEventKeycode.GLFW_KEY_F9),
        KeySpec(label = "F10", code = ControlEventKeycode.GLFW_KEY_F10),
        KeySpec(label = "F11", code = ControlEventKeycode.GLFW_KEY_F11),
        KeySpec(label = "F12", code = ControlEventKeycode.GLFW_KEY_F12)
    ),
    //2
    listOf(
        KeySpec(label = "`", code = ControlEventKeycode.GLFW_KEY_GRAVE_ACCENT),
        KeySpec(label = "1", code = ControlEventKeycode.GLFW_KEY_1),
        KeySpec(label = "2", code = ControlEventKeycode.GLFW_KEY_2),
        KeySpec(label = "3", code = ControlEventKeycode.GLFW_KEY_3),
        KeySpec(label = "4", code = ControlEventKeycode.GLFW_KEY_4),
        KeySpec(label = "5", code = ControlEventKeycode.GLFW_KEY_5),
        KeySpec(label = "6", code = ControlEventKeycode.GLFW_KEY_6),
        KeySpec(label = "7", code = ControlEventKeycode.GLFW_KEY_7),
        KeySpec(label = "8", code = ControlEventKeycode.GLFW_KEY_8),
        KeySpec(label = "9", code = ControlEventKeycode.GLFW_KEY_9),
        KeySpec(label = "0", code = ControlEventKeycode.GLFW_KEY_0),
        KeySpec(label = "-", code = ControlEventKeycode.GLFW_KEY_MINUS),
        KeySpec(label = "+", code = ControlEventKeycode.GLFW_KEY_EQUAL),
        KeySpec(label = "Backspace", code = ControlEventKeycode.GLFW_KEY_BACKSPACE, weight = 1.5f, aspect = 1.5f)
    ),
    //3
    listOf(
        KeySpec(label = "Tab", code = ControlEventKeycode.GLFW_KEY_TAB, weight = 1.3f, aspect = 1.3f),
        KeySpec(label = "Q", code = ControlEventKeycode.GLFW_KEY_Q),
        KeySpec(label = "W", code = ControlEventKeycode.GLFW_KEY_W),
        KeySpec(label = "E", code = ControlEventKeycode.GLFW_KEY_E),
        KeySpec(label = "R", code = ControlEventKeycode.GLFW_KEY_R),
        KeySpec(label = "T", code = ControlEventKeycode.GLFW_KEY_T),
        KeySpec(label = "Y", code = ControlEventKeycode.GLFW_KEY_Y),
        KeySpec(label = "U", code = ControlEventKeycode.GLFW_KEY_U),
        KeySpec(label = "I", code = ControlEventKeycode.GLFW_KEY_I),
        KeySpec(label = "O", code = ControlEventKeycode.GLFW_KEY_O),
        KeySpec(label = "P", code = ControlEventKeycode.GLFW_KEY_P),
        KeySpec(label = "[", code = ControlEventKeycode.GLFW_KEY_LEFT_BRACKET),
        KeySpec(label = "]", code = ControlEventKeycode.GLFW_KEY_RIGHT_BRACKET),
        KeySpec(label = "\\", code = ControlEventKeycode.GLFW_KEY_BACKSLASH, weight = 1.2f, aspect = 1.2f)
    ),
    //4
    listOf(
        KeySpec(label = "Capslock", code = ControlEventKeycode.GLFW_KEY_CAPS_LOCK, weight = 1.4f, aspect = 1.4f),
        KeySpec(label = "A", code = ControlEventKeycode.GLFW_KEY_A),
        KeySpec(label = "S", code = ControlEventKeycode.GLFW_KEY_S),
        KeySpec(label = "D", code = ControlEventKeycode.GLFW_KEY_D),
        KeySpec(label = "F", code = ControlEventKeycode.GLFW_KEY_F),
        KeySpec(label = "G", code = ControlEventKeycode.GLFW_KEY_G),
        KeySpec(label = "H", code = ControlEventKeycode.GLFW_KEY_H),
        KeySpec(label = "J", code = ControlEventKeycode.GLFW_KEY_J),
        KeySpec(label = "K", code = ControlEventKeycode.GLFW_KEY_K),
        KeySpec(label = "L", code = ControlEventKeycode.GLFW_KEY_L),
        KeySpec(label = ";", code = ControlEventKeycode.GLFW_KEY_SEMICOLON),
        KeySpec(label = "'", code = ControlEventKeycode.GLFW_KEY_APOSTROPHE),
        KeySpec(label = "Enter", code = ControlEventKeycode.GLFW_KEY_ENTER, weight = 2.1f, aspect = 2.1f)
    ),
    //5
    listOf(
        KeySpec(label = "Shift", code = ControlEventKeycode.GLFW_KEY_LEFT_SHIFT, weight = 2f, aspect = 2f),
        KeySpec(label = "Z", code = ControlEventKeycode.GLFW_KEY_Z),
        KeySpec(label = "X", code = ControlEventKeycode.GLFW_KEY_X),
        KeySpec(label = "C", code = ControlEventKeycode.GLFW_KEY_C),
        KeySpec(label = "V", code = ControlEventKeycode.GLFW_KEY_V),
        KeySpec(label = "B", code = ControlEventKeycode.GLFW_KEY_B),
        KeySpec(label = "N", code = ControlEventKeycode.GLFW_KEY_N),
        KeySpec(label = "M", code = ControlEventKeycode.GLFW_KEY_M),
        KeySpec(label = ",", code = ControlEventKeycode.GLFW_KEY_COMMA),
        KeySpec(label = ".", code = ControlEventKeycode.GLFW_KEY_PERIOD),
        KeySpec(label = "/", code = ControlEventKeycode.GLFW_KEY_SLASH),
        KeySpec(label = "Shift", code = ControlEventKeycode.GLFW_KEY_RIGHT_SHIFT, weight = 2.2f, aspect = 2.2f)
    ),
    //6
    listOf(
        KeySpec(label = "Ctrl", code = ControlEventKeycode.GLFW_KEY_LEFT_CONTROL),
        KeySpec(label = "", code = "", isSpacer = true),
        KeySpec(label = "Alt", code = ControlEventKeycode.GLFW_KEY_LEFT_ALT),
        KeySpec(label = "Space", code = ControlEventKeycode.GLFW_KEY_SPACE, weight = 7f, aspect = 7f),
        KeySpec(label = "Alt", code = ControlEventKeycode.GLFW_KEY_RIGHT_ALT),
        KeySpec(label = "", code = "", weight = 2f, aspect = 2f, isSpacer = true),
        KeySpec(label = "Ctrl", code = ControlEventKeycode.GLFW_KEY_RIGHT_CONTROL)
    )
)

private val EDIT_LAYOUT: List<List<KeySpec>> = listOf(
    listOf(
        KeySpec(label = "Printf", code = ControlEventKeycode.GLFW_KEY_PRINT_SCREEN),
        KeySpec(label = "Scroll", code = ControlEventKeycode.GLFW_KEY_SCROLL_LOCK),
        KeySpec(label = "Pause", code = ControlEventKeycode.GLFW_KEY_PAUSE),
        KeySpec(label = "", code = "", weight = 12.2f, isSpacer = true)
    ),
    listOf(
        KeySpec(label = "Insert", code = ControlEventKeycode.GLFW_KEY_INSERT),
        KeySpec(label = "Home", code = ControlEventKeycode.GLFW_KEY_HOME),
        KeySpec(label = "PgUp", code = ControlEventKeycode.GLFW_KEY_PAGE_UP),
        KeySpec(label = "", code = "", weight = 7.8f, isSpacer = true),
        KeySpec(label = "Num lk", code = ControlEventKeycode.GLFW_KEY_NUM_LOCK),
        KeySpec(label = "/", code = ControlEventKeycode.GLFW_KEY_KP_DIVIDE),
        KeySpec(label = "*", code = ControlEventKeycode.GLFW_KEY_KP_MULTIPLY),
        KeySpec(label = "-", code = ControlEventKeycode.GLFW_KEY_KP_SUBTRACT)
    ),
    listOf(
        KeySpec(label = "Delete", code = ControlEventKeycode.GLFW_KEY_DELETE),
        KeySpec(label = "End", code = ControlEventKeycode.GLFW_KEY_END),
        KeySpec(label = "PgDn", code = ControlEventKeycode.GLFW_KEY_PAGE_DOWN),
        KeySpec(label = "", code = "", weight = 7.8f, isSpacer = true),
        KeySpec(label = "7", code = ControlEventKeycode.GLFW_KEY_KP_7),
        KeySpec(label = "8", code = ControlEventKeycode.GLFW_KEY_KP_8),
        KeySpec(label = "9", code = ControlEventKeycode.GLFW_KEY_KP_9),
        KeySpec(label = "+", code = ControlEventKeycode.GLFW_KEY_KP_ADD)
    ),
    listOf(
        KeySpec(label = "", code = "", weight = 11f, isSpacer = true),
        KeySpec(label = "4", code = ControlEventKeycode.GLFW_KEY_KP_4),
        KeySpec(label = "5", code = ControlEventKeycode.GLFW_KEY_KP_5),
        KeySpec(label = "6", code = ControlEventKeycode.GLFW_KEY_KP_6),
        KeySpec(label = "", code = "", isSpacer = true)
    ),
    listOf(
        KeySpec(label = "", code = "", isSpacer = true),
        KeySpec(label = "↑", code = ControlEventKeycode.GLFW_KEY_UP),
        KeySpec(label = "", code = "", 8.8f, isSpacer = true),
        KeySpec(label = "1", code = ControlEventKeycode.GLFW_KEY_KP_1),
        KeySpec(label = "2", code = ControlEventKeycode.GLFW_KEY_KP_2),
        KeySpec(label = "3", code = ControlEventKeycode.GLFW_KEY_KP_3),
        KeySpec(label = "Enter", code = ControlEventKeycode.GLFW_KEY_KP_ENTER)
    ),
    listOf(
        KeySpec(label = "←", code = ControlEventKeycode.GLFW_KEY_LEFT),
        KeySpec(label = "↓", code = ControlEventKeycode.GLFW_KEY_DOWN),
        KeySpec(label = "→", code = ControlEventKeycode.GLFW_KEY_RIGHT),
        KeySpec(label = "", code = "", weight = 8.8f, isSpacer = true),
        KeySpec(label = "0", code = ControlEventKeycode.GLFW_KEY_KP_0),
        KeySpec(label = ".", code = ControlEventKeycode.GLFW_KEY_KP_DECIMAL),
        KeySpec(label = "", code = "", isSpacer = true)
    )
)

@Composable
fun Keyboard(
    onDismissRequest: () -> Unit,
    isTapMode: Boolean = false,
    onSwitch: (key: String, pressed: Boolean) -> Unit = { _, _ -> },
    onTap: (key: String) -> Unit = {}
) {
    val tabs = listOf(
        TabItem(stringResource(R.string.cryo_keyboard_tab_main)),
        TabItem(stringResource(R.string.cryo_keyboard_tab_nav))
    )
    val held = remember { mutableStateListOf<String>() }
    val currentOnSwitch by rememberUpdatedState(onSwitch)
    val currentOnTap by rememberUpdatedState(onTap)

    val onKey: (String) -> Unit = { key ->
        if (isTapMode) {
            currentOnTap(key)
        } else if (key in held) {
            held.remove(key)
            currentOnSwitch(key, false)
        } else {
            held.add(key)
            currentOnSwitch(key, true)
        }
    }
    val isActive: (String) -> Boolean = { key -> !isTapMode && key in held }

    KeyboardNavDialog(
        tabs = tabs,
        hint = stringResource(if (isTapMode) R.string.cryo_keyboard_hint_tap else R.string.cryo_keyboard_hint_hold),
        heldCount = if (isTapMode) 0 else held.size,
        onReleaseAll = {
            held.toList().forEach { key -> currentOnSwitch(key, false) }
            held.clear()
        },
        onDismissRequest = onDismissRequest
    ) { page ->
        when (page) {
            0 -> KeyboardArea(layout = MAIN_LAYOUT, onKey = onKey, isActive = isActive, refreshed = held.size)
            1 -> KeyboardArea(layout = EDIT_LAYOUT, onKey = onKey, isActive = isActive, refreshed = held.size)
        }
    }
}

@Composable
fun GamepadBindingKeyboard(
    selectedKeys: List<String>,
    onKeyAdd: (String) -> Unit,
    onKeyRemove: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val tabs = listOf(
        TabItem(stringResource(R.string.cryo_keyboard_tab_main)),
        TabItem(stringResource(R.string.cryo_keyboard_tab_nav)),
        TabItem(stringResource(R.string.cryo_keyboard_tab_mouse))
    )

    val currentSelectedKeys by rememberUpdatedState(selectedKeys)
    val currentOnKeyAdd by rememberUpdatedState(onKeyAdd)
    val currentOnKeyRemove by rememberUpdatedState(onKeyRemove)

    var refreshed by remember { mutableStateOf(false) }

    fun onKeyTap(key: String) {
        if (currentSelectedKeys.contains(key)) currentOnKeyRemove(key)
        else currentOnKeyAdd(key)
        refreshed = refreshed.not()
    }

    val isActive: (String) -> Boolean = { key -> currentSelectedKeys.contains(key) }

    KeyboardNavDialog(
        tabs = tabs,
        hint = stringResource(R.string.cryo_keyboard_hint_bind),
        heldCount = 0,
        onReleaseAll = {},
        onDismissRequest = onDismissRequest
    ) { page ->
        when (page) {
            0 -> KeyboardArea(layout = MAIN_LAYOUT, onKey = { onKeyTap(it) }, isActive = isActive, refreshed = refreshed)
            1 -> KeyboardArea(layout = EDIT_LAYOUT, onKey = { onKeyTap(it) }, isActive = isActive, refreshed = refreshed)
            2 -> GamepadSpecialArea(
                onTap = { key -> onKeyTap(key) },
                refreshed = refreshed,
                isSelected = isActive
            )
        }
    }
}

@Composable
fun GamepadSpecialArea(
    modifier: Modifier = Modifier,
    onTap: (String) -> Unit,
    refreshed: Any? = null,
    isSelected: (String) -> Boolean
) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .nonInteractiveScrollbar(
                state = scrollState.scrollIndicatorState!!,
                orientation = Orientation.Vertical,
            )
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(all = 12.dp),
        state = scrollState
    ) {
        //鼠标左键
        item {
            val selected = remember(refreshed) { isSelected(ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT) }
            InfoLayoutTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_event_launcher_mouse_left),
                onClick = {
                    onTap(ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT)
                },
                showArrow = false,
                selected = selected
            )
        }
        //鼠标中键
        item {
            val selected = remember(refreshed) { isSelected(ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE) }
            InfoLayoutTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_event_launcher_mouse_middle),
                onClick = {
                    onTap(ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
                },
                showArrow = false,
                selected = selected
            )
        }
        //鼠标右键
        item {
            val selected = remember(refreshed) { isSelected(ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT) }
            InfoLayoutTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_event_launcher_mouse_right),
                onClick = {
                    onTap(ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                },
                showArrow = false,
                selected = selected
            )
        }
        //单次鼠标滚轮上
        item {
            val selected = remember(refreshed) { isSelected(SPECIAL_KEY_MOUSE_SCROLL_UP) }
            InfoLayoutTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_event_launcher_mouse_scroll_up_single),
                onClick = {
                    onTap(SPECIAL_KEY_MOUSE_SCROLL_UP)
                },
                showArrow = false,
                selected = selected
            )
        }
        //单次鼠标滚轮下
        item {
            val selected = remember(refreshed) { isSelected(SPECIAL_KEY_MOUSE_SCROLL_DOWN) }
            InfoLayoutTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_event_launcher_mouse_scroll_down_single),
                onClick = {
                    onTap(SPECIAL_KEY_MOUSE_SCROLL_DOWN)
                },
                showArrow = false,
                selected = selected
            )
        }
    }
}

private val MODIFIER_LABELS = setOf("Esc", "Tab", "Capslock", "Shift", "Ctrl", "Alt", "Enter", "Backspace", "Space", "Delete", "Insert", "Home", "End", "PgUp", "PgDn", "Printf", "Scroll", "Pause", "Num lk")

private enum class KeyTone { NORMAL, FUNCTION, MODIFIER }

private fun toneOf(label: String): KeyTone = when {
    label in MODIFIER_LABELS -> KeyTone.MODIFIER
    label.length >= 2 && label[0] == 'F' && label.drop(1).all { it.isDigit() } -> KeyTone.FUNCTION
    else -> KeyTone.NORMAL
}

@Composable
private fun KeyboardNavDialog(
    tabs: List<TabItem>,
    hint: String,
    heldCount: Int,
    onReleaseAll: () -> Unit,
    onDismissRequest: () -> Unit,
    pageContent: @Composable (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val ice = CryoGlassColors.Ice
    val panelShape = cryoChamferShape(22.dp)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(panelShape)
                .background(
                    Brush.verticalGradient(
                        listOf(CryoGlassColors.Navy.copy(alpha = 0.97f), CryoGlassColors.Deep.copy(alpha = 0.99f))
                    )
                )
                .border(1.dp, Brush.linearGradient(listOf(ice.copy(alpha = 0.85f), ice.copy(alpha = 0.15f), CryoGlassColors.Aurora.copy(alpha = 0.6f))), panelShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.linearGradient(listOf(ice.copy(alpha = 0.35f), CryoGlassColors.Aurora.copy(alpha = 0.18f))))
                        .border(1.dp, ice.copy(alpha = 0.6f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_keyboard_filled),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_keyboard_kicker),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                        color = ice,
                        maxLines = 1
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, ice.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tabs.forEachIndexed { index, item ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) Brush.horizontalGradient(listOf(ice, CryoGlassColors.Aurora))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (heldCount > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFB0123A).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable(onClick = onReleaseAll)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cryo_keyboard_release_all, heldCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, ice.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.generic_close),
                        tint = Color.White
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.6f), Color.Transparent)))
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                pageContent(page)
            }
        }
    }
}

@Composable
private fun KeyboardArea(
    layout: List<List<KeySpec>>,
    onKey: (String) -> Unit,
    isActive: (String) -> Boolean,
    refreshed: Any?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        layout.forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                row.forEach { key ->
                    if (key.isSpacer || key.label.isEmpty()) {
                        Spacer(modifier = Modifier.weight(key.weight))
                    } else {
                        KeyButton(
                            modifier = Modifier.weight(key.weight),
                            name = key.label,
                            identifier = key.code,
                            active = refreshed.let { isActive(key.code) },
                            onPress = onKey,
                            aspectRatio = key.aspect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    modifier: Modifier = Modifier,
    name: String,
    identifier: String,
    active: Boolean,
    onPress: (String) -> Unit,
    aspectRatio: Float = 1f
) {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val tone = remember(name) { toneOf(name) }
    val currentOnPress by rememberUpdatedState(onPress)
    var touching by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val lift by animateDpAsState(if (touching) 0.dp else 2.dp, label = "key_lift")

    val face = when {
        active -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), ice, aurora))
        tone == KeyTone.MODIFIER -> Brush.verticalGradient(listOf(ice.copy(alpha = 0.26f), CryoGlassColors.Navy.copy(alpha = 0.95f)))
        tone == KeyTone.FUNCTION -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Black.copy(alpha = 0.35f)))
        else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), CryoGlassColors.Navy.copy(alpha = 0.9f)))
    }
    val edge = when {
        active -> Color.White
        tone == KeyTone.MODIFIER -> ice.copy(alpha = 0.55f)
        else -> ice.copy(alpha = 0.22f)
    }
    val labelColor = when {
        active -> CryoGlassColors.Deep
        tone == KeyTone.MODIFIER -> ice
        tone == KeyTone.FUNCTION -> CryoGlassColors.Frost.copy(alpha = 0.85f)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .drawBehind {
                drawRoundRect(
                    color = if (active) ice.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 2.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .offset(y = 2.dp - lift)
            .clip(shape)
            .background(face)
            .border(if (active) 1.5.dp else 1.dp, edge, shape)
            .pointerInput(identifier) {
                detectTapGestures(
                    onPress = {
                        touching = true
                        tryAwaitRelease()
                        touching = false
                    },
                    onTap = { currentOnPress(identifier) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AutoSizeText(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .basicMarquee(Int.MAX_VALUE),
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (tone == KeyTone.NORMAL) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = if (tone == KeyTone.MODIFIER) 0.5.sp else 0.sp
            ),
            color = labelColor,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = MaterialTheme.typography.labelMedium.fontSize)
        )
    }
}
