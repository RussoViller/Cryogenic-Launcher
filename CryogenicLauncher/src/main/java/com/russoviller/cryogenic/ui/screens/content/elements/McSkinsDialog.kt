package com.russoviller.cryogenic.ui.screens.content.elements

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonParser
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.SimpleTextInputField
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.russoviller.cryogenic.ui.components.safeRadialGradient

data class McSkinEntry(
    val id: String,
    val name: String,
    val source: String,
    val playerName: String? = null,
    val textureUrl: String? = null,
    val slim: Boolean? = null
)

class McSkinTexture(val bytes: ByteArray, val slim: Boolean, val face: ImageBitmap)

/** Fonte da busca de skins. */
enum class McSkinSource { ALL, LITTLESKIN, MINESKIN }

object McSkinsRepository {
    private const val AGENT = "CryogenicLauncher (+https://github.com/RussoViller/Cryogenic-Launcher)"
    private const val MINESKIN_API = "https://api.mineskin.org/v2/skins"
    private const val MOJANG_TEXTURES = "https://textures.minecraft.net/texture/"

    val featured = listOf(
        "Notch", "jeb_", "Dinnerbone", "Technoblade", "Dream", "Ph1LzA",
        "Grian", "MumboJumbo", "Herobrine", "MHF_Alex", "MHF_Steve", "Tommyinnit"
    ).map { McSkinEntry(id = "player_" + it.lowercase(), name = it, source = "Mojang", playerName = it) }

