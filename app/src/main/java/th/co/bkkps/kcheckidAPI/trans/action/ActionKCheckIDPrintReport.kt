package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import android.drm.DrmStore
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.TransContext
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDPrintReportActivity
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDVerifyIdCardActivity

class ActionKCheckIDPrintReport(listener: AAction.ActionStartListener) : AAction(listener) {


    enum class processMode (val intValue: Int) {
        DETAIL_REPORT(0),
        SUMMARY_REPORT(1)
    }

    var context: Context? = null
    var mode: processMode? = null
    fun setParam(context: Context, mode: processMode) {
        this.context = context
        this.mode = mode
    }

    override fun process() {
        context?.let {
            val intent = Intent(it, KCheckIDPrintReportActivity::class.java)
            intent.putExtra("MODE", mode?.intValue)
            it.startActivity(intent)
        }?:run{
            setResult(ActionResult(TransResult.ERR_PARAM, null))
            (this as AAction).isFinished = true
        }
    }

    override fun setResult(result: ActionResult?) {
        super.setResult(result)

    }
}