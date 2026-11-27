package com.russoviller.cryogenic.ui.screens.content.home

import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import com.russoviller.cryogenic.filemanager.ui.components.HeroWallpaperStore
import com.russoviller.cryogenic.filemanager.ui.components.PortraitWallpaperStore
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.lerp
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.CryoTheme
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.russoviller.cryogenic.setting.AllSettings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.russoviller.cryogenic.ui.components.CryoIdleMode
import com.russoviller.cryogenic.extras.CryoPresets
import com.russoviller.cryogenic.ui.screens.content.extras.PresetsDialog
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.toAndroidString
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.getAccountTypeName
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.game.versioninfo.MinecraftVersions
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.screens.content.elements.PlayerFace
import com.russoviller.cryogenic.ui.screens.content.elements.VersionIconImage
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import com.russoviller.cryogenic.ui.components.safeRadialGradient

class CryoHomeActions(
    val launch: (Version?) -> Unit,
    val openAccounts: () -> Unit,
    val openVersions: () -> Unit,
    val openVersionSettings: (Version) -> Unit,
    val downloadGame: () -> Unit,
    val downloadMods: () -> Unit,
    val downloadModpacks: () -> Unit,
    val downloadResourcePacks: () -> Unit,
    val downloadShaders: () -> Unit,
    val openFavorites: () -> Unit,
    val openRendererSettings: () -> Unit,
    val openGameSettings: () -> Unit,
    val openControlSettings: () -> Unit,
    val openLauncherSettings: () -> Unit,
    val openJavaSettings: () -> Unit,
    val openHudManager: () -> Unit,
    val openAbout: () -> Unit,
    val downloadSaves: () -> Unit,
    val openFiles: () -> Unit,
    val openWebPage: () -> Unit,
    val openWardrobe: () -> Unit = {},
    val openChunkEraser: () -> Unit = {},
    val openChunkBase: () -> Unit = {},
    val openTikTokLive: () -> Unit = {},
    val openCryoMods: () -> Unit = {},
    val openGamepadSettings: () -> Unit = {},
    val openLauncherExtras: () -> Unit = {}
)

private data class LatestNews(val release: String, val releaseDate: String, val snapshot: String, val snapshotDate: String)

@Composable
fun CryoHome(
    isVisible: Boolean,
    actions: CryoHomeActions,
    modifier: Modifier = Modifier
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    val news by produceState<LatestNews?>(null) {
        value = runCatching {
            val manifest = MinecraftVersions.getVersionManifest()
            val release = manifest.versions.firstOrNull { it.id == manifest.latest.release }
            val snapshot = manifest.versions.firstOrNull { it.id == manifest.latest.snapshot }
            LatestNews(
                release = manifest.latest.release,
                releaseDate = release?.releaseTime?.take(10).orEmpty(),
                snapshot = manifest.latest.snapshot,
                snapshotDate = snapshot?.releaseTime?.take(10).orEmpty()
            )
        }.getOrNull()
    }

    val ctx1 = rememberAnimatedContext(isVisible = isVisible)
    val ctx2 = rememberAnimatedContext(isVisible = isVisible, delayMillis = 70)
    val ctx3 = rememberAnimatedContext(isVisible = isVisible, delayMillis = 140)

    if (CryoIdleMode.active) {
        CryoIdleHome(
            modifier = modifier,
            account = account,
            version = version,
            versions = versions,
            isRefreshing = isRefreshing,
            latestRelease = news?.release,
            actions = actions
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 14.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .animated(ctx1, from = AnimatedDirection.Top),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HeroCard(
                    modifier = Modifier
                        .weight(1.65f)
                        .fillMaxHeight(),
                    account = account,
                    version = version,
                    versions = versions,
                    isRefreshing = isRefreshing,
                    actions = actions
                )
                MinecraftVersionCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    version = version,
                    latestRelease = news?.release,
                    actions = actions
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(352.dp)
                    .animated(ctx2, from = AnimatedDirection.Bottom),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(182.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VersionsListCard(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            versions = versions,
                            current = version,
                            actions = actions
                        )
                        AccountCard(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight(),
                            account = account,
                            onManage = actions.openAccounts,
                            onSkins = if (account != null) actions.openWardrobe else actions.openAccounts
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val featureCatalog = featureCardCatalog(actions)
                        val featureIds = rememberFeatureCardIds(featureCatalog)
                        var editingSlot by remember { mutableStateOf<Int?>(null) }
                        featureIds.forEachIndexed { slot, id ->
                            val item = featureCatalog.first { it.id == id }
                            FeatureCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                iconRes = item.iconRes,
                                title = stringResource(item.titleRes),
                                text = stringResource(item.textRes),
                                action = stringResource(item.actionRes),
                                art = { CardImage(item.imageRes, it) },
                                onClick = item.action,
                                onEdit = { editingSlot = slot }
                            )
                        }
                        editingSlot?.let { slot ->
                            FeatureCardPicker(
                                catalog = featureCatalog,
                                current = featureIds[slot],
                                other = featureIds[1 - slot],
                                onPick = { picked ->
                                    val updated = featureIds.toMutableList().also { it[slot] = picked }
                                    AllSettings.cryoHomeFeatureCards.save(updated.joinToString(","))
                                    editingSlot = null
                                },
                                onDismiss = { editingSlot = null }
                            )
                        }
                    }
                }
                PortraitWallpaperCard(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(9f / 16f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .animated(ctx3, from = AnimatedDirection.Bottom),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ExtrasCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                QuickSettingsCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    actions = actions
                )
            }
        }
    }
}

/** Opacidade dos cartões que não são wallpaper enquanto o modo inativo está ligado. */
private const val IdleCardAlpha = 0.55f

