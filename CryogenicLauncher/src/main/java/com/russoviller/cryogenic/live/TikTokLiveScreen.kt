package com.russoviller.cryogenic.live

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor

/** Tela "Live TikTok" do launcher (mesma função do TikTok do Painel da Live do jogo). */
@Composable
fun TikTokLiveScreen(
    key: NormalNavKey.TikTokLive,
    currentKey: TitledNavKey?
) {
    BaseScreen(screenKey = key, currentKey = currentKey) { isVisible ->
        if (!isVisible) return@BaseScreen
        TikTokLiveLauncherContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp),
            color = cardColor(false),
            contentColor = onCardColor()
        )
    }
}
