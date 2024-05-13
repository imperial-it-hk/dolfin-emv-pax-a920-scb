package th.co.bkkps.amexapi.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import th.co.bkkps.amexapi.AmexTransAPI

class ActionAmexClearReversal(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var acquirerName: String? = null

    fun setParam(context: Context?, acquirerName: String?) {
        this.mContext = context
        this.acquirerName = acquirerName
    }

    override fun process() {
        AmexTransAPI.getInstance().process.doClearReversal(mContext!!)
        setResult(ActionResult(TransResult.SUCC, null))
    }
}