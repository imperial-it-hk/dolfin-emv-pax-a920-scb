package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.constant.EUIParamKeys
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDInquiryStatusActivity

class ActionKCheckIDInquiryStatus(listener: AAction.ActionStartListener) : AAction(listener) {

    private var mContext: Context? = null
    private var mSessionId: String? = null
    fun setParam(context: Context?, sessionId: String?) {
        mContext = context
        mSessionId = sessionId
    }

    override fun process() {
        mContext?.let{
            val intent = Intent(it, KCheckIDInquiryStatusActivity::class.java)
            intent.putExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString(), mSessionId)
            it.startActivity(intent)
        }?: run {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
            (this as AAction).isFinished = true
        }
    }
}