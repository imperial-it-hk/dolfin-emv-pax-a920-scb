package th.co.bkkps.amexapi.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.amexapi.trans.AmexVoidTrans

class ActionAmexVoidTrans(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var origTransNo: Long = -1
    private var lastTransNo: Long = -1
    private var ecrProcListener: ActionEndListener? = null
    private var endListener = ATransaction.TransEndListener{ setResult(it) }

    fun setParam(context: Context?, origTransNo: Long, lastTransNo: Long, ecrProcListener: ActionEndListener?) {
        this.context = context
        this.origTransNo = origTransNo
        this.lastTransNo = lastTransNo
        this.ecrProcListener = ecrProcListener
    }

    override fun process() {
        try {
            AmexVoidTrans(context!!, endListener, origTransNo, lastTransNo, ecrProcListener).execute()
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }
}