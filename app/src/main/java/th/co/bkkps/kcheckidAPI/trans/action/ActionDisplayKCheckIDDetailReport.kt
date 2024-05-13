package th.co.bkkps.kcheckidAPI.trans.action

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDDisplayDetailReportActivity

class ActionDisplayKCheckIDDetailReport(listener: ActionStartListener) : AAction(listener){

    private var mContext : Context? = null
    private var endListener : AAction.ActionEndListener? =null
    fun setParam(context: Context?) {
        this.mContext = context
    }

    override fun setEndListener(listener: ActionEndListener?) {
        super.setEndListener(listener)
        endListener = listener
    }


    override fun process() {
        mContext?.let{
            val intent = Intent(it, KCheckIDDisplayDetailReportActivity::class.java)
            it.startActivity(intent)
        }?:run{
            this.isFinished=true
            setResult(ActionResult(TransResult.ERR_PARAM, null))
        }
    }

    override fun setResult(result: ActionResult?) {
        super.setResult(result)
        endListener?.onEnd(this, result)
    }
}