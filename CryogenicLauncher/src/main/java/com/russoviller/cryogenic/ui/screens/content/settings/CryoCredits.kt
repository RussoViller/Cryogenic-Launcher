package com.russoviller.cryogenic.ui.screens.content.settings

import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSectionHeader
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient

private class CreditEntry(
    val name: String,
    @param:StringRes val roleRes: Int,
    val url: String
)

private val credits = listOf(
    CreditEntry("Zalith Launcher 2", R.string.cryo_credit_zl2, "https://github.com/ZalithLauncher/ZalithLauncher2"),
    CreditEntry("PojavLauncher", R.string.cryo_credit_pojav, "https://github.com/PojavLauncherTeam/PojavLauncher"),
    CreditEntry("Fold Craft Launcher (FCL)", R.string.cryo_credit_fcl, "https://github.com/FCL-Team/FoldCraftLauncher"),
    CreditEntry("HMCL", R.string.cryo_credit_hmcl, "https://github.com/HMCL-dev/HMCL"),
    CreditEntry("PCL2", R.string.cryo_credit_pcl, "https://github.com/Meloong-Git/PCL"),
    CreditEntry("Zalith Launcher 2 Plus", R.string.cryo_credit_zl2plus, "https://github.com/Star1xr/ZalithLauncher2Plus"),
    CreditEntry("MinecraftCapes", R.string.cryo_credit_minecraftcapes, "https://minecraftcapes.net"),
    CreditEntry("Ely.by", R.string.cryo_credit_elyby, "https://ely.by"),
    CreditEntry("Aerix Launcher", R.string.cryo_credit_aerix, "https://github.com/entitybrian69-bit/Aerix-launcher"),
    CreditEntry("antisocialcord-kt", R.string.cryo_credit_antisocialcord, "https://github.com/Nightdavisao/antisocialcord-kt"),
    CreditEntry("Modrinth", R.string.cryo_credit_modrinth, "https://github.com/modrinth/code"),
    CreditEntry("BMCLAPI", R.string.cryo_credit_bmclapi, "https://bmclapidoc.bangbang93.com"),
    CreditEntry("MCIM", R.string.cryo_credit_mcim, "https://github.com/mcmod-info-mirror/mcim")
)

@Composable
fun CryoCreditsSection(
    modifier: Modifier = Modifier,
    openLink: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CryoSectionHeader(title = stringResource(R.string.cryo_credits_title))
        Text(
            text = stringResource(R.string.cryo_credits_summary),
            style = MaterialTheme.typography.bodySmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
        )
        credits.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { entry ->
                    CreditTile(
                        modifier = Modifier.weight(1f),
                        entry = entry,
                        onClick = { openLink(entry.url) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreditTile(
    modifier: Modifier,
    entry: CreditEntry,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.10f), CryoGlassColors.Navy.copy(alpha = 0.35f))))
            .border(1.dp, cryoBorderBrush(), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.name.take(1),
                style = MaterialTheme.typography.titleMedium.copy(shadow = null),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = stringResource(entry.roleRes),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                maxLines = 3
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
                tint = CryoGlassColors.Ice
            )
        }
    }
}
