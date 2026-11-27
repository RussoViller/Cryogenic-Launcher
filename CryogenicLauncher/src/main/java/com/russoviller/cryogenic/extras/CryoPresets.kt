package com.russoviller.cryogenic.extras

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.materialkolor.PaletteStyle
import com.russoviller.cryogenic.filemanager.ui.components.FmWallpaperStore
import com.russoviller.cryogenic.filemanager.ui.components.HeroWallpaperStore
import com.russoviller.cryogenic.filemanager.ui.components.PortraitWallpaperStore
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.viewmodel.BackgroundViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Uma predefinição: o tema do Launcher (cor, ajustes de cor e degradê) junto dos 4 wallpapers
 * (fundo, cartão principal, 9:16 e fundo do gerenciador de arquivos) e da opacidade de cada um.
 */
@Serializable
data class CryoPreset(
    val id: String,
    val name: String,
    val createdAt: Long,
    val theme: String,
    val customColor: Int,
    val paletteStyle: String,
    val saturation: Int = 0,
    val vibrance: Int = 0,
    val contrast: Int = 0,
    val gradientAnimated: Boolean = false,
    val gradientSpeed: Int = 50,
    val backgroundOpacity: Int = 80,
    val heroOpacity: Int = 100,
    val portraitOpacity: Int = 100,
    val background: WallpaperRef? = null,
    val hero: WallpaperRef? = null,
    val portrait: WallpaperRef? = null,
    //Fundo do gerenciador de arquivos. Predefinições antigas não têm (hasFm = false): ao aplicar
    //uma delas, o fundo do gerenciador fica como está em vez de ser apagado
    val hasFm: Boolean = false,
    val fm: WallpaperRef? = null,
    val fmOpacity: Int = 55,
    val fmVolume: Int = 0,
    //Degradê personalizado. null = predefinição antiga: não mexe no degradê atual
    val customGradient: String? = null,
    //Volume do wallpaper de fundo (vídeo). null = predefinição antiga: não mexe no volume atual
    val backgroundVolume: Int? = null
) {
    val themeType: ColorThemeType
        get() = ColorThemeType.entries.firstOrNull { it.name == theme } ?: ColorThemeType.CRYOGENIC
    val palette: PaletteStyle
        get() = PaletteStyle.entries.firstOrNull { it.name == paletteStyle } ?: PaletteStyle.TonalSpot
}

@Serializable
private data class PresetFile(val version: Int = 1, val presets: List<CryoPreset> = emptyList())

object CryoPresets {
    const val NAME_MAX = 40

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val lock = Mutex()

    private val dir: File get() = File(PathManager.DIR_FILES_PRIVATE, "presets")
    private val file: File get() = File(dir, "presets.json")

    /** Lista em memória, da mais nova para a mais antiga. */
    var presets by mutableStateOf<List<CryoPreset>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    private val backgroundFile: File get() = PathManager.FILE_LAUNCHER_BACKGROUND

    suspend fun load() = lock.withLock {
        if (loaded) return@withLock
        val list = withContext(Dispatchers.IO) {
            runCatching { json.decodeFromString<PresetFile>(file.readText()).presets }.getOrDefault(emptyList())
        }
        withContext(Dispatchers.Main) {
            presets = list
            loaded = true
        }
    }

    /** Salva o visual atual como uma nova predefinição. */
    suspend fun saveCurrent(name: String): CryoPreset = lock.withLock {
        val snapshot = withContext(Dispatchers.Main) { settingsSnapshot() }
        val preset = withContext(Dispatchers.IO) {
            snapshot.copy(
                id = UUID.randomUUID().toString(),
                name = name.trim().take(NAME_MAX),
                createdAt = System.currentTimeMillis(),
                background = CryoWallpaperLibrary.store(backgroundFile),
                hero = CryoWallpaperLibrary.store(HeroWallpaperStore.file),
                portrait = CryoWallpaperLibrary.store(PortraitWallpaperStore.file),
                hasFm = true,
                fm = CryoWallpaperLibrary.store(FmWallpaperStore.file)
            )
        }
        val updated = listOf(preset) + presets
        persist(updated)
        withContext(Dispatchers.Main) {
            presets = updated
            //O visual salvo é o que está na tela: ela já está "em uso"
            markActive(preset.id)
        }
        preset
    }

