package com.russoviller.cryogenic.cmod

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.cmod.api.CryoLauncherListener
import com.russoviller.cryogenic.cmod.api.CryoMod
import com.russoviller.cryogenic.cmod.api.CryoModApi
import com.russoviller.cryogenic.cmod.api.CryoModButton
import com.russoviller.cryogenic.cmod.api.CryoModInfo
import com.russoviller.cryogenic.cmod.api.CryoModMessageListener
import com.russoviller.cryogenic.cmod.api.CryoModPrefs
import com.russoviller.cryogenic.cmod.api.CryoModTask
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Dados do cmod.json. */
data class CmodInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    /** Classe do lado do Launcher (CryoMod). Vazio = mod só do jogo. */
    val main: String,
    val apiVersion: Int,
    val website: String = "",
    val dependencies: List<String> = emptyList(),
    /** Classe do lado do jogo (CryoGameMod). Vazio = mod só do Launcher. */
    val gameMain: String = "",
    /** Argumentos extras da JVM do jogo (-D, -X, -javaagent:...). */
    val jvmArgs: List<String> = emptyList(),
    /** Argumentos extras do Minecraft (no fim da linha de comando). */
    val gameArgs: List<String> = emptyList()
) {
    val hasLauncherSide: Boolean get() = main.isNotEmpty()
    val hasGameSide: Boolean get() = gameMain.isNotEmpty()

    companion object {
        private val ID = Regex("^[A-Za-z0-9_.-]{2,64}$")
        private val CLASS = Regex("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$")
        private val JVM_ALLOWED = listOf("-D", "-X", "-javaagent:", "--add-opens", "--add-exports", "--enable-native-access")

        private fun JSONObject.strings(key: String, max: Int, maxLen: Int): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i -> arr.optString(i).trim().takeIf { it.isNotEmpty() && it.length <= maxLen } }.take(max)
        }

        /** Lê e valida o cmod.json. */
        fun parse(text: String): CmodInfo {
            val o = JSONObject(text)
            val id = o.optString("id").trim()
            require(ID.matches(id)) { "invalid id" }
            val main = o.optString("main").trim()
            val gameMain = o.optString("gameMain").trim()
            require(main.isNotEmpty() || gameMain.isNotEmpty()) { "missing main" }
            require(main.isEmpty() || CLASS.matches(main)) { "invalid main" }
            require(gameMain.isEmpty() || CLASS.matches(gameMain)) { "invalid gameMain" }
            val jvmArgs = o.strings("jvmArgs", 32, 1024)
            jvmArgs.firstOrNull { a -> JVM_ALLOWED.none { a.startsWith(it) } }?.let { throw IllegalArgumentException("jvmArgs not allowed: $it") }
            val website = o.optString("website").trim().takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty().take(300)
            return CmodInfo(
                id = id,
                name = o.optString("name").trim().ifEmpty { id }.take(48),
                version = o.optString("version").trim().take(24),
                author = o.optString("author").trim().take(48),
                description = o.optString("description").trim().take(600),
                main = main,
                apiVersion = o.optInt("apiVersion", 1),
                website = website,
                dependencies = o.strings("dependencies", 16, 64).filter { ID.matches(it) && it != id }.distinct(),
                gameMain = gameMain,
                jvmArgs = jvmArgs,
                gameArgs = o.strings("gameArgs", 32, 1024)
            )
        }
    }
}

/** Um mod instalado. */
class InstalledCmod(val info: CmodInfo, val dir: File, val icon: Bitmap?, val sizeBytes: Long)

/** Botão registrado por um mod. */
class CmodButtonEntry(val modId: String, val modName: String, val modIcon: Bitmap?, val button: CryoModButton)