/**
 * Tela inicial no modo inativo: pensada para exibir os 3 wallpapers.
 * - o wallpaper 9:16 sobe e ocupa toda a altura, à direita;
 * - o wallpaper do cartão principal vai para a esquerda (a barra lateral some);
 * - os demais cartões continuam visíveis, esmaecidos, e nada responde ao toque.
 */
@Composable
private fun CryoIdleHome(
    account: Account?,
    version: Version?,
    versions: List<Version>,
    isRefreshing: Boolean,
    latestRelease: String?,
    actions: CryoHomeActions,
    modifier: Modifier = Modifier
) {
    //O botão Recarregar continua funcionando no descanso (o resto não responde ao toque)
    var reloadRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var layerOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HeroCard(
                        modifier = Modifier
                            .weight(1.65f)
                            .fillMaxHeight(),
                        account = account,
                        version = version,
                        versions = versions,
                        isRefreshing = isRefreshing,
                        actions = actions,
                        onReloadBounds = { reloadRect = it }
                    )
                    MinecraftVersionCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(IdleCardAlpha),
                        version = version,
                        latestRelease = latestRelease,
                        actions = actions
                    )
                }
                // O resto da tela inicial continua lá, cortado pela borda de baixo como na tela normal,
                // e some aos poucos para deixar o wallpaper de fundo aparecer.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .graphicsLayer {
                            alpha = IdleCardAlpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.35f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .height(182.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VersionsListCard(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            versions = versions,
                            current = version,
                            actions = actions
                        )
                        AccountCard(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight(),
                            account = account,
                            onManage = actions.openAccounts,
                            onSkins = if (account != null) actions.openWardrobe else actions.openAccounts
                        )
                    }
                }
            }
            PortraitWallpaperCard(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
            )
        }
        // Camada que engole os toques: os cartões ficam visíveis, mas inutilizáveis (menos o Recarregar).
        Box(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { layerOrigin = it.positionInRoot() }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
        )
        // Por cima da camada: área do botão Recarregar, que continua funcionando no descanso
        if (reloadRect.width > 0f && reloadRect.height > 0f) {
            val reload = rememberWallpaperReload()
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            (reloadRect.left - layerOrigin.x).toInt(),
                            (reloadRect.top - layerOrigin.y).toInt()
                        )
                    }
                    .size(
                        width = with(density) { reloadRect.width.toDp() },
                        height = with(density) { reloadRect.height.toDp() }
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = reload)
            )
        }
    }
}

@Composable
private fun PortraitWallpaperCard(modifier: Modifier = Modifier) {
    androidx.compose.runtime.LaunchedEffect(Unit) { PortraitWallpaperStore.ensureLoaded() }
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        CryoWallpaperDialog(
            store = PortraitWallpaperStore,
            opacity = com.russoviller.cryogenic.setting.AllSettings.cryoPortraitWallpaperOpacity,
            titleRes = R.string.cryo_portrait_wallpaper_title,
            summaryRes = R.string.cryo_portrait_wallpaper_summary,
            opacityForDefault = true,
            onDismiss = { showDialog = false }
        )
    }
    val shape = RoundedCornerShape(22.dp)
    val alpha = com.russoviller.cryogenic.setting.AllSettings.cryoPortraitWallpaperOpacity.state / 100f
    Box(
        modifier = modifier
            .cryoGlass(shape = shape, cornerRadius = 22.dp)
            .clip(shape)
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            if (PortraitWallpaperStore.isValid) {
                val generation = PortraitWallpaperStore.generation
                if (PortraitWallpaperStore.isVideo) {
                    com.russoviller.cryogenic.ui.components.VideoPlayer(
                        videoUri = android.net.Uri.fromFile(PortraitWallpaperStore.file),
                        modifier = Modifier.fillMaxSize(),
                        refreshTrigger = generation,
                        muted = true,
                        volume = 0f
                    )
                } else {
                    com.russoviller.cryogenic.ui.screens.content.elements.BackgroundImage(
                        modifier = Modifier.fillMaxSize(),
                        imageFile = PortraitWallpaperStore.file,
                        refreshTrigger = generation
                    )
                }
            } else {
                androidx.compose.foundation.Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = com.russoviller.cryogenic.ui.components.cryoArtPainter(R.drawable.img_cryo_portrait),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, CryoGlassColors.Deep.copy(alpha = 0.75f))))
        )
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            text = "9:16",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            RoundIconButton(R.drawable.ic_image_filled, onClick = { showDialog = true }, size = 36.dp)
        }
    }
}

@Composable
private fun CardHeader(
    iconRes: Int,
    title: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1
        )
        action?.let {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Ice
                )
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = null,
                    tint = CryoGlassColors.Ice
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .cryoGlass()
            .padding(padding),
        content = content
    )
}

