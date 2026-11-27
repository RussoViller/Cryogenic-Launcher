package com.russoviller.cryogenic.ui.screens.content.download.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.annotation.DrawableRes
import com.russoviller.cryogenic.setting.AllSettings
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.versioninfo.MinecraftVersion
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.utils.formatDate

enum class VersionCategoryKind { RELEASE, SNAPSHOT, APRIL_FOOLS, OLD }

data class VersionCategory(
    val key: String,
    val title: String,
    val kind: VersionCategoryKind,
    val versions: List<MinecraftVersion>
)

private val updateNames = mapOf(
    "1.21" to "Tricky Trials",
    "1.20" to "Trails & Tales",
    "1.19" to "The Wild Update",
    "1.18" to "Caves & Cliffs: Part II",
    "1.17" to "Caves & Cliffs: Part I",
    "1.16" to "Nether Update",
    "1.15" to "Buzzy Bees",
    "1.14" to "Village & Pillage",
    "1.13" to "Update Aquatic",
    "1.12" to "World of Color",
    "1.11" to "Exploration Update",
    "1.10" to "Frostburn Update",
    "1.9" to "Combat Update",
    "1.8" to "Bountiful Update",
    "1.7" to "The Update that Changed the World",
    "1.6" to "Horse Update",
    "1.5" to "Redstone Update",
    "1.4" to "Pretty Scary Update"
)

private fun releaseGroupOf(id: String): String {
    val parts = id.split('.')
    return if (parts.firstOrNull() == "1" && parts.size >= 2) "1.${parts[1].takeWhile { it.isDigit() }}"
    else parts.firstOrNull()?.takeWhile { it.isDigit() }?.ifEmpty { id } ?: id
}

fun buildVersionCategories(versions: List<MinecraftVersion>): List<VersionCategory> {
    val sorted = versions.sortedDescending()
    val result = ArrayList<VersionCategory>()
    sorted.filter { it.type == MinecraftVersion.Type.Release }
        .groupBy { releaseGroupOf(it.version.id) }
        .forEach { (group, list) ->
            result.add(VersionCategory("release_$group", group, VersionCategoryKind.RELEASE, list))
        }
    sorted.filter { it.type == MinecraftVersion.Type.Snapshot }.takeIf { it.isNotEmpty() }?.let {
        result.add(VersionCategory("snapshot", "Snapshots", VersionCategoryKind.SNAPSHOT, it))
    }
    sorted.filter { it.type == MinecraftVersion.Type.AprilFools }.takeIf { it.isNotEmpty() }?.let {
        result.add(VersionCategory("april", "April Fools", VersionCategoryKind.APRIL_FOOLS, it))
    }
    sorted.filter { it.type == MinecraftVersion.Type.OldBeta || it.type == MinecraftVersion.Type.OldAlpha }
        .takeIf { it.isNotEmpty() }?.let {
            result.add(VersionCategory("old", "Classic", VersionCategoryKind.OLD, it))
        }
    return result
}

@Composable
private fun categoryIcon(kind: VersionCategoryKind): Painter = painterResource(
    when (kind) {
        VersionCategoryKind.RELEASE -> R.drawable.img_minecraft
        VersionCategoryKind.SNAPSHOT -> R.drawable.img_command_block
        VersionCategoryKind.APRIL_FOOLS -> R.drawable.img_diamond_block
        VersionCategoryKind.OLD -> R.drawable.img_old_grass_block
    }
)

@Composable
private fun categorySubtitle(category: VersionCategory): String = when (category.kind) {
    VersionCategoryKind.RELEASE -> updateNames[category.title]
        ?: if (!category.title.startsWith("1.")) stringResource(R.string.cryo_dl_year_drops, "20" + category.title)
        else stringResource(R.string.download_game_type_release)
    VersionCategoryKind.SNAPSHOT -> stringResource(R.string.download_game_type_snapshot)
    VersionCategoryKind.APRIL_FOOLS -> stringResource(R.string.download_game_type_april_fools)
    VersionCategoryKind.OLD -> stringResource(R.string.download_game_type_old)
}

private fun categoryAccent(category: VersionCategory): Color = emblemFor(category).color

@DrawableRes
private fun categoryImage(category: VersionCategory): Int =
    if (category.kind != VersionCategoryKind.RELEASE) R.drawable.img_mc_cat_other else imageForGroup(category.title)

@DrawableRes
private fun imageForGroup(group: String): Int = when (group) {
    "26" -> R.drawable.img_mc_cat_26
    "1.21" -> R.drawable.img_mc_cat_1_21
    "1.20" -> R.drawable.img_mc_cat_1_20
    "1.19" -> R.drawable.img_mc_cat_1_19
    "1.18" -> R.drawable.img_mc_cat_1_18
    "1.17" -> R.drawable.img_mc_cat_1_17
    "1.16" -> R.drawable.img_mc_cat_1_16
    "1.15" -> R.drawable.img_mc_cat_1_15
    else -> R.drawable.img_mc_cat_other
}

@DrawableRes
fun minecraftArtFor(gameVersion: String): Int = imageForGroup(releaseGroupOf(gameVersion))

@Composable
fun categoryImageAlpha(): Float =
    (AllSettings.launcherBackgroundOpacity.state / 100f).coerceIn(0f, 1f)

@Composable
private fun CategoryArt(category: VersionCategory, modifier: Modifier = Modifier) {
    val alpha = categoryImageAlpha()
    Box(modifier = modifier) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            painter = painterResource(categoryImage(category)),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CryoGlassColors.Deep.copy(alpha = 0.70f * alpha),
                            CryoGlassColors.Deep.copy(alpha = 0.20f * alpha),
                            CryoGlassColors.Deep.copy(alpha = 0.85f * alpha)
                        )
                    )
                )
        )
    }
}

