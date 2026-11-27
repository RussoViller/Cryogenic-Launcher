package com.russoviller.cryogenic.sponsor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoAlertDialog
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Fica na tela principal: mantém a lista atualizada e abre a tela de patrocinadores
 * na primeira vez que o Launcher é aberto, sempre que surge um patrocinador novo e
 * quando o botão em Sobre pede.
 */
@Composable
fun CryoSponsorsHost(onSupport: () -> Unit) {
    LaunchedEffect(Unit) { CryoSponsors.runLoop() }

    val data by CryoSponsors.data.collectAsState()
    val attempted by CryoSponsors.attempted.collectAsState()
    val openRequest by CryoSponsors.openRequest.collectAsState()
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(attempted, data?.latest) {
        if (!attempted) return@LaunchedEffect
        val introShown = AllSettings.cryoSponsorIntroShown.getValue()
        val seen = AllSettings.cryoSponsorSeen.getValue().toLongOrNull() ?: 0L
        val latest = data?.latest ?: 0L
        if (!introShown || latest > seen) show = true
    }
    LaunchedEffect(openRequest) {
        if (openRequest) {
            show = true
            CryoSponsors.consumeOpen()
        }
    }

    if (show) {
        val close = {
            AllSettings.cryoSponsorIntroShown.save(true)
            val latest = data?.latest ?: 0L
            val seen = AllSettings.cryoSponsorSeen.getValue().toLongOrNull() ?: 0L
            if (latest > seen) AllSettings.cryoSponsorSeen.save(latest.toString())
            show = false
        }
        CryoSponsorsDialog(
            onDismiss = close,
            onSupport = {
                close()
                onSupport()
            }
        )
    }
}

@Composable
fun CryoSponsorsDialog(onDismiss: () -> Unit, onSupport: () -> Unit) {
    val data by CryoSponsors.data.collectAsState()
    val loading by CryoSponsors.loading.collectAsState()
    val failed by CryoSponsors.failed.collectAsState()
    val scope = rememberCoroutineScope()
    val active = remember(data) { data?.active().orEmpty() }

    CryoAlertDialog(
        title = stringResource(R.string.cryo_sponsors_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.cryo_sponsors_support),
        onConfirm = onSupport,
        dismissText = stringResource(R.string.cryo_sponsors_close),
        onCancel = onDismiss,
        iconRes = R.drawable.ic_favorite_filled
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            //Contador grande
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
                    .border(1.dp, cryoBorderBrush(), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (data == null && loading) "…" else active.size.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_sponsors_count),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.cryo_sponsors_summary),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost
                    )
                }
            }

            when {
                active.isNotEmpty() -> active.forEachIndexed { index, sponsor ->
                    SponsorRow(position = index + 1, sponsor = sponsor, amount = data?.formatAmount(sponsor))
                }
                data == null && loading -> InfoLine(stringResource(R.string.cryo_sponsors_loading))
                data == null && failed -> InfoLine(stringResource(R.string.cryo_sponsors_offline))
                else -> InfoLine(stringResource(R.string.cryo_sponsors_empty))
            }
            if (data != null && failed) {
                InfoLine(stringResource(R.string.cryo_sponsors_offline))
            }

            //Próxima atualização (no fuso do aparelho) + atualizar agora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val next = remember(data) {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(CryoSponsors.nextUpdate()))
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cryo_sponsors_next, next),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = CryoGlassColors.Frost
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
                        .border(1.dp, cryoBorderBrush(), CircleShape)
                        .clickable(enabled = !loading) { scope.launch { CryoSponsors.refresh(force = true) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                        tint = CryoGlassColors.Ice
                    )
                }
            }
        }
    }
}

@Composable
private fun SponsorRow(position: Int, sponsor: CryoSponsor, amount: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "#$position",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = CryoGlassColors.Ice
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            if (sponsor.avatar.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    model = sponsor.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_favorite_filled),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
        }
        //Nome em cima (até 2 linhas) e, embaixo, o valor patrocinado e até quando fica na lista
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = sponsor.name.ifBlank { stringResource(R.string.cryo_sponsors_anonymous) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (amount != null) {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .cryoBackground(cryoGradient())
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                        text = amount,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = null
                        ),
                        fontWeight = FontWeight.Black,
                        color = CryoGlassColors.Deep,
                        maxLines = 1
                    )
                }
                if (sponsor.until > 0L) {
                    val until = remember(sponsor.until) {
                        DateFormat.getDateInstance(DateFormat.SHORT).format(Date(sponsor.until * 1000L))
                    }
                    Text(
                        text = stringResource(R.string.cryo_sponsors_until, until),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
            .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = CryoGlassColors.Frost
    )
}

/** Cartão da tela Sobre: quantidade atual e botão para abrir a lista. */
@Composable
fun CryoSponsorsCard(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val data by CryoSponsors.data.collectAsState()
    val count = remember(data) { data?.active()?.size }
    LaunchedEffect(Unit) { CryoSponsors.refresh() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFF6B9A).copy(alpha = 0.16f), CryoGlassColors.Deep.copy(alpha = 0.6f))
                )
            )
            .border(1.dp, cryoBorderBrush(), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFF6B9A), Color(0xFFFF9EC4)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(R.drawable.ic_favorite_filled),
                contentDescription = null,
                tint = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cryo_sponsors_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = count?.let { stringResource(R.string.cryo_sponsors_card_text, it.toString()) }
                    ?: stringResource(R.string.cryo_sponsors_loading),
                style = MaterialTheme.typography.bodySmall,
                color = CryoGlassColors.Frost
            )
        }
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .cryoBackground(cryoGradient())
                .padding(horizontal = 14.dp, vertical = 7.dp),
            text = stringResource(R.string.cryo_sponsors_view),
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Deep
        )
    }
}