@Composable
private fun GlowButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .height(height)
            .cryoGlow(height / 2, selected = true)
            .clip(shape)
            .cryoBackground(cryoGradient())
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Deep
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Deep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RoundIconButton(iconRes: Int, onClick: () -> Unit, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.5.dp, CryoGlassColors.Ice.copy(alpha = 0.7f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun HeroCard(
    account: Account?,
    version: Version?,
    versions: List<Version>,
    isRefreshing: Boolean,
    actions: CryoHomeActions,
    modifier: Modifier = Modifier,
    /** Modo Descanso: posição do botão Recarregar na tela (é o único toque que passa) */
    onReloadBounds: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val hasVersion = version != null && !isRefreshing
    var showWallpaper by remember { mutableStateOf(false) }
    if (showWallpaper) {
        HeroWallpaperDialog(onDismiss = { showWallpaper = false })
    }
    Box(
        modifier = modifier.cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
    ) {
        HeroBackdrop(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_home_welcome) + ",",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Minecraft Java",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice, CryoGlassColors.IceSoft)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )
            )
            Text(
                text = stringResource(R.string.cryo_home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            if (version != null) {
                val info = remember(version) { version.getVersionInfo() }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Badge(version.getVersionName())
                    info?.primaryLoader?.let { Badge(it.loader.displayName) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                //Recarrega os 3 wallpapers (fundo, cartão principal e 9:16) quando algum trava
                val reload = rememberWallpaperReload()
                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        onReloadBounds?.invoke(coords.boundsInRoot())
                    }
                ) {
                GlowButton(
                    text = stringResource(R.string.cryo_home_reload),
                    iconRes = R.drawable.ic_refresh,
                    onClick = reload
                )
                }
                RoundIconButton(R.drawable.ic_image_filled, onClick = { showWallpaper = true })
            }
        }
    }
}

/** Recarrega os wallpapers (fundo, cartão principal e 9:16) quando algum trava. */
@Composable
private fun rememberWallpaperReload(): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val background = com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel.current
    val doneText = stringResource(R.string.cryo_home_wallpapers_reloaded)
    val reloading = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    return remember(background, doneText) {
        {
            if (reloading.compareAndSet(false, true)) {
                scope.launch {
                    runCatching { com.russoviller.cryogenic.ui.screens.content.elements.WallpaperImageLoader.clearMemory() }
                    runCatching { HeroWallpaperStore.refresh() }
                    runCatching { com.russoviller.cryogenic.filemanager.ui.components.PortraitWallpaperStore.refresh() }
                    runCatching { background?.reload() }
                    reloading.set(false)
                    android.widget.Toast.makeText(context, doneText, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun MinecraftVersionCard(
    version: Version?,
    latestRelease: String?,
    actions: CryoHomeActions,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        CardHeader(iconRes = R.drawable.ic_videogame_asset_outlined, title = stringResource(R.string.cryo_home_mc_version))
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                if (version != null) {
                    VersionIconImage(modifier = Modifier.size(42.dp), version = version)
                } else {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(R.drawable.ic_download_2_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Ice
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = version?.getVersionName() ?: (latestRelease ?: "—"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (version == null && latestRelease != null) {
                        Badge(stringResource(R.string.cryo_home_latest_version))
                    }
                }
                val info = remember(version) { version?.getVersionInfo() }
                Text(
                    text = if (version != null) {
                        buildString {
                            append(info?.minecraftVersion ?: "")
                            info?.loaderInfos?.forEach { append(" • ").append(it.loader.displayName) }
                        }
                    } else stringResource(R.string.cryo_home_get_minecraft_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlowButton(
                modifier = Modifier.weight(1f),
                text = if (version != null) stringResource(R.string.cryo_home_play) else stringResource(R.string.cryo_install),
                iconRes = if (version != null) R.drawable.ic_play_arrow_filled else R.drawable.ic_download_2_filled,
                onClick = { if (version != null) actions.launch(version) else actions.downloadGame() },
                height = 40.dp
            )
            RoundIconButton(
                iconRes = R.drawable.ic_settings_filled,
                onClick = { version?.let(actions.openVersionSettings) ?: actions.openVersions() },
                size = 40.dp
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, accent = CryoGlassColors.Aurora)
}

@Composable
private fun VersionsListCard(
    versions: List<Version>,
    current: Version?,
    actions: CryoHomeActions,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        CardHeader(
            iconRes = R.drawable.ic_package_2_outlined,
            title = stringResource(R.string.cryo_home_versions),
            action = stringResource(R.string.cryo_home_see_all),
            onAction = actions.openVersions
        )
        Spacer(Modifier.height(8.dp))
        if (versions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.cryo_home_empty_versions),
                    style = MaterialTheme.typography.bodySmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(ContainScrollConnection),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(versions.size, key = { versions[it].getVersionName() }) { index ->
                    val item = versions[index]
                    val selected = item.getVersionName() == current?.getVersionName()
                    val info = remember(item) { item.getVersionInfo() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (selected) 0.10f else 0.0f))
                            .clickable { VersionsManager.saveVersion(item) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VersionIconImage(modifier = Modifier.size(28.dp), version = item)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.getVersionName(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(info?.minecraftVersion ?: "")
                                    info?.primaryLoader?.let { append(" • ").append(it.loader.displayName) }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                        RoundIconButton(R.drawable.ic_play_arrow_filled, onClick = { actions.launch(item) }, size = 32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: Account?,
    onManage: () -> Unit,
    onSkins: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CardHeader(iconRes = R.drawable.ic_person_outlined, title = stringResource(R.string.cryo_home_account))
            }
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .cryoBackground(cryoGradient())
                    .clickable(onClick = onSkins)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_checkroom),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
                Text(
                    text = stringResource(R.string.cryo_home_skins),
                    style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(safeRadialGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.5f), CryoGlassColors.Deep)))
                    .border(2.dp, CryoGlassColors.Ice, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                if (account != null) {
                    PlayerFace(account = account, avatarSize = 38.dp)
                } else {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.username ?: stringResource(R.string.account_add_new_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (account != null) Color(0xFF4ADE80) else Color(0xFF94A3B8))
                    )
                    Text(
                        text = account?.let { getAccountTypeName(it) } ?: stringResource(R.string.cryo_home_no_account),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (account != null) Color(0xFF4ADE80) else CryoGlassColors.Frost.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlineButton(
            modifier = Modifier.fillMaxWidth(),
            iconRes = R.drawable.ic_settings_filled,
            text = stringResource(R.string.cryo_home_manage_account),
            onClick = onManage
        )
    }
}

@Composable
private fun OutlineButton(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Funções Extras: funções simples que não ficam nas configurações padrão do Launcher.
 * Primeira função: Predefinições (salvar e trocar tema + 3 wallpapers de uma vez).
 */
@Composable
private fun ExtrasCard(modifier: Modifier = Modifier) {
    var showPresets by remember { mutableStateOf(false) }
    var showConfigs by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        CryoPresets.load()
        com.russoviller.cryogenic.extras.CryoConfigs.load()
    }
    val count = CryoPresets.presets.size
    val configCount = com.russoviller.cryogenic.extras.CryoConfigs.configs.size

    GlassCard(modifier = modifier) {
        CardHeader(
            iconRes = R.drawable.ic_star_filled,
            title = stringResource(R.string.cryo_extras_title)
        )
        Spacer(Modifier.height(10.dp))
        ExtrasRow(
            iconRes = R.drawable.ic_style_outlined,
            title = stringResource(R.string.cryo_presets_title),
            subtitle = if (count == 0) stringResource(R.string.cryo_extras_presets_summary)
                else stringResource(R.string.cryo_presets_count, count),
            accent = CryoGlassColors.Ice,
            onClick = { showPresets = true }
        )
        Spacer(Modifier.height(8.dp))
        //Salvar Configurações: todas as configurações do Launcher (menos wallpapers), com exportar .cryoconf
        ExtrasRow(
            iconRes = R.drawable.ic_settings_filled,
            title = stringResource(R.string.cryo_configs_title),
            subtitle = if (configCount == 0) stringResource(R.string.cryo_configs_summary)
                else stringResource(R.string.cryo_configs_count, configCount),
            accent = CryoGlassColors.Aurora,
            onClick = { showConfigs = true }
        )
    }
    if (showPresets) {
        PresetsDialog(onDismiss = { showPresets = false })
    }
    if (showConfigs) {
        com.russoviller.cryogenic.ui.screens.content.extras.ConfigsDialog(onDismiss = { showConfigs = false })
    }
}

@Composable
private fun ExtrasRow(iconRes: Int, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = CryoGlassColors.Frost.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FeatureCard(
    iconRes: Int,
    title: String,
    text: String,
    action: String,
    art: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    GlassCard(modifier = modifier.clickable(onClick = onClick), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CardHeader(iconRes = iconRes, title = title)
            }
            onEdit?.let { EditPencilButton(onClick = it) }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val artAlpha = cryoFillFactor().coerceIn(0.15f, 1f)
            art(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = artAlpha }
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = CryoGlassColors.Frost.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Aurora)))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = action,
                        style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                        fontWeight = FontWeight.Black,
                        color = CryoGlassColors.Deep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsCard(actions: CryoHomeActions, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    val selectedIds = rememberQuickShortcutIds()
    //atalhos que abrem janelas (não telas)
    var shortcutDialog by remember { mutableStateOf<String?>(null) }
    val catalog = quickShortcutCatalog(actions) { shortcutDialog = it }
    ShortcutDialogs(shortcutDialog, actions) { shortcutDialog = null }
    val shown = selectedIds.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
    if (editing) {
        QuickShortcutEditor(
            catalog = catalog,
            selected = selectedIds,
            onDismiss = { editing = false }
        )
    }
    GlassCard(modifier = modifier, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CardHeader(iconRes = R.drawable.ic_settings_filled, title = stringResource(R.string.cryo_home_quick_settings))
            }
            EditPencilButton(onClick = { editing = true })
        }
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { editing = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.cryo_home_shortcuts_empty),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                )
            }
        } else {
            val columns = if (shown.size > 4) 3 else 2
            val rows = shown.chunked(columns)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { row ->
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item -> QuickTile(item.iconRes, stringResource(item.labelRes), item.action) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPencilButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Ice.copy(alpha = 0.14f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(15.dp),
            painter = painterResource(R.drawable.ic_edit_filled),
            contentDescription = stringResource(R.string.cryo_home_edit_shortcuts),
            tint = CryoGlassColors.Ice
        )
    }
}

private class FeatureShortcut(
    val id: String,
    val iconRes: Int,
    val titleRes: Int,
    val textRes: Int,
    val actionRes: Int,
    val imageRes: Int,
    val action: () -> Unit
)

private val DEFAULT_FEATURE_CARDS = listOf("shaders", "mods")

private fun featureCardCatalog(actions: CryoHomeActions): List<FeatureShortcut> = listOf(
    FeatureShortcut("shaders", R.drawable.ic_lightbulb, R.string.download_category_shaders, R.string.cryo_home_shaders_text, R.string.cryo_home_explore, R.drawable.img_cryo_shaders, actions.downloadShaders),
    FeatureShortcut("mods", R.drawable.ic_extension_outlined, R.string.download_category_mod, R.string.cryo_home_mods_text, R.string.cryo_home_see_mods, R.drawable.img_cryo_mods, actions.downloadMods),
    FeatureShortcut("modpacks", R.drawable.ic_package_2_outlined, R.string.cryo_home_modpacks_title, R.string.cryo_home_modpacks_text, R.string.cryo_home_see_modpacks, R.drawable.img_cryo_modpacks, actions.downloadModpacks),
    FeatureShortcut("resourcepacks", R.drawable.ic_format_paint_outlined, R.string.download_category_resource_pack, R.string.cryo_home_textures_text, R.string.cryo_home_see_textures, R.drawable.img_cryo_textures, actions.downloadResourcePacks),
    FeatureShortcut("saves", R.drawable.ic_public, R.string.download_category_saves, R.string.cryo_home_worlds_text, R.string.cryo_home_see_worlds, R.drawable.img_cryo_worlds, actions.downloadSaves)
)

@Composable
private fun rememberFeatureCardIds(catalog: List<FeatureShortcut>): List<String> {
    val raw = AllSettings.cryoHomeFeatureCards.state
    return remember(raw) {
        val valid = catalog.map { it.id }
        val ids = raw.split(',').map { it.trim() }.filter { it in valid }.distinct().take(2).toMutableList()
        (DEFAULT_FEATURE_CARDS + valid).forEach { if (ids.size < 2 && it !in ids) ids.add(it) }
        ids
    }
}

@Composable
private fun FeatureCardPicker(
    catalog: List<FeatureShortcut>,
    current: String,
    other: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.6f)
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_home_card_pick_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                ),
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.cryo_home_card_pick_summary),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                catalog.forEach { item ->
                    val selected = item.id == current
                    val blocked = item.id == other
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .graphicsLayer { alpha = if (blocked) 0.4f else 1f }
                            .clip(shape)
                            .background(
                                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.28f), CryoGlassColors.Navy.copy(alpha = 0.5f)))
                                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent))
                            )
                            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.85f else 0.2f), shape)
                            .clickable(enabled = !blocked) {
                                if (selected) onDismiss() else onPick(item.id)
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardImage(
                            item.imageRes,
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        )
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            tint = CryoGlassColors.Ice
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else CryoGlassColors.Frost,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        when {
                            selected -> Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = CryoGlassColors.Ice
                            )
                            blocked -> Text(
                                text = stringResource(R.string.cryo_home_card_in_use),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost
                            )
                        }
                    }
                }
            }
            com.russoviller.cryogenic.ui.components.CryoTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.generic_close), maxLines = 1)
            }
        }
    }
}

