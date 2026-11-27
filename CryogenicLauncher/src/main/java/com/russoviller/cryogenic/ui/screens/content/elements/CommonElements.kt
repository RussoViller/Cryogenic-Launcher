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

package com.russoviller.cryogenic.ui.screens.content.elements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextOverflow
import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.key
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloat
import com.russoviller.cryogenic.ui.components.CryoButton
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.context.copyLocalFile
import com.russoviller.cryogenic.context.getFileName
import com.russoviller.cryogenic.contract.extensionToMimeType
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskLogOutput
import com.russoviller.cryogenic.coroutine.TaskStage
import com.russoviller.cryogenic.coroutine.TitledTask
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.TaskLogCard
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.file.checkExtensionOrThrow
import com.russoviller.cryogenic.utils.file.formatFileSize
import com.russoviller.cryogenic.utils.platform.bytesToMB
import com.russoviller.cryogenic.utils.platform.getTotalMemory
import com.russoviller.cryogenic.utils.platform.getUsedMemory
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * [androidx.compose.material3.DisabledAlpha]
 */
const val DisabledAlpha = 0.38f

@Composable
fun CategoryIcon(
    @DrawableRes
    icon: Int,
    @StringRes
    textRes: Int
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(textRes),
        modifier = Modifier.size(24.dp)
    )
}

data class CategoryItem(
    val key: TitledNavKey,
    val icon: @Composable () -> Unit,
    val textRes: Int,
    val division: Boolean = false
)

/**
 * 排序方式枚举
 */
enum class SortByEnum(val textRes: Int) {
    /** 按照名称排序 */
    Name(R.string.sort_by_name),
    /** 按照文件名称排序 */
    FileName(R.string.sort_by_file_name),
    /** 按照文件上次修改时间排序 */
    FileModifiedTime(R.string.sort_by_last_modified),
    /** 按照上次游玩时间排序 */
    LastPlayed(R.string.sort_by_last_played)
}

/**
 * 通用的排序方式下来菜单
 * @param enums 当前菜单支持的排序方式
 * @param currentEnum 当前的排序方式
 * @param onEnumChanged 变更当前的排序方式
 * @param isAscending 当前是否为升序
 * @param onToggleSortOrder 切换当前的排序顺序
 */
@Composable
fun SortByDropdownMenu(
    expanded: Boolean,
    onClose: () -> Unit,
    enums: List<SortByEnum>,
    currentEnum: SortByEnum,
    onEnumChanged: (SortByEnum) -> Unit,
    isAscending: Boolean,
    onToggleSortOrder: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.large
    ) {
        enums.forEach { item ->
            DropdownMenuItem(
                text = { Text(stringResource(item.textRes)) },
                onClick = {
                    onEnumChanged(item)
                },
                trailingIcon = if (item == currentEnum) {
                    {
                        IconButton(
                            onClick = onToggleSortOrder
                        ) {
                            val rotation by animateFloatAsState(
                                if (isAscending) 0f else 180f
                            )
                            Icon(
                                modifier = Modifier.rotate(rotation),
                                painter = painterResource(R.drawable.ic_keyboard_double_arrow_up),
                                contentDescription = null
                            )
                        }
                    }
                } else null
            )
        }
    }
}

/**
 * 多 Uri 导入文件任务构建器
 * @param checkExtension 检查被选中的文件后缀是否符合要求
 */
@Composable
fun rememberMultipleUriImportTaskBuilder(
    id: String,
    targetDir: File,
    errorTitle: String = stringResource(R.string.generic_error),
    errorMessage: String? = stringResource(R.string.error_import_file),
    checkExtension: List<String>? = null,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit = {},
    onFileCopied: suspend (Task, File) -> Unit = { _, _ -> },
    onImported: () -> Unit = {}
): (List<Uri>) -> Task {
    val context = LocalContext.current
    val cErrorTitle by rememberUpdatedState(errorTitle)
    val cErrorMessage by rememberUpdatedState(errorMessage)
    val cSubmitError by rememberUpdatedState(submitError)
    val cOnFileCopied by rememberUpdatedState(onFileCopied)
    val cOnImported by rememberUpdatedState(onImported)

    return remember(id) {
        object : (List<Uri>) -> Task {
            override fun invoke(uris: List<Uri>): Task {
                return Task.runTask(
                    id = id,
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateProgress(-1f)
                        task.updateMessage(null)
                        uris.forEach { uri ->
                            try {
                                val fileName = context.getFileName(uri) ?: throw IOException("Failed to get file name")
                                task.updateProgress(-1f)
                                task.updateMessage(androidText(fileName))
                                val outputFile = File(targetDir, fileName)
                                if (checkExtension != null) {
                                    outputFile.checkExtensionOrThrow(checkExtension)
                                }
                                context.copyLocalFile(uri, outputFile)
                                //成功复制，如调用者有额外操作，可使用回调运行
                                cOnFileCopied(task, outputFile)
                            } catch (e: Exception) {
                                val eString = e.getMessageOrToString()
                                val messageString = if (cErrorMessage != null) {
                                    cErrorMessage + "\n" + eString
                                } else {
                                    eString
                                }

                                cSubmitError(
                                    ErrorViewModel.ThrowableMessage(
                                        title = androidText(cErrorTitle),
                                        message = androidText(messageString)
                                    )
                                )
                            }
                        }
                        cOnImported()
                    }
                )
            }
        }
    }
}

