package th.co.bkkps.scbapi.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.Controller
import com.pax.pay.utils.Utils

class ActionScbClearTradeVoucher(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var acquirerName: String? = null

    fun setParam(context: Context?, acquirerName: String?) {
        this.mContext = context
        this.acquirerName = acquirerName
    }

    override fun process() {
        if (acquirerName.equals(Utils.getString(R.string.acq_all_acquirer))) {
            Component.setSettleStatus(Controller.Constant.WORKED, Constants.ACQ_SCB_IPP)
            Component.setSettleStatus(Controller.Constant.WORKED, Constants.ACQ_SCB_REDEEM)
            val ipp = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP)
            val redeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM)
            FinancialApplication.getTransDataDbHelper().deleteAllTransData(ipp, false)
            FinancialApplication.getTransDataDbHelper().deleteAllTransData(redeem, false)
            FinancialApplication.getTransMultiAppDataDbHelper().deleteAllTransData(ipp)
            FinancialApplication.getTransMultiAppDataDbHelper().deleteAllTransData(redeem)
        } else {
            Component.setSettleStatus(Controller.Constant.WORKED, acquirerName)
            val acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName)
            FinancialApplication.getTransDataDbHelper().deleteAllTransData(acquirer, false)
            FinancialApplication.getTransDataDbHelper().deleteAllTransData(acquirer, false)
        }
        setResult(ActionResult(TransResult.SUCC, null))
        //TODO-Implement to call API clear trade voucher
    }
}