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

package com.russoviller.cryogenic.ui.screens.content.settings

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ZLApplication
import com.russoviller.cryogenic.context.getFileName
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.game.launch.executeJarWithUri
import com.russoviller.cryogenic.game.multirt.Runtime
import com.russoviller.cryogenic.game.multirt.RuntimesManager
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CardTitleLayout
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.ImportMultipleFileButton
import com.russoviller.cryogenic.ui.screens.content.elements.ImportSingleFileButton
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.device.Architecture
import com.russoviller.cryogenic.utils.file.checkExtensionOrThrow
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.utils.string.throwableToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.sendToast
import kotlinx.coroutines.Dispatchers

private sealed interface RuntimeOperation {
    data object None: RuntimeOperation
    data class PreDelete(val runtime: Runtime): RuntimeOperation
    data class Delete(val runtime: Runtime): RuntimeOperation
}

@Composable
fun JavaManageScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.JavaManager, settingsScreenKey, false)
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)

        var runtimes by remember { mutableStateOf(getRuntimes()) }
        var runtimeOperation by remember { mutableStateOf<RuntimeOperation>(RuntimeOperation.None) }

        var showSelectRuntimeDialog by remember { mutableStateOf(false) }
        var selectedRuntimeForJar by remember { mutableStateOf<Runtime?>(null) }
        var launchJarPicker by remember { mutableStateOf<(() -> Unit)?>(null) }

        RuntimeOperation(
            runtimeOperation = runtimeOperation,
            updateOperation = { runtimeOperation = it },
            callRefresh = { runtimes = getRuntimes(true) },
            submitError = submitError
        )

        if (showSelectRuntimeDialog) {
            SelectJavaRuntimeDialog(
                runtimes = runtimes,
                onDismissRequest = {
                    showSelectRuntimeDialog = false
                    launchJarPicker = null
                },
                onSelectRuntime = { selectedRuntime ->
                    showSelectRuntimeDialog = false
                    selectedRuntimeForJar = selectedRuntime
                    launchJarPicker?.invoke()
                    launchJarPicker = null
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .cryoBackground(cryoGradient()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_java),
                            contentDescription = null,
                            tint = CryoGlassColors.Deep
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.cryo_java_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.cryo_java_count, runtimes.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CryoGlassColors.Ice.copy(alpha = 0.18f))
                )
                JavaActionSlot {
                    IconTextButton(
                        onClick = { runtimes = getRuntimes(true) },
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.generic_refresh),
                        text = stringResource(R.string.generic_refresh),
                    )
                }
                JavaActionSlot {
                    ImportMultipleFileButton(
                        extension = "xz",
                        progressUris = { uris ->
                            uris.forEach { uri ->
                                progressRuntimeUri(
                                    context = context,
                                    uri = uri,
                                    callRefresh = { runtimes = getRuntimes(true) },
                                    submitError = submitError
                                )
                            }
                        }
                    )
                }
                JavaActionSlot {
                    ImportSingleFileButton(
                        extension = "jar",
                        onClick = {
                            selectedRuntimeForJar = null
                        },
                        onLongClick = { launch ->
                            launchJarPicker = launch
                            showSelectRuntimeDialog = true
                        },
                        progressUris = { uris ->
                            uris.firstOrNull()?.let { uri ->
                                val customRuntime = selectedRuntimeForJar
                                selectedRuntimeForJar = null

                                if (customRuntime != null) {
                                    (context as? Activity)?.let { activity ->
                                        executeJarWithUri(activity, uri, customRuntime.name)
                                    }
                                } else {
                                    RuntimesManager.getExactJreName(8) ?: run {
                                        eventViewModel.sendToast(androidText(R.string.multirt_no_java_8), Toast.LENGTH_LONG)
                                        return@ImportSingleFileButton
                                    }
                                    (context as? Activity)?.let { activity ->
                                        val jreName = AllSettings.javaRuntime.takeIf { AllSettings.autoPickJavaRuntime.getValue() }?.getValue()
                                        executeJarWithUri(activity, uri, jreName)
                                    }
                                }
                            }
                        },
                        painter = painterResource(R.drawable.ic_terminal_outlined),
                        text = stringResource(R.string.execute_jar_title)
                    )
                }
            }

            LazyVerticalGrid(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(runtimes, key = { it.name }) { runtime ->
                    JavaRuntimeItem(
                        runtime = runtime,
                        onDeleteClick = {
                            runtimeOperation = RuntimeOperation.PreDelete(runtime)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun JavaActionSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

private fun getRuntimes(forceLoad: Boolean = false): List<Runtime> =
    RuntimesManager.getRuntimes(forceLoad = forceLoad)

@Composable
private fun RuntimeOperation(
    runtimeOperation: RuntimeOperation,
    updateOperation: (RuntimeOperation) -> Unit,
    callRefresh: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    when(runtimeOperation) {
        is RuntimeOperation.None -> {}
        is RuntimeOperation.PreDelete -> {
            val runtime = runtimeOperation.runtime
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.multirt_runtime_delete_message, runtime.name),
                onConfirm = { updateOperation(RuntimeOperation.Delete(runtime)) },
                onDismiss = { updateOperation(RuntimeOperation.None) }
            )
        }
        is RuntimeOperation.Delete -> {
            val failedMessage = stringResource(R.string.multirt_runtime_delete_failed)
            val runtime = runtimeOperation.runtime
            TaskSystem.submitTask(
                Task.runTask(
                    id = runtime.name,
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(
                            R.string.multirt_runtime_deleting, runtime.name
                        ))
                        RuntimesManager.removeRuntime(runtime.name)
                    },
                    onError = {
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(failedMessage),
                                message = androidText(it.getMessageOrToString())
                            )
                        )
                    },
                    onFinally = callRefresh
                )
            )
            updateOperation(RuntimeOperation.None)
        }
    }
}

