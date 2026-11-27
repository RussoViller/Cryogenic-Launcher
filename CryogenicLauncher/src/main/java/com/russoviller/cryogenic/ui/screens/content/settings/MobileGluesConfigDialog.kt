package com.russoviller.cryogenic.ui.screens.content.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.game.renderer.config.ConfigEntryState
import com.russoviller.cryogenic.game.renderer.config.ConfigScalar
import com.russoviller.cryogenic.game.renderer.config.ConfigText
import com.russoviller.cryogenic.game.renderer.config.GlobalRendererConfigStore
import com.russoviller.cryogenic.game.renderer.config.NumberEntry
import com.russoviller.cryogenic.game.renderer.config.OrderedChoiceEntry
import com.russoviller.cryogenic.game.renderer.config.RendererConfigSession
import com.russoviller.cryogenic.game.renderer.config.SingleChoiceEntry
import com.russoviller.cryogenic.game.renderer.config.SwitchEntry
import com.russoviller.cryogenic.game.renderer.config.VersionRendererConfigStore
import com.russoviller.cryogenic.game.renderer.renderers.MobileGluesRenderer
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoIconOrb
import com.russoviller.cryogenic.ui.components.CryoNumberDialog
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/*
 * Painel exclusivo do Cryogenic para o MobileGlues embutido:
 * trilho lateral com os módulos (Núcleo, ANGLE, MultiDraw, Extensões, Imagem) e,
 * ao lado, os controles do módulo escolhido. Os valores passam pela mesma sessão de
 * configuração dos renderizadores (global ou por versão), então o launcher grava
 * exatamente as mesmas chaves no config.json do MobileGlues.
 */

private enum class MgModule(val iconRes: Int, val titleRes: Int, val subtitleRes: Int, val ids: List<String>) {
    Core(
        R.drawable.ic_bolt, R.string.cryo_mg_module_core, R.string.cryo_mg_module_core_sub,
        listOf("sfpewLayer", "MG_COUNT_LAUNCH", "customGLVersion", "maxGlslCacheSize")
    ),
    Angle(
        R.drawable.ic_cryo_layers, R.string.cryo_mg_module_angle, R.string.cryo_mg_module_angle_sub,
        listOf("enableANGLE", "angleDepthClearFixMode", "enableNoError")
    ),
    MultiDraw(
        R.drawable.ic_sort, R.string.cryo_mg_module_multidraw, R.string.cryo_mg_module_multidraw_sub,
        listOf(
            "multidrawOrder", "multidrawOrderArrays", "multidrawOrderElements",
            "multidrawOrderElementsBaseVertex", "multidrawOrderArraysIndirect", "multidrawOrderElementsIndirect"
        )
    ),
    Extensions(
        R.drawable.ic_extension_outlined, R.string.cryo_mg_module_ext, R.string.cryo_mg_module_ext_sub,
        listOf("enableExtTimerQuery", "enableExtComputeShader", "enableExtDirectStateAccess")
    ),
    Image(
        R.drawable.ic_image_outlined, R.string.cryo_mg_module_image, R.string.cryo_mg_module_image_sub,
        listOf("fsr1Setting")
    )
}

private val panelShape = RoundedCornerShape(26.dp)
private val cardShape = RoundedCornerShape(16.dp)

