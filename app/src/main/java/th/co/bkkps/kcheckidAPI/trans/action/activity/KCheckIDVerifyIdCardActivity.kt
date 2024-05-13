package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.Convert
import com.pax.pay.utils.EReceiptUtils
import com.pax.settings.SysParam
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.kcheckidAPI.trans.KCheckIDTrans
import th.co.bkkps.utils.Log
import java.text.SimpleDateFormat
import java.util.*

class KCheckIDVerifyIdCardActivity : Activity() {
    companion object {
        const val TAG ="KCheckIDVerifyIdCard"
    }

    lateinit var transAPI : ITransAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        val sessionID : String? = intent?.getStringExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString())
        sessionID?:run{
            Log.d(TAG, "onCreate missing sessionId")
            finish(ActionResult(TransResult.ERR_PARAM, null))
            return
        }

        val stanNumber : Int  = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1)
        val verifyReq = KCheckIDVerifyMsg.Companion.Request()
        verifyReq.setSessionId(sessionID)
        verifyReq.setStanNo(stanNumber.toLong())
        verifyReq.setDateTime(SimpleDateFormat("yyyyMMdd HHmmss", Locale.US).format(Date()))
        transAPI.startTrans(this, verifyReq as BaseRequest)
        Log.d(KCheckIDSettingActivity.TAG, "onCreate --- startTransCalled")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            var resp = transAPI.onResult(requestCode, resultCode, data) as KCheckIDVerifyMsg.Companion.Response?

            resp?.let { msg->
                Log.d(TAG, "onActivityResult --- KCheckIDVerify sessionId is '${msg.getSessionID()}'")

                val transData = TransData()
                Component.transInit(transData, FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KCHECKID))
                transData.transType = ETransType.KCHECKID_DUMMY

                val allowUoload = msg.getErmAllowUploadFlag()
                val eReceiptData: ByteArray? = msg.getErmReceiptData()
                if (allowUoload && eReceiptData != null) {
                    Log.d(KCheckIDVerifyIdCardActivity.TAG, "onActivityResult --- eReceipt data len = ${eReceiptData.size} bytes")
                    uploadEReceipt(msg)
                } else {
                    if (msg.getApiResultCode() != TransResult.SUCC) {
                        Device.beepErr()
                    }
                    finish(ActionResult(msg.getApiResultCode(),null))
                }
            }?:run{
                Log.d(TAG, "onActivityResult --- responseData is null")
                finish(ActionResult(TransResult.ERR_PARAM, null))
            }
        } catch (e: Exception) {
            Log.d(TAG, "onActivityResult --- internal error during process the result")
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }

    var eReceiptData : ByteArray? = null
    fun uploadEReceipt(resp: KCheckIDVerifyMsg.Companion.Response) {
        try {
            eReceiptData = resp.getErmReceiptData()
            eReceiptData?.let {
                doUploadReceipt(resp)
            }?:run {
                Log.d(TAG, "onActivityResult --- internal error during process the result")
            }
        } catch (e: Exception) {
            Log.d(TAG, "onActivityResult --- exception on upload ERCM for KCheckID")
        }
    }

    var printCountFlag : Boolean = false
    fun doUploadReceipt(resp: KCheckIDVerifyMsg.Companion.Response) {
        val agentTransID = resp.getAgentTransId()
        val listener = AAction.ActionStartListener{
            var transData = TransData()
            val acquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KCHECKID)
            Component.transInit(transData, acquirer)
            transData.stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1).toLong()
            transData.traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO, 1).toLong()
            transData.batchNo = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KCHECKID).currBatchNo.toLong()
            transData.seteSlipFormat(eReceiptData)

            transData.initAcquirerIndex = EReceiptUtils.StringPadding(acquirer.getId().toString(), 3, "0", Convert.EPaddingPosition.PADDING_LEFT)
            transData.initAcquirerNii = "000"
            transData.initAcquirerName = acquirer.getName()
            transData.pan = "XXXXXXXXX0000"
            transData.amount = "0000000000"
            transData.refNo = resp.getRequestReferenceId()
            transData.authCode = "000000"
            transData.authCode = "000000"
            transData.acquirer = acquirer
            transData.reversalStatus = TransData.ReversalStatus.NORMAL
            transData.transType = ETransType.KCHECKID_DUMMY
            transData.transState = TransData.ETransStatus.NORMAL
            transData.expDate = "XXXX"

            lateinit var refNo : String
            resp.getRequestReferenceId()?.let {
                refNo = it
            }?:run{
                refNo = "000000"
            }
            val uploadFileName: String = "${resp.getAgentTransId()}REF${refNo}"
            // store .erm file to secure memory
            EReceiptUtils.setExternalAppUploadRawData(this, uploadFileName, acquirer, refNo, eReceiptData)
            // upload externalapp content to ERM
            (it as ActionEReceiptInfoUpload).setParam(this, transData, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_UPLOAD_FROM_FILE, uploadFileName, acquirer)
        }
        val endListener = AAction.ActionEndListener { action, result ->
            if (printCountFlag) {
                val sendErcmUploadResult = DeliverErmUploadResultMsg.Companion.Request()
                sendErcmUploadResult.setERMResult(result.ret)
                sendErcmUploadResult.setOriginalAgentTransID(agentTransID!!)
                transAPI.startTrans(this, sendErcmUploadResult as BaseRequest)
                printCountFlag = false
            }
        }
        val eReceiptUploadTrans = ActionEReceiptInfoUpload(listener)
        eReceiptUploadTrans.setEndListener(endListener)
        eReceiptUploadTrans.execute()
        printCountFlag= true
    }
}