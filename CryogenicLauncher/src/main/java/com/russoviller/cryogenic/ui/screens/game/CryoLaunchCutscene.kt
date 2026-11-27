package com.russoviller.cryogenic.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.hasVulkanBackend
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.russoviller.cryogenic.ui.components.safeRadialGradient

private const val CUTSCENE_MILLIS = 2200
private const val TOTAL_VISIBLE_MILLIS = 120_000L
private const val HOLD_AFTER_CUTSCENE_MILLIS = TOTAL_VISIBLE_MILLIS - CUTSCENE_MILLIS

private class CutsceneLine(val label: String, val value: String, val icon: Int, val meter: Float? = null)

/**
 * Se a instância vai rodar pela API Vulkan nativa do Minecraft (26.2+), devolve o texto
 * "Vulkan API x.y.z" com a versão exata do driver do aparelho; senão, null.
 * Mesma regra do GameHandler: "Vulkan" forçado na configuração, ou "padrão" com o jogo
 * já configurado em Vulkan no options.txt.
 */
private suspend fun describeNativeVulkan(version: Version): String? {
    if (!version.hasVulkanBackend()) return null
    val api = version.getGraphicsApi()
    val usesVulkan = when (api) {
        com.russoviller.cryogenic.game.version.installed.GraphicsApi.VULKAN -> true
        com.russoviller.cryogenic.game.version.installed.GraphicsApi.OPENGL -> false
        else -> {
            val options = java.io.File(version.getGameDir(), "options.txt")
            options.takeIf { it.isFile }?.useLines { lines ->
                lines.firstOrNull { it.startsWith("preferredGraphicsBackend:") }
            }?.contains("vulkan", ignoreCase = true) == true
        }
    }
    if (!usesVulkan) return null

    val record = com.russoviller.cryogenic.viewmodel.loadVulkanCheckRecord()
    val apiVersion = record?.apiVersion
        ?: runCatching {
            //Sem registro: nível de Vulkan que o sistema declara (sem o número de patch exato)
            val pm = com.russoviller.cryogenic.context.GlobalContext.packageManager
            pm.systemAvailableFeatures.firstOrNull { it.name == android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version?.let { v -> "${v ushr 22}.${(v ushr 12) and 0x3FF}.${v and 0xFFF}" }
        }.getOrNull()
    val driver = record?.takeIf { it.useTurnip }?.driverName?.let { " · $it" }.orEmpty()
    return if (apiVersion != null) "Vulkan API $apiVersion$driver" else "Vulkan API$driver"
}

@Composable
fun CryoLaunchCutscene(
    version: Version,
    gameRendering: Boolean,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val countdown = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(CUTSCENE_MILLIS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        countdown.animateTo(0f, tween(TOTAL_VISIBLE_MILLIS.toInt(), easing = LinearEasing))
        onClose()
    }

    val info = remember(version) { version.getVersionInfo() }
    val loader = info?.primaryLoader
    val rendererName = remember(version) {
        val id = version.getRenderer()
        runCatching { Renderers.getRenderers().firstOrNull { it.getUniqueIdentifier() == id }?.getRendererName() }.getOrNull() ?: id
    }
    //Versões 26.2+ rodando pela API Vulkan nativa do jogo: não usam renderizador do Launcher
    var nativeApi by remember(version) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    LaunchedEffect(version) {
        nativeApi = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { describeNativeVulkan(version) }.getOrNull()
        }
    }
    //Dados extras (aparelho, RAM, mods), lidos fora da thread da interface
    val context = androidx.compose.ui.platform.LocalContext.current
    var extra by remember(version) { androidx.compose.runtime.mutableStateOf<BootExtra?>(null) }
    LaunchedEffect(version) {
        extra = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { readBootExtra(context, version, loader != null) }.getOrNull()
        }
    }
    val ramAlloc = version.getRamAllocation()
    val tiles = listOfNotNull(
        nativeApi?.let { CutsceneLine(stringResource(R.string.cryo_cutscene_native_api), it, R.drawable.ic_gpu) }
            ?: CutsceneLine(stringResource(R.string.cryo_cutscene_renderer), rendererName, R.drawable.ic_gpu),
        version.getJavaRuntime().takeIf { it.isNotBlank() }?.let { CutsceneLine(stringResource(R.string.cryo_cutscene_java), it, R.drawable.ic_java) },
        CutsceneLine(
            label = stringResource(R.string.cryo_cutscene_memory),
            value = extra?.let { "$ramAlloc MB / ${formatGb(it.totalRamMb)}" } ?: "$ramAlloc MB",
            icon = R.drawable.ic_memory,
            meter = extra?.let { (ramAlloc.toFloat() / it.totalRamMb.coerceAtLeast(1)).coerceIn(0f, 1f) }
        ),
        extra?.let { CutsceneLine(stringResource(R.string.cryo_cutscene_free_ram), formatGb(it.freeRamMb), R.drawable.ic_memory) },
        extra?.let { CutsceneLine(stringResource(R.string.cryo_cutscene_device), it.device, R.drawable.ic_cryo_stream) },
        CutsceneLine(stringResource(R.string.cryo_cutscene_resolution), "${com.russoviller.cryogenic.setting.AllSettings.resolutionRatio.state}%", R.drawable.ic_video_settings),
        extra?.mods?.let { CutsceneLine(stringResource(R.string.cryo_cutscene_mods), it.toString(), R.drawable.ic_extension_outlined) },
        extra?.let { CutsceneLine("Android", it.android, R.drawable.ic_setting_launcher) }
    )
    val tips = listOf(
        stringResource(R.string.cryo_cutscene_tip1),
        stringResource(R.string.cryo_cutscene_tip2),
        stringResource(R.string.cryo_cutscene_tip3)
    )
    var tipIndex by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsed++
            if (elapsed % 7 == 0) tipIndex = (tipIndex + 1) % tips.size
        }
    }

    val ambient = rememberInfiniteTransition(label = "cutscene_ambient")
    val sweep by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sweep"
    )
    val blink by ambient.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "blink"
    )
    //Relógio lento (0..1 a cada 9 s) para partículas e anéis
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "drift"
    )

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(450)) + scaleOut(targetScale = 1.04f, animationSpec = tween(450))
    ) {
        val p = progress.value
        val bg = phase(p, 0f, 0.1f)
        val ice = CryoGlassColors.Ice
        val aurora = CryoGlassColors.Aurora
        val ready = Color(0xFF7CF5A8)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.9f * bg),
                            Color.Black.copy(alpha = 0.72f * bg),
                            Color.Black.copy(alpha = 0.5f * bg)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                //Grade em perspectiva no chão
                val horizon = h * 0.62f
                for (i in 0..8) {
                    val t = i / 8f
                    val y = horizon + (h - horizon) * t * t
                    drawLine(ice.copy(alpha = 0.10f * bg * t), Offset(0f, y), Offset(w, y), 1f)
                }
                for (i in -10..10) {
                    val x0 = w / 2f + i * w * 0.045f
                    val x1 = w / 2f + i * w * 0.16f
                    drawLine(ice.copy(alpha = 0.06f * bg), Offset(x0, horizon), Offset(x1, h), 1f)
                }
                //Partículas de gelo subindo
                for (i in 0 until 34) {
                    val seed = (i * 0.61803f) % 1f
                    val speed = 1f + (i % 4)
                    val y = h - ((seed + drift * speed) % 1f) * h
                    val x = ((i * 0.37f) % 1f) * w + kotlin.math.sin((drift * 6.28f * speed) + i) * 10.dp.toPx()
                    val a = kotlin.math.sin(((seed + drift * speed) % 1f) * Math.PI.toFloat())
                    drawCircle((if (i % 3 == 0) aurora else ice).copy(alpha = 0.55f * a * bg), (1f + i % 3) * 0.8.dp.toPx(), Offset(x, y))
                }
                //Feixe de varredura
                val sx = w * sweep
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, ice.copy(alpha = 0.07f * bg), Color.Transparent),
                        startX = sx - w * 0.12f,
                        endX = sx + w * 0.12f
                    )
                )
                //Barras de cima e de baixo + cantos em L
                val bar = phase(p, 0f, 0.3f)
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, ice, aurora, Color.Transparent)),
                    topLeft = Offset(0f, 0f),
                    size = Size(w * bar, 2.dp.toPx())
                )
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, aurora, ice, Color.Transparent)),
                    topLeft = Offset(w * (1f - bar), h - 2.dp.toPx()),
                    size = Size(w * bar, 2.dp.toPx())
                )
                val m = 12.dp.toPx()
                val l = 26.dp.toPx() * bar
                val st = 2.dp.toPx()
                for ((cx, cy, dx, dy) in listOf(
                    listOf(m, m, 1f, 1f), listOf(w - m, m, -1f, 1f),
                    listOf(m, h - m, 1f, -1f), listOf(w - m, h - m, -1f, -1f)
                )) {
                    drawLine(ice.copy(alpha = 0.8f * bg), Offset(cx, cy), Offset(cx + dx * l, cy), st)
                    drawLine(ice.copy(alpha = 0.8f * bg), Offset(cx, cy), Offset(cx, cy + dy * l), st)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Box(
                    modifier = Modifier.size(196.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ReactorCore(
                        modifier = Modifier.fillMaxSize(),
                        progress = p,
                        drift = drift,
                        blink = blink,
                        ready = gameRendering
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { alpha = phase(p, 0.3f, 0.5f) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "T+ %02d:%02d".format(elapsed / 60, elapsed % 60),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                            fontWeight = FontWeight.Black,
                            color = if (gameRendering) ready else Color.White
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val kickerIn = phase(p, 0.02f, 0.18f)
                    Text(
                        modifier = Modifier.graphicsLayer { alpha = kickerIn },
                        text = "SYS // BOOT SEQUENCE" + if (blink > 0.65f) " _" else "",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp),
                        color = ice
                    )
                    //Título com eco colorido (efeito "glitch")
                    val titleIn = phase(p, 0.05f, 0.25f)
                    val titleStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = 2.sp
                    )
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = titleIn
                            translationX = (1f - titleIn) * 60f
                        }
                    ) {
                        val jitter = if (blink > 0.9f) 3f else 1.5f
                        Text(
                            modifier = Modifier.graphicsLayer { translationX = -jitter; alpha = 0.55f },
                            text = "CRYO ENGINE", style = titleStyle, fontWeight = FontWeight.Black, color = aurora, maxLines = 1
                        )
                        Text(
                            modifier = Modifier.graphicsLayer { translationX = jitter; alpha = 0.55f },
                            text = "CRYO ENGINE", style = titleStyle, fontWeight = FontWeight.Black, color = ice, maxLines = 1
                        )
                        Text(
                            text = "CRYO ENGINE",
                            style = titleStyle.copy(brush = Brush.horizontalGradient(listOf(Color.White, Color.White, ice))),
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    //Instância + versão e loader em etiquetas
                    Row(
                        modifier = Modifier.graphicsLayer { alpha = phase(p, 0.12f, 0.3f) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(22.dp)
                                .background(Brush.verticalGradient(listOf(ice, aurora)))
                        )
                        Text(
                            modifier = Modifier.weight(1f, fill = false),
                            text = version.getVersionName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        BootChip(text = "MC ${info?.minecraftVersion ?: "?"}", filled = true)
                        BootChip(text = loader?.let { "${it.loader.displayName} ${it.version}".trim() } ?: "Vanilla", filled = false)
                    }
                    SegmentBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        fill = phase(p, 0.15f, 0.6f),
                        active = if (gameRendering) 1f else blink,
                        scan = if (gameRendering) -1f else sweep,
                        done = gameRendering
                    )
                    //Painel do sistema: 3 colunas
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        var index = 0
                        tiles.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                row.forEach { line ->
                                    val start = 0.18f + index * 0.045f
                                    index++
                                    val a = phase(p, start, start + 0.16f)
                                    InfoTile(
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                alpha = a
                                                translationY = (1f - a) * 24f
                                            },
                                        line = line,
                                        reveal = a
                                    )
                                }
                                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                    val statusIn = phase(p, 0.55f, 0.75f)
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .graphicsLayer { alpha = statusIn }
                            .clip(com.russoviller.cryogenic.ui.components.cryoChamferShape(8.dp))
                            .background(
                                if (gameRendering) ready.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (gameRendering) ready.copy(alpha = 0.7f) else ice.copy(alpha = 0.5f),
                                com.russoviller.cryogenic.ui.components.cryoChamferShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer { alpha = if (gameRendering) 1f else blink }
                                .clip(CircleShape)
                                .background(if (gameRendering) ready else ice)
                        )
                        Text(
                            text = stringResource(if (gameRendering) R.string.cryo_cutscene_ready else R.string.game_loading),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    //Dica que troca a cada 7 s
                    androidx.compose.animation.Crossfade(
                        modifier = Modifier.graphicsLayer { alpha = statusIn },
                        targetState = tipIndex,
                        label = "tip"
                    ) { i ->
                        Text(
                            text = stringResource(R.string.cryo_cutscene_tip_label) + "  " + tips[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            CountdownClose(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                remaining = countdown.value,
                seconds = (countdown.value * TOTAL_VISIBLE_MILLIS / 1000f).toInt() + 1,
                onClose = onClose
            )
        }
    }
}

private class BootExtra(
    val device: String,
    val android: String,
    val totalRamMb: Int,
    val freeRamMb: Int,
    val mods: Int?
)

private fun formatGb(mb: Int): String = String.format(java.util.Locale.US, "%.1f GB", mb / 1024f)

private fun readBootExtra(context: android.content.Context, version: Version, modded: Boolean): BootExtra {
    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val mem = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val soc = if (android.os.Build.VERSION.SDK_INT >= 31) android.os.Build.SOC_MODEL.takeIf { it.isNotBlank() && it != android.os.Build.UNKNOWN } else null
    val mods = if (modded) {
        java.io.File(version.getGameDir(), "mods").listFiles()?.count { it.isFile && it.name.endsWith(".jar", ignoreCase = true) }
    } else null
    return BootExtra(
        device = listOfNotNull(android.os.Build.MODEL, soc).joinToString(" · "),
        android = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
        totalRamMb = (mem.totalMem / 1048576L).toInt(),
        freeRamMb = (mem.availMem / 1048576L).toInt(),
        mods = mods
    )
}

@Composable
private fun BootChip(text: String, filled: Boolean) {
    val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(6.dp)
    Text(
        modifier = Modifier
            .clip(shape)
            .then(
                if (filled) Modifier.background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)))
                else Modifier
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), shape)
            )
            .padding(horizontal = 9.dp, vertical = 2.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, shadow = null),
        fontWeight = FontWeight.Black,
        color = if (filled) CryoGlassColors.Deep else Color.White,
        maxLines = 1
    )
}

