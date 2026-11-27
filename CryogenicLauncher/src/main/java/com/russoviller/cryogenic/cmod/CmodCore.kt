package com.russoviller.cryogenic.cmod

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.russoviller.cryogenic.cmod.api.CryoModPrefs
import com.russoviller.cryogenic.cmod.api.CryoModTask
import com.russoviller.cryogenic.path.PathManager
import dalvik.system.DexClassLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

/** Pastas dos Mods do Launcher (iguais nos processos do Launcher e do jogo). */
internal object CmodPaths {
    val root: File get() = File(PathManager.DIR_FILES_PRIVATE, "cmods")
    val dataRoot: File get() = File(PathManager.DIR_FILES_PRIVATE, "cmods_data")
    val logRoot: File get() = File(PathManager.DIR_FILES_PRIVATE, "cmods_logs")

    fun modDir(id: String) = File(root, id)
    fun dataDir(id: String) = File(dataRoot, id).apply { mkdirs() }
    fun prefsFile(id: String) = File(dataDir(id), ".cryomod_prefs.properties")
    fun gameLogFile(id: String) = File(logRoot, "$id-game.log")

    /** Troca ${modDir}, ${dataDir} e ${gameDir} nos argumentos do cmod.json. */
    fun expand(arg: String, id: String, gameDir: File?): String = arg
        .replace("\${modDir}", modDir(id).absolutePath)
        .replace("\${dataDir}", dataDir(id).absolutePath)
        .replace("\${gameDir}", gameDir?.absolutePath.orEmpty())

    /** Lê os cmod.json de todos os mods instalados (sem carregar código). */
    fun readInstalled(): List<Pair<CmodInfo, File>> =
        root.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.mapNotNull { dir ->
            runCatching { CmodInfo.parse(File(dir, "cmod.json").readText()) to dir }.getOrNull()
        }.orEmpty()

    /**
     * Ordem de carregamento respeitando "dependencies": quem depende vem depois. Devolve a ordem e,
     * para quem não pode carregar, o id da dependência que falta.
     */
    fun order(infos: List<CmodInfo>, wanted: Set<String>): Pair<List<CmodInfo>, Map<String, String>> {
        val byId = infos.associateBy { it.id }
        val pending = infos.filter { it.id in wanted }.sortedBy { it.name.lowercase() }.toMutableList()
        val ok = LinkedHashMap<String, CmodInfo>()
        var progress = true
        while (progress && pending.isNotEmpty()) {
            progress = false
            val it = pending.iterator()
            while (it.hasNext()) {
                val info = it.next()
                if (info.dependencies.all { d -> d in ok }) {
                    ok[info.id] = info
                    it.remove()
                    progress = true
                }
            }
        }
        val missing = pending.associate { info ->
            info.id to (info.dependencies.firstOrNull { d -> d !in ok && (d !in byId || d !in wanted) }
                ?: info.dependencies.first { d -> d !in ok })
        }
        return ok.values.toList() to missing
    }
}

/** Log de cada mod (tela Mods do Launcher › Logs). */
internal object CmodLogs {
    private const val MAX = 300
    private val buffers = HashMap<String, ArrayDeque<String>>()
    private val main = Handler(Looper.getMainLooper())
    private val time = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.ROOT) }

    /** Muda a cada linha nova (para a tela atualizar). */
    var revision by mutableIntStateOf(0)
        private set

    fun line(level: Char, message: String) = "${time.get()!!.format(Date())} $level  $message"

    fun add(id: String, level: Char, message: String) {
        synchronized(buffers) {
            val buf = buffers.getOrPut(id) { ArrayDeque() }
            message.lineSequence().forEach { buf.addLast(line(level, it)) }
            while (buf.size > MAX) buf.removeFirst()
        }
        main.post { revision++ }
    }

    fun get(id: String): List<String> = synchronized(buffers) { buffers[id]?.toList().orEmpty() }

    fun clear(id: String) {
        synchronized(buffers) { buffers.remove(id) }
        runCatching { CmodPaths.gameLogFile(id).delete() }
        main.post { revision++ }
    }

    /** Linhas da última sessão de jogo do mod (lado do jogo). */
    fun gameLines(id: String): List<String> = runCatching {
        CmodPaths.gameLogFile(id).takeIf { it.isFile }?.readLines()?.takeLast(MAX)
    }.getOrNull().orEmpty()
}

