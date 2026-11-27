package com.russoviller.cryogenic.ui.screens.game.elements

import android.os.SystemClock
import androidx.compose.foundation.background
import com.russoviller.cryogenic.game.download.assets.platform.modrinth.models.ModrinthFeatures
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.mapExceptionToMessage
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.game.download.assets.platform.PlatformFilterCode
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSearchData
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSearchFilter
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSortField
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.download.assets.platform.curseforge.models.CurseForgeResourcePackCategory
import com.russoviller.cryogenic.game.download.assets.platform.curseforge.models.CurseForgeShadersCategory
import com.russoviller.cryogenic.game.download.assets.platform.mcim.mapMCIMMirrorUrls
import com.russoviller.cryogenic.game.download.assets.platform.modrinth.models.ModrinthResourcePackCategory
import com.russoviller.cryogenic.game.download.assets.platform.modrinth.models.ModrinthShadersCategory
import com.russoviller.cryogenic.game.download.assets.platform.searchAssets
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSegmentedTabs
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.resolveAndroidString
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.initAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PAGE_SIZE = 20
private const val MAX_VERSIONS_SHOWN = 40

/**
 * Instala texturas/shaders direto na pasta da versão em jogo.
 * Fica fora da tela para o download continuar mesmo se o painel for fechado.
 */
object CryoAssetsInstaller {
    sealed interface State {
        data class Downloading(val progress: Float) : State
        data object Done : State
        data class Failed(val message: AndroidStringText) : State
    }

    val states = mutableStateMapOf<String, State>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun key(version: PlatformVersion): String = "${version.platform().name}/${version.platformId()}"

