package com.russoviller.cryogenic.utils.device

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object CryoUsageMonitor {
    private var lastCpuTicks = -1L
    private var lastCpuTime = 0L

    private fun processTicks(): Long? = runCatching {
        val stat = File("/proc/self/stat").readText()
        val fields = stat.substring(stat.lastIndexOf(')') + 2).split(' ')
        fields[11].toLong() + fields[12].toLong()
    }.getOrNull()

    @Synchronized
    fun cpuPercent(): Int? {
        val ticks = processTicks() ?: return null
        val now = SystemClock.elapsedRealtime()
        val previous = lastCpuTicks
        val previousTime = lastCpuTime
        lastCpuTicks = ticks
        lastCpuTime = now
        if (previous < 0 || now <= previousTime) return null
        val clk = runCatching { Os.sysconf(OsConstants._SC_CLK_TCK) }.getOrDefault(100L).coerceAtLeast(1L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val usedSeconds = (ticks - previous).toDouble() / clk
        val wallSeconds = (now - previousTime) / 1000.0
        return (usedSeconds / (wallSeconds * cores) * 100.0).toInt().coerceIn(0, 100)
    }

    class GpuReading(val percent: Int?, val mhz: Int?, val pending: Boolean = false)

    private val busyPaths = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/kernel/gpu/gpu_busy",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/devices/platform/mali.0/utilization",
        "/sys/devices/platform/gpusysfs/gpu_busy",
        "/sys/module/ged/parameters/gpu_loading",
        "/sys/kernel/ged/hal/gpu_utilization",
        "/proc/gpufreq/gpufreq_var_dump"
    )

    private val freqPairs = listOf(
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq" to "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
        "/sys/class/kgsl/kgsl-3d0/gpuclk" to "/sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "/sys/kernel/gpu/gpu_clock" to "/sys/kernel/gpu/gpu_max_clock",
        "/sys/class/misc/mali0/device/clock" to "/sys/class/misc/mali0/device/max_clock"
    )

    private val deniedPaths = HashSet<String>()

    private fun readText(path: String): String? {
        if (path in deniedPaths) return null
        return try {
            File(path).readText().trim().ifEmpty { null }
        } catch (e: Exception) {
            deniedPaths.add(path)
            null
        }
    }

    private fun numbers(text: String) = Regex("\\d+").findAll(text).map { it.value.toLong() }.toList()

    private fun parseBusy(path: String, text: String): Int? {
        val values = numbers(text)
        if (values.isEmpty()) return null
        return when {
            path.endsWith("gpubusy") && values.size >= 2 -> {
                if (values[1] <= 0L) 0 else (values[0] * 100 / values[1]).toInt()
            }
            path.endsWith("gpufreq_var_dump") -> Regex("gpu_loading\\s*=\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
            else -> values[0].toInt()
        }
    }

    private fun devfreqGpuDirs(): List<File> = runCatching {
        File("/sys/class/devfreq").listFiles()?.filter { dir ->
            val name = dir.name.lowercase()
            name.contains("kgsl") || name.contains("gpu") || name.contains("mali") || name.contains("g3d") || name.contains("pvr")
        }.orEmpty()
    }.getOrDefault(emptyList())

    private fun hz(value: Long): Int = when {
        value > 10_000_000L -> (value / 1_000_000L).toInt()
        value > 10_000L -> (value / 1_000L).toInt()
        else -> value.toInt()
    }

    private fun nativeGpuPercent(): Int? = runCatching {
        org.lwjgl.glfw.CallbackBridge.getGpuBusyPercent()
    }.getOrNull()

    fun setGpuTimer(enabled: Boolean) {
        runCatching { org.lwjgl.glfw.CallbackBridge.setGpuTimerEnabled(enabled) }
    }

    @Synchronized
    fun gpuReading(): GpuReading {
        val native = nativeGpuPercent()
        if (native != null && native >= 0) return GpuReading(native.coerceIn(0, 100), null)
        val sysfs = sysfsGpuReading()
        if (sysfs.percent == null && sysfs.mhz == null && native == -1) return GpuReading(null, null, pending = true)
        return sysfs
    }

    private fun sysfsGpuReading(): GpuReading {
        busyPaths.forEach { path ->
            val text = readText(path) ?: return@forEach
            parseBusy(path, text)?.let { return GpuReading(it.coerceIn(0, 100), null) }
        }
        devfreqGpuDirs().forEach { dir ->
            readText(File(dir, "load").path)?.let { text ->
                numbers(text).firstOrNull()?.let { return GpuReading(it.toInt().coerceIn(0, 100), null) }
            }
        }
        freqPairs.forEach { (curPath, maxPath) ->
            val cur = readText(curPath)?.let { numbers(it).firstOrNull() } ?: return@forEach
            val max = readText(maxPath)?.let { numbers(it).firstOrNull() }
            return GpuReading(max?.takeIf { it > 0 }?.let { (cur * 100 / it).toInt().coerceIn(0, 100) }, hz(cur))
        }
        devfreqGpuDirs().forEach { dir ->
            val cur = readText(File(dir, "cur_freq").path)?.let { numbers(it).firstOrNull() } ?: return@forEach
            val max = readText(File(dir, "max_freq").path)?.let { numbers(it).firstOrNull() }
            return GpuReading(max?.takeIf { it > 0 }?.let { (cur * 100 / it).toInt().coerceIn(0, 100) }, hz(cur))
        }
        return GpuReading(null, null)
    }

    fun gpuPercent(): Int? = gpuReading().percent
}

@Composable
fun rememberGpuReading(enabled: Boolean): State<CryoUsageMonitor.GpuReading?> =
    produceState<CryoUsageMonitor.GpuReading?>(null, enabled) {
        if (!enabled) {
            value = null
            return@produceState
        }
        CryoUsageMonitor.setGpuTimer(true)
        try {
            while (true) {
                value = withContext(Dispatchers.IO) { CryoUsageMonitor.gpuReading() }
                delay(1000)
            }
        } finally {
            CryoUsageMonitor.setGpuTimer(false)
        }
    }

@Composable
fun rememberUsagePercent(enabled: Boolean, reader: () -> Int?): State<Int?> =
    produceState<Int?>(null, enabled) {
        if (!enabled) {
            value = null
            return@produceState
        }
        while (true) {
            value = withContext(Dispatchers.IO) { reader() }
            delay(1000)
        }
    }
