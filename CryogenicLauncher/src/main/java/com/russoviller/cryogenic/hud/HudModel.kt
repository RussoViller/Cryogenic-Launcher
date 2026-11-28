package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.JsonObject

/** Esquema de HUD escolhido pelo jogador. Cada esquema tem os próprios arquivos, editor e menu. */
enum class HudScheme(val id: String, val folder: String) {
    CRYOGENIC("cryogenic", "control_layouts"),
    FCL("fcl", "hud_fcl"),
    POJARUSH("pojarush", "hud_pojarush");

    companion object {
        fun of(id: String?): HudScheme = entries.firstOrNull { it.id == id || it.name == id } ?: CRYOGENIC
    }
}

/** Formato reconhecido de um arquivo de HUD. */
enum class HudFormat(val label: String) {
    CRYOGENIC("Cryogenic"),
    POJAV("Zalith 1 / Pojav / Mojo"),
    FCL("FCL"),
    POJARUSH("PojaRush"),
    UNKNOWN("?");

    /** Esquema que consegue usar esse formato (Pojav entra no Cryogenic convertido). */
    val scheme: HudScheme?
        get() = when (this) {
            CRYOGENIC, POJAV -> HudScheme.CRYOGENIC
            FCL -> HudScheme.FCL
            POJARUSH -> HudScheme.POJARUSH
            UNKNOWN -> null
        }
}

enum class HudVisibility { ALWAYS, IN_GAME, IN_MENU }

sealed interface HudAction {
    /** Tecla do teclado (código GLFW). */
    data class Key(val glfw: Int) : HudAction
    /** Botão do mouse (0 esquerdo, 1 direito, 2 meio). */
    data class Mouse(val button: Int) : HudAction
    /** Rolagem da roda do mouse. */
    data class Scroll(val up: Boolean, val repeat: Boolean = false) : HudAction
    /** Abre/fecha o teclado do Android. */
    data object Keyboard : HudAction
    /** Abre/fecha o menu do jogo do launcher. */
    data object Menu : HudAction
    /** Alterna o modo do mouse virtual (deslizar ⇄ clicar). */
    data object MouseMode : HudAction
    /** Esconde/mostra o HUD inteiro (fica só um botão para voltar). */
    data object HideHud : HudAction
    /** Envia um texto no chat. */
    data class Text(val text: String) : HudAction
    /** Mostra/esconde camadas (grupos). mode: 0 alterna, 1 mostra, 2 esconde. */
    data class Layer(val ids: List<String>, val mode: Int = 0) : HudAction

    /** Ações que ficam "seguradas" enquanto o botão está pressionado. */
    val isHoldable: Boolean get() = this is Key || this is Mouse
}

/** Tamanho em dp, ou fração da largura/altura da tela. */
data class HudSize(val dp: Float? = 50f, val fraction: Float = 0.1f, val ofWidth: Boolean = false) {
    fun px(screenW: Int, screenH: Int, density: Float): Float =
        dp?.let { it * density } ?: (fraction * if (ofWidth) screenW else screenH)

    companion object {
        fun ofPx(px: Float, like: HudSize, screenW: Int, screenH: Int, density: Float): HudSize =
            if (like.dp != null) like.copy(dp = (px / density).coerceIn(8f, 600f))
            else like.copy(fraction = (px / (if (like.ofWidth) screenW else screenH)).coerceIn(0.005f, 1f))
    }
}

/**
 * Gatilho de um botão.
 * - keep: liga/desliga a cada toque (fica travado), em vez de soltar ao tirar o dedo
 * - repeat: repete o clique enquanto ativo (spam)
 */
data class HudTrigger(
    val acts: List<HudAction> = emptyList(),
    val keep: Boolean = false,
    val repeat: Boolean = false
) {
    val isEmpty: Boolean get() = acts.isEmpty()
}

data class HudStyle(
    val fill: Int = 0x66000000,
    val fillPressed: Int = 0x99FFFFFF.toInt(),
    val text: Int = 0xFFFFFFFF.toInt(),
    val textPressed: Int = 0xFFFFFFFF.toInt(),
    val stroke: Int = 0xFF444444.toInt(),
    val strokePressed: Int = 0xFF444444.toInt(),
    val strokeDp: Float = 1f,
    val radiusDp: Float = 10f,
    /** O raio está em pixels e não em dp (opção do PojaRush). */
    val radiusPx: Boolean = false,
    val textSp: Float = 14f,
    val opacity: Float = 1f,
    /** Arredondamento em % do menor lado (formato do Cryogenic); null = usa radiusDp. */
    val radiusPercent: Float? = null,
    /** Imagem PNG (nome do arquivo na pasta de imagens do esquema). Vazio = sem imagem. */
    val image: String = "",
    /** Imagem quando pressionado (no joystick: o botão central). */
    val imagePressed: String = "",
    /** Nome do estilo compartilhado (FCL). */
    val name: String = "",
    /** Joystick: tamanho do botão central (fração do joystick). */
    val knobRatio: Float = 0.4f,
    /** Joystick: arredondamento do fundo como fração do tamanho (0.5 = círculo). */
    val radiusRatio: Float = 0.5f,
    /** Joystick: arredondamento do botão central como fração do tamanho dele (0.5 = círculo). */
    val knobRadiusRatio: Float = 0.5f,
    /** D-pad: espaço entre as setas como fração do tamanho. */
    val gapRatio: Float = 0.05f,
    val knobFill: Int = 0xFF888888.toInt(),
    val knobStroke: Int = 0xFF444444.toInt()
)

