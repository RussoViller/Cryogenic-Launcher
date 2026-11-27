package com.russoviller.cryogenic.ui.screens.content.chunks

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.chunks.ChunkWorld
import com.russoviller.cryogenic.game.seedmap.SeedBiomes
import com.russoviller.cryogenic.game.seedmap.SeedDimension
import com.russoviller.cryogenic.game.seedmap.SeedNative
import com.russoviller.cryogenic.game.seedmap.SeedStructure
import com.russoviller.cryogenic.game.seedmap.SeedVersion
import com.russoviller.cryogenic.game.seedmap.SeedVersions
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoAlertDialog
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * ChunkBase: escolhe a versão e o mapa, pega a seed do mapa e mostra os biomas e as estruturas
 * gerados por ela (cubiomes). Estruturas e biomas mudam conforme a versão escolhida.
 */

private const val SEED_MIN_SCALE = 1f / 256f
private const val SEED_MAX_SCALE = 4f
private const val SEED_ZOOM_STEP = 1.5f
/** Raio da busca de biomas, em blocos. */
private const val SEARCH_RADIUS = 6000

@Composable
fun ChunkBaseScreen(
    key: NormalNavKey.ChunkBase,
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
            label = "seedStage",
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp)
        ) { s ->
            when (s) {
                0 -> VersionPicker(iconRes = R.drawable.ic_public, onPick = { version = it })
                1 -> version?.let { v ->
                    WorldPicker(v, onBack = { version = null }, withSeed = true, iconRes = R.drawable.ic_public, onPick = { world = it })
                }
                else -> {
                    val v = version
                    val w = world
                    if (v != null && w != null) SeedMapEditor(w, v, onBack = { world = null })
                }
            }
        }
    }
}

/**
 * ChunkBase dentro do jogo (menu CRYO-MODE): abre direto no mundo que está aberto agora
 * (o session.lock mais recente da pasta saves); dá para voltar e escolher outro mundo.
 */
@Composable
fun CryoChunkBaseGamePanel(version: Version?) {
    if (version == null) {
        EmptyNote(stringResource(R.string.cryo_chunks_no_worlds))
        return
    }
    var world by remember(version) { mutableStateOf<WorldEntry?>(null) }
    var checked by remember(version) { mutableStateOf(false) }
    LaunchedEffect(version) {
        world = withContext(Dispatchers.IO) {
            runCatching {
                File(version.getGameDir(), "saves").listFiles()
                    ?.filter { File(it, "level.dat").isFile && File(it, "session.lock").isFile }
                    ?.maxByOrNull { File(it, "session.lock").lastModified() }
                    ?.let { loadWorldEntry(it, withSeed = true) }
            }.getOrNull()
        }
        checked = true
    }
    if (!checked) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CryoGlassColors.Ice) }
        return
    }
    val w = world
    if (w == null) {
        WorldPicker(version, onBack = null, withSeed = true, iconRes = R.drawable.ic_public, onPick = { world = it })
    } else {
        SeedMapEditor(w, version, onBack = { world = null })
    }
}