@Composable
fun ImportMultipleFileButton(
    extension: String,
    progressUris: (uris: List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    text: String = stringResource(R.string.generic_import)
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.takeIf { it.isNotEmpty() }?.let { uris1 ->
            progressUris(uris1)
        }
    }

    IconTextButton(
        modifier = modifier,
        onClick = {
            launcher.launch(extension.extensionToMimeType())
        },
        painter = painter,
        text = text
    )
}

@Composable
fun ImportSingleFileButton(
    extension: String,
    progressUris: (uris: List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    text: String = stringResource(R.string.generic_import),
    onClick: () -> Unit = {},
    onLongClick: ((launch: () -> Unit) -> Unit)? = null
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uri1 ->
            progressUris(listOf(uri1))
        }
    }

    IconTextButton(
        modifier = modifier,
        onClick = {
            onClick()
            launcher.launch(extension.extensionToMimeType())
        },
        painter = painter,
        text = text,
        onLongClick = {
            if (onLongClick != null) {
                onLongClick { launcher.launch(extension.extensionToMimeType()) }
            } else {
                launcher.launch(extension.extensionToMimeType())
            }
        }
    )
}

@Composable
fun <I, O> ImportFileButton(
    contract: ActivityResultContract<I, O>,
    onLaunch: (launcher: ManagedActivityResultLauncher<I, O>) -> Unit,
    progressOutput: (output: O) -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    text: String = stringResource(R.string.generic_import)
) {
    val launcher = rememberLauncherForActivityResult(
        contract = contract,
        onResult = progressOutput
    )

    IconTextButton(
        modifier = modifier,
        onClick = {
            onLaunch(launcher)
        },
        painter = painter,
        text = text
    )
}

@Composable
fun TitleTaskFlowDialog(
    title: String,
    tasks: List<TitledTask>,
    onCancel: () -> Unit = {},
    logOutput: TaskLogOutput? = null
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(all = 12.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val stages = tasks.map { t -> key(t.task.id) { t.task.stage.collectAsStateWithLifecycle().value } }
            val progresses = tasks.map { t -> key(t.task.id) { t.task.progress.collectAsStateWithLifecycle().value } }
            val completed = stages.count { it == TaskStage.COMPLETED }
            val runningIndex = stages.indexOfFirst { it == TaskStage.RUNNING }
            val overall = if (tasks.isEmpty()) 0f else {
                val partial = if (runningIndex >= 0) progresses[runningIndex].coerceIn(0f, 1f) else 0f
                ((completed + partial) / tasks.size).coerceIn(0f, 1f)
            }
            val animatedOverall by androidx.compose.animation.core.animateFloatAsState(overall, label = "task_overall")
            val currentStep = (if (runningIndex >= 0) runningIndex + 1 else completed).coerceIn(0, tasks.size)

            Column(
                modifier = Modifier
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight()
                    .then(if (logOutput != null) Modifier.fillMaxWidth(0.86f) else Modifier.width(460.dp))
                    .cryoGlow(26.dp, selected = true, strength = 0.7f)
                    .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, opaque = true)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CryoTaskHeader(
                    title = title,
                    currentStep = currentStep,
                    total = tasks.size,
                    overall = animatedOverall,
                    stages = stages
                )
                if (logOutput == null) {
                    CryoTaskTimeline(
                        tasks = tasks,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CryoTaskTimeline(
                            tasks = tasks,
                            modifier = Modifier.weight(1f)
                        )
                        TaskLogCard(
                            logOutput = logOutput,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF6B8A).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFFF6B8A).copy(alpha = 0.55f), RoundedCornerShape(50))
                        .clickable(onClick = onCancel),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = Color(0xFFFF6B8A)
                    )
                    Text(
                        text = stringResource(R.string.generic_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF8FA6)
                    )
                }
            }
        }
    }
}

