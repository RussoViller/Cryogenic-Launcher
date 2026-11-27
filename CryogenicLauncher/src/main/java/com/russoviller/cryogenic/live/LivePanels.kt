package com.russoviller.cryogenic.live

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSlider
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val TikTokPink = Color(0xFFFE2C55)
private val TikTokCyan = Color(0xFF25F4EE)

// ======================================================================== Painel da Live (jogo)

/**
 * "Painel da Live" do menu do jogo: TikTok (live) e Modo Streamer (sobreposições).
 */
@Composable
fun LivePanel(
    modifier: Modifier,
    onAdjustPositions: () -> Unit,
    onCloseMenu: () -> Unit,
    color: Color,
    contentColor: Color
) {
    var page by remember { mutableIntStateOf(0) }
    when (page) {
        1 -> Column(modifier) {
            LiveBackHeader(stringResource(R.string.cryo_live_tiktok_testing)) { page = 0 }
            TikTokLivePanel(Modifier.fillMaxWidth().weight(1f), color, contentColor)
        }
        2 -> Column(modifier) {
            LiveBackHeader(stringResource(R.string.cryo_stream_mode)) { page = 0 }
            com.russoviller.cryogenic.streamer.StreamerPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                onAdjustPositions = onAdjustPositions,
                onCloseMenu = onCloseMenu,
                color = color,
                contentColor = contentColor
            )
        }
        else -> Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiveHomeCard(
                icon = "📡",
                title = stringResource(R.string.cryo_live_tiktok_testing),
                summary = stringResource(R.string.cryo_live_tiktok_summary),
                accent = TikTokPink,
                badge = if (LiveClient.active) stringResource(R.string.cryo_live_status_live) else null,
                onClick = { page = 1 }
            )
            LiveHomeCard(
                icon = "🎛",
                title = stringResource(R.string.cryo_stream_mode),
                summary = stringResource(R.string.cryo_live_streamer_summary),
                accent = CryoGlassColors.Ice,
                badge = null,
                onClick = { page = 2 }
            )
        }
    }
}

@Composable
private fun LiveHomeCard(icon: String, title: String, summary: String, accent: Color, badge: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f))))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiveIconBox(icon, accent)
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
            Text(summary, color = CryoGlassColors.Frost.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }
        if (badge != null) {
            Text(
                text = "● $badge",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TikTokPink)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text("›", color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

@Composable
private fun LiveIconBox(icon: String, accent: Color, size: Int = 36) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.35f))))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = (size * 0.45f).sp)
    }
}

@Composable
private fun LiveBackHeader(title: String, onBack: () -> Unit) {
    Text(
        modifier = Modifier
            .padding(start = 8.dp, top = 6.dp, end = 8.dp)
            .clip(CircleShape)
            .clickable(onClick = onBack)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = "‹  $title",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        color = CryoGlassColors.Ice,
        maxLines = 2
    )
}

// ======================================================================== estado compartilhado

/** Seções da configuração da live. */
private enum class LiveTab(val icon: String, val title: Int) {
    STREAM("📡", R.string.cryo_live_section_stream),
    QUALITY("🎞", R.string.cryo_live_section_quality),
    AUDIO("🎙", R.string.cryo_live_section_audio),
    PRIVACY("🔒", R.string.cryo_live_section_privacy),
    CHAT("💬", R.string.cryo_live_section_chat),
    BUBBLE("🔘", R.string.cryo_live_section_bubble)
}

private class LiveUi(
    val config: LiveConfig,
    val active: Boolean,
    val update: ((LiveConfig) -> LiveConfig) -> Unit,
    val start: () -> Unit,
    val stop: () -> Unit,
    val pickImage: () -> Unit,
    /** abrir a página do TikTok (só no launcher) */
    val openPage: (() -> Unit)?
)

