package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.activity.PrintReportActivity
import com.pax.pay.trans.action.activity.SelectMerchantActivity
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.IS_SAVE_CURRENT_MERCHANT
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.SELECT_ALL_MERCHANT_ENABLE


class ActionPrintMerchantReport(listener: ActionStartListener?) : AAction(listener) {

    private lateinit var context: Context
    private var reportType: String = ""

    fun setParam(context: Context, reportType: String) {
        this.context = context
        this.reportType = reportType
    }


    override fun process() {
        val intent = Intent(
            context,
            PrintReportActivity::class.java
        )
        val bundle = Bundle()
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "Print Report")
        bundle.putString(PrintReportActivity.REPORT_TYPE, reportType)
        intent.putExtras(bundle)
        context.startActivity(intent)
    }
}