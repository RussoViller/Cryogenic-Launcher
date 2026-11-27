package com.russoviller.cryogenic.game.input

import android.content.ContentResolver
import android.net.Uri
import android.os.Looper
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import com.russoviller.cryogenic.utils.logging.Logger
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Seletor de arquivos do Android para o `tinyfd_openFileDialog` do LWJGL dentro do jogo.
 *
 * No Android não existe zenity/kdialog: o tinyfd cai no modo console e sempre retorna null,
 * então mods que pedem "escolher arquivo" (ex.: Keyboard Sound Effects) nunca recebiam nada.
 * A JVM do jogo chama [open] (via libpojavexec), que abre o seletor do sistema, copia o(s)
 * arquivo(s) escolhido(s) para o cache do app e devolve o caminho real.
 */
@Keep
object GameFileDialog {
    private const val TAG = "GameFileDialog"
    private const val DIR_NAME = "file_dialog"
    private val KEEP_MS = TimeUnit.DAYS.toMillis(3)

    private class Request {
        val latch = CountDownLatch(1)
        @Volatile var result: String? = ""
        fun finish(value: String?) {
            result = value
            latch.countDown()
        }
    }

    private val lock = Any()
    private var pending: Request? = null
    private var activityRef: WeakReference<ComponentActivity>? = null
    private val seq = AtomicInteger()

    /** VMActivity.onCreate */
    @JvmStatic
    fun attach(activity: ComponentActivity) {
        synchronized(lock) { activityRef = WeakReference(activity) }
    }

    /** VMActivity.onDestroy: libera um pedido pendente para a thread do jogo não travar. */
    @JvmStatic
    fun detach(activity: ComponentActivity) {
        val request = synchronized(lock) {
            if (activityRef?.get() === activity) activityRef = null
            pending.also { pending = null }
        }
        request?.finish("")
    }

    /**
     * Chamado pela thread do jogo (bloqueia até o usuário escolher).
     * @return null = indisponível (o jogo segue com o tinyfd nativo), "" = cancelado,
     * senão os caminhos separados por '|' (formato do tinyfd)
     */
    @JvmStatic
    fun open(title: String?, defaultPath: String?, patterns: String?, description: String?, multiple: Boolean): String? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val request = Request()
        val activity = synchronized(lock) {
            val activity = activityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed } ?: return null
            if (pending != null) return "" // já existe um seletor aberto
            pending = request
            activity
        }
        val mimeTypes = mimeTypesFor(patterns)
        Logger.info(TAG, "open: title=$title, patterns=${patterns?.replace('\n', ' ')}, multiple=$multiple, mime=${mimeTypes.joinToString()}")
        activity.runOnUiThread {
            try {
                val key = "cryo_file_dialog_${seq.incrementAndGet()}"
                lateinit var launcher: ActivityResultLauncher<Array<String>>
                launcher = if (multiple) {
                    activity.activityResultRegistry.register(key, ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                        launcher.unregister()
                        deliver(activity, request, uris)
                    }
                } else {
                    activity.activityResultRegistry.register(key, ActivityResultContracts.OpenDocument()) { uri ->
                        launcher.unregister()
                        deliver(activity, request, listOfNotNull(uri))
                    }
                }
                launcher.launch(mimeTypes)
            } catch (t: Throwable) {
                Logger.error(TAG, "Failed to launch the system file picker", t)
                request.finish(null)
            }
        }
        request.latch.await()
        synchronized(lock) { if (pending === request) pending = null }
        Logger.info(TAG, "result: ${request.result}")
        return request.result
    }

    private fun deliver(activity: ComponentActivity, request: Request, uris: List<Uri>) {
        if (uris.isEmpty()) {
            request.finish("")
            return
        }
        Thread({
            val paths = runCatching {
                val root = File(activity.cacheDir, DIR_NAME)
                cleanup(root)
                val dir = File(root, "${System.currentTimeMillis()}_${seq.incrementAndGet()}").apply { mkdirs() }
                val used = HashSet<String>()
                uris.mapNotNull { uri -> copy(activity.contentResolver, uri, dir, used) }
            }.onFailure { Logger.error(TAG, "Failed to copy the picked file", it) }.getOrDefault(emptyList())
            request.finish(paths.joinToString("|"))
        }, "CryoFileDialog").start()
    }

    private fun copy(resolver: ContentResolver, uri: Uri, dir: File, used: MutableSet<String>): String? {
        return runCatching {
            val mime = resolver.getType(uri)
            var name = sanitizeFileName(queryDisplayName(resolver, uri) ?: uri.lastPathSegment, mime)
            if (!used.add(name)) {
                val dot = name.lastIndexOf('.')
                var i = 2
                while (true) {
                    val candidate = if (dot > 0) "${name.substring(0, dot)} ($i)${name.substring(dot)}" else "$name ($i)"
                    if (used.add(candidate)) { name = candidate; break }
                    i++
                }
            }
            val target = File(dir, name)
            val input = resolver.openInputStream(uri) ?: error("openInputStream returned null")
            input.use { src -> target.outputStream().use { dst -> src.copyTo(dst) } }
            target.absolutePath
        }.onFailure { Logger.error(TAG, "Failed to copy $uri", it) }.getOrNull()
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun cleanup(root: File) {
        val now = System.currentTimeMillis()
        root.listFiles()?.forEach { if (now - it.lastModified() > KEEP_MS) it.deleteRecursively() }
    }

    /** Nome seguro para o arquivo copiado; acrescenta a extensão do MIME quando faltar. */
    @VisibleForTesting
    internal fun sanitizeFileName(raw: String?, mime: String?): String {
        var name = (raw ?: "").substringAfterLast('/').substringAfterLast('\\')
            .map { c -> if (c < ' ' || c in "|\\/:*?\"<>") '_' else c }
            .joinToString("").trim().trim('.')
        if (name.isEmpty()) name = "file"
        if (name.length > 120) {
            val ext = name.substringAfterLast('.', "").take(10)
            name = name.take(100) + if (ext.isNotEmpty()) ".$ext" else ""
        }
        if (!name.contains('.') && mime != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { name += ".$it" }
        }
        return name
    }

    /** Filtros do tinyfd ("*.wav\n*.mp3") -> tipos MIME do seletor; sem filtro reconhecível -> todos. */
    @VisibleForTesting
    internal fun mimeTypesFor(patterns: String?): Array<String> {
        val list = patterns?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        if (list.isEmpty()) return arrayOf("*/*")
        val result = LinkedHashSet<String>()
        for (pattern in list) {
            if (!pattern.startsWith("*.")) return arrayOf("*/*")
            val ext = pattern.substring(2).lowercase(Locale.ROOT)
            if (ext.isEmpty() || ext.contains('*') || ext.contains('.')) return arrayOf("*/*")
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: return arrayOf("*/*")
            result += mime
            // provedores costumam reportar subtipos diferentes (ex.: audio/x-wav vs audio/wav)
            result += mime.substringBefore('/') + "/*"
            if (ext == "ogg" || ext == "oga") result += "application/ogg"
        }
        return result.toTypedArray()
    }
}