@Composable
private fun SeedMapEditor(world: WorldEntry, version: Version, onBack: () -> Unit) {
    val seed = world.seed
    val sv: SeedVersion = remember(version, world) {
        SeedVersions.of(version.getVersionInfo()?.minecraftVersion ?: world.mcVersion)
    }
    if (!SeedNative.available || seed == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .cryoGlass(shape = cryoGlassShape(22.dp), cornerRadius = 22.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepHeader(world.name, world.dir.name, onBack, R.drawable.ic_public)
            EmptyNote(stringResource(if (seed == null) R.string.cryo_seedmap_no_seed else R.string.cryo_seedmap_no_native))
        }
        return
    }
    SeedMapContent(world, seed, sv, onBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeedMapContent(
    world: WorldEntry,
    seed: Long,
    sv: SeedVersion,
    onBack: () -> Unit,
    sourceFactory: (dim: SeedDimension, large: Boolean, caveY: Int?) -> SeedSource = { d, l, y -> NativeSeedSource(sv.mc, seed, d.id, l, y) }
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var dim by remember { mutableStateOf(SeedDimension.Overworld) }
    var large by remember(world) { mutableStateOf(world.largeBiomes) }
    //cavernas: biomas numa altura Y (só 1.18+ e só no Overworld)
    val caveSupport = SeedVersions.hasCaveBiomes(sv.mc)
    var caves by remember { mutableStateOf(false) }
    var caveY by remember { mutableStateOf(0) }
    var caveYDraft by remember { mutableFloatStateOf(0f) }
    val caveKey: Int? = if (caves && caveSupport && dim == SeedDimension.Overworld) caveY else null
    val tiles = remember(dim, large, caveKey) { SeedMapTiles(sourceFactory(dim, large, caveKey)) }
    DisposableEffect(tiles) { onDispose { tiles.dispose() } }

    val spawn by produceState<Pair<Int, Int>?>(null, world) {
        value = withContext(Dispatchers.IO) { runCatching { ChunkWorld.readSpawn(world.dir) }.getOrNull() }
    }
    var centerX by remember(world) { mutableFloatStateOf(Float.NaN) }
    var centerZ by remember(world) { mutableFloatStateOf(Float.NaN) }
    var scale by remember { mutableFloatStateOf(0.25f) }
    var stick by remember { mutableStateOf(Offset.Zero) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

    //estruturas que existem na versão (por dimensão) e as escolhidas
    val available = remember(sv.mc) { SeedStructure.entries.associateWith { it.dimensionIn(sv.mc) } }
    var chosen by remember { mutableStateOf(setOf(SeedStructure.Village, SeedStructure.Stronghold, SeedStructure.Fortress, SeedStructure.Bastion, SeedStructure.EndCity)) }
    val dimStructures = SeedStructure.entries.filter { available[it] == dim }
    var picked by remember { mutableStateOf<Triple<SeedStructure, Int, Int>?>(null) }

    //biomas
    val biomeIds = remember(sv.mc, dim) { SeedBiomes.ids(sv.mc, dim) }
    var filter by remember(dim) { mutableStateOf<Set<Int>>(emptySet()) }
    var showBiomes by remember { mutableStateOf(false) }
    var showStructures by remember { mutableStateOf(false) }
    var spots by remember(dim, large, caveKey) { mutableStateOf<List<BiomeSpot>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchCenter by remember { mutableStateOf(0 to 0) }

    fun runSearch() {
        if (filter.isEmpty() || centerX.isNaN()) {
            spots = null
            return
        }
        val cx = centerX.roundToInt()
        val cz = centerZ.roundToInt()
        searchCenter = cx to cz
        searching = true
        val ids = filter
        tiles.search(ids, cx, cz, SEARCH_RADIUS) { result ->
            spots = result
            searching = false
        }
    }

    LaunchedEffect(filter, tiles) {
        tiles.setFilter(filter)
        runSearch()
    }
    LaunchedEffect(spawn, world) {
        if (centerX.isNaN()) {
            centerX = (spawn?.first ?: 0).toFloat()
            centerZ = (spawn?.second ?: 0).toFloat()
        }
    }
    //analógico
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
                last = 0L
                snapshotFlow { stick }.first { it != Offset.Zero }
            }
        }
    }

    val markerPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            color = android.graphics.Color.WHITE
        }
    }
    val structureNames = SeedStructure.entries.associateWith { stringResource(it.titleRes) }
    //o que está desenhado agora (para o toque achar a estrutura)
    val drawnMarkers = remember { ArrayList<Triple<SeedStructure, Int, Int>>() }

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
            fun toBlock(p: Offset) = Offset(centerX + (p.x - mapSize.width / 2f) / scale, centerZ + (p.y - mapSize.height / 2f) / scale)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .onSizeChanged { mapSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (centerX.isNaN()) return@detectTransformGestures
                            val before = toBlock(centroid)
                            val newScale = (scale * zoom).coerceIn(SEED_MIN_SCALE, SEED_MAX_SCALE)
                            scale = newScale
                            //mantém o ponto embaixo dos dedos no lugar e aplica o arraste
                            centerX = before.x - (centroid.x - mapSize.width / 2f) / newScale - pan.x / newScale
                            centerZ = before.y - (centroid.y - mapSize.height / 2f) / newScale - pan.y / newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { p ->
                            val s = scale
                            var best: Triple<SeedStructure, Int, Int>? = null
                            var bestD = 28.dp.toPx()
                            val list = synchronized(drawnMarkers) { ArrayList(drawnMarkers) }
                            for (m in list) {
                                val sx = (m.second - centerX) * s + mapSize.width / 2f
                                val sz = (m.third - centerZ) * s + mapSize.height / 2f
                                val d = hypot(sx - p.x, sz - p.y)
                                if (d < bestD) { bestD = d; best = m }
                            }
                            picked = best
                        }
                    }
            ) {
                if (centerX.isNaN()) return@Canvas
                tiles.version //redesenha quando algo fica pronto
                val s = scale
                val left = centerX - size.width / 2f / s
                val top = centerZ - size.height / 2f / s
                val right = centerX + size.width / 2f / s
                val bottom = centerZ + size.height / 2f / s
                tiles.draw(this, left, top, s)

                // estruturas
                val shown = chosen.filter { available[it] == dim }
                val markers = tiles.structuresIn(shown, left, top, right, bottom, s)
                synchronized(drawnMarkers) {
                    drawnMarkers.clear()
                    drawnMarkers.addAll(markers)
                }
                val r = 9.dp.toPx()
                markerPaint.textSize = 11.dp.toPx()
                val native = drawContext.canvas.nativeCanvas
                for ((st, x, z) in markers) {
                    val px = (x - left) * s
                    val pz = (z - top) * s
                    drawCircle(Color.Black.copy(alpha = 0.55f), r + 2.dp.toPx(), Offset(px, pz))
                    drawCircle(Color(st.color), r, Offset(px, pz))
                    markerPaint.color = if (luminance(st.color) > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    native.drawText(st.glyph, px, pz + markerPaint.textSize * 0.36f, markerPaint)
                }

                // manchas de bioma achadas
                spots?.let { list ->
                    labelPaint.textSize = 10.dp.toPx()
                    for (sp in list.take(60)) {
                        val px = (sp.x - left) * s
                        val pz = (sp.z - top) * s
                        if (px < -40 || pz < -40 || px > size.width + 40 || pz > size.height + 40) continue
                        drawCircle(Color.Black.copy(alpha = 0.6f), 7.dp.toPx(), Offset(px, pz))
                        drawCircle(Color(0xFFFF3D5A), 5.dp.toPx(), Offset(px, pz))
                        if (s >= 1f / 16f) {
                            val text = "${sp.x}, ${sp.z}"
                            val tw = labelPaint.measureText(text)
                            native.drawRoundRect(
                                px + 9.dp.toPx(), pz - 8.dp.toPx(), px + 15.dp.toPx() + tw, pz + 7.dp.toPx(),
                                4.dp.toPx(), 4.dp.toPx(),
                                android.graphics.Paint().apply { color = 0xB0000000.toInt() }
                            )
                            native.drawText(text, px + 12.dp.toPx(), pz + 3.5f.dp.toPx(), labelPaint)
                        }
                    }
                }

                // estrutura tocada
                picked?.let { (st, x, z) ->
                    val px = (x - left) * s
                    val pz = (z - top) * s
                    drawCircle(Color.White, r + 4.dp.toPx(), Offset(px, pz), style = Stroke(2.dp.toPx()))
                    labelPaint.textSize = 12.dp.toPx()
                    val text = "${structureNames[st]}  $x, $z"
                    val tw = labelPaint.measureText(text)
                    native.drawRoundRect(
                        px - tw / 2 - 6.dp.toPx(), pz - r - 26.dp.toPx(), px + tw / 2 + 6.dp.toPx(), pz - r - 6.dp.toPx(),
                        6.dp.toPx(), 6.dp.toPx(),
                        android.graphics.Paint().apply { color = 0xE0101828.toInt() }
                    )
                    native.drawText(text, px - tw / 2, pz - r - 12.dp.toPx(), labelPaint)
                }

                // mira no centro
                val c = Offset(size.width / 2f, size.height / 2f)
                val arm = 8.dp.toPx()
                drawLine(Color.White.copy(alpha = 0.85f), c - Offset(arm, 0f), c + Offset(arm, 0f), 1.5f.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.85f), c - Offset(0f, arm), c + Offset(0f, arm), 1.5f.dp.toPx())
            }

            // coordenada do centro
            Text(
                if (centerX.isNaN()) "" else "X ${centerX.roundToInt()}  Z ${centerZ.roundToInt()}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            //estruturas escolhidas que só aparecem com mais zoom
            val hidden = chosen.filter { available[it] == dim && scale < it.minZoom }
            if (hidden.isNotEmpty()) {
                Text(
                    stringResource(R.string.cryo_seedmap_need_zoom, hidden.joinToString(", ") { structureNames[it] ?: "" }),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 44.dp, start = 60.dp, end = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (caveKey != null) {
                Text(
                    stringResource(R.string.cryo_seedmap_caves_y, caveKey),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Ice,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (tiles.version >= 0 && tiles.isBusy()) {
                CircularProgressIndicator(
                    color = CryoGlassColors.Ice,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }
            Joystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                onMove = { stick = it }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundIcon(R.drawable.ic_add, size = 46) { scale = (scale * SEED_ZOOM_STEP).coerceAtMost(SEED_MAX_SCALE) }
                RoundIcon(R.drawable.ic_remove, size = 46) { scale = (scale / SEED_ZOOM_STEP).coerceAtLeast(SEED_MIN_SCALE) }
            }
        }

        // ------------------------------------------------ painel
        Column(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight()
                .cryoGlass(shape = cryoGlassShape(20.dp), cornerRadius = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIcon(R.drawable.ic_arrow_back, size = 34, onClick = onBack)
                Column(modifier = Modifier.weight(1f)) {
                    Text(world.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FolderLine(world.dir.name)
                }
            }
            //seed (toque copia)
            val copied = stringResource(R.string.cryo_seedmap_copied)
            Text(
                stringResource(R.string.cryo_seedmap_seed, seed.toString()),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(seed.toString()))
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    }
                    .padding(2.dp)
            )
            Text(
                stringResource(if (sv.approximate) R.string.cryo_seedmap_generation_approx else R.string.cryo_seedmap_generation, sv.label),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )

            SectionLabel(stringResource(R.string.cryo_chunks_dimension))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SeedDimension.entries.forEach { d ->
                    Pill(stringResource(d.titleRes), selected = d == dim) {
                        if (d != dim) {
                            dim = d
                            picked = null
                        }
                    }
                }
            }
            if (dim == SeedDimension.Overworld) {
                CompactToggle(stringResource(R.string.cryo_seedmap_large_biomes), large) { large = it; picked = null }
                SectionLabel(stringResource(R.string.cryo_seedmap_layer))
                if (!caveSupport) {
                    Text(stringResource(R.string.cryo_seedmap_caves_old), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.8f))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(stringResource(R.string.cryo_seedmap_surface), selected = !caves) { caves = false }
                        Pill(stringResource(R.string.cryo_seedmap_caves), selected = caves) {
                            caves = true
                            caveYDraft = caveY.toFloat()
                        }
                    }
                    if (caves) {
                        Text(
                            stringResource(R.string.cryo_seedmap_caves_y, caveYDraft.roundToInt()),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = CryoGlassColors.Ice
                        )
                        androidx.compose.material3.Slider(
                            value = caveYDraft,
                            onValueChange = { caveYDraft = (it / 8f).roundToInt() * 8f },
                            onValueChangeFinished = { caveY = caveYDraft.roundToInt() },
                            valueRange = -64f..256f,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = CryoGlassColors.Ice,
                                activeTrackColor = CryoGlassColors.Ice,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Text(stringResource(R.string.cryo_seedmap_caves_note), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.75f))
                    }
                }
            }

            SectionLabel(stringResource(R.string.cryo_seedmap_structures))
            if (dimStructures.isEmpty()) {
                Text("—", color = CryoGlassColors.Frost)
            } else {
                val shownHere = dimStructures.filter { it in chosen }
                Pill(stringResource(R.string.cryo_seedmap_structures_count, shownHere.size), selected = shownHere.isNotEmpty()) { showStructures = true }
                if (shownHere.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        shownHere.forEach { st ->
                            StructureChip(structureNames[st] ?: "", st.color, true) {
                                chosen = chosen - st
                                if (picked?.first == st) picked = null
                            }
                        }
                    }
                }
            }
            picked?.let { (st, x, z) ->
                Text(
                    stringResource(R.string.cryo_seedmap_selected, structureNames[st] ?: "", x, z),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { centerX = x.toFloat(); centerZ = z.toFloat() }
                        .padding(2.dp)
                )
            }

            SectionLabel(stringResource(R.string.cryo_seedmap_biomes))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(
                    if (filter.isEmpty()) stringResource(R.string.cryo_seedmap_filter_biomes)
                    else stringResource(R.string.cryo_seedmap_filter_count, filter.size),
                    selected = filter.isNotEmpty()
                ) { showBiomes = true }
                if (filter.isNotEmpty()) Pill(stringResource(R.string.cryo_seedmap_clear), selected = false) { filter = emptySet() }
            }
            if (filter.isNotEmpty()) {
                Pill(stringResource(R.string.cryo_seedmap_search_here), selected = false) { runSearch() }
                val list = spots
                when {
                    searching || list == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressIndicator(color = CryoGlassColors.Ice, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.cryo_seedmap_searching), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost)
                    }
                    list.isEmpty() -> Text(stringResource(R.string.cryo_seedmap_none_found, SEARCH_RADIUS), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost)
                    else -> {
                        Text(
                            stringResource(R.string.cryo_seedmap_found, list.size, SEARCH_RADIUS, searchCenter.first, searchCenter.second),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                        )
                        list.take(80).forEach { sp ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable { centerX = sp.x.toFloat(); centerZ = sp.z.toFloat() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(SeedBiomes.color(sp.biome))))
                                    Text(SeedBiomes.name(sv.mc, sp.biome), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    "${sp.x}, ${sp.z}  ·  ${sp.distance} m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = CryoGlassColors.Ice
                                )
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.cryo_seedmap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        }
    }

    if (showStructures) {
        var draft by remember { mutableStateOf(chosen) }
        CryoAlertDialog(
            title = stringResource(R.string.cryo_seedmap_structure_catalog),
            onDismissRequest = { showStructures = false },
            confirmText = stringResource(R.string.cryo_seedmap_apply),
            onConfirm = {
                chosen = draft
                if (picked?.first !in draft) picked = null
                showStructures = false
            },
            dismissText = stringResource(R.string.cryo_seedmap_none),
            onCancel = { draft = draft - dimStructures.toSet() },
            iconRes = R.drawable.ic_public
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(stringResource(R.string.cryo_seedmap_all), selected = false) { draft = draft + dimStructures }
                    Pill(stringResource(dim.titleRes), selected = true) {}
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                    items(dimStructures, key = { it.name }) { st ->
                        val on = st in draft
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) Color(st.color).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { draft = if (on) draft - st else draft + st }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(st.color)).border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(st.glyph, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = if (luminance(st.color) > 0.55f) Color.Black else Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(structureNames[st] ?: "", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                if (st.minZoom > 1f / 64f) {
                                    Text(stringResource(R.string.cryo_seedmap_zoom_tag), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.7f))
                                }
                            }
                            if (on) Text("✓", color = CryoGlassColors.Ice, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (showBiomes) {
        var draft by remember { mutableStateOf(filter) }
        var query by remember { mutableStateOf("") }
        val named = remember(biomeIds) { biomeIds.map { it to SeedBiomes.name(sv.mc, it) }.sortedBy { it.second } }
        CryoAlertDialog(
            title = stringResource(R.string.cryo_seedmap_filter_biomes),
            onDismissRequest = { showBiomes = false },
            confirmText = stringResource(R.string.cryo_seedmap_apply),
            onConfirm = {
                filter = draft
                showBiomes = false
            },
            dismissText = stringResource(R.string.cryo_seedmap_clear),
            onCancel = { draft = emptySet() },
            iconRes = R.drawable.ic_public
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (query.isEmpty()) Text(stringResource(R.string.cryo_seedmap_search_hint), style = MaterialTheme.typography.labelMedium, color = CryoGlassColors.Frost.copy(alpha = 0.6f))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        cursorBrush = SolidColor(CryoGlassColors.Ice),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val shown = named.filter { query.isBlank() || it.second.contains(query.trim(), ignoreCase = true) }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                    items(shown, key = { it.first }) { (id, name) ->
                        val on = id in draft
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) CryoGlassColors.Ice.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { draft = if (on) draft - id else draft + id }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(SeedBiomes.color(id))).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                            Text(name, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.weight(1f))
                            if (on) Text("✓", color = CryoGlassColors.Ice, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StructureChip(text: String, color: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(color).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) Color(color) else CryoGlassColors.Ice.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(color)))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}

private fun luminance(argb: Int): Float {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b
}
