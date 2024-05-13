package th.co.bkkps.kcheckidAPI.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.task.BaseTask
import th.co.bkkps.kcheckidAPI.trans.action.ActionGetKCheckIDRecordCount

class KCheckIDSettlementTrans(val context: Context, val state: State, val listener: TransEndListener) : BaseTask(context, listener) {

    enum class State {
        GET_RECORD_COUNT,
        SETTLEMENT
    }

    override fun bindStateOnAction() {
        val  actionGetRecordCount = ActionGetKCheckIDRecordCount (AAction.ActionStartListener {
            val actionGetRecordCount = ActionGetKCheckIDRecordCount(AAction.ActionStartListener {
                (it as ActionGetKCheckIDRecordCount).setParam(FinancialApplication.getApp().applicationContext)
            })
            actionGetRecordCount.execute()
        })
        bind(State.GET_RECORD_COUNT.toString(), actionGetRecordCount)

       gotoState(state.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (currentState) {
            State.GET_RECORD_COUNT.toString() -> {
                transEnd(result)
            }
            State.SETTLEMENT.toString() -> {
                transEnd(result)
            }
        }
    }
}