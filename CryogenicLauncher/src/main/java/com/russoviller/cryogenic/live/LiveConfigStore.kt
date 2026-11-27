package com.russoviller.cryogenic.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.russoviller.cryogenic.R
import java.io.File

/** Configuração da live em Compose state (um por processo), sempre gravada no arquivo compartilhado. */
object LiveConfigStore {
    var config by mutableStateOf(LiveConfig())
        private set
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        reload(context)
    }

    fun reload(context: Context) {
        config = LiveConfig.load(context)
        loaded = true
    }

    /** Lê o arquivo de novo antes de mudar (o serviço também grava mic/volumes/bloqueio). */
    fun update(context: Context, change: (LiveConfig) -> LiveConfig) {
        val next = change(LiveConfig.load(context))
        config = next
        loaded = true
        LiveConfig.save(context, next)
        LiveClient.configChanged()
    }

    /** Copia o PNG escolhido para a pasta privada da live (reduzido para no máximo 1920 px). */
    fun importPrivacyImage(context: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 3840 || bounds.outHeight / sample > 3840) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(1f, 1920f / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
        val dir = File(context.filesDir, "live").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.startsWith("privacy_") }?.forEach { it.delete() }
        val out = File(dir, "privacy_${System.currentTimeMillis()}.png")
        out.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        out.absolutePath
    }.getOrNull()

    fun canDrawOverlays(context: Context): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

    fun overlayPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * Fluxo para começar a live: permissões (microfone/notificação) -> autorização de captura da tela
 * (tela inteira) -> serviço em primeiro plano.
 */
@Composable
fun rememberLiveStarter(): () -> Unit {
    val context = LocalContext.current
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, LiveService::class.java)
                .setAction(LiveService.ACTION_START)
                .putExtra(LiveService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(LiveService.EXTRA_RESULT_DATA, result.data)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
                LiveClient.reconnect(context)
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.cryo_live_error_start, it.message ?: ""), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, R.string.cryo_live_error_permission, Toast.LENGTH_SHORT).show()
        }
    }
    val requestProjection = remember(context) {
        {
            val pm = context.getSystemService(MediaProjectionManager::class.java)
            // Android 14+: pede a tela inteira (a live continua mostrando o celular quando o usuário sai do app)
            val intent = if (Build.VERSION.SDK_INT >= 34) {
                pm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
            } else {
                pm.createScreenCaptureIntent()
            }
            runCatching { projectionLauncher.launch(intent) }.onFailure {
                Toast.makeText(context, R.string.cryo_live_error_permission, Toast.LENGTH_SHORT).show()
            }
            Unit
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // segue mesmo sem o microfone (só som interno) ou sem notificação
        requestProjection()
    }
    return remember(context) {
        {
            val config = LiveConfig.load(context)
            if (!config.isReady) {
                Toast.makeText(context, R.string.cryo_live_error_config, Toast.LENGTH_LONG).show()
            } else {
                val needed = buildList {
                    if (config.micEnabled && !granted(context, Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= 33 && !granted(context, Manifest.permission.POST_NOTIFICATIONS)) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (needed.isEmpty()) requestProjection() else permissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
