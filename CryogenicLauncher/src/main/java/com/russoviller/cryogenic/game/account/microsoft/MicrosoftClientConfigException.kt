package com.russoviller.cryogenic.game.account.microsoft

import com.russoviller.cryogenic.R

class MicrosoftClientConfigException(val reason: Reason) : RuntimeException(reason.name) {
    enum class Reason(val messageRes: Int) {
        MISSING_CLIENT_ID(R.string.cryo_ms_error_missing_client_id),
        APP_NOT_FOUND(R.string.cryo_ms_error_app_not_found),
        PUBLIC_FLOW_DISABLED(R.string.cryo_ms_error_public_flow_disabled),
        APP_NOT_APPROVED(R.string.cryo_ms_error_app_not_approved)
    }
}
