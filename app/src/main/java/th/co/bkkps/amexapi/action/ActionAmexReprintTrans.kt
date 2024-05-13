package th.co.bkkps.amexapi.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.model.TransData
import th.co.bkkps.amexapi.trans.AmexReprintTrans

class ActionAmexReprintTrans(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var origTransNo: Long = -1
    private var lastTransNo: Long = -1
    private var reprintType: Int = -1
    private var endListener = ATransaction.TransEndListener{ setResult(it) }

    fun setParam(context: Context?, origTransNo: Long, lastTransNo: Long, reprintType: Int) {
        this.context = context
        this.origTransNo = origTransNo
        this.lastTransNo = lastTransNo
        this.reprintType = reprintType
    }

    override fun process() {
        try {
            AmexReprintTrans(context!!, endListener, origTransNo, lastTransNo, reprintType).execute()
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }
}