@Composable
private fun CryoTaskHeader(
    title: String,
    currentStep: Int,
    total: Int,
    overall: Float,
    stages: List<TaskStage>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .cryoGlow(14.dp, selected = true)
                    .clip(RoundedCornerShape(14.dp))
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_package_2_outlined),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.cryo_task_step, currentStep, total),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Ice,
                    maxLines = 1
                )
            }
            Text(
                text = "${(overall * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    brush = Brush.verticalGradient(listOf(Color.White, CryoGlassColors.Ice))
                ),
                fontWeight = FontWeight.Black
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stages.forEach { stage ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep.copy(alpha = 0.9f))
                        .then(
                            when (stage) {
                                TaskStage.COMPLETED -> Modifier.cryoBackground(cryoGradient())
                                TaskStage.RUNNING -> Modifier.background(CryoGlassColors.Ice.copy(alpha = 0.55f))
                                TaskStage.PREPARING -> Modifier.border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.2f), RoundedCornerShape(50))
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun CryoTaskTimeline(
    tasks: List<TitledTask>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fadeEdge(state = scrollState),
        state = scrollState,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tasks.size, key = { tasks[it].task.id }) { index ->
            val task = tasks[index]
            CryoTaskStep(
                index = index,
                isLast = index == tasks.lastIndex,
                title = task.title,
                runningIcon = task.runningIcon,
                task = task.task
            )
        }
    }
}

@Composable
private fun CryoTaskStep(
    index: Int,
    isLast: Boolean,
    title: AndroidStringText,
    @DrawableRes
    runningIcon: Int?,
    task: Task
) {
    val taskStage by task.stage.collectAsStateWithLifecycle()
    val taskProgress by task.progress.collectAsStateWithLifecycle()
    val taskMessage by task.message.collectAsStateWithLifecycle()
    val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()
    val running = taskStage == TaskStage.RUNNING
    val done = taskStage == TaskStage.COMPLETED
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (taskStage == TaskStage.PREPARING) 0.55f else 1f)
            .then(
                if (running) {
                    Modifier
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(CryoGlassColors.Ice.copy(alpha = 0.16f), CryoGlassColors.Navy.copy(alpha = 0.5f))
                            )
                        )
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), shape)
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(if (running) Modifier.cryoGlow(17.dp, selected = true) else Modifier)
                .clip(CircleShape)
                .cryoBackground(
                    when {
                        done -> cryoGradient()
                        running -> Brush.linearGradient(listOf(CryoGlassColors.Deep, CryoGlassColors.Navy))
                        else -> Brush.linearGradient(listOf(CryoGlassColors.Deep.copy(alpha = 0.6f), CryoGlassColors.Deep.copy(alpha = 0.6f)))
                    }
                )
                .border(
                    if (running) 2.dp else 1.dp,
                    if (running) cryoBorderBrush() else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = if (done) 0.8f else 0.3f), CryoGlassColors.Ice.copy(alpha = if (done) 0.8f else 0.3f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                done -> Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
                running -> Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(runningIcon ?: R.drawable.ic_download),
                    contentDescription = null,
                    tint = CryoGlassColors.Ice
                )
                else -> Text(
                    text = "%02d".format(index + 1),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null),
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Frost
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AndroidStringText(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (running) FontWeight.Black else FontWeight.SemiBold,
                    color = when {
                        running -> Color.White
                        done -> CryoGlassColors.Frost.copy(alpha = 0.75f)
                        else -> CryoGlassColors.Frost
                    }
                )
            )
            if (running) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelSmall.copy(color = CryoGlassColors.Frost.copy(alpha = 0.85f))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CryoTaskBar(
                        progress = if (taskProgress < 0) null else taskProgress,
                        modifier = Modifier.weight(1f)
                    )
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CryoGlassColors.Ice.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            text = text,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null),
                            color = CryoGlassColors.Ice,
                            maxLines = 1
                        )
                    }
                    if (taskProgress >= 0) {
                        Text(
                            text = "${(taskProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Black,
                            color = CryoGlassColors.Ice
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CryoTaskBar(progress: Float?, modifier: Modifier = Modifier) {
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    val deep = CryoGlassColors.Deep
    val animated by androidx.compose.animation.core.animateFloatAsState(progress ?: 0f, label = "task_bar")
    val sweep = if (progress == null) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "task_sweep")
        transition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.LinearEasing)
            ),
            label = "task_sweep_value"
        ).value
    } else 0f
    androidx.compose.foundation.Canvas(
        modifier = modifier.height(8.dp)
    ) {
        val r = size.height / 2f
        drawRoundRect(color = deep, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        drawRoundRect(
            color = ice.copy(alpha = 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
        )
        if (progress == null) {
            val w = size.width * 0.35f
            val x = size.width * sweep
            val start = x.coerceAtLeast(0f)
            val end = (x + w).coerceAtMost(size.width)
            if (end > start) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(iceSoft, ice, aurora), startX = start, endX = end),
                    topLeft = androidx.compose.ui.geometry.Offset(start, 0f),
                    size = androidx.compose.ui.geometry.Size(end - start, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
                )
            }
        } else {
            val w = size.width * animated.coerceIn(0f, 1f)
            if (w > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(iceSoft, ice, aurora), startX = 0f, endX = w),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
                )
                drawCircle(Color.White, r * 0.8f, androidx.compose.ui.geometry.Offset(w - r, r))
            }
        }
    }
}