data class HudButton(
    val id: String,
    val text: String = "",
    val layer: String = "",
    /** Posição 0..1 dentro do espaço livre (tela − tamanho do botão). */
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val width: HudSize = HudSize(50f),
    val height: HudSize = HudSize(50f),
    val visibility: HudVisibility = HudVisibility.ALWAYS,
    /** Enquanto pressionado (FCL: Pressionar · PojaRush: Segurar). */
    val press: HudTrigger = HudTrigger(),
    /** Toque rápido (FCL: Clique · PojaRush: Toque). */
    val tap: HudTrigger = HudTrigger(),
    /** Segurar 400 ms (FCL). */
    val long: HudTrigger = HudTrigger(),
    /** Dois toques (FCL). */
    val double: HudTrigger = HudTrigger(),
    /** Liga/desliga (PojaRush). */
    val toggle: HudTrigger = HudTrigger(),
    /** Cliques por segundo do modo repetir. */
    val repeatCps: Float = 20f,
    /** Dá para deslizar o dedo de um botão para outro. */
    val swipe: Boolean = false,
    /** O dedo também mexe a câmera (penetrável / seguir ponteiro). */
    val look: Boolean = false,
    /** O botão pode ser arrastado durante o jogo (FCL). */
    val movable: Boolean = false,
    /** Recebe toques (PojaRush: botões só decorativos ficam desligados). */
    val interactive: Boolean = true,
    val style: HudStyle = HudStyle(),
    /** JSON original do botão, para manter campos que o Cryogenic não edita. */
    val raw: JsonObject? = null
) {
    val triggers: List<HudTrigger> get() = listOf(press, tap, long, double, toggle)
}

enum class HudStickMode { ROCKER, DPAD }
enum class HudStickFollow { FIXED, CENTER, FOLLOW }

data class HudStick(
    val id: String,
    val layer: String = "",
    val x: Float = 0.05f,
    val y: Float = 0.9f,
    val size: HudSize = HudSize(null, 0.45f, false),
    val visibility: HudVisibility = HudVisibility.IN_GAME,
    /** 8 direções: 0=L(E), 1=NE, 2=N, 3=NO, 4=O, 5=SO, 6=S, 7=SE. */
    val dirs: List<List<HudAction>> = defaultDirs(),
    val mode: HudStickMode = HudStickMode.ROCKER,
    val follow: HudStickFollow = HudStickFollow.CENTER,
    val canLock: Boolean = false,
    /** Distância extra (fração do raio) para travar a corrida. */
    val lockThreshold: Float = 0.3f,
    val lockActs: List<HudAction> = emptyList(),
    /** Zona morta (fração do raio). */
    val deadZone: Float = 0.1f,
    val doubleActs: List<HudAction> = emptyList(),
    /** Dois toques no centro liga/desliga (agachar do FCL). */
    val doubleKeep: Boolean = false,
    /** Corre sozinho ao empurrar para frente (PojaRush). */
    val autoRun: Boolean = false,
    val style: HudStyle = HudStyle(fill = 0, strokeDp = 2f, radiusDp = 500f),
    val raw: JsonObject? = null
) {
    companion object {
        fun defaultDirs(): List<List<HudAction>> {
            val w = HudAction.Key(87); val a = HudAction.Key(65); val s = HudAction.Key(83); val d = HudAction.Key(68)
            return listOf(listOf(d), listOf(w, d), listOf(w), listOf(w, a), listOf(a), listOf(s, a), listOf(s), listOf(s, d))
        }
    }
}

data class HudLayer(
    val id: String,
    val name: String = id,
    val visible: Boolean = true,
    val visibility: HudVisibility = HudVisibility.ALWAYS,
    val raw: JsonObject? = null
)

data class HudLayout(
    val scheme: HudScheme,
    val name: String,
    val author: String = "",
    val version: String = "",
    val description: String = "",
    val layers: List<HudLayer> = emptyList(),
    val buttons: List<HudButton> = emptyList(),
    val sticks: List<HudStick> = emptyList(),
    val raw: JsonObject? = null
) {
    fun layerName(id: String): String = layers.firstOrNull { it.id == id }?.name ?: id
}

/** Lê/grava um formato de HUD. */
interface HudCodec {
    val scheme: HudScheme
    fun parse(text: String, fallbackName: String): HudLayout
    fun write(layout: HudLayout): String
    fun empty(name: String, author: String): HudLayout
}

object HudCodecs {
    fun of(scheme: HudScheme): HudCodec? = when (scheme) {
        HudScheme.FCL -> FclCodec
        HudScheme.POJARUSH -> PojarushCodec
        HudScheme.CRYOGENIC -> Zl2Codec
    }
}

internal fun newHudId(): String = java.util.UUID.randomUUID().toString()
