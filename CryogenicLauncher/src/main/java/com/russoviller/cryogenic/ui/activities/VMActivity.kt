/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jakewharton.processphoenix.ProcessPhoenix
import com.russoviller.inputmap.keycodes.LwjglGlfwKeycode
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.bridge.CURSOR_DISABLED
import com.russoviller.cryogenic.bridge.FliteTts
import com.russoviller.cryogenic.bridge.LoggerBridge
import com.russoviller.cryogenic.bridge.ZLBridge
import com.russoviller.cryogenic.bridge.ZLBridgeStates
import com.russoviller.cryogenic.coroutine.DataBridge
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.input.AWTCharSender
import com.russoviller.cryogenic.game.input.CharacterSenderStrategy
import com.russoviller.cryogenic.game.input.LWJGLCharSender
import com.russoviller.cryogenic.game.launch.GameLauncher
import com.russoviller.cryogenic.game.launch.GameService
import com.russoviller.cryogenic.game.launch.JvmLaunchInfo
import com.russoviller.cryogenic.game.launch.JvmLauncher
import com.russoviller.cryogenic.game.launch.LaunchConfig
import com.russoviller.cryogenic.game.launch.Launcher
import com.russoviller.cryogenic.game.launch.handler.AbstractHandler
import com.russoviller.cryogenic.game.launch.handler.GameHandler
import com.russoviller.cryogenic.game.launch.handler.HandlerType
import com.russoviller.cryogenic.game.launch.handler.JVMHandler
import com.russoviller.cryogenic.game.multirt.RuntimesManager
import com.russoviller.cryogenic.game.plugin.PluginLoader
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.game.sdl.SdlBridge
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.ResolutionRule
import com.russoviller.cryogenic.ui.base.BaseAppCompatActivity
import com.russoviller.cryogenic.ui.base.ObserveFullScreenSetting
import com.russoviller.cryogenic.ui.components.rememberBoxSize
import com.russoviller.cryogenic.ui.control.input.HidableInputLayout
import com.russoviller.cryogenic.ui.control.input.TextInputMode
import com.russoviller.cryogenic.ui.screens.game.elements.OpenFolderLayer
import com.russoviller.cryogenic.ui.screens.game.elements.OpenFolderOperation
import com.russoviller.cryogenic.ui.theme.CryogenicLauncherTheme
import com.russoviller.cryogenic.ui.toAndroidString
import com.russoviller.cryogenic.utils.computeGameDisplayLayout
import com.russoviller.cryogenic.utils.computeGameRenderSize
import com.russoviller.cryogenic.utils.device.PhysicalMouseChecker
import com.russoviller.cryogenic.utils.getDisplayFriendlyRes
import com.russoviller.cryogenic.utils.getParcelableSafely
import com.russoviller.cryogenic.utils.rememberGameRenderSize
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.GamepadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import android.graphics.Color as NativeColor
import android.view.WindowInsets as AndroidWindowInsets


private const val INTENT_RUN_GAME = "BUNDLE_RUN_GAME"
private const val INTENT_RUN_JAR = "INTENT_RUN_JAR"
private const val INTENT_GAME_CONFIG = "INTENT_GAME_CONFIG"
private const val INTENT_JAR_INFO = "INTENT_JAR_INFO"

data class LaunchSession(
    val activityTitle: String,
    val launcher: Launcher,
    val handler: AbstractHandler,
    val inputSender: CharacterSenderStrategy
)

/**
 * 一些关键状态须在此存放
 */
class VMViewModel : ViewModel() {
    var isRunning = false

    /**
     * 是否允许VMActivity处理按键
     */
    var keyHandle = true

    val screenSizeBridge = DataBridge<IntSize>()
    var screenSize: IntSize = IntSize.Zero

    private val _onConfigurationChanged = MutableStateFlow(false)
    val onConfigurationChanged = _onConfigurationChanged.asStateFlow()

    fun onConfigurationChanged(value: Boolean = true) {
        _onConfigurationChanged.update { value }
    }

    var sender: CharacterSenderStrategy = LWJGLCharSender
        private set

    private val _openFolderOperation = MutableStateFlow<OpenFolderOperation>(OpenFolderOperation.None)
    /** 启动器内浏览目录（将文件导入该目录） */
    val openFolderOperation = _openFolderOperation.asStateFlow()

    /** 关闭浏览目录 */
    fun clearFolder() {
        _openFolderOperation.update {
            OpenFolderOperation.None
        }
    }

    private var _session: LaunchSession? = null
    val session: LaunchSession
        get() = _session ?: error("LaunchSession not initialized")