private class QuickShortcut(val id: String, val iconRes: Int, val labelRes: Int, val action: () -> Unit)

private const val MAX_QUICK_SHORTCUTS = 6

/** Janelas abertas pelos atalhos rápidos. */
@Composable
private fun ShortcutDialogs(id: String?, actions: CryoHomeActions, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (id) {
        "wallpapers" -> com.russoviller.cryogenic.ui.screens.content.settings.AppearanceSettingsDialog(
            onDismiss = onDismiss,
            submitError = { msg ->
                android.widget.Toast.makeText(context, msg.message.toAndroidString(context), android.widget.Toast.LENGTH_LONG).show()
            }
        )
        "theme" -> com.russoviller.cryogenic.ui.screens.content.settings.ThemeStudioDialog(
            onDismiss = onDismiss,
            onEditCustomColor = {
                onDismiss()
                actions.openLauncherSettings()
            }
        )
        "idle" -> com.russoviller.cryogenic.ui.screens.content.settings.IdleModeDialog(onDismiss = onDismiss)
        "discord" -> com.russoviller.cryogenic.ui.screens.content.settings.DiscordSettingsDialog(onDismiss = onDismiss)
        "presets" -> PresetsDialog(onDismiss = onDismiss)
        "configs" -> com.russoviller.cryogenic.ui.screens.content.extras.ConfigsDialog(onDismiss = onDismiss)
        "launcher_mods" -> com.russoviller.cryogenic.cmod.LauncherModsDialog(
            onDismiss = onDismiss,
            onOpenCryoMods = {
                onDismiss()
                actions.openCryoMods()
            }
        )
        else -> Unit
    }
}

