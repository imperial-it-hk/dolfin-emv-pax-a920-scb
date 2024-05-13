package com.pax.pay.menu

import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.trans.action.ActionSearchCard
import com.pax.pay.trans.action.activity.SearchCardActivity
import com.pax.pay.trans.action.ActionSearchCard.SearchMode
import com.pax.pay.trans.action.ActionSearchCard.SearchMode.SWIPE
import com.pax.pay.trans.action.ActionSearchCard.SearchMode.INSERT
import com.pax.pay.trans.action.ActionSearchCard.SearchMode.KEYIN
import com.pax.pay.utils.MultiMerchantUtils
import com.pax.pay.utils.MultiMerchantUtils.Companion.isMasterMerchant
import com.pax.pay.utils.Utils
import com.pax.view.MenuPage
import th.co.bkkps.edc.SubMenuActivityBay
import th.co.bkkps.edc.trans.InstalmentAmexTrans
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.menu.ScbIppMenuActivity
import kotlin.experimental.or

class InstalmentRootMenuActivity : BaseMenuActivity() {

    private val listener : ATransaction.TransEndListener = ATransaction.TransEndListener { result -> finish(result) }

    override fun onStart() {
        super.onStart()
        super.setTitle(R.string.menu_instalment_str)
    }


    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this, 9, 3)

        // 1. KBANK
        if (Utils.isEnableInstalmentKbank() || Utils.isEnableInstalmentKbankBdms()) {
            builder.addMenuItem(getString(R.string.menu_bybank_kbank), R.drawable.icon_smartpay, InstalmentMenuActivity::class.java)
        }

        // 2. AMEX-EPP
        if (MultiMerchantUtils.Companion.isMasterMerchant() && Utils.isEnableAmexInstalment()) {
            val amexEppTrans = InstalmentAmexTrans(this, (SWIPE or INSERT or KEYIN) as Byte, true, listener)
            builder.addTransItem(getString(R.string.menu_bybank_amex), R.drawable.icon_amex, amexEppTrans)
        }

        // 3. BAY-IPP
        if (MultiMerchantUtils.Companion.isMasterMerchant() && Utils.isEnableBay()) {
            builder.addMenuItem(getString(R.string.menu_bybank_bay), R.drawable.icons_bay, SubMenuActivityBay::class.java)
        }

        // 4. SCB-IPP
        if (MultiMerchantUtils.Companion.isMasterMerchant() && ScbIppService.isSCBInstalled(this) && Utils.isEnableScbIpp()) {
            builder.addMenuItem(getString(R.string.menu_bybank_scb), R.drawable.icons_scb, ScbIppMenuActivity::class.java)
        }

        return builder.create()
    }


}