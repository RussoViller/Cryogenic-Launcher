package com.russoviller.cryogenic.game.seedmap

/**
 * Ponte para a biblioteca nativa "cryoseed" (cubiomes): biomas e estruturas a partir da seed.
 * Um handle guarda o gerador de uma versão + seed + dimensão e NÃO pode ser usado por duas
 * threads ao mesmo tempo.
 */
object SeedNative {
    /** false quando a biblioteca não carregou (ex.: testes no PC). */
    val available: Boolean = runCatching {
        //testes no PC carregam uma cópia compilada para o computador
        val hostPath = System.getProperty("cryoseed.path")
        if (hostPath != null) System.load(hostPath) else System.loadLibrary("cryoseed")
    }.isSuccess

    @JvmStatic external fun create(mc: Int, seed: Long, dim: Int, large: Boolean): Long
    @JvmStatic external fun free(handle: Long)
    @JvmStatic external fun biomes(handle: Long, scale: Int, x: Int, z: Int, w: Int, h: Int, y: Int): IntArray?
    @JvmStatic external fun structureDim(type: Int, mc: Int): Int
    @JvmStatic external fun structures(handle: Long, type: Int, x0: Int, z0: Int, x1: Int, z1: Int, max: Int): IntArray?
    @JvmStatic external fun strongholds(handle: Long, count: Int): IntArray?
    @JvmStatic external fun biomeIds(mc: Int, dim: Int): IntArray?
    @JvmStatic external fun biomeName(mc: Int, id: Int): String?
    @JvmStatic external fun biomeColors(): ByteArray?
    @JvmStatic external fun newestVersion(): Int
}