private fun quickShortcutCatalog(actions: CryoHomeActions, openDialog: (String) -> Unit): List<QuickShortcut> = listOf(
    QuickShortcut("renderer", R.drawable.ic_video_settings, R.string.settings_tab_renderer, actions.openRendererSettings),
    QuickShortcut("game", R.drawable.ic_rocket_launch_filled, R.string.settings_tab_game, actions.openGameSettings),
    QuickShortcut("control", R.drawable.ic_touch_app_filled, R.string.settings_tab_control, actions.openControlSettings),
    QuickShortcut("launcher", R.drawable.ic_setting_launcher, R.string.cryo_home_appearance, actions.openLauncherSettings),
    QuickShortcut("java", R.drawable.ic_java, R.string.cryo_shortcut_java, actions.openJavaSettings),
    QuickShortcut("hud", R.drawable.ic_dashboard_filled, R.string.settings_tab_control_manage, actions.openHudManager),
    QuickShortcut("about", R.drawable.ic_info_filled, R.string.cryo_shortcut_about, actions.openAbout),
    QuickShortcut("versions", R.drawable.ic_videogame_asset_outlined, R.string.cryo_nav_versions, actions.openVersions),
    QuickShortcut("accounts", R.drawable.ic_person_outlined, R.string.cryo_nav_accounts, actions.openAccounts),
    QuickShortcut("files", R.drawable.ic_folder_filled, R.string.cryo_nav_files, actions.openFiles),
    QuickShortcut("webpage", R.drawable.ic_public, R.string.cryo_nav_webpage, actions.openWebPage),
    QuickShortcut("dl_game", R.drawable.ic_download_2_filled, R.string.cryo_shortcut_dl_game, actions.downloadGame),
    QuickShortcut("dl_mods", R.drawable.ic_extension_outlined, R.string.cryo_shortcut_dl_mods, actions.downloadMods),
    QuickShortcut("dl_modpacks", R.drawable.ic_package_2_outlined, R.string.cryo_shortcut_dl_modpacks, actions.downloadModpacks),
    QuickShortcut("dl_resourcepacks", R.drawable.ic_format_paint_outlined, R.string.cryo_shortcut_dl_resourcepacks, actions.downloadResourcePacks),
    QuickShortcut("dl_shaders", R.drawable.ic_lightbulb, R.string.cryo_shortcut_dl_shaders, actions.downloadShaders),
    QuickShortcut("dl_saves", R.drawable.ic_public, R.string.cryo_shortcut_dl_saves, actions.downloadSaves),
    QuickShortcut("favorites", R.drawable.ic_favorite_filled, R.string.cryo_shortcut_favorites, actions.openFavorites),
    QuickShortcut("launcher_mods", R.drawable.ic_extension_outlined, R.string.cryo_cmods_launcher_mods) { openDialog("launcher_mods") },
    QuickShortcut("cryomods", R.drawable.ic_extension_outlined, R.string.cryo_cmods_title, actions.openCryoMods),
    QuickShortcut("live", R.drawable.ic_cryo_stream, R.string.cryo_nav_live, actions.openTikTokLive),
    QuickShortcut("delchunk", R.drawable.ic_cryo_chunks, R.string.cryo_nav_chunks, actions.openChunkEraser),
    QuickShortcut("chunkbase", R.drawable.ic_public, R.string.cryo_nav_chunkbase, actions.openChunkBase),
    QuickShortcut("wallpapers", R.drawable.ic_image_outlined, R.string.cryo_shortcut_wallpapers) { openDialog("wallpapers") },
    QuickShortcut("theme", R.drawable.ic_format_paint_outlined, R.string.cryo_shortcut_theme) { openDialog("theme") },
    QuickShortcut("idle", R.drawable.ic_visibility_outlined, R.string.cryo_idle_title) { openDialog("idle") },
    QuickShortcut("discord", R.drawable.ic_cryo_discord, R.string.cryo_tab_discord) { openDialog("discord") },
    QuickShortcut("presets", R.drawable.ic_style_outlined, R.string.cryo_presets_title) { openDialog("presets") },
    QuickShortcut("configs", R.drawable.ic_settings_filled, R.string.cryo_configs_title) { openDialog("configs") },
    QuickShortcut("gamepad", R.drawable.ic_cryo_gamepad, R.string.settings_tab_gamepad, actions.openGamepadSettings),
    QuickShortcut("launcher_tab", R.drawable.ic_extension_outlined, R.string.settings_tab_launcher_extras, actions.openLauncherExtras)
)

