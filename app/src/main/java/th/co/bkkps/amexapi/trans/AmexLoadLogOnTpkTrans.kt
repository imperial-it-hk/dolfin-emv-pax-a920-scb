package th.co.bkkps.amexapi.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.BaseTrans
import com.pax.pay.trans.model.ETransType
import th.co.bkkps.amexapi.action.ActionAmexLoadLogOnTpk

class AmexLoadLogOnTpkTrans(context: Context?, transListener: TransEndListener?) : BaseTrans(context, ETransType.LOAD_UPI_TMK, transListener) {
    override fun bindStateOnAction() {
        val actionAmexLoadLogOnTpk = ActionAmexLoadLogOnTpk(AAction.ActionStartListener {
            (it as ActionAmexLoadLogOnTpk).setParam(context)
        })
        bind(State.LOAD_LOGON_TPK.toString(), actionAmexLoadLogOnTpk, false)

        gotoState(State.LOAD_LOGON_TPK.toString())
    }

    enum class State {
        LOAD_LOGON_TPK
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.LOAD_LOGON_TPK -> {
                transEnd(ActionResult(TransResult.ERR_ABORTED, null))//no dialog
            }
        }
    }
}