@Composable
fun MobileGluesConfigDialog(
    versionFolder: File? = null,
    onDismissRequest: () -> Unit
) {
    val renderer = MobileGluesRenderer
    val rendererId = renderer.getUniqueIdentifier()
    val sessionState = produceState<RendererConfigSession?>(null, versionFolder) {
        value = withContext(Dispatchers.IO) {
            GlobalRendererConfigStore.migrateLegacy(rendererId, renderer.legacyConfigKeys)
            val versionStore = versionFolder?.let {
                VersionRendererConfigStore(it, Renderers.getLegacyRendererIds())
            }
            RendererConfigSession(
                rendererId = rendererId,
                spec = renderer.configSpec,
                localStore = versionStore ?: GlobalRendererConfigStore
            )
        }
    }
    val session = sessionState.value ?: return
    var showResetDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        MobileGluesConfigContent(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            session = session,
            perVersion = versionFolder != null,
            onReset = { showResetDialog = true },
            onClose = onDismissRequest
        )
    }

    if (showResetDialog) {
        SimpleAlertDialog(
            title = stringResource(R.string.generic_reset),
            text = stringResource(
                if (versionFolder != null) R.string.versions_config_renderer_reset_global_message
                else R.string.settings_renderer_config_reset_message,
                renderer.getRendererName()
            ),
            onConfirm = {
                session.resetAll()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

/** Conteúdo do painel (separado da janela para poder ser renderizado em prévias). */
@Composable
internal fun MobileGluesConfigContent(
    session: RendererConfigSession,
    perVersion: Boolean,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialModule: Int = 0
) {
    val states = remember(session) { session.states.associateBy { it.entry.id } }
    var module by rememberSaveable { mutableIntStateOf(initialModule) }

    run {
        Row(
            modifier = modifier
                .cryoGlass(shape = panelShape, cornerRadius = 26.dp, window = true)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //trilho dos módulos
            Column(
                modifier = Modifier
                    .width(196.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MgEmblem(perVersion = perVersion)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MgModule.entries.forEachIndexed { index, item ->
                        val changed = item.ids.count { states[it]?.isOverridden == true }
                        MgModuleTab(
                            module = item,
                            selected = index == module,
                            changed = changed,
                            onClick = { module = index }
                        )
                    }
                }
            }

            //conteúdo do módulo
            val current = MgModule.entries[module]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.35f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            ) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "mgModule"
                ) { target ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MgModuleHeader(
                            module = target,
                            perVersion = perVersion,
                            resetEnabled = session.hasOverrides,
                            onReset = onReset,
                            onClose = onClose
                        )
                        when (target) {
                            MgModule.Core -> {
                                states["sfpewLayer"]?.let { MgSwitchCard(it, R.drawable.ic_cryo_shader) }
                                states["MG_COUNT_LAUNCH"]?.let { MgSwitchCard(it, R.drawable.ic_list_alt_check_outlined) }
                                states["customGLVersion"]?.let { MgGlVersionCard(it) }
                                states["maxGlslCacheSize"]?.let { MgCacheCard(it) }
                            }
                            MgModule.Angle -> {
                                states["enableANGLE"]?.let { MgChoiceCard(it, columns = 2) }
                                states["angleDepthClearFixMode"]?.let { MgSwitchCard(it, R.drawable.ic_cryo_layers) }
                                states["enableNoError"]?.let { MgChoiceCard(it, columns = 2, levels = true) }
                            }
                            MgModule.MultiDraw -> {
                                states["multidrawOrder"]?.let { MgOrderCard(it) }
                                MgSubHeader(stringResource(R.string.cryo_mg_exceptions_title))
                                target.ids.drop(1).forEach { id ->
                                    states[id]?.let { MgExceptionCard(it) }
                                }
                            }
                            MgModule.Extensions -> {
                                states["enableExtTimerQuery"]?.let { MgSwitchCard(it, R.drawable.ic_memory) }
                                states["enableExtComputeShader"]?.let { MgSwitchCard(it, R.drawable.ic_gpu) }
                                states["enableExtDirectStateAccess"]?.let { MgSwitchCard(it, R.drawable.ic_code) }
                            }
                            MgModule.Image -> {
                                states["fsr1Setting"]?.let { MgChoiceCard(it, columns = 3, levels = true) }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun MgEmblem(perVersion: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = true)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CryoIconOrb(R.drawable.ic_gpu, size = 32)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MobileGlues",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "GL 4.x › GLES 3",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null),
                    color = CryoGlassColors.Ice
                )
            }
        }
        CryoTag(
            text = stringResource(if (perVersion) R.string.cryo_mg_scope_version else R.string.cryo_mg_scope_global),
            accent = if (perVersion) CryoGlassColors.Aurora else CryoGlassColors.Ice
        )
    }
}

@Composable
private fun MgModuleTab(module: MgModule, selected: Boolean, changed: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = selected)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CryoIconOrb(module.iconRes, size = 28, active = selected)
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(module.titleRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) Color.White else CryoGlassColors.Frost,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (changed > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(CryoGlassColors.Aurora),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = changed.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(shadow = null),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep
                )
            }
        }
    }
}

@Composable
private fun MgModuleHeader(
    module: MgModule,
    perVersion: Boolean,
    resetEnabled: Boolean,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(module.titleRes).uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp, shadow = null),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Ice
                )
                Text(
                    text = stringResource(module.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                )
            }
            CryoChipButton(
                text = stringResource(
                    if (perVersion) R.string.versions_config_renderer_reset_global else R.string.generic_reset
                ),
                iconRes = R.drawable.ic_refresh,
                enabled = resetEnabled,
                onClick = onReset
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient(), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_close),
                    tint = CryoGlassColors.Deep
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora.copy(alpha = 0.4f), Color.Transparent)))
        )
    }
}

