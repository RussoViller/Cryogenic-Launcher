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

package com.russoviller.cryogenic.setting

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.russoviller.layer_controller.utils.snap.SnapMode
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.game.version.installed.GraphicsApi
import com.russoviller.cryogenic.setting.enums.ActionMenuSide
import com.russoviller.cryogenic.setting.enums.AppLanguage
import com.russoviller.cryogenic.setting.enums.BackgroundBlur
import com.russoviller.cryogenic.setting.enums.DarkMode
import com.russoviller.cryogenic.setting.enums.FpsDisplayMode
import com.russoviller.cryogenic.setting.enums.GamepadInputMode
import com.russoviller.cryogenic.setting.enums.GestureActionType
import com.russoviller.cryogenic.setting.enums.MirrorSourceType
import com.russoviller.cryogenic.setting.enums.MouseControlMode
import com.russoviller.cryogenic.setting.enums.MouseTouchBlockMode
import com.russoviller.cryogenic.setting.enums.ResolutionRule
import com.russoviller.cryogenic.ui.control.HotbarRule
import com.russoviller.cryogenic.ui.control.gamepad.JoystickMode
import com.russoviller.cryogenic.ui.control.mouse.CENTER_HOTSPOT
import com.russoviller.cryogenic.ui.control.mouse.CursorHotspot
import com.russoviller.cryogenic.ui.control.mouse.LEFT_TOP_HOTSPOT
import com.russoviller.cryogenic.ui.theme.ColorThemeType


object AllSettings : SettingsRegistry() {
    //Renderer
    /**
     * 全局渲染器
     */
    val renderer = stringSetting("renderer", "")

    /**
     * Vulkan 驱动器
     */
    val vulkanDriver = stringSetting("vulkanDriver", "default turnip")

    /**
     * 图形 API（Minecraft 26.2+）
     */
    val graphicsApi = enumSetting("graphicsApi", GraphicsApi.DEFAULT_OPENGL)

    /**
     * 分辨率
     */
    val resolutionRatio = intSetting("resolutionRatio", 100, 25..200)

    /**
     * 分辨率规则
     */
    val resolutionRule = enumSetting("cryoResolutionRule", ResolutionRule.PERCENTAGE)

    /**
     * 自定义分辨率宽度，0 表示尚未初始化
     */
    val customResolutionWidth = intSetting("customResolutionWidth", 0)

    /**
     * 自定义分辨率高度，0 表示尚未初始化
     */
    val customResolutionHeight = intSetting("customResolutionHeight", 0)

    /**
     * 游戏页面全屏化
     */
    val gameFullScreen = boolSetting("gameFullScreen", true)

    /**
     * 持续性能模式
     */
    val sustainedPerformance = boolSetting("sustainedPerformance", false)

    /**
     * 游戏运行时请求系统切换到同分辨率最高刷新率档位（关闭则回落系统自适应刷新）
     */
    val requestMaxRefreshRate = boolSetting("requestMaxRefreshRate", true)

    /**
     * Superfície do jogo em SurfaceView (Android 14+) em vez de TextureView
     */
    val gameSurfaceView = boolSetting("cryoGameSurfaceView", false)

    /**
     * Núcleos de alto desempenho: afinidade das threads do jogo (ver game/cpu/CpuAffinity.kt)
     */
    val cpuAffinityMode = enumSetting("cryoCpuAffinityMode", com.russoviller.cryogenic.game.cpu.CpuAffinityMode.OFF)

    /**
     * 使用系统的 Vulkan 驱动
     */
    val zinkPreferSystemDriver = boolSetting("zinkPreferSystemDriver", false)

    /**
     * Zink 垂直同步
     */
    val vsyncInZink = boolSetting("cryoVsyncInZinkDisabled", false)

    /**
     * 启用着色器日志输出
     */
    val dumpShaders = boolSetting("dumpShaders", false)

    //Game
    /**
     * 版本隔离
     */
    val versionIsolation = boolSetting("versionIsolation", true)

    /**
     * 不检查游戏完整性
     */
    val skipGameIntegrityCheck = boolSetting("skipGameIntegrityCheck", false)

    /**
     * 版本自定义信息
     */
    val versionCustomInfo = stringSetting("versionCustomInfo", "${BuildKeys.LAUNCHER_IDENTIFIER}[zl_version]")

    /**
     * 启动器的Java环境
     */
    val javaRuntime = stringSetting("javaRuntime", "")

