package com.russoviller.cryogenic.hud

import android.content.Context
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import com.russoviller.layer_controller.utils.toJsonString
import java.io.File

/** HUD de outro esquema enviado para o esquema errado. */
class HudSchemeMismatchException(val found: HudFormat, val target: HudScheme) :
    IllegalArgumentException("This HUD belongs to the ${found.label} system, it does not work in ${target.title}")

val HudScheme.title: String
    get() = when (this) {
        HudScheme.CRYOGENIC -> "Cryogenic"
        HudScheme.FCL -> "FCL"
        HudScheme.POJARUSH -> "PojaRush"
    }

object HudDetector {
    fun detect(root: JsonObject): HudFormat = when {
        root.containsKey("pojarush") -> HudFormat.POJARUSH
        root.containsKey("editorVersion") -> HudFormat.CRYOGENIC
        root.containsKey("viewGroups") -> HudFormat.FCL
        root.containsKey("mControlDataList") -> HudFormat.POJAV
        root.containsKey("buttons") && root.containsKey("sticks") -> HudFormat.POJARUSH
        root.containsKey("layers") && root.containsKey("info") -> HudFormat.CRYOGENIC
        else -> HudFormat.UNKNOWN
    }

    fun detect(text: String): HudFormat = runCatching { detect(HudJson.parseObject(text)) }.getOrDefault(HudFormat.UNKNOWN)
}

/** Arquivos de HUD dos esquemas FCL e PojaRush (o Cryogenic continua no ControlManager). */
object HudStore {
    private const val TAG = "HudStore"

    class Entry(val file: File, val layout: HudLayout?, val error: String? = null) {
        val name: String get() = layout?.name?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var appContext: Context? = null

    private val lists = HudScheme.entries.associateWith { MutableStateFlow<List<Entry>>(emptyList()) }
    private val loading = HudScheme.entries.associateWith { MutableStateFlow(false) }

    fun entries(scheme: HudScheme): StateFlow<List<Entry>> = lists.getValue(scheme).asStateFlow()
    fun isLoading(scheme: HudScheme): StateFlow<Boolean> = loading.getValue(scheme).asStateFlow()

    /** Esquema atual. */
    var scheme: HudScheme
        get() = HudScheme.of(AllSettings.hudScheme.getValue())
        set(value) { AllSettings.hudScheme.save(value.id) }

    fun init(context: Context) {
        appContext = context.applicationContext
        installLayerImages()
        scope.launch {
            refreshNow(HudScheme.FCL)
            refreshNow(HudScheme.POJARUSH)
        }
    }

    /** Imagens PNG nos botões do HUD Cryogenic (desenhadas pelo LayerController). */
    fun installLayerImages() {
        com.russoviller.layer_controller.utils.LayerImages.loader = { name -> HudImages.bitmap(HudScheme.CRYOGENIC, name) }
    }

    fun dir(scheme: HudScheme): File = when (scheme) {
        HudScheme.CRYOGENIC -> PathManager.DIR_CONTROL_LAYOUTS
        else -> File(PathManager.DIR_FILES_EXTERNAL, scheme.folder)
    }

    private fun setting(scheme: HudScheme) = when (scheme) {
        HudScheme.FCL -> AllSettings.hudLayoutFcl
        HudScheme.POJARUSH -> AllSettings.hudLayoutPojarush
        HudScheme.CRYOGENIC -> AllSettings.controlLayout
    }

    fun selectedName(scheme: HudScheme): String = setting(scheme).getValue()

    fun selectedFile(scheme: HudScheme): File? {
        val dir = dir(scheme)
        val name = setting(scheme).getValue()
        if (name.isNotBlank()) File(dir, name).takeIf { it.isFile }?.let { return it }
        return lists.getValue(scheme).value.firstOrNull { it.layout != null }?.file
            ?: dir.listFiles()?.filter { it.isFile && it.extension.equals("json", true) }?.minByOrNull { it.name }
    }

    fun select(scheme: HudScheme, file: File) {
        setting(scheme).save(file.name)
        if (scheme == HudScheme.CRYOGENIC) com.russoviller.cryogenic.game.control.ControlManager.refresh()
    }

    fun codec(scheme: HudScheme): HudCodec = requireNotNull(HudCodecs.of(scheme)) { "No codec for $scheme" }

    fun load(scheme: HudScheme, file: File): HudLayout {
        var text = file.readText()
        val format = HudDetector.detect(text)
        if (format != HudFormat.UNKNOWN && format.scheme != scheme) throw HudSchemeMismatchException(format, scheme)
        if (format == HudFormat.POJAV) throw HudSchemeMismatchException(format, scheme)
        if (scheme == HudScheme.CRYOGENIC) {
            // layouts antigos são atualizados para a versão atual antes de editar
            val version = runCatching { HudJson.parseObject(text).int("editorVersion", 0) }.getOrDefault(0)
            if (version < Zl2Codec.EDITOR_VERSION) {
                text = com.russoviller.layer_controller.layout.loadLayoutFromString(text).toJsonString()
            }
        }
        return codec(scheme).parse(text, file.nameWithoutExtension)
    }