@Composable
private fun SegmentBar(modifier: Modifier, fill: Float, active: Float, scan: Float, done: Boolean) {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    Canvas(modifier = modifier) {
        val count = 32
        val gap = 3.dp.toPx()
        val skew = size.height * 0.8f
        val segW = (size.width - gap * (count - 1)) / count
        for (i in 0 until count) {
            val on = (i + 1f) / count <= fill
            val x = i * (segW + gap)
            val t = i / (count - 1f)
            val base = if (done) Color(0xFF7CF5A8) else androidx.compose.ui.graphics.lerp(ice, aurora, t)
            //Enquanto o jogo carrega, uma onda de luz percorre a barra
            val wave = if (scan >= 0f) (1f - kotlin.math.abs(t - scan) * 5f).coerceIn(0f, 1f) else 0f
            val alpha = when {
                !on -> 0.08f
                done -> 1f
                else -> (0.45f + 0.55f * wave).coerceAtMost(1f) * (if (i == (fill * count).toInt() - 1) active else 1f)
            }
            val seg = Path().apply {
                moveTo(x + skew, 0f)
                lineTo(x + segW + skew, 0f)
                lineTo(x + segW, size.height)
                lineTo(x, size.height)
                close()
            }
            drawPath(seg, if (on) base.copy(alpha = alpha) else Color.White.copy(alpha = alpha))
        }
    }
}

