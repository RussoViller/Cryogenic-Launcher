package com.russoviller.cryogenic.ui.screens.content.chunks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.chunks.ChunkInspector
import com.russoviller.cryogenic.game.chunks.ChunkWorld
import com.russoviller.cryogenic.game.chunks.DIM_END
import com.russoviller.cryogenic.game.chunks.DIM_NETHER
import com.russoviller.cryogenic.game.chunks.DIM_OVERWORLD
import com.russoviller.cryogenic.game.chunks.RegionRenderer
import com.russoviller.cryogenic.game.chunks.WorldDimension
import com.russoviller.cryogenic.game.chunks.packXZ
import com.russoviller.cryogenic.game.chunks.unpackX
import com.russoviller.cryogenic.game.chunks.unpackZ
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.game.version.saves.parseLevelDatFile
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoAlertDialog
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoIconOrb
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.CryoToggleTile
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.BitSet
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Apagar chunks (inspirado no MCA Selector do PC): escolhe a versão, o mapa e a dimensão,
 * marca os chunks passando o dedo no mapa e salva. Os chunks apagados são gerados de novo
 * pelo jogo, já com os biomas e estruturas da versão atual.
 */

internal class WorldEntry(
    val dir: File,
    val name: String,
    val mcVersion: String?,
    val icon: ImageBitmap?,
    val seed: Long? = null,
    val largeBiomes: Boolean = false
)

/** Mundo criado com "Biomas grandes" (procura o nome do preset nos arquivos de configuração). */
internal fun detectLargeBiomes(dir: File): Boolean = listOf(
    File(dir, "level.dat"),
    File(dir, "data/minecraft/world_gen_settings.dat")
).any { f ->
    f.isFile && runCatching {
        val bytes = java.util.zip.GZIPInputStream(f.inputStream().buffered()).use { it.readBytes() }
        val text = String(bytes, Charsets.ISO_8859_1)
        text.contains("large_biomes") || text.contains("largeBiomes")
    }.getOrDefault(false)
}

@Composable
fun ChunkEraserScreen(
    key: NormalNavKey.ChunkEraser,
    currentKey: TitledNavKey?
) {
    BaseScreen(screenKey = key, currentKey = currentKey) { isVisible ->
        if (!isVisible) return@BaseScreen
        var version by remember { mutableStateOf<Version?>(null) }
        var world by remember { mutableStateOf<WorldEntry?>(null) }
        val stage = when {
            world != null -> 2
            version != null -> 1
            else -> 0
        }
        AnimatedContent(
            targetState = stage,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(110)) },
            label = "chunkStage",
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp)
        ) { s ->
            when (s) {
                0 -> VersionPicker(onPick = { version = it })
                1 -> version?.let { v -> WorldPicker(v, onBack = { version = null }, onPick = { world = it }) }
                else -> world?.let { w -> ChunkEditor(w, onBack = { world = null }) }
            }
        }
    }
}

// ------------------------------------------------------------------ passo 1: versão

@Composable
internal fun StepHeader(title: String, subtitle: String, onBack: (() -> Unit)?, iconRes: Int = R.drawable.ic_cryo_chunks) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) RoundIcon(R.drawable.ic_arrow_back, onClick = onBack)
        CryoIconOrb(iconRes, size = 34)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.8f), maxLines = 2)
        }
    }
}