    fun save(file: File, layout: HudLayout) {
        file.parentFile?.mkdirs()
        val text = codec(layout.scheme).write(layout)
        // o HUD Cryogenic precisa abrir no jogo: valida antes de gravar
        if (layout.scheme == HudScheme.CRYOGENIC) com.russoviller.layer_controller.layout.loadLayoutFromString(text)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
        if (layout.scheme == HudScheme.CRYOGENIC) com.russoviller.cryogenic.game.control.ControlManager.refresh()
    }

    private fun newFile(scheme: HudScheme): File {
        val dir = dir(scheme).apply { mkdirs() }
        var f: File
        do { f = File(dir, newHudId().take(8) + ".json") } while (f.exists())
        return f
    }

    fun refresh(scheme: HudScheme) {
        scope.launch { refreshNow(scheme) }
    }

    suspend fun refreshNow(scheme: HudScheme) {
        mutex.withLock {
            loading.getValue(scheme).value = true
            try {
                val dir = dir(scheme).apply { mkdirs() }
                if (scheme != HudScheme.CRYOGENIC && dir.listFiles()?.none { it.isFile && it.extension.equals("json", true) } != false) {
                    unpackDefaults(scheme)
                }
                val list = dir.listFiles()
                    ?.filter { it.isFile && it.extension.equals("json", true) }
                    ?.map { f ->
                        try {
                            Entry(f, load(scheme, f))
                        } catch (e: Exception) {
                            Logger.warning(TAG, "Failed to load HUD $f", e)
                            Entry(f, null, e.message ?: e.toString())
                        }
                    }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
                lists.getValue(scheme).value = list
                // seleção inválida → primeiro HUD que funciona
                val sel = setting(scheme).getValue()
                if (scheme != HudScheme.CRYOGENIC && list.none { it.file.name == sel && it.layout != null }) {
                    list.firstOrNull { it.layout != null }?.let { setting(scheme).save(it.file.name) }
                }
            } finally {
                loading.getValue(scheme).value = false
            }
        }
    }

    private fun unpackDefaults(scheme: HudScheme) {
        runCatching { save(newFile(scheme), HudDefaults.starter(scheme)) }
            .onFailure { Logger.warning(TAG, "Failed to create starter HUD", it) }
        if (scheme == HudScheme.FCL) {
            // HUD padrão do FoldCraft Launcher (GPLv3, créditos em assets/hud/NOTICE.txt)
            runCatching {
                val text = appContext?.assets?.open("hud/fcl_default.json")?.use { it.readBytes().decodeToString() } ?: return@runCatching
                newFile(scheme).writeText(text)
            }.onFailure { Logger.warning(TAG, "Failed to unpack FCL default HUD", it) }
        }
    }

    fun create(scheme: HudScheme, name: String, author: String, starter: Boolean): File {
        val file = newFile(scheme)
        val cryoDefault = if (starter && scheme == HudScheme.CRYOGENIC) {
            runCatching {
                appContext?.assets?.open("default_layout.json")?.use { it.readBytes().decodeToString() }
                    ?.let { Zl2Codec.parse(it, name).copy(name = name, author = author) }
            }.getOrNull()
        } else null
        val layout = cryoDefault ?: if (starter) HudDefaults.starter(scheme, name, author) else codec(scheme).empty(name, author)
        save(file, layout)
        refresh(scheme)
        return file
    }

    fun duplicate(scheme: HudScheme, entry: Entry): File? {
        val layout = entry.layout ?: return null
        val file = newFile(scheme)
        save(file, layout.copy(name = layout.name + " (2)"))
        refresh(scheme)
        return file
    }

    fun delete(scheme: HudScheme, entry: Entry) {
        entry.file.delete()
        refresh(scheme)
        if (scheme == HudScheme.CRYOGENIC) com.russoviller.cryogenic.game.control.ControlManager.refresh()
    }

    /** Arquivo para compartilhar: .zip com as imagens quando o HUD usa PNG, senão o próprio .json. */
    fun shareable(scheme: HudScheme, entry: Entry, cacheDir: File): File {
        val layout = entry.layout ?: return entry.file
        if (HudImages.refs(layout).none { HudImages.file(scheme, it) != null }) return entry.file
        val safe = layout.name.replace(Regex("[^A-Za-z0-9_\\- ]"), "_").trim().ifBlank { "HUD" }.take(40)
        return HudImages.pack(scheme, entry.file, layout, File(File(cacheDir, "hud_share").apply { mkdirs() }, "$safe.zip"))
    }

    sealed interface ImportResult {
        data class Ok(val name: String, val file: File) : ImportResult
        data class Mismatch(val found: HudFormat, val target: HudScheme) : ImportResult
        data class Invalid(val message: String) : ImportResult
    }

    /** Importa para FCL ou PojaRush. HUDs de outro esquema são recusados. */
    fun import(scheme: HudScheme, text: String, fileName: String): ImportResult {
        val format = HudDetector.detect(text)
        if (format == HudFormat.UNKNOWN) return ImportResult.Invalid("Unknown HUD format")
        if (format.scheme != scheme || format == HudFormat.POJAV) return ImportResult.Mismatch(format, scheme)
        return try {
            val layout = codec(scheme).parse(text, fileName.substringBeforeLast('.'))
            val file = newFile(scheme)
            // grava o texto original (nada se perde); o editor regrava mantendo os campos extras
            file.writeText(text)
            refresh(scheme)
            ImportResult.Ok(layout.name, file)
        } catch (e: Exception) {
            ImportResult.Invalid(e.message ?: e.toString())
        }
    }
}