@Composable
private fun rememberLiveUi(webLogin: Boolean): LiveUi {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        LiveConfigStore.reload(context)
        LiveClient.connect(context)
    }
    var pageOpen by remember { mutableStateOf(false) }
    val start = rememberLiveStarter()
    val scope = rememberCoroutineScope()
    val imageFailed = stringResource(R.string.cryo_stream_image_failed)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { LiveConfigStore.importPrivacyImage(context, uri) }
                if (path == null) Toast.makeText(context, imageFailed, Toast.LENGTH_SHORT).show()
                else LiveConfigStore.update(context) { it.copy(privacyImagePath = path) }
            }
        }
    }
    val config = LiveConfigStore.config
    if (pageOpen) {
        TikTokPageDialog(user = config.tiktokUser) {
            pageOpen = false
            LiveClient.reloadChat()
        }
    }
    return LiveUi(
        config = config,
        active = LiveClient.active,
        update = { change -> LiveConfigStore.update(context, change) },
        start = start,
        stop = { LiveClient.stop(context) },
        pickImage = { runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } },
        openPage = if (webLogin) ({ pageOpen = true }) else null
    )
}

@Composable
private fun tabTag(tab: LiveTab, ui: LiveUi): String {
    val c = ui.config
    return when (tab) {
        LiveTab.STREAM -> if (c.isReady) c.serverUrl.substringAfter("://").substringBefore('/').ifEmpty { c.serverUrl }
        else stringResource(R.string.cryo_live_not_set)
        LiveTab.QUALITY -> "${c.resolution}p · ${c.fps} FPS"
        LiveTab.AUDIO -> {
            val mic = if (ui.active) LiveClient.micOn else c.micEnabled
            (if (mic) "🎙 ${if (ui.active) LiveClient.micVolume else c.micVolume}%" else "🎙✕") +
                " · 🔊 ${if (ui.active) LiveClient.internalVolume else c.internalVolume}%"
        }
        LiveTab.PRIVACY -> stringResource(if (if (ui.active) LiveClient.privacy else c.privacyEnabled) R.string.cryo_live_on else R.string.cryo_live_off)
        LiveTab.CHAT -> if (c.tiktokUser.isBlank()) stringResource(R.string.cryo_live_not_set) else "@${c.tiktokUser}"
        LiveTab.BUBBLE -> stringResource(if (c.floatingBubble) R.string.cryo_live_on else R.string.cryo_live_off)
    }
}

// ======================================================================== TikTok no jogo (compacto)

/**
 * Configuração e controle da live do TikTok dentro do Painel da Live do jogo:
 * aviso de testes, cartão de status com os botões e as seções em abas.
 */
@Composable
fun TikTokLivePanel(modifier: Modifier, color: Color, contentColor: Color, webLogin: Boolean = false) {
    val ui = rememberLiveUi(webLogin)
    var tab by rememberSaveable { mutableStateOf(LiveTab.STREAM) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveTestingBanner()
        LiveHeroCard(ui, showPreview = false)
        LiveActions(ui)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LiveTab.entries.forEach { t ->
                LiveTabChip(t, selected = t == tab) { tab = t }
            }
        }
        LiveTabContent(tab, ui)
        LiveHint(stringResource(R.string.cryo_live_screen_note))
    }
}

@Composable
private fun LiveTabChip(tab: LiveTab, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.6f), CryoGlassColors.Aurora.copy(alpha = 0.2f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(tab.icon, fontSize = 13.sp)
        Text(
            stringResource(tab.title),
            color = Color.White,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

// ======================================================================== tela do launcher

/**
 * Tela "Live TikTok" do launcher, no estilo do Estúdio de temas: seções à esquerda;
 * à direita a prévia da transmissão, os botões e as opções da seção escolhida.
 */
@Composable
fun TikTokLiveLauncherContent(modifier: Modifier, color: Color, contentColor: Color) {
    val ui = rememberLiveUi(webLogin = true)
    var tab by rememberSaveable { mutableStateOf(LiveTab.STREAM) }
    Column(
        modifier = modifier
            .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveTestingBanner()
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.width(236.dp).fillMaxHeight()) {
                Text(
                    text = stringResource(R.string.cryo_live_screen_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_live_screen_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveTab.entries.forEach { t ->
                        LiveTabTile(t, tabTag(t, ui), selected = t == tab) { tab = t }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiveHeroCard(ui, showPreview = true)
                LiveActions(ui)
                LiveTabContent(tab, ui)
                LiveNoteBox(stringResource(R.string.cryo_live_screen_note))
            }
        }
    }
}

@Composable
private fun LiveTabTile(tab: LiveTab, tag: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.55f), CryoGlassColors.Aurora.copy(alpha = 0.15f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveIconBox(tab.icon, if (tab == LiveTab.STREAM) TikTokPink else CryoGlassColors.Ice, 34)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(tab.title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Ice,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ======================================================================== peças comuns

@Composable
private fun LiveTestingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300).copy(alpha = 0.22f), TikTokPink.copy(alpha = 0.12f))))
            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.75f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚠", fontSize = 16.sp, color = Color(0xFFFFC94D))
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.cryo_live_testing_banner),
            color = Color(0xFFFFE08A),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp)
        )
    }
}