@Composable
private fun MgSubHeader(text: String) {
    Text(
        modifier = Modifier.padding(top = 6.dp, start = 2.dp),
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, shadow = null),
        fontWeight = FontWeight.Bold,
        color = CryoGlassColors.Aurora
    )
}

@Composable
private fun ConfigText.text(): String = when (this) {
    is ConfigText.Resource -> stringResource(resId)
    is ConfigText.Dynamic -> value
}

/** Cabeçalho comum dos cartões: título, descrição e marca de "alterado" com botão de desfazer. */
@Composable
private fun MgCardTitle(state: ConfigEntryState, monospace: Boolean = false, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = state.entry.title?.text().orEmpty(),
                    style = if (monospace) {
                        MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace)
                    } else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.isOverridden) {
                    CryoTag(text = stringResource(R.string.cryo_mg_changed), accent = CryoGlassColors.Aurora)
                }
            }
            state.entry.description?.let {
                Text(
                    text = it.text(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
        }
        if (state.isOverridden) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { state.reset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.generic_reset),
                    tint = CryoGlassColors.Ice
                )
            }
        }
        trailing()
    }
}

@Composable
private fun MgSwitchCard(state: ConfigEntryState, iconRes: Int) {
    val entry = state.entry as? SwitchEntry ?: return
    val checked = (state.value as? ConfigScalar.BooleanValue)?.value ?: entry.default
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = checked)
            .clickable { state.saveValue(ConfigScalar.BooleanValue(!checked)) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CryoIconOrb(iconRes, size = 36, active = checked)
        Box(modifier = Modifier.weight(1f)) {
            MgCardTitle(state)
        }
        DefaultSwitch(
            checked = checked,
            onCheckedChange = { state.saveValue(ConfigScalar.BooleanValue(it)) }
        )
    }
}

/** Opções lado a lado em blocos; [levels] desenha uma barrinha de intensidade em cada bloco. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MgChoiceCard(state: ConfigEntryState, columns: Int, levels: Boolean = false) {
    val entry = state.entry as? SingleChoiceEntry ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MgCardTitle(state)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entry.options.forEachIndexed { index, option ->
                val selected = option.value.asString() == state.value.asString()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(cardShape)
                        .then(
                            if (selected) Modifier.cryoBackground(cryoGradient(), cardShape)
                            else Modifier
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.22f), cardShape)
                        )
                        .clickable { state.saveValue(option.value) }
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                ) {
                    if (levels) {
                        MgLevelBar(level = index, max = entry.options.lastIndex, selected = selected)
                    }
                    Text(
                        text = option.label?.text() ?: option.value.asString(),
                        style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                        color = if (selected) CryoGlassColors.Deep else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MgLevelBar(level: Int, max: Int, selected: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(max.coerceAtLeast(1)) { i ->
            val on = i < level
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selected && on -> CryoGlassColors.Deep
                            selected -> CryoGlassColors.Deep.copy(alpha = 0.25f)
                            on -> CryoGlassColors.Ice
                            else -> Color.White.copy(alpha = 0.15f)
                        }
                    )
            )
        }
    }
}

/** Versão do OpenGL informada ao jogo: fileira de "teclas" com o número da versão. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MgGlVersionCard(state: ConfigEntryState) {
    val entry = state.entry as? SingleChoiceEntry ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MgCardTitle(state)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entry.options.forEach { option ->
                val raw = (option.value as? ConfigScalar.IntValue)?.value ?: 0
                val selected = option.value.asString() == state.value.asString()
                val label = if (raw == 0) option.label?.text().orEmpty() else "${raw / 10}.${raw % 10}"
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .widthIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected) Modifier.cryoBackground(cryoGradient(), RoundedCornerShape(12.dp))
                            else Modifier
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        )
                        .clickable { state.saveValue(option.value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = null),
                        fontWeight = FontWeight.Black,
                        color = if (selected) CryoGlassColors.Deep else Color.White
                    )
                }
            }
        }
    }
}

private val cachePresets = listOf(16, 32, 64, 128, 256, 512)

/** Limite do cache de shaders: visor com o valor, atalhos e campo livre. Vazio = cache desligado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MgCacheCard(state: ConfigEntryState) {
    val entry = state.entry as? NumberEntry ?: return
    val current = state.value
    val disabled = (current as? ConfigScalar.StringValue)?.value?.isEmpty() == true
    val number = (current as? ConfigScalar.IntValue)?.value
    var showInput by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = !disabled)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MgCardTitle(state) {
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (disabled) Modifier.background(Color.White.copy(alpha = 0.08f))
                        else Modifier.cryoBackground(cryoGradient(), CircleShape)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                text = if (disabled) stringResource(R.string.cryo_mg_off) else "${number ?: current.asString()} MiB",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = null),
                fontWeight = FontWeight.Black,
                color = if (disabled) CryoGlassColors.Frost else CryoGlassColors.Deep
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MgPill(text = stringResource(R.string.cryo_mg_off), selected = disabled) {
                state.saveValue(ConfigScalar.StringValue(""))
            }
            cachePresets.forEach { preset ->
                MgPill(text = "$preset", selected = !disabled && number == preset) {
                    state.saveValue(ConfigScalar.IntValue(preset))
                }
            }
            MgPill(
                text = stringResource(R.string.cryo_mg_cache_custom),
                selected = !disabled && number != null && number !in cachePresets
            ) { showInput = true }
        }
    }
    if (showInput) {
        CryoNumberDialog(
            title = entry.title?.text().orEmpty(),
            valueRange = 0f..65536f,
            value = (number ?: 32).toFloat().coerceIn(0f, 65536f),
            isInt = true,
            onDismissRequest = { showInput = false },
            onConfirm = { state.saveValue(ConfigScalar.IntValue(it.toInt())) }
        )
    }
}

@Composable
private fun MgPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient(), CircleShape)
                else Modifier
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, shadow = null),
            fontWeight = FontWeight.Bold,
            color = if (selected) CryoGlassColors.Deep else Color.White
        )
    }
}

/** Ordem principal das estratégias do MultiDraw. */
@Composable
private fun MgOrderCard(state: ConfigEntryState) {
    val entry = state.entry as? OrderedChoiceEntry ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = true)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MgCardTitle(state)
        MgOrderList(entry, state.value.asString()) { state.saveValue(ConfigScalar.StringValue(it)) }
    }
}

