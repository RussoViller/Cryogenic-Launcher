package com.russoviller.cryogenic.sponsor

import com.russoviller.cryogenic.path.GLOBAL_CLIENT
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.utils.logging.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/** Um patrocinador do afdian. Tempos em segundos (Unix). */
@Serializable
data class CryoSponsor(
    val id: String = "",
    val name: String = "",
    val avatar: String = "",
    val since: Long = 0L,
    val until: Long = 0L,
    /** Total patrocinado no afdian (ex.: "10.00"), na moeda de [CryoSponsorData.currency]. */
    val amount: String = ""
)

/**
 * Lista publicada pelo GitHub Action do repositório (sponsors/sponsors.json).
 * [latest] é o momento do patrocínio mais recente: quando aumenta, há patrocinador novo.
 */
@Serializable
data class CryoSponsorData(
    val version: Int = 1,
    val updated: Long = 0L,
    val latest: Long = 0L,
    val count: Int = 0,
    val currency: String = "CNY",
    val sponsors: List<CryoSponsor> = emptyList()
) {
    /** "¥10.00" (afdian cobra em yuan). */
    fun formatAmount(sponsor: CryoSponsor): String? {
        val value = sponsor.amount.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val symbol = if (currency.equals("CNY", true)) "¥" else "$currency "
        return symbol + String.format(java.util.Locale.US, "%.2f", value)
    }

    /** Só quem ainda está dentro dos 30 dias (vale mesmo se o arquivo estiver atrasado). */
    fun active(nowMs: Long = System.currentTimeMillis()): List<CryoSponsor> =
        sponsors.filter { it.until <= 0L || it.until * 1000L > nowMs }.sortedBy { it.since }
}

@Serializable
private data class SponsorCache(val fetchedAt: Long = 0L, val data: CryoSponsorData = CryoSponsorData())

/**
 * Patrocinadores atuais do afdian.
 *
 * O token do afdian fica só no GitHub (Secret do repositório): um Action roda às 00:00, 08:00 e
 * 16:00 (UTC-3) e publica um JSON apenas com nome, foto e datas. O Launcher só lê esse JSON,
 * uma vez por janela de 8 horas (com 30 min de folga para o Action terminar).
 */
object CryoSponsors {
    private const val TAG = "CryoSponsors"
    private const val PATH = "RussoViller/Cryogenic-Launcher"
    private val URLS = listOf(
        "https://raw.githubusercontent.com/$PATH/main/sponsors/sponsors.json",
        "https://cdn.jsdelivr.net/gh/$PATH@main/sponsors/sponsors.json",
        "https://fastly.jsdelivr.net/gh/$PATH@main/sponsors/sponsors.json"
    )

    private const val HOUR = 3_600_000L
    private const val SLOT = 8 * HOUR
    /** Horário de referência do Action: UTC-3 (00:00 / 08:00 / 16:00). */
    private const val ZONE_OFFSET = -3 * HOUR
    private const val GRACE = 30 * 60_000L
    private const val MANUAL_COOLDOWN = 60_000L

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val mutex = Mutex()

    private val _data = MutableStateFlow<CryoSponsorData?>(null)
    val data: StateFlow<CryoSponsorData?> = _data.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Já houve pelo menos uma tentativa (com ou sem sucesso) desde que o Launcher abriu. */
    private val _attempted = MutableStateFlow(false)
    val attempted: StateFlow<Boolean> = _attempted.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    /** Pedido para abrir a tela de patrocinadores (botão em Sobre). */
    private val _openRequest = MutableStateFlow(false)
    val openRequest: StateFlow<Boolean> = _openRequest.asStateFlow()

    fun requestOpen() { _openRequest.value = true }
    fun consumeOpen() { _openRequest.value = false }

    private var fetchedAt = 0L
    private var lastManual = 0L
    private var cacheLoaded = false

    /** Início da janela de 8 h atual (00:00/08:00/16:00 em UTC-3), em ms UTC. */
    fun slotStart(nowMs: Long = System.currentTimeMillis()): Long {
        val local = nowMs + ZONE_OFFSET
        return Math.floorDiv(local, SLOT) * SLOT - ZONE_OFFSET
    }

    /** Próximo horário de atualização (00:00/08:00/16:00 em UTC-3), em ms UTC. */
    fun nextUpdate(nowMs: Long = System.currentTimeMillis()): Long = slotStart(nowMs) + SLOT

    /** Último ponto em que o JSON já deveria estar atualizado. */
    private fun lastRefreshPoint(nowMs: Long): Long {
        val point = slotStart(nowMs) + GRACE
        return if (nowMs < point) point - SLOT else point
    }

    private fun loadCache() {
        if (cacheLoaded) return
        cacheLoaded = true
        runCatching {
            val file = PathManager.FILE_SPONSORS_CACHE
            if (file.isFile) {
                val cache = json.decodeFromString<SponsorCache>(file.readText())
                fetchedAt = cache.fetchedAt
                if (_data.value == null) _data.value = cache.data
            }
        }.onFailure { Logger.warning(TAG, "Could not read the sponsors cache", it) }
    }

    private fun saveCache(data: CryoSponsorData) {
        runCatching {
            val file = PathManager.FILE_SPONSORS_CACHE
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(SponsorCache.serializer(), SponsorCache(fetchedAt, data)))
        }.onFailure { Logger.warning(TAG, "Could not save the sponsors cache", it) }
    }

    /**
     * Baixa a lista se a janela atual ainda não foi lida (ou se [force]).
     * Retorna true quando há dados (novos ou do cache).
     */
    suspend fun refresh(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadCache()
            val now = System.currentTimeMillis()
            if (force) {
                if (now - lastManual < MANUAL_COOLDOWN) return@withLock _data.value != null
                lastManual = now
            } else if (_data.value != null && fetchedAt >= lastRefreshPoint(now)) {
                _attempted.value = true
                return@withLock true
            }
            _loading.value = true
            try {
                var fresh: CryoSponsorData? = null
                for (url in URLS) {
                    fresh = runCatching {
                        json.decodeFromString<CryoSponsorData>(GLOBAL_CLIENT.get(url).bodyAsText())
                    }.onFailure { Logger.warning(TAG, "Sponsors fetch failed: $url (${it.message})") }.getOrNull()
                    if (fresh != null) break
                }
                if (fresh != null) {
                    fetchedAt = now
                    _data.value = fresh
                    _failed.value = false
                    saveCache(fresh)
                } else {
                    _failed.value = true
                }
                _data.value != null
            } finally {
                _loading.value = false
                _attempted.value = true
            }
        }
    }

    /** Mantém a lista em dia enquanto o Launcher está aberto. */
    suspend fun runLoop() {
        while (coroutineContext.isActive) {
            refresh()
            val now = System.currentTimeMillis()
            //Sem internet: tenta de novo em alguns minutos em vez de esperar a próxima janela
            val wait = if (_failed.value) 5 * 60_000L
                else (lastRefreshPoint(now) + SLOT - now).coerceAtLeast(60_000L)
            delay(wait)
        }
    }
}