    fun initSession(
        activity: VMActivity,
        bundle: Bundle,
        errorViewModel: ErrorViewModel,
        eventViewModel: EventViewModel,
        gamepadViewModel: GamepadViewModel,
        exitListener: (Int, Boolean) -> Unit,
    ) {
        if (_session != null) return

        _session = when {
            bundle.getBoolean(INTENT_RUN_GAME) -> {
                val config: LaunchConfig = bundle.getParcelableSafely(INTENT_GAME_CONFIG, LaunchConfig::class.java)
                    ?: throw IllegalStateException("No launch config has been set.")

                val launcher = GameLauncher(
                    activity = activity,
                    config = config,
                    onExit = { code, isSignal ->
                        if (code == 0) {
                            val finishedCount = AllSettings.finishedGame.getValue()
                            if (finishedCount < Int.MAX_VALUE)  {
                                AllSettings.finishedGame.save(finishedCount + 1)
                            }
                        }
                        exitListener(code, isSignal)
                    },
                    openPath = { folder ->
                        _openFolderOperation.update {
                            OpenFolderOperation.OpenFolder(folder)
                        }
                    }
                )

                sender = LWJGLCharSender

                LaunchSession(
                    activityTitle = config.version.getVersionName(),
                    launcher = launcher,
                    handler = GameHandler(
                        activity = activity,
                        config = config,
                        errorViewModel = errorViewModel,
                        eventViewModel = eventViewModel,
                        gamepadViewModel = gamepadViewModel,
                        gameLauncher = launcher
                    ) { code ->
                        exitListener(code, false)
                    },
                    inputSender = LWJGLCharSender
                )
            }
            bundle.getBoolean(INTENT_RUN_JAR) -> {
                val jvmLaunchInfo: JvmLaunchInfo = bundle.getParcelableSafely(INTENT_JAR_INFO, JvmLaunchInfo::class.java)
                    ?: throw IllegalStateException("No launch jar info has been set.")

                val launcher = JvmLauncher(
                    context = activity,
                    jvmLaunchInfo = jvmLaunchInfo,
                    onExit = exitListener,
                    openPath = { folder ->
                        _openFolderOperation.update {
                            OpenFolderOperation.OpenFolder(folder)
                        }
                    }
                )

                sender = AWTCharSender

                LaunchSession(
                    activityTitle = activity.getString(R.string.execute_jar_title),
                    launcher = launcher,
                    handler = JVMHandler(
                        jvmLauncher = launcher,
                        errorViewModel = errorViewModel,
                        eventViewModel = eventViewModel,
                    ) { code ->
                        exitListener(code, false)
                    },
                    inputSender = AWTCharSender
                )
            }
            else -> error("Unknown VM launch mode")
        }
    }

    /**
     * 当前输入法开启状态
     */
    var textInputMode by mutableStateOf(TextInputMode.DISABLE)


    fun disableInputMode() {
        if (textInputMode == TextInputMode.ENABLE) textInputMode = TextInputMode.DISABLE
    }


    /**
     * 直接发送文本到游戏
     */
    private fun String.sendText() {
        forEach { char ->
            sender.sendChar(char)
        }
    }

    fun sendInputText(text: String) {
        text.sendText()
    }

    fun sendBackspace() {
        sender.sendBackspace()
    }

    fun sendEnder() {
        sender.sendEnter()
    }

    /**
     * 仅处理特殊按键
     */
    fun handleSpecialKey(keyEvent: KeyEvent) {
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_ENTER -> {
                //忽略掉删除事件，避免状态不同步
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> sender.sendLeft()
            KeyEvent.KEYCODE_DPAD_RIGHT -> sender.sendRight()
            KeyEvent.KEYCODE_DPAD_UP -> sender.sendUp()
            KeyEvent.KEYCODE_DPAD_DOWN -> sender.sendDown()

            KeyEvent.KEYCODE_TAB -> sender.sendTab()

            else -> {
                val isSoftKeyboard = (keyEvent.flags and KeyEvent.FLAG_SOFT_KEYBOARD) != 0
                val isTextInput = SDLActivity.isTextInputEvent(keyEvent)
                when {
                    shouldConsumeSdlTextInput(keyEvent) -> sender.sendChar(keyEvent.unicodeChar.toChar())
                    !isSoftKeyboard && !isTextInput -> sender.sendOther(keyEvent)
                }
            }
        }
    }

    /**
     * SDL 渲染路径没有输入代理的文本回调，物理字符键需直接发送文本并消费按键事件
     */
    fun shouldConsumeSdlTextInput(keyEvent: KeyEvent): Boolean {
        return SdlBridge.sdlEnabled && SdlBridge.isSdlRenderActive() &&
                (keyEvent.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == 0 &&
                SDLActivity.isTextInputEvent(keyEvent)
    }

    /**
     * IME 可能清除后续按键的 Meta 状态，Ctrl 本身必须直接同步到 SDL
     */
    fun shouldConsumeSdlModifierKey(keyEvent: KeyEvent): Boolean {
        return SdlBridge.sdlEnabled && SdlBridge.isSdlRenderActive() &&
                (keyEvent.keyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
                        keyEvent.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT)
    }

    /**
     * 返回这个按键事件是否允许被处理
     */
    fun keyCanHandle(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.keyCode
        //因为输入法选区时会发出Shift键的事件，但同步为游戏内的文本进行选区会比较复杂
        //比如选区时没法拿到当前输入框选择了哪些文本，极容易导致输入框与游戏内的文本出现状态差异
        //这类比较打破预期的情况应该尽量避免，所以应该忽略Shift
        val isShift = keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
        //避免处理Ctrl，大部分输入法不支持处理这个，而在游戏内可能会影响到指针位置
        val isCtrl = keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
        return !isShift && !isCtrl
    }
}

class VMActivity : BaseAppCompatActivity(), SurfaceTextureListener {
    override fun isIgnoreNotch(): Boolean = AllSettings.gameFullScreen.getValue()

    private val errorViewModel: ErrorViewModel by viewModels()

    private val eventViewModel: EventViewModel by viewModels()
    /**
     * 手柄状态存储 ViewModel
     */
    private val gamepadViewModel: GamepadViewModel by viewModels()

    private val vmViewModel: VMViewModel by viewModels()

    /** View used to locate the layout hosting SDL's text input bridge. */
    private var gameTextureView: TextureView? = null