/**
 * Mods do Launcher (.cmod).
 *
 * Um .cmod é um zip com:
 * - cmod.json (id, name, version, author, description, main, gameMain, apiVersion, website,
 *   dependencies, jvmArgs, gameArgs);
 * - classes.dex (+ classes2.dex...) com o código (Java/Kotlin compilado com d8);
 * - icon.png (opcional), assets/ (opcional) e game/ (opcional: arquivos para o jogo, ex.: um
 *   -javaagent).
 *
 * Instalados em files/cmods/<id>/. Os ativados ficam em AllSettings.cryoEnabledMods (entram no
 * "Salvar Configurações"). O lado do jogo (gameMain) é carregado pelo CmodGameHost no processo do
 * jogo. O código roda dentro do Launcher, então só deve ser instalado mod de quem a pessoa confia:
 * a tela de importação avisa isso.
 */
object CmodManager {
    private const val TAG = "CryoMods"
    const val EXTENSION = ".cmod"
    private const val MAX_FILE = 64L * 1024 * 1024

    class WrongExtension : Exception("Not a .cmod file")
    class InvalidCmod(message: String) : Exception(message)

    private val main = Handler(Looper.getMainLooper())
    private val background = Executors.newCachedThreadPool { r -> Thread(r, "CryoMod").apply { isDaemon = true } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Instalados (ordem por nome). */
    val installed = mutableStateListOf<InstalledCmod>()

    /** Botões dos mods ativos (tela CryoMods). */
    val buttons = mutableStateListOf<CmodButtonEntry>()

    /** Selo de cada mod na tela CryoMods (setButtonBadge). */
    val badges = mutableStateMapOf<String, String>()

    /** Erro ao carregar/ativar, por id de mod. */
    val errors = mutableStateMapOf<String, String>()

    /** Ativos agora (inclusive os que só têm o lado do jogo). */
    val active = mutableStateListOf<String>()

    /** Mods rodando agora: id -> instância. */
    private val running = LinkedHashMap<String, CryoMod>()
    private val apis = LinkedHashMap<String, Api>()

    var ready by mutableStateOf(false)
        private set

    private var appContext: Context? = null

    /** Lê os mods instalados e ativa os marcados. Seguro chamar várias vezes (só roda uma). */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        reloadInstalled()
        syncEnabled()
        watchLauncher(context.applicationContext as? Application)
        ready = true
    }

    @androidx.annotation.VisibleForTesting
    internal fun reloadForTest() = reloadInstalled()

    fun enabledIds(): Set<String> = AllSettings.cryoEnabledMods.getValue().toSet()

    fun isEnabled(id: String): Boolean = id in AllSettings.cryoEnabledMods.state

    fun isActive(id: String): Boolean = id in active

    private fun reloadInstalled() {
        val list = CmodPaths.root.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.mapNotNull { dir ->
            runCatching {
                val info = CmodInfo.parse(File(dir, "cmod.json").readText())
                val icon = File(dir, "icon.png").takeIf { it.isFile }?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
                InstalledCmod(info, dir, icon, dir.walkTopDown().filter { it.isFile }.sumOf { it.length() })
            }.onFailure { Logger.warning(TAG, "Broken cmod at ${dir.name}: ${it.message}") }.getOrNull()
        }.orEmpty().sortedBy { it.info.name.lowercase() }
        installed.clear()
        installed.addAll(list)
    }