/** Cartão principal: prévia (launcher), status, qualidade e espectadores. */
@Composable
private fun LiveHeroCard(ui: LiveUi, showPreview: Boolean) {
    val status = LiveClient.status
    val elapsed = rememberLiveElapsed(LiveClient.startedAt)
    val accent = when (status) {
        LiveStatus.LIVE -> TikTokPink
        LiveStatus.ERROR -> Color(0xFFFF8A65)
        LiveStatus.CONNECTING, LiveStatus.RECONNECTING -> TikTokCyan
        LiveStatus.IDLE -> CryoGlassColors.Ice
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
            .border(1.dp, accent.copy(alpha = if (status == LiveStatus.IDLE) 0.3f else 0.8f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPreview) {
            LivePreviewBox(Modifier.weight(1.35f), ui)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(if (showPreview) R.string.cryo_live_preview_title else R.string.cryo_live_status_title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice
            )
            Text(
                text = when (status) {
                    LiveStatus.LIVE -> stringResource(R.string.cryo_live_status_live) + "  " + elapsed
                    LiveStatus.CONNECTING -> stringResource(R.string.cryo_live_status_connecting)
                    LiveStatus.RECONNECTING -> stringResource(R.string.cryo_live_status_reconnecting)
                    LiveStatus.ERROR -> stringResource(R.string.cryo_live_status_error)
                    LiveStatus.IDLE -> stringResource(R.string.cryo_live_status_idle)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LiveStatChip("${ui.config.resolution}p")
                LiveStatChip("${ui.config.fps} FPS")
                if (status == LiveStatus.LIVE && LiveClient.bitrateKbps > 0) LiveStatChip("${LiveClient.bitrateKbps} kbps")
                if (status == LiveStatus.LIVE && LiveClient.viewers.isNotEmpty()) LiveStatChip("👁 ${LiveClient.viewers}", TikTokPink)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .alpha(if (ui.active) 1f else 0.6f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (ui.active) Brush.horizontalGradient(listOf(TikTokCyan, TikTokPink))
                        else cryoGradient()
                    )
            )
            val line = when {
                LiveClient.error != null && (status == LiveStatus.ERROR || status == LiveStatus.RECONNECTING || status == LiveStatus.IDLE) ->
                    LiveClient.error.orEmpty()
                ui.active && LiveClient.privacy -> stringResource(R.string.cryo_live_privacy_active)
                ui.active -> stringResource(R.string.cryo_live_streaming)
                ui.config.isReady -> stringResource(R.string.cryo_live_ready)
                else -> stringResource(R.string.cryo_live_not_ready)
            }
            Text(
                text = "● $line",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    status == LiveStatus.ERROR -> Color(0xFFFFAB91)
                    ui.active -> TikTokCyan
                    ui.config.isReady -> CryoGlassColors.Aurora
                    else -> CryoGlassColors.Ice
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveStatChip(text: String, accent: Color = CryoGlassColors.Ice) {
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        maxLines = 1
    )
}

/** Prévia da transmissão (o mesmo quadro que vai para a live) + botão de tela cheia. */
@Composable
private fun LivePreviewBox(modifier: Modifier, ui: LiveUi) {
    val portrait = ui.config.orientation == LiveOrientation.PORTRAIT
    var fullscreen by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            LivePreviewContent(portrait, big = false)
        }
        CryoTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { fullscreen = true }
        ) {
            Text("⛶", fontWeight = FontWeight.Black)
            MarqueeText(text = stringResource(R.string.cryo_live_fullscreen_open))
        }
    }
    if (fullscreen) {
        LiveFullscreenPreview(portrait) { fullscreen = false }
    }
}

/** O vídeo da live (ou o aviso de que ainda não começou), dentro de uma caixa preta. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.LivePreviewContent(portrait: Boolean, big: Boolean) {
    val streaming = LiveClient.status == LiveStatus.LIVE || LiveClient.status == LiveStatus.RECONNECTING
    if (streaming && !LocalInspectionMode.current) {
        AndroidView(
            modifier = if (portrait) Modifier.fillMaxHeight().aspectRatio(9f / 16f) else Modifier.fillMaxSize(),
            factory = { ctx -> LivePreview.createView(ctx) }
        )
        if (!big) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TikTokPink)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                text = "● LIVE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            )
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📺", fontSize = if (big) 48.sp else 28.sp)
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.cryo_live_preview_idle),
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                style = if (big) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Prévia da live na tela inteira do celular (com a live ligada ou desligada).
 * Enquanto aberta, ela recebe a prévia; ao fechar, a prévia volta para a telinha.
 */
@Composable
private fun LiveFullscreenPreview(portrait: Boolean, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // esconde as barras do sistema: tela cheia de verdade
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.DisposableEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.let { w ->
                w.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { }
        }
        val elapsed = rememberLiveElapsed(LiveClient.startedAt)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val ratio = 16f / 9f
            val wide = maxWidth / maxHeight > ratio
            Box(
                modifier = if (wide) Modifier.fillMaxHeight().aspectRatio(ratio) else Modifier.fillMaxWidth().aspectRatio(ratio),
                contentAlignment = Alignment.Center
            ) {
                LivePreviewContent(portrait, big = true)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (LiveClient.status == LiveStatus.LIVE) {
                    LiveStatChip("● LIVE  $elapsed", TikTokPink)
                    if (LiveClient.viewers.isNotEmpty()) LiveStatChip("👁 ${LiveClient.viewers}", TikTokPink)
                } else {
                    LiveStatChip(stringResource(R.string.cryo_live_status_idle))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    text = "✕  " + stringResource(R.string.cryo_live_fullscreen_close),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/** Iniciar/encerrar, microfone e bloquear tela. */
@Composable
private fun LiveActions(ui: LiveUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ui.active) {
            CryoButton(
                modifier = Modifier.weight(1.2f),
                onClick = ui.stop,
                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink, contentColor = Color.White)
            ) {
                Text("■", fontWeight = FontWeight.Black)
                MarqueeText(text = stringResource(R.string.cryo_live_stop))
            }
        } else {
            CryoButton(
                modifier = Modifier.weight(1.2f),
                enabled = ui.config.isReady,
                onClick = ui.start,
                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink, contentColor = Color.White)
            ) {
                Text("●", fontWeight = FontWeight.Black)
                MarqueeText(text = stringResource(R.string.cryo_live_start))
            }
        }
        val micOn = if (ui.active) LiveClient.micOn else ui.config.micEnabled
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            enabled = !ui.active || LiveClient.hasMic,
            onClick = { if (ui.active) LiveClient.setMic(!micOn) else ui.update { it.copy(micEnabled = !micOn) } }
        ) {
            MarqueeText(text = stringResource(if (micOn) R.string.cryo_live_mic_on else R.string.cryo_live_mic_off))
        }
        val privacyOn = if (ui.active) LiveClient.privacy else ui.config.privacyEnabled
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            onClick = { if (ui.active) LiveClient.changePrivacy(!privacyOn) else ui.update { it.copy(privacyEnabled = !privacyOn) } }
        ) {
            MarqueeText(text = stringResource(if (privacyOn) R.string.cryo_live_privacy_on_short else R.string.cryo_live_privacy_off_short))
        }
    }
}

