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

package com.russoviller.cryogenic.ui.screens.main

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.activities.CrashType
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar

private val Warn = Color(0xFFFFC857)

@Composable
fun ErrorScreen(
    crashType: CrashType,
    shareLogs: Boolean = true,
    canUpload: Boolean = false,
    canRestart: Boolean = true,
    onShareLogsClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    onOrientationChanged: (Int) -> Unit = {},
    body: @Composable ColumnScope.() -> Unit
) {
    //获取方向信息，展示两套不同的UI
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    //detalhes técnicos ficam escondidos: o importante é compartilhar o arquivo de log
    var showDetails by rememberSaveable { mutableStateOf(false) }

    val actions = CrashActionsSpec(
        shareLogs = shareLogs,
        canUpload = canUpload,
        canRestart = canRestart,
        showDetails = showDetails,
        onShareLogsClick = onShareLogsClick,
        onUploadClick = onUploadClick,
        onRestartClick = onRestartClick,
        onExitClick = onExitClick,
        onToggleDetails = { showDetails = !showDetails }
    )

    CrashBackdrop {
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (AllSettings.launcherFullScreen.state) Modifier
                        else Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Horizontal))
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CrashHeader(
                    crashType = crashType,
                    rotateIcon = R.drawable.ic_mobile_rotate_filled,
                    onRotateClick = { onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(6.2f)
                            .fillMaxHeight()
                    ) {
                        if (showDetails) {
                            CrashDetails(crashType = crashType, modifier = Modifier.fillMaxSize(), body = body)
                        } else {
                            NoPrintCard(modifier = Modifier.fillMaxSize(), big = true)
                        }
                    }
                    CrashActions(
                        modifier = Modifier
                            .weight(3.4f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        spec = actions
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeContent)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CrashHeader(
                    crashType = crashType,
                    rotateIcon = R.drawable.ic_fullscreen_exit,
                    onRotateClick = { onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScrollWithBar(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoPrintCard(modifier = Modifier.fillMaxWidth(), big = false)
                    AnimatedVisibility(visible = showDetails, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        CrashDetails(crashType = crashType, modifier = Modifier.fillMaxWidth(), scrollable = false, body = body)
                    }
                }
                CrashActions(modifier = Modifier.fillMaxWidth(), spec = actions)
            }
        }
    }
}

private class CrashActionsSpec(
    val shareLogs: Boolean,
    val canUpload: Boolean,
    val canRestart: Boolean,
    val showDetails: Boolean,
    val onShareLogsClick: () -> Unit,
    val onUploadClick: () -> Unit,
    val onRestartClick: () -> Unit,
    val onExitClick: () -> Unit,
    val onToggleDetails: () -> Unit
)

/** Fundo escuro com um brilho vermelho suave (sem animação contínua). */
@Composable
private fun CrashBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0710), CryoGlassColors.Deep, Color(0xFF05070D))))
            .background(Brush.radialGradient(listOf(CryoDanger.copy(alpha = 0.16f), Color.Transparent), radius = 900f))
    ) {
        content()
    }
}

