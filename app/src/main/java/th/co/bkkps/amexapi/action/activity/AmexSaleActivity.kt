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
import th.co.bkkps.bps_amexapi.EmvSP200
import th.co.bkkps.bps_amexapi.SaleMsg
import th.co.bkkps.utils.Log

class AmexSaleActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process
    private var state: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.hasExtra("AMEX_PRINT_VOUCHER_NO")) {
            state = AmexAPIConstants.REQUEST_PARAM_SETTING
            val intent = Intent(this@AmexSaleActivity, AmexParamsActivity::class.java)
            startActivityForResult(intent, state)
        } else {
            state = REQUEST_PRINT
            val voucherNo = intent.getLongExtra("AMEX_PRINT_VOUCHER_NO", -1);
            doSaleTrans(apiProcess, voucherNo)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        when (state) {
            REQUEST_PRINT -> {
                finish(ActionResult(TransResult.SUCC, null))
            }
            AmexAPIConstants.REQUEST_PARAM_SETTING -> {
                if (resultCode != TransResult.SUCC) {
                    finish(ActionResult(TransResult.ERR_PARAM, null))
                } else {
                    try {
                        state = AmexAPIConstants.REQUEST_SALE
                        apiProcess?.apply {
                            doSaleTrans(this, -999L)
                        } ?: run {
                            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                        }
                    }
                    catch (ex: Exception) {
                        finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                    }
                }
            }
            AmexAPIConstants.REQUEST_SALE -> {
                val response: SaleMsg.Response? = apiProcess.transAPI.onResult(requestCode, resultCode, data) as SaleMsg.Response?
                response?.let {
                    if (it.rspCode == TransResult.SUCC) {
                        Log.i(AmexAPIConstants.TAG, "SALE response received")
                        Log.d(AmexAPIConstants.TAG, "getStanNo=${it.stanNo}")
                        Log.d(AmexAPIConstants.TAG, "getVoucherNo=${it.voucherNo}")
                        Log.d(AmexAPIConstants.TAG, "getRefNo=${it.refNo}")
                        Log.d(AmexAPIConstants.TAG, "ermJsonInfo=${it.ermInfoJson}")
                        val amexAcq : Acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
                        val ermInitHostIndex : String? = amexAcq?.let { Component.getPaddedString(amexAcq.id.toString(),3, '0') }?:run{ null }
                        val batchNoPadded = Component.getPaddedString(it.batchNo.toString(),6,'0')
                        val traceNoPadded = Component.getPaddedString(it.voucherNo.toString(),6,'0')
                        val stanNoPadded = Component.getPaddedString(it.stanNo.toString(),6,'0')
                        val TransID : String = "${amexAcq.name}-${amexAcq.nii}_B${batchNoPadded}T${traceNoPadded}S${stanNoPadded}"

                        it.ermInfoJson?.let {
                            if (amexAcq.isEnableUploadERM) {
                                val eReceiptRawData: ByteArray? = doPrepareErmUploadData(it, amexAcq, ermInitHostIndex!!, TransID)
                                eReceiptRawData?.let {
                                    //doUploadERM(response, TransID, amexAcq, ermInitHostIndex!!, it)

                                    var ermUploadModel = MultiAppErmUploadModel()
                                    ermUploadModel.acquirer = amexAcq!!
                                    ermUploadModel.batchNo = batchNoPadded!!
                                    ermUploadModel.traceNo = traceNoPadded!!
                                    ermUploadModel.stanNo = stanNoPadded!!
                                    ermUploadModel.transNumber = TransID
                                    ermUploadModel.saleResp = response
                                    ermUploadModel.voidResp = null
                                    ermUploadModel.eSlipData = it

                                    finish(ActionResult(TransResult.SUCC, response, ermUploadModel))
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
                    Log.e(AmexAPIConstants.TAG, "SALE response not received")
                    finish(ActionResult(TransResult.ERR_ABORTED, null))
                }
            }
            else -> {
                //do nothing
            }
        }
    }

    fun doSaleTrans(apiProcess: AmexTransProcess, ermSuccessPrintVoucherNo: Long) {
        apiProcess?.apply {
            val amount: String? = intent.getStringExtra("AMEX_API_AMOUNT")
            val tipAmount: String? = intent.getStringExtra("AMEX_API_TIP_AMOUNT")
            val enterMode: Int = intent.getIntExtra("AMEX_API_ENTER_MODE", 0)
            val track1: String? = intent.getStringExtra("AMEX_API_TRACK1")
            val track2: String? = intent.getStringExtra("AMEX_API_TRACK2")
            val track3: String? = intent.getStringExtra("AMEX_API_TRACK3")
            val pan: String? = intent.getStringExtra("AMEX_API_PAN")
            val expDate: String? = intent.getStringExtra("AMEX_API_EXP_DATE")
            val emvSP200: EmvSP200? = intent.getParcelableExtra("AMEX_API_EMV_SP200")

            if (ermSuccessPrintVoucherNo == -999L) {
                this.doSale(this@AmexSaleActivity, amount?.toLong(10) ?: 0,
                    tipAmount?.toLong(10) ?: 0, enterMode, track1, track2, track3, pan, expDate, emvSP200)
            } else {
                this.doSaleResponseUploadEReceipt(this@AmexSaleActivity, 0,
                    0, enterMode, null, null, null, null, null, null, ermSuccessPrintVoucherNo )
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
                tempSlipFormat = converter.replaceTagBcd(tempSlipFormat,"?AMEX-API?", ermInitHostIndex)
                //tempSlipFormat = tempSlipFormat?.replace("?AMEX-API?",ermInitHostIndex)

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
//    fun doUploadERM(resp : SaleMsg.Response, uploadTransID: String, amexAcq: Acquirer, ermInitAcqIndex: String, eReceiptData: ByteArray) {
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
//            transData.transType = ETransType.SALE
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
//            action.setEndListener(null)
//
//            var uploadRespTraceNo : Long = if (result.ret== TransResult.SUCC) voucherNumber else -1
//            doSaleTrans(apiProcess, uploadRespTraceNo)
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