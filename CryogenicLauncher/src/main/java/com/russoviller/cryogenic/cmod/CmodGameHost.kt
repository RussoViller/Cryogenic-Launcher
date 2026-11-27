package com.russoviller.cryogenic.cmod

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.bridge.GameLogHub
import com.russoviller.cryogenic.bridge.LoggerBridge
import com.russoviller.cryogenic.cmod.api.CryoGameApi
import com.russoviller.cryogenic.cmod.api.CryoGameLogListener
import com.russoviller.cryogenic.cmod.api.CryoGameMod
import com.russoviller.cryogenic.cmod.api.CryoKeys
import com.russoviller.cryogenic.cmod.api.CryoModApi
import com.russoviller.cryogenic.cmod.api.CryoModPrefs
import com.russoviller.cryogenic.cmod.api.CryoModTask
import com.russoviller.cryogenic.cmod.api.CryoModUi
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Lado do jogo dos Mods do Launcher ("gameMain" no cmod.json). Roda no processo do jogo
 * (VMActivity): camada por cima do jogo, teclas/mouse/chat via CallbackBridge e linhas do log.
 * Também junta os "jvmArgs"/"gameArgs" dos mods ativos para o GameLauncher.
 */
object CmodGameHost {
    private const val TAG = "CryoMods/Game"
    private val main = Handler(Looper.getMainLooper())
    private val background = Executors.newCachedThreadPool { r -> Thread(r, "CryoGameMod").apply { isDaemon = true } }
    private val mods = LinkedHashMap<String, GameApi>()
    private var overlay: FrameLayout? = null

    /** Mods ativos e instalados, na ordem das dependências. */
    private fun enabledInfos(): List<CmodInfo> = runCatching {
        val wanted = AllSettings.cryoEnabledMods.getValue().toSet()
        CmodPaths.order(CmodPaths.readInstalled().map { it.first }, wanted).first
            .filter { it.apiVersion <= CryoModApi.API_VERSION }
    }.getOrElse { emptyList() }

    /** Argumentos extras da JVM pedidos pelos mods ativos. */
    fun extraJvmArgs(gameDir: File?): List<String> = enabledInfos().flatMap { info ->
        info.jvmArgs.map { CmodPaths.expand(it, info.id, gameDir) }
    }.also { if (it.isNotEmpty()) LoggerBridge.appendInfo("CryoMods: JVM args ${it.joinToString(" ")}") }

    /** Argumentos extras do Minecraft pedidos pelos mods ativos. */
    fun extraGameArgs(gameDir: File?): List<String> = enabledInfos().flatMap { info ->
        info.gameArgs.map { CmodPaths.expand(it, info.id, gameDir) }
    }