@Composable
private fun CrashHeader(crashType: CrashType, rotateIcon: Int, onRotateClick: () -> Unit) {
    val title = when (crashType) {
        CrashType.LAUNCHER_CRASH -> stringResource(R.string.crash_launcher_title, BuildKeys.LAUNCHER_NAME)
        CrashType.GAME_CRASH -> stringResource(R.string.cryo_crash_game_title)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(CryoDanger.copy(alpha = 0.18f))
                .border(1.dp, CryoDanger.copy(alpha = 0.7f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(modifier = Modifier.size(22.dp), painter = painterResource(R.drawable.ic_warning_filled), contentDescription = null, tint = CryoDanger)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = BuildKeys.LAUNCHER_NAME.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = CryoDanger
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onRotateClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(modifier = Modifier.size(20.dp), painter = painterResource(rotateIcon), contentDescription = null, tint = CryoGlassColors.Ice)
        }
    }
}

/** O recado principal: não tirar print, compartilhar o arquivo de log no Discord. */
@Composable
private fun NoPrintCard(modifier: Modifier, big: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CryoDanger.copy(alpha = 0.16f), CryoGlassColors.Deep.copy(alpha = 0.92f))))
            .border(1.5.dp, Brush.linearGradient(listOf(CryoDanger, Warn.copy(alpha = 0.4f))), shape)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(if (big) 46.dp else 52.dp)
                .clip(CircleShape)
                .background(CryoDanger.copy(alpha = 0.2f))
                .border(2.dp, CryoDanger, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(modifier = Modifier.size(if (big) 24.dp else 28.dp), painter = painterResource(R.drawable.ic_cryo_no_photo), contentDescription = null, tint = CryoDanger)
        }
        Text(
            text = stringResource(R.string.cryo_crash_no_print_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (big) 26.sp else 24.sp,
                lineHeight = if (big) 29.sp else 28.sp,
                brush = Brush.horizontalGradient(listOf(Color.White, Color(0xFFFF9AA2)))
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.cryo_crash_no_print_text),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Step(1, stringResource(R.string.cryo_crash_step_1))
            Step(2, stringResource(R.string.cryo_crash_step_2))
            Step(3, stringResource(R.string.cryo_crash_step_3))
        }
    }
}

@Composable
private fun RowScope.Step(number: Int, text: String) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), color = CryoGlassColors.Deep, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Detalhes técnicos (para quem quer ver): tipo do erro + mensagem do log. */
@Composable
private fun CrashDetails(
    crashType: CrashType,
    modifier: Modifier,
    scrollable: Boolean = true,
    body: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(R.drawable.ic_terminal_outlined), contentDescription = null, tint = CryoGlassColors.Ice)
            Text(
                text = stringResource(R.string.cryo_crash_diagnosis),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = CryoGlassColors.Ice
            )
        }
        CompositionLocalProvider(
            LocalContentColor provides CryoGlassColors.Frost,
            LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = CryoGlassColors.Frost)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.weight(1f).verticalScrollWithBar(state = rememberScrollState()) else Modifier)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(crashType.textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                body()
            }
        }
    }
}

@Composable
private fun CrashActions(modifier: Modifier, spec: CrashActionsSpec) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (spec.shareLogs) {
            //botão principal, maior que os outros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .cryoBackground(cryoGradient())
                    .clickable(onClick = spec.onShareLogsClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(modifier = Modifier.size(24.dp), painter = painterResource(R.drawable.ic_share_filled), contentDescription = null, tint = CryoGlassColors.Deep)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.crash_share_logs),
                        color = CryoGlassColors.Deep,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.cryo_crash_share_sub),
                        color = CryoGlassColors.Deep.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (spec.canUpload) {
            ActionRow(R.drawable.ic_link, stringResource(R.string.crash_link_share_button), onClick = spec.onUploadClick)
        }
        ActionRow(
            R.drawable.ic_description_outlined,
            stringResource(if (spec.showDetails) R.string.cryo_crash_details_hide else R.string.cryo_crash_details_show),
            active = spec.showDetails,
            onClick = spec.onToggleDetails
        )
        if (spec.canRestart) {
            ActionRow(R.drawable.ic_restart_alt, stringResource(R.string.crash_restart), onClick = spec.onRestartClick)
        }
        ActionRow(R.drawable.ic_close, stringResource(R.string.crash_exit), danger = true, onClick = spec.onExitClick)
    }
}

@Composable
private fun ActionRow(iconRes: Int, text: String, active: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val tint = when {
        danger -> CryoDanger
        else -> CryoGlassColors.Ice
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(if (active) tint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = if (active) 0.9f else 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(modifier = Modifier.size(19.dp), painter = painterResource(iconRes), contentDescription = null, tint = tint)
        Text(
            text = text,
            color = if (danger) CryoDanger else Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun PreviewErrorScreen() {
    MaterialExpressiveTheme {
        ErrorScreen(
            crashType = CrashType.GAME_CRASH,
            shareLogs = true,
            canUpload = true,
            canRestart = true,
            body = {}
        )
    }
}
