package com.russoviller.cryogenic.game.account.wardrobe

import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL

data class OfficialCape(
    val name: String,
    val alias: String,
    val texture: String
)

object OfficialCapes {
    private const val LIST_URL = "https://api.minecraftcapes.net/api/gallery/officialcapes"

    private fun get(url: String): ByteArray? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "CryogenicLauncher")
        }
        return try {
            if (connection.responseCode in 200..299) connection.inputStream.use { it.readBytes() } else null
        } finally {
            connection.disconnect()
        }
    }

    fun fetch(): List<OfficialCape> {
        val text = get(LIST_URL)?.toString(Charsets.UTF_8) ?: error("empty response")
        val root = JsonParser.parseString(text).asJsonObject
        val data = root.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = element.asJsonObject
            val texture = obj.get("texture")?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OfficialCape(
                name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                alias = obj.get("alias")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                texture = texture
            )
        }
    }

    fun download(cape: OfficialCape): ByteArray? = get(cape.texture)
}
