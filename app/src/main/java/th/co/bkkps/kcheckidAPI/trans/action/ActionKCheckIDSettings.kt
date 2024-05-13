package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDConfigurationActivity

class ActionKCheckIDSettings(listener: ActionStartListener) : AAction(listener) {

    private var context : Context?=null
    private var endListener : AAction.ActionEndListener? =null
    fun setParam(context: Context) {
        this.context = context
    }


    override fun setEndListener(listener: ActionEndListener?) {
        super.setEndListener(listener)
        endListener = listener
    }

     override fun process() {
         context?.let {
             val intent = Intent(it, KCheckIDConfigurationActivity::class.java)
             it.startActivity(intent)
         }?:run{
             this.isFinished = true
             this.setResult(ActionResult(TransResult.ERR_PARAM, null))
         }
    }

    override fun setResult(result: ActionResult?) {
        super.setResult(result)
        endListener?.onEnd(this, result)
    }
}