@Composable
private fun rememberQuickShortcutIds(): List<String> {
    val raw = com.russoviller.cryogenic.setting.AllSettings.cryoQuickShortcuts.state
    return remember(raw) { raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_QUICK_SHORTCUTS) }
}

@Composable
private fun QuickShortcutEditor(
    catalog: List<QuickShortcut>,
    selected: List<String>,
    onDismiss: () -> Unit
) {
    val current = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(selected) } }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.82f)
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_home_edit_shortcuts),
                        style = MaterialTheme.typography.titleMedium.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                        ),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.cryo_home_edit_shortcuts_summary, MAX_QUICK_SHORTCUTS),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = "${current.size}/$MAX_QUICK_SHORTCUTS",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Ice
                )
            }
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp),
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(catalog.size) { index ->
                    val item = catalog[index]
                    val position = current.indexOf(item.id)
                    val active = position >= 0
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(shape)
                            .background(
                                if (active) Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.28f), CryoGlassColors.Navy.copy(alpha = 0.5f)))
                                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent))
                            )
                            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (active) 0.85f else 0.2f), shape)
                            .clickable {
                                if (active) current.remove(item.id)
                                else if (current.size < MAX_QUICK_SHORTCUTS) current.add(item.id)
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .cryoBackground(
                                    if (active) com.russoviller.cryogenic.ui.components.cryoGradient()
                                    else Brush.linearGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Navy))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                tint = if (active) CryoGlassColors.Deep else CryoGlassColors.Ice
                            )
                        }
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else CryoGlassColors.Frost,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (active) {
                            Text(
                                text = "${position + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                fontWeight = FontWeight.Black,
                                color = CryoGlassColors.Ice
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.russoviller.cryogenic.ui.components.CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        current.clear()
                        current.addAll(listOf("renderer", "game", "control", "launcher"))
                    }
                ) {
                    Text(text = stringResource(R.string.cryo_home_shortcuts_reset), maxLines = 1)
                }
                com.russoviller.cryogenic.ui.components.CryoButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        com.russoviller.cryogenic.setting.AllSettings.cryoQuickShortcuts.save(current.joinToString(","))
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.generic_save), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HeroWallpaperDialog(onDismiss: () -> Unit) {
    CryoWallpaperDialog(
        store = HeroWallpaperStore,
        opacity = com.russoviller.cryogenic.setting.AllSettings.cryoHeroWallpaperOpacity,
        titleRes = R.string.cryo_hero_wallpaper_title,
        summaryRes = R.string.cryo_hero_wallpaper_summary,
        opacityForDefault = false,
        onDismiss = onDismiss
    )
}

@Composable
private fun CryoWallpaperDialog(
    store: com.russoviller.cryogenic.filemanager.ui.components.CryoMediaWallpaper,
    opacity: com.russoviller.cryogenic.setting.unit.IntSettingUnit,
    titleRes: Int,
    summaryRes: Int,
    opacityForDefault: Boolean,
    onDismiss: () -> Unit
) {
    val HeroWallpaperStore = store
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val errorText = stringResource(R.string.error_import_image)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.russoviller.cryogenic.contract.MediaPickerContract(allowImages = true, allowVideos = true, allowMultiple = false)
    ) { result ->
        val uri = result?.firstOrNull() ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching { HeroWallpaperStore.import(context, uri) }
                .onFailure { android.widget.Toast.makeText(context, errorText, android.widget.Toast.LENGTH_LONG).show() }
            busy = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { HeroWallpaperStore.ensureLoaded() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = stringResource(summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            Text(
                text = when {
                    busy -> stringResource(R.string.settings_launcher_background_importing)
                    HeroWallpaperStore.isVideo -> stringResource(R.string.cryo_hero_wallpaper_state_video)
                    HeroWallpaperStore.isImage -> stringResource(R.string.cryo_fm_wallpaper_state_image)
                    else -> stringResource(R.string.cryo_hero_wallpaper_state_default)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.russoviller.cryogenic.ui.components.CryoButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = { picker.launch(Unit) }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_image_filled),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_fm_wallpaper_pick), maxLines = 1)
                }
                com.russoviller.cryogenic.ui.components.CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && HeroWallpaperStore.isValid,
                    onClick = { scope.launch { HeroWallpaperStore.delete() } }
                ) {
                    Text(text = stringResource(R.string.cryo_hero_wallpaper_restore), maxLines = 1)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.cryo_hero_wallpaper_opacity),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Frost
                )
                val unit = opacity
                com.russoviller.cryogenic.ui.components.SimpleTextSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = unit.state.toFloat(),
                    enabled = opacityForDefault || HeroWallpaperStore.isValid,
                    onValueChange = { unit.updateState(it.toInt()) },
                    onValueChangeFinished = { unit.save(unit.state) },
                    toInt = true,
                    suffix = "%",
                    valueRange = 0f..100f
                )
            }
            com.russoviller.cryogenic.ui.components.CryoTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.generic_close))
            }
        }
    }
}

