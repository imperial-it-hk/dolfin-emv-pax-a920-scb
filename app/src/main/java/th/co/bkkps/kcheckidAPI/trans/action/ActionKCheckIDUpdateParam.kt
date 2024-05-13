package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDSettingActivity

class ActionKCheckIDUpdateParam(listener: ActionStartListener?) : AAction(listener) {

    private var mContext: Context? = null
    fun setParam(context: Context?) {
        mContext = context
    }

    override fun process() {
        mContext?.let {
            val intent = Intent(it, KCheckIDSettingActivity::class.java)
            it.startActivity(intent)
        }?: run {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
            (this as AAction).isFinished = true
        }
    }
}