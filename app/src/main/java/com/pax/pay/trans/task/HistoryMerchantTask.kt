package com.pax.pay.trans.task


import android.content.Context
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.base.MerchantProfile
import com.pax.pay.trans.action.ActionPreviewHistory
import com.pax.pay.trans.action.ActionPrintMerchantReport
import com.pax.pay.trans.action.ActionSelectMerchant
import com.pax.pay.trans.action.ActionSelectReportType
import com.pax.pay.trans.model.MerchantProfileManager


class HistoryMerchantTask(context: Context?, transListener: TransEndListener?) :
    BaseTask(context, transListener) {

    companion object {
    }

    internal enum class State {
        SELECT_MERCHANT, PREVIEW_HISTORY, SELECT_REPORT_TYPE, PRINT_REPORT
    }

    private var reportType: String = ""

    override fun bindStateOnAction() {

        val actionSelectMerchant =
            ActionSelectMerchant { action ->
                (action as ActionSelectMerchant).setParam(
                    currentContext
                    ,true
                )
            }
        bind(State.SELECT_MERCHANT.toString(), actionSelectMerchant, false)

        val actionPreviewHistory =
            ActionPreviewHistory { action ->
                (action as ActionPreviewHistory).setParam(
                    currentContext
                )
            }
        bind(State.PREVIEW_HISTORY.toString(), actionPreviewHistory, false)

        val actionSelectReport =
            ActionSelectReportType { action ->
                (action as ActionSelectReportType).setParam(
                    currentContext
                )
            }
        bind(State.SELECT_REPORT_TYPE.toString(), actionSelectReport, false)

        val actionPrintReport =
            ActionPrintMerchantReport { action ->
                (action as ActionPrintMerchantReport).setParam(
                    currentContext,
                    reportType
                )
            }
        bind(State.PRINT_REPORT.toString(), actionPrintReport, false)

        gotoState(State.SELECT_MERCHANT.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult) {
        when (State.valueOf(currentState!!)) {
            State.SELECT_MERCHANT -> {
                if (result.ret == TransResult.SUCC) {
                    if(getString(R.string.multi_merchant_all_merchant) != result.data)
                        gotoState(State.PREVIEW_HISTORY.toString())
                    else
                        gotoState(State.SELECT_REPORT_TYPE.toString())
                } else {
                   transEnd(result)
                }
            }
            State.PREVIEW_HISTORY -> {
                if (result.ret == TransResult.ERR_USER_CANCEL) {
                    gotoState(State.SELECT_MERCHANT.toString())
                } else {
                    transEnd(result)
                }
            }
            State.SELECT_REPORT_TYPE -> {
                reportType = result.data as String
                gotoState(State.PRINT_REPORT.toString())
            }
            State.PRINT_REPORT -> {
                if (MerchantProfileManager.applyNextProfile() == TransResult.SUCC) {
                    gotoState(State.PRINT_REPORT.toString())
                } else {
                    transEnd(result)
                }
            }
            else -> transEnd(result)
        }
    }
}