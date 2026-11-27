package com.russoviller.cryogenic.ui.screens.content.settings

import com.russoviller.cryogenic.ui.components.cryoBackground

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSectionHeader
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class ProfileFlag { BRAZIL, INDONESIA }

class TikTokProfile(
    val label: String,
    val user: String,
    val url: String,
    val flag: ProfileFlag? = null,
    val labelRes: Int? = null
)

private val russoProfiles = listOf(
    TikTokProfile("Principal", "russoviller.mc", "https://www.tiktok.com/@russoviller.mc?_r=1&_t=ZS-9B1dFZesB53", labelRes = R.string.cryo_tiktok_main),
    TikTokProfile("Secundário", "russoviller.blox", "https://www.tiktok.com/@russoviller.blox?_r=1&_t=ZS-9B1dEv9vnAy", labelRes = R.string.cryo_tiktok_secondary)
)

private val partnerProfiles = listOf(
    TikTokProfile("Wanders_test3", "wanders_test3", "https://www.tiktok.com/@wanders_test3?_r=1&_t=ZS-9B1dOJq8TRM", ProfileFlag.BRAZIL),
    TikTokProfile("QST", "qstzalith", "https://www.tiktok.com/@qstzalith?_r=1&_t=ZS-9B1dXYHMLBt", ProfileFlag.BRAZIL),
    TikTokProfile("Rato Player", "rato_player", "https://www.tiktok.com/@rato_player?_r=1&_t=ZS-9B1dMBiRir6", ProfileFlag.BRAZIL),
    TikTokProfile("Dolly", "dolly_lives_", "https://www.tiktok.com/@dolly_lives_?_r=1&_t=ZS-9B1dZnRLS69", ProfileFlag.BRAZIL),
    TikTokProfile("Jhon", "jhonatancristianoo", "https://www.tiktok.com/@jhonatancristianoo?_r=1&_t=ZS-9B1daffJrvw", ProfileFlag.BRAZIL),
    TikTokProfile("MonsterX", "monsteerxx", "https://www.tiktok.com/@monsteerxx?_r=1&_t=ZS-9BBWHx2RwSz", ProfileFlag.BRAZIL),
    TikTokProfile("Atomix", ".tomx.l", "https://www.tiktok.com/@.tomx.l?_r=1&_t=ZS-9BBWFnKDUk0", ProfileFlag.BRAZIL),
    TikTokProfile("Yakai", "yakaicogan1", "https://www.tiktok.com/@yakaicogan1?_r=1&_t=ZS-9BBWH1H213s", ProfileFlag.INDONESIA)
)

private object TikTokAvatars {
    private const val PREFS = "cryo_tiktok_avatars"
    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    private val avatarRegex = Regex("\"avatar(?:Medium|Thumb)Url\":\"([^\"]+)\"")
    private val resolved = ConcurrentHashMap<String, String>()
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun cached(context: Context, user: String): String? {
        return resolved[user] ?: context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(user, null)
    }

    suspend fun resolve(context: Context, user: String): String = withContext(Dispatchers.IO) {
        resolved[user]?.let { return@withContext it }
        val fetched = runCatching {
            val request = Request.Builder()
                .url("https://www.tiktok.com/embed/@$user")
                .header("User-Agent", UA)
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                avatarRegex.find(body)?.groupValues?.get(1)
                    ?.replace("\\u002F", "/")
                    ?.replace("\\u0026", "&")
                    ?.replace("\\/", "/")
            }
        }.getOrNull()
        val url = fetched ?: "https://unavatar.io/tiktok/$user?fallback=false&t=${System.currentTimeMillis() / 86_400_000L}"
        resolved[user] = url
        if (fetched != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(user, url).apply()
        }
        url
    }
}

@Composable
fun CryoTikTokSection(
    modifier: Modifier = Modifier,
    openLink: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CryoSectionHeader(title = stringResource(R.string.cryo_tiktok_title))
        TikTokGroup(stringResource(R.string.cryo_tiktok_russo), russoProfiles, openLink)
        TikTokGroup(stringResource(R.string.cryo_tiktok_partners), partnerProfiles, openLink)
    }
}

@Composable
private fun TikTokGroup(
    title: String,
    profiles: List<TikTokProfile>,
    openLink: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )
        profiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { profile ->
                    TikTokProfileTile(
                        modifier = Modifier.weight(1f),
                        profile = profile,
                        openLink = openLink
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TikTokProfileTile(
    modifier: Modifier,
    profile: TikTokProfile,
    openLink: (String) -> Unit
) {
    val context = LocalContext.current
    var avatar by remember(profile.user) { mutableStateOf(TikTokAvatars.cached(context, profile.user)) }
    val label = profile.labelRes?.let { stringResource(it) } ?: profile.label
    LaunchedEffect(profile.user) {
        avatar = TikTokAvatars.resolve(context, profile.user)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CryoGlassColors.Ice.copy(alpha = 0.10f), CryoGlassColors.Navy.copy(alpha = 0.35f))
                )
            )
            .border(1.dp, cryoBorderBrush(), RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient())
                .padding(2.dp)
                .clip(CircleShape)
                .background(CryoGlassColors.Deep),
            contentAlignment = Alignment.Center
        ) {
            val initial: @Composable () -> Unit = {
                Text(
                    text = label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Ice
                )
            }
            if (avatar != null) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    model = avatar,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    loading = { initial() },
                    error = { initial() }
                )
            } else {
                initial()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                profile.flag?.let { CountryFlag(it) }
            }
            Text(
                text = "@${profile.user}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(2.dp))
        CryoButton(
            modifier = Modifier.height(36.dp),
            onClick = { openLink(profile.url) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null
            )
            Text(text = stringResource(R.string.cryo_tiktok_open), maxLines = 1)
        }
    }
}

@Composable
private fun CountryFlag(flag: ProfileFlag) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .size(width = 21.dp, height = 14.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
    ) {
        val w = size.width
        val h = size.height
        when (flag) {
            ProfileFlag.BRAZIL -> {
                drawRect(Color(0xFF009C3B))
                val diamond = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    lineTo(w * 0.92f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.90f)
                    lineTo(w * 0.08f, h * 0.5f)
                    close()
                }
                drawPath(diamond, Color(0xFFFFDF00))
                drawCircle(Color(0xFF002776), radius = h * 0.24f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f))
                drawLine(
                    Color.White,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f - h * 0.24f, h * 0.46f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.5f + h * 0.24f, h * 0.56f),
                    strokeWidth = h * 0.06f
                )
            }
            ProfileFlag.INDONESIA -> {
                drawRect(Color(0xFFCE1126), size = androidx.compose.ui.geometry.Size(w, h / 2f))
                drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(0f, h / 2f), size = androidx.compose.ui.geometry.Size(w, h / 2f))
            }
        }
    }
}
