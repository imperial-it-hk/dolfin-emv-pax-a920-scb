package com.pax.pay.trans

import android.content.Context
import android.content.DialogInterface
import com.pax.abl.core.AAction
import com.pax.abl.core.AAction.ActionStartListener
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.eemv.entity.CTransResult
import com.pax.eemv.enums.ECvmResult
import com.pax.eemv.enums.ETransResult
import com.pax.jemv.clcommon.TransactionPath
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.constant.Constants.*
import com.pax.pay.emv.EmvSP200
import com.pax.pay.emv.EmvTags
import com.pax.pay.emv.EmvTransProcess
import com.pax.pay.emv.clss.ClssTransProcess
import com.pax.pay.extensions.toByteArrayOfHexString
import com.pax.pay.extensions.toHexString
import com.pax.pay.trans.action.*
import com.pax.pay.trans.action.ActionSearchCard.*
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransData.OfflineStatus
import com.pax.pay.trans.task.PrintTask
import com.pax.pay.utils.ToastUtils
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.CustomAlertDialog
import kotlinx.serialization.descriptors.PrimitiveKind
import th.co.bkkps.amexapi.action.ActionAmexCheckAID
import kotlin.experimental.and
import kotlin.experimental.or

class PreAuthorizationTrans(val context: Context?, val transType: ETransType?, val mode: Byte, val sendTcAdvice: Boolean ,val transListener: TransEndListener?) : BaseTrans(context, transType, transListener) {

    companion object {
        private const val DEBIT1 = "A000000333010101"
        private const val DEBIT2 = "A000000333010106"
        private const val TPN_DEBIT = "A000000677010101"
    }

    var cTransResult : CTransResult? = null
    var searchCardMode : Byte = -1
    var emvSP200 : EmvSP200? = null
    var currentSearchMode:Byte? = null
    enum class enumCardMode {
        ICC,
        CLSS,
        SP200
    }
    enum class State {
        ENTER_AMOUNT,
        CHECK_CARD,
        CHECK_AMEX_AID,
        AMEX_API,
        EMV_READ_CARD,
        EMV_PROC,
        ENTER_PIN,
        MAG_ONLINE,
        CTLSS_PRE_PROC,
        CTLSS_READ_CARD,
        CTLSS_PROC,
        TC_ADVICE_SEND,
        OFFLINE_SEND,
        PRINT,
        SIGNATURE,
    }

    init {
        searchCardMode = mode
    }

