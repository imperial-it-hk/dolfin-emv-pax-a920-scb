package com.pax.pay.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.AAction.ActionStartListener
import com.pax.abl.core.ActionResult
import com.pax.abl.utils.EncUtils
import com.pax.appstore.DownloadManager
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.eemv.entity.CTransResult
import com.pax.eemv.enums.ETransResult
import com.pax.pay.ECR.EcrData
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.emv.EmvTransProcess
import com.pax.pay.trans.action.*
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.task.PrintTask
import com.pax.settings.SysParam
import com.pax.view.dialog.CustomAlertDialog
import kotlin.experimental.and

class OfflineTrans(context: Context?, transListener: TransEndListener?) : BaseTrans(context, ETransType.OFFLINE_TRANS_SEND, transListener) {
    private var amount: String? = null
    private var isNeedInputAmount: Boolean = true
    private var isFreePin: Boolean = true
    private var isSupportBypass: Boolean = true

    private var searchCardMode: Byte = -1
    private var orgSearchCardMode: Byte = -1
    private var needFallBack: Boolean = false
    private var currentMode: Byte? = null

    private var cntTryAgain: Int = 0

    constructor(context: Context?, mode: Byte, isFreePin: Boolean, transListener: TransEndListener?): this(context, transListener) {
        this.isNeedInputAmount = true
        setParam(null, mode, isFreePin)
    }

    constructor(context: Context?, amount: String?, mode: Byte, transListener: TransEndListener?): this(context, transListener) {
        this.isNeedInputAmount = false
        setParam(null, mode, isFreePin)
    }

    private fun setParam(amount: String?, mode: Byte, isFreePin: Boolean) {
        this.amount = amount
        this.searchCardMode = mode
        this.orgSearchCardMode = mode
        this.isFreePin = isFreePin
    }

    private fun ermErrorExceedCheck() {
        if (FinancialApplication.getTransDataDbHelper().findCountTransDataWithEReceiptUploadStatus(true) >= 30) {
            transEnd(ActionResult(TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR, null))
            return
        }
    }

    enum class State {
        INPUT_PWD, ENTER_AMOUNT, ENTER_REF1_REF2, ENTER_AUTH_CODE, CHECK_CARD,
        ENTER_PIN, EMV_READ_CARD, EMV_PROC, SIGNATURE, PRINT
    }

    override fun bindStateOnAction() {
        //input manager password
        val inputPasswordAction = ActionInputPassword { action ->
            (action as ActionInputPassword).setParam(
                currentContext, 6,
                getString(R.string.prompt_offline_pwd), null
            )
        }
        bind(State.INPUT_PWD.toString(), inputPasswordAction)

        //Enter Amount
        val amountAction = ActionEnterAmount { action ->
            (action as ActionEnterAmount).setParam(
                currentContext,
                getString(R.string.menu_offline), false
            )
        }
        bind(State.ENTER_AMOUNT.toString(), amountAction, true)

        val actionEnterRef1Ref2 = ActionEnterRef1Ref2 { action ->
            (action as ActionEnterRef1Ref2).setParam(
                currentContext,
                getString(R.string.menu_offline)
            )
        }
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true)

        //enter auth code action
        val enterAuthCodeAction = ActionEnterAuthCode { action ->
            (action as ActionEnterAuthCode).setParam(
                currentContext,
                getString(R.string.menu_offline),
                getString(R.string.prompt_auth_code),
                transData.amount
            )
        }
        bind(State.ENTER_AUTH_CODE.toString(), enterAuthCodeAction, true)

        // search card action
        val searchCardAction = ActionSearchCard { action ->
            (action as ActionSearchCard).setParam(
                currentContext, getString(R.string.menu_offline), searchCardMode, transData.amount,
                null, getString(R.string.prompt_insert_swipe_card), transData
            )
        }
        bind(State.CHECK_CARD.toString(), searchCardAction, true)