@Composable
fun VersionCategoryGrid(
    modifier: Modifier = Modifier,
    versions: List<MinecraftVersion>,
    onVersionSelect: (String) -> Unit,
    openLink: (String) -> Unit
) {
    val categories = remember(versions) { buildVersionCategories(versions) }
    var opened by remember { mutableStateOf<VersionCategory?>(null) }

    if (categories.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.cryo_dl_no_versions),
                style = MaterialTheme.typography.bodyMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.key }) { category ->
            VersionCategoryTile(category = category, onClick = { opened = category })
        }
    }

    opened?.let { category ->
        VersionCategoryDialog(
            category = category,
            onDismiss = { opened = null },
            onInstall = { id ->
                opened = null
                onVersionSelect(id)
            },
            openLink = openLink
        )
    }
}

@Composable
private fun VersionCategoryTile(category: VersionCategory, onClick: () -> Unit) {
    val accent = categoryAccent(category)
    val shape = cryoChamferShape(20.dp)
    val fill = cryoFillFactor().coerceAtMost(1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.30f),
                        CryoGlassColors.Navy.copy(alpha = 0.6f + 0.35f * fill),
                        CryoGlassColors.Deep.copy(alpha = 0.75f + 0.25f * fill)
                    )
                )
            )
            .drawBehind {
                val step = 18.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.035f), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                val br = 18.dp.toPx()
                val i = 10.dp.toPx()
                val arc = 20.dp.toPx() - i
                drawPath(
                    com.russoviller.cryogenic.ui.theme.cryoCornerBracketPath(Offset(size.width - i, size.height - i), -1f, -1f, arc, br + arc * 0.5f),
                    accent,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            .border(1.5.dp, Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.1f), accent.copy(alpha = 0.6f))), shape)
            .clickable(onClick = onClick)
    ) {
        CategoryArt(category = category, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "MC // " + category.kind.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, fontSize = 9.sp),
                color = accent
            )
            Text(
                text = category.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.verticalGradient(listOf(Color.White, accent))
                ),
                maxLines = 1
            )
            Text(
                text = categorySubtitle(category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.75f))
                    .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                text = stringResource(R.string.cryo_dl_versions_count, category.versions.size),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = stringResource(R.string.cryo_dl_latest, category.versions.first().version.id),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VersionCategoryDialog(
    category: VersionCategory,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    openLink: (String) -> Unit
) {
    val accent = categoryAccent(category)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight()
                .cryoGlass(shape = cryoChamferShape(26.dp), cornerRadius = 26.dp, opaque = true)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .clip(cryoChamferShape(18.dp))
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.25f), CryoGlassColors.Deep)))
                    .border(1.dp, accent.copy(alpha = 0.6f), cryoChamferShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.cryo_dl_category_kicker),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                    color = accent
                )
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.verticalGradient(listOf(Color.White, accent))
                    ),
                    maxLines = 1
                )
                Text(
                    text = categorySubtitle(category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                CategoryArt(
                    category = category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(cryoChamferShape(14.dp))
                        .border(1.dp, accent.copy(alpha = 0.5f), cryoChamferShape(14.dp))
                )
                Text(
                    text = stringResource(R.string.cryo_dl_versions_count, category.versions.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = stringResource(R.string.cryo_dl_pick_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.generic_close),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(category.versions, key = { _, v -> v.version.id }) { index, version ->
                    VersionPickRow(
                        index = index,
                        version = version,
                        accent = accent,
                        latest = index == 0,
                        onInstall = { onInstall(version.version.id) },
                        openLink = openLink
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionPickRow(
    index: Int,
    version: MinecraftVersion,
    accent: Color,
    latest: Boolean,
    onInstall: () -> Unit,
    openLink: (String) -> Unit
) {
    val vm = version.version
    val urlSuffix = version.urlSuffix ?: vm.id
    val wiki = when (version.type) {
        MinecraftVersion.Type.Release -> stringResource(R.string.url_wiki_minecraft_game_release, urlSuffix)
        MinecraftVersion.Type.Snapshot, MinecraftVersion.Type.AprilFools -> stringResource(R.string.url_wiki_minecraft_game_snapshot, urlSuffix)
        else -> null
    }
    val shape = cryoChamferShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (latest) Modifier.cryoGlow(12.dp, selected = true, strength = 0.5f) else Modifier)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (latest) 0.22f else 0.08f),
                        CryoGlassColors.Navy.copy(alpha = 0.7f),
                        CryoGlassColors.Deep.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, if (latest) cryoBorderBrush(1.3f) else cryoBorderBrush(0.5f), shape)
            .clickable(onClick = onInstall)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = accent.copy(alpha = 0.8f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = vm.id,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                if (latest) {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .cryoBackground(cryoGradient())
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        text = stringResource(R.string.cryo_home_latest_version).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, shadow = null),
                        fontWeight = FontWeight.Black,
                        color = CryoGlassColors.Deep
                    )
                }
            }
            Text(
                text = formatDate(input = vm.releaseTime, pattern = stringResource(R.string.date_format)),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        }
        wiki?.let { url ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), CircleShape)
                    .clickable { openLink(url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = "Wiki",
                    tint = CryoGlassColors.Ice
                )
            }
        }
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .cryoBackground(cryoGradient())
                .clickable(onClick = onInstall)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_download_2_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
            Text(
                text = stringResource(R.string.cryo_install),
                style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Deep
            )
        }
    }
}
