package th.co.bkkps.amexapi.action.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.eemv.utils.Tools
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.MultiAppErmUploadModel
import com.pax.pay.utils.BitmapImageConverterUtils
import com.pax.pay.utils.Utils
import org.json.JSONObject
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexAPIConstants.REQUEST_PRINT
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.amexapi.AmexTransProcess
import th.co.bkkps.bps_amexapi.VoidMsg
import th.co.bkkps.utils.Log

class AmexVoidActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process
    private var state: Int = -1
    private var voucherNo: Long = -1
    private var lastTransNo: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra("AMEX_PRINT_VOUCHER_NO")) {
            state = AmexAPIConstants.REQUEST_PARAM_SETTING
            val intent = Intent(this@AmexVoidActivity, AmexParamsActivity::class.java)
            startActivityForResult(intent, state)
        } else {
            state = REQUEST_PRINT
            val ermVoucherNo = intent.getLongExtra("AMEX_PRINT_VOUCHER_NO", -1)

            doVoid(apiProcess, voucherNo, lastTransNo, ermVoucherNo)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        when (state) {
            REQUEST_PRINT-> {
                finish(ActionResult(TransResult.SUCC, null))
            }
            AmexAPIConstants.REQUEST_PARAM_SETTING -> {
                if (resultCode != TransResult.SUCC) {
                    finish(ActionResult(TransResult.ERR_PARAM, null))
                } else {
                    try {
                        state = AmexAPIConstants.REQUEST_VOID
                        apiProcess?.apply {
                            voucherNo = intent.getLongExtra("AMEX_API_TRANS_NO", 0)
                            lastTransNo = intent.getLongExtra("AMEX_API_LAST_TXN_NO", 0)
                            doVoid(this, voucherNo, lastTransNo,-999L)
                        } ?: run {
                            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                        }
                    }
                    catch (ex: Exception) {
                        finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                    }
                }
            }
            AmexAPIConstants.REQUEST_VOID -> {
                val response: VoidMsg.Response? = apiProcess.transAPI.onResult(requestCode, resultCode, data) as VoidMsg.Response?
                response?.let {
                    if (it.rspCode == TransResult.SUCC) {
                        Log.i(AmexAPIConstants.TAG, "VOID response received")
                        Log.d(AmexAPIConstants.TAG, "getStanNo=${it.stanNo}")
                        Log.d(AmexAPIConstants.TAG, "getVoucherNo=${it.voucherNo}")
                        Log.d(AmexAPIConstants.TAG, "getRefNo=${it.refNo}")
                        Log.d(AmexAPIConstants.TAG, "ermJsonInfo=${it.ermInfoJson}")
                        val amexAcq : Acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
                        val ermInitHostIndex : String? = amexAcq?.let { Component.getPaddedString(amexAcq.id.toString(),3, '0') }?:run{ null }
                        val batchNoPadded = Component.getPaddedString(it.batchNo.toString(),6,'0')
                        val traceNoPadded = Component.getPaddedString(it.voucherNo.toString(),6,'0')
                        val stanNoPadded = Component.getPaddedString(it.stanNo.toString(),6,'0')
                        val TransID : String = "/${amexAcq.name}-${amexAcq.nii}_B${batchNoPadded}T${traceNoPadded}S${stanNoPadded}"

                        it.ermInfoJson?.let {
                            if (amexAcq.isEnableUploadERM) {
                                // begin upload ERM
                                val eReceiptRawData : ByteArray? = doPrepareErmUploadData(it, amexAcq, ermInitHostIndex!!, TransID)
                                eReceiptRawData?.let{
                                    //doUploadERM(response, TransID, amexAcq, ermInitHostIndex!!, it)

                                    val ermUploadModel = MultiAppErmUploadModel()
                                    ermUploadModel.acquirer = amexAcq!!
                                    ermUploadModel.batchNo = batchNoPadded!!
                                    ermUploadModel.traceNo = traceNoPadded!!
                                    ermUploadModel.stanNo = stanNoPadded!!
                                    ermUploadModel.transNumber = TransID
                                    ermUploadModel.saleResp = null
                                    ermUploadModel.voidResp = response
                                    ermUploadModel.eSlipData = it

                                    finish(ActionResult(TransResult.SUCC, response ,ermUploadModel))
                                }
                            } else {
                                finish(ActionResult(TransResult.SUCC, response))
                            }
                        }?:run{
                            finish(ActionResult(TransResult.SUCC, response))
                        }
                    } else {
                        finish(ActionResult(it.rspCode, it))
                    }
                } ?: run {
                    Log.e(AmexAPIConstants.TAG, "VOID response not received")
                    finish(ActionResult(TransResult.ERR_ABORTED, null))
                }
            }
            else -> {
                finish(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
            }
        }
    }

    private fun doVoid(apiProcess: AmexTransProcess, voucherNumber:Long, lastTransNo: Long, ermSuccessPrintTraceNo: Long) {
        apiProcess.apply {
            var transNo: Long = -1

            if (ermSuccessPrintTraceNo == -999L) {
                transNo = intent.getLongExtra("AMEX_API_TRANS_NO", 0)
                this.doVoid(this@AmexVoidActivity, transNo, lastTransNo)
            } else {
                this.doVoidResponseUploadEReceipt(this@AmexVoidActivity, voucherNumber, ermSuccessPrintTraceNo)
            }
        }
    }

    fun doPrepareErmUploadData(ermJsonString : String, amexAcq: Acquirer, ermInitHostIndex: String, transID: String) : ByteArray? {
        var ermESlipFormat : String? = null
        var ermSignature : ByteArray? = null

        val converter = ActionEReceiptInfoUpload(null)
        var vfFormatSignatureText : String? = null

        val jsonObj = JSONObject(ermJsonString)
        jsonObj.let{
//            ermESlipFormat = it.get("ERM_ERECEIPT_DATA") as String
            if (it.has("ERM_ERECEIPT_DATA")) {
                ermESlipFormat = (Utils.str2Bcd(it.get("ERM_ERECEIPT_DATA") as String) as ByteArray).toString(Charsets.UTF_8)
            }
            if (jsonObj.has("ERM_ESIGNATURE_DATA")) {
                ermSignature = Utils.str2Bcd(it.get("ERM_ESIGNATURE_DATA") as String)

                if (converter!=null) {
                    vfFormatSignatureText = converter.getSignatureWithVerifoneFormat(ermSignature)
                }
            }
        }

        if (ermESlipFormat!=null) {
            var tempSlipFormat : String = ermESlipFormat!!
            converter?.let{
                tempSlipFormat = converter.replaceErmHostIndex(tempSlipFormat)
                tempSlipFormat = tempSlipFormat?.replace("?AMEX-API?",ermInitHostIndex)

                if (vfFormatSignatureText!=null) {
                    tempSlipFormat = converter.replaceErmSignatureData(tempSlipFormat, vfFormatSignatureText!!)
                }
            }

            ermESlipFormat = tempSlipFormat

            val savPath = "/data/data/" + FinancialApplication.getApp().applicationContext.packageName + "/files/output_slip_data"
            val savFileName = "/slipinfo_amex.slp"
            BitmapImageConverterUtils.saveDataToFile(Tools.str2Bcd(ermESlipFormat), savPath, savFileName)

            return Tools.str2Bcd(ermESlipFormat); //ermESlipFormat?.toByteArray()
        }

        return null
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        action?.apply {
            if (this.isFinished) return
            this.isFinished = true
            this.setResult(result)
        }
        finish()
    }


//    var printCountFlag : Int = 0
//    fun doUploadERM(resp : VoidMsg.Response, uploadTransID: String, amexAcq: Acquirer, ermInitAcqIndex: String, eReceiptData: ByteArray) {
//        val voucherNumber = resp.voucherNo;
//        val startListener = AAction.ActionStartListener {
//            val transData = TransData()
//            Component.transInit(transData, amexAcq)
//
//            // transaction identification
//            transData.stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1).toLong()
//            transData.traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO, 1).toLong()
//            transData.batchNo = amexAcq.currBatchNo.toLong()
//            transData.acquirer = amexAcq
//            transData.issuer = FinancialApplication.getAcqManager().findIssuer(resp.issuerName)
//            transData.reversalStatus = TransData.ReversalStatus.NORMAL
//            transData.transState = TransData.ETransStatus.NORMAL
//            transData.transType = ETransType.VOID
//
//            // ereceipt data
//            transData.seteSlipFormat(eReceiptData)
//
//            // erm prerequisite info
//            transData.initAcquirerIndex = ermInitAcqIndex
//            transData.initAcquirerName = amexAcq.name
//            transData.initAcquirerNii = amexAcq.nii
//
//            // Cardholder data
//            transData.pan = resp.cardNo
//            transData.expDate = "0000"
//            transData.amount = resp.amount
//            transData.refNo = resp.refNo?.let{ it }?:run { Device.getTime(Constants.TIME_PATTERN_TRANS2)}
//            transData.authCode = resp.authCode
//
//            transData.signData = resp.cardholderSignature
//            transData.isPinVerifyMsg = false
//            transData.isTxnSmallAmt = false
//
//            val jsonTransInfo : ByteArray? = (GsonBuilder().create().toJson(transData, TransData::class.java)).toByteArray()
//            EReceiptUtils.setExternalAppUploadRawData(this, uploadTransID, amexAcq, transData.refNo, eReceiptData, jsonTransInfo)
//
//            (it as ActionEReceiptInfoUpload).setParam(this, transData, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_UPLOAD_FROM_FILE, uploadTransID, amexAcq)
//        }
//        val endListener = AAction.ActionEndListener { action, result ->
//            var uploadRespTraceNo : Long = if (result.ret== TransResult.SUCC) voucherNumber else -1
//            doVoid(apiProcess, voucherNumber, uploadRespTraceNo)
//            printCountFlag += 1
//
//            setResult(result.ret)
//        }
//
//        val eReceiptUploadProcess = ActionEReceiptInfoUpload(startListener)
//        eReceiptUploadProcess.setEndListener(endListener)
//        eReceiptUploadProcess.execute()
//    }


}