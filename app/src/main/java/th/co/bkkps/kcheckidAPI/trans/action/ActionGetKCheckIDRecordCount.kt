package th.co.bkkps.kcheckidAPI.trans.action

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDPresettleActivity
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDSettingActivity
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDSettleActivity
import kotlin.concurrent.thread

class ActionGetKCheckIDRecordCount(listener: ActionStartListener?) : AAction(listener) {

    private var context: Context? = null
    fun setParam(context: Context) {
        this.context = context
    }

    override fun process() {
        context?.let {
            val intent = Intent(it, KCheckIDPresettleActivity::class.java)
            it.startActivity(intent)
        } ?: run {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
            (this as AAction).isFinished = true
        }
    }
}