/**
 * Configurações de um mod num arquivo .properties na pasta de dados: o Launcher e o jogo rodam em
 * processos diferentes, então o arquivo é relido quando muda.
 */
internal class CmodPrefs(private val file: File) : CryoModPrefs {
    private val props = Properties()
    private var stamp = -1L

    private fun load() {
        val now = if (file.isFile) file.lastModified() xor file.length() else 0L
        if (now == stamp) return
        props.clear()
        if (file.isFile) runCatching { file.inputStream().use { props.load(it) } }
        stamp = now
    }

    private fun write() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.outputStream().use { props.store(it, null) }
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        stamp = file.lastModified() xor file.length()
    }

    private fun raw(key: String): String? = synchronized(this) { load(); props.getProperty(key) }

    private fun put(key: String, value: String?) = synchronized(this) {
        load()
        if (value == null) props.remove(key) else props.setProperty(key, value)
        write()
    }

    override fun getString(key: String, def: String?): String? = raw(key) ?: def
    override fun getInt(key: String, def: Int): Int = raw(key)?.toIntOrNull() ?: def
    override fun getLong(key: String, def: Long): Long = raw(key)?.toLongOrNull() ?: def
    override fun getFloat(key: String, def: Float): Float = raw(key)?.toFloatOrNull() ?: def
    override fun getBoolean(key: String, def: Boolean): Boolean = raw(key)?.toBooleanStrictOrNull() ?: def
    override fun putString(key: String, value: String?) = put(key, value)
    override fun putInt(key: String, value: Int) = put(key, value.toString())
    override fun putLong(key: String, value: Long) = put(key, value.toString())
    override fun putFloat(key: String, value: Float) = put(key, value.toString())
    override fun putBoolean(key: String, value: Boolean) = put(key, value.toString())
    override fun contains(key: String): Boolean = raw(key) != null
    override fun remove(key: String) = put(key, null)
    override fun keys(): MutableSet<String> = synchronized(this) { load(); props.stringPropertyNames().toMutableSet() }
    override fun clear() = synchronized(this) { props.clear(); write() }
}

/** Tarefas agendadas de um mod, todas canceladas quando o mod para. */
internal class CmodScheduler(private val guard: (Runnable) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val tasks = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Task, Boolean>())

    @Volatile private var closed = false

    inner class Task(private val action: Runnable, private val period: Long) : CryoModTask, Runnable {
        @Volatile private var cancelled = false
        override fun run() {
            if (cancelled || closed) return
            if (period <= 0) tasks.remove(this)
            guard(action)
            if (period > 0 && !cancelled && !closed) main.postDelayed(this, period)
        }
        override fun cancel() {
            cancelled = true
            main.removeCallbacks(this)
            tasks.remove(this)
        }
        override fun isCancelled(): Boolean = cancelled
    }

    fun schedule(delay: Long, action: Runnable): CryoModTask = add(Task(action, 0), delay)

    fun repeat(initial: Long, period: Long, action: Runnable): CryoModTask = add(Task(action, period.coerceAtLeast(16)), initial)

    private fun add(task: Task, delay: Long): CryoModTask {
        if (closed) { task.cancel(); return task }
        tasks.add(task)
        main.postDelayed(task, delay.coerceAtLeast(0))
        return task
    }

    fun close() {
        closed = true
        tasks.toList().forEach { it.cancel() }
    }
}

/**
 * Carrega primeiro as classes do próprio mod (ele pode trazer as bibliotecas dele, inclusive Kotlin)
 * e só usa as do Launcher para o Android, o Java e a API dos mods.
 */
internal class CmodClassLoader(dexPath: String, parent: ClassLoader) : DexClassLoader(dexPath, null, null, parent) {
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

        /** ClassLoader com os classes*.dex do mod (deixados só-leitura: exigência do Android 14+). */
        fun forMod(dir: File): CmodClassLoader {
            val dexFiles = dir.listFiles { f -> f.name.matches(Regex("classes\\d*\\.dex")) }?.sortedBy { it.name }.orEmpty()
            if (dexFiles.isEmpty()) throw CmodManager.InvalidCmod("no classes.dex")
            dexFiles.forEach { it.setReadOnly() }
            return CmodClassLoader(dexFiles.joinToString(File.pathSeparator) { it.absolutePath }, CmodClassLoader::class.java.classLoader!!)
        }
    }
}