private fun progressRuntimeUri(
    context: Context,
    uri: Uri,
    callRefresh: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    fun showError(
        title: AndroidStringText = androidText(R.string.multirt_runtime_import_failed),
        message: AndroidStringText
    ) {
        submitError(
            ErrorViewModel.ThrowableMessage(
                title = title,
                message = message
            )
        )
    }

    val name = context.getFileName(uri) ?: run {
        showError(message = androidText(R.string.multirt_runtime_import_failed_file_name))
        return
    }
    TaskSystem.submitTask(
        Task.runTask(
            id = name,
            dispatcher = Dispatchers.IO,
            task = { task ->
                name.checkExtensionOrThrow(listOf("xz"))

                val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    showError(message = androidText(R.string.multirt_runtime_import_failed_input_stream))
                    return@runTask
                }
                RuntimesManager.installRuntime(
                    nativeLibDir = PathManager.DIR_NATIVE_LIB,
                    inputStream = inputStream,
                    name = name,
                    updateProgress = { textRes, textArg ->
                        task.updateMessage(androidText(
                            textRes, *textArg
                        ))
                    }
                )
            },
            onError = {
                showError(message = androidText(throwableToString(it)))
            },
            onFinally = callRefresh,
            onCancel = {
                runCatching {
                    RuntimesManager.removeRuntime(name)
                    callRefresh()
                }.onFailure { t ->
                    showError(
                        title = androidText(R.string.multirt_runtime_delete_failed),
                        message = androidText(t.getMessageOrToString())
                    )
                }
            }
        )
    )
}

@Composable
private fun JavaRuntimeItem(
    runtime: Runtime,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val error = MaterialTheme.colorScheme.error
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cryoGlass(shape = RoundedCornerShape(18.dp), glow = false)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .cryoBackground(
                        if (runtime.versionString != null) cryoGradient()
                        else Brush.linearGradient(listOf(error, error.copy(alpha = 0.6f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = runtime.javaVersion.takeIf { it != 0 }?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge.copy(shadow = null),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = runtime.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = runtime.versionString?.let {
                        stringResource(R.string.multirt_runtime_version_name, it)
                    } ?: stringResource(R.string.multirt_runtime_corrupt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (runtime.versionString != null) CryoGlassColors.Frost.copy(alpha = 0.8f) else error,
                    maxLines = 1
                )
            }
            if (onDeleteClick != null) {
                val canDelete = !runtime.isProvidedByLauncher || !runtime.isCompatible()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(if (canDelete) 1f else 0.3f)
                        .clip(RoundedCornerShape(50))
                        .background(error.copy(alpha = 0.16f))
                        .border(1.dp, error.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable(enabled = canDelete, onClick = onDeleteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_delete_filled),
                        contentDescription = stringResource(R.string.generic_delete),
                        tint = error
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (runtime.isProvidedByLauncher) {
                JavaChip(stringResource(R.string.multirt_runtime_provided_by_launcher), CryoGlassColors.Aurora)
            }
            runtime.arch?.let { arch ->
                val compatible = ZLApplication.DEVICE_ARCHITECTURE == Architecture.archAsInt(arch)
                JavaChip(
                    text = if (compatible) arch else stringResource(R.string.multirt_runtime_incompatible_arch, arch),
                    color = if (compatible) CryoGlassColors.Ice else error
                )
            }
        }
    }
}

@Composable
private fun JavaChip(text: String, color: Color) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, accent = color, fontSize = 11.sp)
}

@Composable
private fun SelectJavaRuntimeDialog(
    runtimes: List<Runtime>,
    onDismissRequest: () -> Unit,
    onSelectRuntime: (Runtime) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(all = 16.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight()
                .fillMaxWidth(0.55f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.execute_jar_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .nonInteractiveScrollbar(
                                state = scrollState.scrollIndicatorState!!,
                                orientation = Orientation.Vertical,
                            ),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(runtimes) { runtime ->
                            JavaRuntimeItem(
                                runtime = runtime,
                                onClick = { onSelectRuntime(runtime) },
                                onDeleteClick = null
                            )
                        }
                    }

                    CryoButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismissRequest
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_cancel))
                    }
                }
            }
        }
    }
}