    fun install(version: PlatformVersion, targetDir: File) {
        val key = key(version)
        if (states[key] is State.Downloading) return
        states[key] = State.Downloading(0f)
        scope.launch {
            val name = version.platformFileName()
            val target = File(targetDir, name)
            //baixa num arquivo temporário: um download pela metade nunca aparece para o jogo
            val temp = File(targetDir, ".$name.part")
            runCatching {
                if (!targetDir.isDirectory && !targetDir.mkdirs()) throw java.io.IOException("Cannot create $targetDir")
                val total = version.platformFileSize().coerceAtLeast(1L)
                var downloaded = 0L
                var lastPost = 0L
                downloadSafely(
                    urls = version.platformDownloadUrl().mapMCIMMirrorUrls(),
                    output = temp,
                    sha1 = version.platformSha1(),
                    onBytes = { size ->
                        downloaded += size
                        val now = SystemClock.uptimeMillis()
                        if (now - lastPost >= 150L) {
                            lastPost = now
                            states[key] = State.Downloading((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                )
                if (target.exists() && !target.delete()) throw java.io.IOException("Cannot replace $target")
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
            }.onSuccess {
                states[key] = State.Done
            }.onFailure { e ->
                temp.delete()
                states[key] = State.Failed(mapExceptionToMessage(e))
            }
        }
    }
}

/** Grupo de categorias mostrado junto (Estilo, Efeitos, Resolução...). */
private class CategoryGroup(val titleRes: Int, val codes: List<PlatformFilterCode>)

private val ShaderEffects = setOf("ATMOSPHERE", "BLOOM", "COLORED_LIGHTING", "FOLIAGE", "PATH_TRACING", "PBR", "REFLECTIONS", "SHADOWS")
private val ShaderPerformance = setOf("POTATO", "LOW", "MEDIUM", "HIGH", "SCREENSHOT")

private fun categoryGroups(classes: PlatformClasses, platform: Platform): List<CategoryGroup> {
    fun name(code: PlatformFilterCode) = (code as? Enum<*>)?.name.orEmpty()
    return when (classes) {
        PlatformClasses.SHADERS -> when (platform) {
            Platform.CURSEFORGE -> listOf(CategoryGroup(R.string.cryo_assets_group_style, CurseForgeShadersCategory.entries))
            Platform.MODRINTH -> {
                val all = ModrinthShadersCategory.entries
                listOf(
                    CategoryGroup(R.string.cryo_assets_group_style, all.filter { name(it) !in ShaderEffects && name(it) !in ShaderPerformance }),
                    CategoryGroup(R.string.cryo_assets_group_effects, all.filter { name(it) in ShaderEffects }),
                    CategoryGroup(R.string.cryo_assets_group_performance, all.filter { name(it) in ShaderPerformance })
                )
            }
        }
        else -> when (platform) {
            Platform.CURSEFORGE -> {
                val all = CurseForgeResourcePackCategory.entries
                listOf(
                    CategoryGroup(R.string.cryo_assets_group_style, all.filter { !name(it).startsWith("X_") }),
                    CategoryGroup(R.string.cryo_assets_group_resolution, all.filter { name(it).startsWith("X_") })
                )
            }
            Platform.MODRINTH -> listOf(
                CategoryGroup(R.string.cryo_assets_group_style, ModrinthResourcePackCategory.entries),
                CategoryGroup(R.string.cryo_assets_group_content, ModrinthFeatures.entries)
            )
        }
    }.filter { it.codes.isNotEmpty() }
}

private val SortOptions = listOf(
    PlatformSortField.DOWNLOADS,
    PlatformSortField.RELEVANCE,
    PlatformSortField.UPDATED,
    PlatformSortField.NEWEST
)

/* Cores fixas do painel (legíveis por cima de qualquer cena do jogo). */
private val PanelBg = Color(0xFF0B1626)
private val CardBg = Color(0xFF122238)
private val CardShape = RoundedCornerShape(16.dp)

private fun Modifier.assetsPanel(): Modifier = this
    .clip(CardShape)
    .background(PanelBg.copy(alpha = 0.94f))
    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.22f), CardShape)

/**
 * Painel do menu do jogo para pesquisar e instalar texturas (resource packs) ou shaders
 * no CurseForge/Modrinth, direto na pasta da versão que está aberta.
 */
@Composable
fun CryoAssetsPanel(classes: PlatformClasses, version: Version?) {
    val platformSetting = if (classes == PlatformClasses.SHADERS) AllSettings.searchShadersPlatform else AllSettings.searchResourcePackPlatform
    val platforms = remember { Platform.available }
    var platform by remember { mutableStateOf(platformSetting.getValue().takeIf { it in platforms } ?: platforms.first()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(PlatformSortField.DOWNLOADS) }
    var sortTouched by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<Set<PlatformFilterCode>>(emptySet()) }
    val mcVersion = remember(version) { runCatching { version?.getVersionInfo()?.minecraftVersion }.getOrNull()?.takeIf { it.isNotBlank() } }
    var onlyMine by remember { mutableStateOf(mcVersion != null) }
    val targetDir = remember(version, classes) {
        runCatching { version?.let { File(it.getGameDir(), classes.versionFolder.folderName) } }.getOrNull()
    }

    var selected by remember { mutableStateOf<PlatformSearchData?>(null) }

    val items = remember { mutableStateListOf<PlatformSearchData>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<AndroidStringText?>(null) }
    var isLast by remember { mutableStateOf(true) }
    var nextIndex by remember { mutableStateOf(0) }
    var retryTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var moreJob by remember { mutableStateOf<Job?>(null) }
    val gridState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current

    val effectiveSort = if (!sortTouched && query.isNotBlank()) PlatformSortField.RELEVANCE else sort

    //cada busca nova ganha um número; resultado de busca antiga (cancelada) nunca é aplicado
    val generation = remember { java.util.concurrent.atomic.AtomicInteger(0) }

    suspend fun load(index: Int, gen: Int) {
        if (gen != generation.get()) return
        loading = true
        error = null
        val filter = PlatformSearchFilter(
            searchName = query.trim(),
            gameVersion = if (onlyMine && mcVersion != null) mcVersion else "",
            sortField = effectiveSort,
            categories = categories.toList(),
            index = index,
            limit = PAGE_SIZE
        )
        val searchPlatform = platform
        var page: List<PlatformSearchData>? = null
        var last = true
        var failure: AndroidStringText? = null
        try {
            //a requisição em andamento nunca é interrompida no meio: se o usuário trocar a busca,
            //ela termina sozinha e o resultado é descartado (ver generation)
            withContext(NonCancellable + Dispatchers.IO) {
                searchAssets(
                    searchPlatform = searchPlatform,
                    searchFilter = filter,
                    platformClasses = classes,
                    onSuccess = { result ->
                        val assets = result.getAssetsPage(classes)
                        page = assets.data.map { it.first }
                        last = assets.isLastPage || assets.data.isEmpty()
                    },
                    onError = { state -> failure = state.message }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure = mapExceptionToMessage(e)
        }
        //daqui em diante só na thread principal e só se esta ainda for a busca atual
        if (gen != generation.get()) return
        val data = page
        if (data != null) {
            val known = items.mapTo(HashSet()) { it.platformId() }
            items.addAll(data.filter { known.add(it.platformId()) })
            isLast = last
            nextIndex = index + PAGE_SIZE
        } else {
            error = failure
        }
        loading = false
    }

    fun loadMore() {
        if (moreJob?.isActive == true || loading) return
        val gen = generation.get()
        moreJob = scope.launch { load(nextIndex, gen) }
    }

    LaunchedEffect(platform, query, effectiveSort, categories, onlyMine, retryTick) {
        val gen = generation.incrementAndGet()
        moreJob?.cancel()
        moreJob = null
        //toques rápidos em categoria/ordenação só disparam a última busca
        delay(if (query.isNotBlank()) 450L else 300L)
        items.clear()
        error = null
        isLast = true
        nextIndex = 0
        load(0, gen)
        if (gen == generation.get()) gridState.requestScrollToItem(0)
    }

    val isShaders = classes == PlatformClasses.SHADERS
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //cabeçalho: título, busca e plataforma
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(if (isShaders) R.drawable.ic_cryo_shader else R.drawable.ic_cryo_texture),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column {
                Text(
                    text = stringResource(if (isShaders) R.string.cryo_assets_shaders else R.string.cryo_assets_textures),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (mcVersion != null && onlyMine) stringResource(R.string.cryo_assets_for_version, mcVersion) else stringResource(R.string.cryo_assets_any_version),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Ice,
                    maxLines = 1
                )
            }
            AssetsSearchField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = { query = it },
                onDone = { focusManager.clearFocus() }
            )
            if (platforms.size > 1) {
                PlatformSwitch(platforms = platforms, selected = platform) { p ->
                    if (p != platform) {
                        platform = p
                        categories = emptySet()
                        selected = null
                        platformSetting.save(p)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            //filtros
            FiltersSidebar(
                modifier = Modifier
                    .weight(0.33f)
                    .fillMaxHeight(),
                classes = classes,
                platform = platform,
                mcVersion = mcVersion,
                onlyMine = onlyMine,
                onOnlyMine = { onlyMine = it },
                sort = effectiveSort,
                onSort = { sort = it; sortTouched = true },
                categories = categories,
                onToggleCategory = { code -> categories = if (code in categories) categories - code else categories + code },
                onClearCategories = { categories = emptySet() }
            )

            //resultados / versões
            Box(
                modifier = Modifier
                    .weight(0.67f)
                    .fillMaxHeight()
            ) {
                val current = selected
                if (current != null) {
                    AssetDetail(
                        data = current,
                        mcVersion = mcVersion,
                        onlyMine = onlyMine,
                        targetDir = targetDir,
                        onBack = { selected = null }
                    )
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 190.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(items, key = { it.platform().name + it.platformId() }) { data ->
                            AssetCard(data = data, onClick = { selected = data })
                        }
                        item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = if (items.isEmpty()) 160.dp else 56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val err = error
                                when {
                                    loading -> CircularProgressIndicator(modifier = Modifier.size(26.dp), color = CryoGlassColors.Ice, strokeWidth = 2.5.dp)
                                    err != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = resolveAndroidString(err),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CryoDanger,
                                            textAlign = TextAlign.Center
                                        )
                                        AssetChip(stringResource(R.string.cryo_assets_retry), true) {
                                            if (items.isEmpty()) retryTick++
                                            else loadMore()
                                        }
                                    }
                                    items.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(modifier = Modifier.size(30.dp), painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = CryoGlassColors.Ice.copy(alpha = 0.5f))
                                        Text(
                                            text = stringResource(R.string.generic_no_matching_items),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                                        )
                                        if (categories.isNotEmpty()) {
                                            AssetChip(stringResource(R.string.cryo_assets_clear_filters), false) { categories = emptySet() }
                                        }
                                    }
                                    !isLast -> AssetChip(stringResource(R.string.cryo_assets_load_more), false) {
                                        loadMore()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Troca CurseForge / Modrinth com o logo de cada um. */
@Composable
private fun PlatformSwitch(platforms: List<Platform>, selected: Platform, onSelect: (Platform) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(PanelBg.copy(alpha = 0.94f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        platforms.forEach { p ->
            val active = p == selected
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (active) Modifier.cryoBackground(cryoGradient()) else Modifier)
                    .clickable { onSelect(p) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(if (p == Platform.CURSEFORGE) R.drawable.img_platform_curseforge else R.drawable.img_platform_modrinth),
                    contentDescription = null
                )
                Text(
                    text = p.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (active) CryoGlassColors.Deep else Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FiltersSidebar(
    modifier: Modifier,
    classes: PlatformClasses,
    platform: Platform,
    mcVersion: String?,
    onlyMine: Boolean,
    onOnlyMine: (Boolean) -> Unit,
    sort: PlatformSortField,
    onSort: (PlatformSortField) -> Unit,
    categories: Set<PlatformFilterCode>,
    onToggleCategory: (PlatformFilterCode) -> Unit,
    onClearCategories: () -> Unit
) {
    val groups = remember(classes, platform) { categoryGroups(classes, platform) }
    Column(
        modifier = modifier
            .assetsPanel()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mcVersion != null) {
            FilterSection(stringResource(R.string.cryo_assets_game_version)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssetChip(mcVersion, onlyMine) { onOnlyMine(true) }
                    AssetChip(stringResource(R.string.cryo_assets_all_versions), !onlyMine) { onOnlyMine(false) }
                }
            }
        }
        FilterSection(stringResource(R.string.cryo_assets_sort)) {
            ChipFlow {
                SortOptions.forEach { field ->
                    AssetChip(stringResource(field.getDisplayName()), sort == field) { onSort(field) }
                }
            }
        }
        //categorias organizadas em grupos
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cryo_assets_categories).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (categories.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClearCategories)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    text = stringResource(R.string.cryo_assets_clear_filters) + " (${categories.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CryoDanger
                )
            }
        }
        groups.forEach { group ->
            val count = group.codes.count { it in categories }
            FilterSection(stringResource(group.titleRes) + if (count > 0) "  ·  $count" else "") {
                ChipFlow {
                    group.codes.forEach { code ->
                        AssetChip(stringResource(code.getDisplayName()), code in categories, check = true) { onToggleCategory(code) }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(modifier = Modifier.size(13.dp), painter = painterResource(R.drawable.ic_info_outlined), contentDescription = null, tint = CryoGlassColors.Ice)
            Text(
                text = stringResource(if (classes == PlatformClasses.SHADERS) R.string.cryo_assets_hint_shaders else R.string.cryo_assets_hint_textures),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(CryoGlassColors.Ice)
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.sp),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
        content()
    }
}

@Composable
private fun AssetCard(data: PlatformSearchData, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cryo_asset_row")
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(CardBg, PanelBg)))
            .border(1.dp, Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.45f), CryoGlassColors.Ice.copy(alpha = 0.08f))), CardShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CryoAssetIcon(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                size = 46.dp,
                url = data.platformIconUrl()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.platformTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(lineHeight = 17.sp),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val author = data.platformAuthor()
                if (author.isNotBlank()) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            modifier = Modifier.heightIn(min = 28.dp),
            text = data.platformDescription(),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
            color = CryoGlassColors.Frost.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(modifier = Modifier.size(11.dp), painter = painterResource(R.drawable.ic_download), contentDescription = null, tint = CryoGlassColors.Ice)
                Text(
                    text = compactCount(data.platformDownloadCount()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Ice
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.cryo_assets_install),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep
                )
                Icon(modifier = Modifier.size(12.dp), painter = painterResource(R.drawable.ic_keyboard_arrow_right), contentDescription = null, tint = CryoGlassColors.Deep)
            }
        }
    }
}

@Composable
private fun AssetDetail(
    data: PlatformSearchData,
    mcVersion: String?,
    onlyMine: Boolean,
    targetDir: File?,
    onBack: () -> Unit
) {
    var versions by remember(data) { mutableStateOf<List<PlatformVersion>?>(null) }
    var error by remember(data) { mutableStateOf<AndroidStringText?>(null) }
    var retry by remember(data) { mutableStateOf(0) }

    LaunchedEffect(data, retry) {
        error = null
        versions = null
        try {
            val id = data.platformId()
            versions = withContext(NonCancellable + Dispatchers.IO) { fetchVersionsSafely(id, data.platform()).initAll(id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = mapExceptionToMessage(e)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .assetsPanel()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(modifier = Modifier.size(17.dp), painter = painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.generic_back), tint = CryoGlassColors.Ice)
            }
            CryoAssetIcon(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                size = 50.dp,
                url = data.platformIconUrl()
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = data.platformTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.platformDescription(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        modifier = Modifier.size(12.dp),
                        painter = painterResource(if (data.platform() == Platform.CURSEFORGE) R.drawable.img_platform_curseforge else R.drawable.img_platform_modrinth),
                        contentDescription = null
                    )
                    Text(
                        text = data.platform().displayName + "  ·  " + compactCount(data.platformDownloadCount()) + "  ·  " + data.platformAuthor(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Ice,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.cryo_assets_versions).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontSize = 9.sp),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )

        val list = versions
        val err = error
        when {
            err != null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = resolveAndroidString(err), style = MaterialTheme.typography.bodySmall, color = CryoDanger, textAlign = TextAlign.Center)
                AssetChip(stringResource(R.string.cryo_assets_retry), true) { retry++ }
            }
            list == null -> Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), color = CryoGlassColors.Ice, strokeWidth = 2.5.dp)
            }
            list.isEmpty() -> Text(
                text = stringResource(R.string.generic_no_matching_items),
                style = MaterialTheme.typography.bodyMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
            else -> {
                val matching = remember(list, mcVersion) {
                    if (mcVersion == null) list else list.filter { mcVersion in it.platformGameVersion() }
                }
                val noMatch = onlyMine && mcVersion != null && matching.isEmpty()
                val shown = remember(list, matching, onlyMine) {
                    (if (onlyMine && matching.isNotEmpty()) matching else list).take(MAX_VERSIONS_SHOWN)
                }
                if (targetDir == null) {
                    Text(text = stringResource(R.string.cryo_assets_no_version), style = MaterialTheme.typography.labelMedium, color = CryoDanger)
                } else if (noMatch) {
                    Text(text = stringResource(R.string.cryo_assets_no_match, mcVersion ?: ""), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(shown, key = { it.platformId() }) { v ->
                        VersionRow(version = v, mcVersion = mcVersion, targetDir = targetDir)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(version: PlatformVersion, mcVersion: String?, targetDir: File?) {
    val key = CryoAssetsInstaller.key(version)
    val state = CryoAssetsInstaller.states[key]
    val fileName = version.platformFileName()
    val installedOnDisk = remember(targetDir, fileName, state) { targetDir?.let { File(it, fileName).isFile } == true }
    val type = version.platformReleaseType()
    val games = version.platformGameVersion()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (state is CryoAssetsInstaller.State.Done || installedOnDisk) Color(0xFF15304A) else CardBg)
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (state is CryoAssetsInstaller.State.Done || installedOnDisk) 0.8f else 0.25f), CardShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = version.platformDisplayName(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(type.color.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    text = stringResource(type.textRes),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = type.color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = games.sortedByDescending { it == mcVersion }.take(4).joinToString(", ") + if (games.size > 4) " +${games.size - 4}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val enabled = targetDir != null
        when {
            state is CryoAssetsInstaller.State.Downloading -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.size(34.dp),
                    color = CryoGlassColors.Ice,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    strokeWidth = 3.dp
                )
                Text(text = "${(state.progress * 100).toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
            }
            state is CryoAssetsInstaller.State.Done || installedOnDisk -> Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.16f))
                    .border(1.dp, CryoGlassColors.Ice, CircleShape)
                    .then(if (enabled) Modifier.clickable { CryoAssetsInstaller.install(version, targetDir!!) } else Modifier)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(modifier = Modifier.size(13.dp), painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = CryoGlassColors.Ice)
                Text(text = stringResource(R.string.cryo_assets_installed), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CryoGlassColors.Ice)
            }
            else -> Column(horizontalAlignment = Alignment.End) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(if (enabled) Modifier.cryoBackground(cryoGradient()) else Modifier.background(Color.White.copy(alpha = 0.08f)))
                        .then(if (enabled) Modifier.clickable { CryoAssetsInstaller.install(version, targetDir!!) } else Modifier)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(modifier = Modifier.size(13.dp), painter = painterResource(R.drawable.ic_download), contentDescription = null, tint = if (enabled) CryoGlassColors.Deep else Color.White.copy(alpha = 0.4f))
                    Text(
                        text = stringResource(if (state is CryoAssetsInstaller.State.Failed) R.string.cryo_assets_retry else R.string.cryo_assets_install),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (enabled) CryoGlassColors.Deep else Color.White.copy(alpha = 0.4f)
                    )
                }
                if (state is CryoAssetsInstaller.State.Failed) {
                    Text(
                        modifier = Modifier.width(150.dp),
                        text = resolveAndroidString(state.message),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoDanger,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        content()
    }
}

@Composable
private fun AssetChip(text: String, selected: Boolean, check: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(CardBg)
            )
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.6f) else CryoGlassColors.Ice.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (check && selected) {
            Icon(modifier = Modifier.size(11.dp), painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = CryoGlassColors.Deep)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) CryoGlassColors.Deep else Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun AssetsSearchField(modifier: Modifier = Modifier, value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(PanelBg.copy(alpha = 0.94f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(modifier = Modifier.size(16.dp), painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = CryoGlassColors.Ice)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.generic_search),
                    style = MaterialTheme.typography.labelLarge,
                    color = CryoGlassColors.Frost.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(CryoGlassColors.Ice),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDone() }, onDone = { onDone() })
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onValueChange("") },
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = CryoGlassColors.Frost
            )
        }
    }
}

private fun compactCount(value: Long): String = when {
    value >= 1_000_000_000L -> String.format(java.util.Locale.ROOT, "%.1fB", value / 1_000_000_000.0)
    value >= 1_000_000L -> String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(java.util.Locale.ROOT, "%.1fK", value / 1_000.0)
    else -> value.toString()
}
