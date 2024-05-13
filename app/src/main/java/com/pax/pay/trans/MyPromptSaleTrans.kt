package com.pax.pay.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.abl.utils.EncUtils
import com.pax.appstore.DownloadManager.EdcRef1Ref2Mode
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.*
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.task.PrintTask
import com.pax.pay.utils.TransResultUtils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils

class MyPromptSaleTrans(
    context: Context,
    transType: ETransType,
    transListener: TransEndListener?,
) : BaseTrans(context, transType, transListener) {
    private var isVerifyState = false
    private var roundNo = 0
    private lateinit var saleTrans: TransData

    enum class State { ENTER_AMOUNT, ENTER_REF1_REF2, SCAN_QR, INQUIRY, VERIFY, INPUT_PWD, PRINT }

    override fun bindStateOnAction() {

        val amountAction = ActionEnterAmount { action ->
            (action as ActionEnterAmount).setParam(
                currentContext,
                getString(R.string.trans_sale), false
            )
        }
        bind(State.ENTER_AMOUNT.toString(), amountAction, true)

        val actionEnterRef1Ref2 = ActionEnterRef1Ref2 { action: AAction ->
            (action as ActionEnterRef1Ref2).setParam(
                currentContext,
                getString(R.string.menu_sale)
            )
        }
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true)

        val scanCodeAction = ActionScanQr { action ->
            (action as ActionScanQr).setParam(currentContext, "", 100)
        }
        bind(State.SCAN_QR.toString(), scanCodeAction)

        val qrSaleInquiry = ActionQrSaleInquiry { action ->
            (action as ActionQrSaleInquiry).setParam(currentContext,
                getString(R.string.menu_qr_sale_inquiry), transData)
        }
        bind(State.INQUIRY.toString(), qrSaleInquiry, false)

        // confirm information
        val confirmInfoAction = ActionVerifyTrans { action ->
            (action as ActionVerifyTrans).setParam(
                currentContext,
                getString(R.string.trans_qr_sale_inquiry),
                isVerifyState,
                transData.amount,
                transData.walletPartnerID
            )
        }
        bind(State.VERIFY.toString(), confirmInfoAction, true)

        val inputPasswordAction = ActionInputPassword { action ->
            (action as ActionInputPassword).setParam(currentContext, 6,
                getString(R.string.prompt_cancel_pwd), null, false, true)
        }
        bind(State.INPUT_PWD.toString(), inputPasswordAction, true)

        val printTask = PrintTask(currentContext,
            transData,
            PrintTask.genTransEndListener(this@MyPromptSaleTrans, WalletQrSaleTrans.State.PRINT.toString()))
        bind(State.PRINT.toString(), printTask)

        gotoState(State.ENTER_AMOUNT.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        val ret = result!!.ret
        if(ret == null) {
            transEnd(result)
            return
        }

        when (State.valueOf(currentState!!)) {
            State.ENTER_AMOUNT -> goToEnterRef(result)
            State.ENTER_REF1_REF2 -> goToScanQR(result)
            State.SCAN_QR -> goToSaleOnline(result)
            State.INQUIRY -> onAfterInquiry(result)
            State.VERIFY -> goToInquiry(result)
            State.INPUT_PWD -> onAfterInputPassword(result)
            State.PRINT -> onPrint(result)
        }
    }

    private fun goToEnterRef(result: ActionResult) {
        if (result.ret != TransResult.SUCC) {
            transEnd(result)
            return
        }

        transData.amount = result.data.toString()

        val iMode = FinancialApplication.getSysParam()[SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE]
        val mode = EdcRef1Ref2Mode.getByMode(iMode)
        if (mode != EdcRef1Ref2Mode.DISABLE) {
            gotoState(State.ENTER_REF1_REF2.toString())
        } else {
            gotoState(State.SCAN_QR.toString())
        }
    }

    private fun goToScanQR(result: ActionResult) {
        var ref1: String? = null
        var ref2: String? = null
        if (result.data != null) {
            ref1 = result.data.toString()
        }
        if (result.data1 != null) {
            ref2 = result.data1.toString()
        }

        transData.saleReference1 = ref1
        transData.saleReference2 = ref2

        gotoState(State.SCAN_QR.toString())
    }

    private fun goToSaleOnline(result: ActionResult) {
        if(transData.transType == ETransType.QR_MYPROMPT_VERIFY && result.ret == TransResult.ERR_USER_CANCEL) {
            gotoState(State.VERIFY.toString())
            return
        }
        val acqManager = FinancialApplication.getAcqManager()
        val acquirer = acqManager.findAcquirer(Constants.ACQ_MY_PROMPT)
        transData.acquirer = acquirer
        transData.issuer = acqManager.findIssuer(Constants.ISSUER_MY_PROMPT)
        transData.batchNo = acquirer.currBatchNo.toLong()
        transData.qrSourceOfFund = Constants.ISSUER_MY_PROMPT
        transData.tpdu = "600" + acquirer.nii + "0000"
        transData.qrCode = result.data.toString()
        gotoState(State.INQUIRY.toString())
    }

    private fun onAfterInquiry(result: ActionResult) {
        val transType = transData.transType
        when {
            transType == ETransType.QR_MYPROMPT_INQUIRY && transData.lastTrans && result.ret != TransResult.SUCC -> {
                result.ret = TransResult.ERR_USER_CANCEL
                transEnd(result)
            }
            transType == ETransType.QR_MYPROMPT_SALE && result.ret == TransResult.PROMPT_INQUIRY -> {
                saleTrans = transData
                gotoState(State.VERIFY.toString())
            }
            (transType == ETransType.QR_MYPROMPT_INQUIRY || transType == ETransType.QR_MYPROMPT_VERIFY)
                    && result.ret == TransResult.PROMPT_INQUIRY -> goToVerifyScreen()
            (transType == ETransType.QR_MYPROMPT_INQUIRY || transType == ETransType.QR_MYPROMPT_VERIFY) &&
                    result.ret != TransResult.SUCC &&
                    result.ret != TransResult.ERR_HOST_REJECT -> {
                DialogUtils.showErrMessage(
                    currentContext,
                    getString(R.string.trans_sale),
                    TransResultUtils.getMessage(result.ret),
                    { goToVerifyScreen() },
                    Constants.SUCCESS_DIALOG_SHOW_TIME
                )
            }
            transType == ETransType.QR_MYPROMPT_SALE && result.ret == TransResult.SUCC -> {
                FinancialApplication.getTransDataDbHelper().insertTransData(transData)
                gotoState(State.PRINT.toString())
            }
            (transType == ETransType.QR_MYPROMPT_INQUIRY
                    || transType == ETransType.QR_MYPROMPT_VERIFY)
                    && result.ret == TransResult.SUCC -> {
                saleTrans.channel = transData.channel
                transData = saleTrans
                FinancialApplication.getTransDataDbHelper().insertTransData(transData)
                gotoState(State.PRINT.toString())
            }
            result.ret == TransResult.ERR_HOST_REJECT -> DialogUtils.showErrMessage(
                currentContext,
                getString(R.string.trans_sale),
                TransResultUtils.getMessage(result.ret),
                { transEnd(result) },
                Constants.SUCCESS_DIALOG_SHOW_TIME
            )
            else -> transEnd(result)
        }
    }

    private fun goToVerifyScreen() {
        roundNo++
        if (roundNo == 4) isVerifyState = true
        gotoState(State.VERIFY.toString())
    }

    private fun goToInquiry (result: ActionResult) {
        if (result.ret != TransResult.SUCC) {
            transEnd(result)
            return
        }

       when(result.data.toString()){
           "Inquiry" -> onInitialTrans(ETransType.QR_MYPROMPT_INQUIRY)
           "Cancel" -> gotoState(State.INPUT_PWD.toString())
           "Verify" -> onInitialTrans(ETransType.QR_MYPROMPT_VERIFY)
       }
    }

    private fun onAfterInputPassword(result: ActionResult) {
        if (result.ret != TransResult.SUCC) {
            transEnd(result)
            return
        }

       if (result.data.toString() == "DismissDialogPass") {
           gotoState(State.VERIFY.toString())
           return
        }

        val data = EncUtils.sha1(result.data as String)
        if (data != FinancialApplication.getSysParam()[SysParam.StringParam.SEC_VOID_PWD]) {
            DialogUtils.showErrMessage(currentContext,
                getString(R.string.trans_sale),
                getString(R.string.err_password),
                { gotoState(State.VERIFY.toString()) },
                Constants.FAILED_DIALOG_SHOW_TIME)
        } else {
            onInitialTrans(ETransType.QR_MYPROMPT_INQUIRY, true)
        }

    }

    private fun onInitialTrans(eTransType: ETransType, isCancel: Boolean = false) {
        val trans = Component.transInit()
        val acqManager = FinancialApplication.getAcqManager()
        val acquirer = acqManager.findAcquirer(Constants.ACQ_MY_PROMPT)
        trans.acquirer = acquirer
        trans.issuer = acqManager.findIssuer(Constants.ISSUER_MY_PROMPT)
        trans.batchNo = acquirer.currBatchNo.toLong()
        trans.transType = eTransType
        trans.amount = transData.amount
        trans.field63 = transData.field63
        trans.qrCode = saleTrans.qrCode
        trans.walletPartnerID = transData.walletPartnerID
        trans.refNo = transData.refNo
        trans.field28 = transData.field28
        trans.tpdu = "600" + acquirer.nii + "0000"
        trans.lastTrans = isCancel
        transData = trans

        when (eTransType) {
            ETransType.QR_MYPROMPT_VERIFY -> gotoState(State.SCAN_QR.toString())
            else -> gotoState(State.INQUIRY.toString())
        }
    }

    private fun onPrint(result: ActionResult) {
        when (result.ret) {
            TransResult.SUCC -> transEnd(result)
            else -> {
                dispResult(transType.transName, result, null)
                gotoState(WalletQrSaleTrans.State.PRINT.toString())
            }
        }
    }
}