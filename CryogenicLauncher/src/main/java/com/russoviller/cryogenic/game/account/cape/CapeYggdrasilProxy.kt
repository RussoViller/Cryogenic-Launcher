package com.russoviller.cryogenic.game.account.cape

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.utils.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jackhuang.hmcl.util.DigestUtils
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG = "CapeYggdrasilProxy"

class CapeYggdrasilProxy private constructor(
    private val upstream: Upstream,
    profileId: String,
    private val capeBytes: ByteArray
) {
    sealed interface Upstream {
        data object Mojang : Upstream
        data class Custom(val apiRoot: String) : Upstream
    }

    private val ownUuid = profileId.replace("-", "").lowercase()
    private val capeHash = DigestUtils.digestToString("SHA-256", capeBytes)
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val startedLatch = CountDownLatch(1)
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var running = false
    private var upstreamMetadata: JsonObject? = null
    private var cachedPort: Int? = null

    var upstreamPublicKey: String? = null
        private set

    private suspend fun start(): Boolean {
        val custom = upstream as? Upstream.Custom
        if (custom != null) {
            val metadata = runCatching {
                val request = Request.Builder().url(custom.apiRoot).header("User-Agent", BuildKeys.LAUNCHER_SHORT_NAME).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    JsonParser.parseString(response.body.string()).asJsonObject
                }
            }.onFailure { e ->
                Logger.warning(TAG, "Failed to read upstream metadata from ${custom.apiRoot}", e)
            }.getOrNull() ?: return false
            upstreamMetadata = metadata
            upstreamPublicKey = metadata.get("signaturePublickey")
                ?.takeIf { it.isJsonPrimitive }?.asString
                ?.replace(Regex("-----[A-Z ]+-----"), "")
                ?.replace(Regex("\\s"), "")
                ?.takeIf { it.isNotEmpty() }
        }

        server = embeddedServer(CIO, port = 0, host = "127.0.0.1") {
            routing {
                get("/") {
                    runCatching { call.respondText(root(), ContentType.Application.Json) }
                        .onFailure { e -> Logger.error(TAG, "Root failed", e) }
                }
                get("/cryo-textures/{hash}") {
                    if (call.parameters["hash"] == capeHash) {
                        call.response.header("Cache-Control", "max-age=2592000, public")
                        call.response.header("Etag", "\"$capeHash\"")
                        call.respondBytes(capeBytes, ContentType.Image.PNG)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                route("{...}") {
                    handle {
                        runCatching { forward(call) }.onFailure { e ->
                            Logger.error(TAG, "Forward failed: ${call.request.uri}", e)
                            runCatching { call.respond(HttpStatusCode.BadGateway) }
                        }
                    }
                }
            }
        }.apply {
            monitor.subscribe(ApplicationStarted) { startedLatch.countDown() }
        }
        server?.start(wait = false)
        running = startedLatch.await(10, TimeUnit.SECONDS)
        if (running) {
            cachedPort = server?.engine?.let { engine ->
                runCatching { engine.resolvedConnectors().firstOrNull()?.port }.getOrNull()
            }
        }
        return running && cachedPort != null
    }

    fun stop() {
        running = false
        server?.stop(1000, 5000)
        server = null
        startedLatch.countDown()
    }

    fun getPort(): Int? = if (running) cachedPort else null

    fun apiRoot(): String? = getPort()?.let { "http://127.0.0.1:$it" }

    private fun pem(): String =
        "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(keyPair.public.encoded) + "\n-----END PUBLIC KEY-----"

    private fun root(): String {
        val result = when (upstream) {
            is Upstream.Mojang -> JsonObject().apply {
                add("skinDomains", JsonArray())
                add("meta", JsonObject().apply {
                    addProperty("serverName", "Mojang")
                    addProperty("implementationName", BuildKeys.LAUNCHER_SHORT_NAME)
                    addProperty("implementationVersion", "1.0")
                    addProperty("feature.no_mojang_namespace", true)
                    addProperty("feature.enable_profile_key", true)
                    addProperty("feature.enable_mojang_anti_features", true)
                })
            }
            is Upstream.Custom -> upstreamMetadata?.deepCopy() ?: JsonObject()
        }
        val domains = (result.get("skinDomains")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray())
        listOf("localhost", "127.0.0.1", ".minecraft.net", ".mojang.com").forEach { domain ->
            if (domains.none { it.isJsonPrimitive && it.asString == domain }) domains.add(domain)
        }
        result.add("skinDomains", domains)
        result.addProperty("signaturePublickey", pem())
        return result.toString()
    }

    private fun upstreamUrl(path: String): String? {
        return when (upstream) {
            is Upstream.Mojang -> {
                val clean = path.removePrefix("/")
                val first = clean.substringBefore('/')
                val rest = clean.removePrefix(first)
                when (first) {
                    "sessionserver" -> "https://sessionserver.mojang.com$rest"
                    "api" -> if (rest.startsWith("/profiles/minecraft")) {
                        "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname"
                    } else {
                        "https://api.mojang.com$rest"
                    }
                    "authserver" -> "https://authserver.mojang.com$rest"
                    "skins" -> "https://skins.minecraft.net$rest"
                    "minecraftservices" -> "https://api.minecraftservices.com$rest"
                    else -> null
                }
            }
            is Upstream.Custom -> upstream.apiRoot.removeSuffix("/") + "/" + path.removePrefix("/")
        }
    }

    private suspend fun forward(call: ApplicationCall) {
        val uri = call.request.uri
        val path = uri.substringBefore('?')
        val target = upstreamUrl(path)?.let { base ->
            val query = uri.substringAfter('?', "")
            if (query.isNotEmpty()) "$base?$query" else base
        } ?: return call.respond(HttpStatusCode.NotFound)

        val method = call.request.httpMethod
        val hasBody = method == HttpMethod.Post || method == HttpMethod.Put || method == HttpMethod.Patch || method == HttpMethod.Delete
        val bodyBytes = if (hasBody) runCatching { call.receive<ByteArray>() }.getOrDefault(ByteArray(0)) else null
        val requestType = call.request.headers["Content-Type"]

        val builder = Request.Builder().url(target)
        listOf("Authorization", "Accept", "User-Agent", "Accept-Language").forEach { name ->
            call.request.headers[name]?.let { builder.header(name, it) }
        }
        if (call.request.headers["User-Agent"] == null) builder.header("User-Agent", BuildKeys.LAUNCHER_SHORT_NAME)
        val body = bodyBytes?.toRequestBody(requestType?.toMediaTypeOrNull())
        builder.method(method.value, if (method == HttpMethod.Get || method == HttpMethod.Head) null else (body ?: ByteArray(0).toRequestBody(null)))

        val (status, contentType, bytes) = withContext(Dispatchers.IO) {
            client.newCall(builder.build()).execute().use { response ->
                Triple(response.code, response.header("Content-Type"), response.body.bytes())
            }
        }

        val patched = if (status == 200 && method == HttpMethod.Get && isProfilePath(path)) {
            runCatching { patchProfile(bytes) }.onFailure { e ->
                Logger.warning(TAG, "Failed to patch profile response", e)
            }.getOrNull() ?: bytes
        } else {
            bytes
        }

        val type = contentType?.let { runCatching { ContentType.parse(it) }.getOrNull() } ?: ContentType.Application.Json
        if (status == 204 || (patched.isEmpty() && status in 200..299)) {
            call.respond(HttpStatusCode.fromValue(status))
        } else {
            call.respondBytes(patched, type, HttpStatusCode.fromValue(status))
        }
    }

    private fun isProfilePath(path: String): Boolean =
        path.startsWith("/sessionserver/session/minecraft/profile/") ||
            path == "/sessionserver/session/minecraft/hasJoined"

    private fun patchProfile(bytes: ByteArray): ByteArray? {
        val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        val id = root.get("id")?.asString?.replace("-", "")?.lowercase() ?: return null
        if (id != ownUuid) return null
        val properties = root.getAsJsonArray("properties") ?: JsonArray().also { root.add("properties", it) }
        val existing = properties.firstOrNull { it.isJsonObject && it.asJsonObject.get("name")?.asString == "textures" }?.asJsonObject
        val textures = existing?.get("value")?.asString?.let { value ->
            JsonParser.parseString(String(Base64.getDecoder().decode(value), Charsets.UTF_8)).asJsonObject
        } ?: JsonObject().apply {
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("profileId", id)
            root.get("name")?.let { add("profileName", it) }
        }
        val map = textures.getAsJsonObject("textures") ?: JsonObject().also { textures.add("textures", it) }
        val port = getPort() ?: return null
        map.add("CAPE", JsonObject().apply {
            add("url", JsonPrimitive("http://127.0.0.1:$port/cryo-textures/$capeHash"))
        })
        val encoded = Base64.getEncoder().encodeToString(textures.toString().toByteArray(Charsets.UTF_8))
        val property = existing ?: JsonObject().also {
            it.addProperty("name", "textures")
            properties.add(it)
        }
        property.addProperty("value", encoded)
        property.addProperty("signature", sign(encoded))
        Logger.info(TAG, "Injected launcher cape into profile $id")
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    private fun sign(data: String): String {
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(keyPair.private)
        signature.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    companion object {
        fun create(upstream: Upstream, profileId: String, capeBytes: ByteArray): CapeYggdrasilProxy? {
            val proxy = CapeYggdrasilProxy(upstream, profileId, capeBytes)
            val ok = runCatching { runBlocking(Dispatchers.IO) { proxy.start() } }
                .onFailure { e -> Logger.error(TAG, "Failed to start cape proxy", e) }
                .getOrDefault(false)
            if (!ok || proxy.getPort() == null) {
                runCatching { proxy.stop() }
                return null
            }
            return proxy
        }
    }
}