    private fun get(url: String, timeout: Int = 7000): ByteArray? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeout
            readTimeout = timeout
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", AGENT)
        }
        return try {
            if (connection.responseCode in 200..299) connection.inputStream.use { it.readBytes() } else null
        } finally {
            connection.disconnect()
        }
    }

    private fun json(url: String) = get(url)?.toString(Charsets.UTF_8)?.let { JsonParser.parseString(it) }

    private fun faceOf(bitmap: Bitmap): ImageBitmap {
        val scale = bitmap.width / 64
        val out = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawBitmap(bitmap, android.graphics.Rect(8 * scale, 8 * scale, 16 * scale, 16 * scale), android.graphics.Rect(0, 0, 8, 8), null)
        if (bitmap.height == bitmap.width) {
            canvas.drawBitmap(bitmap, android.graphics.Rect(40 * scale, 8 * scale, 48 * scale, 16 * scale), android.graphics.Rect(0, 0, 8, 8), null)
        }
        return out.asImageBitmap()
    }

    private fun texture(bytes: ByteArray, slim: Boolean?): McSkinTexture? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val valid = bitmap.width >= 64 && bitmap.width % 64 == 0 &&
            (bitmap.height == bitmap.width || bitmap.height * 2 == bitmap.width)
        if (!valid) return null
        return McSkinTexture(bytes, slim ?: detectSlim(bitmap), faceOf(bitmap))
    }

    /**
     * Fontes que não informam o modelo (MineSkin): no modelo slim o braço tem 3 px de largura,
     * então a coluna 54-55 da frente do braço direito fica transparente.
     */
    private fun detectSlim(bitmap: Bitmap): Boolean {
        if (bitmap.height != bitmap.width) return false
        val scale = bitmap.width / 64
        return runCatching {
            (20 until 32).all { y ->
                android.graphics.Color.alpha(bitmap.getPixel(54 * scale, y * scale)) == 0 &&
                    android.graphics.Color.alpha(bitmap.getPixel(55 * scale + scale - 1, y * scale)) == 0
            }
        }.getOrDefault(false)
    }

    private fun resolvePlayer(name: String): McSkinTexture? {
        runCatching {
            val profile = json("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(name, "UTF-8"))?.asJsonObject
            val id = profile?.get("id")?.asString
            if (id != null) {
                val session = json("https://sessionserver.mojang.com/session/minecraft/profile/$id")?.asJsonObject
                val value = session?.getAsJsonArray("properties")
                    ?.firstOrNull { it.asJsonObject.get("name")?.asString == "textures" }
                    ?.asJsonObject?.get("value")?.asString
                if (value != null) {
                    val decoded = JsonParser.parseString(String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)).asJsonObject
                    val skin = decoded.getAsJsonObject("textures")?.getAsJsonObject("SKIN")
                    val url = skin?.get("url")?.asString
                    val slim = skin?.getAsJsonObject("metadata")?.get("model")?.asString == "slim"
                    if (url != null) {
                        get(url.replace("http://", "https://"))?.let { bytes -> texture(bytes, slim)?.let { return it } }
                    }
                }
            }
        }
        return runCatching {
            get("https://mc-heads.net/skin/" + URLEncoder.encode(name, "UTF-8"))?.let { texture(it, false) }
        }.getOrNull()
    }

    fun load(entry: McSkinEntry): McSkinTexture? = when {
        entry.playerName != null -> resolvePlayer(entry.playerName)
        entry.textureUrl != null -> runCatching { get(entry.textureUrl)?.let { texture(it, entry.slim) } }.getOrNull()
        else -> null
    }

    /** Skins mostradas antes de buscar, conforme a fonte escolhida. */
    fun browse(source: McSkinSource): List<McSkinEntry> = when (source) {
        McSkinSource.ALL -> featured
        McSkinSource.LITTLESKIN -> littleSkin("")
        //Sem busca, o MineSkin devolve as mais recentes; as sem nome costumam ser de plugins de servidor
        McSkinSource.MINESKIN -> mineSkin("", 48).filter { it.name.isNotBlank() }.take(24)
    }

    fun search(query: String, source: McSkinSource = McSkinSource.ALL): List<McSkinEntry> {
        val clean = query.trim()
        if (clean.isEmpty()) return emptyList()
        val results = ArrayList<McSkinEntry>()
        if (source == McSkinSource.ALL && clean.matches(Regex("^[A-Za-z0-9_]{2,16}$"))) {
            results.add(McSkinEntry(id = "player_" + clean.lowercase(), name = clean, source = "Mojang", playerName = clean))
        }
        if (source != McSkinSource.MINESKIN) results += littleSkin(clean)
        if (source != McSkinSource.LITTLESKIN) results += mineSkin(clean, 24)
        return results.distinctBy { it.id }
    }

    private fun littleSkin(keyword: String): List<McSkinEntry> {
        val results = ArrayList<McSkinEntry>()
        runCatching {
            val root = json(
                "https://littleskin.cn/skinlib/list?filter=skin&sort=likes&page=1&keyword=" + URLEncoder.encode(keyword, "UTF-8")
            )?.asJsonObject
            val data = root?.getAsJsonArray("data")
            if (data != null) {
                for (i in 0 until minOf(data.size(), 18)) {
                    val item = data[i].asJsonObject
                    val tid = item.get("tid")?.asInt ?: continue
                    val name = item.get("name")?.asString ?: "#$tid"
                    val slim = item.get("type")?.asString.equals("alex", ignoreCase = true)
                    val hash = runCatching {
                        json("https://littleskin.cn/skinlib/info/$tid")?.asJsonObject?.get("hash")?.asString
                    }.getOrNull() ?: continue
                    results.add(
                        McSkinEntry(
                            id = "ls_$tid",
                            name = name,
                            source = "LittleSkin",
                            textureUrl = "https://littleskin.cn/textures/$hash",
                            slim = slim
                        )
                    )
                }
            }
        }
        return results
    }

    /** Galeria pública do MineSkin (texturas hospedadas nos servidores da Mojang). */
    private fun mineSkin(keyword: String, size: Int): List<McSkinEntry> {
        val results = ArrayList<McSkinEntry>()
        runCatching {
            val url = buildString {
                append(MINESKIN_API).append("?size=").append(size)
                if (keyword.isNotEmpty()) append("&filter=").append(URLEncoder.encode(keyword, "UTF-8"))
            }
            val skins = json(url)?.asJsonObject?.getAsJsonArray("skins") ?: return@runCatching
            val seen = HashSet<String>()
            for (element in skins) {
                val item = element.asJsonObject
                val hash = item.get("texture")?.takeIf { !it.isJsonNull }?.asString ?: continue
                if (!hash.matches(Regex("^[0-9a-f]{20,80}$")) || !seen.add(hash)) continue
                val uuid = item.get("uuid")?.takeIf { !it.isJsonNull }?.asString ?: hash
                val name = item.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
                results.add(
                    McSkinEntry(
                        id = "ms_$uuid",
                        name = name,
                        source = "MineSkin",
                        textureUrl = MOJANG_TEXTURES + hash,
                        slim = null
                    )
                )
            }
        }
        return results
    }
}

