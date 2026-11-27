package com.russoviller.cryogenic.extras

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.unit.AbstractSettingUnit
import com.russoviller.cryogenic.setting.unit.BooleanSettingUnit
import com.russoviller.cryogenic.setting.unit.EnumSettingUnit
import com.russoviller.cryogenic.setting.unit.FloatSettingUnit
import com.russoviller.cryogenic.setting.unit.IntSettingUnit
import com.russoviller.cryogenic.setting.unit.LongSettingUnit
import com.russoviller.cryogenic.setting.unit.NullableIntSettingUnit
import com.russoviller.cryogenic.setting.unit.OffsetSettingUnit
import com.russoviller.cryogenic.setting.unit.ParcelableSettingUnit
import com.russoviller.cryogenic.setting.unit.StringListSettingUnit
import com.russoviller.cryogenic.setting.unit.StringSettingUnit
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.ui.control.mouse.CursorHotspot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.apache.commons.io.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Uma configuração salva: todas as configurações do Launcher (renderizador, driver, Liquid Glass,
 * HUD, gráficos, controles...), MENOS o que é das Predefinições: os wallpapers, a opacidade e o
 * volume deles.
 */
@Serializable
data class CryoConfig(
    val id: String,
    val name: String,
    val createdAt: Long,
    /** chave da configuração -> valor */
    val values: Map<String, JsonElement> = emptyMap()
)

@Serializable
private data class ConfigFile(val version: Int = 1, val configs: List<CryoConfig> = emptyList())

/** Conteúdo de um arquivo .cryoconf (depois da assinatura). */
@Serializable
private data class ConfigExport(val format: String = "cryoconf", val version: Int = 1, val config: CryoConfig)

object CryoConfigs {
    const val NAME_MAX = 40
    const val EXTENSION = ".cryoconf"