    /** Modo SurfaceView (Android 14+): o jogo vai direto para o SurfaceFlinger, sem passar pela composição do TextureView. */
    private var gameSurfaceView: android.view.SurfaceView? = null
    private var surfaceViewReady = false

    private val surfaceViewCallback = object : android.view.SurfaceHolder.Callback {
        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
            surfaceViewReady = false
        }

        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
            val view = gameSurfaceView
            val viewW = view?.width?.takeIf { it > 0 } ?: width
            val viewH = view?.height?.takeIf { it > 0 } ?: height
            if (!surfaceViewReady) {
                surfaceViewReady = true
                onGameSurfaceAvailable(holder.surface, holder, viewW, viewH) { holder.surface }
            } else {
                onGameSurfaceSizeChanged(viewW, viewH)
            }
        }

        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
            surfaceViewReady = false
            onGameSurfaceDestroyed(holder)
        }
    }

    private fun gameView(): android.view.View? = gameTextureView ?: gameSurfaceView

    /** Decidido uma vez por sessão (trocar a View no meio do jogo derrubaria a superfície). */
    private val useGameSurfaceView: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && AllSettings.gameSurfaceView.getValue()
    }

    private inline fun <T> withHandler(block: AbstractHandler.() -> T): T {
        return vmViewModel.session.handler.block()
    }

    private inline fun <T> withLauncher(block: Launcher.() -> T): T {
        return vmViewModel.session.launcher.block()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Seletor de arquivos do Android para mods (tinyfd_openFileDialog)
        com.russoviller.cryogenic.game.input.GameFileDialog.attach(this)
        //Painel da Live: liga-se ao serviço da live (se estiver rodando)
        com.russoviller.cryogenic.live.LiveClient.connect(this)
        //Música: volta a música que estava carregada (pausada), se o Launcher continua aberto
        runCatching { com.russoviller.cryogenic.music.CryoPlayer.restoreSession(this) }
        com.russoviller.cryogenic.ui.components.CryoTheme.inGame = true
        //加载渲染器
        Renderers.init()
        //加载插件
        PluginLoader.loadAllPlugins(this, false)
        refreshData()

        //初始化物理鼠标连接检查器
        PhysicalMouseChecker.initChecker(this)

        //启动前台服务，防止后台网络中断
        runCatching {
            //应用处于后台等受限状态时系统会拒绝启动，此时无需保活，忽略即可
            startService(Intent(this, GameService::class.java))
        }

        val bundle = intent.extras ?: throw IllegalStateException("Unknown VM launch state!")

        vmViewModel.initSession(
            activity = this,
            bundle = bundle,
            errorViewModel = errorViewModel,
            eventViewModel = eventViewModel,
            gamepadViewModel = gamepadViewModel,
            exitListener = { exitCode: Int, isSignal: Boolean ->
                stopAllService()
                if (exitCode != 0) {
                    val logPath = withLauncher {
                        getLogFile().absolutePath
                    }
                    showExitMessage(this@VMActivity, exitCode, isSignal, logPath)
                } else {
                    //重启启动器
                    ProcessPhoenix.triggerRebirth(this@VMActivity)
                }
            }
        )

        if (bundle.getBoolean(INTENT_RUN_GAME)) {
            bundle.getParcelableSafely(INTENT_GAME_CONFIG, LaunchConfig::class.java)?.version?.let { version ->
                com.russoviller.cryogenic.ui.screens.game.CryoGraphicsError.gameVersion =
                    runCatching { version.getVersionInfo()?.minecraftVersion }.getOrNull() ?: version.getVersionName()
                //Discord: só o nome da instância; a segunda linha aparece apenas com o servidor
                //(ou "Um jogador") em que a pessoa está jogando
                com.russoviller.cryogenic.discord.CryoDiscordPresence.playing(
                    this,
                    version.getVersionName(),
                    ""
                )
                //Acompanha o log do jogo para mostrar no Discord o servidor em que a pessoa está
                com.russoviller.cryogenic.discord.CryoServerWatcher.start(this, "")
            }
        }

        //设置画面渲染输出回调
        CallbackBridge.setGraphicOutputListener {
            withHandler { onGraphicOutput() }
        }

        window?.apply {
            setBackgroundDrawable(NativeColor.BLACK.toDrawable())
            if (AllSettings.sustainedPerformance.getValue()) {
                setSustainedPerformanceMode(true)
            }
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // 防止系统息屏
        }

        applyMaxRefreshRatePolicy()

        val logFile = withLauncher { getLogFile() }
        logFile.parentFile?.mkdirs() // 换过一次日志文件路径，此处创建父目录是必要的
        if (!logFile.exists() && !logFile.createNewFile()) throw IOException("Failed to create a new log file")
        LoggerBridge.start(logFile.absolutePath)
        //depois do LoggerBridge.start: os dois listeners escrevem no latest_game.log
        getSystemService(DisplayManager::class.java)?.let { dm ->
            if (Build.VERSION.SDK_INT >= 36) {
                //Android 16: mudanças de taxa só chegam com EVENT_TYPE_DISPLAY_REFRESH_RATE
                dm.registerDisplayListener(
                    mainExecutor,
                    DisplayManager.EVENT_TYPE_DISPLAY_CHANGED or DisplayManager.EVENT_TYPE_DISPLAY_REFRESH_RATE,
                    displayListener
                )
            } else {
                dm.registerDisplayListener(displayListener, null)
            }
        }
        registerThermalLogging()
        currentDisplay()?.let { displayListener.onDisplayChanged(it.displayId) } //taxa inicial no log

        //错误信息展示
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorViewModel.errorEvents.collect { tm ->
                    errorViewModel.showErrorDialog(
                        context = this@VMActivity,
                        tm = tm
                    )
                }
            }
        }

        lifecycleScope.launch {
            //开始接收事件
            eventViewModel.events.collect { event ->
                when (event) {
                    is EventViewModel.Event.Game.RefreshSize -> {
                        vmViewModel.onConfigurationChanged()
                    }
                    is EventViewModel.Event.Game.SwitchIme -> {
                        vmViewModel.textInputMode = event.mode ?: vmViewModel.textInputMode.switch()
                    }
                    is EventViewModel.Event.Game.KeyHandle -> {
                        vmViewModel.keyHandle = event.handle
                    }
                    is EventViewModel.Event.ShowToast -> {
                        Toast.makeText(
                            this@VMActivity,
                            event.text.toAndroidString(this@VMActivity),
                            event.duration
                        ).show()
                    }
                    else -> { /* Ignore */ }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vmViewModel.textInputMode == TextInputMode.ENABLE) {
                    //那应该是想退出输入框了
                    vmViewModel.disableInputMode()
                    return
                }
                if (!vmViewModel.keyHandle) return

                eventViewModel.sendEvent(EventViewModel.Event.Game.OnBack)
            }
        })

        //关闭菜单之后，每次启动游戏都提醒，防止部分人误触了不知道怎么解决 >:(
        if (!AllSettings.showMenuBall.getValue()) {
            Toast.makeText(
                this@VMActivity,
                getString(R.string.game_menu_option_show_menu_hided),
                Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            CryogenicLauncherTheme(textShadow = false) {
                ObserveFullScreenSetting(AllSettings.gameFullScreen.state)
                //游戏内菜单切换“请求最高刷新率”后自动重应用策略
                LaunchedEffect(AllSettings.requestMaxRefreshRate.state) {
                    applyMaxRefreshRatePolicy()
                }
                Screen {
                    withHandler {
                        ComposableLayout(vmViewModel.textInputMode)
                    }

                    if (vmViewModel.textInputMode == TextInputMode.ENABLE) {
                        //输入栏控制区域
                        HidableInputLayout(
                            onSend = { text ->
                                vmViewModel.sendInputText(text)
                            },
                            onBackspace = {
                                vmViewModel.sendBackspace()
                            },
                            onEnter = {
                                vmViewModel.sendEnder()
                            },
                            onClose = {
                                vmViewModel.textInputMode = TextInputMode.DISABLE
                            }
                        )
                    }

                    //鼠标变更为抓获模式时，应该关闭输入框
                    val cursorMode by ZLBridgeStates.cursorMode.collectAsStateWithLifecycle()
                    LaunchedEffect(cursorMode) {
                        if (cursorMode == CURSOR_DISABLED) vmViewModel.disableInputMode()
                    }

                    val operation by vmViewModel.openFolderOperation.collectAsStateWithLifecycle()
                    OpenFolderLayer(
                        modifier = Modifier.fillMaxSize(),
                        operation = operation,
                        requestClose = {
                            vmViewModel.clearFolder()
                        },
                        lifecycleScope = lifecycleScope
                    )
                }
            }
        }
    }

    override fun getTaskDescriptionTitle(): String? {
        return runCatching {
            vmViewModel.session.activityTitle
        }.getOrNull()
    }

    override fun onResume() {
        super.onResume()
        withHandler { onResume() }
        //alguns ROMs (HyperOS) descartam o pedido de taxa de atualização ao voltar para o app
        applyMaxRefreshRatePolicy()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, 1)
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
    }

    override fun onPause() {
        super.onPause()
        withHandler { onPause() }
        CallbackBridge.resetInputState()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, 0)
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
    }

    override fun onStart() {
        super.onStart()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
        com.russoviller.cryogenic.live.LiveClient.setForeground(true)
        //Música do Launcher: volta a tocar se estava tocando antes de sair
        runCatching { com.russoviller.cryogenic.music.CryoPlayer.onForeground() }
    }

    override fun onStop() {
        super.onStop()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
        //Painel da Live: fora do app aparece o botão flutuante da live
        com.russoviller.cryogenic.live.LiveClient.setForeground(false)
        //Música do Launcher: pausa quando o app sai da tela
        runCatching { com.russoviller.cryogenic.music.CryoPlayer.onBackground() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            CallbackBridge.resetInputState()
        }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, if (hasFocus) 1 else 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        vmViewModel.onConfigurationChanged()
    }

    /**
     * SDL 会在窗口创建等时机按窗口宽高动态请求方向，导致有可能被设置为竖屏,
     * 在此处重写强制锁定为 sensorLandscape 即可
     */
    override fun setRequestedOrientation(requestedOrientation: Int) {
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (vmViewModel.isRunning) {
            requestRefreshWindowSize(screenSize = vmViewModel.screenSize)
        }
    }

    /**
     * 尺寸刷新的参照屏幕尺寸
     * 自定义分辨率下游戏 Surface 不再铺满全屏，其视图尺寸不能作为参照，此时优先使用全屏布局尺寸
     */
    private fun referenceScreenSize(fallback: IntSize): IntSize {
        return vmViewModel.screenSize.takeIf { it.width > 0 && it.height > 0 } ?: fallback
    }

    private var lastWindowSize: IntSize? = null
    private var refreshSizeJob: Job? = null
    private var pendingRefreshSize: IntSize? = null
    /**
     * 事件驱动的窗口尺寸刷新入口
     */
    private fun requestRefreshWindowSize(screenSize: IntSize) {
        if (screenSize.width <= 0 || screenSize.height <= 0) return
        pendingRefreshSize = screenSize
        refreshSizeJob?.cancel()
        refreshSizeJob = lifecycleScope.launch {
            delay(50L.milliseconds)
            val size = pendingRefreshSize ?: return@launch
            pendingRefreshSize = null
            refreshWindowSize(screenSize = size)
        }
    }

    private suspend fun refreshWindowSize(
        screenSize: IntSize
    ): IntSize = withContext(Dispatchers.Main) {
        val newSize = withHandler {
            when (type) {
                HandlerType.GAME -> computeGameRenderSize(screenSize)
                HandlerType.JVM -> IntSize(
                    getDisplayFriendlyRes(screenSize.width, 0.8f),
                    getDisplayFriendlyRes(screenSize.height, 0.8f)
                )
            }
        }
        lastWindowSize = newSize

        gameTextureView?.surfaceTexture?.setDefaultBufferSize(newSize.width, newSize.height)
        gameSurfaceView?.holder?.setFixedSize(newSize.width, newSize.height)
        ZLBridgeStates.onWindowChange()
        CallbackBridge.sendUpdateWindowSize(newSize.width, newSize.height)
        if (SdlBridge.sdlEnabled) {
            SDLActivity.getSDLSurface()?.let { surface ->
                surface.surfaceChanged()
                surface.nativeResize(newSize.width, newSize.height)
            }
        }

        newSize
    }

    override fun onDestroy() {
        com.russoviller.cryogenic.game.input.GameFileDialog.detach(this)
        com.russoviller.cryogenic.discord.CryoDiscordPresence.stop()
        runCatching { com.russoviller.cryogenic.music.CryoPlayer.release() }
        com.russoviller.cryogenic.ui.components.CryoTheme.inGame = false
        getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(displayListener)
        unregisterThermalLogging()
        refreshHandler.removeCallbacksAndMessages(null)
        stopAllService()
        withHandler { onDestroy() }
        SdlBridge.reset()
        FliteTts.shutdown()
        super.onDestroy()
    }

    private fun stopAllService() {
        stopService(Intent(this, GameService::class.java))
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!vmViewModel.keyHandle) return super.dispatchKeyEvent(event)

        val isPressed = event.action == KeyEvent.ACTION_DOWN

        val code = AllSettings.physicalKeyImeCode.state
        if (isPressed && code != null && event.keyCode == code) {
            //用户按下了绑定呼出输入法的按键
            //开启或关闭输入法
            vmViewModel.textInputMode = vmViewModel.textInputMode.switch()
            return true
        }
        if (vmViewModel.textInputMode == TextInputMode.ENABLE) {
            if (vmViewModel.shouldConsumeSdlModifierKey(event)) {
                vmViewModel.handleSpecialKey(event)
                return true
            }
            if (isPressed && !vmViewModel.keyCanHandle(event)) {
                return super.dispatchKeyEvent(event)
            }

            if (isPressed) {
                vmViewModel.handleSpecialKey(event)
            }
            if (vmViewModel.shouldConsumeSdlTextInput(event)) {
                return true
            }

            if (event.keyCode == KeyEvent.KEYCODE_TAB) {
                //对于Tab键，为了避免选中其他的组件，这里应该直接拦截
                return true
            }
            //在输入文本的时候，应该避免继续处理按键事件
            //否则输入法的一些功能键会失效
            return super.dispatchKeyEvent(event)
        }
        event.device?.let {
            val source = event.source
            if (source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE ||
                source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {

                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    //一些系统会将鼠标右键当成KEYCODE_BACK来处理，需要在这里进行拦截
                    //然后发送真实的鼠标右键
                    withHandler { sendMouseRight(isPressed) }
                    return false
                }
            }
        }
        withHandler {
            if (shouldIgnoreKeyEvent(event)) {
                return super.dispatchKeyEvent(event)
            }
        }
        return true
    }

    @Keep
    fun messageboxShowMessageBox(
        flags: Int,
        title: String?,
        message: String?,
        buttonFlags: IntArray,
        buttonIds: IntArray,
        buttonTexts: Array<String?>,
        colors: IntArray?
    ): Int {
        //"No supported graphics backend was found" do Minecraft 26.x: tela própria do Cryogenic, explicando o erro
        if (com.russoviller.cryogenic.ui.screens.game.CryoGraphicsError.matches(message) && !isFinishing && !isDestroyed) {
            return com.russoviller.cryogenic.ui.screens.game.CryoGraphicsError.showAndWait(
                message = message.orEmpty(),
                buttonId = buttonIds.lastOrNull() ?: -1
            )
        }
        return SDLActivity.messageboxShowMessageBox(
            this, flags, title, message, buttonFlags, buttonIds, buttonTexts, colors
        )
    }

    /** 游戏渲染用的 native Surface，保留引用供系统降档后重新发起刷新率请求 */
    private var gameSurface: Surface? = null

    /** 已请求的目标刷新率，0 表示未请求；DisplayListener 据此判断系统是否中途降档 */
    private var requestedRefreshRate = 0f

    /** 系统中途降档（智能刷新率/省电策略）后重新发起请求；1 秒冷却防御部分 ROM 频繁回调 */
    private val displayListener = object : DisplayManager.DisplayListener {
        private var lastRequestAt = 0L

        override fun onDisplayAdded(displayId: Int) {}

        override fun onDisplayRemoved(displayId: Int) {}

        private var lastLoggedRate = 0f
        private var lastLoggedAppRate = 0f
        private val retry = Runnable { onDisplayChanged(currentDisplay()?.displayId ?: return@Runnable) }

        override fun onDisplayChanged(displayId: Int) {
            val display = currentDisplay() ?: return
            if (display.displayId != displayId) return
            val rate = display.mode.refreshRate
            //taxa que o sistema entrega a ESTE app (pode ser menor que a da tela: limite de FPS por app do sistema)
            val appRate = display.refreshRate
            //registra no latest_game.log toda troca de taxa de atualização (diagnóstico de FPS travando em 60/90)
            if (kotlin.math.abs(rate - lastLoggedRate) >= 0.5f || kotlin.math.abs(appRate - lastLoggedAppRate) >= 0.5f) {
                lastLoggedRate = rate
                lastLoggedAppRate = appRate
                LoggerBridge.append(
                    "Display: refresh rate ${rate.roundToIntSafe()} Hz, app frame rate ${appRate.roundToIntSafe()} Hz" +
                        (if (requestedRefreshRate > 0f) " (requested ${requestedRefreshRate.roundToIntSafe()} Hz)" else "") +
                        thermalSuffix()
                )
            }
            if (requestedRefreshRate <= 0f || rate >= requestedRefreshRate - 0.1f) return
            val elapsed = SystemClock.uptimeMillis() - lastRequestAt
            if (elapsed < 1000L) {
                //dentro do cooldown: agenda uma nova tentativa em vez de desistir
                refreshHandler.removeCallbacks(retry)
                refreshHandler.postDelayed(retry, 1000L - elapsed)
                return
            }
            lastRequestAt = SystemClock.uptimeMillis()
            applyMaxRefreshRatePolicy()
        }
    }

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

    /** Estado térmico atual para o log (o sistema baixa a taxa de atualização quando esquenta). */
    private fun thermalSuffix(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ""
        val pm = getSystemService(android.os.PowerManager::class.java) ?: return ""
        val parts = mutableListOf("thermal ${thermalName(pm.currentThermalStatus)}")
        if (pm.isPowerSaveMode) parts += "battery saver ON"
        return ", " + parts.joinToString(", ")
    }

    private fun thermalName(status: Int): String = when (status) {
        android.os.PowerManager.THERMAL_STATUS_NONE -> "none"
        android.os.PowerManager.THERMAL_STATUS_LIGHT -> "light"
        android.os.PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
        android.os.PowerManager.THERMAL_STATUS_SEVERE -> "severe"
        android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
        android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
        android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
        else -> status.toString()
    }

    private var thermalListener: Any? = null

    /** Loga mudanças do estado térmico no latest_game.log (API 29+). */
    private fun registerThermalLogging() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = getSystemService(android.os.PowerManager::class.java) ?: return
        val listener = android.os.PowerManager.OnThermalStatusChangedListener { status ->
            LoggerBridge.append("Thermal status: ${thermalName(status)}")
        }
        runCatching { pm.addThermalStatusListener(listener) }.onSuccess { thermalListener = listener }
    }

    private fun unregisterThermalLogging() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val listener = thermalListener as? android.os.PowerManager.OnThermalStatusChangedListener ?: return
        runCatching { getSystemService(android.os.PowerManager::class.java)?.removeThermalStatusListener(listener) }
        thermalListener = null
    }

    /**
     * 应用“请求最高刷新率”策略（设置开关，默认开）：
     * 窗口级 preferredDisplayModeId 硬请求同分辨率下的最高刷新率档位，
     * surface 级 setFrameRate 投票（CHANGE_FRAME_RATE_ALWAYS，非无缝切档的机型也生效）。
     * 关闭时清除两类请求，回落系统自适应刷新。
     */
    fun applyMaxRefreshRatePolicy() {
        val display = currentDisplay() ?: return
        if (!AllSettings.requestMaxRefreshRate.getValue()) {
            requestedRefreshRate = 0f
            setPreferredDisplayModeId(0, 0f)
            clearSurfaceFrameRate()
            applyViewFrameRateVotes(0f)
            return
        }
        val best = maxRefreshRateMode(display)
        requestedRefreshRate = best.refreshRate
        setPreferredDisplayModeId(best.modeId, best.refreshRate)
        voteMaxDisplayRefreshRate()
        applyViewFrameRateVotes(best.refreshRate)
    }

    /**
     * Android 15+ (frame rate por View / economia de energia adaptativa): o jogo é desenhado num TextureView,
     * e o voto padrão de uma View pode deixar o sistema baixar a tela para 60/90 Hz. Aqui a janela desliga
     * a redução por economia e o TextureView pede explicitamente a taxa máxima.
     * rate <= 0 volta ao comportamento padrão.
     */
    private fun applyViewFrameRateVotes(rate: Float) {
        if (Build.VERSION.SDK_INT < 35) return
        runCatching {
            window.setFrameRatePowerSavingsBalanced(rate <= 0f)
            gameView()?.setRequestedFrameRate(if (rate > 0f) rate else android.view.View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT)
        }.onFailure { com.russoviller.cryogenic.utils.logging.Logger.warning("VMActivity", "Frame rate vote failed", it) }
    }

    /** 当前关联的 Display；API 30 前走已废弃的 getDefaultDisplay */
    private fun currentDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
        else @Suppress("DEPRECATION") windowManager.defaultDisplay
    }

    /** 在与当前模式同分辨率的档位中取最高刷新率档（只比同分辨率，避免选到低分辨率高刷档） */
    private fun maxRefreshRateMode(display: Display): Display.Mode {
        val current = display.mode
        return display.supportedModes.fold(current) { best, mode ->
            if (mode.physicalWidth == current.physicalWidth &&
                mode.physicalHeight == current.physicalHeight &&
                mode.refreshRate > best.refreshRate
            ) mode else best
        }
    }

    /** surface 级投票：请求系统将屏幕切换到设备支持的最高刷新率，避免游戏帧率被系统限制在自选的较低刷新档位 */
    private fun voteMaxDisplayRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val surface = gameSurface ?: return
        val display = currentDisplay() ?: return
        surface.setFrameRate(
            maxRefreshRateMode(display).refreshRate,
            Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
            Surface.CHANGE_FRAME_RATE_ALWAYS
        )
    }

    /** modeId 传 0 表示清除窗口的显示模式偏好；refreshRate 同步写入 preferredRefreshRate（部分 ROM 只看这一项） */
    private fun setPreferredDisplayModeId(modeId: Int, refreshRate: Float) {
        val attributes = window.attributes
        if (attributes.preferredDisplayModeId == modeId && attributes.preferredRefreshRate == refreshRate) return
        attributes.preferredDisplayModeId = modeId
        attributes.preferredRefreshRate = refreshRate
        window.attributes = attributes
    }

    private fun clearSurfaceFrameRate() {
        gameSurface?.let { surface ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                surface.clearFrameRate()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                surface.setFrameRate(0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        onGameSurfaceAvailable(Surface(surface), surface, width, height) { Surface(surface) }
    }

    /**
     * Superfície do jogo pronta (TextureView ou SurfaceView).
     * @param source identidade da origem (SurfaceTexture ou SurfaceHolder) para o SdlBridge
     * @param executeSurface superfície entregue ao primeiro launch
     */
    private fun onGameSurfaceAvailable(nativeSurface: Surface, source: Any, width: Int, height: Int, executeSurface: () -> Surface) {
        gameSurface = nativeSurface
        applyMaxRefreshRatePolicy()
        SdlBridge.prepareSurface(this, nativeSurface, gameView()?.parent as? ViewGroup, source)
        //游戏请求 GLFW direct gamepad 时的通知接收方
        CallbackBridge.setDirectGamepadEnableHandler {
            LoggerBridge.append("CryogenicLauncher: Direct gamepad handler enabled")
        }
        if (vmViewModel.isRunning) {
            ZLBridge.setupBridgeWindow(nativeSurface)
            // surface 重建后新 SurfaceTexture 的渲染 buffer 回落视图尺寸，须重新恢复渲染状态；
            // 复用分支若不重置销毁标记，后续尺寸回调会被持续阻塞，错位状态将无法自愈
            withHandler { mIsSurfaceDestroyed = false }
            requestRefreshWindowSize(screenSize = referenceScreenSize(fallback = IntSize(width, height)))
            return
        }
        vmViewModel.isRunning = true

        withHandler { mIsSurfaceDestroyed = false }
        lifecycleScope.launch(Dispatchers.Default) {
            val screenSize = vmViewModel.screenSizeBridge.awaitData()
            val currentSize = refreshWindowSize(screenSize = screenSize)
            withHandler {
                execute(
                    surface = executeSurface(),
                    screenSize = currentSize,
                    scope = lifecycleScope
                )
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        onGameSurfaceSizeChanged(width, height)
    }

    private fun onGameSurfaceSizeChanged(width: Int, height: Int) {
        if (withHandler { mIsSurfaceDestroyed }) return
        requestRefreshWindowSize(screenSize = referenceScreenSize(fallback = IntSize(width, height)))
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        onGameSurfaceDestroyed(surface)
        return true
    }

    private fun onGameSurfaceDestroyed(source: Any) {
        gameSurface = null
        val nativeSurface = SDLSurface.getNativeSurface()
        if (SdlBridge.beginSurfaceDestroy(source, nativeSurface)) {
            if (SdlBridge.sdlEnabled) {
                SDLActivity.getSDLSurface()?.surfaceDestroyed()
            }
            SdlBridge.unregisterSurface(nativeSurface)
        }
        withHandler { mIsSurfaceDestroyed = true }
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    @Composable
    private fun Screen(
        content: @Composable () -> Unit = {}
    ) {
        val imeInsets = WindowInsets.ime
        val inputArea by withHandler { inputArea }.collectAsStateWithLifecycle()
        val density = LocalDensity.current

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val screenSize = rememberBoxSize()

            val changed by vmViewModel.onConfigurationChanged.collectAsStateWithLifecycle()
            LaunchedEffect(screenSize, changed) {
                vmViewModel.screenSize = screenSize
                vmViewModel.screenSizeBridge.provideData(screenSize)
                if (changed) {
                    requestRefreshWindowSize(screenSize = screenSize)
                    vmViewModel.onConfigurationChanged(false)
                }
            }

            //游戏模式下使用自定义分辨率时，游戏画面等比缩放居中显示（黑边）
            val letterboxed = withHandler { type } == HandlerType.GAME &&
                    AllSettings.resolutionRule.state == ResolutionRule.CUSTOM
            val renderSize = rememberGameRenderSize(screenSize)
            val displaySize = if (letterboxed) computeGameDisplayLayout(screenSize, renderSize).displaySize else screenSize

            AndroidView(
                modifier = Modifier
                    .then(
                        if (displaySize == screenSize) Modifier.fillMaxSize()
                        else Modifier
                            .align(Alignment.Center)
                            .size(
                                width = with(density) { displaySize.width.toDp() },
                                height = with(density) { displaySize.height.toDp() }
                            )
                    )
                    .absoluteOffset {
                        val area = inputArea ?: return@absoluteOffset IntOffset.Zero
                        val imeHeight = imeInsets.getBottom(this@absoluteOffset)
                        val bottomDistance = screenSize.height - area.bottom
                        val bottomPadding = (imeHeight - bottomDistance).coerceAtLeast(0)
                        IntOffset(0, -bottomPadding)
                    },
                factory = { context ->
                    val imeInsetsListener = android.view.View.OnApplyWindowInsetsListener { v, insets ->
                        if (SdlBridge.sdlEnabled && Build.VERSION.SDK_INT >= 30) {
                            SDLActivity.notifyImeVisibilityChanged(
                                insets.isVisible(AndroidWindowInsets.Type.ime())
                            )
                        }
                        v.onApplyWindowInsets(insets)
                    }
                    if (useGameSurfaceView) {
                        //SurfaceView: quadros do jogo vão direto ao SurfaceFlinger (sem cópia nem limite de taxa da
                        //composição do app); a superfície vive enquanto a View estiver anexada, como no TextureView
                        android.view.SurfaceView(context).apply {
                            setSurfaceLifecycle(android.view.SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)
                            holder.addCallback(surfaceViewCallback)
                            setOnApplyWindowInsetsListener(imeInsetsListener)
                        }.also { view ->
                            gameSurfaceView = view
                            LoggerBridge.append("CryogenicLauncher: game surface = SurfaceView")
                            applyMaxRefreshRatePolicy()
                        }
                    } else {
                        TextureView(context).apply {
                            isOpaque = true
                            alpha = 1.0f
                            surfaceTextureListener = this@VMActivity
                            setOnApplyWindowInsetsListener(imeInsetsListener)
                        }.also { texture ->
                            gameTextureView = texture
                            //o voto de taxa do TextureView (Android 15+) precisa da View criada
                            applyMaxRefreshRatePolicy()
                        }
                    }
                }
            )

            //Modo streamer: sobreposições por cima do jogo e por baixo dos controles
            com.russoviller.cryogenic.streamer.StreamOverlayLayer(modifier = Modifier.fillMaxSize())
            //PNGs e textos na tela (também por baixo dos controles)
            com.russoviller.cryogenic.streamer.StreamExtrasLayer(modifier = Modifier.fillMaxSize())
            //Painel da Live: chat da live do TikTok por cima do jogo
            com.russoviller.cryogenic.live.LiveChatOverlay(modifier = Modifier.fillMaxSize())

            content()

            //Player de música flutuante (por cima dos controles para poder ser tocado)
            com.russoviller.cryogenic.ui.screens.game.CryoMusicFloatingPlayer(modifier = Modifier.fillMaxSize())

            //Ajuste de posição das sobreposições (por cima de tudo enquanto aberto)
            com.russoviller.cryogenic.streamer.StreamOverlayEditor(modifier = Modifier.fillMaxSize())
            com.russoviller.cryogenic.streamer.StreamExtrasEditor(modifier = Modifier.fillMaxSize())

            //Erro de modo gráfico do Minecraft 26.x (substitui a caixa padrão)
            com.russoviller.cryogenic.ui.screens.game.CryoGraphicsErrorOverlay(modifier = Modifier.fillMaxSize())

        }
    }
}

/**
 * 让VMActivity进入运行游戏模式
 * @param version 指定版本
 */
fun runGame(
    context: Context,
    version: Version,
    account: Account,
) {
    startGameService(context)
    com.russoviller.cryogenic.discord.CryoDiscordPresence.handoffToGame()
    //O jogo roda em outro processo: avisa também o processo do Launcher, que para gradientes,
    //GIFs e animações enquanto o jogo estiver aberto
    com.russoviller.cryogenic.ui.components.CryoTheme.inGame = true
    val intent = Intent(context, VMActivity::class.java).apply {
        putExtra(INTENT_RUN_GAME, true)
        putExtra(INTENT_GAME_CONFIG, LaunchConfig(version, account))
    }
    context.startActivity(intent)
}

/**
 * 让VMActivity进入运行Jar模式
 * @param jarFile 指定 jar 文件
 * @param jreName 指定使用的 Java 环境，null 则为自动选择
 * @param customArgs 指定 jvm 参数
 */
fun runJar(
    context: Context,
    jarFile: File,
    jreName: String? = null,
    customArgs: String? = null
) {
    RuntimesManager.getExactJreName(8) ?: run {
        Toast.makeText(context, R.string.multirt_no_java_8, Toast.LENGTH_SHORT).show()
        return
    }

    val jvmArgsPrefix = customArgs?.let { "$it " } ?: ""
    val jvmArgs = "$jvmArgsPrefix-jar ${jarFile.absolutePath}"

    val jvmLaunchInfo = JvmLaunchInfo(
        jvmArgs = jvmArgs,
        jreName = jreName
    )

    startGameService(context)

    val intent = Intent(context, VMActivity::class.java).apply {
        putExtra(INTENT_RUN_JAR, true)
        putExtra(INTENT_JAR_INFO, jvmLaunchInfo)
    }
    context.startActivity(intent)
}

private fun startGameService(context: Context) {
    runCatching {
        //应用处于后台等受限状态时系统会拒绝启动，此时无需保活，忽略即可
        context.startService(Intent(context, GameService::class.java))
    }
}
