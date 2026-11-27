package com.russoviller.cryogenic.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoAlertDialog
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.MarkdownView
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient

private val UnofficialRed = Color(0xFFE5484D)

/**
 * Faixa fixa no topo quando o APK não tem a assinatura oficial de RussoViller.
 * Não pode ser fechada: toca para abrir o repositório oficial.
 */
@Composable
fun CryoUnofficialBanner(modifier: Modifier = Modifier, openLink: (String) -> Unit) {
    val context = LocalContext.current
    val official = remember { CryoIntegrity.isOfficial(context) }
    if (official) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UnofficialRed.copy(alpha = 0.92f))
            .clickable { openLink(CryoIntegrity.OFFICIAL_REPO) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(R.drawable.ic_warning_filled),
            contentDescription = null,
            tint = Color.White
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.cryo_unofficial_build),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Cartão da tela Sobre: autoria, base do projeto e a licença RVCL completa. */
@Composable
fun CryoLicenseCard(modifier: Modifier = Modifier, openLink: (String) -> Unit) {
    val context = LocalContext.current
    var showLicense by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CryoGlassColors.Deep.copy(alpha = 0.6f))
            .border(1.dp, cryoBorderBrush(), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.cryo_license_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Cryogenic Launcher — created by ${CryoIntegrity.CREATOR}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice,
            modifier = Modifier.clickable { openLink(CryoIntegrity.OFFICIAL_REPO) }
        )
        Text(
            text = stringResource(R.string.cryo_license_owner),
            style = MaterialTheme.typography.bodySmall,
            color = CryoGlassColors.Frost
        )
        Text(
            text = stringResource(R.string.cryo_license_base),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.75f)
        )
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .cryoBackground(cryoGradient())
                .clickable { showLicense = true }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            text = stringResource(R.string.cryo_license_read),
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Deep
        )
    }

    if (showLicense) {
        val text = remember { CryoIntegrity.readLicense(context) }
        CryoAlertDialog(
            title = "RVCLapk1.1",
            onDismissRequest = { showLicense = false },
            confirmText = stringResource(R.string.cryo_sponsors_close),
            onConfirm = { showLicense = false }
        ) {
            MarkdownView(content = text)
        }
    }
}
