package com.pax.pay.trans.task

import android.content.Context
import com.pax.abl.core.ActionResult
import com.pax.abl.utils.EncUtils
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.ActionDoSettlement
import com.pax.pay.trans.action.ActionInputPassword
import com.pax.pay.trans.action.ActionSelectMerchant
import com.pax.pay.trans.model.MerchantProfileManager
import com.pax.pay.utils.TransResultUtils
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import java.util.*
import kotlin.collections.ArrayList


class SettlementMerchantTask(context: Context?, transListener: TransEndListener?) :
    BaseTask(context, transListener) {

    private var isSettlementAll = false
    private var acqList : ArrayList<String>? = null

    companion object {
    }

    internal enum class State {
        INPUT_PWD,
        SELECT_MERCHANT,
        SETTLE
    }
    override fun bindStateOnAction() {
        val inputPasswordAction = ActionInputPassword { action ->
            (action as ActionInputPassword).setParam(
                currentContext, 6,
                getString(R.string.prompt_settle_pwd), null
            )
        }
        bind(State.INPUT_PWD.toString(), inputPasswordAction)

        val actionSelectMerchant =
            ActionSelectMerchant { action ->
                (action as ActionSelectMerchant).setParam(
                    currentContext,
                    true
                )
            }
        bind(State.SELECT_MERCHANT.toString(), actionSelectMerchant, false)

        val actionDoSettlement =
            ActionDoSettlement { action ->
                (action as ActionDoSettlement).setParam( currentContext, isSettlementAll, acqList)
            }
        bind(State.SETTLE.toString(), actionDoSettlement, false)

        gotoState(State.INPUT_PWD.toString())
    }


    override fun onActionResult(currentState: String?, result: ActionResult) {
        when (State.valueOf(currentState!!)) {
            State.INPUT_PWD -> {
                if (result.ret != TransResult.SUCC) {
                    transEnd(result)
                } else {
                    val data = EncUtils.sha1(result.data as String)
                    if (data != FinancialApplication.getSysParam()[SysParam.StringParam.SEC_SETTLE_PWD]) {
                        transEnd(ActionResult(TransResult.ERR_PASSWORD, null))
                        return
                    }
                    gotoState(State.SELECT_MERCHANT.toString())
                }
            }
            State.SELECT_MERCHANT -> {
                //if(getString(R.string.multi_merchant_all_merchant) == result.data) {
                if(result.data as String == "All Merchant") {
                    isSettlementAll = true
                    acqList = loadAllAcquirer()
                    if (acqList!!.isEmpty()) {
                        transEnd(ActionResult(TransResult.ERR_NO_TRANS, null))
                        return
                    }
                }
                gotoState(State.SETTLE.toString())
            }
            State.SETTLE -> {
                if (isSettlementAll
                    && MerchantProfileManager.applyNextProfile() == TransResult.SUCC) {

                    gotoState(State.SETTLE.toString())
                } else {
                    transEnd(result)
                }
            }
            else -> transEnd(result)
        }
    }

    fun loadAllAcquirer() : ArrayList<String>? {
        var tmpAcqList : ArrayList<String>? = arrayListOf()
        val isEmptyBatchAlMerchant = FinancialApplication.getTransDataDbHelper().countOf() == 0L
        val acqs = FinancialApplication.getAcqManager().findEnableAcquirersExcept(listOf(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE))
        if (!isEmptyBatchAlMerchant && acqs!=null && acqs.size>0) {
            for (acq: Acquirer in acqs) {
                if (acq.isEnable) {
                    tmpAcqList?.add(acq.name)
                }
            }
        }
        return tmpAcqList
    }

    override fun transEnd(result: ActionResult?) {
        if (result!!.ret == TransResult.ERR_NO_TRANS || result.ret == TransResult.ERR_PASSWORD) {
            ActivityStack.getInstance().popTo(MainActivity::class.java)
            DialogUtils.showErrMessage(ActivityStack.getInstance().top(), getString(R.string.settle_all_merchants), TransResultUtils.getMessage(result.ret), {
                super.transEnd(result)
            }, Constants.FAILED_DIALOG_SHOW_TIME)
        } else {
            super.transEnd(result)
        }
    }
}