    /**
     * Fora das configurações salvas:
     * - exclusivas das Predefinições: opacidade e volume dos wallpapers;
     * - estado interno do Launcher (conta e pasta atuais, predefinição em uso, avisos já vistos...);
     * - dados pessoais/secretos (chave de API).
     */
    val EXCLUDED_KEYS: Set<String> = setOf(
        //Predefinições
        "launcherBackgroundOpacity", "cryoHeroWallpaperOpacity", "cryoPortraitWallpaperOpacity",
        "cryoFmBackgroundOpacity", "cryoFmWallpaperVolume", "videoBackgroundVolume",
        "cryoActivePresetId", "cryoActivePresetSignature",
        //estado interno
        "currentAccount", "currentGamePathId", "launcherTaskMenuExpanded", "lastIgnoredVersion",
        "lastUpgradeCheck", "finishedGame", "lastSponsorReportMonth", "cryoSponsorIntroShown",
        "cryoSponsorSeen", "gamepadInputModePrompted",
        //secretos
        "cryoCurseForgeApiKey"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val lock = Mutex()

    private val dir: File get() = File(PathManager.DIR_FILES_PRIVATE, "configs")
    private val file: File get() = File(dir, "configs.json")

    /** Lista em memória, da mais nova para a mais antiga. */
    var configs by mutableStateOf<List<CryoConfig>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    class InvalidConfigFile : Exception("Not a Cryogenic config file")

    /** O arquivo escolhido não termina em .cryoconf. */
    class WrongExtension : Exception("Not a .cryoconf file")

    private val MAGIC = "CRYOCFG1".toByteArray(Charsets.US_ASCII)
    private const val IMPORT_MAX_BYTES = 2L * 1024 * 1024

    suspend fun load() = lock.withLock {
        if (loaded) return@withLock
        val list = withContext(Dispatchers.IO) {
            runCatching { json.decodeFromString(ConfigFile.serializer(), file.readText()).configs }.getOrDefault(emptyList())
        }
        withContext(Dispatchers.Main) {
            configs = list
            loaded = true
        }
    }

    // ------------------------------------------------------------------ valores

    private fun units(): List<AbstractSettingUnit<*>> =
        AllSettings.allUnits.filter { it.key !in EXCLUDED_KEYS }

    /** Valor atual de uma configuração, em JSON (null = tipo que não é salvo). */
    private fun encode(unit: AbstractSettingUnit<*>): JsonElement? = when (unit) {
        is BooleanSettingUnit -> JsonPrimitive(unit.getValue())
        is IntSettingUnit -> JsonPrimitive(unit.getValue())
        is NullableIntSettingUnit -> unit.getValue()?.let { JsonPrimitive(it) } ?: JsonNull
        is FloatSettingUnit -> JsonPrimitive(unit.getValue())
        is LongSettingUnit -> JsonPrimitive(unit.getValue())
        is StringSettingUnit -> JsonPrimitive(unit.getValue())
        is StringListSettingUnit -> JsonArray(unit.getValue().map { JsonPrimitive(it) })
        is OffsetSettingUnit -> JsonPrimitive(unit.getValue().packedValue)
        is EnumSettingUnit<*> -> JsonPrimitive(unit.getValue().name)
        is ParcelableSettingUnit<*> -> (unit.getValue() as? CursorHotspot)?.let {
            JsonObject(mapOf("x" to JsonPrimitive(it.xPercent), "y" to JsonPrimitive(it.yPercent)))
        }
        else -> null
    }

    /** Aplica um valor salvo; false se o valor não serve (tipo diferente, versão mais nova...). */
    @Suppress("UNCHECKED_CAST")
    private fun decodeAndSave(unit: AbstractSettingUnit<*>, value: JsonElement): Boolean = runCatching {
        val p = value as? JsonPrimitive
        when (unit) {
            is BooleanSettingUnit -> unit.save(p?.booleanOrNull ?: return false)
            is IntSettingUnit -> unit.save(p?.intOrNull ?: return false)
            is NullableIntSettingUnit -> unit.save(if (value is JsonNull) null else (p?.intOrNull ?: return false))
            is FloatSettingUnit -> unit.save(p?.floatOrNull ?: return false)
            is LongSettingUnit -> unit.save(p?.longOrNull ?: return false)
            is StringSettingUnit -> unit.save(p?.takeIf { it.isString }?.content ?: return false)
            is StringListSettingUnit -> unit.save((value as? JsonArray ?: return false).map { it.jsonPrimitive.content })
            is OffsetSettingUnit -> unit.save(Offset(p?.longOrNull ?: return false))
            is EnumSettingUnit<*> -> return unit.saveName(p?.takeIf { it.isString }?.content ?: return false)
            is ParcelableSettingUnit<*> -> {
                if (unit.defaultValue !is CursorHotspot) return false
                val o = value as? JsonObject ?: return false
                val x = o["x"]?.jsonPrimitive?.intOrNull ?: return false
                val y = o["y"]?.jsonPrimitive?.intOrNull ?: return false
                (unit as ParcelableSettingUnit<CursorHotspot>).save(
                    (unit.defaultValue as CursorHotspot).copy(xPercent = x.coerceIn(0, 100), yPercent = y.coerceIn(0, 100))
                )
            }
            else -> return false
        }
        true
    }.getOrDefault(false)

    /** Configurações atuais do Launcher (sem as das Predefinições). */
    fun snapshot(): Map<String, JsonElement> {
        val out = LinkedHashMap<String, JsonElement>()
        units().forEach { u -> encode(u)?.let { out[u.key] = it } }
        return out
    }

    /** true se a configuração salva é igual à que está em uso agora (só as chaves que ela tem). */
    fun isCurrent(config: CryoConfig, current: Map<String, JsonElement>): Boolean =
        current.isNotEmpty() && config.values.all { (k, v) -> k in EXCLUDED_KEYS || !current.containsKey(k) || current[k] == v }

    // ------------------------------------------------------------------ salvar / aplicar

    suspend fun saveCurrent(name: String): CryoConfig = lock.withLock {
        val values = withContext(Dispatchers.Main) { snapshot() }
        val config = CryoConfig(
            id = UUID.randomUUID().toString(),
            name = name.trim().take(NAME_MAX).ifEmpty { "Config" },
            createdAt = System.currentTimeMillis(),
            values = values
        )
        val updated = listOf(config) + configs
        persist(updated)
        withContext(Dispatchers.Main) { configs = updated }
        config
    }

    /** Aplica: grava cada configuração salva. Retorna quantas foram aplicadas. */
    suspend fun apply(config: CryoConfig): Int = withContext(Dispatchers.Main) {
        val byKey = units().associateBy { it.key }
        var applied = 0
        config.values.forEach { (key, value) ->
            if (key in EXCLUDED_KEYS) return@forEach
            val unit = byKey[key] ?: return@forEach
            if (decodeAndSave(unit, value)) applied++
        }
        //Mods do Launcher ativos fazem parte das configurações
        runCatching { com.russoviller.cryogenic.cmod.CmodManager.syncEnabled() }
        applied
    }

    suspend fun rename(id: String, name: String) = lock.withLock {
        val clean = name.trim().take(NAME_MAX)
        if (clean.isEmpty()) return@withLock
        val updated = configs.map { if (it.id == id) it.copy(name = clean) else it }
        persist(updated)
        withContext(Dispatchers.Main) { configs = updated }
    }

    suspend fun delete(id: String) = lock.withLock {
        val updated = configs.filterNot { it.id == id }
        persist(updated)
        withContext(Dispatchers.Main) { configs = updated }
    }

    fun suggestedName(base: String): String {
        val names = configs.map { it.name }.toSet()
        var n = configs.size + 1
        while ("$base $n" in names) n++
        return "$base $n"
    }

    // ------------------------------------------------------------------ exportar / importar (.cryoconf)
    //
    // Um .cryoconf é: 8 bytes de assinatura ("CRYOCFG1") + JSON (UTF-8) com o nome e os valores.
    // Só arquivos com o nome terminando em .cryoconf E com a assinatura são aceitos.

    /** "Minha config" -> "Minha-config.cryoconf". */
    fun exportFileName(config: CryoConfig): String {
        val clean = config.name.trim()
            .replace(Regex("""[\\/:*?"<>|\s]+"""), "-")
            .trim('-', '.')
            .ifEmpty { "config" }
        return "$clean$EXTENSION"
    }

    suspend fun export(context: Context, config: CryoConfig, uri: Uri) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open output")
        out.buffered().use { raw -> raw.write(encodeExport(config)) }
    }