    /**
     * 自动选择Java环境
     */
    val autoPickJavaRuntime = boolSetting("autoPickJavaRuntime", true)

    /**
     * 游戏内存分配大小
     */
    val ramAllocation = intSetting("ramAllocation", null, min = 256)

    /**
     * 自定义Jvm启动参数
     */
    val jvmArgs = stringSetting("jvmArgs", "")

    /**
     * 已禁用的原生库插件列表
     */
    val disableNativeLibPlugins = stringListSetting("nativeLibPlugins", emptyList())

    /**
     * 启动游戏时自动展示日志，直到游戏开始渲染
     */
    val showLogAutomatic = boolSetting("showLogAutomatic", false)
    val cryoLogAutoReset = boolSetting("cryoLogAutoReset", false)

    /**
     * 日志字体大小
     */
    val logTextSize = intSetting("logTextSize", 15, 5..20)

    /**
     * 日志缓冲区刷新时间
     */
    val logBufferFlushInterval = intSetting("logBufferFlushInterval", 200, 100..1000)

    //Control
    /**
     * 实体鼠标控制
     */
    val physicalMouseMode = boolSetting("physicalMouseMode", true)

    /**
     * 按键键值，按下按键呼出输入法
     */
    val physicalKeyImeCode = intSetting("physicalKeyImeCode", null)

    /**
     * 隐藏虚拟鼠标
     */
    val hideMouse = boolSetting("hideMouse", false)

    /**
     * 虚拟鼠标大小（Dp）
     */
    val mouseSize = intSetting("mouseSize", 24, 5..50)

    /**
     * 虚拟鼠标箭头热点坐标
     */
    val arrowMouseHotspot = parcelableSetting("arrowMouseHotspot", LEFT_TOP_HOTSPOT)

    /**
     * 虚拟鼠标链接选择热点坐标
     */
    val linkMouseHotspot = parcelableSetting("linkMouseHotspot", CursorHotspot(xPercent = 23, yPercent = 0))

