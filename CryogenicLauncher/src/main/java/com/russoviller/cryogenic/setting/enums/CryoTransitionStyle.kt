package com.russoviller.cryogenic.setting.enums

import com.russoviller.cryogenic.R

/** Estilo da animação de entrada dos itens ao trocar de página. */
enum class CryoTransitionStyle(val textRes: Int) {
    DEFAULT(R.string.cryo_trans_default),
    FADE(R.string.cryo_trans_fade),
    SLIDE_UP(R.string.cryo_trans_slide_up),
    SLIDE_DOWN(R.string.cryo_trans_slide_down),
    SLIDE_LEFT(R.string.cryo_trans_slide_left),
    SLIDE_RIGHT(R.string.cryo_trans_slide_right),
    BOUNCE(R.string.cryo_trans_bounce),
    JELLY(R.string.cryo_trans_jelly),
    ZOOM(R.string.cryo_trans_zoom),
    FLIP(R.string.cryo_trans_flip)
}
