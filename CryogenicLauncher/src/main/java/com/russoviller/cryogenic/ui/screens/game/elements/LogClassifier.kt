package com.russoviller.cryogenic.ui.screens.game.elements

/**
 * Separa as linhas do log do jogo em categorias simples, para quem não entende log
 * conseguir achar rápido o que importa: erros, avisos e eventos importantes.
 *
 * Usado só pela thread de flush do LogBox (não é thread-safe por causa do estado [previous]).
 */
internal class LogClassifier {
    enum class Kind { NORMAL, IMPORTANT, WARN, ERROR }

    /** Resultado: categoria + se a linha é a "manchete" de um erro (não uma linha de stack trace). */
    data class Result(val kind: Kind, val headline: Boolean)

    private var previous: Kind = Kind.NORMAL

    fun reset() {
        previous = Kind.NORMAL
    }

    fun classify(raw: String): Result {
        val trimmed = raw.trimStart()
        if (trimmed.isEmpty()) return Result(previous.continuation(), false)

        //continuação de stack trace herda a categoria da linha anterior
        if (isStackContinuation(trimmed)) {
            val kind = if (previous == Kind.WARN) Kind.WARN else Kind.ERROR
            previous = kind
            return Result(kind, false)
        }

        val head = if (raw.length > 180) raw.substring(0, 180) else raw
        val upper = head.uppercase()
        val kind = when {
            ERROR_TAGS.any { upper.contains(it) } -> Kind.ERROR
            EXCEPTION_HEAD.containsMatchIn(trimmed) -> Kind.ERROR
            WARN_TAGS.any { upper.contains(it) } -> Kind.WARN
            IMPORTANT.any { it.containsMatchIn(head) } -> Kind.IMPORTANT
            else -> Kind.NORMAL
        }
        previous = kind
        return Result(kind, kind == Kind.ERROR)
    }

    private fun Kind.continuation(): Kind = if (this == Kind.ERROR || this == Kind.WARN) this else Kind.NORMAL

    private fun isStackContinuation(trimmed: String): Boolean =
        (trimmed.startsWith("at ") && trimmed.contains('(')) ||
            trimmed.startsWith("Caused by:") ||
            trimmed.startsWith("Suppressed:") ||
            (trimmed.startsWith("... ") && trimmed.endsWith(" more"))

    private companion object {
        val ERROR_TAGS = listOf(
            "/ERROR]", "/FATAL]", "/SEVERE]", "[ERROR]", "[FATAL]", "[SEVERE]",
            " ERROR:", "ERROR: ", "FATAL ERROR", "#@!@# GAME CRASHED", "---- MINECRAFT CRASH REPORT ----",
            "EXCEPTION IN THREAD", "OUTOFMEMORYERROR", "UNSATISFIEDLINKERROR", "SIGSEGV", "SIGABRT"
        )
        val WARN_TAGS = listOf("/WARN]", "/WARNING]", "[WARN]", "[WARNING]", " WARN:", "WARNING:")

        /** Linha que começa com uma exceção Java: "java.lang.RuntimeException: ..." */
        val EXCEPTION_HEAD = Regex("^([a-zA-Z_$][\\w$]*\\.)+[\\w$]*(Exception|Error|Throwable)(:|\\s|$)")

        val IMPORTANT = listOf(
            "exit code", "process exited", "game exited", "stopping!", "crash report", "reported exception",
            "loading minecraft", "minecraft version", "setting user", "backend library", "lwjgl version",
            "gl_renderer", "gl_version", "opengl", "using renderer", "renderer:",
            "fabric loader", "quilt loader", "forge mod loader", "modlauncher", "neoforge", "optifine",
            "loading \\d+ mods", "loaded \\d+ mods", "found \\d+ mods", "mods loaded",
            "java version", "java is", "jvm arguments", "max memory", "-xmx",
            "sound engine started", "reloading resourcemanager", "shaderpack", "shader pack",
            "starting integrated server", "preparing spawn", "done \\(", "connecting to", "joined the game",
            "launching", "out of memory"
        ).map { Regex(it, RegexOption.IGNORE_CASE) }
    }
}
