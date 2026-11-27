package com.russoviller.cryogenic.ui.screens.content.elements

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.wardrobe.OfficialCape
import com.russoviller.cryogenic.game.account.wardrobe.OfficialCapes
import com.russoviller.cryogenic.game.account.isLocalAccount
import com.russoviller.cryogenic.game.account.wardrobe.EmptyCape
import com.russoviller.cryogenic.game.account.yggdrasil.PlayerProfile
import com.russoviller.cryogenic.game.account.yggdrasil.getFile
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.russoviller.cryogenic.ui.components.safeRadialGradient

fun capeFrontOf(bytes: ByteArray): ImageBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val scale = (bitmap.width / 64).coerceAtLeast(1)
    if (bitmap.width < 22 * scale || bitmap.height < 17 * scale) return null
    val out = Bitmap.createBitmap(bitmap, scale, scale, 10 * scale, 16 * scale)
    return out.asImageBitmap()
}

private object CapeGalleryCache {
    @Volatile
    var capes: List<OfficialCape>? = null
    val bytes = ConcurrentHashMap<String, ByteArray>()
    val previews = ConcurrentHashMap<String, ImageBitmap>()
    val failedPreviews: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val scroll = ConcurrentHashMap<String, Pair<Int, Int>>()
    val downloads = Semaphore(4)
}

private data class CapeEntry(
    val key: String,
    val name: String,
    val official: OfficialCape?,
    val owned: PlayerProfile.Cape?
)

private fun textureHashOf(url: String): String = url.substringBefore('?').substringAfterLast('/').lowercase()

private fun buildCapeEntries(capes: List<OfficialCape>, owned: List<PlayerProfile.Cape>?): List<CapeEntry> {
    if (owned == null) return capes.map { CapeEntry(it.texture, it.alias.ifBlank { it.name }, it, null) }
    val used = HashSet<String>()
    val ownedEntries = owned.filter { it.id.isNotEmpty() }.map { cape ->
        val hash = textureHashOf(cape.url)
        val match = capes.firstOrNull { official ->
            official.texture !in used && (
                cape.alias.equals(official.alias, ignoreCase = true) ||
                    cape.alias.equals(official.name, ignoreCase = true) ||
                    (hash.isNotEmpty() && textureHashOf(official.texture) == hash)
                )
        }
        if (match != null) used.add(match.texture)
        CapeEntry(
            key = match?.texture ?: "owned:${cape.id}",
            name = cape.alias.ifBlank { match?.alias?.ifBlank { match.name } ?: cape.id },
            official = match,
            owned = cape
        )
    }
    val rest = capes.filter { it.texture !in used }.map { CapeEntry(it.texture, it.alias.ifBlank { it.name }, it, null) }
    return ownedEntries + rest
}

private fun downloadCapeBytes(entry: CapeEntry): ByteArray? {
    return try {
        entry.official?.let { OfficialCapes.download(it) }
            ?: entry.owned?.let { cape ->
                val file = cape.getFile(PathManager.DIR_ACCOUNT_CAPE)
                if (file.exists()) file.readBytes() else java.net.URL(cape.url).readBytes()
            }
    } catch (_: Exception) {
        null
    }
}

private suspend fun loadCapeBytes(entry: CapeEntry, priority: Boolean = false): ByteArray? {
    CapeGalleryCache.bytes[entry.key]?.let { return it }
    val bytes = withContext(Dispatchers.IO) {
        if (priority) {
            downloadCapeBytes(entry) ?: downloadCapeBytes(entry)
        } else {
            CapeGalleryCache.downloads.withPermit {
                CapeGalleryCache.bytes[entry.key] ?: downloadCapeBytes(entry)
            }
        }
    }
    if (bytes != null) CapeGalleryCache.bytes[entry.key] = bytes
    kotlinx.coroutines.yield()
    return bytes
}

