package com.russoviller.cryogenic.cmod

import android.os.Looper
import com.russoviller.cryogenic.path.PathManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CmodApi2Test {
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("cmod2").toFile()
        PathManager.DIR_FILES_PRIVATE = dir
    }

    private fun info(id: String, deps: List<String> = emptyList()) =
        CmodInfo(id, id, "1", "", "", "a.B", 2, dependencies = deps)

    @Test
    fun parseApi2Fields() {
        val i = CmodInfo.parse(
            """{"id":"game.only","gameMain":"x.Game","website":"https://example.com","dependencies":["dep.one","game.only"],
               "jvmArgs":["-Dfoo=1","-javaagent:${'$'}{modDir}/game/a.jar"],"gameArgs":["--demo"],"apiVersion":2}"""
        )
        assertFalse(i.hasLauncherSide)
        assertTrue(i.hasGameSide)
        assertEquals("https://example.com", i.website)
        assertEquals(listOf("dep.one"), i.dependencies) // não depende de si mesmo
        assertEquals(2, i.jvmArgs.size)
        assertEquals(listOf("--demo"), i.gameArgs)
        listOf(
            """{"id":"bad.args","main":"a.B","jvmArgs":["-cp","x"]}""",   // só -D/-X/-javaagent...
            """{"id":"bad.main","main":"a b"}""",
            """{"id":"bad.web","main":"a.B","website":"javascript:alert(1)"}"""
        ).forEachIndexed { n, it ->
            if (n == 2) assertEquals("", CmodInfo.parse(it).website)
            else try { CmodInfo.parse(it); fail("aceitou $it") } catch (_: Exception) {}
        }
    }

    @Test
    fun dependencyOrder() {
        val all = listOf(info("c", listOf("b")), info("b", listOf("a")), info("a"), info("x", listOf("missing")), info("y", listOf("z")))
        val (order, missing) = CmodPaths.order(all, setOf("a", "b", "c", "x", "y"))
        assertEquals(listOf("a", "b", "c"), order.map { it.id })
        assertEquals("missing", missing["x"])
        assertEquals("z", missing["y"]) // instalado? não: falta
    }

    @Test
    fun expandPlaceholders() {
        val s = CmodPaths.expand("-javaagent:\${modDir}/game/a.jar=\${dataDir}", "m.id", null)
        assertTrue(s.startsWith("-javaagent:" + File(dir, "cmods/m.id").absolutePath))
        assertTrue(s.endsWith(File(dir, "cmods_data/m.id").absolutePath))
    }

    @Test
    fun prefsAreSharedThroughTheFile() {
        val f = File(dir, "p.properties")
        val a = CmodPrefs(f)
        val b = CmodPrefs(f)
        a.putInt("n", 5)
        a.putBoolean("on", true)
        a.putString("s", "olá = mundo")
        assertEquals(5, b.getInt("n", 0))
        assertTrue(b.getBoolean("on", false))
        assertEquals("olá = mundo", b.getString("s", null))
        Thread.sleep(5)
        b.putInt("n", 7)
        f.setLastModified(f.lastModified() + 2000)
        assertEquals(7, a.getInt("n", 0))
        a.remove("s")
        assertNull(b.getString("s", null).takeIf { b.contains("s") })
        assertEquals(setOf("n", "on"), b.keys())
    }

    @Test
    fun schedulerRunsAndCancels() {
        var once = 0
        var rep = 0
        val sched = CmodScheduler { it.run() }
        sched.schedule(100) { once++ }
        val task = sched.repeat(0, 50) { rep++ }
        shadowOf(Looper.getMainLooper()).idleFor(220, TimeUnit.MILLISECONDS)
        assertEquals(1, once)
        assertTrue(rep >= 4)
        task.cancel()
        val before = rep
        shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        assertEquals(before, rep)
        sched.schedule(10) { once++ }
        sched.close()
        shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)
        assertEquals(1, once)
    }
}