@Composable
private fun InfoTile(modifier: Modifier, line: CutsceneLine, reveal: Float) {
    val ice = CryoGlassColors.Ice
    val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(7.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(ice.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, ice.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            modifier = Modifier.size(15.dp),
            painter = painterResource(line.icon),
            contentDescription = null,
            tint = ice
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, fontSize = 8.sp, lineHeight = 10.sp),
                color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val shown = (line.value.length * reveal).toInt().coerceIn(0, line.value.length)
            Text(
                text = line.value.take(shown),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, lineHeight = 14.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            line.meter?.let { meter ->
                //Barra: quanto da RAM do aparelho foi reservada para o jogo (laranja acima de 70%)
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(meter * reveal)
                            .fillMaxHeight()
                            .background(if (meter > 0.7f) Color(0xFFFFB454) else ice)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownClose(modifier: Modifier, remaining: Float, seconds: Int, onClose: () -> Unit) {
    val ice = CryoGlassColors.Ice
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "%02ds".format(seconds),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = ice
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = 2.5.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.15f), radius = size.minDimension / 2f - sw, style = Stroke(sw))
                drawArc(
                    color = ice,
                    startAngle = -90f,
                    sweepAngle = 360f * remaining,
                    useCenter = false,
                    topLeft = Offset(sw, sw),
                    size = Size(size.width - sw * 2, size.height - sw * 2),
                    style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.generic_close),
                tint = Color.White
            )
        }
    }
}