    /**
     * Deixa rodando exatamente os mods marcados como ativos, respeitando as dependências (ex.: depois
     * de aplicar uma configuração salva). Só na thread principal.
     */
    fun syncEnabled() {
        val wanted = enabledIds()
        val (order, missing) = CmodPaths.order(installed.map { it.info }, wanted)
        val allowed = order.map { it.id }.toSet()
        // para quem saiu (do último para o primeiro: dependentes antes das dependências)
        active.toList().reversed().filter { it !in allowed }.forEach { stopMod(it) }
        missing.forEach { (id, dep) -> errors[id] = "Missing dependency: $dep" }
        val byId = installed.associateBy { it.info.id }
        for (info in order) {
            if (info.id in active) continue
            if (info.dependencies.any { it !in active }) {
                errors[info.id] = "Dependency failed: ${info.dependencies.first { it !in active }}"
                continue
            }
            byId[info.id]?.let { startMod(it) }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val now = AllSettings.cryoEnabledMods.getValue().toMutableList()
        now.remove(id)
        if (enabled) now.add(id)
        AllSettings.cryoEnabledMods.save(now)
        if (enabled) errors.remove(id)
        val before = active.toSet()
        syncEnabled()
        (active.toSet() - before).forEach { on -> dispatch { if (it.getModId() != on) it.launcherListeners.forEach { l -> it.guard { l.onModEnabled(on) } } } }
        (before - active.toSet()).forEach { off -> dispatch { if (it.getModId() != off) it.launcherListeners.forEach { l -> it.guard { l.onModDisabled(off) } } } }
    }

    /** Reinicia o mod (relê o .cmod do disco: útil para quem está criando um mod). */
    fun restart(id: String) {
        val dependents = active.filter { a -> installed.firstOrNull { it.info.id == a }?.info?.dependencies?.contains(id) == true }
        dependents.reversed().forEach { stopMod(it) }
        stopMod(id)
        errors.remove(id)
        reloadInstalled()
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
        if (!info.hasLauncherSide) {
            //só o lado do jogo: fica ativo e é carregado quando o jogo abrir
            active.add(info.id)
            errors.remove(info.id)
            return
        }
        val api = Api(ctx, mod)
        runCatching {
            val loader = CmodClassLoader.forMod(mod.dir)
            val instance = loader.loadClass(info.main).getDeclaredConstructor().newInstance() as? CryoMod
                ?: throw InvalidCmod("${info.main} does not implement CryoMod")
            running[info.id] = instance
            apis[info.id] = api
            active.add(info.id)
            instance.onEnable(api)
            errors.remove(info.id)
            CmodLogs.add(info.id, 'I', "Enabled ${info.name} ${info.version}")
            Logger.info(TAG, "Enabled ${info.id} ${info.version}")
        }.onFailure { t ->
            Logger.error(TAG, "Failed to enable ${info.id}", t)
            running.remove(info.id)
            apis.remove(info.id)?.dispose()
            api.dispose()
            active.remove(info.id)
            removeButtons(info.id)
            val msg = (t.cause ?: t).let { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }.take(240)
            errors[info.id] = msg
            CmodLogs.add(info.id, 'E', msg)
        }
    }

    private fun stopMod(id: String) {
        val instance = running.remove(id)
        runCatching { instance?.onDisable() }.onFailure {
            Logger.error(TAG, "onDisable failed for $id", it)
            CmodLogs.add(id, 'E', "onDisable: ${it.javaClass.simpleName}: ${it.message.orEmpty()}")
        }
        apis.remove(id)?.dispose()
        active.remove(id)
        removeButtons(id)
        badges.remove(id)
    }

    private fun removeButtons(id: String) {
        buttons.removeAll { it.modId == id }
    }

    /** Chama a ação de um botão protegendo o Launcher de erros do mod. */
    fun safeRun(entry: CmodButtonEntry, block: () -> Unit) {
        runCatching(block).onFailure { t ->
            Logger.error(TAG, "Mod ${entry.modId} failed", t)
            CmodLogs.add(entry.modId, 'E', "${t.javaClass.simpleName}: ${t.message.orEmpty()}")
            appContext?.let { Toast.makeText(it, "${entry.modName}: ${t.javaClass.simpleName} ${t.message.orEmpty()}".take(200), Toast.LENGTH_LONG).show() }
        }
    }

    private fun dispatch(block: (Api) -> Unit) {
        val run = { apis.values.toList().forEach { block(it) } }
        if (Looper.myLooper() == Looper.getMainLooper()) run() else main.post(run)
    }

    // ------------------------------------------------------------------ eventos do Launcher

    /** O jogo vai abrir (chamado pelo runGame). */
    fun onGameLaunch(versionName: String) {
        dispatch { api -> api.launcherListeners.forEach { l -> api.guard { l.onGameLaunch(versionName) } } }
    }

    private fun watchLauncher(app: Application?) {
        app?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: Activity) {
                if (started++ == 0) dispatch { api -> api.launcherListeners.forEach { l -> api.guard { l.onLauncherForeground() } } }
            }
            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                if (started == 0) dispatch { api -> api.launcherListeners.forEach { l -> api.guard { l.onLauncherBackground() } } }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        scope.launch {
            VersionsManager.currentVersion.map { it?.getVersionName() }.distinctUntilChanged().drop(1).collect { name ->
                dispatch { api -> api.launcherListeners.forEach { l -> api.guard { l.onGameVersionSelected(name) } } }
            }
        }
    }

