package com.pax.pay.menu

import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.utils.MultiMerchantUtils
import com.pax.pay.utils.MultiMerchantUtils.Companion.isMasterMerchant
import com.pax.pay.utils.Utils
import com.pax.view.MenuPage
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.menu.ScbRedeemMenuActivity

class RedemptionRootMenuActivity : BaseMenuActivity() {

    private val listener : ATransaction.TransEndListener = ATransaction.TransEndListener { result -> finish(result) }

    override fun onStart() {
        super.onStart()
        super.setTitle(R.string.menu_redemption_str)
    }

    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this, 9, 3)

        // 1. KBANK
        if (Utils.isEnableRedeemKbank()) {
            builder.addMenuItem(getString(R.string.menu_bybank_kbank), R.drawable.icon_rewardpoint, RedeemedMenuActivity::class.java)
        }

        // 2. SCB
        if (MultiMerchantUtils.Companion.isMasterMerchant() && ScbIppService.isSCBInstalled(this) && Utils.isEnableScbRedeem()) {
            builder.addMenuItem(getString(R.string.menu_bybank_scb), R.drawable.icons_scb, ScbRedeemMenuActivity::class.java)
        }

        return builder.create()
    }
}