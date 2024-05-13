package th.co.bkkps.scbapi.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.utils.Utils

class ActionScbClearReversal(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var acquirerName: String? = null

    fun setParam(context: Context?, acquirerName: String?) {
        this.mContext = context
        this.acquirerName = acquirerName
    }

    override fun process() {
        if (acquirerName.equals(Utils.getString(R.string.acq_all_acquirer))) {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP))
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM))
        } else {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(FinancialApplication.getAcqManager().findAcquirer(acquirerName))
        }
        setResult(ActionResult(TransResult.SUCC, null))
        //TODO-Implement to call API clear reversal
    }
}