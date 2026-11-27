package com.russoviller.cryogenic.legal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.russoviller.cryogenic.BuildConfig
import java.security.MessageDigest

/**
 * Cryogenic Launcher — criado por RussoViller.
 * https://github.com/RussoViller/Cryogenic-Launcher (licença RVCLapk1.1 + GPL-3.0)
 *
 * Confere se o APK foi assinado com o certificado oficial de RussoViller. Uma cópia
 * recompilada (com outro nome ou não) tem outro certificado e passa a mostrar o aviso
 * de cópia não oficial com os créditos do criador.
 */
object CryoIntegrity {
    const val CREATOR = "RussoViller"
    const val OFFICIAL_REPO = "https://github.com/RussoViller/Cryogenic-Launcher"
    const val OFFICIAL_SITE = "https://russoviller.shardweb.app"
    const val LICENSE_ASSET = "legal/RVCL.md"

    /** SHA-256 do certificado de assinatura oficial (o mesmo citado na RVCL). */
    private const val OFFICIAL_CERT_SHA256 =
        "c73a7377aa07507b2f215291c91f0e2f88e16e3b88219affcef56518ac30aa56"

    @Volatile
    private var cached: Boolean? = null

    /** true = APK oficial (ou build de depuração local). */
    fun isOfficial(context: Context): Boolean {
        cached?.let { return it }
        val result = BuildConfig.DEBUG || runCatching { signerDigests(context).contains(OFFICIAL_CERT_SHA256) }
            .getOrDefault(false)
        cached = result
        return result
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(context: Context): Set<String> {
        val pm = context.packageManager
        val name = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return emptySet()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            pm.getPackageInfo(name, PackageManager.GET_SIGNATURES).signatures
        } ?: return emptySet()
        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun readLicense(context: Context): String =
        runCatching { context.assets.open(LICENSE_ASSET).bufferedReader().use { it.readText() } }.getOrDefault("")
}