@Composable
fun OfflineCapeDialog(
    account: Account,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    owned: List<PlayerProfile.Cape>? = null,
    selectedOwned: PlayerProfile.Cape? = null,
    onSelectOwned: (PlayerProfile.Cape) -> Unit = {}
) {
    val microsoft = owned != null
    val local = account.isLocalAccount()
    val scope = rememberCoroutineScope()
    val targetFile = remember(account.uniqueUUID) { if (local) account.getCapeFile() else account.getCapeOverrideFile() }
    val sourceFile = remember(account.uniqueUUID) { account.getCapeSourceFile() }

    var capes by remember { mutableStateOf(CapeGalleryCache.capes) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var hasTarget by remember { mutableStateOf(targetFile.exists()) }
    var activeKey by remember {
        mutableStateOf(
            if (targetFile.exists()) runCatching { sourceFile.readText().trim() }.getOrNull()?.ifEmpty { null } else null
        )
    }
    val previews = remember {
        mutableStateMapOf<String, ImageBitmap?>().apply { putAll(CapeGalleryCache.previews) }
    }

    LaunchedEffect(Unit) {
        if (capes == null) {
            val result = withContext(Dispatchers.IO) { runCatching { OfficialCapes.fetch() } }
            result.onSuccess {
                CapeGalleryCache.capes = it
                capes = it
            }.onFailure { failed = true }
        }
    }

    val ownedHolder = remember { arrayOfNulls<List<PlayerProfile.Cape>>(1) }
    if (owned != null && (owned.isNotEmpty() || ownedHolder[0] == null)) ownedHolder[0] = owned
    val stableOwned = if (owned == null) null else ownedHolder[0]
    val entries = remember(capes, stableOwned) { capes?.let { buildCapeEntries(it, stableOwned) } }
    val ownedCount = remember(entries) { entries?.count { it.owned != null } ?: 0 }

    val scrollKey = account.uniqueUUID
    val scrollState = rememberScrollState(CapeGalleryCache.scroll[scrollKey]?.first ?: 0)
    val gesture = remember { CapeScrollGesture() }
    var anchor by remember { mutableIntStateOf(scrollState.value) }
    LaunchedEffect(scrollState) {
        var userSession = false
        var wasInProgress = false
        snapshotFlow {
            CapeScrollSnapshot(scrollState.value, scrollState.maxValue, scrollState.isScrollInProgress, gesture.dragging)
        }.collect { snap ->
            if (snap.inProgress && !wasInProgress) userSession = gesture.isUserDriven()
            val endedUserSession = !snap.inProgress && wasInProgress && userSession
            wasInProgress = snap.inProgress
            if (!snap.inProgress) userSession = false
            if (snap.max == Int.MAX_VALUE) return@collect
            if (snap.dragging || userSession || endedUserSession) {
                anchor = snap.value
                CapeGalleryCache.scroll[scrollKey] = snap.value to 0
                return@collect
            }
            val target = anchor.coerceIn(0, snap.max)
            if (snap.value != target) scrollState.scrollTo(target)
        }
    }

    LaunchedEffect(entries) {
        entries?.forEach { entry ->
            if (previews.containsKey(entry.key) || entry.key in CapeGalleryCache.failedPreviews) return@forEach
            launch {
                val bitmap = loadCapeBytes(entry)?.let { bytes ->
                    withContext(Dispatchers.Default) { runCatching { capeFrontOf(bytes) }.getOrNull() }
                }
                if (bitmap != null) CapeGalleryCache.previews[entry.key] = bitmap
                else CapeGalleryCache.failedPreviews.add(entry.key)
                previews[entry.key] = bitmap
            }
        }
    }

    val density = LocalDensity.current
    val hostHeight = LocalWindowInfo.current.containerSize.height
    val maxDialogHeight = remember(hostHeight, density) {
        with(density) { (hostHeight * 0.96f).toDp() }.takeIf { it > 0.dp } ?: Dp.Unspecified
    }

    var applyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val wardrobeDirty = remember { booleanArrayOf(false) }
    DisposableEffect(Unit) {
        onDispose { if (wardrobeDirty[0]) AccountsManager.refreshWardrobe() }
    }

    fun clearTarget(afterClear: () -> Unit) {
        applyJob?.cancel()
        busy = null
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { targetFile.delete() }
                runCatching { sourceFile.delete() }
            }
            hasTarget = false
            activeKey = null
            afterClear()
            wardrobeDirty[0] = true
            onChanged()
        }
    }

    fun applyEntry(entry: CapeEntry) {
        val ownedCape = entry.owned
        if (microsoft && ownedCape != null) {
            clearTarget { onSelectOwned(ownedCape) }
            return
        }
        applyJob?.cancel()
        busy = entry.key
        applyJob = scope.launch {
            val ok = runCatching {
                val bytes = loadCapeBytes(entry, priority = true) ?: error("download")
                withContext(Dispatchers.IO) {
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(bytes)
                    sourceFile.writeText(entry.key)
                }
            }.isSuccess
            if (busy == entry.key) busy = null
            if (ok) {
                hasTarget = true
                activeKey = entry.key
                wardrobeDirty[0] = true
                onChanged()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.9f)
                .heightIn(max = maxDialogHeight)
                .fillMaxHeight()
                .cryoGlass(shape = cryoChamferShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(cryoChamferShape(10.dp))
                        .cryoBackground(cryoGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_styler),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_capes_kicker),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                        color = CryoGlassColors.Ice
                    )
                    Text(
                        text = stringResource(R.string.cryo_capes_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.generic_close),
                        tint = CryoGlassColors.Ice
                    )
                }
            }
            Text(
                text = stringResource(
                    when {
                        local -> R.string.cryo_capes_summary
                        microsoft -> R.string.cryo_capes_summary_ms
                        else -> R.string.cryo_capes_summary_online
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
            )
            if (microsoft) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OwnedMark()
                    Text(
                        text = stringResource(R.string.cryo_capes_owned_count, ownedCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                val list = entries
                when {
                    list == null && failed -> Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.cryo_capes_failed),
                        color = CryoGlassColors.Frost
                    )
                    list == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val spacing = 10.dp
                        val columns = (((maxWidth - 4.dp) + spacing) / (96.dp + spacing)).toInt().coerceAtLeast(1)
                        val rows = remember(list, columns) { (listOf<CapeEntry?>(null) + list).chunked(columns) }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .capeScrollGesture(gesture)
                                .verticalScroll(scrollState, overscrollEffect = null)
                                .padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(spacing)
                                ) {
                                    row.forEach { entry ->
                                        key(entry?.key ?: "none") {
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (entry == null) {
                                                    CapeTile(
                                                        name = stringResource(R.string.cape_name_none),
                                                        preview = null,
                                                        selected = when {
                                                            hasTarget -> false
                                                            microsoft -> selectedOwned == null || selectedOwned.id.isEmpty()
                                                            else -> true
                                                        },
                                                        loading = false,
                                                        onClick = {
                                                            clearTarget { if (microsoft) onSelectOwned(EmptyCape) }
                                                        }
                                                    )
                                                } else {
                                                    val selected = when {
                                                        hasTarget -> activeKey == entry.key
                                                        microsoft -> entry.owned != null && selectedOwned != null &&
                                                            selectedOwned.id.isNotEmpty() && selectedOwned.id == entry.owned.id
                                                        else -> false
                                                    }
                                                    CapeTile(
                                                        name = entry.name,
                                                        preview = previews[entry.key],
                                                        selected = selected,
                                                        loading = busy == entry.key ||
                                                            (!previews.containsKey(entry.key) && entry.key !in CapeGalleryCache.failedPreviews),
                                                        owned = microsoft && entry.owned != null,
                                                        onClick = { applyEntry(entry) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(columns - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.cryo_capes_note),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.6f)
            )
        }
    }
}

private data class CapeScrollSnapshot(val value: Int, val max: Int, val inProgress: Boolean, val dragging: Boolean)

private class CapeScrollGesture {
    var dragging by mutableStateOf(false)
    var touching = false
    var lastDragRelease = 0L

    fun isUserDriven(): Boolean =
        dragging || (!touching && SystemClock.uptimeMillis() - lastDragRelease < 250L)
}

private fun Modifier.capeScrollGesture(gesture: CapeScrollGesture): Modifier = pointerInput(gesture) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        gesture.touching = true
        val slop = viewConfiguration.touchSlop
        var dragged = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (!dragged) {
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change != null && (change.position - down.position).getDistance() > slop) {
                        dragged = true
                        gesture.dragging = true
                    }
                }
                if (event.changes.none { it.pressed }) break
            }
        } finally {
            gesture.touching = false
            if (dragged) gesture.lastDragRelease = SystemClock.uptimeMillis()
            gesture.dragging = false
        }
    }
}

