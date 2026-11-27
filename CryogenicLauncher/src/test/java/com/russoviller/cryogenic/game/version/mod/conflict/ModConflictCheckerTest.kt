package com.russoviller.cryogenic.game.version.mod.conflict

import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModConflictCheckerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun jar(name: String, vararg entries: Pair<String, String>): File {
        val f = File(tmp.root, name)
        ZipOutputStream(f.outputStream()).use { z ->
            entries.forEach { (path, text) ->
                z.putNextEntry(ZipEntry(path)); z.write(text.toByteArray()); z.closeEntry()
            }
        }
        return f
    }

    private fun fabric(name: String, id: String, version: String, extra: String = "") =
        jar(name, "fabric.mod.json" to """{"schemaVersion":1,"id":"$id","name":"${id.uppercase()}","version":"$version"$extra}""")

    @Test
    fun noConflict() {
        val a = fabric("a.jar", "combat_a", "1.0.0")
        val b = fabric("b.jar", "other", "2.0.0")
        assertTrue(ModConflictChecker.checkFiles(listOf(a, b), ModLoader.FABRIC).isEmpty())
    }

    @Test
    fun declaredBreaksAnyVersion() {
        val a = fabric("a.jar", "combat_a", "1.0.0", ""","breaks":{"combat_b":"*"}""")
        val b = fabric("b.jar", "combat_b", "3.1.0")
        val g = ModConflictChecker.checkFiles(listOf(a, b), ModLoader.FABRIC)
        assertEquals(1, g.size)
        assertEquals(ModConflictChecker.Kind.DECLARED, g[0].links[0].kind)
        assertTrue(!g[0].softOnly)
    }

    @Test
    fun declaredBreaksVersionRange() {
        val a = fabric("a.jar", "x", "1.0.0", ""","breaks":{"y":"<2.0.0"}""")
        val oldY = fabric("y1.jar", "y", "1.5.0+mc1.20.1")
        val newY = fabric("y2.jar", "z", "2.5.0", ""","provides":["q"]""")
        assertEquals(1, ModConflictChecker.checkFiles(listOf(a, oldY), ModLoader.FABRIC).size)
        assertTrue(ModConflictChecker.checkFiles(listOf(a, newY), ModLoader.FABRIC).isEmpty())
        val newer = fabric("y3.jar", "y", "2.0.1")
        assertTrue(ModConflictChecker.checkFiles(listOf(a, newer), ModLoader.FABRIC).isEmpty())
    }

    @Test
    fun conflictsIsSoft() {
        val a = fabric("a.jar", "x", "1.0.0", ""","conflicts":{"y":"*"}""")
        val b = fabric("b.jar", "y", "1.0.0")
        val g = ModConflictChecker.checkFiles(listOf(a, b), ModLoader.FABRIC)
        assertEquals(1, g.size)
        assertTrue(g[0].softOnly)
    }

    @Test
    fun duplicateAndThreeWayGroup() {
        val a1 = fabric("a1.jar", "a", "1.0.0")
        val a2 = fabric("a2.jar", "a", "1.1.0", ""","breaks":{"c":"*"}""")
        val c = fabric("c.jar", "c", "1.0.0")
        val g = ModConflictChecker.checkFiles(listOf(a1, a2, c), ModLoader.FABRIC)
        assertEquals(1, g.size)
        assertEquals(3, g[0].mods.size)
    }

    @Test
    fun knownRendererGroupNeoForge() {
        fun toml(id: String) = "META-INF/neoforge.mods.toml" to "modLoader=\"javafml\"\nloaderVersion=\"[1,)\"\nlicense=\"x\"\n[[mods]]\nmodId=\"$id\"\nversion=\"1.0\"\ndisplayName=\"$id\"\n"
        val s = jar("s.jar", toml("sodium"))
        val e = jar("e.jar", toml("embeddium"))
        val g = ModConflictChecker.checkFiles(listOf(s, e), ModLoader.NEOFORGE)
        assertEquals(1, g.size)
        assertEquals(ModConflictChecker.Kind.KNOWN, g[0].links[0].kind)
    }

    @Test
    fun forgeIncompatibleMavenRange() {
        val a = jar(
            "a.jar", "META-INF/mods.toml" to """
modLoader="javafml"
loaderVersion="[47,)"
license="x"
[[mods]]
modId="alpha"
version="1.0"
displayName="Alpha"
[[dependencies.alpha]]
modId="beta"
type="incompatible"
versionRange="[1.0,2.0)"
""".trimIndent()
        )
        val b1 = jar("b1.jar", "META-INF/mods.toml" to "modLoader=\"javafml\"\nloaderVersion=\"[47,)\"\nlicense=\"x\"\n[[mods]]\nmodId=\"beta\"\nversion=\"1.5\"\n")
        val b2 = jar("b2.jar", "META-INF/mods.toml" to "modLoader=\"javafml\"\nloaderVersion=\"[47,)\"\nlicense=\"x\"\n[[mods]]\nmodId=\"beta\"\nversion=\"2.0\"\n")
        assertEquals(1, ModConflictChecker.checkFiles(listOf(a, b1), ModLoader.FORGE).size)
        assertTrue(ModConflictChecker.checkFiles(listOf(a, b2), ModLoader.FORGE).isEmpty())
    }

    @Test
    fun dependentsWhenRemoving() {
        val lib = fabric("lib.jar", "corelib", "1.0.0")
        val a = fabric("a.jar", "moda", "1.0.0", ""","depends":{"corelib":"*","minecraft":"*"}""")
        val b = fabric("b.jar", "modb", "1.0.0", ""","depends":{"corelib":">=1"}""")
        val c = fabric("c.jar", "modc", "1.0.0")
        val all = listOf(lib, a, b, c).flatMap { f ->
            kotlinx.coroutines.runBlocking { ModConflictChecker.scanDir(tmp.root, ModLoader.FABRIC) }.filter { it.file == f }
        }
        val deps = ModConflictChecker.dependentsOf(listOf(lib), all)
        assertEquals(setOf("moda", "modb"), deps[lib]!!.map { it.id }.toSet())
        assertTrue(ModConflictChecker.dependentsOf(listOf(c), all).isEmpty())
        // outro mod que fornece o mesmo ID evita a quebra
        val alt = fabric("alt.jar", "altlib", "1.0.0", ""","provides":["corelib"]""")
        val all2 = kotlinx.coroutines.runBlocking { ModConflictChecker.scanDir(tmp.root, ModLoader.FABRIC) }
        assertTrue(ModConflictChecker.dependentsOf(listOf(lib), all2).isEmpty())
        alt.delete()
    }
}