@Composable
fun McSkinsDialog(
    onDismiss: () -> Unit,
    onPick: (ByteArray, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<McSkinEntry>?>(null) }
    var source by remember { mutableStateOf(McSkinSource.ALL) }
    var browsed by remember { mutableStateOf<List<McSkinEntry>?>(null) }
    var browsing by remember { mutableStateOf(false) }
    val textures = remember { mutableStateMapOf<String, McSkinTexture?>() }
    val loading = remember { mutableStateMapOf<String, Boolean>() }
    val added = remember { mutableStateMapOf<String, Boolean>() }

    fun runSearch() {
        val text = query.trim()
        if (text.isEmpty()) {
            results = null
            return
        }
        searching = true
        focusManager.clearFocus()
        scope.launch {
            val found = withContext(Dispatchers.IO) { runCatching { McSkinsRepository.search(text, source) }.getOrDefault(emptyList()) }
            results = found
            searching = false
        }
    }

    //Lista inicial de cada fonte (antes de buscar)
    androidx.compose.runtime.LaunchedEffect(source) {
        browsed = null
        if (source == McSkinSource.ALL) return@LaunchedEffect
        browsing = true
        browsed = withContext(Dispatchers.IO) { runCatching { McSkinsRepository.browse(source) }.getOrDefault(emptyList()) }
        browsing = false
    }

    val entries = results ?: if (source == McSkinSource.ALL) McSkinsRepository.featured else browsed.orEmpty()
    val gridState = remember(results) { androidx.compose.foundation.lazy.grid.LazyGridState() }

    androidx.compose.runtime.LaunchedEffect(entries) {
        val gate = kotlinx.coroutines.sync.Semaphore(4)
        entries.filter { !textures.containsKey(it.id) && loading[it.id] != true }.forEach { entry ->
            loading[entry.id] = true
            launch {
                gate.acquire()
                try {
                    val tex = withContext(Dispatchers.IO) { runCatching { McSkinsRepository.load(entry) }.getOrNull() }
                    textures[entry.id] = tex
                } finally {
                    loading[entry.id] = false
                    gate.release()
                }
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
                        painter = painterResource(R.drawable.ic_checkroom),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_mcskins_kicker),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                        color = CryoGlassColors.Ice
                    )
                    Text(
                        text = stringResource(R.string.cryo_mcskins_title),
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
                        .background(Color.White.copy(alpha = 0.06f))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimpleTextInputField(
                    modifier = Modifier.weight(1f),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.cryo_mcskins_search_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.5f)
                        )
                    }
                )
                CryoButton(
                    enabled = !searching,
                    onClick = { runSearch() }
                ) {
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CryoGlassColors.Deep)
                    } else {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null
                        )
                    }
                    Text(text = stringResource(R.string.cryo_mcskins_search))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                McSkinSource.entries.forEach { option ->
                    McSourceTab(
                        label = when (option) {
                            McSkinSource.ALL -> stringResource(R.string.cryo_mcskins_source_all)
                            McSkinSource.LITTLESKIN -> "LittleSkin"
                            McSkinSource.MINESKIN -> "MineSkin"
                        },
                        selected = source == option,
                        onClick = {
                            if (source != option) {
                                source = option
                                if (query.isNotBlank()) runSearch() else results = null
                            }
                        }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = (if (results == null) stringResource(R.string.cryo_mcskins_featured)
                    else stringResource(R.string.cryo_mcskins_results, entries.size)).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                    color = CryoGlassColors.Ice
                )
                if (results != null) {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                results = null
                                query = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        text = stringResource(R.string.cryo_mcskins_back_featured),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryoGlassColors.Aurora
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (entries.isEmpty() && (browsing || searching)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 2.dp)
                } else if (entries.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.cryo_mcskins_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                    )
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        columns = GridCells.Adaptive(124.dp),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.id }, contentType = { "skin" }) { entry ->
                            McSkinTile(
                                entry = entry,
                                texture = textures[entry.id],
                                loading = loading[entry.id] != false && !textures.containsKey(entry.id),
                                added = added[entry.id] == true,
                                onClick = {
                                    val tex = textures[entry.id] ?: return@McSkinTile
                                    onPick(tex.bytes, tex.slim)
                                    added[entry.id] = true
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.cryo_mcskins_note),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun McSkinTile(
    entry: McSkinEntry,
    texture: McSkinTexture?,
    loading: Boolean,
    added: Boolean,
    onClick: () -> Unit
) {
    val shape = cryoChamferShape(12.dp)
    val available = texture != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy.copy(alpha = 0.85f), CryoGlassColors.Deep)))
            .border(if (added) 1.5.dp else 1.dp, if (added) cryoBorderBrush(1.5f) else cryoBorderBrush(0.6f), shape)
            .clickable(enabled = available, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(safeRadialGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.25f), CryoGlassColors.Deep))),
            contentAlignment = Alignment.Center
        ) {
            when {
                texture != null -> Image(
                    modifier = Modifier.size(48.dp),
                    bitmap = texture.face,
                    contentDescription = null,
                    filterQuality = FilterQuality.None
                )
                loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else -> Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_warning_outlined),
                    contentDescription = null,
                    tint = CryoGlassColors.Frost.copy(alpha = 0.5f)
                )
            }
            if (added) {
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
            text = entry.name.ifBlank { "MineSkin" },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            McChip(
                when (entry.source) {
                    "LittleSkin" -> "LSKIN"
                    "MineSkin" -> "MSKIN"
                    else -> entry.source.uppercase()
                }
            )
            texture?.let { McChip(if (it.slim) "SLIM" else "WIDE") }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (added) Modifier.background(CryoGlassColors.Aurora.copy(alpha = 0.18f))
                    else Modifier.cryoBackground(cryoGradient())
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (added) stringResource(R.string.cryo_mcskins_added) else stringResource(R.string.cryo_mcskins_add),
                style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                fontWeight = FontWeight.Black,
                color = if (added) CryoGlassColors.Aurora else CryoGlassColors.Deep
            )
        }
    }
}

@Composable
private fun McSourceTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(shadow = null),
        fontWeight = FontWeight.Black,
        color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost
    )
}

@Composable
private fun McChip(text: String) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, fontSize = 9.sp)
}
