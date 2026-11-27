package com.russoviller.cryogenic.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

/*
 * Liquid Glass de verdade (Android 13+): o fundo do launcher é gravado numa camada e cada
 * painel de vidro desenha essa camada passando por um shader AGSL na GPU, que entorta o
 * fundo perto das bordas (lente), separa um pouco as cores (dispersão) e acende a borda
 * onde a "luz" bate. No Android 12 ou menos fica o vidro desenhado de antes.
 */

/** Só para testes: adapta o texto dos shaders ao compilador do ambiente de teste. */
internal var agslSource: (String) -> String = { it }

/** Fundo do launcher gravado para os vidros refratarem. */
object CryoBackdrop {
    /** Shaders AGSL (RuntimeShader) existem a partir do Android 13. */
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var layer by mutableStateOf<GraphicsLayer?>(null)
        private set

    /** Canto superior esquerdo do fundo na tela. */
    var origin by mutableStateOf(Offset.Zero)
        private set

    private var owner: Any? = null

    internal fun publish(owner: Any, layer: GraphicsLayer, origin: Offset) {
        this.owner = owner
        if (this.layer !== layer) this.layer = layer
        if (this.origin != origin) this.origin = origin
    }

    internal fun withdraw(owner: Any) {
        if (this.owner !== owner) return
        this.owner = null
        layer = null
    }

    /** Vidro líquido real ativo agora (Liquid Glass ligado, Android 13+ e fundo gravado). */
    val active: Boolean get() = supported && CryoLiquid.on && layer != null
}

/** No fundo do launcher (imagem, vídeo ou wallpaper animado): grava o que é desenhado para os vidros. */
fun Modifier.cryoBackdropSource(enabled: Boolean = true): Modifier =
    if (!CryoBackdrop.supported || !enabled) this else this then BackdropSourceElement

private data object BackdropSourceElement : ModifierNodeElement<BackdropSourceNode>() {
    override fun create() = BackdropSourceNode()
    override fun update(node: BackdropSourceNode) = Unit
}

private class BackdropSourceNode : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var context: GraphicsContext? = null
    private var layer: GraphicsLayer? = null
    private var origin = Offset.Zero

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionOnScreen()
        if (position.isSpecified && position != origin) {
            origin = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        //leitura do estado aqui: ligar/desligar o Liquid Glass redesenha este nó
        if (!CryoLiquid.on) {
            CryoBackdrop.withdraw(this@BackdropSourceNode)
            drawContent()
            return
        }
        val target = layer ?: currentValueOf(LocalGraphicsContext).let { graphics ->
            context = graphics
            graphics.createGraphicsLayer().also { layer = it }
        }
        target.record { this@draw.drawContent() }
        drawLayer(target)
        CryoBackdrop.publish(this@BackdropSourceNode, target, origin)
    }

    override fun onDetach() {
        CryoBackdrop.withdraw(this)
        layer?.let { context?.releaseGraphicsLayer(it) }
        layer = null
        context = null
    }
}

private object LensTraverseKey

/** Lente de vidro: desenha o fundo refratado dentro do elemento (usar depois do clip da forma). */
internal fun Modifier.cryoLens(cornerRadius: Dp): Modifier =
    if (!CryoBackdrop.supported) this else this then LensElement(cornerRadius)

private data class LensElement(val cornerRadius: Dp) : ModifierNodeElement<LensNode>() {
    override fun create() = LensNode(cornerRadius)
    override fun update(node: LensNode) {
        if (node.cornerRadius != cornerRadius) {
            node.cornerRadius = cornerRadius
            node.dirty = true
            node.invalidateDraw()
        }
    }
}

private class LensNode(var cornerRadius: Dp) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode, TraversableNode {
    override val traverseKey: Any get() = LensTraverseKey
    /** Vidro dentro de outro vidro: o de fora já refrata o fundo, o de dentro não repete o efeito. */
    private var nested = false

    override fun onAttach() {
        nested = findNearestAncestor(this) != null
    }

