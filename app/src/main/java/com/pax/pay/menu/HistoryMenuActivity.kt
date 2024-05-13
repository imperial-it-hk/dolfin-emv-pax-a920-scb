package com.pax.pay.menu

import com.pax.edc.R
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.record.TransPreAuthQueryActivity
import com.pax.pay.record.TransQueryActivity
import com.pax.pay.trans.action.ActionAcquirerHistory
import com.pax.pay.utils.Utils
import com.pax.pay.trans.model.MerchantProfileManager.isMultiMerchantEnable
import com.pax.pay.trans.task.HistoryMerchantTask
import com.pax.pay.utils.MultiMerchantUtils
import com.pax.pay.utils.MultiMerchantUtils.Companion.isMasterMerchant
import com.pax.settings.SysParam
import com.pax.view.MenuPage
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.activity.AmexHistoryActivity

class HistoryMenuActivity: BaseMenuActivity() {

    override fun createMenuPage(): MenuPage {
        val acquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX)

        val enablePreAuth = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, false)

        val builder = MenuPage.Builder(this, 6, 2)

        if (isMultiMerchantEnable()) {
            builder.addTransItem(getString(R.string.menu_report), R.drawable.app_query, HistoryMerchantTask(this, null))
        } else {
            // default report menu
            builder.addMenuItem(getString(R.string.menu_report), R.drawable.app_query, TransQueryActivity::class.java)
        }

        // PreAuth Report menu
        if (MultiMerchantUtils.Companion.isMasterMerchant() && enablePreAuth) {
            builder.addMenuItem(getString(R.string.menu_preauth_report), R.drawable.app_query, TransPreAuthQueryActivity::class.java)
        }

        // AMEX Report menu
        acquirer?.let {
            if (MultiMerchantUtils.Companion.isMasterMerchant() && AmexTransService.isAmexAppInstalled(this)) {
                builder.addMenuItem(getString(R.string.menu_amex_report), R.drawable.app_query, AmexHistoryActivity::class.java)
            }
        }

        if (MultiMerchantUtils.Companion.isMasterMerchant() && Utils.isEnableDolfinInstalment()) {
            builder.addActionItem(getString(R.string.menu_dolfin_instalment_report), R.drawable.app_query, ActionAcquirerHistory {
                (it as ActionAcquirerHistory).setParam(this, Constants.ACQ_DOLFIN_INSTALMENT)
            })
        }
        return builder.create()
    }
}