@Composable
private fun RowScope.QuickTile(iconRes: Int, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberArtTime(duration: Int): androidx.compose.runtime.State<Float> =
    remember(duration) { androidx.compose.runtime.mutableFloatStateOf(0.18f) }

@Composable
private fun HeroBackdrop(modifier: Modifier = Modifier) {
    androidx.compose.runtime.LaunchedEffect(Unit) { HeroWallpaperStore.ensureLoaded() }
    val custom = HeroWallpaperStore.isValid
    val artAlpha = if (custom) {
        com.russoviller.cryogenic.setting.AllSettings.cryoHeroWallpaperOpacity.state / 100f
    } else {
        cryoFillFactor().coerceIn(0.2f, 1f)
    }
    val deep = CryoGlassColors.Deep
    Box(modifier = modifier.graphicsLayer { alpha = artAlpha }) {
        if (custom) {
            val generation = HeroWallpaperStore.generation
            if (HeroWallpaperStore.isVideo) {
                com.russoviller.cryogenic.ui.components.VideoPlayer(
                    videoUri = android.net.Uri.fromFile(HeroWallpaperStore.file),
                    modifier = Modifier.fillMaxSize(),
                    refreshTrigger = generation,
                    muted = true,
                    volume = 0f
                )
            } else {
                com.russoviller.cryogenic.ui.screens.content.elements.BackgroundImage(
                    modifier = Modifier.fillMaxSize(),
                    imageFile = HeroWallpaperStore.file,
                    refreshTrigger = generation
                )
            }
        } else {
            androidx.compose.foundation.Image(
                modifier = Modifier.fillMaxSize(),
                painter = com.russoviller.cryogenic.ui.components.cryoArtPainter(R.drawable.img_cryo_hero),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = Alignment.CenterEnd
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(deep.copy(alpha = 0.92f), deep.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun CastleArt(modifier: Modifier = Modifier) {
    val time = rememberArtTime(30_000)
    val stars = remember {
        val r = Random(99)
        List(40) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.6f, r.nextFloat()) }
    }
    val spec = CryoTheme.spec
    val artAlpha = cryoFillFactor().coerceIn(0.12f, 1f)
    Canvas(modifier = modifier.graphicsLayer { alpha = artAlpha }) {
        val t = time.value
        drawRect(
            Brush.verticalGradient(
                listOf(spec.deep.copy(alpha = 0.8f), spec.navy.copy(alpha = 0.6f), lerp(spec.navy, spec.iceSoft, 0.35f).copy(alpha = 0.67f))
            )
        )
        stars.forEach { s ->
            val a = 0.3f + 0.7f * (0.5f + 0.5f * sin((t * 20f + s[2] * 7f) * 2f * PI.toFloat()))
            drawCircle(Color.White.copy(alpha = a * 0.8f), 1.4f, Offset(s[0] * size.width, s[1] * size.height))
        }
        val moon = Offset(size.width * 0.72f, size.height * 0.30f)
        val r = size.height * 0.22f
        drawCircle(
            safeRadialGradient(listOf(spec.ice.copy(alpha = 0.4f), Color.Transparent), center = moon, radius = r * 2.6f),
            radius = r * 2.6f,
            center = moon
        )
        drawCircle(
            safeRadialGradient(listOf(Color.White, lerp(spec.frost, spec.ice, 0.5f)), center = moon, radius = r),
            radius = r,
            center = moon
        )
        drawMountains(size.height * 0.62f, lerp(spec.navy, spec.iceSoft, 0.3f), 0.8f)
        drawCastle(Offset(size.width * 0.72f, size.height * 0.80f), size.height * 0.62f)
        drawMountains(size.height * 0.78f, lerp(spec.deep, spec.navy, 0.7f), 1.3f)
        drawRect(
            brush = Brush.horizontalGradient(listOf(spec.deep.copy(alpha = 0.9f), spec.deep.copy(alpha = 0.6f), Color.Transparent)),
            size = Size(size.width * 0.62f, size.height)
        )
    }
}

private fun DrawScope.drawMountains(baseY: Float, color: Color, freq: Float) {
    val path = Path()
    path.moveTo(0f, size.height)
    var x = 0f
    val step = size.width / 14f
    var i = 0
    path.lineTo(0f, baseY)
    while (x <= size.width + step) {
        val peak = if (i % 2 == 0) baseY - size.height * 0.12f * (0.6f + 0.4f * sin(i * freq)) else baseY
        path.lineTo(x, peak)
        x += step
        i++
    }
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, Brush.verticalGradient(listOf(color, CryoGlassColors.Deep), startY = baseY - size.height * 0.15f, endY = size.height))
}

private fun DrawScope.drawCastle(base: Offset, height: Float) {
    val glow = CryoGlassColors.Ice
    val body = Brush.verticalGradient(listOf(CryoGlassColors.IceSoft, lerp(CryoGlassColors.Navy, CryoGlassColors.IceSoft, 0.4f)), startY = base.y - height, endY = base.y)
    val towers = listOf(
        floatArrayOf(-0.30f, 0.45f, 0.07f),
        floatArrayOf(-0.16f, 0.70f, 0.08f),
        floatArrayOf(0.00f, 1.00f, 0.10f),
        floatArrayOf(0.15f, 0.75f, 0.08f),
        floatArrayOf(0.29f, 0.50f, 0.07f)
    )
    towers.forEach { (dx, h, w) ->
        val cx = base.x + dx * height
        val top = base.y - h * height
        val half = w * height / 2f
        drawRect(body, topLeft = Offset(cx - half, top), size = Size(half * 2, base.y - top))
        val spire = Path().apply {
            moveTo(cx - half * 1.2f, top)
            lineTo(cx, top - h * height * 0.35f)
            lineTo(cx + half * 1.2f, top)
            close()
        }
        drawPath(spire, Brush.verticalGradient(listOf(CryoGlassColors.Frost, CryoGlassColors.IceSoft), startY = top - h * height * 0.35f, endY = top))
        drawRect(glow.copy(alpha = 0.9f), topLeft = Offset(cx - half * 0.25f, top + h * height * 0.25f), size = Size(half * 0.5f, half * 0.9f))
    }
    drawRect(body, topLeft = Offset(base.x - 0.32f * height, base.y - 0.28f * height), size = Size(0.64f * height, 0.28f * height))
}

@Composable
private fun CardImage(res: Int, modifier: Modifier) {
    androidx.compose.foundation.Image(
        modifier = modifier,
        painter = com.russoviller.cryogenic.ui.components.cryoArtPainter(res),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

@Composable
private fun SunsetArt(modifier: Modifier) {
    val time = rememberArtTime(12_000)
    Canvas(modifier = modifier) {
        val t = time.value
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2B2F6B), Color(0xFFE07A5F), Color(0xFFF2CC8F)), endY = size.height * 0.6f))
        val sun = Offset(size.width * 0.55f, size.height * 0.56f)
        drawCircle(Color(0xFFFFE1A8), radius = size.height * 0.14f, center = sun)
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF6C8EBF), Color(0xFF1B2F55)), startY = size.height * 0.6f),
            topLeft = Offset(0f, size.height * 0.6f),
            size = Size(size.width, size.height * 0.4f)
        )
        for (i in 0 until 5) {
            val y = size.height * (0.64f + i * 0.07f)
            val w = size.width * (0.18f - i * 0.02f) * (0.8f + 0.2f * sin((t + i * 0.2f) * 2f * PI.toFloat()))
            drawRect(Color(0x99FFE1A8), topLeft = Offset(sun.x - w / 2, y), size = Size(w, 2f))
        }
        for (i in 0 until 6) {
            val x = size.width * (0.05f + i * 0.17f)
            val h = size.height * (0.22f + (i % 3) * 0.05f)
            val tree = Path().apply {
                moveTo(x, size.height * 0.62f - h)
                lineTo(x - h * 0.22f, size.height * 0.62f)
                lineTo(x + h * 0.22f, size.height * 0.62f)
                close()
            }
            if (i != 3) drawPath(tree, Color(0xFF14213D))
        }
    }
}

