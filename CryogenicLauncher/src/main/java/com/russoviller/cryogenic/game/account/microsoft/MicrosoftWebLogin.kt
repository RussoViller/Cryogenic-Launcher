package com.russoviller.cryogenic.game.account.microsoft

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.russoviller.cryogenic.ui.activities.MicrosoftLoginActivity
import com.russoviller.cryogenic.utils.network.submitForm
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject

object MicrosoftWebLogin {
    const val CLIENT_ID = "00000000402b5328"
    const val SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"
    const val REDIRECT = "https://login.live.com/oauth20_desktop.srf"
    private const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"

    val authorizeUrl: String = Uri.parse("https://login.live.com/oauth20_authorize.srf").buildUpon()
        .appendQueryParameter("client_id", CLIENT_ID)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("scope", SCOPE)
        .appendQueryParameter("redirect_uri", REDIRECT)
        .appendQueryParameter("prompt", "select_account")
        .build().toString()

    class WebLoginException(message: String) : Exception(message)

    @Volatile
    private var pending: CompletableDeferred<String>? = null

    suspend fun requestCode(context: Context): String {
        pending?.cancel()
        val deferred = CompletableDeferred<String>()
        pending = deferred
        withContext(Dispatchers.Main) {
            val intent = Intent(context, MicrosoftLoginActivity::class.java)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        return try {
            deferred.await()
        } finally {
            if (pending === deferred) pending = null
        }
    }

    fun deliverCode(code: String) {
        pending?.complete(code)
    }

    fun deliverError(error: String?) {
        pending?.let { deferred ->
            if (error.isNullOrBlank()) {
                deferred.completeExceptionally(CancellationException("Microsoft login cancelled"))
            } else {
                deferred.completeExceptionally(WebLoginException(error))
            }
        }
    }

    suspend fun exchangeCode(code: String): Pair<String, String> = requestToken(
        Parameters.build {
            append("client_id", CLIENT_ID)
            append("code", code)
            append("grant_type", "authorization_code")
            append("redirect_uri", REDIRECT)
            append("scope", SCOPE)
        }
    )

    suspend fun refresh(refreshToken: String): Pair<String, String> = requestToken(
        Parameters.build {
            append("client_id", CLIENT_ID)
            append("refresh_token", refreshToken)
            append("grant_type", "refresh_token")
            append("redirect_uri", REDIRECT)
            append("scope", SCOPE)
        },
        fallbackRefresh = refreshToken
    )

    private suspend fun requestToken(parameters: Parameters, fallbackRefresh: String = ""): Pair<String, String> {
        val response = try {
            submitForm<JsonObject>(TOKEN_URL, parameters, Dispatchers.IO)
        } catch (e: ClientRequestException) {
            val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (json?.optString("error") == "invalid_grant") throw com.russoviller.cryogenic.game.account.CredentialsExpiredException()
            throw WebLoginException(json?.optString("error_description")?.takeIf { it.isNotBlank() } ?: "HTTP ${e.response.status.value}")
        }
        val access = response["access_token"]?.jsonPrimitive?.content
            ?: throw WebLoginException("Missing access_token")
        val refresh = response["refresh_token"]?.jsonPrimitive?.content ?: fallbackRefresh
        return access to refresh
    }
}
