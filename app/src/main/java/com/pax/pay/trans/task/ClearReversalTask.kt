package com.pax.pay.trans.task

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.trans.action.ActionClearReversal
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.ActionAmexClearReversal
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinClearReversal
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbClearReversal

class ClearReversalTask(context: Context?, private val selAcq: String?, transListener: TransEndListener?) : BaseTask(context, transListener) {

    private var isAllAcquirer: Boolean = false

    init {
        isAllAcquirer = selAcq.equals(getString(R.string.acq_all_acquirer))
    }

    override fun bindStateOnAction() {
        val actionClearReversal = ActionClearReversal(AAction.ActionStartListener {
            (it as ActionClearReversal).setParam(currentContext, selAcq!!)
        })
        bind(State.CLEAR_MAIN.toString(), actionClearReversal)

        val actionDolfinClearReversal = ActionDolfinClearReversal { /* Do nothing */ }
        bind(State.CLEAR_DOLFIN.toString(), actionDolfinClearReversal)

        val actionScbClearReversal = ActionScbClearReversal(AAction.ActionStartListener {
            (it as ActionScbClearReversal).setParam(currentContext, selAcq)
        })
        bind(State.CLEAR_SCB.toString(), actionScbClearReversal)

        val actionAmexClearReversal = ActionAmexClearReversal(AAction.ActionStartListener {
            (it as ActionAmexClearReversal).setParam(currentContext, com.pax.pay.constant.Constants.ACQ_AMEX)
        })
        bind(State.CLEAR_AMEX.toString(), actionAmexClearReversal)

        if (isAllAcquirer) {
            gotoState(State.CLEAR_MAIN.toString())
        } else {
            when (selAcq) {
                com.pax.pay.constant.Constants.ACQ_DOLFIN -> {
                    gotoState(State.CLEAR_DOLFIN.toString())
                }
                com.pax.pay.constant.Constants.ACQ_SCB_IPP,
                com.pax.pay.constant.Constants.ACQ_SCB_REDEEM -> {
                    gotoState(State.CLEAR_SCB.toString())
                }
                com.pax.pay.constant.Constants.ACQ_AMEX -> {
                    gotoState(State.CLEAR_AMEX.toString())
                }
                else -> {
                    gotoState(State.CLEAR_MAIN.toString())
                }
            }
        }
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.CLEAR_MAIN -> {
                if (isAllAcquirer) {
                    gotoState(State.CLEAR_DOLFIN.toString())
                } else {
                    transEnd(result)
                }
            }
            State.CLEAR_DOLFIN -> {
                if (isAllAcquirer) {
                    when {
                        ScbIppService.isSCBInstalled(currentContext) -> {
                            gotoState(State.CLEAR_SCB.toString())
                        }
                        AmexTransService.isAmexAppInstalled(currentContext) -> {
                            gotoState(State.CLEAR_AMEX.toString())
                        }
                        else -> {
                            transEnd(result)
                        }
                    }
                } else {
                    transEnd(result)
                }
            }
            State.CLEAR_SCB -> {
                if (isAllAcquirer) {
                    when {
                        AmexTransService.isAmexAppInstalled(currentContext) -> {
                            gotoState(State.CLEAR_AMEX.toString())
                        }
                        else -> {
                            transEnd(result)
                        }
                    }
                } else {
                    transEnd(result)
                }
            }
            State.CLEAR_AMEX -> {
                transEnd(result)
            }
        }
    }

    enum class State {
        CLEAR_MAIN,
        CLEAR_DOLFIN,
        CLEAR_SCB,
        CLEAR_AMEX,
    }
}