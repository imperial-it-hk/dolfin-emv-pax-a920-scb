package th.co.bkkps.scbapi.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.settings.SysParam
import th.co.bkkps.bpsapi.TransResponse
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam
import th.co.bkkps.scbapi.trans.action.ActionScbVoidApi

class ScbVoidTran(
    context: Context?,
    transListener: TransEndListener?,
    var origTransData: TransData?
) : BaseTrans(context, ETransType.VOID, transListener) {

    init {
        setBackToMain(true)
    }

    override fun bindStateOnAction() {
        val actionScbUpdateParam = ActionScbUpdateParam { action: AAction ->
            (action as ActionScbUpdateParam).setParam(currentContext)
        }
        bind(State.SENT_CONFIG.toString(), actionScbUpdateParam)

        val actionScbVoidApi = ActionScbVoidApi(AAction.ActionStartListener {
            var voucherNo: Long = 0
            origTransData?.let { orig ->
                voucherNo = orig.traceNo
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND, false)) {
                    voucherNo = orig.stanNo
                }
            }
            (it as ActionScbVoidApi).setParam(currentContext, voucherNo)
        })
        bind(State.VOID.toString(), actionScbVoidApi, false)

        if (!ScbIppService.isSCBInstalled(currentContext)) {
            transEnd(ActionResult(TransResult.ERR_SCB_CONNECTION, null))
            return
        }

        gotoState(State.SENT_CONFIG.toString())
    }

    internal enum class State {
        SENT_CONFIG, VOID
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.SENT_CONFIG -> {
                if (result!!.ret == TransResult.SUCC) {
                    gotoState(State.VOID.toString())
                    return
                }
                transEnd(result)
            }
            State.VOID -> {
                result?.let { res ->
                    if (res.ret != TransResult.SUCC) {
                        val response = res.data as TransResponse
                        ScbIppService.updateEdcTraceStan(response)
                    } else {
                        res.data?.let { data ->
                            val response = data as TransResponse

                            if (origTransData == null) {
                                origTransData = FinancialApplication.getTransDataDbHelper()
                                    .findTransDataByTraceNo(response.voucherNo, false)
                            }

                            origTransData?.apply {
                                transData.origBatchNo = this.batchNo
                                transData.origAuthCode = this.authCode
                                transData.origRefNo = this.refNo
                                transData.origTransNo = this.traceNo
                                transData.origTransType = this.transType
                                transData.origDateTime = this.dateTime

                                if (Constants.ACQ_SCB_REDEEM == this.acquirer.name) {
                                    transData.redeemedAmount = this.redeemedAmount
                                    transData.redeemPoints = this.redeemPoints
                                    transData.productQty = this.productQty
                                    transData.productCode = this.productCode
                                }

                                ScbIppService.insertTransData(transData, response)

                                this.voidStanNo = transData.stanNo
                                this.dateTime = transData.dateTime
                                this.transState = TransData.ETransStatus.VOIDED
                                this.authCode = if (transData.authCode != null) transData.authCode else transData.origAuthCode
                                FinancialApplication.getTransDataDbHelper().updateTransData(this)
                            } ?: run {
                                transEnd(
                                    ActionResult(
                                        TransResult.ERR_ABORTED,
                                        null
                                    )
                                ) //no alert dialog
                                return
                            }
                        }
                    }
                }
                transEnd(ActionResult(TransResult.ERR_ABORTED, null)) //no alert dialog
            }
        }
    }
}