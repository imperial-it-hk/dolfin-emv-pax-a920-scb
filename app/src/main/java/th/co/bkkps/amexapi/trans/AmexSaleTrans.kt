package th.co.bkkps.amexapi.trans

import android.content.Context
import com.google.gson.GsonBuilder
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.emv.EmvSP200
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.MultiAppErmUploadModel
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.Convert
import com.pax.pay.utils.EReceiptUtils
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.ActionAmexSale
import th.co.bkkps.bps_amexapi.TransResponse

class AmexSaleTrans(val context: Context, val transListener: TransEndListener?, val origTransData: TransData?, private val emvSP200: EmvSP200?, ecrProcListener: AAction.ActionEndListener?) :
    BaseTrans(context, ETransType.SALE, transListener) {

    var ermUploadModel : MultiAppErmUploadModel? = null
    var saleActionResult : ActionResult? =null
    var printVoucherNo : Long = -1

    init {
        setBackToMain(true)
        setECRProcReturnListener(ecrProcListener)

    }

    override fun bindStateOnAction() {
        val actionAmexSale = ActionAmexSale(AAction.ActionStartListener {
            (it as ActionAmexSale).setParam(currentContext, origTransData!!.amount,
                origTransData!!.tipAmount, getPaymentEnterMode(), origTransData!!.track1,
                origTransData!!.track2, origTransData!!.track3, origTransData!!.pan, origTransData!!.expDate, convertEmvSP200())
        })
        bind(State.SALE.toString(), actionAmexSale, false)

        val actionAmexSalePrint = ActionAmexSale(AAction.ActionStartListener {
            (it as ActionAmexSale).setParam(currentContext, origTransData!!.amount,
                origTransData!!.tipAmount, getPaymentEnterMode(), origTransData!!.track1,
                origTransData!!.track2, origTransData!!.track3, origTransData!!.pan, origTransData!!.expDate, convertEmvSP200(), printVoucherNo)
        })
        bind(State.PRINT.toString(), actionAmexSalePrint, false)

        val actionErmUpload = ActionEReceiptInfoUpload(AAction.ActionStartListener { action->
            ermUploadModel?.let {
                val transData = TransData()
                Component.transInit(transData, it.acquirer!!)

                // transaction identification
                transData.stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1).toLong()
                transData.traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO, 1).toLong()
                transData.batchNo = it.acquirer!!.currBatchNo.toLong()
                transData.acquirer = it.acquirer!!
                transData.issuer = FinancialApplication.getAcqManager().findIssuer(it.saleResp!!.issuerName)
                transData.reversalStatus = TransData.ReversalStatus.NORMAL
                transData.transState = TransData.ETransStatus.NORMAL
                transData.transType = ETransType.SALE

                // ereceipt data
                transData.seteSlipFormat(it.eSlipData!!)

                // erm prerequisite info
                transData.initAcquirerIndex = Utils.getStringPadding(it.acquirer!!.id.toString(), 3,"0", Convert.EPaddingPosition.PADDING_LEFT)
                transData.initAcquirerName = it.acquirer!!.name
                transData.initAcquirerNii = it.acquirer!!.nii

                // Cardholder data
                transData.pan = it.saleResp!!.cardNo
                transData.expDate = "0000"
                transData.amount = it.saleResp!!.amount
                transData.refNo = it.saleResp!!.refNo?.let{ it }?:run { Device.getTime(Constants.TIME_PATTERN_TRANS2)}
                transData.authCode = it.saleResp!!.authCode

                transData.signData = it.saleResp!!.cardholderSignature
                transData.isPinVerifyMsg = false
                transData.isTxnSmallAmt = false

                val jsonTransInfo : ByteArray? = (GsonBuilder().create().toJson(transData, TransData::class.java)).toByteArray()
                EReceiptUtils.setExternalAppUploadRawData(currentContext, it.transNumber!!, it.acquirer!!, transData.refNo, it.eSlipData!!, jsonTransInfo)

                (action as ActionEReceiptInfoUpload).setParam(currentContext, transData, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_UPLOAD_FROM_FILE, it.transNumber!!, it.acquirer!!)
            }?:run{
                transEnd(ActionResult(TransResult.ERCM_UPLOAD_FAIL, null))
            }
        })
        bind(State.UPLOAD_ERECEIPT.toString(), actionErmUpload, false)


        gotoState(State.SALE.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        try {
            super.setSilentMode(true)
            when (State.valueOf(currentState!!)) {
                State.SALE -> {
                    result?.let { res->
                        saleActionResult = result
                        if (res.ret==TransResult.SUCC) {
                            res.data?.let{
                                if (it is TransResponse) {
                                    AmexTransService.insertTransData(transData, it as TransResponse)
                                    ECRProcReturn(null, ActionResult(TransResult.SUCC, null))
                                } else {
                                    transEnd(ActionResult(TransResult.ERR_PARAM,null))
                                    return
                                }
                            }

                            res.data1?.let {
                                if (it is MultiAppErmUploadModel) {
                                    ermUploadModel = it as MultiAppErmUploadModel
                                    gotoState(State.UPLOAD_ERECEIPT.toString())
                                    return
                                }
                            }
                            transEnd(ActionResult(TransResult.SUCC,null))
                        } else {
                            transEnd(ActionResult(TransResult.ERR_ABORTED, null))//skip alert dialog
                        }
                    }?:run{
                        transEnd(ActionResult(TransResult.ERR_ABORTED,null))
                    }
                }

                State.UPLOAD_ERECEIPT-> {
                    result?.let{
                        if (it.ret==TransResult.SUCC) {
                            printVoucherNo = ermUploadModel!!.traceNo!!.toLong()
                        } else {
                            printVoucherNo = -1
                        }
                        gotoState(State.PRINT.toString())

                        //transEnd(saleActionResult)
                    }?:run{
                        transEnd(ActionResult(TransResult.ERCM_UPLOAD_FAIL,null))
                    }
                }

                State.PRINT->{
//                    if (result?.ret == TransResult.ERCM_UPLOAD_FAIL) {
//                        return
//                    }
                    transEnd(saleActionResult)
                }
            }
        }
        catch (ex: Exception) {
            transEnd(result)
        }
    }

    private fun getPaymentEnterMode(): Int {
        origTransData?.let{
            return when (it.enterMode) {
                TransData.EnterMode.SWIPE -> TransResponse.MAG
                TransData.EnterMode.FALLBACK -> TransResponse.FALLBACK
                TransData.EnterMode.MANUAL -> TransResponse.MANUAL
                TransData.EnterMode.INSERT -> TransResponse.ICC
                TransData.EnterMode.CLSS -> TransResponse.PICC
                TransData.EnterMode.SP200 -> TransResponse.SP200
                else -> 0
            }
        }?:run{
            return 0
        }
    }

    private fun convertEmvSP200(): th.co.bkkps.bps_amexapi.EmvSP200 {
        val data = th.co.bkkps.bps_amexapi.EmvSP200()
        emvSP200?.let {
            data.apply {
                this.aid = it.aid
                this.trackData = it.trackData
                this.panSeqNo = it.panSeqNo
                this.pan = it.pan
                this.expDate = it.expDate
                this.iccData = it.iccData
                this.appLabel = it.appLabel
                this.appPreferName = it.appPreferName
                this.tvr = it.tvr
                this.appCrypto = it.appCrypto
                this.holderName = it.holderName
                this.emvPan = it.emvPan
                this.isSignFree = it.isSignFree
                this.isPinFree = it.isPinFree
            }
        }
        return data
    }

    internal enum class State {
        SALE,
        UPLOAD_ERECEIPT,
        PRINT
    }
}