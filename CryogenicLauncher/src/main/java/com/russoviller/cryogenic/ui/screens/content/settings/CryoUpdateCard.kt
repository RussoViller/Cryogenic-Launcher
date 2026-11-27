package com.russoviller.cryogenic.ui.screens.content.settings

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.viewmodel.CRYO_RELEASES_PAGE

@Composable
fun CryoUpdateCard(
    modifier: Modifier = Modifier,
    checkUpdate: () -> Unit,
    openLink: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cryoGlass(shape = cryoChamferShape(18.dp), cornerRadius = 18.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(cryoChamferShape(10.dp))
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_download_2_filled),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cryo_update_kicker),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                    color = CryoGlassColors.Ice
                )
                Text(
                    text = stringResource(R.string.cryo_update_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
            }
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
        Text(
            text = stringResource(R.string.cryo_update_summary),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .clickable { AllSettings.cryoAutoCheckUpdate.save(!AllSettings.cryoAutoCheckUpdate.getValue()) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cryo_update_auto),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            DefaultSwitch(
                checked = AllSettings.cryoAutoCheckUpdate.state,
                onCheckedChange = { AllSettings.cryoAutoCheckUpdate.save(it) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CryoButton(
                modifier = Modifier.weight(1f),
                onClick = checkUpdate
            ) {
                Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_refresh), contentDescription = null)
                MarqueeText(text = stringResource(R.string.cryo_update_check))
            }
            CryoTonalButton(
                modifier = Modifier.weight(1f),
                onClick = { openLink(CRYO_RELEASES_PAGE) }
            ) {
                Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_link), contentDescription = null)
                MarqueeText(text = stringResource(R.string.cryo_update_releases))
            }
        }
    }
}