    /** Aplica a predefinição: tema, opacidades e os wallpapers. */
    suspend fun apply(preset: CryoPreset, background: BackgroundViewModel?) = lock.withLock {
        withContext(Dispatchers.IO) {
            restoreSlot(preset.background, backgroundFile)
            restoreSlot(preset.hero, HeroWallpaperStore.file)
            restoreSlot(preset.portrait, PortraitWallpaperStore.file)
            if (preset.hasFm) restoreSlot(preset.fm, FmWallpaperStore.file)
        }
        withContext(Dispatchers.Main) {
            AllSettings.launcherCustomColor.save(preset.customColor)
            AllSettings.launcherCustomPaletteStyle.save(preset.palette)
            AllSettings.cryoThemeSaturation.save(preset.saturation.coerceIn(-100, 100))
            AllSettings.cryoThemeVibrance.save(preset.vibrance.coerceIn(-100, 100))
            AllSettings.cryoThemeContrast.save(preset.contrast.coerceIn(-100, 100))
            AllSettings.cryoGradientAnimated.save(preset.gradientAnimated)
            preset.customGradient?.let { AllSettings.cryoCustomGradient.save(it) }
            AllSettings.cryoGradientSpeed.save(preset.gradientSpeed.coerceIn(1, 100))
            AllSettings.launcherBackgroundOpacity.save(preset.backgroundOpacity.coerceIn(20, 100))
            preset.backgroundVolume?.let { AllSettings.videoBackgroundVolume.save(it.coerceIn(0, 100)) }
            AllSettings.cryoHeroWallpaperOpacity.save(preset.heroOpacity.coerceIn(0, 100))
            AllSettings.cryoPortraitWallpaperOpacity.save(preset.portraitOpacity.coerceIn(0, 100))
            if (preset.hasFm) {
                AllSettings.cryoFmBackgroundOpacity.save(preset.fmOpacity.coerceIn(0, 100))
                AllSettings.cryoFmWallpaperVolume.save(preset.fmVolume.coerceIn(0, 100))
            }
            AllSettings.launcherColorTheme.save(preset.themeType)
        }
        HeroWallpaperStore.refresh()
        PortraitWallpaperStore.refresh()
        if (preset.hasFm) FmWallpaperStore.refresh()
        background?.reload()
        withContext(Dispatchers.Main) { markActive(preset.id) }
    }

    suspend fun rename(id: String, name: String) = lock.withLock {
        val clean = name.trim().take(NAME_MAX)
        if (clean.isEmpty()) return@withLock
        val updated = presets.map { if (it.id == id) it.copy(name = clean) else it }
        persist(updated)
        withContext(Dispatchers.Main) { presets = updated }
    }

    suspend fun delete(id: String) = lock.withLock {
        val updated = presets.filterNot { it.id == id }
        persist(updated)
        withContext(Dispatchers.IO) {
            CryoWallpaperLibrary.collectGarbage(
                updated.flatMap { listOfNotNull(it.background, it.hero, it.portrait, it.fm) }.map { it.id }.toSet()
            )
        }
        withContext(Dispatchers.Main) {
            presets = updated
            if (AllSettings.cryoActivePresetId.state == id) AllSettings.cryoActivePresetId.save("")
        }
    }

    // ------------------------------------------------------------------
    // Exportar / importar (.cryo)
    //
    // Um arquivo .cryo é: 8 bytes de assinatura ("CRYOPRE1") seguidos de um ZIP com
    //   theme.json              -> tema, cor, ajustes de cor, degradê, opacidades e nome
    //   background01.file       -> wallpaper de fundo            (nomes internos do Launcher)
    //   hero_wallpaper.file     -> wallpaper do cartão principal
    //   portrait_wallpaper.file -> wallpaper 9:16
    //   fm_wallpaper.file       -> fundo do gerenciador de arquivos
    // A assinatura na frente faz com que só o Cryogenic reconheça o arquivo.
    // ------------------------------------------------------------------

    class InvalidCryoFile : Exception("Not a Cryogenic preset file")

    private val MAGIC = "CRYOPRE1".toByteArray(Charsets.US_ASCII)
    private const val ENTRY_THEME = "theme.json"
    private const val THEME_MAX_BYTES = 256L * 1024
    private const val WALLPAPER_MAX_BYTES = 600L * 1024 * 1024