        // enter pin action
        val enterPinAction =
            ActionEnterPin { action ->
                // if quick pass by pin, set isSupportBypass as false,input password
                if (!isFreePin) {
                    isSupportBypass = false
                }
                (action as ActionEnterPin).setParam(
                    currentContext,
                    getString(R.string.menu_offline), transData.pan, isSupportBypass,
                    getString(R.string.prompt_pin),
                    getString(R.string.prompt_no_pin),
                    transData.amount,
                    transData.tipAmount,
                    ActionEnterPin.EEnterPinType.ONLINE_PIN, transData
                )
            }
        bind(State.ENTER_PIN.toString(), enterPinAction, true)

        // emv read card action
        val emvReadCardAction = ActionEmvReadCardProcess { action ->
            (action as ActionEmvReadCardProcess).setParam(
                currentContext,
                emv,
                transData
            )
        }
        bind(State.EMV_READ_CARD.toString(), emvReadCardAction)

        // emv action
        val emvProcessAction = ActionEmvAfterReadCardProcess { action ->
            (action as ActionEmvAfterReadCardProcess).setParam(
                currentContext,
                emv,
                transData
            )
        }
        bind(State.EMV_PROC.toString(), emvProcessAction)

        // signature action
        val signatureAction = ActionSignature { action ->
            (action as ActionSignature).setParam(
                currentContext,
                transData.amount,
                !Component.isAllowSignatureUpload(transData)
            )
        }
        bind(State.SIGNATURE.toString(), signatureAction)

        //print preview action
        val printTask = PrintTask(
            currentContext,
            transData,
            PrintTask.genTransEndListener(this, State.PRINT.toString())
        )
        bind(State.PRINT.toString(), printTask)

        // ERM Maximum Exceed Transaction check
        ermErrorExceedCheck()

