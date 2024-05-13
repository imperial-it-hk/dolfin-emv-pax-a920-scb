package com.pax.pay.trans.task

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.ActionClearTradeVoucher
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.Controller
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.ActionAmexClearTradeVoucher
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinClearTransaction
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbClearTradeVoucher

class ClearTradeVoucherTask(context: Context?, private val selAcq: String?, transListener: TransEndListener?) : BaseTask(context, transListener) {

    private var isAllAcquirer: Boolean = false

    init {
        isAllAcquirer = selAcq.equals(getString(R.string.acq_all_acquirer))
    }

    override fun bindStateOnAction() {
        val actionClearTradeVoucher = ActionClearTradeVoucher(AAction.ActionStartListener {
            (it as ActionClearTradeVoucher).setParam(currentContext, selAcq!!)
        })
        bind(State.CLEAR_MAIN.toString(), actionClearTradeVoucher)

        val actionDolfinClearTransaction = ActionDolfinClearTransaction { /* Do nothing */ }
        bind(State.CLEAR_DOLFIN.toString(), actionDolfinClearTransaction)

        val actionScbClearTradeVoucher = ActionScbClearTradeVoucher(AAction.ActionStartListener {
            (it as ActionScbClearTradeVoucher).setParam(currentContext, selAcq)
        })
        bind(State.CLEAR_SCB.toString(), actionScbClearTradeVoucher)

        val actionAmexClearTradeVoucher = ActionAmexClearTradeVoucher(AAction.ActionStartListener {
            (it as ActionAmexClearTradeVoucher).setParam(currentContext, Constants.ACQ_AMEX)
        })
        bind(State.CLEAR_AMEX.toString(), actionAmexClearTradeVoucher)

        if (isAllAcquirer) {
            gotoState(State.CLEAR_MAIN.toString())
        } else {
            when (selAcq) {
                Constants.ACQ_DOLFIN -> {
                    gotoState(State.CLEAR_DOLFIN.toString())
                }
                Constants.ACQ_SCB_IPP,
                Constants.ACQ_SCB_REDEEM -> {
                    gotoState(State.CLEAR_SCB.toString())
                }
                Constants.ACQ_AMEX -> {
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
                Component.setSettleStatus(Controller.Constant.WORKED, Constants.ACQ_DOLFIN)
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
                Component.setSettleStatus(Controller.Constant.WORKED, Constants.ACQ_AMEX)
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