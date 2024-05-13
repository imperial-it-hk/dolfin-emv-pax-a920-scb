package com.pax.pay.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.Constants
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.transmit.TransProcessListener
import com.pax.pay.trans.transmit.TransProcessListenerImpl
import com.pax.pay.trans.transmit.Transmit

class ActionVerifyPaySlipThaiQr(val listener: ActionStartListener?) : AAction(listener) {


    fun setParam( context: Context, transData :  TransData) {
        this.context = context
        this.transData = transData
    }

    lateinit var context: Context
    lateinit var transData: TransData

    override fun process() {

        FinancialApplication.getApp().runInBackground {
            val transProcessListenerImpl = TransProcessListenerImpl(context)
            var ret : Int = -9999
            ret = Transmit().transmitThaiQrVerifyPaySlip(transData, transProcessListenerImpl)
            transProcessListenerImpl.onHideProgress()
            setResult(ActionResult(ret, null))
        }
    }


    override fun setResult(result: ActionResult?) {
        if (TransContext.getInstance().currentAction == null || isFinished) {
            return
        }
        TransContext.getInstance().currentAction?.isFinished = true
        TransContext.getInstance().currentAction = null
        super.setResult(result)
    }
}