    /** Carrega o lado do jogo dos mods ativos. Thread principal, no onCreate da tela do jogo. */
    fun start(activity: Activity, versionName: String, minecraftVersion: String, gameDir: File?) {
        if (mods.isNotEmpty()) return
        val infos = enabledInfos().filter { it.hasGameSide }
        if (infos.isEmpty()) return
        val layer = FrameLayout(activity).apply {
            isClickable = false
            isFocusable = false
        }
        overlay = layer
        activity.window.decorView.post {
            val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@post
            (layer.parent as? ViewGroup)?.removeView(layer)
            content.addView(layer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        for (info in infos) {
            if (info.dependencies.any { it !in mods && infos.any { i -> i.id == it } }) continue
            val dir = CmodPaths.modDir(info.id)
            runCatching { CmodPaths.gameLogFile(info.id).apply { parentFile?.mkdirs(); writeText("") } }
            val api = GameApi(activity, info, dir, layer, versionName, minecraftVersion, gameDir)
            runCatching {
                val loader = CmodClassLoader.forMod(dir)
                val instance = loader.loadClass(info.gameMain).getDeclaredConstructor().newInstance() as? CryoGameMod
                    ?: throw CmodManager.InvalidCmod("${info.gameMain} does not implement CryoGameMod")
                api.instance = instance
                mods[info.id] = api
                instance.onGameStart(api)
                api.log("Game side started (${info.name} ${info.version})")
            }.onFailure { t ->
                mods.remove(info.id)
                api.dispose()
                val msg = (t.cause ?: t).let { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
                api.writeLine('E', "Failed to start: $msg")
                Logger.error(TAG, "Failed to start ${info.id}", t)
                LoggerBridge.appendInfo("CryoMods: ${info.id} failed to start: $msg")
            }
        }
    }

    /** O jogo fechou. */
    fun dispatchExit(exitCode: Int) {
        val run = {
            mods.values.toList().forEach { api -> api.guard { api.instance?.onGameExit(exitCode) } }
            stop()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run() else main.post(run)
    }

    /** A tela do jogo foi destruída. */
    fun stop() {
        mods.values.toList().reversed().forEach { it.dispose() }
        mods.clear()
        overlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
        overlay = null
    }

    private val inLogDispatch = ThreadLocal<Boolean>()

    private class GameApi(
        private val activity: Activity,
        private val info: CmodInfo,
        private val dir: File,
        private val layer: FrameLayout,
        private val versionName: String,
        private val minecraftVersion: String,
        private val gameDir: File?
    ) : CryoGameApi {
        var instance: CryoGameMod? = null
        @Volatile private var disposed = false
        private val scheduler = CmodScheduler { r -> guard { r.run() } }
        private val prefs by lazy { CmodPrefs(CmodPaths.prefsFile(info.id)) }
        private val ui by lazy { CmodUiKit(activity) { block -> guard(block) } }
        private val logListeners = CopyOnWriteArrayList<CryoGameLogListener>()
        private val views = CopyOnWriteArrayList<View>()
        private val pending = StringBuilder()

        private val watcher = LoggerBridge.EventLogListener { chunk ->
            if (disposed || chunk == null) return@EventLogListener
            val lines = synchronized(pending) {
                pending.append(chunk)
                val out = ArrayList<String>()
                while (true) {
                    val nl = pending.indexOf("\n")
                    if (nl < 0) break
                    out.add(pending.substring(0, nl).trimEnd('\r'))
                    pending.delete(0, nl + 1)
                }
                if (pending.length > 16384) pending.setLength(0)
                out
            }
            if (lines.isEmpty()) return@EventLogListener
            inLogDispatch.set(true)
            try {
                for (line in lines) logListeners.forEach { l -> guard { l.onLine(line) } }
            } finally {
                inLogDispatch.set(false)
            }
        }

        fun guard(block: () -> Unit) {
            runCatching(block).onFailure {
                Logger.error("CryoMod/${info.id}", "Game mod task failed", it)
                writeLine('E', "${it.javaClass.simpleName}: ${it.message.orEmpty()}")
            }
        }

        fun writeLine(level: Char, message: String) {
            runCatching {
                val f = CmodPaths.gameLogFile(info.id)
                if (f.length() < 512 * 1024) f.appendText(message.lineSequence().joinToString("\n", postfix = "\n") { CmodLogs.line(level, it) })
            }
        }

        fun dispose() {
            if (disposed) return
            disposed = true
            scheduler.close()
            GameLogHub.removeWatcher(watcher)
            logListeners.clear()
            val v = views.toList()
            views.clear()
            main.post { v.forEach { layer.removeView(it) } }
        }

        private fun onUi(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) guard(block) else main.post { guard(block) }
        }

        override fun getApiVersion(): Int = CryoModApi.API_VERSION
        override fun getLauncherVersion(): String = BuildConfig.VERSION_NAME
        override fun getModId(): String = info.id
        override fun getActivity(): Activity = activity
        override fun getModDirectory(): File = dir
        override fun getDataDirectory(): File = CmodPaths.dataDir(info.id)
        override fun getGameDirectory(): File? = gameDir
        override fun getVersionName(): String = versionName
        override fun getMinecraftVersion(): String = minecraftVersion
        override fun getPreferences(): CryoModPrefs = prefs
        override fun getUi(): CryoModUi = ui

        override fun getOverlay(): FrameLayout = layer

        override fun addOverlayView(view: View, gravity: Int, marginXdp: Int, marginYdp: Int) {
            if (disposed) return
            (view.parent as? ViewGroup)?.removeView(view)
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity).apply {
                val mx = ui.dp(marginXdp.toFloat())
                val my = ui.dp(marginYdp.toFloat())
                setMargins(mx, my, mx, my)
            }
            layer.addView(view, lp)
            views.add(view)
        }

        override fun removeOverlayView(view: View) {
            views.remove(view)
            layer.removeView(view)
        }

        // ---------------------------------------------------------------- entrada

        override fun pressKey(glfwKey: Int, down: Boolean) = onUi {
            CallbackBridge.sendKeyPress(glfwKey, CallbackBridge.getCurrentMods(), down)
            CallbackBridge.setModifiers(glfwKey, down)
        }

        override fun tapKey(glfwKey: Int) {
            pressKey(glfwKey, true)
            pressKey(glfwKey, false)
        }

        override fun holdKey(glfwKey: Int, holdMs: Long) {
            pressKey(glfwKey, true)
            scheduler.schedule(holdMs.coerceAtLeast(1)) { pressKey(glfwKey, false) }
        }

        override fun typeText(text: String) = onUi {
            text.forEach { c ->
                when (c) {
                    '\n' -> CallbackBridge.sendKeyPress(CryoKeys.ENTER)
                    else -> CallbackBridge.sendChar(c, 0)
                }
            }
        }

        override fun mouseButton(button: Int, down: Boolean) = onUi { CallbackBridge.sendMouseButton(button, down) }

        override fun click(button: Int) {
            mouseButton(button, true)
            mouseButton(button, false)
        }

        override fun setCursor(x: Float, y: Float) = onUi { CallbackBridge.sendCursorPos(x, y) }
        override fun moveCursor(dx: Float, dy: Float) = onUi { CallbackBridge.sendCursorDelta(dx, dy) }
        override fun getCursorX(): Float = CallbackBridge.mouseX
        override fun getCursorY(): Float = CallbackBridge.mouseY
        override fun scroll(dx: Double, dy: Double) = onUi { CallbackBridge.sendScroll(dx, dy) }
        override fun isCursorGrabbed(): Boolean = CallbackBridge.isGrabbing()
        override fun getWindowWidth(): Int = CallbackBridge.windowWidth
        override fun getWindowHeight(): Int = CallbackBridge.windowHeight
        override fun getFps(): Int = runCatching { CallbackBridge.getCurrentFps() }.getOrDefault(0)

        override fun sendChatMessage(message: String): Boolean {
            val text = message.replace("\n", " ").take(256)
            if (text.isEmpty() || !CallbackBridge.isGrabbing()) return false
            tapKey(CryoKeys.T)
            //a tela do chat precisa abrir antes de digitar
            scheduler.schedule(180) {
                typeText(text)
                scheduler.schedule(60) { tapKey(CryoKeys.ENTER) }
            }
            return true
        }

        // ---------------------------------------------------------------- eventos

        override fun addLogListener(listener: CryoGameLogListener) {
            if (disposed) return
            logListeners.addIfAbsent(listener)
            GameLogHub.addWatcher(watcher)
        }

        override fun removeLogListener(listener: CryoGameLogListener) {
            logListeners.remove(listener)
            if (logListeners.isEmpty()) GameLogHub.removeWatcher(watcher)
        }

        // ---------------------------------------------------------------- utilidades

        override fun toast(message: String) = onUi { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }

        override fun log(message: String) {
            writeLine('I', message)
            Logger.info("CryoMod/${info.id}", message)
            //dentro de um ouvinte do log, escrever no log do jogo voltaria para o próprio ouvinte
            if (inLogDispatch.get() != true) LoggerBridge.appendInfo("[CryoMod/${info.id}] $message")
        }

        override fun runOnUiThread(action: Runnable) = onUi { action.run() }
        override fun runInBackground(action: Runnable) = background.execute { guard { action.run() } }
        override fun schedule(delayMs: Long, action: Runnable): CryoModTask = scheduler.schedule(delayMs, action)
        override fun scheduleRepeating(initialDelayMs: Long, periodMs: Long, action: Runnable): CryoModTask =
            scheduler.repeat(initialDelayMs, periodMs, action)
    }
}