/** Exceção de uma função de desenho: desligada segue a ordem principal; ligada usa a própria ordem. */
@Composable
private fun MgExceptionCard(state: ConfigEntryState) {
    val entry = state.entry as? OrderedChoiceEntry ?: return
    val enabled = state.enabled == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = enabled)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MgCardTitle(state, monospace = true) {
            DefaultSwitch(checked = enabled, onCheckedChange = { state.saveEnabled(it) })
        }
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
        ) {
            MgOrderList(entry, state.value.asString()) { state.saveValue(ConfigScalar.StringValue(it)) }
        }
        if (!enabled) {
            val labels = entry.options.associate { it.value.asString() to (it.label?.text() ?: it.value.asString()) }
            Text(
                modifier = Modifier.alpha(0.8f),
                text = stringResource(R.string.cryo_mg_exception_follow) + "  " +
                        state.value.asString().split(',').joinToString("  ›  ") { labels[it] ?: it },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null),
                color = CryoGlassColors.Frost
            )
        }
    }
}

/** Lista ordenável por toques: o número indica a prioridade; as setas trocam a posição. */
@Composable
private fun MgOrderList(entry: OrderedChoiceEntry, value: String, onChange: (String) -> Unit) {
    val order = value.split(',').filter { it.isNotEmpty() }
    val options = entry.options.associateBy { it.value.asString() }
    fun move(from: Int, to: Int) {
        if (to !in order.indices) return
        val list = order.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        onChange(list.joinToString(","))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        order.forEachIndexed { index, key ->
            val option = options[key]
            val first = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (first) CryoGlassColors.Ice.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .then(
                            if (first) Modifier.cryoBackground(cryoGradient(), CircleShape)
                            else Modifier.border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, shadow = null),
                        fontWeight = FontWeight.Black,
                        color = if (first) CryoGlassColors.Deep else CryoGlassColors.Ice
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option?.label?.text() ?: key,
                        style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                        fontWeight = if (first) FontWeight.Black else FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    option?.description?.let {
                        Text(
                            text = it.text(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                        )
                    }
                }
                MgArrow(R.drawable.ic_keyboard_arrow_up, enabled = index > 0) { move(index, index - 1) }
                MgArrow(R.drawable.ic_keyboard_arrow_down, enabled = index < order.lastIndex) { move(index, index + 1) }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun MgArrow(iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
    }
}
