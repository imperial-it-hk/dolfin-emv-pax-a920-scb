package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.R
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

class KCheckIDUploadEReceiptActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_null)

        val listener = AAction.ActionStartListener{
            val transData = TransData()
            val acquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KCHECKID)
            val requestRefId : String? = intent.getStringExtra(EUIParamKeys.KCHECKID_ERCM_REFERENCE_ID.toString())
            val eReceiptData : ByteArray? = intent.getByteArrayExtra(EUIParamKeys.KCHECKID_ERCM_ERECEIPT_DATA.toString())
            val sessionId : String? = intent.getStringExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString())
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
            transData.refNo = requestRefId
            transData.authCode = "000000"
            transData.authCode = "000000"
            transData.acquirer = acquirer
            transData.reversalStatus = TransData.ReversalStatus.NORMAL
            transData.transType = ETransType.KCHECKID_DUMMY
            transData.transState = TransData.ETransStatus.NORMAL
            transData.expDate = "XXXX"

            EReceiptUtils.setExternalAppUploadRawData(this, sessionId, acquirer, requestRefId, eReceiptData)

            (it as ActionEReceiptInfoUpload).setParam(this, transData, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_UPLOAD_FROM_FILE, sessionId, acquirer)
        }
        val endListener = AAction.ActionEndListener { action, result ->
            action.setEndListener(null)
            finish(ActionResult(result.ret, null))
        }
        val eReceiptUploadTrans = ActionEReceiptInfoUpload(listener)
        eReceiptUploadTrans.setEndListener(endListener)
        eReceiptUploadTrans.execute()
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
}