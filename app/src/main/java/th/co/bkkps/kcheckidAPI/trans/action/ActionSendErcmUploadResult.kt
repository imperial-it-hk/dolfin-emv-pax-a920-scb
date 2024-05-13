package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.constant.EUIParamKeys
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDPrintingActivity
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDVerifyIdCardActivity

class ActionSendErcmUploadResult(listener: ActionStartListener) : AAction(listener) {

    private var mContext: Context? = null
    private var mSessionId: String? = null
    private var mErcmResult: Int = -99
    private var mAgentTransID: String? = null
    fun setParam(context: Context?, sessionId: String?, agentTransID: String, ercmUploadResult: Int) {
        mContext = context
        mSessionId = sessionId
        mErcmResult = ercmUploadResult
        mAgentTransID = agentTransID

    }

    override fun process() {
        mContext?.let{
            val intent = Intent(it, KCheckIDPrintingActivity::class.java)
            intent.putExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString(), mSessionId)
            intent.putExtra(EUIParamKeys.KCHECKID_TRANS_AGENT_ID.toString(), mAgentTransID)
            intent.putExtra(EUIParamKeys.KCHECKID_ERCM_UPLOAD_RESULT.toString(), mErcmResult)
            it.startActivity(intent)
        }?: run {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
            (this as AAction).isFinished = true
        }
    }
}