@Composable
internal fun VersionPicker(iconRes: Int = R.drawable.ic_cryo_chunks, onPick: (Version) -> Unit) {
    val versions by VersionsManager.versions.collectAsState()
    val list = versions.filter { it.isValid() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .cryoGlass(shape = cryoGlassShape(22.dp), cornerRadius = 22.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepHeader(stringResource(R.string.cryo_chunks_pick_version), stringResource(R.string.cryo_chunks_pick_version_sub), null, iconRes)
        if (list.isEmpty()) {
            EmptyNote(stringResource(R.string.cryo_chunks_no_versions))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(190.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.getVersionName() }) { v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cryoTile()
                            .clickable { onPick(v) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CryoIconOrb(R.drawable.ic_videogame_asset_outlined, size = 30)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.getVersionName(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            v.getVersionInfo()?.minecraftVersion?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CryoGlassColors.Ice, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ passo 2: mapa

/** Lê um mundo da pasta saves (nome, versão, ícone e, se pedido, a seed). */
internal suspend fun loadWorldEntry(dir: File, withSeed: Boolean): WorldEntry {
    val data = runCatching {
        parseLevelDatFile(dir, File(dir, "level.dat"), File(dir, "data/minecraft/world_gen_settings.dat").takeIf { it.isFile })
    }.getOrNull()
    val icon = runCatching {
        File(dir, "icon.png").takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
    }.getOrNull()
    return WorldEntry(
        dir, data?.levelName?.ifBlank { null } ?: dir.name, data?.levelMCVersion, icon,
        seed = data?.worldSeed,
        largeBiomes = withSeed && detectLargeBiomes(dir)
    )
}

@Composable
internal fun WorldPicker(version: Version, onBack: (() -> Unit)?, withSeed: Boolean = false, iconRes: Int = R.drawable.ic_cryo_chunks, onPick: (WorldEntry) -> Unit) {
    val worlds by produceState<List<WorldEntry>?>(null, version) {
        value = withContext(Dispatchers.IO) {
            File(version.getGameDir(), "saves").listFiles()
                ?.filter { File(it, "level.dat").isFile }
                ?.sortedByDescending { File(it, "level.dat").lastModified() }
                ?.map { dir -> loadWorldEntry(dir, withSeed) }
                ?: emptyList()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .cryoGlass(shape = cryoGlassShape(22.dp), cornerRadius = 22.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepHeader(stringResource(R.string.cryo_chunks_pick_world), version.getVersionName(), onBack, iconRes)
        val list = worlds
        when {
            list == null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CryoGlassColors.Ice) }
            list.isEmpty() -> EmptyNote(stringResource(R.string.cryo_chunks_no_worlds))
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.dir.absolutePath }) { w ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cryoTile()
                            .clickable { onPick(w) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CryoGlassColors.Deep),
                            contentAlignment = Alignment.Center
                        ) {
                            if (w.icon != null) Image(bitmap = w.icon, contentDescription = null, contentScale = ContentScale.Crop, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
                            else Icon(painterResource(R.drawable.ic_public), null, tint = CryoGlassColors.Ice, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            //nome da pasta (quase sempre diferente do nome dentro do jogo)
                            FolderLine(w.dir.name)
                            w.mcVersion?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CryoGlassColors.Frost.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FolderLine(folder: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(R.drawable.ic_folder_outlined), null, tint = CryoGlassColors.Ice, modifier = Modifier.size(12.dp))
        Text(
            stringResource(R.string.cryo_chunks_folder, folder),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = CryoGlassColors.Ice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = CryoGlassColors.Frost)
    }
}

// ------------------------------------------------------------------ passo 3: editor

private enum class Tool { Paint, Erase, Area, Inspect }

/** Chunks marcados, por região (índice 0..1023 = x + z*32 dentro da região). */
private class Selection {
    val regions = HashMap<Long, BitSet>()
    var version by mutableIntStateOf(0)
        private set
    var count = 0
        private set
    /** Muda quando a região muda (a imagem de marcação dela é refeita só então). */
    val regionVersion = HashMap<Long, Int>()

    fun isSet(cx: Int, cz: Int): Boolean = regions[packXZ(cx shr 5, cz shr 5)]?.get(index(cx, cz)) == true

    fun set(cx: Int, cz: Int, value: Boolean): Boolean {
        val key = packXZ(cx shr 5, cz shr 5)
        val bits = regions.getOrPut(key) { BitSet(1024) }
        val i = index(cx, cz)
        if (bits[i] == value) return false
        bits[i] = value
        regionVersion[key] = (regionVersion[key] ?: 0) + 1
        count += if (value) 1 else -1
        if (bits.isEmpty) regions.remove(key)
        return true
    }

    fun touch() { version++ }

    fun clear() {
        regions.clear()
        regionVersion.clear()
        count = 0
        version++
    }

    private fun index(cx: Int, cz: Int) = (cx and 31) + (cz and 31) * 32
}

private const val MIN_SCALE = 0.125f
private const val MAX_SCALE = 6f
/** Cada toque no + / − muda pouco o zoom. */
private const val ZOOM_STEP = 1.25f

@Composable
internal fun ChunkEditor(world: WorldEntry, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dims by produceState<List<WorldDimension>?>(null, world) {
        value = withContext(Dispatchers.IO) { ChunkWorld.listDimensions(world.dir) }
    }
    val spawn by produceState<Pair<Int, Int>?>(null, world) {
        value = withContext(Dispatchers.IO) { ChunkWorld.readSpawn(world.dir) }
    }
    var dimId by remember { mutableStateOf(DIM_OVERWORLD) }
    val dimension = dims?.let { list -> list.firstOrNull { it.id == dimId } ?: list.firstOrNull() }
    var reload by remember { mutableIntStateOf(0) }

    //regiões e quais chunks existem (só cabeçalhos: rápido)
    val presence = remember(dimension, reload) { mutableStateMapOf<Long, BitSet>() }
    val files = remember(dimension, reload) { HashMap<Long, File>() }
    var loadingHeaders by remember(dimension, reload) { mutableStateOf(true) }
    LaunchedEffect(dimension, reload) {
        val dim = dimension ?: return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            ChunkWorld.listRegions(dim).mapValues { (_, f) -> f to ChunkWorld.readPresence(f) }
        }
        found.forEach { (k, v) ->
            files[k] = v.first
            presence[k] = v.second
        }
        loadingHeaders = false
    }

    //imagens das regiões: fila fora da tela, níveis de detalhe e cache em disco
    val tiles = remember(dimension, reload) { dimension?.let { ChunkMapTiles(files, it.id, context.cacheDir) } }
    DisposableEffect(tiles) { onDispose { tiles?.dispose() } }

    val selection = remember(dimension) { Selection() }
    val selectionImages = remember(selection) { HashMap<Long, Pair<Int, ImageBitmap>>() }
    fun selectionImage(key: Long): ImageBitmap? {
        val bits = selection.regions[key] ?: return null
        val v = selection.regionVersion[key] ?: 0
        selectionImages[key]?.let { if (it.first == v) return it.second }
        val px = IntArray(1024)
        var i = bits.nextSetBit(0)
        while (i in 0 until 1024) {
            px[i] = 0x8CFF3D5A.toInt()
            i = bits.nextSetBit(i + 1)
        }
        val img = Bitmap.createBitmap(px, 32, 32, Bitmap.Config.ARGB_8888).asImageBitmap()
        selectionImages[key] = v to img
        return img
    }
    var tool by remember { mutableStateOf(Tool.Paint) }
    var showVisited by remember { mutableStateOf(false) }
    var centerX by remember(world) { mutableFloatStateOf(Float.NaN) }
    var centerZ by remember(world) { mutableFloatStateOf(Float.NaN) }
    var scale by remember { mutableFloatStateOf(1f) }
    var hover by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    //forma em desenho: início em blocos do mundo e dedo na tela (o fim acompanha o mapa se ele mexer)
    var shape by remember { mutableStateOf(ChunkShape.Rect) }
    var shapeErase by remember { mutableStateOf(false) }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }
    var shapeFinger by remember { mutableStateOf<Offset?>(null) }
    var stick by remember { mutableStateOf(Offset.Zero) }
    var scanning by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showDimDelete by remember { mutableStateOf(false) }
    var showUnvisited by remember { mutableStateOf(false) }
    //diagnóstico de um chunk (ferramenta Inspecionar)
    var inspect by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    //abre no spawn (ou em 0,0)
    LaunchedEffect(spawn, world) {
        if (centerX.isNaN()) {
            centerX = (spawn?.first ?: 0).toFloat()
            centerZ = (spawn?.second ?: 0).toFloat()
        }
    }
    //analógico: move o mapa enquanto estiver inclinado
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            val v = stick
            if (v != Offset.Zero && !centerX.isNaN()) {
                val speed = 900f / scale
                centerX += v.x * speed * dt
                centerZ += v.y * speed * dt
            }
            if (v == Offset.Zero) {
                //parado: espera o analógico sair do centro, sem rodar a cada frame
                last = 0L
                snapshotFlow { stick }.first { it != Offset.Zero }
            }
        }
    }

    val title = world.name
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ------------------------------------------------ mapa
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF070A10))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
        ) {
            var mapSize by remember { mutableStateOf(IntSize.Zero) }
            fun toChunk(p: Offset): Pair<Int, Int> {
                val bx = centerX + (p.x - mapSize.width / 2f) / scale
                val bz = centerZ + (p.y - mapSize.height / 2f) / scale
                return floor(bx / 16f).toInt() to floor(bz / 16f).toInt()
            }
            fun exists(cx: Int, cz: Int) = presence[packXZ(cx shr 5, cz shr 5)]?.get((cx and 31) + (cz and 31) * 32) == true
            fun toBlock(p: Offset) = Offset(centerX + (p.x - mapSize.width / 2f) / scale, centerZ + (p.y - mapSize.height / 2f) / scale)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    //camada própria: mover o mapa não redesenha o resto da tela
                    .graphicsLayer()
                    .onSizeChanged { mapSize = it }
                    .pointerInput(tool, dimension, reload, shape, shapeErase) {
                        if (tool == Tool.Inspect) {
                            detectTapGestures { p ->
                                val c = toChunk(p)
                                hover = c
                                if (exists(c.first, c.second)) inspect = c
                            }
                        } else if (tool == Tool.Area) {
                            detectDragGestures(
                                onDragStart = { shapeStart = toBlock(it); shapeFinger = it },
                                onDragEnd = {
                                    val a = shapeStart
                                    val f = shapeFinger
                                    if (a != null && f != null) {
                                        val b = toBlock(f)
                                        val value = !shapeErase
                                        shape.forEachChunk(a.x, a.y, b.x, b.y) { cx, cz ->
                                            if (exists(cx, cz)) selection.set(cx, cz, value)
                                        }
                                        selection.touch()
                                    }
                                    shapeStart = null
                                    shapeFinger = null
                                },
                                onDragCancel = { shapeStart = null; shapeFinger = null }
                            ) { change, _ ->
                                change.consume()
                                shapeFinger = change.position
                                hover = toChunk(change.position)
                            }
                        } else {
                            val value = tool == Tool.Paint
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var last = toChunk(down.position)
                                hover = last
                                if (exists(last.first, last.second) && selection.set(last.first, last.second, value)) selection.touch()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    val c = toChunk(change.position)
                                    if (c != last) {
                                        //preenche o caminho entre dois pontos (dedo rápido não pula chunks)
                                        val steps = max(kotlin.math.abs(c.first - last.first), kotlin.math.abs(c.second - last.second))
                                        var changed = false
                                        for (s in 1..steps) {
                                            val cx = last.first + ((c.first - last.first) * s.toFloat() / steps).roundToInt()
                                            val cz = last.second + ((c.second - last.second) * s.toFloat() / steps).roundToInt()
                                            if (exists(cx, cz) && selection.set(cx, cz, value)) changed = true
                                        }
                                        if (changed) selection.touch()
                                        last = c
                                        hover = c
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (centerX.isNaN()) return@Canvas
                selection.version //redesenha quando a seleção muda
                val s = scale
                val left = centerX - size.width / 2f / s
                val top = centerZ - size.height / 2f / s
                val right = centerX + size.width / 2f / s
                val bottom = centerZ + size.height / 2f / s
                val rx1 = floor(left / 512f).toInt()
                val rx2 = floor(right / 512f).toInt()
                val rz1 = floor(top / 512f).toInt()
                val rz2 = floor(bottom / 512f).toInt()
                fun sx(bx: Float) = (bx - left) * s
                fun sz(bz: Float) = (bz - top) * s
                val t = tiles
                t?.revision //redesenha quando uma região fica pronta
                val lod = t?.lodFor(s) ?: 512
                val visibleKeys = HashSet<Long>()
                for (rx in rx1..rx2) for (rz in rz1..rz2) {
                    val key = packXZ(rx, rz)
                    val bits = presence[key] ?: continue
                    visibleKeys += key
                    val img = t?.imageFor(key) ?: t?.presenceImage(key, bits)
                    if (img != null) with(ChunkMapTiles) { drawRegion(img, rx, rz, left, top, right, bottom, s) }
                    //onde jogadores passaram tempo
                    if (showVisited) t?.inhabitedImage(key)?.let { with(ChunkMapTiles) { drawRegion(it, rx, rz, left, top, right, bottom, s) } }
                    //chunks marcados para apagar
                    selectionImage(key)?.let { with(ChunkMapTiles) { drawRegion(it, rx, rz, left, top, right, bottom, s) } }
                }
                t?.frame(visibleKeys, centerX, centerZ, lod)
                //grade de chunks (com zoom suficiente) e de regiões
                val chunkPx = 16f * s
                if (chunkPx >= 10f) {
                    val gc = Color.White.copy(alpha = if (chunkPx >= 24f) 0.12f else 0.06f)
                    var x = floor(left / 16f) * 16f
                    while (x <= right) { val px = sx(x); drawLine(gc, Offset(px, 0f), Offset(px, size.height), 1f); x += 16f }
                    var z = floor(top / 16f) * 16f
                    while (z <= bottom) { val pz = sz(z); drawLine(gc, Offset(0f, pz), Offset(size.width, pz), 1f); z += 16f }
                }
                val rc = CryoGlassColors.Ice.copy(alpha = 0.28f)
                for (rx in rx1..rx2 + 1) { val px = sx(rx * 512f); drawLine(rc, Offset(px, 0f), Offset(px, size.height), 1.5f) }
                for (rz in rz1..rz2 + 1) { val pz = sz(rz * 512f); drawLine(rc, Offset(0f, pz), Offset(size.width, pz), 1.5f) }
                //spawn
                spawn?.let { (x, z) ->
                    val p = Offset(sx(x.toFloat()), sz(z.toFloat()))
                    drawCircle(Color.White, 6.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                    drawCircle(CryoGlassColors.Ice, 3.dp.toPx(), p)
                }
                //forma sendo desenhada (Formas)
                val a = shapeStart
                val f = shapeFinger
                if (a != null && f != null) {
                    val ex = centerX + (f.x - size.width / 2f) / s
                    val ez = centerZ + (f.y - size.height / 2f) / s
                    with(ChunkShape) {
                        drawShapePreview(shape, a.x, a.y, ex, ez, shapeErase, { x, z -> Offset(sx(x), sz(z)) }, s)
                    }
                }
            }

            //info no topo do mapa
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cryo_chunks_selected, selection.let { it.version; it.count }),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7A8E)
                )
                hover?.let { (x, z) ->
                    Text(
                        text = stringResource(R.string.cryo_chunks_coords, x, z),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            if (loadingHeaders || dims == null) {
                Box(Modifier.align(Alignment.Center)) { CircularProgressIndicator(color = CryoGlassColors.Ice) }
            } else if (dims?.isEmpty() == true || presence.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    text = stringResource(R.string.cryo_chunks_no_dims),
                    color = CryoGlassColors.Frost
                )
            }

            //analógico
            Joystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                onMove = { stick = it }
            )
            //zoom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundIcon(R.drawable.ic_add, size = 46) { scale = (scale * ZOOM_STEP).coerceAtMost(MAX_SCALE) }
                RoundIcon(R.drawable.ic_remove, size = 46) { scale = (scale / ZOOM_STEP).coerceAtLeast(MIN_SCALE) }
            }
        }

        // ------------------------------------------------ painel de opções
        Column(
            modifier = Modifier
                .width(214.dp)
                .fillMaxHeight()
                .cryoGlass(shape = cryoGlassShape(20.dp), cornerRadius = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIcon(R.drawable.ic_arrow_back, size = 34, onClick = onBack)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FolderLine(world.dir.name)
                }
            }
            SectionLabel(stringResource(R.string.cryo_chunks_dimension))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dims.orEmpty().forEach { d ->
                    Pill(dimLabel(d.id), selected = d.id == dimension?.id) {
                        if (d.id != dimension?.id) {
                            dimId = d.id
                            hover = null
                        }
                    }
                }
            }
            dimension?.let {
                Text(it.id, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CryoGlassColors.Ice.copy(alpha = 0.8f))
            }
            SectionLabel(stringResource(R.string.cryo_chunks_tools))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolButton(R.drawable.ic_cryo_brush, stringResource(R.string.cryo_chunks_tool_paint), tool == Tool.Paint, Modifier.weight(1f)) { tool = Tool.Paint }
                ToolButton(R.drawable.ic_cryo_eraser, stringResource(R.string.cryo_chunks_tool_erase), tool == Tool.Erase, Modifier.weight(1f)) { tool = Tool.Erase }
                ToolButton(R.drawable.ic_cryo_area, stringResource(R.string.cryo_chunks_tool_shapes), tool == Tool.Area, Modifier.weight(1f)) { tool = Tool.Area }
            }
            ToolButton(R.drawable.ic_cryo_search, stringResource(R.string.cryo_chunks_tool_inspect), tool == Tool.Inspect, Modifier.fillMaxWidth()) { tool = Tool.Inspect }
            if (tool == Tool.Inspect) {
                Text(
                    stringResource(R.string.cryo_chunks_inspect_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
            if (tool == Tool.Area) {
                //formas: quadrado, círculo, triângulo + marcar/desmarcar
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChunkShape.entries.forEach { sh ->
                        ShapeButton(sh.glyph, stringResource(sh.titleRes), shape == sh, Modifier.weight(1f)) { shape = sh }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(stringResource(R.string.cryo_chunks_tool_paint), selected = !shapeErase) { shapeErase = false }
                    Pill(stringResource(R.string.cryo_chunks_tool_erase), selected = shapeErase) { shapeErase = true }
                }
                Text(
                    stringResource(R.string.cryo_chunks_shape_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
            CompactToggle(
                label = stringResource(R.string.cryo_chunks_visited),
                checked = showVisited,
                onChange = { showVisited = it }
            )
            CryoChipButton(
                modifier = Modifier.fillMaxWidth(),
                text = scanning?.let { stringResource(R.string.cryo_chunks_scanning, it) } ?: stringResource(R.string.cryo_chunks_unvisited),
                iconRes = R.drawable.ic_select_all,
                enabled = scanning == null && !busy && presence.isNotEmpty(),
                onClick = { showUnvisited = true }
            )
            CryoChipButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cryo_chunks_clear),
                iconRes = R.drawable.ic_deselect,
                enabled = selection.let { it.version; it.count > 0 } && !busy,
                onClick = { selection.clear() }
            )
            CryoChipButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cryo_chunks_delete_dim),
                iconRes = R.drawable.ic_delete_forever_filled,
                enabled = !dims.isNullOrEmpty() && !busy,
                onClick = { showDimDelete = true }
            )
            Spacer(Modifier.height(2.dp))
            //salvar
            val canSave = selection.let { it.version; it.count > 0 } && !busy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .then(
                        if (canSave) Modifier.cryoBackground(cryoGradient())
                        else Modifier.background(Color.White.copy(alpha = 0.08f))
                    )
                    .clickable(enabled = canSave) { showSave = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CryoGlassColors.Deep)
                else Icon(painterResource(R.drawable.ic_save_filled), null, tint = if (canSave) CryoGlassColors.Deep else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cryo_chunks_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (canSave) CryoGlassColors.Deep else Color.White.copy(alpha = 0.4f)
                )
            }
            Text(
                stringResource(R.string.cryo_chunks_hint),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        }
    }

    // ------------------------------------------------ diálogos

    inspect?.let { (icx, icz) ->
        val regionFile = files[packXZ(icx shr 5, icz shr 5)]
        val report by produceState<String?>(null, icx, icz, regionFile) {
            value = withContext(Dispatchers.IO) {
                regionFile?.let { runCatching { ChunkInspector.report(it, icx, icz) }.getOrElse { e -> e.toString() } } ?: "-"
            }
        }
        fun share(makeFile: (File) -> File?) {
            val f = regionFile ?: return
            scope.launch {
                val out = withContext(Dispatchers.IO) {
                    runCatching { makeFile(File(context.cacheDir, "chunk_export").apply { mkdirs() }) }.getOrNull()
                }
                if (out != null) com.russoviller.cryogenic.utils.file.shareFile(context, out)
            }
        }
        CryoAlertDialog(
            title = stringResource(R.string.cryo_chunks_inspect_title),
            onDismissRequest = { inspect = null },
            confirmText = stringResource(R.string.cryo_chunks_inspect_share_chunk),
            onConfirm = {
                share { dir ->
                    File(dir, "chunk_${icx}_${icz}.nbt").takeIf { ChunkInspector.export(regionFile!!, icx, icz, it) }
                }
            },
            dismissText = stringResource(R.string.cryo_chunks_inspect_close),
            onCancel = { inspect = null },
            iconRes = R.drawable.ic_cryo_search
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            report ?: "…",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White
                        )
                    }
                }
                Pill(stringResource(R.string.cryo_chunks_inspect_share_region), selected = false) {
                    share { dir ->
                        val src = regionFile!!
                        File(dir, src.name).also { src.copyTo(it, overwrite = true) }
                    }
                }
            }
        }
    }

    if (showUnvisited) {
        CryoAlertDialog(
            title = stringResource(R.string.cryo_chunks_unvisited),
            onDismissRequest = { showUnvisited = false },
            confirmText = stringResource(R.string.cryo_chunks_confirm),
            onConfirm = {
                showUnvisited = false
                val keys = files.keys.toList()
                val known = HashMap<Long, LongArray>().apply { keys.forEach { k -> tiles?.inhabitedOf(k)?.let { put(k, it) } } }
                val sources = HashMap(files)
                scope.launch {
                    scanning = 0
                    val picked = withContext(Dispatchers.Default) {
                        val out = HashMap<Long, BitSet>()
                        keys.forEachIndexed { n, key ->
                            val arr = known[key] ?: RegionRenderer.readInhabited(sources.getValue(key))
                            val bits = BitSet(1024)
                            for (i in 0 until 1024) if (arr[i] in 0..1199) bits.set(i)
                            if (!bits.isEmpty) out[key] = bits
                            scanning = ((n + 1) * 100 / keys.size.coerceAtLeast(1))
                        }
                        out
                    }
                    picked.forEach { (key, bits) ->
                        val rx = unpackX(key)
                        val rz = unpackZ(key)
                        var i = bits.nextSetBit(0)
                        while (i in 0 until 1024) {
                            selection.set(rx * 32 + (i and 31), rz * 32 + (i shr 5), true)
                            i = bits.nextSetBit(i + 1)
                        }
                    }
                    selection.touch()
                    scanning = null
                }
            },
            dismissText = stringResource(R.string.cryo_chunks_cancel),
            onCancel = { showUnvisited = false },
            iconRes = R.drawable.ic_select_all
        ) {
            Text(stringResource(R.string.cryo_chunks_unvisited_sub))
        }
    }

    if (showSave) {
        var backup by remember { mutableStateOf(true) }
        val count = selection.count
        CryoAlertDialog(
            title = stringResource(R.string.cryo_chunks_save_title, count),
            onDismissRequest = { showSave = false },
            confirmText = stringResource(R.string.cryo_chunks_save),
            onConfirm = {
                showSave = false
                val dim = dimension ?: return@CryoAlertDialog
                busy = true
                scope.launch {
                    val message = withContext(Dispatchers.IO) {
                        if (ChunkWorld.isWorldInUse(world.dir)) return@withContext context.getString(R.string.cryo_chunks_in_use)
                        runCatching {
                            val dir = if (backup) ChunkWorld.newBackupDir(world.dir, "chunks") else null
                            val snapshot = selection.regions.mapValues { it.value.clone() as BitSet }
                            val n = ChunkWorld.deleteChunks(world.dir, dim, snapshot, dir)
                            buildString {
                                append(context.getString(R.string.cryo_chunks_done, n))
                                if (dir != null) append("\n").append(context.getString(R.string.cryo_chunks_backup_at, dir.relativeTo(world.dir).path))
                            }
                        }.getOrElse { context.getString(R.string.cryo_chunks_failed, it.message ?: it.javaClass.simpleName) }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    selection.clear()
                    reload++
                    busy = false
                }
            },
            dismissText = stringResource(R.string.cryo_chunks_cancel),
            onCancel = { showSave = false },
            danger = true,
            iconRes = R.drawable.ic_delete_filled
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.cryo_chunks_save_sub))
                CryoToggleTile(
                    modifier = Modifier.fillMaxWidth(),
                    iconRes = R.drawable.ic_save_filled,
                    title = stringResource(R.string.cryo_chunks_backup),
                    subtitle = stringResource(R.string.cryo_chunks_backup_sub),
                    checked = backup,
                    onCheckedChange = { backup = it }
                )
            }
        }
    }

    if (showDimDelete) {
        val list = dims.orEmpty()
        var target by remember { mutableStateOf<WorldDimension?>(null) }
        var backup by remember { mutableStateOf(true) }
        var seconds by remember { mutableIntStateOf(10) }
        LaunchedEffect(target) {
            seconds = 10
            if (target == null) return@LaunchedEffect
            while (seconds > 0) {
                delay(1000)
                seconds--
            }
        }
        val ready = target != null && seconds == 0
        CryoAlertDialog(
            title = stringResource(R.string.cryo_chunks_delete_dim),
            onDismissRequest = { showDimDelete = false },
            confirmText = when {
                target == null -> stringResource(R.string.cryo_chunks_confirm)
                seconds > 0 -> stringResource(R.string.cryo_chunks_confirm_wait, seconds)
                else -> stringResource(R.string.cryo_chunks_confirm)
            },
            onConfirm = {
                //só confirma depois dos 10 segundos
                val dim = target ?: return@CryoAlertDialog
                if (!ready) return@CryoAlertDialog
                showDimDelete = false
                busy = true
                scope.launch {
                    val message = withContext(Dispatchers.IO) {
                        if (ChunkWorld.isWorldInUse(world.dir)) return@withContext context.getString(R.string.cryo_chunks_in_use)
                        runCatching {
                            val dir = if (backup) ChunkWorld.newBackupDir(world.dir, "dimension") else null
                            if (!ChunkWorld.deleteDimension(world.dir, dim, dir)) error("I/O")
                            buildString {
                                append(context.getString(R.string.cryo_chunks_dim_done))
                                if (dir != null) append("\n").append(context.getString(R.string.cryo_chunks_backup_at, dir.relativeTo(world.dir).path))
                            }
                        }.getOrElse { context.getString(R.string.cryo_chunks_failed, it.message ?: it.javaClass.simpleName) }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    selection.clear()
                    reload++
                    busy = false
                }
            },
            dismissText = stringResource(R.string.cryo_chunks_back),
            onCancel = { showDimDelete = false },
            danger = true,
            iconRes = R.drawable.ic_delete_forever_filled
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.cryo_chunks_delete_dim_sub))
                listOf(DIM_OVERWORLD, DIM_NETHER, DIM_END).forEach { id ->
                    val dim = list.firstOrNull { it.id == id }
                    val selected = target?.id == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFFFF3D5A).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (selected) Color(0xFFFF3D5A) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable(enabled = dim != null) { target = dim }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            id,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = if (dim != null) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                        Text(dimLabel(id), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = if (dim != null) 0.8f else 0.35f))
                    }
                }
                CryoToggleTile(
                    modifier = Modifier.fillMaxWidth(),
                    iconRes = R.drawable.ic_save_filled,
                    title = stringResource(R.string.cryo_chunks_backup),
                    subtitle = stringResource(R.string.cryo_chunks_backup_dim_sub),
                    checked = backup,
                    onCheckedChange = { backup = it }
                )
            }
        }
    }
}