    override fun bindStateOnAction() {

        // ENTER AMOUNT
        val enterAmountAction = ActionEnterAmount(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionEnterAmount).setParam(currentContext, getString(R.string.transType_auth), false)
            }
        })
        bind(State.ENTER_AMOUNT.toString(), enterAmountAction, false)

        // CHECK CARD PROCESS
        val checkCardAction = ActionSearchCard(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionSearchCard).setParam(currentContext, getString(R.string.transType_auth), searchCardMode, transData.amount, null, "", transData)
            }
        })
        bind(State.CHECK_CARD.toString(), checkCardAction, false)


        // EMV READ CARD
        val emvReadCardProcessAction = ActionEmvReadCardProcess(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionEmvReadCardProcess).setParam(currentContext, emv, transData)
            }
        })
        bind(State.EMV_READ_CARD.toString(), emvReadCardProcessAction, true)


        val emvAfterReadCardProcessAction = ActionEmvAfterReadCardProcess(object: ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionEmvAfterReadCardProcess).setParam(currentContext, emv, transData)
            }
        })
        bind(State.EMV_PROC.toString(), emvAfterReadCardProcessAction, true)


        val enterPinAction = ActionEnterPin(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionEnterPin).setParam(currentContext, getString(R.string.transType_auth), transData.pan,false, getString(
                    R.string.prompt_pin), getString(R.string.prompt_no_pin), transData.amount, null,ActionEnterPin.EEnterPinType.ONLINE_PIN, transData )
            }
        })
        bind(State.ENTER_PIN.toString(), enterPinAction, true)




        val transOnlineAction = ActionTransOnline(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionTransOnline).setParam(currentContext, transData)
            }
        })
        bind(State.MAG_ONLINE.toString(),transOnlineAction, false)


        val clssPreProcAction = ActionClssPreProc(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionClssPreProc).setParam(clss, transData)
            }
        })
        bind(State.CTLSS_PRE_PROC.toString(),clssPreProcAction, false)


        val clssReadCard = ActionClssReadCardProcess(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionClssReadCardProcess).setParam(currentContext, clss, transData)
            }
        })
        bind(State.CTLSS_READ_CARD.toString(),clssReadCard, false)


        val clssProcAction = ActionClssAfterReadCardProcess(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionClssAfterReadCardProcess).setParam(currentContext, clss, transData, cTransResult)
            }
        })
        bind(State.CTLSS_PROC.toString(),clssProcAction, false)


        val tcAdviceAction = ActionTcAdvice(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionTcAdvice).setParam(currentContext, transData)
            }
        })
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction, false)

        val offlineSendAction = ActionOfflineSend(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionOfflineSend).setParam(currentContext, transData)
            }
        })
        bind(State.OFFLINE_SEND.toString(), offlineSendAction, false)

        val signatureAction = ActionSignature(object : ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionSignature).setParam(currentContext, transData.amount)
            }
        })
        bind(State.SIGNATURE.toString(), signatureAction)


        val printAction = PrintTask(currentContext, transData, PrintTask.genTransEndListener(this@PreAuthorizationTrans, State.PRINT.toString()))
        bind(State.PRINT.toString(), printAction)


        val amexCheckAidAction = ActionAmexCheckAID(object: ActionStartListener{
            override fun onStart(action: AAction?) {
                transData.aid?.let{
                    (action as ActionAmexCheckAID).setParam(currentContext, transData.enterMode, transData.aid)
                }?:run{
                    (action as ActionAmexCheckAID).setParam(currentContext, transData.enterMode)
                }
            }
        })
        bind(State.CHECK_AMEX_AID.toString(), amexCheckAidAction, false)

        /* ===============================================================================
                TODO: >> NEED !! Bind AMEX_API state for supporting AMEX preAuthorize
                TODO :          [1]. AMEX_PREAUTH_PROCESS
           =============================================================================== */


        // set default mandatory data
        transData.preAuthTransDate = Device.getTime(com.pax.pay.constant.Constants.DATE_PATTERN_01)
        transData.preAuthStatus = TransData.EPreAuthStatus.NORMAL


        gotoState(State.ENTER_AMOUNT.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        val state = State.valueOf(currentState!!)
        result?.let{
            when (state) {
                State.ENTER_AMOUNT      -> afterEnterAmount(it)
                State.CHECK_CARD        -> afterCheckCard(it)
                State.CHECK_AMEX_AID    -> afterAmexCheckAID(it)
                State.AMEX_API          -> afterAmexApiProceed(it)
                State.ENTER_PIN         -> afterEnterPin(it)
                State.MAG_ONLINE        -> afterMagOnline(it)
                State.CTLSS_PRE_PROC    -> afterContactlessPreProcess(it)
                State.CTLSS_READ_CARD   -> afterContactlessProcess(it)
                State.CTLSS_PROC        -> afterContactlessProcess(it)
                State.EMV_READ_CARD     -> afterEmvReadCard(it)
                State.EMV_PROC          -> afterEmvProcess(it)
                State.TC_ADVICE_SEND    -> afterSendTcAdvice(it)
                State.OFFLINE_SEND      -> toSignOrPrint()
                State.SIGNATURE         -> afterSignature(it)
                State.PRINT             -> afterPrint(it)
                else -> {
                    transEnd(ActionResult(TransResult.ERR_PROCESS_FAILED, null))
                }
            }
        }?:run{
            transEnd(ActionResult(TransResult.ERR_MISSING_INTERNAL_PROC_RESULT, null))
        }
    }

    fun afterEnterAmount(result: ActionResult) {
        if (result.ret==TransResult.SUCC) {
            transData.amount = result.data?.toString()
            if ((searchCardMode and SearchMode.WAVE) == SearchMode.WAVE) {
                gotoState(State.CTLSS_PRE_PROC.toString())
            } else {
                gotoState(State.CHECK_CARD.toString())
            }
        } else {
            transEnd(result)
        }
    }

    fun afterCheckCard(readCardResult: ActionResult) {
        if (readCardResult.ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
            processAmexApi(readCardResult)
        } else if (readCardResult.ret != TransResult.SUCC) {
            transEnd(readCardResult)
            return
        } else  {
            onCheckCard(readCardResult)
        }
    }

    fun onCheckCard(readCardResult:ActionResult) {
        if (readCardResult.data != null && readCardResult.data is CardInformation) {
            val cardInfo = readCardResult.data as CardInformation
            currentSearchMode = cardInfo.searchMode
            currentSearchMode?.let {
                if (it != SearchMode.SP200) {                  // ACCEPT ONLY 2 MODE {SWIPE, INSERT}
                    saveCardInfo(cardInfo, transData)
                } else {
                    saveSP200Info(cardInfo.emvSP200)
                    emvSP200 = cardInfo.emvSP200

                    // Extra check for TPN, UPI Debit card won't support to use PreAuthorization feature
                    if (isDebitCardScheme(enumCardMode.SP200) || !isIssuerSupportPreAuth()) {
                        transEnd(ActionResult(TransResult.ERR_CARD_UNSUPPORTED,null))
                        return
                    }
                }

                needRemoveCard = ((it==SearchMode.INSERT) || (it==SearchMode.WAVE) || (it==SearchMode.SP200))
                transData.transType = ETransType.PREAUTHORIZATION

                if (it.equals(SearchMode.INSERT) || it.equals(SearchMode.WAVE) || it.equals(SearchMode.SP200)) {
                    gotoState(State.CHECK_AMEX_AID.toString())
                }

                if (it.equals(SearchMode.KEYIN) || it.equals(SearchMode.SWIPE)) {
                    goTipBranch()
                }
            }?:run{
                transEnd(ActionResult(TransResult.ERR_READ_CARD, null))
            }
        } else {
            transEnd(ActionResult(TransResult.ERR_READ_CARD, null))
        }
    }

    fun processAmexApi(checkCardResult:ActionResult) {
        // to check Amex Multi-Application isn't on Locked-Batch status
        if (Component.chkSettlementStatus(Constants.ACQ_AMEX)){
            transEnd(ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null))
            return
        }

        if (checkCardResult.data is CardInformation) {
            val cardInfo = checkCardResult.data as CardInformation
            if (cardInfo.searchMode == SearchMode.KEYIN) {
                transData.enterMode = TransData.EnterMode.MANUAL
            } else if (cardInfo.searchMode == SearchMode.SWIPE) {
                transData.enterMode = TransData.EnterMode.SWIPE
            }

            transData.pan = cardInfo.pan
            transData.expDate = cardInfo.expDate
            transData.track1 = cardInfo.track1
            transData.track2 = cardInfo.track2
            transData.track3 = cardInfo.track3
            setTransRunning(false)
            gotoState(State.AMEX_API.toString())
        } else {
            transEnd(ActionResult(TransResult.ERR_MISSING_CARD_INFORMATION, null))
        }
    }

    fun afterAmexCheckAID(amexCheckAidResult:ActionResult) {
        if (amexCheckAidResult.ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
            setTransRunning(false)
            gotoState(State.AMEX_API.toString())
        } else if (amexCheckAidResult.ret.equals(TransResult.ERR_CARD_UNSUPPORTED)
                   || amexCheckAidResult.ret.equals(TransResult.ERR_SETTLE_NOT_COMPLETED)) {
            transEnd(amexCheckAidResult)
        } else {
            if (currentSearchMode == SearchMode.INSERT) {
                gotoState(State.EMV_READ_CARD.toString())
            } else if (currentSearchMode == SearchMode.WAVE) {
                gotoState(State.CTLSS_READ_CARD.toString())
            } else if (currentSearchMode == SearchMode.SP200) {
                afterSp200Process()
            } else {
                transEnd(ActionResult(TransResult.ERR_CARD_ENTRY_NOT_ALLOW, null))
            }
        }
    }

    fun afterAmexApiProceed(amexApiResult:ActionResult) {
        if (amexApiResult.ret == TransResult.SUCC) {
            transEnd(amexApiResult)
        } else {
            transEnd(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }

    fun afterSp200Process() {
        emvSP200?.let {
            when (it.getiResult()) {
                0, 4 -> {
                    Device.beepErr()
                    showMsgDialog(currentContext, "${getString(R.string.dialog_clss_declined)}, ${getString(R.string.dialog_clss_try_contact)}")
                    return
                }
                1 -> {
                    transData.isOnlineTrans = false
                    transData.origAuthCode = getString(R.string.response_Y1_str)
                    transData.authCode = getString(R.string.response_Y1_str)
                    transData.stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO).toLong()
                    transData.traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO).toLong()
                    Component.saveOfflineTransNormalSale(transData)
                    toSignOrPrint()
                    return
                }
                3 -> {
                    ToastUtils.showMessage("Please use contact")
                    transEnd(ActionResult(TransResult.ERR_ABORTED, null))
                    return
                }
                else -> { return@let }
            }
        }

        if (!transData.isPinFree && transData.isSignFree) {
            gotoState(State.ENTER_PIN.toString())
        } else {
            gotoState(State.MAG_ONLINE.toString())
        }
    }

    fun afterEnterPin(pinResult: ActionResult) {
        val pinBloc = pinResult.data as String?
        transData.pin = pinBloc
        pinBloc?.let{
            if (it.isNotEmpty()) {
                transData.isPinVerifyMsg = true
                transData.isHasPin = true
            }
        }

        gotoState(State.MAG_ONLINE.toString())
    }

    fun afterEmvReadCard(emvReadCardResult: ActionResult) {
        if (emvReadCardResult.ret != TransResult.SUCC) {
            transEnd(emvReadCardResult)
            return
        }

        if (isDebitCardScheme(enumCardMode.ICC) || !isIssuerSupportPreAuth()) {
            transEnd(ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null))
            return
        }

        if (emvReadCardResult.ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
            setTransRunning(false)
            gotoState(State.AMEX_API.toString())
            return
        }

        gotoState(State.EMV_PROC.toString())
    }

    fun afterEmvProcess(emvProcessResult: ActionResult) {
        val pan : String = transData.pan as String
        val f55Dup : ByteArray = EmvTags.getF55(emv, transType, true, pan)
        if (f55Dup.isNotEmpty()) {
            val dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.acquirer)
            dupTransData?.let {
                it.dupIccData = Utils.bcd2Str(f55Dup)
                FinancialApplication.getTransDataDbHelper().updateTransData(it)
            }
        }

        if (emvProcessResult.ret != TransResult.SUCC) {
            transEnd(emvProcessResult)
            return
        }

        if (emvProcessResult.data is CTransResult) {
            val emvTransResult = emvProcessResult.data as CTransResult
            EmvTransProcess.emvTransResultProcess(emvTransResult.transResult, emv, transData)

            if (emvTransResult.transResult == ETransResult.ONLINE_APPROVED ) {
                processTcAdvice()
            } else if (emvTransResult.transResult == ETransResult.OFFLINE_APPROVED) {
                toSignOrPrint()
            } else {
                return
            }
        }
    }

    fun processTcAdvice() {
        if (transData.acquirer.isEmvTcAdvice
            && transData.transType == ETransType.PREAUTHORIZATION
            && transData.enterMode != TransData.EnterMode.SP200
            && sendTcAdvice) {
            gotoState(State.TC_ADVICE_SEND.toString())
        } else {
            checkOfflineTrans()
        }
    }

    fun afterMagOnline(magOnlineResult: ActionResult) {
        if (magOnlineResult.ret != TransResult.SUCC) {
            transEnd(magOnlineResult)
            return
        }
        processTcAdvice()
    }

    fun afterContactlessPreProcess(ctlssReadCardProcessResult : ActionResult) {
        if (ctlssReadCardProcessResult.ret != TransResult.SUCC) {
            searchCardMode = searchCardMode and 0x03
        }

        gotoState(State.CHECK_CARD.toString())
    }

    fun afterContactlessProcess(ctlssProcessResult : ActionResult) {
        if (ctlssProcessResult.ret != TransResult.SUCC) {
            transEnd(ctlssProcessResult)
            return
        }

        if (isDebitCardScheme(enumCardMode.CLSS) || !isIssuerSupportPreAuth()) {
            transEnd(ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null))
            return
        }

        if (ctlssProcessResult.ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
            setTransRunning(false)
            gotoState(State.AMEX_API.toString())
            return
        }

        if (ctlssProcessResult.data!=null && ctlssProcessResult.data is CTransResult) {
            cTransResult = ctlssProcessResult.data as CTransResult
            cTransResult?.let {
                contactlessSubProcess(it)
                if (it.transResult == ETransResult.CLSS_OC_APPROVED
                    || it.transResult == ETransResult.OFFLINE_APPROVED
                    || it.transResult == ETransResult.ONLINE_APPROVED) {
                    if (transData.isOnlineTrans) {
                        checkOfflineTrans()
                    } else {
                        toSignOrPrint()
                    }
                }
            }?:run {
                transEnd(ctlssProcessResult)
            }
        } else {
            transEnd(ctlssProcessResult)
        }
    }

    fun contactlessSubProcess(transResult : CTransResult) {
        if (transResult.transResult == ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE
            || transResult.transResult == ETransResult.CLSS_OC_TRY_AGAIN) {
            gotoState(State.CTLSS_PRE_PROC.toString())
        }

        val transPath = transData.clssTypeMode
        if ((transPath == TransactionPath.CLSS_MC_MAG || transPath == TransactionPath.CLSS_MC_MCHIP)
            && transResult.transResult == ETransResult.CLSS_OC_DECLINED) {
            clss.setListener(null)
            searchCardMode = SearchMode.INSERT or SearchMode.SWIPE or SearchMode.KEYIN
            showMsgDialog(currentContext, "${getString(R.string.dialog_clss_declined)}, ${getString(R.string.dialog_clss_try_contact)}")
        }

        transData.emvResult = transResult.transResult
        if (transResult.transResult == ETransResult.ABORT_TERMINATED || transResult.transResult == ETransResult.CLSS_OC_DECLINED) {
            Device.beepErr()
            transEnd(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        if (transResult.transResult == ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE) {
            ToastUtils.showMessage("please use insert card method")
            transEnd(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        ClssTransProcess.clssTransResultProcess(transResult, clss, transData)
        transData.isPinVerifyMsg = (transResult.cvmResult == ECvmResult.OFFLINE_PIN || transResult.cvmResult == ECvmResult.ONLINE_PIN || transResult.cvmResult == ECvmResult.ONLINE_PIN_SIG)

        val isSignFreeBool = !((transResult.cvmResult == ECvmResult.SIG || !Component.isSignatureFree(transData, transResult)) && !transData.isPinVerifyMsg)
        transData.isSignFree = isSignFreeBool

        if (transResult.transResult == ETransResult.CLSS_OC_APPROVED || transResult.transResult == ETransResult.OFFLINE_APPROVED || transResult.transResult == ETransResult.ONLINE_APPROVED) {
            transData.isOnlineTrans = (transResult.transResult == ETransResult.ONLINE_APPROVED)
            return
        }

        gotoState(State.CTLSS_PROC.toString())
    }

    fun afterSendTcAdvice(tcAdviceResult: ActionResult) {
        if (tcAdviceResult.ret==TransResult.SUCC) {
            checkOfflineTrans()
        } else {
            toSignOrPrint()
        }
    }

    fun afterSignature(signatureResult : ActionResult) {
        val signData = signatureResult.data as ByteArray?
        val signPath = signatureResult.data1 as ByteArray?

        signData?.let {
            if (it.isNotEmpty()) {
                transData.signData = it
                transData.signPath = signPath
                FinancialApplication.getTransDataDbHelper().updateTransData(transData)
            }
        }

        gotoState(State.PRINT.toString())
    }

    fun afterPrint(printResult : ActionResult) {
        if (printResult.ret == TransResult.SUCC) {
            transEnd(printResult)
        } else {
            dispResult(transType.transName, printResult, null)
            gotoState(State.PRINT.toString())
        }
    }

    fun checkOfflineTrans() {
        // Trickle feed offline transaction
        val acqList = arrayListOf<Acquirer>(transData.acquirer)
        val excludes = arrayListOf<OfflineStatus>(OfflineStatus.OFFLINE_SENT, OfflineStatus.OFFLINE_VOIDED)
        val offlineTransList = FinancialApplication.getTransDataDbHelper().findAllOfflineTransData(acqList, excludes)

        if (transData.transType.equals(ETransType.PREAUTHORIZATION)
            && !offlineTransList.isEmpty() && offlineTransList.get(0).id != transData.id) {
            gotoState(State.OFFLINE_SEND.toString())
        } else {
            toSignOrPrint()
        }
    }

    fun toSignOrPrint() {
        if (transData.isPinVerifyMsg || (!transData.isOnlineTrans && transData.isHasPin)) {
            transData.isSignFree = true
            gotoState(State.PRINT.toString())
        } else {
            if ((currentSearchMode == SearchMode.WAVE || currentSearchMode == SearchMode.SP200) && transData.isSignFree) {
                gotoState(State.PRINT.toString())
            } else {
                transData.isSignFree = false
                val eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE)
                if (eSignature && !transData.isTxnSmallAmt) {
                    gotoState(State.SIGNATURE.toString())
                } else {
                    gotoState(State.PRINT.toString())
                }
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData)
    }

    fun goTipBranch() {

        transData.acquirer?.let { acq->
            /*
               Not support PIN Bypass, So customer may do CVM as the same
            */
            if (acq.isEnableUpi
                && !acq.isTestMode
                && transData.issuer != null
                && (transData.issuer.name.equals(ISSUER_UP)
                        || transData.issuer.name.equals(ISSUER_TBA))) {
                gotoState(State.ENTER_PIN.toString())
            } else {
                gotoState(State.MAG_ONLINE.toString())
            }
        }
    }


    private fun showMsgDialog( context: Context, msg: String) {
        val dialog = CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE)
        dialog.setCancelClickListener(object: CustomAlertDialog.OnCustomClickListener{
            override fun onClick(alertDialog: CustomAlertDialog?) {
                dialog.dismiss()
                transEnd(ActionResult(TransResult.ERR_USER_CANCEL, null))
            }
        })
        dialog.setConfirmClickListener(object: CustomAlertDialog.OnCustomClickListener{
            override fun onClick(alertDialog: CustomAlertDialog?) {
                dialog.dismiss()
                gotoState(State.CHECK_CARD.toString())
            }
        })
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show()
        dialog.setNormalText(msg)
        dialog.showCancelButton(true)
        dialog.showConfirmButton(true)
        dialog.setOnDismissListener(object: DialogInterface.OnDismissListener{
            override fun onDismiss(p0: DialogInterface?) {
                dialog.dismiss()
            }
        })
    }


    fun isDebitCardScheme(mode: enumCardMode) : Boolean {
        try {
            var aid : String? = null
            var aidByte : ByteArray? = null
            when (mode) {
                enumCardMode.ICC    -> { aidByte = emv?.getTlv(0x4F) }
                enumCardMode.CLSS   -> { aidByte = clss?.getTlv(0x4F) }
                enumCardMode.SP200  -> { aidByte = emvSP200?.aid?.toByteArrayOfHexString() }
            }
            aidByte?.let {
                aid = it.toHexString()
                return (DEBIT1 == aid || DEBIT2 == aid || TPN_DEBIT == aid )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun isIssuerSupportPreAuth() : Boolean {
        transData?.let {
            it.issuer?.let {
                return it.isAllowPreAuth
            }
        }

        return false
    }
}






















