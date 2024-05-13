package com.pax.pay.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication

class ActionDeletePreAuthExpired(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null

    fun setParam(mContext: Context?) {
        this.mContext = mContext
    }

    override fun process() {
        FinancialApplication.getApp().runInBackground {
            FinancialApplication.getTransDataDbHelper().deleteAllPreAuthTransactionExpired()
            setResult(ActionResult(TransResult.SUCC, null))
        }
    }
}