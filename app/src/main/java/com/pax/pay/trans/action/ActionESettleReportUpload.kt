package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.action.activity.ESettleReportUploadActivity

class ActionESettleReportUpload(listener: ActionStartListener?) : AAction(listener) {

    lateinit var context: Context
    private var settleResult = 0

    fun setParam(context: Context, settleResult: Int) {
        this.context = context
        this.settleResult = settleResult
    }

    override fun process() {
        val intent = Intent(context, ESettleReportUploadActivity::class.java)
        intent.putExtra("settleResult", settleResult)
        context.startActivity(intent)
    }
}