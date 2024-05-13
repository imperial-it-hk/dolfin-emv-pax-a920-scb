package th.co.bkkps.scbapi.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import th.co.bkkps.scbapi.trans.action.activity.ScbVoidActivity

class ActionScbVoidApi(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var traceNo: Long = 0

    fun setParam(context: Context?, mode: Long) {
        this.mContext = context
        this.traceNo = mode
    }

    override fun process() {
        val intent = Intent(mContext, ScbVoidActivity::class.java)
        intent.putExtra(EUIParamKeys.SCB_TRACE_NO.toString(), traceNo)
        mContext!!.startActivity(intent)
    }
}