    /** Nome do arquivo escolhido no seletor do sistema. */
    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    /** Lê um .cryoconf (sem aplicar) e adiciona à lista. */
    suspend fun import(context: Context, uri: Uri): CryoConfig = lock.withLock {
        val name = withContext(Dispatchers.IO) { displayName(context, uri) }.orEmpty()
        if (!name.lowercase().endsWith(EXTENSION)) throw WrongExtension()
        val imported = withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(1 shl 14)
                while (true) {
                    val r = input.read(buf)
                    if (r < 0) break
                    out.write(buf, 0, r)
                    if (out.size() > IMPORT_MAX_BYTES) throw InvalidConfigFile()
                }
                out.toByteArray()
            } ?: throw InvalidConfigFile()
            parseExport(bytes)
        }
        val wanted = imported.name.trim().take(NAME_MAX).ifEmpty { name.removeSuffix(EXTENSION).take(NAME_MAX) }
        val names = configs.map { it.name }.toSet()
        var finalName = wanted
        var n = 2
        while (finalName in names) finalName = "${wanted.take(NAME_MAX - 5)} ($n)".also { n++ }
        val config = imported.copy(
            id = UUID.randomUUID().toString(),
            name = finalName,
            createdAt = System.currentTimeMillis(),
            values = imported.values.filterKeys { it !in EXCLUDED_KEYS }
        )
        val updated = listOf(config) + configs
        persist(updated)
        withContext(Dispatchers.Main) { configs = updated }
        config
    }

    /** Assinatura + JSON -> configuração. */
    internal fun parseExport(bytes: ByteArray): CryoConfig {
        if (bytes.size <= MAGIC.size || !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw InvalidConfigFile()
        val text = bytes.copyOfRange(MAGIC.size, bytes.size).toString(Charsets.UTF_8)
        val export = runCatching { json.decodeFromString(ConfigExport.serializer(), text) }.getOrNull()
            ?: throw InvalidConfigFile()
        if (export.format != "cryoconf") throw InvalidConfigFile()
        return export.config
    }

    internal fun encodeExport(config: CryoConfig): ByteArray =
        MAGIC + json.encodeToString(ConfigExport.serializer(), ConfigExport(config = config.copy(id = ""))).toByteArray(Charsets.UTF_8)

    private suspend fun persist(list: List<CryoConfig>) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val tmp = File(dir, "configs.json.tmp")
        tmp.writeText(json.encodeToString(ConfigFile.serializer(), ConfigFile(configs = list)))
        FileUtils.deleteQuietly(file)
        if (!tmp.renameTo(file)) {
            FileUtils.copyFile(tmp, file)
            FileUtils.deleteQuietly(tmp)
        }
    }
}