private fun phase(p: Float, start: Float, end: Float): Float =
    ((p - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Núcleo do reator: anéis girando em sentidos opostos, hexágono pulsando, partículas em órbita,
 * varredura de radar e um cristal no centro. Fica verde quando o jogo aparece.
 */
@Composable
internal fun ReactorCore(modifier: Modifier, progress: Float, drift: Float, blink: Float, ready: Boolean) {
    val ice = if (ready) Color(0xFF7CF5A8) else CryoGlassColors.Ice
    val aurora = if (ready) Color(0xFFD7FFE6) else CryoGlassColors.Aurora
    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f
        val grow = phase(progress, 0f, 0.4f)
        val turn = drift * 360f
        val pulse = 0.85f + 0.15f * blink
        //No primeiro quadro tudo ainda tem tamanho zero: um degradê radial de raio 0 derruba o app
        if (grow < 0.02f || r < 1f) return@Canvas

        //Brilho de fundo
        drawCircle(
            Brush.radialGradient(listOf(ice.copy(alpha = 0.34f * grow * pulse), aurora.copy(alpha = 0.10f * grow), Color.Transparent), center = c, radius = r.coerceAtLeast(1f)),
            r, c
        )
        //Anel de marcações (gira devagar)
        rotate(turn * 0.5f, c) {
            for (i in 0 until 72) {
                val a = Math.toRadians(i * 5.0)
                val long = i % 6 == 0
                val r0 = r * (if (long) 0.86f else 0.90f) * grow
                val r1 = r * 0.94f * grow
                drawLine(
                    ice.copy(alpha = if (long) 0.9f else 0.35f),
                    Offset(c.x + kotlin.math.cos(a).toFloat() * r0, c.y + kotlin.math.sin(a).toFloat() * r0),
                    Offset(c.x + kotlin.math.cos(a).toFloat() * r1, c.y + kotlin.math.sin(a).toFloat() * r1),
                    if (long) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }
        //Arcos em sentidos opostos
        fun arcs(radius: Float, speed: Float, count: Int, sweepDeg: Float, width: Float, color: Color) {
            val rr = radius * grow
            rotate(turn * speed, c) {
                repeat(count) { i ->
                    drawArc(
                        color = color,
                        startAngle = i * (360f / count),
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = Offset(c.x - rr, c.y - rr),
                        size = Size(rr * 2, rr * 2),
                        style = Stroke(width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }
        }
        arcs(r * 0.78f, 2f, 3, 70f, 3.dp.toPx(), ice)
        arcs(r * 0.78f, 2f, 3, 70f, 9.dp.toPx(), ice.copy(alpha = 0.18f))
        arcs(r * 0.66f, -3f, 4, 38f, 2.dp.toPx(), aurora)
        arcs(r * 0.54f, 5f, 2, 120f, 1.5.dp.toPx(), ice.copy(alpha = 0.7f))
        //Varredura de radar
        rotate(turn * 4f, c) {
            drawArc(
                brush = Brush.sweepGradient(0f to Color.Transparent, 0.2f to ice.copy(alpha = 0.38f * grow), 0.201f to Color.Transparent, 1f to Color.Transparent, center = c),
                startAngle = 0f,
                sweepAngle = 72f,
                useCenter = true,
                topLeft = Offset(c.x - r * 0.74f, c.y - r * 0.74f),
                size = Size(r * 1.48f, r * 1.48f)
            )
        }
        //Hexágono pulsando
        rotate(-turn, c) {
            val hr = r * 0.44f * grow * pulse
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val a = Math.toRadians(60.0 * i - 30.0)
                    val x = c.x + kotlin.math.cos(a).toFloat() * hr
                    val y = c.y + kotlin.math.sin(a).toFloat() * hr
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(hex, ice.copy(alpha = 0.10f))
            drawPath(hex, ice.copy(alpha = 0.25f), style = Stroke(6.dp.toPx()))
            drawPath(hex, ice, style = Stroke(1.6.dp.toPx()))
        }
        //Partículas em órbita com rastro
        for (i in 0 until 3) {
            val orbit = r * (0.60f + i * 0.13f) * grow
            val base = turn * (3f - i * 0.8f) + i * 120f
            for (k in 0 until 10) {
                val a = Math.toRadians((base - k * 5f).toDouble())
                drawCircle(
                    (if (i == 1) aurora else ice).copy(alpha = (1f - k / 10f) * 0.9f),
                    (3.2f - k * 0.25f).dp.toPx(),
                    Offset(c.x + kotlin.math.cos(a).toFloat() * orbit, c.y + kotlin.math.sin(a).toFloat() * orbit)
                )
            }
        }
        //Cristal central
        val cr = r * 0.27f * grow * pulse
        val top = Offset(c.x, c.y - cr * 1.25f)
        val bottom = Offset(c.x, c.y + cr * 1.25f)
        val left = Offset(c.x - cr * 0.8f, c.y - cr * 0.15f)
        val right = Offset(c.x + cr * 0.8f, c.y - cr * 0.15f)
        val mid = Offset(c.x, c.y - cr * 0.15f)
        fun facet(a: Offset, b: Offset, d: Offset, color: Color) {
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); close() }, color)
        }
        val haloR = (cr * 2f).coerceAtLeast(1f)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f * grow), Color.Transparent), center = c, radius = haloR), haloR, c)
        facet(top, left, mid, Color.White)
        facet(top, right, mid, androidx.compose.ui.graphics.lerp(Color.White, ice, 0.45f))
        facet(bottom, left, mid, androidx.compose.ui.graphics.lerp(Color.White, ice, 0.7f))
        facet(bottom, right, mid, androidx.compose.ui.graphics.lerp(ice, aurora, 0.5f))
        val outline = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }
        drawPath(outline, ice.copy(alpha = 0.35f), style = Stroke(5.dp.toPx()))
        drawPath(outline, Color.White, style = Stroke(1.2.dp.toPx()))
    }
}
