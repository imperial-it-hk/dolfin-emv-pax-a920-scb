package th.co.bkkps.scbapi.trans.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction.TransEndListener
import com.pax.pay.trans.model.TransData
import th.co.bkkps.scbapi.trans.ScbVoidTran

class ActionScbVoidTrans(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var origTransData: TransData? = null

    fun setParam(context: Context?, origTransData: TransData?) {
        this.mContext = context
        this.origTransData = origTransData
    }

    override fun process() {
        ScbVoidTran(mContext, endListener, origTransData).execute()
    }

    private val endListener = TransEndListener { result -> setResult(result) }
}