@Composable
private fun IslandArt(modifier: Modifier) {
    val time = rememberArtTime(6_000)
    Canvas(modifier = modifier) {
        val t = time.value
        drawRect(Brush.verticalGradient(listOf(Color(0xFF8EC5FF), Color(0xFFD7ECFF))))
        val bob = sin(t * 2f * PI.toFloat()) * size.height * 0.03f
        val cx = size.width / 2f
        val top = size.height * 0.52f + bob
        val island = Path().apply {
            moveTo(cx - size.width * 0.38f, top)
            lineTo(cx + size.width * 0.38f, top)
            lineTo(cx + size.width * 0.12f, top + size.height * 0.30f)
            lineTo(cx, top + size.height * 0.40f)
            lineTo(cx - size.width * 0.14f, top + size.height * 0.28f)
            close()
        }
        drawPath(island, Brush.verticalGradient(listOf(Color(0xFF7A5A3A), Color(0xFF3E2C1C)), startY = top, endY = top + size.height * 0.4f))
        drawRect(Color(0xFF5BBF5B), topLeft = Offset(cx - size.width * 0.38f, top - size.height * 0.04f), size = Size(size.width * 0.76f, size.height * 0.05f))
        drawRect(Color(0xFFEAF6FF), topLeft = Offset(cx - size.width * 0.38f, top - size.height * 0.06f), size = Size(size.width * 0.76f, size.height * 0.02f))
        val castleBase = top - size.height * 0.04f
        drawRect(Color(0xFF8AA3BF), topLeft = Offset(cx - size.width * 0.12f, castleBase - size.height * 0.20f), size = Size(size.width * 0.24f, size.height * 0.20f))
        for (dx in listOf(-0.12f, 0.0f, 0.12f)) {
            val x = cx + size.width * dx
            val spire = Path().apply {
                moveTo(x - size.width * 0.04f, castleBase - size.height * 0.20f)
                lineTo(x, castleBase - size.height * 0.34f)
                lineTo(x + size.width * 0.04f, castleBase - size.height * 0.20f)
                close()
            }
            drawPath(spire, Color(0xFF3D6FA8))
        }
        drawCircle(Color.White.copy(alpha = 0.8f), radius = size.height * 0.05f, center = Offset(size.width * 0.2f, size.height * 0.2f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = size.height * 0.07f, center = Offset(size.width * 0.27f, size.height * 0.18f))
    }
}


private object ContainScrollConnection : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    override fun onPostScroll(
        consumed: androidx.compose.ui.geometry.Offset,
        available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
    ): androidx.compose.ui.geometry.Offset = available.copy(x = 0f)

    override suspend fun onPostFling(
        consumed: androidx.compose.ui.unit.Velocity,
        available: androidx.compose.ui.unit.Velocity
    ): androidx.compose.ui.unit.Velocity = available.copy(x = 0f)
}
