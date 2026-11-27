package com.russoviller.cryogenic.ui.screens.content.settings

import com.russoviller.cryogenic.ui.components.cryoBackground

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.bridge.CursorShape
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import java.io.File
import java.util.zip.ZipInputStream

class SetasEntry(val name: String, val bytes: ByteArray)

class SetasTarget(val shape: CursorShape, val label: String, val file: File)

private val knownRussoNames = mapOf(
    "6c24c665bde4efc5166fe06fc4394471" to CursorShape.Arrow,
    "9c9caa441b225439d2f1dbd3dc5e502d" to CursorShape.Hand,
    "138daa72d9b1259fcea4c2f9036f90f0" to CursorShape.IBeam,
    "1fb3365070e97249ce185e842057d8f4" to CursorShape.CrossHair,
    "8baab25a49f4db8c587b3c680d870e0f" to CursorShape.ResizeNS,
    "6662c8caa21dd049d5bcb85a88623b73" to CursorShape.ResizeEW,
    "ae707f309e4d64ff3b3bf9349d263e0f" to CursorShape.ResizeAll,
    "3c05433e99da09415a6fbe7801e76124" to CursorShape.NotAllowed
)

private val keywordRules: List<Pair<CursorShape, List<String>>> = listOf(
    CursorShape.NotAllowed to listOf("notallowed", "not_allowed", "not-allowed", "unavailable", "forbidden", "blocked", "bloqueado", "proibido", "nao", "no"),
    CursorShape.ResizeAll to listOf("resizeall", "resize_all", "sizeall", "size_all", "move", "mover", "fourway", "all"),
    CursorShape.ResizeNS to listOf("resizens", "resize_ns", "sizens", "size_ns", "ns", "vertical", "vert", "updown", "up_down"),
    CursorShape.ResizeEW to listOf("resizeew", "resize_ew", "sizewe", "size_we", "sizeew", "ew", "we", "horizontal", "horiz", "leftright", "left_right"),
    CursorShape.IBeam to listOf("ibeam", "i_beam", "beam", "text", "texto"),
    CursorShape.CrossHair to listOf("crosshair", "cross", "mira", "precision", "precisao"),
    CursorShape.Hand to listOf("hand", "link", "mao", "finger", "dedo"),
    CursorShape.Arrow to listOf("arrow", "default", "normal", "seta", "principal", "main", "pointer", "cursor")
)

private val imageExtensions = setOf("png", "gif", "webp", "jpg", "jpeg", "bmp")

fun readSetasZip(context: Context, uri: Uri): List<SetasEntry> {
    val result = mutableListOf<SetasEntry>()
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val path = entry.name.replace('\\', '/')
                val name = path.substringAfterLast('/')
                val ext = name.substringAfterLast('.', "").lowercase()
                if (!entry.isDirectory && !path.contains("__MACOSX") && !name.startsWith(".") && ext in imageExtensions) {
                    val bytes = zip.readBytes()
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        result.add(SetasEntry(path, bytes))
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return result
}

private fun tokensOf(name: String): List<String> {
    val base = name.substringAfterLast('/').substringBeforeLast('.').lowercase()
        .replace("ã", "a").replace("á", "a").replace("ç", "c").replace("í", "i")
    return listOf(base, base.replace(Regex("[^a-z0-9]"), "")) + base.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
}

fun classifySetas(entries: List<SetasEntry>): Map<CursorShape, SetasEntry> {
    val assigned = linkedMapOf<CursorShape, SetasEntry>()
    val remaining = entries.toMutableList()
    remaining.toList().forEach { entry ->
        val key = entry.name.substringAfterLast('/').substringBeforeLast('.').lowercase()
        knownRussoNames[key]?.let { shape ->
            if (shape !in assigned) {
                assigned[shape] = entry
                remaining.remove(entry)
            }
        }
    }
    keywordRules.forEach { (shape, words) ->
        if (shape in assigned) return@forEach
        val match = remaining.firstOrNull { entry ->
            val tokens = tokensOf(entry.name)
            words.any { word ->
                if (word.length <= 3) tokens.drop(2).contains(word) else tokens.any { it.contains(word) }
            }
        }
        if (match != null) {
            assigned[shape] = match
            remaining.remove(match)
        }
    }
    return assigned
}

fun applySetas(assignments: Map<CursorShape, SetasEntry>, targets: List<SetasTarget>): Int {
    var count = 0
    assignments.forEach { (shape, entry) ->
        val target = targets.firstOrNull { it.shape == shape } ?: return@forEach
        target.file.parentFile?.mkdirs()
        target.file.writeBytes(entry.bytes)
        count++
    }
    return count
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetasAssignDialog(
    entries: List<SetasEntry>,
    initial: Map<CursorShape, SetasEntry>,
    targets: List<SetasTarget>,
    onConfirm: (Map<CursorShape, SetasEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val assignment = remember { mutableStateMapOf<CursorShape, SetasEntry>().apply { putAll(initial) } }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight()
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, window = true)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_setas_assign_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                ),
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.cryo_setas_assign_summary, initial.size, targets.size),
                style = MaterialTheme.typography.bodySmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targets) { target ->
                    val current = assignment[target.shape]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
                            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (current != null) 0.6f else 0.2f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = target.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = if (current != null) Color.White else CryoGlassColors.Frost.copy(alpha = 0.7f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            entries.forEach { entry ->
                                val selected = current === entry
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .cryoBackground(
                                            if (selected) cryoGradient()
                                            else Brush.linearGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Navy))
                                        )
                                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 1f else 0.2f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            assignment.entries.removeAll { it.value === entry }
                                            if (selected) assignment.remove(target.shape) else assignment[target.shape] = entry
                                        }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        modifier = Modifier.size(38.dp),
                                        model = entry.bytes,
                                        contentDescription = entry.name,
                                        filterQuality = FilterQuality.None
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.generic_cancel))
                }
                CryoButton(
                    modifier = Modifier.weight(1f),
                    enabled = assignment.isNotEmpty(),
                    onClick = { onConfirm(assignment.toMap()) }
                ) {
                    Text(text = stringResource(R.string.cryo_setas_apply, assignment.size))
                }
            }
        }
            }
    }
}