/**
 * Painel de memória RAM: quanto o aparelho está usando, quanto está livre, quanto vai para o jogo
 * e um aviso se o valor escolhido cabe na memória livre.
 * @param delay intervalo entre as leituras de memória
 * @param preview memória alocada para o jogo (MB); null esconde a parte do jogo
 */
@Composable
fun MemoryPreview(
    modifier: Modifier = Modifier,
    delay: Long = 1000,
    preview: Double? = null
) {
    val context = LocalContext.current

    //Total e em uso (MB)
    var totalMemory by remember { mutableDoubleStateOf(0.0) }
    var usedMemory by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        infinityCancellableBlock(delay = delay) {
            totalMemory = getTotalMemory(context).bytesToMB()
            usedMemory = getUsedMemory(context).bytesToMB()
        }
    }

    val freeMemory = (totalMemory - usedMemory).coerceAtLeast(0.0)
    val game = preview?.coerceAtLeast(0.0)
    val usedRatio by animateFloatAsState(
        targetValue = if (totalMemory > 0) (usedMemory / totalMemory).toFloat().coerceIn(0f, 1f) else 0f,
        label = "ram_used"
    )
    val gameRatio by animateFloatAsState(
        targetValue = if (totalMemory > 0 && game != null) (game / totalMemory).toFloat().coerceIn(0f, 1f - usedRatio) else 0f,
        label = "ram_game"
    )
    val leftover = if (game != null) freeMemory - game else null
    val overflow = leftover != null && leftover < 0
    val (statusColor, statusText) = when {
        totalMemory <= 0.0 || leftover == null -> CryoGlassColors.Frost to null
        leftover >= 1024 -> Color(0xFF7CF5A8) to stringResource(R.string.cryo_ram_status_ok, formatRam(leftover))
        leftover >= 0 -> Color(0xFFFFC94D) to stringResource(R.string.cryo_ram_status_tight, formatRam(leftover))
        else -> Color(0xFFFF6B81) to stringResource(R.string.cryo_ram_status_over, formatRam(-leftover))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CryoGlassColors.Navy.copy(alpha = 0.78f), CryoGlassColors.Deep.copy(alpha = 0.88f))
                )
            )
            .border(1.dp, cryoBorderBrush(), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        //Números
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RamStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.cryo_ram_used),
                value = usedMemory,
                dot = CryoGlassColors.Frost.copy(alpha = 0.55f)
            )
            RamStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.cryo_ram_free),
                value = freeMemory,
                dot = Color.White.copy(alpha = 0.15f)
            )
            if (game != null) {
                RamStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.cryo_ram_game),
                    value = game,
                    dot = CryoGlassColors.Ice,
                    highlight = true
                )
            }
            RamStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.cryo_ram_total),
                value = totalMemory,
                dot = null
            )
        }

        //Barra: em uso | jogo | livre
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(50))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (usedRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(usedRatio)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CryoGlassColors.Frost.copy(alpha = 0.35f), CryoGlassColors.Frost.copy(alpha = 0.55f))
                                )
                            )
                    )
                }
                if (gameRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(gameRatio)
                            .background(
                                if (overflow) Brush.horizontalGradient(listOf(Color(0xFFFF6B81), Color(0xFFFF9EAE)))
                                else Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora))
                            )
                    )
                }
                val rest = 1f - usedRatio - gameRatio
                if (rest > 0.0001f) Spacer(modifier = Modifier.weight(rest))
            }
        }

        if (statusText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun RamStat(
    modifier: Modifier,
    label: String,
    value: Double,
    dot: Color?,
    highlight: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (highlight) Brush.verticalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.26f), CryoGlassColors.Ice.copy(alpha = 0.08f)))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (highlight) 0.75f else 0.2f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (dot != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = if (highlight) CryoGlassColors.Ice else CryoGlassColors.Frost.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatRam(value),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = "${value.toInt()} MB",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
            color = CryoGlassColors.Frost.copy(alpha = 0.55f),
            maxLines = 1
        )
    }
}

/** 4096 MB -> "4,0 GB" (separador decimal do idioma do aparelho). */
private fun formatRam(mb: Double): String = String.format(java.util.Locale.getDefault(), "%.1f GB", mb / 1024.0)

private suspend fun infinityCancellableBlock(
    delay: Long = 1000,
    block: suspend () -> Unit
) = withContext(Dispatchers.Default) {
    while (true) {
        try {
            block()
            ensureActive()
            delay(delay.milliseconds)
        } catch (_: CancellationException) {
            break
        }
    }
}
