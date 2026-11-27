package com.russoviller.cryogenic.ui.screens.game

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import java.util.concurrent.CountDownLatch

/**
 * Substitui a caixa "No supported graphics backend was found" do Minecraft 26.x
 * por uma tela do Cryogenic que explica cada falha (OpenGL / Vulkan) e o que fazer.
 */
object CryoGraphicsError {
    /** Erro aberto agora (null = nenhum). */
    var pending by mutableStateOf<Pending?>(null)
        private set

    /** Versão que está rodando (preenchido ao abrir o jogo). */
    var gameVersion: String? = null

    class Pending(val message: String, val buttonId: Int, internal val latch: CountDownLatch)

    fun matches(message: String?): Boolean =
        message != null && message.contains("No supported graphics backend", ignoreCase = true)

    /** Mostra a tela e bloqueia a thread do jogo até a pessoa fechar. Retorna o botão do Minecraft (OK). */
    fun showAndWait(message: String, buttonId: Int): Int {
        val latch = CountDownLatch(1)
        pending = Pending(message, buttonId, latch)
        try {
            latch.await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return buttonId
    }

    /** Só para prévias/testes: abre a tela sem bloquear. */
    internal fun showForPreview(message: String) {
        pending = Pending(message, 0, CountDownLatch(1))
    }

    internal fun close() {
        val p = pending ?: return
        pending = null
        p.latch.countDown()
    }

    enum class Kind {
        GL_MISMATCH, GL_GENERIC,
        VK_CAPS, VK_LAYER, VK_DRIVER, VK_MISSING, VK_NO_DEVICE, VK_BLOCKED, VK_LOADER, GENERIC
    }

    data class Attempt(val backend: String, val raw: String, val kind: Kind)

    /** Lê os blocos "- Tried OpenGL:\n  motivo" da mensagem do Minecraft. */
    fun parse(message: String): List<Attempt> {
        val lines = message.lines()
        val result = mutableListOf<Attempt>()
        var i = 0
        while (i < lines.size) {
            val m = Regex("^\\s*-\\s*Tried\\s+(.+?):\\s*(.*)$").find(lines[i])
            if (m != null) {
                val backend = m.groupValues[1].trim()
                val reason = StringBuilder(m.groupValues[2].trim())
                var j = i + 1
                while (j < lines.size && lines[j].isNotBlank() && !lines[j].trimStart().startsWith("- Tried")) {
                    if (reason.isNotEmpty()) reason.append('\n')
                    reason.append(lines[j].trim())
                    j++
                }
                val raw = reason.toString()
                result += Attempt(backend, raw, classify(backend, raw))
                i = j
            } else i++
        }
        return result
    }

    private fun classify(backend: String, raw: String): Kind {
        val r = raw.lowercase()
        val vulkan = backend.contains("vulkan", true)
        return when {
            r.contains("glgeterror mismatch") -> Kind.GL_MISMATCH
            r.contains("vk_error_layer_not_present") -> Kind.VK_LAYER
            r.contains("missing capabilities") || r.contains("does not support required") -> Kind.VK_CAPS
            r.contains("vk_error_incompatible_driver") -> Kind.VK_DRIVER
            r.contains("vulkan loader library is missing") -> Kind.VK_MISSING
            r.contains("no vulkan capable devices") -> Kind.VK_NO_DEVICE
            r.contains("known problematic") -> Kind.VK_BLOCKED
            r.contains("vkgetinstanceprocaddr mismatch") -> Kind.VK_LOADER
            !vulkan && backend.contains("opengl", true) -> Kind.GL_GENERIC
            else -> Kind.GENERIC
        }
    }
}

private val Danger = Color(0xFFFF5C7A)

@Composable
fun CryoGraphicsErrorOverlay(modifier: Modifier = Modifier) {
    val pending = CryoGraphicsError.pending
    AnimatedVisibility(visible = pending != null, enter = fadeIn(), exit = fadeOut()) {
        val p = pending ?: return@AnimatedVisibility
        GraphicsErrorContent(modifier = modifier, pending = p)
    }
}

@Composable
private fun GraphicsErrorContent(modifier: Modifier, pending: CryoGraphicsError.Pending) {
    val context = LocalContext.current
    val attempts = remember(pending) { CryoGraphicsError.parse(pending.message) }
    val rendererName = remember {
        runCatching { if (Renderers.isCurrentRendererValid()) Renderers.getCurrentRenderer().getRendererName() else null }.getOrNull()
    }
    var showRaw by remember { mutableStateOf(false) }
    val ice = CryoGlassColors.Ice

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(indication = null, interactionSource = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .cryoGlass(shape = RoundedCornerShape(28.dp), cornerRadius = 28.dp, window = true)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //Cabeçalho
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Danger, CryoGlassColors.Aurora))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        painter = painterResource(R.drawable.ic_warning_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_gfx_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.cryo_gfx_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                    )
                }
            }
            //Versão e renderizador
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoGraphicsError.gameVersion?.let { InfoPill(stringResource(R.string.cryo_gfx_version, it)) }
                rendererName?.let { InfoPill(stringResource(R.string.cryo_gfx_renderer, it)) }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                //Uma caixa por modo gráfico que o jogo tentou
                attempts.forEach { AttemptCard(it) }
                if (attempts.isEmpty()) {
                    AttemptCard(CryoGraphicsError.Attempt("Minecraft", pending.message.trim(), CryoGraphicsError.Kind.GENERIC))
                }

                //O que fazer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ice.copy(alpha = 0.08f))
                        .border(1.dp, ice.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cryo_gfx_what_to_do),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = ice
                    )
                    var n = 1
                    Tip(n++, stringResource(R.string.cryo_gfx_tip_renderer))
                    if (attempts.any { it.kind == CryoGraphicsError.Kind.VK_LAYER }) {
                        Tip(n++, stringResource(R.string.cryo_gfx_tip_layer))
                    }
                    Tip(n, stringResource(R.string.cryo_gfx_tip_older))
                }

                //Mensagem original (para quem for pedir ajuda)
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showRaw = !showRaw }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    text = (if (showRaw) "▾ " else "▸ ") + stringResource(R.string.cryo_gfx_details),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Frost
                )
                if (showRaw) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(10.dp),
                        text = pending.message.trim(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val copied = stringResource(R.string.cryo_gfx_copied)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .border(1.5.dp, ice.copy(alpha = 0.7f), CircleShape)
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Minecraft", pending.message))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.cryo_gfx_copy), color = ice, fontWeight = FontWeight.Black)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .cryoBackground(cryoGradient())
                        .clickable { CryoGraphicsError.close() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.cryo_gfx_close), color = CryoGlassColors.Deep, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1
    )
}