    /**
     * 虚拟鼠标输入选择热点坐标
     */
    val iBeamMouseHotspot = parcelableSetting("iBeamMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标十字热点坐标
     */
    val crossHairMouseHotspot = parcelableSetting("crossHairMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标调整大小（上下）热点坐标
     */
    val resizeNSMouseHotspot = parcelableSetting("resizeNSMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标调整大小（左右）热点坐标
     */
    val resizeEWMouseHotspot = parcelableSetting("resizeEWMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标调整大小（全部方向）热点坐标
     */
    val resizeAllMouseHotspot = parcelableSetting("resizeAllMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标禁止/无效操作热点坐标
     */
    val notAllowedMouseHotspot = parcelableSetting("notAllowedMouseHotspot", CENTER_HOTSPOT)

    /**
     * 虚拟鼠标灵敏度
     */
    val cursorSensitivity = intSetting("cursorSensitivity", 100, 25..300)

    /**
     * 被抓获指针移动灵敏度
     */
    val mouseCaptureSensitivity = intSetting("mouseCaptureSensitivity", 100, 25..300)

    /**
     * 虚拟鼠标控制模式
     */
    val mouseControlMode = enumSetting("mouseControlMode", MouseControlMode.SLIDE)

    /**
     * 阻止半屏触摸虚拟鼠标
     */
    val mouseTouchBlockMode = enumSetting("mouseTouchBlockMode", MouseTouchBlockMode.NONE)

    /**
     * 鼠标控制长按延迟
     */
    val mouseLongPressDelay = intSetting("mouseLongPressDelay", 300, 100..1000)

    /**
     * 是否开启虚拟鼠标点击操作
     */
    val enableMouseClick = boolSetting("enableMouseClick", true)

    /**
     * 是否启用手柄控制
     */
    val gamepadControl = boolSetting("cryoGamepadControl", true)

    /**
     * SDL 下是否允许自动唤起输入法
     */
    val sdlAutoShowIme = boolSetting("sdlAutoShowIme", true)

    /**
     * 手柄输入模式（映射虚拟按键 / SDL 直通）
     */
    val gamepadInputMode = enumSetting("gamepadInputMode", GamepadInputMode.Mapped)

    /**
     * 是否已完成手柄输入模式的选择询问
     */
    val gamepadInputModePrompted = boolSetting("gamepadInputModePrompted", false)

    /**
     * 摇杆死区缩放
     */
    val gamepadDeadZoneScale = intSetting("gamepadDeadZoneScale", 100, 50..200)

    /**
     * 手柄映射配置
     */
    val gamepadMappingConfig = stringSetting("gamepadMappingConfig", "default")

    /**
     * 摇杆控制模式
     */
    val joystickControlMode = enumSetting("joystickControlMode", JoystickMode.LeftMovement)

    /**
     * 手柄摇杆控制鼠标指针时的灵敏度
     */
    val gamepadCursorSensitivity = intSetting("gamepadCursorSensitivity", 100, 25..300)

    /**
     * 手柄摇杆控制游戏视角时的灵敏度
     */
    val gamepadCameraSensitivity = intSetting("gamepadCameraSensitivity", 100, 25..300)

    /** Gatilho analógico: quanto apertar (%) para contar como pressionado */
    val gamepadTriggerThreshold = intSetting("cryoGamepadTriggerThreshold", 55, 5..95)

    /** Curva de resposta dos analógicos (x10): 10 = linear, 20 = padrão, 30 = mais precisa no começo */
    val gamepadResponseCurve = intSetting("cryoGamepadResponseCurve", 20, 10..30)

    /** Inverter o eixo vertical da câmera */
    val gamepadInvertY = boolSetting("cryoGamepadInvertY", false)

    /** Inverter o eixo horizontal da câmera */
    val gamepadInvertX = boolSetting("cryoGamepadInvertX", false)

    /** Estilo dos nomes dos botões (automático pelo fabricante do controle) */
    val gamepadGlyphStyle = enumSetting("cryoGamepadGlyphStyle", com.russoviller.cryogenic.ui.control.gamepad.GamepadGlyphStyle.Auto)

    /**
     * 手势控制
     */
    val gestureControl = boolSetting("gestureControl", false)

    /**
     * 手势控制点击时触发的鼠标按钮
     */
    val gestureTapMouseAction = enumSetting("gestureTapMouseAction", GestureActionType.MOUSE_RIGHT)

    /**
     * 手势控制长按时触发的鼠标按钮
     */
    val gestureLongPressMouseAction = enumSetting("gestureLongPressMouseAction", GestureActionType.MOUSE_LEFT)

    /**
     * 手势控制长按延迟
     */
    val gestureLongPressDelay = intSetting("gestureLongPressDelay", 300, 100..1000)

    /**
     * 陀螺仪控制
     */
    val gyroscopeControl = boolSetting("gyroscopeControl", false)

    /**
     * 陀螺仪控制灵敏度
     */
    val gyroscopeSensitivity = intSetting("gyroscopeSensitivity", 100, 25..300)

    /**
     * 陀螺仪采样率
     */
    val gyroscopeSampleRate = intSetting("gyroscopeSampleRate", 16, 5..50)

    /**
     * 陀螺仪数值平滑
     */
    val gyroscopeSmoothing = boolSetting("gyroscopeSmoothing", true)

    /**
     * 陀螺仪平滑处理的窗口大小
     */
    val gyroscopeSmoothingWindow = intSetting("gyroscopeSmoothingWindow", 4, 2..10)

    /**
     * 反转 X 轴
     */
    val gyroscopeInvertX = boolSetting("gyroscopeInvertX", false)

    /**
     * 反转 Y 轴
     */
    val gyroscopeInvertY = boolSetting("gyroscopeInvertY", false)

    //Launcher
    /**
     * 颜色主题色
     * Android 12+ 默认动态主题色
     */
    val launcherColorTheme = enumSetting(
        "launcherColorTheme",
        ColorThemeType.CRYOGENIC
    )

    /**
     * 自定义颜色主题色
     */
    val launcherCustomColor = intSetting("launcherCustomColor", Color.Blue.toArgb())

    /**
     * 自定义颜色配色风格
     */
    val launcherCustomPaletteStyle = enumSetting("launcherCustomPaletteStyle", PaletteStyle.TonalSpot)
    /** Modo streamer: as 4 sobreposições (JSON, ver StreamOverlayStore) */
    val cryoStreamOverlays = stringSetting("cryoStreamOverlays", "")
    /** Modo streamer: volume das sobreposições (vale para as 4) */
    val cryoStreamOverlayVolume = intSetting("cryoStreamOverlayVolume", 100, 0..100)
    /** Modo streamer: PNGs na tela (JSON, ver StreamExtrasStore) */
    val cryoStreamImages = stringSetting("cryoStreamImages", "")
    /** Modo streamer: textos na tela (JSON, ver StreamExtrasStore) */
    val cryoStreamTexts = stringSetting("cryoStreamTexts", "")
    /** Música: player flutuante na tela */
    val cryoMusicFloating = boolSetting("cryoMusicFloating", false)
    /** Música: estilo do player flutuante (0 = padrão, 1 = quadrado, 2 = disco) */
    val cryoMusicFloatingStyle = intSetting("cryoMusicFloatingStyle", 0, 0..2)
    /** Música: posição do player flutuante aberto (0..1000 da largura/altura livre) */
    val cryoMusicCardX = intSetting("cryoMusicCardX", 40, 0..1000)
    val cryoMusicCardY = intSetting("cryoMusicCardY", 80, 0..1000)
    /** Música: bolinha do player flutuante (lado e altura, 0..1000) */
    val cryoMusicBubbleRight = boolSetting("cryoMusicBubbleRight", false)
    val cryoMusicBubbleY = intSetting("cryoMusicBubbleY", 250, 0..1000)
    /** Player do Launcher: volume só da música (0..100) */
    val cryoPlayerVolume = intSetting("cryoPlayerVolume", 70, 0..100)
    /** Player do Launcher: repetir (0 = desligado, 1 = tudo, 2 = uma música) */
    val cryoPlayerRepeat = intSetting("cryoPlayerRepeat", 0, 0..2)
    /** Player do Launcher: ordem aleatória */
    val cryoPlayerShuffle = boolSetting("cryoPlayerShuffle", false)
    /** Degradê personalizado do tema Personalizado (ver CryoCustomGradient) */
    val cryoCustomGradient = stringSetting("cryoCustomGradient", "")

    /**
     * 启动器UI深色主题
     */
    val launcherDarkMode = enumSetting("cryoLauncherDarkMode", DarkMode.Enable)

    /**
     * 启动器语言
     */
    val launcherLanguage = enumSetting("cryoLauncherLanguage", AppLanguage.FOLLOW_SYSTEM)


    /**
     * 启动器部分屏幕全屏
     */
    val launcherFullScreen = boolSetting("launcherFullScreen", true)

    /**
     * 启用页面切换动画
     */
    val launcherSwapAnimEnabled = boolSetting("launcherSwapAnimEnabled", true)

    /**
     * 动画速度
     */
    val launcherAnimateSpeedLevel = intSetting("launcherAnimateSpeedLevel", 10, 1..30)

    /**
     * 主界面操作菜单的停泊侧
     */
    val launcherActionMenuSide = enumSetting("launcherActionMenuSide", ActionMenuSide.END)

    /**
     * 启动器背景元素不透明度
     */
    val launcherBackgroundOpacity = intSetting("launcherBackgroundOpacity", 80, 20..100)

    /** Liquid Glass: todo o visual do launcher em vidro líquido. */
    val cryoInterface2 = boolSetting("cryoInterface2", false)
    /** Ignorar a cutscene (estrela) ao ligar o Liquid Glass pelo botão. */
    val cryoLiquidSkipCutscene = boolSetting("cryoLiquidSkipCutscene", false)
    /** Ligar o Liquid Glass com um toque, sem segurar o botão. */
    val cryoLiquidSkipHold = boolSetting("cryoLiquidSkipHold", false)
    val cryoGlowEnabled = boolSetting("cryoGlowEnabled", false)

    /** Brilho neon animado (na cor do tema) nas bordas dos cartões e botões. Padrão: desligado. */
    val cryoNeonEnabled = boolSetting("cryoNeonEnabled", false)

    /** Espessura do traço neon (1 = fino, 10 = bem grosso). */
    val cryoNeonThickness = intSetting("cryoNeonThickness", 4, 1..10)

    /** Estilo das animações de transição (quicar, gelatina, deslizar...). */
    val cryoTransitionStyle = enumSetting("cryoTransitionStyle", com.russoviller.cryogenic.setting.enums.CryoTransitionStyle.DEFAULT)

    /** Intensidade/força das animações de transição, em %. */
    val cryoTransitionIntensity = intSetting("cryoTransitionIntensity", 100, 20..200)
    val cryoDiscordPresence = boolSetting("cryoDiscordPresence", true)
    /** Discord: mostrar o servidor (ou "Um jogador") em que a pessoa está jogando */
    val cryoDiscordShowServer = boolSetting("cryoDiscordShowServer", true)
    val cryoDiscordShowMusic = boolSetting("cryoDiscordShowMusic", true)
    /** Modo inativo: entrada automática após um tempo sem toque na tela inicial */
    /** Predefinições: id da última predefinição aplicada/salva e a "assinatura" do visual naquele momento */
    val cryoActivePresetId = stringSetting("cryoActivePresetId", "")
    val cryoActivePresetSignature = stringSetting("cryoActivePresetSignature", "")

    /** Mods do Launcher (.cmod) ativados: ids. Entra no "Salvar Configurações". */
    val cryoEnabledMods = stringListSetting("cryoEnabledMods", emptyList())
    val cryoIdleAutoEnabled = boolSetting("cryoIdleAutoEnabled", true)
    /** Modo inativo: tempo sem toque, em segundos (mínimo/padrão 3 min, máximo 1 h) */
    val cryoIdleTimeoutSeconds = intSetting("cryoIdleTimeoutSeconds", 180, 180..3600)
    val cryoThemeSaturation = intSetting("cryoThemeSaturation", 0, -100..100)
    val cryoThemeVibrance = intSetting("cryoThemeVibrance", 0, -100..100)
    val cryoThemeContrast = intSetting("cryoThemeContrast", 0, -100..100)
    val cryoGradientAnimated = boolSetting("cryoGradientAnimated", false)
    val cryoGradientSpeed = intSetting("cryoGradientSpeed", 50, 1..100)
    val cryoHeroWallpaperOpacity = intSetting("cryoHeroWallpaperOpacity", 100, 0..100)
    val cryoPortraitWallpaperOpacity = intSetting("cryoPortraitWallpaperOpacity", 100, 0..100)
    val cryoQuickShortcuts = stringSetting("cryoQuickShortcuts", "renderer,game,control,launcher")
    val cryoHomeFeatureCards = stringSetting("cryoHomeFeatureCards", "shaders,mods")
    val cryoDiscordAppId = stringSetting("cryoDiscordAppId", "")
    val cryoFmWallpaperVolume = intSetting("cryoFmWallpaperVolume", 0, 0..100)
    val cryoFmBackgroundOpacity = intSetting("cryoFmBackgroundOpacity", 55, 0..100)

    val curseForgeApiKey = stringSetting("cryoCurseForgeApiKey", "")

    val cryoGlowSize = intSetting("cryoGlowSize", 4, 1..10)

    val cryoGlowIntensity = intSetting("cryoGlowIntensity", 60, 5..100)

    /**
     * 启动器视频背景音量
     */
    val videoBackgroundVolume = intSetting("videoBackgroundVolume", 0, 0..100)

    /**
     * 启动器背景模糊效果
     */
    val backgroundBlur = intSetting("backgroundBlur", 0, 0..40)

    /**
     * 启动器背景模糊效果类型
     */
    val backgroundBlurType = enumSetting("backgroundBlurType", BackgroundBlur.Background)

    /**
     * 启动器上次检查更新时，用户选择忽略的版本号
     */
    val lastIgnoredVersion = intSetting("lastIgnoredVersion", null)

    /**
     * 启动器日志保留天数
     */
    val launcherLogRetentionDays = intSetting("launcherLogRetentionDays", 7, 1..14)

    /**
     * 游戏内容下载源
     */
    val gameDownloadSource = enumSetting(
        "gameDownloadSource",
        MirrorSourceType.AUTO,
        MirrorSourceType.LEGACY_NAMES
    )

    /**
     * 资源平台下载源
     */
    val assetPlatformSource = enumSetting(
        "assetPlatformSource",
        MirrorSourceType.AUTO,
        MirrorSourceType.LEGACY_NAMES
    )

    //Control
    /**
     * 全局默认控制布局文件名
     */
    val controlLayout = stringSetting("controlLayout", "")

    /**
     * Esquema de HUD em uso: cryogenic / fcl / pojarush
     */
    val hudScheme = stringSetting("hudScheme", "cryogenic")

    /**
     * Arquivo de HUD escolhido no esquema FCL
     */
    val hudLayoutFcl = stringSetting("hudLayoutFcl", "")

    /**
     * Arquivo de HUD escolhido no esquema PojaRush
     */
    val hudLayoutPojarush = stringSetting("hudLayoutPojarush", "")

    //Other
    /**
     * 当前选择的账号
     */
    val currentAccount = stringSetting("currentAccount", "")

    /**
     * 当前选择的游戏目录id
     */
    val currentGamePathId = stringSetting("currentGamePathId", GamePathManager.DEFAULT_ID)

    /**
     * 启动器任务菜单是否展开
     */
    val launcherTaskMenuExpanded = boolSetting("launcherTaskMenuExpanded", true)

    /**
     * 在游戏菜单悬浮窗上显示帧率
     */
    val showFPS = boolSetting("showFPS", true)

    /**
     * 游戏内帧率的展示模式
     */
    val fpsDisplayMode = enumSetting("fpsDisplayMode", FpsDisplayMode.NUMBER)

    /**
     * 在游戏菜单悬浮窗上显示内存
     */
    val showMemory = boolSetting("showMemory", false)

    val showCpuUsage = boolSetting("cryoShowCpuUsage", false)

    val showGpuUsage = boolSetting("cryoShowGpuUsage", false)

    /**
     * 在游戏画面上展示菜单悬浮窗
     */
    val showMenuBall = boolSetting("showMenuBall", true)

    /**
     * 游戏菜单悬浮窗位置
     */
    val menuBallPos = offsetSetting("menuBallPos", Offset.Zero)

    /**
     * 游戏菜单悬浮窗不透明度
     */
    val menuBallOpacity = intSetting("menuBallOpacity", 100, 20..100)

    /**
     * 快捷栏判定箱计算规则
     */
    val hotbarRule = enumSetting("hotbarRule", HotbarRule.Auto)

    /**
     * 快捷栏宽度百分比
     */
    val hotbarWidth = intSetting("hotbarWidth", 500, 0..1000)

    /**
     * 快捷栏高度百分比
     */
    val hotbarHeight = intSetting("hotbarHeight", 100, 0..1000)

    /**
     * 快捷栏双击与副手交换物品
     */
    val hotbarDoubleClick = boolSetting("hotbarDoubleClick", true)

    /**
     * 快捷栏长按丢弃所选物品
     */
    val hotbarLongClick = boolSetting("hotbarLongClick", true)

    /**
     * 快捷栏长按快捷栏触发延迟
     */
    val hotbarLongClickDelay = intSetting("hotbarLongClickDelay", 300, 100..1000)

    /**
     * 游戏内控制布局的整体不透明度
     */
    val controlsOpacity = intSetting("controlsOpacity", 100, 0..100)

    /**
     * 控制布局编辑器：是否开启控件吸附功能
     */
    val editorEnableWidgetSnap = boolSetting("editorEnableWidgetSnap", true)

    /**
     * 控制布局编辑器：是否在所有控件层范围内吸附
     */
    val editorSnapInAllLayers = boolSetting("editorSnapInAllLayers", false)

    /**
     * 控制布局编辑器：控件吸附模式
     */
    val editorWidgetSnapMode = enumSetting("editorWidgetSnapMode", SnapMode.FullScreen)

    /**
     * 上次检查更新的时间戳
     */
    val lastUpgradeCheck = longSetting("lastUpgradeCheck", 0L)

    val cryoAutoCheckUpdate = boolSetting("cryoAutoCheckUpdate", true)

    val cryoWardrobeFrozen = boolSetting("cryoWardrobeFrozen", false)

    val cryoAccountPanelSide = enumSetting("cryoAccountPanelSide", ActionMenuSide.START)

    /**
     * 玩家结束运行游戏的次数
     */
    val finishedGame = intSetting("finishedGame", 0)

    /**
     * 是否在打开启动器时，根据特定的运行游戏次数，显示赞助支持弹窗
     */
    val showSponsorship = boolSetting("showSponsorship", true)

    /**
     * 上次已展示结算报告的上月赞助者月份标识
     */
    val lastSponsorReportMonth = stringSetting("lastSponsorReportMonth", "")

    /** A tela de patrocinadores já apareceu uma vez (primeira abertura do Launcher). */
    val cryoSponsorIntroShown = boolSetting("cryoSponsorIntroShown", false)

    /** Patrocínio mais recente já mostrado (segundos Unix): se aparecer um mais novo, a tela abre de novo. */
    val cryoSponsorSeen = stringSetting("cryoSponsorSeen", "")

    /**
     * 搜索模组的初始搜索平台
     */
    val searchModPlatform = enumSetting("searchModPlatform", Platform.MODRINTH)

    /**
     * 搜索整合包的初始搜索平台
     */
    val searchModpackPlatform = enumSetting("searchModpackPlatform", Platform.MODRINTH)

    /**
     * 搜索资源包的初始搜索平台
     */
    val searchResourcePackPlatform = enumSetting("searchResourcePackPlatform", Platform.MODRINTH)

    /**
     * 搜索光影的初始搜索平台
     */
    val searchShadersPlatform = enumSetting("searchShadersPlatform", Platform.MODRINTH)
}
