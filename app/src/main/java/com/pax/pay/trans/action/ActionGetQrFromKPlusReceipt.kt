package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.trans.action.activity.ThaiQrVerifyPaySlipActivity
import com.pax.pay.trans.model.TransData

class ActionGetQrFromKPlusReceipt(listener: ActionStartListener) : AAction(listener) {

    fun setParam (context : Context, transData: TransData) {

        this.context = context
        this.transData = transData
    }

    var listener: ActionStartListener = listener
    lateinit var context: Context
    lateinit var transData: TransData

    override fun process() {
        val intent = Intent(context, ThaiQrVerifyPaySlipActivity::class.java)
        context.startActivity(intent)
    }
}