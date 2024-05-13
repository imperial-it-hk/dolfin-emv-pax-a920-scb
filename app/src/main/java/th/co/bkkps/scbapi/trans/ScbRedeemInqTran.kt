package th.co.bkkps.scbapi.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.model.ETransType
import th.co.bkkps.bpsapi.BaseResponse
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbRedeemInq
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam

class ScbRedeemInqTran(
    context: Context?,
    transListener: TransEndListener?
) : BaseTrans(context, ETransType.SALE, transListener) {

    init {
        setBackToMain(true)
    }

    override fun bindStateOnAction() {
        val actionScbUpdateParam = ActionScbUpdateParam { action: AAction ->
            (action as ActionScbUpdateParam).setParam(currentContext)
        }
        bind(State.SENT_CONFIG.toString(), actionScbUpdateParam)

        val actionScbRedeemInq = ActionScbRedeemInq(AAction.ActionStartListener {
            (it as ActionScbRedeemInq).setParam(currentContext)
        })
        bind(State.INQUIRY.toString(), actionScbRedeemInq, false)

        if (!ScbIppService.isSCBInstalled(currentContext)) {
            transEnd(ActionResult(TransResult.ERR_SCB_CONNECTION, null))
            return
        }
        gotoState(State.SENT_CONFIG.toString())
    }

    internal enum class State {
        SENT_CONFIG, INQUIRY
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.SENT_CONFIG -> {
                if (result!!.ret == TransResult.SUCC) {
                    gotoState(State.INQUIRY.toString())
                    return
                }
                transEnd(result)
            }
            State.INQUIRY -> {
                ScbIppService.updateEdcTraceStan(result!!.data as BaseResponse)
                transEnd(ActionResult(TransResult.ERR_ABORTED, null)) //no alert dialog
            }
        }
    }
}