    private var context: GraphicsContext? = null
    private var lens: GraphicsLayer? = null
    private var shader: Any? = null
    private var effectSize = IntSize.Zero
    var dirty = true
    private var origin = Offset.Zero

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionOnScreen()
        if (position.isSpecified && position != origin) {
            origin = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val backdrop = CryoBackdrop.layer
        val from = CryoBackdrop.origin
        if (nested || backdrop == null || !CryoLiquid.on || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            drawContent()
            return
        }
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w <= 0 || h <= 0) {
            drawContent()
            return
        }
        val target = lens ?: currentValueOf(LocalGraphicsContext).let { graphics ->
            context = graphics
            graphics.createGraphicsLayer().also { lens = it }
        }
        val current = IntSize(w, h)
        if (dirty || current != effectSize) {
            applyEffect(target, current)
            effectSize = current
            dirty = false
        }
        target.record(current) {
            translate(from.x - origin.x, from.y - origin.y) { drawLayer(backdrop) }
        }
        drawLayer(target)
        drawContent()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun ContentDrawScope.applyEffect(target: GraphicsLayer, size: IntSize) {
        val runtime = (shader as? RuntimeShader) ?: RuntimeShader(agslSource(LENS_SHADER)).also { shader = it }
        val minSide = min(size.width, size.height).toFloat()
        val radius = cornerRadius.toPx().coerceAtMost(minSide / 2f)
        //largura da borda que entorta o fundo e quanto ela puxa
        val edge = min(24.dp.toPx(), minSide * 0.34f).coerceAtLeast(2f)
        runtime.setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
        runtime.setFloatUniform("radius", radius)
        runtime.setFloatUniform("edge", edge)
        runtime.setFloatUniform("strength", edge * 0.7f)
        runtime.setFloatUniform("dispersion", 0.12f)
        val lensEffect = RenderEffect.createRuntimeShaderEffect(runtime, "content")
        //desfoque leve antes da lente: vidro limpo, mas sem o fundo competir com o texto
        val frost = 3.dp.toPx()
        val blur = RenderEffect.createBlurEffect(frost, frost, Shader.TileMode.CLAMP)
        target.renderEffect = RenderEffect.createChainEffect(lensEffect, blur).asComposeRenderEffect()
    }

    override fun onDetach() {
        lens?.let { context?.releaseGraphicsLayer(it) }
        lens = null
        context = null
        dirty = true
    }
}

/**
 * Lente: SDF de retângulo arredondado. Perto da borda o fundo é puxado para dentro seguindo a
 * normal (perfil de lente circular), com as cores separadas de leve; a borda virada para a luz
 * (em cima à esquerda) ganha um brilho, e a de baixo um reflexo mais fraco.
 */
internal const val LENS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float edge;
uniform float strength;
uniform float dispersion;

half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 rel = p - c;
    float2 q = abs(rel) - (c - float2(radius));
    float outside = length(max(q, float2(0.0)));
    float d = outside + min(max(q.x, q.y), 0.0) - radius;
    float inside = -d;
    float t = clamp(1.0 - inside / edge, 0.0, 1.0);

    float2 s = float2(rel.x >= 0.0 ? 1.0 : -1.0, rel.y >= 0.0 ? 1.0 : -1.0);
    float2 n;
    if (q.x > 0.0 && q.y > 0.0) {
        n = normalize(q) * s;
    } else if (q.x > q.y) {
        n = float2(s.x, 0.0);
    } else {
        n = float2(0.0, s.y);
    }

    float bend = 1.0 - sqrt(max(1.0 - t * t, 0.0));
    float2 off = -n * strength * bend;

    half4 g = content.eval(p + off);
    half4 r = content.eval(p + off * (1.0 + dispersion));
    half4 b = content.eval(p + off * (1.0 - dispersion));
    half4 col = half4(r.r, g.g, b.b, g.a);

    float rim = smoothstep(0.55, 1.0, t);
    float lit = clamp(dot(n, normalize(float2(-0.6, -1.0))), 0.0, 1.0);
    float back = clamp(dot(n, normalize(float2(0.6, 1.0))), 0.0, 1.0);
    half glow = half(rim * (lit * lit * 0.42 + back * back * 0.16));
    col.rgb = col.rgb + half3(glow) * col.a + half3(glow * 0.35);
    col.a = max(col.a, glow);
    return col;
}
"""

// ------------------------------------------------------------------ onda da cutscene

/**
 * Onda de choque da cutscene sobre a interface (Android 13+): um anel que se expande a partir do
 * centro e entorta a interface por onde passa, com as cores separadas.
 */
@androidx.compose.runtime.Composable
fun Modifier.cryoShockwave(radiusFraction: () -> Float, amplitude: () -> Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val shader = androidx.compose.runtime.remember { RuntimeShader(agslSource(SHOCKWAVE_SHADER)) }
    return this.graphicsLayer {
        val a = amplitude()
        if (a <= 0.01f || size.minDimension <= 0f) {
            renderEffect = null
            return@graphicsLayer
        }
        shader.setFloatUniform("uCenter", size.width / 2f, size.height / 2f)
        shader.setFloatUniform("uRadius", size.maxDimension * radiusFraction())
        shader.setFloatUniform("uWidth", 46.dp.toPx())
        shader.setFloatUniform("uAmp", a * 14.dp.toPx())
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

internal const val SHOCKWAVE_SHADER = """
uniform shader content;
uniform float2 uCenter;
uniform float uRadius;
uniform float uWidth;
uniform float uAmp;

half4 main(float2 p) {
    float2 d = p - uCenter;
    float dist = length(d);
    float x = (dist - uRadius) / uWidth;
    float w = exp(-x * x * 2.5);
    float2 n = dist > 0.001 ? d / dist : float2(0.0);
    float off = sin(x * 3.14159) * w * uAmp;
    half4 g = content.eval(p - n * off);
    half4 r = content.eval(p - n * off * 1.12);
    half4 b = content.eval(p - n * off * 0.88);
    half4 col = half4(r.r, g.g, b.b, g.a);
    col.rgb = col.rgb + half3(0.6, 0.85, 1.0) * half(w * 0.18) * col.a;
    return col;
}
"""
