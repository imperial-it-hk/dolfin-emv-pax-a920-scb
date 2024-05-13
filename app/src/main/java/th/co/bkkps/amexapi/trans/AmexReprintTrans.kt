package th.co.bkkps.amexapi.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.settings.SysParam
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.ActionAmexReprint
import th.co.bkkps.bps_amexapi.ReprintTransMsg
import th.co.bkkps.bps_amexapi.TransResponse

class AmexReprintTrans(val context: Context, val transListener: TransEndListener, val origTransNo: Long, val lastTransNo: Long, val reprintType: Int) :
    BaseTrans(context, ETransType.BPS_REPRINT, transListener) {

    init {
        setBackToMain(true)
    }

    override fun bindStateOnAction() {
        val actionAmexReprint = ActionAmexReprint(AAction.ActionStartListener {
            (it as ActionAmexReprint).setParam(currentContext, origTransNo, lastTransNo, reprintType)
        })
        bind(State.REPRINT.toString(), actionAmexReprint, false)

        gotoState(State.REPRINT.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        try {
            when (State.valueOf(currentState!!)) {
                State.REPRINT -> {
                    result?.let { res ->
                        val response = res.data as ReprintTransMsg.Response
                        setSilentMode(true);
                        transEnd(ActionResult(TransResult.SUCC, response.rspCode))
                    }
                }
            }
        }
        catch (ex: Exception) {
            transEnd(result)
        }
    }

    internal enum class State {
        REPRINT
    }
}