@Composable
private fun AttemptCard(attempt: CryoGraphicsError.Attempt) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
            .border(1.dp, Danger.copy(alpha = 0.55f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Danger.copy(alpha = 0.16f))
                    .border(1.dp, Danger, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                text = attempt.backend,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Danger
            )
            Text(
                modifier = Modifier.weight(1f),
                text = attempt.raw,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                maxLines = 2
            )
        }
        Text(
            text = stringResource(
                when (attempt.kind) {
                    CryoGraphicsError.Kind.GL_MISMATCH -> R.string.cryo_gfx_reason_gl_mismatch
                    CryoGraphicsError.Kind.GL_GENERIC -> R.string.cryo_gfx_reason_gl_generic
                    CryoGraphicsError.Kind.VK_CAPS -> R.string.cryo_gfx_reason_vk_caps
                    CryoGraphicsError.Kind.VK_LAYER -> R.string.cryo_gfx_reason_vk_layer
                    CryoGraphicsError.Kind.VK_DRIVER -> R.string.cryo_gfx_reason_vk_driver
                    CryoGraphicsError.Kind.VK_MISSING -> R.string.cryo_gfx_reason_vk_missing
                    CryoGraphicsError.Kind.VK_NO_DEVICE -> R.string.cryo_gfx_reason_vk_no_device
                    CryoGraphicsError.Kind.VK_BLOCKED -> R.string.cryo_gfx_reason_vk_blocked
                    CryoGraphicsError.Kind.VK_LOADER -> R.string.cryo_gfx_reason_vk_loader
                    CryoGraphicsError.Kind.GENERIC -> R.string.cryo_gfx_reason_generic
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun Tip(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = CryoGlassColors.Deep)
        }
        Text(modifier = Modifier.weight(1f), text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