    // ------------------------------------------------------------------ instalar / apagar / exportar

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
            val wasActive = info.id in active
            if (wasActive) stopMod(info.id)
            reloadInstalled()
            errors.remove(info.id)
            if (wasActive) syncEnabled()
        }
        return info
    }

    /** Extrai o zip numa pasta temporária, valida e troca a pasta do mod. */
    internal fun install(input: InputStream): CmodInfo {
        val root = CmodPaths.root
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
                        path.matches(Regex("classes\\d*\\.dex")) || path.startsWith("assets/") || path.startsWith("game/")
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
            CmodPaths.dataDir(info.id)
            return info
        } finally {
            if (tmp.exists()) tmp.walkBottomUp().forEach { it.setWritable(true); it.delete() }
        }
    }

    /** Desativa e apaga o mod (os dados dele também). */
    fun delete(id: String) {
        val now = AllSettings.cryoEnabledMods.getValue().filter { it != id }
        AllSettings.cryoEnabledMods.save(now)
        stopMod(id)
        File(CmodPaths.root, id).takeIf { it.exists() }?.walkBottomUp()?.forEach { it.setWritable(true); it.delete() }
        File(CmodPaths.dataRoot, id).takeIf { it.exists() }?.deleteRecursively()
        CmodLogs.clear(id)
        errors.remove(id)
        reloadInstalled()
        syncEnabled()
    }

    /** Apaga só os dados salvos do mod (configurações e arquivos), reiniciando-o se estiver ativo. */
    fun clearData(id: String) {
        val wasActive = id in active
        if (wasActive) stopMod(id)
        File(CmodPaths.dataRoot, id).takeIf { it.exists() }?.deleteRecursively()
        CmodPaths.dataDir(id)
        if (wasActive) syncEnabled()
    }

    /** Empacota o mod instalado de novo num .cmod (para compartilhar). */
    suspend fun export(context: Context, id: String): File = withContext(Dispatchers.IO) {
        val mod = installed.firstOrNull { it.info.id == id } ?: throw InvalidCmod("not installed")
        val dir = File(context.cacheDir, "cmods_export").apply { deleteRecursively(); mkdirs() }
        val name = "${mod.info.name.replace(Regex("[^A-Za-z0-9 _.-]"), "").trim().ifEmpty { id }}${mod.info.version.takeIf { it.isNotEmpty() }?.let { "-$it" }.orEmpty()}$EXTENSION"
        val out = File(dir, name)
        ZipOutputStream(out.outputStream().buffered()).use { zip ->
            mod.dir.walkTopDown().filter { it.isFile }.forEach { f ->
                zip.putNextEntry(ZipEntry(f.relativeTo(mod.dir).invariantSeparatorsPath))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        out
    }

    /** Abre o compartilhar do Android com o arquivo exportado. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/octet-stream")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun infoOf(mod: InstalledCmod) = CryoModInfo(
        mod.info.id, mod.info.name, mod.info.version, mod.info.author, mod.info.description,
        mod.info.website, mod.info.apiVersion, mod.info.id in active, mod.info.hasGameSide
    )

    // ------------------------------------------------------------------ API para o mod

    private class Api(private val context: Context, private val mod: InstalledCmod) : CryoModApi {
        val messageListeners = CopyOnWriteArrayList<CryoModMessageListener>()
        val launcherListeners = CopyOnWriteArrayList<CryoLauncherListener>()
        val services = java.util.concurrent.ConcurrentHashMap<String, Any>()
        private val scheduler = CmodScheduler { r -> guard { r.run() } }
        private val prefs by lazy { CmodPrefs(CmodPaths.prefsFile(mod.info.id)) }
        @Volatile private var disposed = false

        fun guard(block: () -> Unit) {
            runCatching(block).onFailure {
                Logger.error("CryoMod/${mod.info.id}", "Mod task failed", it)
                CmodLogs.add(mod.info.id, 'E', "${it.javaClass.simpleName}: ${it.message.orEmpty()}")
            }
        }

        fun dispose() {
            disposed = true
            scheduler.close()
            messageListeners.clear()
            launcherListeners.clear()
            services.clear()
        }

        override fun getApiVersion(): Int = CryoModApi.API_VERSION
        override fun getLauncherVersion(): String = BuildConfig.VERSION_NAME
        override fun getContext(): Context = context
        override fun getModId(): String = mod.info.id
        override fun getModDirectory(): File = mod.dir
        override fun getDataDirectory(): File = CmodPaths.dataDir(mod.info.id)
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
                if (disposed || mod.info.id !in running) return@runOnUiThread
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
            CmodLogs.add(mod.info.id, 'I', message)
        }

        override fun runOnUiThread(action: Runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) guard { action.run() } else main.post { guard { action.run() } }
        }

        override fun runInBackground(action: Runnable) {
            background.execute { guard { action.run() } }
        }

        // ---------------------------------------------------------------- API 2

        override fun getPreferences(): CryoModPrefs = prefs
        override fun schedule(delayMs: Long, action: Runnable): CryoModTask = scheduler.schedule(delayMs, action)
        override fun scheduleRepeating(initialDelayMs: Long, periodMs: Long, action: Runnable): CryoModTask =
            scheduler.repeat(initialDelayMs, periodMs, action)

        override fun openUrl(url: String) {
            runOnUiThread {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        override fun copyToClipboard(text: String) {
            runOnUiThread {
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(mod.info.name, text))
            }
        }

        override fun shareText(text: String) {
            runOnUiThread {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, mod.info.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        override fun setButtonBadge(badge: String?) {
            runOnUiThread {
                if (disposed) return@runOnUiThread
                val b = badge?.trim()?.take(8)
                if (b.isNullOrEmpty()) badges.remove(mod.info.id) else badges[mod.info.id] = b
            }
        }

        override fun getLanguage(): String = runCatching { context.resources.configuration.locales[0].language }.getOrDefault("en")

        override fun getInstalledMods(): List<CryoModInfo> = installed.toList().map { infoOf(it) }

        override fun isModEnabled(modId: String): Boolean = modId in active

        override fun getGameVersions(): List<String> = VersionsManager.versions.value.map { it.getVersionName() }

        override fun getSelectedGameVersion(): String? = VersionsManager.currentVersion.value?.getVersionName()

        override fun selectGameVersion(name: String): Boolean {
            if (VersionsManager.versions.value.none { it.getVersionName() == name }) return false
            runOnUiThread { VersionsManager.saveCurrentVersion(name) }
            return true
        }

        override fun sendMessage(targetModId: String, channel: String, payload: Any?): Boolean {
            val target = apis[targetModId] ?: return false
            main.post { target.deliver(mod.info.id, channel, payload) }
            return true
        }

        override fun broadcast(channel: String, payload: Any?) {
            main.post { apis.values.toList().filter { it !== this }.forEach { it.deliver(mod.info.id, channel, payload) } }
        }

        fun deliver(from: String, channel: String, payload: Any?) {
            if (disposed) return
            messageListeners.forEach { l -> guard { l.onMessage(from, channel, payload) } }
        }

        override fun addMessageListener(listener: CryoModMessageListener) { if (!disposed) messageListeners.addIfAbsent(listener) }
        override fun removeMessageListener(listener: CryoModMessageListener) { messageListeners.remove(listener) }

        override fun exposeService(name: String, service: Any?) {
            if (disposed) return
            if (service == null) services.remove(name) else services[name] = service
        }

        override fun getService(modId: String, name: String): Any? = apis[modId]?.services?.get(name)

        override fun addLauncherListener(listener: CryoLauncherListener) { if (!disposed) launcherListeners.addIfAbsent(listener) }
        override fun removeLauncherListener(listener: CryoLauncherListener) { launcherListeners.remove(listener) }
    }
}
