package com.russoviller.cryogenic.game.cpu

import java.io.File

/** Opção "Núcleos de alto desempenho" (configurações do renderizador). */
enum class CpuAffinityMode {
    /** O Android decide (padrão). */
    OFF,

    /** O jogo inteiro roda só nos núcleos de desempenho (fora o cluster de eficiência). */
    PERFORMANCE,

    /** Igual a [PERFORMANCE] + a thread de renderização do Minecraft fica com o(s) núcleo(s) mais rápido(s) só para ela. */
    MAX
}

/**
 * Plano de afinidade calculado a partir da topologia da CPU.
 * @property gameCores núcleos para todas as threads do jogo
 * @property renderCores núcleos exclusivos da thread de renderização (vazio = sem fixar)
 */
data class CpuAffinityPlan(
    val mode: CpuAffinityMode,
    val gameCores: List<Int>,
    val renderCores: List<Int>,
    val description: String
) {
    val active: Boolean get() = mode != CpuAffinityMode.OFF && gameCores.isNotEmpty()

    /** Variáveis lidas pelo libpojavexec (cpu_affinity.c) antes de criar a JVM. */
    fun env(): Map<String, String> {
        if (!active) return emptyMap()
        val map = linkedMapOf("CRYO_AFFINITY_GAME" to gameCores.joinToString(","))
        if (renderCores.isNotEmpty()) map["CRYO_AFFINITY_RENDER"] = renderCores.joinToString(",")
        return map
    }
}

object CpuAffinity {
    /**
     * Capacidade relativa de cada núcleo: `cpu_capacity` (escala do EAS, mais fiel em big.LITTLE)
     * ou, sem ela, `cpuinfo_max_freq`. Núcleos sem nenhuma das duas ficam de fora.
     */
    fun readCoreScores(sysCpuDir: File = File("/sys/devices/system/cpu")): Map<Int, Long> {
        val cores = sysCpuDir.listFiles()
            ?.mapNotNull { f -> Regex("cpu(\\d+)").matchEntire(f.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.sorted()
            .orEmpty()
        fun read(file: File): Long? = runCatching { file.readText().trim().toLong() }.getOrNull()?.takeIf { it > 0 }
        val capacity = cores.associateWith { read(File(sysCpuDir, "cpu$it/cpu_capacity")) }
        val source: (Int) -> Long? = if (capacity.values.all { it != null } && capacity.isNotEmpty()) {
            { capacity[it] }
        } else {
            { read(File(sysCpuDir, "cpu$it/cpufreq/cpuinfo_max_freq")) }
        }
        return cores.mapNotNull { core -> source(core)?.let { core to it } }.toMap()
    }

    fun plan(mode: CpuAffinityMode, scores: Map<Int, Long>): CpuAffinityPlan {
        if (mode == CpuAffinityMode.OFF) return CpuAffinityPlan(mode, emptyList(), emptyList(), "off")
        if (scores.size < 2) return CpuAffinityPlan(CpuAffinityMode.OFF, emptyList(), emptyList(), "CPU topology unavailable")
        val min = scores.values.min()
        val max = scores.values.max()
        if (min == max) {
            return CpuAffinityPlan(CpuAffinityMode.OFF, emptyList(), emptyList(), "all cores are identical, nothing to do")
        }
        // núcleos de desempenho = todos fora do cluster mais lento
        val performance = scores.filterValues { it > min }.keys.sorted()
        if (mode == CpuAffinityMode.PERFORMANCE) {
            return CpuAffinityPlan(mode, performance, emptyList(), "game on cores ${performance.joinToString(",")}")
        }
        val prime = scores.filterValues { it == max }.keys.sorted()
        // só dedica os mais rápidos se sobrarem pelo menos 2 núcleos de desempenho para as outras threads
        val render = if (performance.size - prime.size >= 2) prime else emptyList()
        val desc = if (render.isEmpty()) {
            "game on cores ${performance.joinToString(",")} (not enough cores to dedicate the fastest one)"
        } else {
            "render thread on cores ${render.joinToString(",")}, other game threads on ${(performance - render.toSet()).joinToString(",")}"
        }
        return CpuAffinityPlan(mode, performance, render, desc)
    }
}
