package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.game.renderer.config.ConfigScalar
import com.russoviller.cryogenic.game.renderer.config.GlobalRendererConfigStore
import com.russoviller.cryogenic.game.renderer.config.RendererConfigSession
import com.russoviller.cryogenic.game.renderer.config.resolveRendererConfig
import com.russoviller.cryogenic.game.renderer.renderers.MobileGluesRenderer
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.ui.screens.content.settings.MobileGluesConfigContent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h370dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R166PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private val id = MobileGluesRenderer.getUniqueIdentifier()

    @Before
    fun clear() = ShadowMMKV.clearAll()

    /** O config.json do MobileGlues recebe as mesmas chaves e valores do Zalith. */
    @Test
    fun mgConfigJson() {
        val dir = Files.createTempDirectory("mg").toFile()
        PathManager.DIR_FILES_PRIVATE = dir
        val mgDir = File(dir, "mobileglues").apply { mkdirs() }
        //chave do próprio usuário deve sobreviver
        File(mgDir, "config.json").writeText("""{"userKey":7}""")

        val spec = MobileGluesRenderer.configSpec
        var config = resolveRendererConfig(id, spec)
        var env = MobileGluesRenderer.getConfiguredEnv(config)
        assertEquals("3", env["LIBGL_ES"])
        assertEquals("libmobileglues.so", env["LIBGL_EGL"])
        assertEquals("1", env["MG_COUNT_LAUNCH"])
        assertEquals(mgDir.absolutePath, env["MG_DIR_PATH"])
        assertEquals(true, config.sfpewLayerEnabled)
        var json = JSONObject(File(mgDir, "config.json").readText())
        assertEquals(7, json.getInt("userKey"))
        assertEquals(0, json.getInt("enableANGLE"))
        assertEquals(0, json.getInt("enableNoError"))
        assertEquals("native,multiindirect,multibasevertex,multiarrays,indirect,basevertex,unroll,compute", json.getString("multidrawOrder"))
        assertFalse(json.has("multidrawOrderArrays"))
        assertEquals(0, json.getInt("angleDepthClearFixMode"))
        assertEquals(0, json.getInt("customGLVersion"))
        assertEquals(32, json.getInt("maxGlslCacheSize"))
        assertEquals(1, json.getInt("enableExtTimerQuery"))
        assertEquals(0, json.getInt("enableExtComputeShader"))
        assertEquals(0, json.getInt("enableExtDirectStateAccess"))
        assertEquals(0, json.getInt("fsr1Setting"))
        assertFalse(json.has("sfpewLayer"))
        assertFalse(json.has("MG_COUNT_LAUNCH"))

        //mudanças pelo painel (sessão)
        val session = RendererConfigSession(id, spec, GlobalRendererConfigStore)
        val st = session.states.associateBy { it.entry.id }
        st.getValue("sfpewLayer").saveValue(ConfigScalar.BooleanValue(false))
        st.getValue("MG_COUNT_LAUNCH").saveValue(ConfigScalar.BooleanValue(false))
        st.getValue("enableANGLE").saveValue(ConfigScalar.IntValue(3))
        st.getValue("multidrawOrder").saveValue(ConfigScalar.StringValue("unroll,native,compute"))
        //exceção herdada: "native" vira o valor nativo da função
        st.getValue("multidrawOrderArrays").saveEnabled(true)
        assertEquals("unroll,multiarrays,multiindirect", st.getValue("multidrawOrderArrays").value.asString())
        st.getValue("maxGlslCacheSize").saveValue(ConfigScalar.StringValue(""))
        st.getValue("customGLVersion").saveValue(ConfigScalar.IntValue(46))

        config = resolveRendererConfig(id, spec)
        env = MobileGluesRenderer.getConfiguredEnv(config)
        assertEquals(false, config.sfpewLayerEnabled)
        assertFalse(env.containsKey("MG_COUNT_LAUNCH"))
        json = JSONObject(File(mgDir, "config.json").readText())
        assertEquals(7, json.getInt("userKey"))
        assertEquals(3, json.getInt("enableANGLE"))
        assertEquals("unroll,native,compute,multiindirect,multibasevertex,multiarrays,indirect,basevertex", json.getString("multidrawOrder"))
        assertEquals("unroll,multiarrays,multiindirect", json.getString("multidrawOrderArrays"))
        assertEquals(-1, json.getInt("maxGlslCacheSize"))
        assertEquals(46, json.getInt("customGLVersion"))
        assertTrue(File(mgDir, "launcher-keys").readText().contains("multidrawOrderArrays"))

        //desligar a exceção remove a chave do arquivo
        st.getValue("multidrawOrderArrays").saveEnabled(false)
        MobileGluesRenderer.getConfiguredEnv(resolveRendererConfig(id, spec))
        json = JSONObject(File(mgDir, "config.json").readText())
        assertFalse(json.has("multidrawOrderArrays"))
        assertEquals(7, json.getInt("userKey"))

        //restaurar tudo volta aos padrões
        session.resetAll()
        assertFalse(session.hasOverrides)
        assertEquals(32, (st.getValue("maxGlslCacheSize").value as ConfigScalar.IntValue).value)
        dir.deleteRecursively()
    }

    @Test
    fun mgPanel() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        val session = RendererConfigSession(id, MobileGluesRenderer.configSpec, GlobalRendererConfigStore)
        val st = session.states.associateBy { it.entry.id }
        st.getValue("enableANGLE").saveValue(ConfigScalar.IntValue(1))
        st.getValue("multidrawOrderElementsBaseVertex").saveEnabled(true)
        st.getValue("fsr1Setting").saveValue(ConfigScalar.IntValue(2))
        var module by androidx.compose.runtime.mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0B1E33), Color(0xFF050B14))))
                        .padding(12.dp)
                ) {
                    androidx.compose.runtime.key(module) {
                        MobileGluesConfigContent(
                            modifier = Modifier.fillMaxSize(),
                            session = session,
                            perVersion = module == 2,
                            onReset = {},
                            onClose = {},
                            initialModule = module
                        )
                    }
                }
            }
        }
        for (m in 0..4) {
            rule.runOnUiThread { module = m }
            rule.waitForIdle()
            rule.mainClock.advanceTimeBy(800L)
            rule.waitForIdle()
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(dir, "mg_$m.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
