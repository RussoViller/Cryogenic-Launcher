package com.russoviller.cryogenic.game.media

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.channels.Channels
import java.nio.channels.ServerSocketChannel
import kotlin.concurrent.thread

/**
 * Teste de integração local com o leitor MPRIS de um mod real (ex.: Show the Lyrics).
 * Só roda quando o ambiente aponta o jar do mod, o jar do JNA e o endereço do barramento:
 * CRYO_STL_JAR, CRYO_STL_CLASSPATH (JNA, slf4j e um stub do Fabric, separados por ':')
 * e DBUS_SESSION_BUS_ADDRESS=unix:path=...
 * O jar do mod não faz parte do projeto.
 */
class MprisBridgeTest {
    private var server: ServerSocketChannel? = null

    @After
    fun tearDown() {
        server?.close()
        MprisBridge.trackOverride = null
    }

    @Test
    fun modReadsPhoneTrack() {
        val stlJar = System.getenv("CRYO_STL_JAR")?.let(::File)
        val extra = System.getenv("CRYO_STL_CLASSPATH")?.split(':')?.map(::File).orEmpty()
        val busPath = System.getenv("DBUS_SESSION_BUS_ADDRESS")?.removePrefix("unix:path=")
        assumeTrue(stlJar?.isFile == true && extra.isNotEmpty() && extra.all { it.isFile } && busPath != null)

        File(busPath!!).delete()
        //APIs de socket Unix do JDK 16+ (fora do android.jar usado na compilação dos testes)
        val unix = java.net.StandardProtocolFamily.valueOf("UNIX")
        val channel = ServerSocketChannel::class.java
            .getMethod("open", java.net.ProtocolFamily::class.java)
            .invoke(null, unix) as ServerSocketChannel
        val address = Class.forName("java.net.UnixDomainSocketAddress")
            .getMethod("of", String::class.java)
            .invoke(null, busPath) as java.net.SocketAddress
        channel.bind(address)
        server = channel
        thread(isDaemon = true) {
            while (true) {
                val client = runCatching { channel.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    runCatching {
                        client.use {
                            MprisBridge.serveStreams(Channels.newInputStream(it), Channels.newOutputStream(it))
                        }
                    }
                }
            }
        }

        val loader = URLClassLoader((listOf(stlJar!!) + extra).map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        val dbus = loader.loadClass("com.showthelyrics.media.DbusConnection")
        val open = dbus.getDeclaredMethod("openSessionBus", Long::class.javaPrimitiveType).apply { isAccessible = true }
        val listenerClass = loader.loadClass("com.showthelyrics.media.MediaSessionListener")
        val listener = java.lang.reflect.Proxy.newProxyInstance(loader, arrayOf(listenerClass)) { _, _, _ -> null }
        val sessionClass = loader.loadClass("com.showthelyrics.media.MprisSession")
        val session = sessionClass.getDeclaredConstructor(listenerClass).apply { isAccessible = true }.newInstance(listener)
        val readPlayer = sessionClass.getDeclaredMethod("readPlayer", dbus, String::class.java)
            .apply { isAccessible = true }
        val call = dbus.getDeclaredMethod(
            "call", String::class.java, String::class.java, String::class.java, String::class.java,
            Long::class.javaPrimitiveType, Array<String>::class.java
        ).apply { isAccessible = true }
        fun listNames(connection: Any): List<*> = (call.invoke(
            connection, "org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "ListNames",
            2000L, arrayOf<String>()
        ) as List<*>).first() as List<*>

        //sem música tocando: o mod conecta (auth + Hello) e não encontra player MPRIS
        MprisBridge.trackOverride = { null }
        val connection = open.invoke(null, 3000L)!!
        assertEquals(false, listNames(connection).any { it.toString().startsWith("org.mpris.MediaPlayer2.") })

        //tocando no celular
        MprisBridge.trackOverride = {
            PhoneTrack(
                title = "Numb", artists = listOf("Linkin Park"), album = "Meteora",
                lengthUs = 185_000_000L, positionUs = 42_000_000L, status = "Playing", rate = 1.0,
                source = "com.spotify.music"
            )
        }
        val players = listNames(connection).map { it.toString() }.filter { it.startsWith("org.mpris.MediaPlayer2.") }
        assertEquals(listOf("org.mpris.MediaPlayer2.cryogenic"), players)
        val player = readPlayer.invoke(session, connection, players.first())
        assertNotNull(player)
        fun field(name: String) = player!!.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(player)
        assertEquals("Numb", field("title"))
        assertEquals("Linkin Park", field("artist"))
        assertEquals("Meteora", field("album"))
        assertEquals("Playing", field("status"))
        assertEquals(185_000L, field("lengthMs"))
        assertEquals(42_000L, field("positionMs"))
        dbus.getDeclaredMethod("close").apply { isAccessible = true }.invoke(connection)
    }
}
