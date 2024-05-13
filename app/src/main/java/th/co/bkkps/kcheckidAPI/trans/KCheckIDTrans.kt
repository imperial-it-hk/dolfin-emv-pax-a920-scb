package th.co.bkkps.kcheckidAPI.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.kbank.bpskcheckidapi.KCheckIDVerifyMsg
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.Convert
import com.pax.pay.utils.EReceiptUtils
import com.pax.settings.SysParam
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.trans.action.*
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDVerifyIdCardActivity
import th.co.bkkps.utils.Log

class KCheckIDTrans(val context: Context, val transType: ETransType, val pcsType: ProcessType, val listener: TransEndListener?) : BaseTrans( context, transType, listener) {

    companion object {
        enum class ProcessType { VERIFY, INQUIRY }
        enum class State {
            SEND_UPDATE_CONFIG,
            VERIFY_IDCARD,
            INQUIRY_STATUS,
            ERECEIPT_UPLOAD_AFTER_VERIFY,
            ERECEIPT_UPLOAD_AFTER_INQUIRY,
            PRINT }
    }

    override fun bindStateOnAction() {
        val actionKCheckIDUpdateParam = ActionKCheckIDUpdateParam(AAction.ActionStartListener {
            (it as ActionKCheckIDUpdateParam).setParam(FinancialApplication.getApp().applicationContext)
        })
        bind(State.SEND_UPDATE_CONFIG.toString(), actionKCheckIDUpdateParam,false)


        val actionVerifyIDCard = ActionKCheckIDVerifyIdCard(AAction.ActionStartListener {
            (it as ActionKCheckIDVerifyIdCard).setParam(FinancialApplication.getApp().applicationContext, sessionId)
        })
        bind(State.VERIFY_IDCARD.toString(), actionVerifyIDCard, false)

        val actionInquiryStatus = ActionKCheckIDInquiryStatus(AAction.ActionStartListener {
            (it as ActionKCheckIDInquiryStatus).setParam(FinancialApplication.getApp().applicationContext, sessionId)
        })
        bind(State.INQUIRY_STATUS.toString(), actionInquiryStatus, false)


        val actionUploadERecipt = ActionUploadEReceipt(AAction.ActionStartListener {
            ((it as ActionUploadEReceipt).setParam(FinancialApplication.getApp().applicationContext, verifyIdCardResponse, sessionId))
        })
        bind(State.ERECEIPT_UPLOAD_AFTER_VERIFY.toString(), actionUploadERecipt, false)


        val actionSendErcmUploadResult = ActionSendErcmUploadResult(AAction.ActionStartListener {
            verifyIdCardResponse?.let {
                (it as ActionSendErcmUploadResult).setParam(FinancialApplication.getApp().applicationContext, sessionId, agentTransID!!, ercmUploadResult)
            }
        })
        bind(State.PRINT.toString(), actionSendErcmUploadResult, false)

        if (Component.chkSettlementStatus(Constants.ACQ_KCHECKID)) {
            transEnd(ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null))
        } else {
            gotoState(State.SEND_UPDATE_CONFIG.toString())
        }
    }



    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (currentState) {
            Companion.State.SEND_UPDATE_CONFIG.toString()           -> { afterSetConfig(result) }
            Companion.State.VERIFY_IDCARD.toString()                -> { afterVerifyIdCard(result) }
            Companion.State.INQUIRY_STATUS.toString()               -> { afterInquiryStatus(result) }
            Companion.State.ERECEIPT_UPLOAD_AFTER_VERIFY.toString() -> { afterEReceiptUpload(ProcessType.VERIFY, result) }
            Companion.State.ERECEIPT_UPLOAD_AFTER_VERIFY.toString() -> { afterEReceiptUpload(ProcessType.INQUIRY,result) }
            Companion.State.PRINT.toString()                        -> {  }
        }
    }


    var ercmUploadResult : Int = -99
    fun afterEReceiptUpload(type: ProcessType, result: ActionResult?) {
        ercmUploadResult = result?.ret!!
        gotoState(State.PRINT.toString())
    }

    var sessionId: String? = null
    fun afterSetConfig(result: ActionResult?) {
        result?.let {
            sessionId = it.data as String?
            when (pcsType) {
                ProcessType.VERIFY -> gotoState(State.VERIFY_IDCARD.toString())
                ProcessType.INQUIRY-> gotoState(State.INQUIRY_STATUS.toString())
            }
        }?:run{
            Log.d(TAG, "Missing --- KCheckID --- afterSetConfig --- result")
        }
    }


    var agentTransID : String? = null
    var verifyIdCardResponse : KCheckIDVerifyMsg.Companion.Response? = null
    fun afterVerifyIdCard(result: ActionResult?) {
        result?.let {
            verifyIdCardResponse = it.data as KCheckIDVerifyMsg.Companion.Response
            verifyIdCardResponse?.let {
                agentTransID = it.getAgentTransId()
                Component.transInit(transData, FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KCHECKID))
                transData.transType = ETransType.KCHECKID_DUMMY

                val allowUoload = it.getErmAllowUploadFlag()
                val eReceiptData: ByteArray? = it.getErmReceiptData()
                if (allowUoload && eReceiptData != null) {
                    Log.d(KCheckIDVerifyIdCardActivity.TAG, "onActivityResult --- eReceipt data len = ${eReceiptData.size} bytes")
                    gotoState(State.ERECEIPT_UPLOAD_AFTER_VERIFY.toString())
                } else {
                    gotoState(State.PRINT.toString())
                }
            }?:run{
                gotoState(State.PRINT.toString())
            }
        }?:run{
            Log.d(TAG, "Missing --- KCheckID --- afterVerifyIdCard --- result")
        }
    }

    var inquiryIdCardResponse : KCheckIDVerifyMsg.Companion.Response? = null
    fun afterInquiryStatus(result: ActionResult?) {
        result?.let {
            inquiryIdCardResponse = it.data as KCheckIDVerifyMsg.Companion.Response
            gotoState(State.ERECEIPT_UPLOAD_AFTER_INQUIRY.toString())
        }?:run{
            Log.d(TAG, "Missing --- KCheckID --- afterInquiryStatus --- result")
        }
    }




}