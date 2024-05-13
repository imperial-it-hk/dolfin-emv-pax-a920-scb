package com.pax.pay.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.Controller

class ActionClearReversal(listener: AAction.ActionStartListener) : AAction(listener) {
    private lateinit var context: Context
    private lateinit var selAcq : String

    fun setParam(context : Context, selAcquirer: String) {
        this.context = context
        this.selAcq = selAcquirer
    }

    override fun process() {
        // set default
        var isDone : Boolean = false

        // clear force settle as WORKED status
        Component.setSettleStatus(Controller.Constant.WORKED, selAcq)

        // clear transaction out of database
        if (selAcq == context.getString(R.string.acq_all_acquirer)) {
            isDone = FinancialApplication.getTransDataDbHelper().deleteAllDupRecord()
        } else {
            val targetAcq = FinancialApplication.getAcqManager().findAcquirer(selAcq)
            if (targetAcq!=null) {
                isDone = FinancialApplication.getTransDataDbHelper().deleteDupRecord(targetAcq)
            }
        }

        // setresult data back to transaction
        if (isDone) {
            setResult(ActionResult(TransResult.SUCC, null))
        } else {
            setResult(ActionResult(TransResult.ERR_PROCESS_FAILED, null))
        }

    }
}