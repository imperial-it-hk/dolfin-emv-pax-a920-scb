package th.co.bkkps.kcheckidAPI.trans.action

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.drm.DrmStore
import android.util.Log
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.kbank.bpskcheckidapi.KCheckIDVerifyMsg
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.Convert
import com.pax.pay.utils.EReceiptUtils
import com.pax.settings.SysParam
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDUploadEReceiptActivity

class ActionUploadEReceipt(listener: ActionStartListener) : AAction(listener) {

    private var mContext : Context? = null
    //private var mTransData : TransData? = null
    private var mResponseMessage : KCheckIDVerifyMsg.Companion.Response? = null
    private var mSessionId : String? = null

    fun setParam(context: Context, responseMessage: KCheckIDVerifyMsg.Companion.Response?, sessionId: String?) {
        this.mContext = context
        this.mResponseMessage = responseMessage
        this.mSessionId=sessionId
    }


    override fun process() {
        mContext?.let {
            val intent = Intent(it, KCheckIDUploadEReceiptActivity::class.java)
            intent.putExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString(), mSessionId)
            intent.putExtra(EUIParamKeys.KCHECKID_ERCM_REFERENCE_ID.toString(), mResponseMessage?.getRequestReferenceId())
            intent.putExtra(EUIParamKeys.KCHECKID_ERCM_ERECEIPT_DATA.toString(), mResponseMessage?.getErmReceiptData())
            it.startActivity(intent)
        }?:run{
            setResult(ActionResult(TransResult.ERR_PARAM,null))
            Log.e(TAG, "missing context for start activity")
        }
    }


}