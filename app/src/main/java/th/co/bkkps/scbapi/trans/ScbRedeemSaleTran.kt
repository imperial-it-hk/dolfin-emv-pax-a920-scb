package th.co.bkkps.scbapi.trans

import android.content.Context
import androidx.annotation.IntDef
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.model.ETransType
import th.co.bkkps.bpsapi.TransResponse
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbRedeemSale
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam

class ScbRedeemSaleTran(
    context: Context?,
    transListener: TransEndListener?,
    @ScbRedeemMode val mode: Int
) : BaseTrans(context, ETransType.SALE, transListener) {

    companion object {
        @IntDef(INQUIRY, VOUCHER, POINT_SALE, DISCOUNT_SALE, SPECIFIC_PRODUCT)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ScbRedeemMode

        const val INQUIRY: Int = 0
        const val VOUCHER: Int = 1
        const val POINT_SALE: Int = 2
        const val DISCOUNT_SALE: Int = 3
        const val SPECIFIC_PRODUCT: Int = 4
    }

    init {
        setBackToMain(true)
    }

    override fun bindStateOnAction() {
        val actionScbUpdateParam = ActionScbUpdateParam { action: AAction ->
            (action as ActionScbUpdateParam).setParam(currentContext)
        }
        bind(State.SENT_CONFIG.toString(), actionScbUpdateParam)

        val actionScbRedeemSale = ActionScbRedeemSale(AAction.ActionStartListener {
            (it as ActionScbRedeemSale).setParam(currentContext, mode)
        })
        bind(State.SALE.toString(), actionScbRedeemSale, false)

        if (!ScbIppService.isSCBInstalled(currentContext)) {
            transEnd(ActionResult(TransResult.ERR_SCB_CONNECTION, null))
            return
        }
        gotoState(State.SENT_CONFIG.toString())
    }

    internal enum class State {
        SENT_CONFIG, SALE
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.SENT_CONFIG -> {
                if (result!!.ret == TransResult.SUCC) {
                    gotoState(State.SALE.toString())
                    return
                }
                transEnd(result)
            }
            State.SALE -> {
                ScbIppService.insertRedeemTransData(transData, result!!.data as TransResponse)
                transEnd(ActionResult(TransResult.ERR_ABORTED, null)) //no alert dialog
            }
        }
    }

}