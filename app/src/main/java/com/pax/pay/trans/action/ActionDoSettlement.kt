package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.activity.DoSettlementActivity
import com.pax.pay.trans.action.activity.SelectReportActivity


class ActionDoSettlement(listener: ActionStartListener?) : AAction(listener) {

    private lateinit var context: Context
    private var acqList: ArrayList<String>? =null
    private var isAllMerchantSettle : Boolean = false

    fun setParam(context: Context, isAllAcquirerSettle: Boolean, acqList: ArrayList<String>?) {
        this.context = context
        this.acqList = acqList
        this.isAllMerchantSettle = isAllAcquirerSettle
    }

    override fun process() {
        val intent = Intent(
            context,
            DoSettlementActivity::class.java
        )
        val bundle = Bundle()
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "Settlement")
        bundle.putBoolean("isAllMerchantSettle", isAllMerchantSettle)
        bundle.putStringArrayList("SettleListAcquirer", acqList)
        intent.putExtras(bundle)

        context.startActivity(intent)
    }

}