        when {
            FinancialApplication.getSysParam()[SysParam.BooleanParam.OTHTC_VERIFY] -> {
                gotoState(State.INPUT_PWD.toString())
            }
            isNeedInputAmount -> {
                if (searchCardMode.toInt() == -1) {
                    searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND)
                    orgSearchCardMode = searchCardMode
                    transType = ETransType.OFFLINE_TRANS_SEND
                }
                gotoState(State.ENTER_AMOUNT.toString())
            }
            else -> {
                cntTryAgain = 0
                needFallBack = false
                if (searchCardMode.toInt() == -1) {
                    searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND)
                    orgSearchCardMode = searchCardMode
                    transType = ETransType.OFFLINE_TRANS_SEND
                }
                transData.amount = this.amount
                gotoState(State.CHECK_CARD.toString())
            }
        }
    }

    override fun gotoState(state: String?) {
        if (state!! == State.INPUT_PWD.toString()) {
            EcrData.instance.isOnHomeScreen = false
        }
        super.gotoState(state)
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        when (State.valueOf(currentState!!)) {
            State.INPUT_PWD -> {
                if (result!!.ret != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true
                    transEnd(result)
                } else {
                    cntTryAgain = 0
                    needFallBack = false
                    onInputPwd(result)
                }
            }
            State.ENTER_AMOUNT -> {
                cntTryAgain = 0
                needFallBack = false
                onEnterAmount(result!!)
            }
            State.ENTER_REF1_REF2 -> {
                var ref1: String? = null
                var ref2: String? = null
                result?.data?.let {
                    ref1 = it.toString()
                }
                result?.data1?.let {
                    ref2 = it.toString()
                }

                transData.saleReference1 = ref1
                transData.saleReference2 = ref2

                gotoState(State.ENTER_AUTH_CODE.toString())
            }
            State.ENTER_AUTH_CODE -> {
                onEnterAuthCode(result!!)
            }
            State.CHECK_CARD -> {
                onCheckCard(result!!)
            }
            State.ENTER_PIN -> {
                onEnterPin(result!!)
            }
            State.EMV_READ_CARD -> {
                onEmvReadCard(result!!)
            }
            State.EMV_PROC -> {
                if (result!!.ret != TransResult.SUCC) {
                    transEnd(result)
                } else {
                    val transResult: CTransResult = result.data as CTransResult
                    afterEMVProcess(transResult.transResult)
                }
            }
            State.SIGNATURE -> {
                onSignature(result!!)
            }
            State.PRINT -> {
                if (result!!.ret == TransResult.SUCC) {
                    transEnd(result)
                } else {
                    dispResult(transType.transName, result, null)
                    gotoState(State.PRINT.toString())
                }
            }
        }
    }

    private fun onInputPwd(result: ActionResult) {
        val data = EncUtils.sha1(result.data as String)
        if (data != FinancialApplication.getSysParam()[SysParam.StringParam.SEC_OFFLINE_PWD]) {
            EcrData.instance.isOnHomeScreen = true
            transEnd(ActionResult(TransResult.ERR_PASSWORD, null))
            return
        }

        if (isNeedInputAmount) {
            if (searchCardMode.toInt() == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND)
                orgSearchCardMode = searchCardMode
                transType = ETransType.OFFLINE_TRANS_SEND
            }
            gotoState(State.ENTER_AMOUNT.toString())
        } else {
            cntTryAgain = 0
            needFallBack = false
            if (searchCardMode.toInt() == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND)
                orgSearchCardMode = searchCardMode
                transType = ETransType.OFFLINE_TRANS_SEND
            }
            transData.amount = this.amount
            gotoState(State.ENTER_AUTH_CODE.toString())
        }
    }

    private fun onEnterAmount(result: ActionResult) {
        //set total amount
        transData.amount = result.data?.toString()
        //set tip amount
        transData.tipAmount = result.data1?.toString()

        val iMode = FinancialApplication.getSysParam()[SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE]
        val mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode)

        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            gotoState(State.ENTER_REF1_REF2.toString())
        } else {
            gotoState(State.ENTER_AUTH_CODE.toString())
        }
    }

    private fun onEnterAuthCode(result: ActionResult) {
        //get auth code
        val authCode = result.data?.toString()
        //set auth code
        transData.origAuthCode = authCode
        transData.authCode = authCode

        gotoState(State.CHECK_CARD.toString())
    }

    private fun onCheckCard(result: ActionResult) {
        val cardInfo: ActionSearchCard.CardInformation = result.data as ActionSearchCard.CardInformation
        saveCardInfo(cardInfo, transData)

        transData.transType = ETransType.OFFLINE_TRANS_SEND

        currentMode = cardInfo.searchMode
        when (currentMode) {
            ActionSearchCard.SearchMode.INSERT -> {
                if (needFallBack) {
                    transData.enterMode = TransData.EnterMode.FALLBACK
                }
                needRemoveCard = true
                gotoState(State.EMV_READ_CARD.toString())
            }
            ActionSearchCard.SearchMode.SWIPE,
            ActionSearchCard.SearchMode.KEYIN -> {
                if (isSupportedOfflineTrans()) {
                    // Include TEST MODE
                    val acq = FinancialApplication.getAcqManager().curAcq
                    if (acq.isEnableUpi && !acq.isTestMode && transData.issuer != null &&
                        (Constants.ISSUER_UP == transData.issuer.name || Constants.ISSUER_TBA == transData.issuer.name)
                    ) {
                        gotoState(State.ENTER_PIN.toString())
                    } else {
                        if (!isFreePin) {
                            gotoState(State.ENTER_PIN.toString())
                        } else {
                            // save trans data
                            Component.saveOfflineTransNormalSale(transData)
                            // signature
                            toSignOrPrint()
                        }
                    }
                }
            }
        }
    }

    private fun onEnterPin(result: ActionResult) {
        val pinBlock = result.data?.toString()
        transData.pin = pinBlock
        if (pinBlock != null && pinBlock.isNotEmpty()) {
            transData.isHasPin = true
        }

        // save trans data
        Component.saveOfflineTransNormalSale(transData)
        // signature
        toSignOrPrint()
    }

    private fun onEmvReadCard(result: ActionResult) {
        val ret = result.ret
        when {
            ret == TransResult.ICC_TRY_AGAIN -> {
                cntTryAgain++
                if (cntTryAgain == 3) {
                    needFallBack = true
                    searchCardMode = searchCardMode and 0x01
                    showMsgDialog(
                        currentContext,
                        getString(R.string.prompt_fallback) + getString(R.string.prompt_swipe_card)
                    )
                } else {
                    showTryAgainDialog(currentContext)
                }
                return
            }
            ret == TransResult.NEED_FALL_BACK -> {
                needFallBack = true
                searchCardMode = searchCardMode and 0x01
                gotoState(State.CHECK_CARD.toString())
                return
            }
            ret != TransResult.SUCC -> {
                transEnd(result)
                return
            }
            isSupportedOfflineTrans() -> {
                gotoState(State.EMV_PROC.toString())
            }
        }
    }

    private fun afterEMVProcess(transResult: ETransResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData)
        if (transResult == ETransResult.OFFLINE_APPROVED) {
            // save trans data
            Component.saveOfflineTransNormalSale(transData)
            toSignOrPrint()
        }
    }

    private fun onSignature(result: ActionResult) {
        // save signature data
        val signData = result.data as ByteArray?
        val signPath = result.data1 as ByteArray?
        signData?.let {
            if (it.isNotEmpty()) {
                transData.signData = it
                transData.signPath = signPath
                // update trans data，save signature
                FinancialApplication.getTransDataDbHelper().updateTransData(transData)
            }
        }
        // if terminal does not support signature ,card holder does not sign or time out，print preview directly.
        gotoState(State.PRINT.toString())
    }

    // need electronic signature or send
    private fun toSignOrPrint() {
//        if (transData.isTxnSmallAmt && transData.numSlipSmallAmt == 0) { //EDCBBLAND-426 Support small amount
//            FinancialApplication.getTransDataDbHelper().updateTransData(transData)
//        }
        if (transData.isPinVerifyMsg || !transData.isOnlineTrans && transData.isHasPin) {
            transData.isSignFree = true
            gotoState(State.PRINT.toString())
        } else {
            transData.isSignFree = false
            val eSignature = FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE]
            if (eSignature && !transData.isTxnSmallAmt) {
                gotoState(State.SIGNATURE.toString())
            } else {
                gotoState(State.PRINT.toString()) // Skip SIGNATURE process
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData)
    }

    private fun isSupportedOfflineTrans(): Boolean {
        transData.issuer?.let {
            if (!it.isEnableOffline) {
                transEnd(ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null))
                return false
            }
            return true
        } ?: run {
            transEnd(ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null))
            return false
        }
    }

    private fun showTryAgainDialog(context: Context) {
        val dialog = CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE, 5000)
        dialog.setCancelClickListener {
            dialog.dismiss()
            transEnd(ActionResult(TransResult.ERR_ABORTED, null))
        }
        dialog.setConfirmClickListener {
            dialog.dismiss()
            gotoState(SaleTrans.State.CHECK_CARD.toString())
        }
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME)
        dialog.show()
        dialog.normalText =
            getString(R.string.prompt_try_again) + getString(R.string.prompt_insert_card)
        dialog.showCancelButton(true)
        dialog.showConfirmButton(true)
        dialog.setOnDismissListener { dialog.dismiss() }
    }

    private fun showMsgDialog(context: Context, msg: String) {
        val dialog = CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE)
        dialog.setCancelClickListener {
            dialog.dismiss()
            transEnd(ActionResult(TransResult.ERR_USER_CANCEL, null))
        }
        dialog.setConfirmClickListener {
            dialog.dismiss()
            gotoState(SaleTrans.State.CHECK_CARD.toString())
        }
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME)
        dialog.show()
        dialog.normalText = msg
        dialog.showCancelButton(true)
        dialog.showConfirmButton(true)
        dialog.setOnDismissListener { dialog.dismiss() }
    }
}