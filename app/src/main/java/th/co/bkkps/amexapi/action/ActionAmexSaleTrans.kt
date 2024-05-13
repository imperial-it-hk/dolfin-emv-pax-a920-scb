package th.co.bkkps.amexapi.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.emv.EmvSP200
import com.pax.pay.trans.model.TransData
import th.co.bkkps.amexapi.trans.AmexSaleTrans

class ActionAmexSaleTrans(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var origTransData: TransData? = null
    private var emvSP200: EmvSP200? = null
    private var ecrProcListener: ActionEndListener? = null
    private var endListener = ATransaction.TransEndListener{ setResult(it) }

    fun setParam(context: Context?, origTransData: TransData?, emvSP200: EmvSP200?, ecrProcListener: ActionEndListener?) {
        this.context = context
        this.origTransData = origTransData
        this.emvSP200 = emvSP200
        this.ecrProcListener = ecrProcListener
    }

    override fun process() {
        try {
            AmexSaleTrans(context!!, endListener, origTransData!!, emvSP200, ecrProcListener).setBackToMain(false).execute()
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }
}