    private fun slotEntries(): List<Pair<String, (CryoPreset) -> WallpaperRef?>> = listOf(
        backgroundFile.name to { it.background },
        HeroWallpaperStore.file.name to { it.hero },
        PortraitWallpaperStore.file.name to { it.portrait },
        FmWallpaperStore.file.name to { it.fm }
    )

    /** Nome de arquivo sugerido: "Russo teste" -> "Russo-teste.cryo". */
    fun exportFileName(preset: CryoPreset): String {
        val clean = preset.name.trim()
            .replace(Regex("""[\\/:*?"<>|\s]+"""), "-")
            .trim('-', '.')
            .ifEmpty { "preset" }
        return "$clean.cryo"
    }

    suspend fun export(context: Context, preset: CryoPreset, uri: Uri) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open output")
        out.buffered().use { raw ->
            raw.write(MAGIC)
            ZipOutputStream(raw).use { zip ->
                zip.putNextEntry(ZipEntry(ENTRY_THEME))
                zip.write(json.encodeToString(CryoPreset.serializer(), preset.copy(id = "")).toByteArray())
                zip.closeEntry()
                slotEntries().forEach { (entryName, pick) ->
                    val source = pick(preset)?.let { CryoWallpaperLibrary.file(it.id) }?.takeIf { it.isFile }
                        ?: return@forEach
                    //Imagens e vídeos já são comprimidos: guardar sem recomprimir é bem mais rápido
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    zip.putNextEntry(ZipEntry(entryName))
                    source.inputStream().use { it.copyTo(zip, 1 shl 16) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Importa um .cryo: guarda os wallpapers na biblioteca e adiciona a predefinição à lista (sem aplicar). */
    suspend fun import(context: Context, uri: Uri): CryoPreset = lock.withLock {
        val tempDir = File(context.cacheDir, "preset_import")
        val imported = withContext(Dispatchers.IO) {
            FileUtils.deleteQuietly(tempDir)
            tempDir.mkdirs()
            try {
                var theme: CryoPreset? = null
                val refs = HashMap<String, WallpaperRef?>()
                val known = slotEntries().map { it.first }.toSet()
                val input = context.contentResolver.openInputStream(uri) ?: throw InvalidCryoFile()
                input.buffered().use { raw ->
                    val head = ByteArray(MAGIC.size)
                    var got = 0
                    while (got < head.size) {
                        val r = raw.read(head, got, head.size - got)
                        if (r < 0) break
                        got += r
                    }
                    if (got != MAGIC.size || !head.contentEquals(MAGIC)) throw InvalidCryoFile()
                    ZipInputStream(raw).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            when (entry.name) {
                                ENTRY_THEME -> {
                                    val bytes = readLimited(zip, THEME_MAX_BYTES)
                                    theme = runCatching {
                                        json.decodeFromString(CryoPreset.serializer(), bytes.toString(Charsets.UTF_8))
                                    }.getOrNull()
                                }
                                //Só os 3 nomes conhecidos são lidos; o destino é sempre um arquivo temporário fixo
                                in known -> {
                                    val temp = File(tempDir, "slot_${refs.size}.file")
                                    temp.outputStream().use { copyLimited(zip, it, WALLPAPER_MAX_BYTES) }
                                    refs[entry.name] = CryoWallpaperLibrary.store(temp)
                                    FileUtils.deleteQuietly(temp)
                                }
                            }
                            zip.closeEntry()
                        }
                    }
                }
                val base = theme ?: throw InvalidCryoFile()
                val wanted = base.name.trim().take(NAME_MAX).ifEmpty { "Preset" }
                val names = presets.map { it.name }.toSet()
                var finalName = wanted
                var n = 2
                while (finalName in names) finalName = "${wanted.take(NAME_MAX - 5)} ($n)".also { n++ }
                base.copy(
                    id = UUID.randomUUID().toString(),
                    name = finalName,
                    createdAt = System.currentTimeMillis(),
                    //Valida o tema: um nome desconhecido (versão mais nova) cai no padrão
                    theme = base.themeType.name,
                    paletteStyle = base.palette.name,
                    background = refs[backgroundFile.name],
                    hero = refs[HeroWallpaperStore.file.name],
                    portrait = refs[PortraitWallpaperStore.file.name],
                    fm = refs[FmWallpaperStore.file.name]
                )
            } finally {
                FileUtils.deleteQuietly(tempDir)
            }
        }
        val updated = listOf(imported) + presets
        persist(updated)
        withContext(Dispatchers.Main) { presets = updated }
        imported
    }

    private fun readLimited(input: InputStream, max: Long): ByteArray {
        val out = ByteArrayOutputStream()
        copyLimited(input, out, max)
        return out.toByteArray()
    }

    private fun copyLimited(input: InputStream, out: OutputStream, max: Long) {
        val buffer = ByteArray(1 shl 16)
        var total = 0L
        while (true) {
            val r = input.read(buffer)
            if (r < 0) break
            total += r
            if (total > max) throw InvalidCryoFile()
            out.write(buffer, 0, r)
        }
    }

    /**
     * Id da predefinição que está na tela agora, ou "" se o visual foi mexido depois de aplicar/salvar.
     * Compara uma assinatura barata (configurações + tamanho e data dos 3 arquivos), sem ler os arquivos.
     */
    suspend fun activeId(): String = withContext(Dispatchers.IO) {
        val id = AllSettings.cryoActivePresetId.state
        if (id.isEmpty()) return@withContext ""
        if (currentSignature() == AllSettings.cryoActivePresetSignature.state) id else ""
    }

    /** Nome sugerido para a próxima predefinição. */
    fun suggestedName(base: String): String {
        val names = presets.map { it.name }.toSet()
        var n = presets.size + 1
        while ("$base $n" in names) n++
        return "$base $n"
    }

    private fun markActive(id: String) {
        AllSettings.cryoActivePresetId.save(id)
        AllSettings.cryoActivePresetSignature.save(currentSignature())
    }

    private fun currentSignature(): String {
        fun f(file: File) = if (file.isFile) "${file.length()}:${file.lastModified()}" else "-"
        val s = settingsSnapshot()
        return listOf(
            s.theme, s.customColor, s.paletteStyle, s.saturation, s.vibrance, s.contrast,
            s.gradientAnimated, s.gradientSpeed, s.backgroundOpacity, s.heroOpacity, s.portraitOpacity,
            f(backgroundFile), f(HeroWallpaperStore.file), f(PortraitWallpaperStore.file),
            s.fmOpacity, s.fmVolume, f(FmWallpaperStore.file), s.customGradient, s.backgroundVolume
        ).joinToString("|")
    }

    private fun settingsSnapshot() = CryoPreset(
        id = "",
        name = "",
        createdAt = 0L,
        theme = AllSettings.launcherColorTheme.state.name,
        customColor = AllSettings.launcherCustomColor.state,
        paletteStyle = AllSettings.launcherCustomPaletteStyle.state.name,
        saturation = AllSettings.cryoThemeSaturation.state,
        vibrance = AllSettings.cryoThemeVibrance.state,
        contrast = AllSettings.cryoThemeContrast.state,
        gradientAnimated = AllSettings.cryoGradientAnimated.state,
        gradientSpeed = AllSettings.cryoGradientSpeed.state,
        backgroundOpacity = AllSettings.launcherBackgroundOpacity.state,
        heroOpacity = AllSettings.cryoHeroWallpaperOpacity.state,
        portraitOpacity = AllSettings.cryoPortraitWallpaperOpacity.state,
        fmOpacity = AllSettings.cryoFmBackgroundOpacity.state,
        fmVolume = AllSettings.cryoFmWallpaperVolume.state,
        customGradient = AllSettings.cryoCustomGradient.state,
        backgroundVolume = AllSettings.videoBackgroundVolume.state
    )

    private fun restoreSlot(ref: WallpaperRef?, target: File) {
        //Sem wallpaper na predefinição (ou arquivo sumiu): o espaço fica sem wallpaper, como estava ao salvar
        if (ref == null || !CryoWallpaperLibrary.restore(ref, target)) {
            FileUtils.deleteQuietly(target)
        }
    }

    private suspend fun persist(list: List<CryoPreset>) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val tmp = File(dir, "presets.json.tmp")
        tmp.writeText(json.encodeToString(PresetFile.serializer(), PresetFile(presets = list)))
        FileUtils.deleteQuietly(file)
        if (!tmp.renameTo(file)) {
            FileUtils.copyFile(tmp, file)
            FileUtils.deleteQuietly(tmp)
        }
    }
}
