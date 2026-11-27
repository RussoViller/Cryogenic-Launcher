package com.russoviller.cryogenic.cmod

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.cmod.api.CryoMod
import com.russoviller.cryogenic.cmod.api.CryoModApi
import com.russoviller.cryogenic.cmod.api.CryoModButton
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

/** Dados do cmod.json. */
data class CmodInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val main: String,
    val apiVersion: Int
) {
    companion object {
        private val ID = Regex("^[A-Za-z0-9_.-]{2,64}$")

        /** Lê e valida o cmod.json. */
        fun parse(text: String): CmodInfo {
            val o = JSONObject(text)
            val id = o.optString("id").trim()
            require(ID.matches(id)) { "invalid id" }
            val main = o.optString("main").trim()
            require(main.isNotEmpty()) { "missing main" }
            return CmodInfo(
                id = id,
                name = o.optString("name").trim().ifEmpty { id }.take(48),
                version = o.optString("version").trim().take(24),
                author = o.optString("author").trim().take(48),
                description = o.optString("description").trim().take(300),
                main = main,
                apiVersion = o.optInt("apiVersion", 1)
            )
        }
    }
}

/** Um mod instalado. */
class InstalledCmod(val info: CmodInfo, val dir: File, val icon: Bitmap?)

/** Botão registrado por um mod. */
class CmodButtonEntry(val modId: String, val modName: String, val modIcon: Bitmap?, val button: CryoModButton)

/**
 * Mods do Launcher (.cmod).
 *
 * Um .cmod é um zip com:
 * - cmod.json (id, name, version, author, description, main, apiVersion);
 * - classes.dex (+ classes2.dex...) com o código (Java/Kotlin compilado com d8);
 * - icon.png (opcional) e assets/ (opcional).
 *
 * Instalados em files/cmods/<id>/. Os ativados ficam em AllSettings.cryoEnabledMods (entram no
 * "Salvar Configurações"). O código roda dentro do Launcher, então só deve ser instalado mod de
 * quem a pessoa confia: a tela de importação avisa isso.
 */
object CmodManager {
    private const val TAG = "CryoMods"
    const val EXTENSION = ".cmod"
    private const val MAX_FILE = 64L * 1024 * 1024

    class WrongExtension : Exception("Not a .cmod file")
    class InvalidCmod(message: String) : Exception(message)

    private val main = Handler(Looper.getMainLooper())
    private val background = Executors.newCachedThreadPool { r -> Thread(r, "CryoMod").apply { isDaemon = true } }

    private val root: File get() = File(PathManager.DIR_FILES_PRIVATE, "cmods")
    private val dataRoot: File get() = File(PathManager.DIR_FILES_PRIVATE, "cmods_data")

    /** Instalados (ordem por nome). */
    val installed = mutableStateListOf<InstalledCmod>()

    /** Botões dos mods ativos (tela CryoMods). */
    val buttons = mutableStateListOf<CmodButtonEntry>()

    /** Erro ao carregar/ativar, por id de mod. */
    val errors = mutableStateMapOf<String, String>()

    /** Mods rodando agora: id -> instância. */
    private val running = LinkedHashMap<String, CryoMod>()

    var ready by mutableStateOf(false)
        private set

    private var appContext: Context? = null