@Composable
internal fun dimLabel(id: String): String = when (id) {
    DIM_OVERWORLD -> stringResource(R.string.cryo_chunks_dim_overworld)
    DIM_NETHER -> stringResource(R.string.cryo_chunks_dim_nether)
    DIM_END -> stringResource(R.string.cryo_chunks_dim_end)
    else -> id.substringAfter(':')
}

/** Liga/desliga compacto para o painel estreito do editor. */
@Composable
internal fun CompactToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color(0xFFFFA726).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (checked) Color(0xFFFFA726).copy(alpha = 0.7f) else CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(painterResource(R.drawable.ic_person_outlined), null, tint = if (checked) Color(0xFFFFA726) else Color.White, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 20.dp)
                .clip(CircleShape)
                .background(if (checked) Color(0xFFFFA726) else Color.White.copy(alpha = 0.18f)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.padding(3.dp).size(14.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        fontWeight = FontWeight.Black,
        color = CryoGlassColors.Ice
    )
}

@Composable
internal fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.White.copy(alpha = 0.07f)).border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) CryoGlassColors.Deep else Color.White, maxLines = 1)
    }
}

@Composable
private fun ToolButton(iconRes: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.White.copy(alpha = 0.07f)).border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(painterResource(iconRes), null, tint = if (selected) CryoGlassColors.Deep else Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (selected) CryoGlassColors.Deep else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ShapeButton(glyph: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.White.copy(alpha = 0.07f)).border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (selected) CryoGlassColors.Deep else Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (selected) CryoGlassColors.Deep else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun RoundIcon(iconRes: Int, size: Int = 36, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), null, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** Analógico: arrasta o pino; devolve a direção (-1..1) e solta volta ao centro. */
@Composable
internal fun Joystick(modifier: Modifier, onMove: (Offset) -> Unit) {
    val radius = 46.dp
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .size(radius * 2)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.5.dp, CryoGlassColors.Ice.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                val r = radius.toPx()
                val center = Offset(r, r)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun update(p: Offset) {
                        var v = p - center
                        val len = v.getDistance()
                        if (len > r) v *= r / len
                        knob = v
                        onMove(Offset(v.x / r, v.y / r))
                    }
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        update(change.position)
                    }
                    knob = Offset.Zero
                    onMove(Offset.Zero)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(40.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient())
                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
        )
    }
}
