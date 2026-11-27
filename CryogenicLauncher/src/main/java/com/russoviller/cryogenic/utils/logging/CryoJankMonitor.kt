package com.russoviller.cryogenic.utils.logging

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Registro de travadas da interface.
 *
 * Uma thread auxiliar envia um "ping" para a thread principal; se ela demorar mais que
 * [THRESHOLD_MS] para responder, a interface estava travada: anota quanto tempo durou e o que a
 * thread principal estava executando naquele momento. O resultado vai para
 * `launcher_jank.log`, na pasta de logs do Launcher (entra junto no "compartilhar logs").
 *
 * Só roda com o Launcher na tela e custa praticamente nada (uma thread dormindo).
 */
object CryoJankMonitor {
    private const val THRESHOLD_MS = 120L
    private const val MAX_ENTRIES = 200
    private const val MAX_FILE_BYTES = 512L * 1024
    private const val MAX_FRAMES = 40

    private val main = Handler(Looper.getMainLooper())
    private val answered = AtomicLong(0)
    @Volatile private var thread: Thread? = null
    @Volatile private var logFile: File? = null
    private var entries = 0

    fun start(logsDir: File, versionName: String) {
        if (thread != null) return
        val file = File(logsDir, "launcher_jank.log")
        logFile = file
        val worker = Thread({ loop(file, versionName) }, "cryo-jank")
        worker.isDaemon = true
        worker.priority = Thread.MIN_PRIORITY
        thread = worker
        worker.start()
    }

    fun stop() {
        thread?.interrupt()
        thread = null
    }

    private fun loop(file: File, versionName: String) {
        runCatching {
            file.parentFile?.mkdirs()
            if (file.length() > MAX_FILE_BYTES) file.delete()
            file.appendText("==== ${now()} | $versionName | sessão iniciada ====\n")
        }
        val mainThread = Looper.getMainLooper().thread
        var ping = 0L
        try {
            while (!Thread.currentThread().isInterrupted) {
                ping++
                val id = ping
                val sentAt = SystemClock.uptimeMillis()
                main.post { answered.set(id) }
                Thread.sleep(THRESHOLD_MS)
                if (answered.get() >= id) continue

                //A thread principal não respondeu a tempo: guarda o que ela está fazendo agora
                val first = mainThread.stackTrace
                var second: Array<StackTraceElement>? = null
                while (answered.get() < id && SystemClock.uptimeMillis() - sentAt < 10_000L) {
                    Thread.sleep(40L)
                    if (second == null && answered.get() < id) second = mainThread.stackTrace
                }
                val duration = SystemClock.uptimeMillis() - sentAt
                if (entries < MAX_ENTRIES) {
                    entries++
                    write(file, duration, first, second)
                }
            }
        } catch (_: InterruptedException) {
        }
    }

    private fun write(
        file: File,
        duration: Long,
        first: Array<StackTraceElement>,
        second: Array<StackTraceElement>?
    ) {
        runCatching {
            val text = buildString {
                append("---- ").append(now()).append(" | travada de ~").append(duration).append(" ms ----\n")
                first.take(MAX_FRAMES).forEach { append("    at ").append(it).append('\n') }
                if (second != null && !second.contentEquals(first)) {
                    append("  (+40 ms)\n")
                    second.take(MAX_FRAMES).forEach { append("    at ").append(it).append('\n') }
                }
            }
            file.appendText(text)
        }
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