@Composable
private fun OwnedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Aurora)
            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(11.dp),
            painter = painterResource(R.drawable.ic_star_filled),
            contentDescription = null,
            tint = CryoGlassColors.Deep
        )
    }
}

@Composable
private fun Modifier.capeTap(onClick: () -> Unit): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .indication(interactionSource, LocalIndication.current)
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val slop = viewConfiguration.touchSlop
                var tapped = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        val synthetic = change.isConsumed && change.uptimeMillis == change.previousUptimeMillis
                        val inside = !synthetic && change.position.x in 0f..size.width.toFloat() &&
                            change.position.y in 0f..size.height.toFloat()
                        tapped = inside && (change.position - down.position).getDistance() <= slop * 2f
                        break
                    }
                    if ((change.position - down.position).getDistance() > slop) break
                }
                if (tapped) {
                    interactionSource.tryEmit(PressInteraction.Release(press))
                    currentOnClick()
                } else {
                    interactionSource.tryEmit(PressInteraction.Cancel(press))
                }
            }
        }
}

@Composable
private fun CapeTile(
    name: String,
    preview: ImageBitmap?,
    selected: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    owned: Boolean = false
) {
    val shape = cryoChamferShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy.copy(alpha = 0.85f), CryoGlassColors.Deep)))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) cryoBorderBrush(1.5f) else cryoBorderBrush(0.5f), shape)
            .focusProperties { canFocus = false }
            .capeTap(onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(safeRadialGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.22f), CryoGlassColors.Deep))),
            contentAlignment = Alignment.Center
        ) {
            when {
                preview != null -> Image(
                    modifier = Modifier.size(width = 40.dp, height = 64.dp),
                    bitmap = preview,
                    contentDescription = null,
                    filterQuality = FilterQuality.None
                )
                loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else -> Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint = CryoGlassColors.Frost.copy(alpha = 0.5f)
                )
            }
            if (owned) {
                OwnedMark(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CryoGlassColors.Aurora)
                        .padding(horizontal = 4.dp),
                    text = stringResource(R.string.cryo_capes_owned_badge),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, shadow = null),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .cryoBackground(cryoGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(12.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
