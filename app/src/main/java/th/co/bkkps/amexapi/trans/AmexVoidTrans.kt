package th.co.bkkps.amexapi.trans

import android.content.Context
import android.content.DialogInterface
import com.google.gson.GsonBuilder
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
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
import th.co.bkkps.amexapi.action.ActionAmexVoid
import th.co.bkkps.bps_amexapi.TransResponse

class AmexVoidTrans(val context: Context, val transListener: TransEndListener, var origTransNo: Long, val lastTransNo: Long, ecrProcListener: AAction.ActionEndListener?) :
    BaseTrans(context, ETransType.VOID, transListener) {

    var ermUploadModel : MultiAppErmUploadModel? = null
    var voidActionResult : ActionResult? =null
    var printVoucherNo : Long = -1

    init {
        setBackToMain(true)
        setECRProcReturnListener(ecrProcListener)
    }

    override fun bindStateOnAction() {
        val actionAmexVoid = ActionAmexVoid(AAction.ActionStartListener {
            (it as ActionAmexVoid).setParam(currentContext, origTransNo, lastTransNo)
        })
        bind(State.VOID.toString(), actionAmexVoid, false)

        val actionPrintVoidReceipt = ActionAmexVoid(AAction.ActionStartListener {
            (it as ActionAmexVoid).setParam(currentContext, origTransNo, lastTransNo, printVoucherNo)
        })
        bind(State.PRINT.toString(), actionPrintVoidReceipt, false)

        val actionErmUpload = ActionEReceiptInfoUpload(AAction.ActionStartListener { action->
            ermUploadModel?.let {
                val transData = TransData()
                Component.transInit(transData, it.acquirer!!)

                // transaction identification
                transData.stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1).toLong()
                transData.traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO, 1).toLong()
                transData.batchNo = it.acquirer!!.currBatchNo.toLong()
                transData.acquirer = it.acquirer!!
                transData.issuer = FinancialApplication.getAcqManager().findIssuer(it.voidResp!!.issuerName)
                transData.reversalStatus = TransData.ReversalStatus.NORMAL
                transData.transState = TransData.ETransStatus.NORMAL
                transData.transType = this.transType

                // ereceipt data
                transData.seteSlipFormat(it.eSlipData!!)

                // erm prerequisite info
                transData.initAcquirerIndex = Utils.getStringPadding(it.acquirer!!.id.toString(), 3,"0", Convert.EPaddingPosition.PADDING_LEFT)
                transData.initAcquirerName = it.acquirer!!.name
                transData.initAcquirerNii = it.acquirer!!.nii

                // Cardholder data
                transData.pan = it.voidResp!!.cardNo
                transData.expDate = "0000"
                transData.amount = it.voidResp!!.amount
                transData.refNo = it.voidResp!!.refNo?.let{ it }?:run { Device.getTime(Constants.TIME_PATTERN_TRANS2)}
                transData.authCode = it.voidResp!!.authCode

                transData.signData = it.voidResp!!.cardholderSignature
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

        gotoState(State.VOID.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        try {
            super.setSilentMode(true)
            when (State.valueOf(currentState!!)) {
                State.VOID -> {
                    result?.let { res->
                        voidActionResult = res
                        if (res.ret == TransResult.SUCC) {
                            res.data?.let {
                                if (it is TransResponse) {
                                    val response = it as TransResponse?
                                    AmexTransService.insertTransData(transData, response)// increase trace/stan in this process
                                    ECRProcReturn(null, ActionResult(TransResult.SUCC, null))
                                } else {
                                    transEnd(ActionResult(TransResult.ERR_PARAM,null))
                                    return
                                }
                            }

                            res.data1?.let{
                                if (it is MultiAppErmUploadModel) {
                                    ermUploadModel = it as MultiAppErmUploadModel
                                    gotoState(AmexSaleTrans.State.UPLOAD_ERECEIPT.toString())
                                    return
                                }
                            }

                            transEnd(ActionResult(TransResult.SUCC,null))
                        } else {
                            res.data?.let {
                                val response = it as TransResponse
                                AmexTransService.updateEdcTraceStan(response)
                            }
                            transEnd(res)
                        }
                    }?:run{
                        transEnd(ActionResult(TransResult.ERR_ABORTED,null))//no dialog
                    }
                }

                State.UPLOAD_ERECEIPT -> {
                    result?.let{
                        if (it.ret==TransResult.SUCC) {
                            printVoucherNo = ermUploadModel!!.traceNo!!.toLong()
                        } else {
                            printVoucherNo = -1
                        }
                        gotoState(State.PRINT.toString())
                    }?:run{
                        transEnd(ActionResult(TransResult.ERCM_UPLOAD_FAIL,null))
                    }
                }

                State.PRINT->{
                    transEnd(voidActionResult)
                }
            }
        }
        catch (ex: Exception) {
            transEnd(result)
        }
    }

    internal enum class State {
        VOID,
        UPLOAD_ERECEIPT,
        PRINT
    }

    override fun dispResult(transName: String?, result: ActionResult?, dismissListener: DialogInterface.OnDismissListener?) {
        dismissListener?.apply {
            this.onDismiss(null)
        }
    }
}