    /** Lê os mods instalados e ativa os marcados. Seguro chamar várias vezes (só roda uma). */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        reloadInstalled()
        syncEnabled()
        ready = true
    }

    @androidx.annotation.VisibleForTesting
    internal fun reloadForTest() = reloadInstalled()

    fun enabledIds(): Set<String> = AllSettings.cryoEnabledMods.getValue().toSet()

    fun isEnabled(id: String): Boolean = id in AllSettings.cryoEnabledMods.state

    private fun reloadInstalled() {
        val list = root.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            runCatching {
                val info = CmodInfo.parse(File(dir, "cmod.json").readText())
                val icon = File(dir, "icon.png").takeIf { it.isFile }?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
                InstalledCmod(info, dir, icon)
            }.onFailure { Logger.warning(TAG, "Broken cmod at ${dir.name}: ${it.message}") }.getOrNull()
        }.orEmpty().sortedBy { it.info.name.lowercase() }
        installed.clear()
        installed.addAll(list)
    }

    /**
     * Deixa rodando exatamente os mods marcados como ativos (ex.: depois de aplicar uma configuração
     * salva). Só na thread principal.
     */
    fun syncEnabled() {
        val wanted = enabledIds()
        running.keys.toList().filter { it !in wanted }.forEach { stopMod(it) }
        installed.filter { it.info.id in wanted && it.info.id !in running }.forEach { startMod(it) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val now = AllSettings.cryoEnabledMods.getValue().toMutableList()
        now.remove(id)
        if (enabled) now.add(id)
        AllSettings.cryoEnabledMods.save(now)
        if (enabled) errors.remove(id)
        syncEnabled()
    }

    // ------------------------------------------------------------------ executar

    private fun startMod(mod: InstalledCmod) {
        val ctx = appContext ?: return
        val info = mod.info
        if (info.apiVersion > CryoModApi.API_VERSION) {
            errors[info.id] = "API ${info.apiVersion} > ${CryoModApi.API_VERSION}"
            return
        }
        runCatching {
            val dexFiles = mod.dir.listFiles { f -> f.name.matches(Regex("classes\\d*\\.dex")) }?.sortedBy { it.name }.orEmpty()
            if (dexFiles.isEmpty()) throw InvalidCmod("no classes.dex")
            // Android 14+: código carregado precisa estar só-leitura
            dexFiles.forEach { it.setReadOnly() }
            val loader = CmodClassLoader(dexFiles.joinToString(File.pathSeparator) { it.absolutePath }, CmodManager::class.java.classLoader!!)
            val instance = loader.loadClass(info.main).getDeclaredConstructor().newInstance() as? CryoMod
                ?: throw InvalidCmod("${info.main} does not implement CryoMod")
            running[info.id] = instance
            instance.onEnable(Api(ctx, mod))
            errors.remove(info.id)
            Logger.info(TAG, "Enabled ${info.id} ${info.version}")
        }.onFailure { t ->
            Logger.error(TAG, "Failed to enable ${info.id}", t)
            running.remove(info.id)
            removeButtons(info.id)
            errors[info.id] = (t.cause ?: t).let { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }.take(240)
        }
    }

    private fun stopMod(id: String) {
        val instance = running.remove(id)
        runCatching { instance?.onDisable() }.onFailure { Logger.error(TAG, "onDisable failed for $id", it) }
        removeButtons(id)
    }

    private fun removeButtons(id: String) {
        buttons.removeAll { it.modId == id }
    }

    /** Chama a ação de um botão protegendo o Launcher de erros do mod. */
    fun safeRun(entry: CmodButtonEntry, block: () -> Unit) {
        runCatching(block).onFailure { t ->
            Logger.error(TAG, "Mod ${entry.modId} failed", t)
            appContext?.let { Toast.makeText(it, "${entry.modName}: ${t.javaClass.simpleName} ${t.message.orEmpty()}".take(200), Toast.LENGTH_LONG).show() }
        }
    }

    // ------------------------------------------------------------------ instalar / apagar

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    /** Instala (ou atualiza) um .cmod. Não ativa: a pessoa ativa na lista. */
    suspend fun import(context: Context, uri: Uri): CmodInfo {
        val name = withContext(Dispatchers.IO) { displayName(context, uri) }.orEmpty()
        if (!name.lowercase().endsWith(EXTENSION)) throw WrongExtension()
        val info = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { install(it) } ?: throw InvalidCmod("cannot open file")
        }
        withContext(Dispatchers.Main) {
            val wasRunning = info.id in running
            if (wasRunning) stopMod(info.id)
            reloadInstalled()
            errors.remove(info.id)
            if (wasRunning) syncEnabled()
        }
        return info
    }

    /** Extrai o zip numa pasta temporária, valida e troca a pasta do mod. */
    internal fun install(input: InputStream): CmodInfo {
        root.mkdirs()
        val tmp = File(root, ".install-${System.nanoTime()}")
        tmp.mkdirs()
        try {
            var total = 0L
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val path = entry.name.replace('\\', '/').trimStart('/')
                    if (entry.isDirectory || path.isEmpty()) continue
                    // só os arquivos do formato; nada fora da pasta do mod
                    if (path.contains("..")) throw InvalidCmod("invalid path")
                    val allowed = path == "cmod.json" || path == "icon.png" ||
                        path.matches(Regex("classes\\d*\\.dex")) || path.startsWith("assets/")
                    if (!allowed) continue
                    val out = File(tmp, path)
                    if (!out.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) throw InvalidCmod("invalid path")
                    out.parentFile?.mkdirs()
                    out.outputStream().use { o ->
                        val buf = ByteArray(1 shl 16)
                        while (true) {
                            val n = zip.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > MAX_FILE) throw InvalidCmod("too big")
                            o.write(buf, 0, n)
                        }
                    }
                }
            }
            val json = File(tmp, "cmod.json")
            if (!json.isFile) throw InvalidCmod("missing cmod.json")
            val info = runCatching { CmodInfo.parse(json.readText()) }.getOrElse { throw InvalidCmod("invalid cmod.json: ${it.message}") }
            if (File(tmp, "classes.dex").length() <= 0L) throw InvalidCmod("missing classes.dex")
            val target = File(root, info.id)
            if (target.exists()) {
                target.walkBottomUp().forEach { it.setWritable(true); it.delete() }
            }
            if (!tmp.renameTo(target)) throw InvalidCmod("cannot install")
            File(dataRoot, info.id).mkdirs()
            return info
        } finally {
            if (tmp.exists()) tmp.walkBottomUp().forEach { it.setWritable(true); it.delete() }
        }
    }

    /** Desativa e apaga o mod (os dados dele também). */
    fun delete(id: String) {
        stopMod(id)
        val now = AllSettings.cryoEnabledMods.getValue().filter { it != id }
        AllSettings.cryoEnabledMods.save(now)
        File(root, id).takeIf { it.exists() }?.walkBottomUp()?.forEach { it.setWritable(true); it.delete() }
        File(dataRoot, id).takeIf { it.exists() }?.deleteRecursively()
        errors.remove(id)
        reloadInstalled()
    }

    // ------------------------------------------------------------------ API para o mod

    private class Api(private val context: Context, private val mod: InstalledCmod) : CryoModApi {
        override fun getApiVersion(): Int = CryoModApi.API_VERSION
        override fun getLauncherVersion(): String = BuildConfig.VERSION_NAME
        override fun getContext(): Context = context
        override fun getModId(): String = mod.info.id
        override fun getModDirectory(): File = mod.dir
        override fun getDataDirectory(): File = File(dataRoot, mod.info.id).apply { mkdirs() }
        override fun getGameDirectory(): File? = runCatching { File(GamePathManager.currentPath.value) }.getOrNull()

        /** Id da função deste mod (cada mod tem uma só). */
        @Volatile private var functionId: String? = null

        override fun addButton(button: CryoModButton) {
            synchronized(this) {
                val current = functionId
                if (current != null && current != button.id) {
                    throw IllegalStateException("Each Launcher Mod can add only one function (already has \"$current\")")
                }
                functionId = button.id
            }
            runOnUiThread {
                if (mod.info.id !in running) return@runOnUiThread
                val entry = CmodButtonEntry(mod.info.id, mod.info.name, mod.icon, button)
                val index = buttons.indexOfFirst { it.modId == mod.info.id }
                if (index >= 0) buttons[index] = entry else buttons.add(entry)
            }
        }

        override fun removeButton(id: String) {
            synchronized(this) { if (functionId == id) functionId = null }
            runOnUiThread { buttons.removeAll { it.modId == mod.info.id && it.button.id == id } }
        }

        override fun toast(message: String) {
            runOnUiThread { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }

        override fun log(message: String) {
            Logger.info("CryoMod/${mod.info.id}", message)
        }

        override fun runOnUiThread(action: Runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) safe(action) else main.post { safe(action) }
        }

        override fun runInBackground(action: Runnable) {
            background.execute { safe(action) }
        }

        private fun safe(action: Runnable) {
            runCatching { action.run() }.onFailure { Logger.error("CryoMod/${mod.info.id}", "Mod task failed", it) }
        }
    }
}

/**
 * Carrega primeiro as classes do próprio mod (ele pode trazer as bibliotecas dele, inclusive Kotlin)
 * e só usa as do Launcher para o Android, o Java e a API dos mods.
 */
private class CmodClassLoader(dexPath: String, parent: ClassLoader) : DexClassLoader(dexPath, null, null, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }
        if (PARENT_FIRST.any { name.startsWith(it) }) return super.loadClass(name, resolve)
        return try {
            findClass(name)
        } catch (e: ClassNotFoundException) {
            super.loadClass(name, resolve)
        }
    }

    companion object {
        private val PARENT_FIRST = listOf(
            "java.", "javax.", "android.", "dalvik.", "org.json.", "org.xml.", "org.w3c.", "sun.",
            "com.russoviller.cryogenic.cmod.api."
        )
    }
}