/** Opções de uma seção, num cartão igual aos do Estúdio de temas. */
@Composable
private fun LiveTabContent(tab: LiveTab, ui: LiveUi) {
    val context = LocalContext.current
    val c = ui.config
    LiveSection("${tab.icon}  " + stringResource(tab.title)) {
        when (tab) {
            LiveTab.STREAM -> {
                LiveTextField(
                    value = c.serverUrl,
                    hint = stringResource(R.string.cryo_live_server_hint),
                    keyboardType = KeyboardType.Uri,
                    enabled = !ui.active,
                    onApply = { v -> ui.update { it.copy(serverUrl = v.trim()) } }
                )
                LiveTextField(
                    value = c.streamKey,
                    hint = stringResource(R.string.cryo_live_key_hint),
                    password = true,
                    enabled = !ui.active,
                    onApply = { v -> ui.update { it.copy(streamKey = v.trim()) } }
                )
                LiveHint(stringResource(R.string.cryo_live_key_help))
            }
            LiveTab.QUALITY -> {
                LiveChoices(
                    title = stringResource(R.string.cryo_live_resolution),
                    options = LiveConfig.RESOLUTIONS.map { "${it}p" },
                    selected = LiveConfig.RESOLUTIONS.indexOf(c.resolution),
                    enabled = !ui.active
                ) { i -> ui.update { it.copy(resolution = LiveConfig.RESOLUTIONS[i]) } }
                LiveChoices(
                    title = stringResource(R.string.cryo_live_fps),
                    options = LiveConfig.FRAME_RATES.map { "$it FPS" },
                    selected = LiveConfig.FRAME_RATES.indexOf(c.fps),
                    enabled = !ui.active
                ) { i -> ui.update { it.copy(fps = LiveConfig.FRAME_RATES[i]) } }
                val values = listOf(0, 2000, 3000, 4500, 6000)
                val auto = stringResource(R.string.cryo_live_bitrate_auto, c.copy(bitrateKbps = 0).effectiveBitrateKbps())
                LiveChoices(
                    title = stringResource(R.string.cryo_live_bitrate),
                    options = values.map { if (it == 0) auto else "$it kbps" },
                    selected = values.indexOf(c.bitrateKbps).coerceAtLeast(0),
                    enabled = !ui.active
                ) { i -> ui.update { it.copy(bitrateKbps = values[i]) } }
                LiveChoices(
                    title = stringResource(R.string.cryo_live_orientation),
                    options = listOf(stringResource(R.string.cryo_live_orientation_landscape), stringResource(R.string.cryo_live_orientation_portrait)),
                    selected = c.orientation.ordinal,
                    enabled = !ui.active
                ) { i -> ui.update { it.copy(orientation = LiveOrientation.entries[i]) } }
                if (ui.active) LiveHint(stringResource(R.string.cryo_live_quality_locked))
            }
            LiveTab.AUDIO -> {
                //Microfone da live: o do sistema, celular, fone Bluetooth ou com fio
                LiveChoices(
                    title = stringResource(R.string.cryo_live_mic_device),
                    options = listOf(
                        stringResource(R.string.cryo_live_mic_auto),
                        stringResource(R.string.cryo_live_mic_phone),
                        stringResource(R.string.cryo_live_mic_bt),
                        stringResource(R.string.cryo_live_mic_wired)
                    ),
                    selected = c.micDevice.ordinal,
                    enabled = !ui.active
                ) { i -> ui.update { it.copy(micDevice = LiveMicDevice.entries[i]) } }
                LiveHint(
                    stringResource(
                        when (c.micDevice) {
                            LiveMicDevice.AUTO -> R.string.cryo_live_mic_auto_note
                            LiveMicDevice.PHONE -> R.string.cryo_live_mic_phone_note
                            LiveMicDevice.BLUETOOTH -> R.string.cryo_live_mic_bt_note
                            LiveMicDevice.WIRED -> R.string.cryo_live_mic_wired_note
                        }
                    )
                )
                if (ui.active) {
                    if (LiveClient.micNote.isNotEmpty()) {
                        Text(LiveClient.micNote, color = CryoGlassColors.Ice, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    if (LiveClient.voiceShared) {
                        Text(stringResource(R.string.cryo_live_voice_shared), color = TikTokCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    LiveHint(stringResource(R.string.cryo_live_mic_next))
                }
                val micOn = if (ui.active) LiveClient.micOn else c.micEnabled
                LiveSwitchRow(
                    title = stringResource(R.string.cryo_live_mic),
                    checked = micOn,
                    enabled = !ui.active || LiveClient.hasMic
                ) { on -> if (ui.active) LiveClient.setMic(on) else ui.update { it.copy(micEnabled = on) } }
                LiveSliderRow(
                    label = stringResource(R.string.cryo_live_mic_volume),
                    value = if (ui.active) LiveClient.micVolume else c.micVolume,
                    range = 0..100,
                    suffix = "%",
                    onLive = if (ui.active) ({ v -> LiveClient.changeMicVolume(v) }) else null
                ) { v -> if (!ui.active) ui.update { it.copy(micVolume = v) } }
                LiveSliderRow(
                    label = stringResource(R.string.cryo_live_internal_volume),
                    value = if (ui.active) LiveClient.internalVolume else c.internalVolume,
                    range = 0..100,
                    suffix = "%",
                    onLive = if (ui.active) ({ v -> LiveClient.changeInternalVolume(v) }) else null
                ) { v -> if (!ui.active) ui.update { it.copy(internalVolume = v) } }
            }
            LiveTab.PRIVACY -> {
                val on = if (ui.active) LiveClient.privacy else c.privacyEnabled
                LiveSwitchRow(
                    title = stringResource(R.string.cryo_live_privacy),
                    checked = on
                ) { v -> if (ui.active) LiveClient.changePrivacy(v) else ui.update { it.copy(privacyEnabled = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CryoTonalButton(modifier = Modifier.weight(1f), onClick = ui.pickImage) {
                        MarqueeText(text = stringResource(if (c.privacyImagePath.isBlank()) R.string.cryo_live_privacy_pick else R.string.cryo_live_privacy_change))
                    }
                    if (c.privacyImagePath.isNotBlank()) {
                        CryoTonalButton(modifier = Modifier.weight(1f), onClick = { ui.update { it.copy(privacyImagePath = "") } }) {
                            MarqueeText(text = stringResource(R.string.cryo_live_privacy_remove))
                        }
                    }
                }
                LiveHint(stringResource(if (c.privacyImagePath.isBlank()) R.string.cryo_live_privacy_note_black else R.string.cryo_live_privacy_note_image))
            }
            LiveTab.CHAT -> {
                LiveTextField(
                    value = c.tiktokUser,
                    hint = stringResource(R.string.cryo_live_user_hint),
                    onApply = { v -> ui.update { it.copy(tiktokUser = LiveConfig.normalizeUser(v)) } }
                )
                LiveHint(
                    when (LiveClient.chatState) {
                        TikTokChatReader.STATE_LOADING -> stringResource(R.string.cryo_live_chat_loading)
                        TikTokChatReader.STATE_WAITING -> stringResource(R.string.cryo_live_chat_waiting)
                        TikTokChatReader.STATE_OK -> stringResource(R.string.cryo_live_chat_ok)
                        TikTokChatReader.STATE_NOT_FOUND -> stringResource(R.string.cryo_live_chat_not_found)
                        TikTokChatReader.STATE_BLOCKED -> stringResource(R.string.cryo_live_chat_blocked)
                        else -> stringResource(R.string.cryo_live_chat_help)
                    }
                )
                if (LiveClient.chatDetail.isNotEmpty() && ui.active) {
                    Text(
                        text = LiveClient.chatDetail,
                        color = CryoGlassColors.Ice.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                val openPage = ui.openPage
                if (openPage != null && c.tiktokUser.isNotBlank()) {
                    CryoTonalButton(modifier = Modifier.fillMaxWidth(), onClick = openPage) {
                        MarqueeText(text = stringResource(R.string.cryo_live_chat_open_page))
                    }
                }
                LiveSwitchRow(
                    title = stringResource(R.string.cryo_live_chat_in_game),
                    checked = c.chatInGame
                ) { v -> ui.update { it.copy(chatInGame = v) } }
                LiveSliderRow(stringResource(R.string.cryo_live_chat_size), c.chatSize, 70..160, "%") { v -> ui.update { it.copy(chatSize = v) } }
                LiveSliderRow(stringResource(R.string.cryo_live_chat_width), c.chatWidth, 20..60, "%") { v -> ui.update { it.copy(chatWidth = v) } }
                LiveSliderRow(stringResource(R.string.cryo_live_chat_opacity), c.chatOpacity, 10..100, "%") { v -> ui.update { it.copy(chatOpacity = v) } }
                LiveChoices(
                    title = stringResource(R.string.cryo_live_chat_position),
                    options = listOf(
                        stringResource(R.string.cryo_live_corner_top_left),
                        stringResource(R.string.cryo_live_corner_top_right),
                        stringResource(R.string.cryo_live_corner_bottom_left),
                        stringResource(R.string.cryo_live_corner_bottom_right)
                    ),
                    selected = c.chatCorner.ordinal
                ) { i -> ui.update { it.copy(chatCorner = LiveChatCorner.entries[i]) } }
            }
            LiveTab.BUBBLE -> {
                LiveSwitchRow(
                    title = stringResource(R.string.cryo_live_bubble),
                    checked = c.floatingBubble
                ) { v -> ui.update { it.copy(floatingBubble = v) } }
                var granted by remember { mutableStateOf(LiveConfigStore.canDrawOverlays(context)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        granted = LiveConfigStore.canDrawOverlays(context)
                        delay(1500L)
                    }
                }
                if (c.floatingBubble && !granted) {
                    CryoButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { runCatching { context.startActivity(LiveConfigStore.overlayPermissionIntent(context)) } }
                    ) {
                        MarqueeText(text = stringResource(R.string.cryo_live_bubble_permission))
                    }
                }
                LiveHint(stringResource(R.string.cryo_live_bubble_note))
            }
        }
    }
}

@Composable
private fun LiveSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
            color = CryoGlassColors.Ice
        )
        content()
    }
}

@Composable
private fun LiveSwitchRow(title: String, checked: Boolean, enabled: Boolean = true, summary: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.75f))
            }
        }
        DefaultSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/**
 * Barra com o valor ao lado (igual aos ajustes de cor). Salva ao soltar; [onLive] aplica enquanto
 * arrasta (volumes durante a live).
 */
@Composable
private fun LiveSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    onLive: ((Int) -> Unit)? = null,
    onCommit: (Int) -> Unit
) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "${v.roundToInt()}$suffix",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
        }
        CryoSlider(
            modifier = Modifier.fillMaxWidth(),
            value = v,
            onValueChange = {
                v = it.roundToInt().toFloat()
                onLive?.invoke(v.roundToInt())
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            onValueChangeFinished = { onCommit(v.roundToInt()) }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LiveChoices(title: String, options: List<String>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Text(
                    modifier = Modifier
                        .alpha(if (enabled || on) 1f else 0.45f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (on) cryoGradient()
                            else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                        )
                        .border(1.dp, if (on) Color.White.copy(alpha = 0.55f) else CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled && !on) { onSelect(i) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    text = label,
                    color = if (on) CryoGlassColors.Deep else Color.White,
                    fontWeight = if (on) FontWeight.Black else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LiveHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = CryoGlassColors.Frost.copy(alpha = 0.75f)
    )
}

@Composable
private fun LiveNoteBox(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
            .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = CryoGlassColors.Frost
    )
}

/** Tempo de live formatado, atualizado a cada segundo. */
@Composable
fun rememberLiveElapsed(startedAt: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (startedAt > 0L) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    return if (startedAt > 0L) LiveFloatingWindow.formatElapsed(startedAt).also { now.hashCode() } else ""
}

@Composable
private fun LiveTextField(
    value: String,
    hint: String,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    onApply: (String) -> Unit
) {
    val ice = CryoGlassColors.Ice
    val focus = LocalFocusManager.current
    var text by remember(value) { mutableStateOf(value) }
    var reveal by remember { mutableStateOf(false) }
    val dirty = text.trim() != value.trim()
    val apply = {
        onApply(text)
        focus.clearFocus()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, ice.copy(alpha = if (enabled) 0.45f else 0.2f), RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(text = hint, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp, maxLines = 1)
            }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                enabled = enabled,
                onValueChange = { text = it.replace("\n", "") },
                singleLine = true,
                textStyle = TextStyle(color = Color.White.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 13.sp),
                cursorBrush = SolidColor(ice),
                visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { apply() })
            )
        }
        if (password) {
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { reveal = !reveal }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                text = if (reveal) "🙈" else "👁",
                fontSize = 14.sp
            )
        }
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (dirty) ice else Color.Transparent)
                .clickable(enabled = dirty && enabled) { apply() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            text = "✓",
            color = if (dirty) CryoGlassColors.Deep else ice.copy(alpha = 0.4f),
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Chat da live por cima do jogo (sem tocar: os controles por baixo continuam funcionando).
 * Também aparece na transmissão, porque a captura é da tela inteira.
 */
@Composable
fun LiveChatOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        LiveConfigStore.reload(context)
        LiveClient.connect(context)
    }
    val config = LiveConfigStore.config
    if (!LiveClient.active || !config.chatInGame) return
    // recarrega quando o painel muda as opções em outro processo
    LaunchedEffect(LiveClient.status) { LiveConfigStore.reload(context) }
    val elapsed = rememberLiveElapsed(LiveClient.startedAt)
    BoxWithConstraints(modifier = modifier) {
        val align = when (config.chatCorner) {
            LiveChatCorner.TOP_LEFT -> Alignment.TopStart
            LiveChatCorner.TOP_RIGHT -> Alignment.TopEnd
            LiveChatCorner.BOTTOM_LEFT -> Alignment.BottomStart
            LiveChatCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
        val scale = config.chatSize / 100f
        val bg = config.chatOpacity / 100f
        Column(
            modifier = Modifier
                .align(align)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .width(maxWidth * (config.chatWidth / 100f))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.55f * bg))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (LiveClient.status == LiveStatus.LIVE) "● LIVE $elapsed" else "● …") +
                        (if (LiveClient.viewers.isNotEmpty()) "  👁 ${LiveClient.viewers}" else ""),
                    color = TikTokPink.copy(alpha = 0.5f + 0.5f * bg),
                    fontWeight = FontWeight.Black,
                    fontSize = (11 * scale).sp
                )
                if (!LiveClient.micOn) {
                    Text("  🎙✕", color = Color.White.copy(alpha = bg), fontSize = (11 * scale).sp)
                }
                if (LiveClient.privacy) {
                    Text("  🔒", color = Color.White.copy(alpha = bg), fontSize = (11 * scale).sp)
                }
            }
            val messages = LiveClient.chat.takeLast(8)
            if (messages.isEmpty()) {
                Text(
                    text = stringResource(R.string.cryo_live_chat_empty),
                    color = Color.White.copy(alpha = 0.6f * bg + 0.2f),
                    fontSize = (12 * scale).sp
                )
            }
            for (m in messages) {
                Text(
                    text = buildAnnotatedString {
                        if (m.user.isNotEmpty()) {
                            withStyle(SpanStyle(color = TikTokCyan.copy(alpha = 0.4f + 0.6f * bg), fontWeight = FontWeight.Bold)) { append(m.user) }
                            append("  ")
                        }
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.4f + 0.6f * bg))) { append(m.text) }
                    },
                    fontSize = (12 * scale).sp,
                    lineHeight = (15 * scale).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Página da live no TikTok (visível), para entrar na conta ou resolver uma verificação.
 * Os cookies ficam com o leitor do chat (mesmo processo). O vídeo da live é bloqueado para o som
 * não ir para a transmissão.
 */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TikTokPageDialog(user: String, onDismiss: () -> Unit) {
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val close = {
        runCatching { android.webkit.CookieManager.getInstance().flush() }
        webView?.let { v -> runCatching { v.stopLoading(); v.destroy() } }
        webView = null
        onDismiss()
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = close,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CryoGlassColors.Deep)
                .border(1.dp, TikTokCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cryo_live_chat_page_note),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TikTokPink)
                        .clickable { close() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    text = stringResource(R.string.cryo_live_chat_page_done),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.userAgentString = TikTokChatReader.DESKTOP_UA
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        runCatching { android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true) }
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                if (TikTokChatReader.isMediaStream(url)) {
                                    return android.webkit.WebResourceResponse(
                                        "text/plain", "utf-8", 204, "No Content", emptyMap(),
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return null
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                view?.evaluateJavascript(
                                    "document.querySelectorAll('video,audio').forEach(function(v){try{v.muted=true;v.pause();}catch(e){}});",
                                    null
                                )
                            }
                        }
                        loadUrl("https://www.tiktok.com/@${LiveConfig.normalizeUser(user)}/live")
                        webView = this
                    }
                }
            )
        }
    }
}
