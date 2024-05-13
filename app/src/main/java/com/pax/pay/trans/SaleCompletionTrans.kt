package com.pax.pay.trans

import android.content.Context
import android.content.DialogInterface
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.emv.EmvTags
import com.pax.pay.trans.action.*
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.task.PrintTask
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils

class SaleCompletionTrans(val context: Context, val transType: ETransType, val transEndListener: TransEndListener) : BaseTrans(context, transType, transEndListener) {

    enum class State {
        INPUT_TRANS_NO,
        DISP_TRANS_DETAIL,
        INPUT_AMOUNT,
        DCC_GET_RATE,
        DCC_CONFIRM,
        SAVE_OFFLINE_TRANS,
        SIGNATURE,
        PRINT
    }

    var isVoidWithSTAN : Boolean = false
    var supportLastTransAcquirerList : ArrayList<Acquirer>
    var dccGetRateTransData : TransData? = null
    var originalTransData : TransData? = null
    var originalBasedAmount : Long = 0L
    var originalMaxSaleAmount : Long = 0L
    var saleCompPercent : Float = 0F
    var saleCompInputAmount : Long = 0L
    private var supportAcquirers: ArrayList<Acquirer>? = null

    init {
        // only 2 hosts support for KBank's PreAuthorization
        supportLastTransAcquirerList = arrayListOf( FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK) ,
                                                    FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP) ,
                                                    FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX))

        isVoidWithSTAN = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)
        saleCompPercent = (FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_MAX_PERCENTAGE_SALE_COMPLETION) / 100F)
    }

    fun containAcquirerName(acqName: String) : Boolean {
        for (acq in supportLastTransAcquirerList) {
            if (acq.toString().equals(acqName)) {
                return true
            }
        }

        return false
    }

    override fun bindStateOnAction() {
        val inputTransNoAction = ActionInputTransData( object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                val dispMsg : String = if(isVoidWithSTAN) {getString(R.string.prompt_input_stanno)} else getString(
                    R.string.prompt_input_transno)
                (action as ActionInputTransData).setParam(currentContext, getString(R.string.menu_sale_completion)).setInputLine(dispMsg,
                    ActionInputTransData.EInputType.NUM, 6, false)
            }
        })
        bind(State.INPUT_TRANS_NO.toString(), inputTransNoAction, true)


        val dispTransDetailAction = ActionDispTransDetail(object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                originalTransData?.let {
                    val map : LinkedHashMap<String, String> = PreAuthCancellationTrans.mapPreAuthDetails(currentContext, it)
                    ((action) as ActionDispTransDetail).setParam(currentContext, "${getString(R.string.menu_sale_completion)}", map)
                }?:run{
                    transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS,null))
                }
            }
        })
        bind(State.DISP_TRANS_DETAIL.toString(), dispTransDetailAction, false)


        val enterAmountAction = ActionEnterAmount(object: AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                ((action) as ActionEnterAmount).setParam(currentContext, getString(R.string.menu_sale_completion), false)
            }
        })
        bind(State.INPUT_AMOUNT.toString(), enterAmountAction, false)


        val dccGetRateAction = ActionTransOnline(object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionTransOnline).setParam(currentContext, transData)
            }
        })
        bind(State.DCC_GET_RATE.toString(), dccGetRateAction, false)


        val dccConfirmAction = ActionConfirmDCC(object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                (action as ActionConfirmDCC).setParam(currentContext, getString(R.string.menu_sale_completion), transData)
            }
        })
        bind(State.DCC_CONFIRM.toString(), dccConfirmAction, false)

        val signatureAction = ActionSignature(object : AAction.ActionStartListener {
            override fun onStart(action: AAction?) {
                (action as ActionSignature).setParam(currentContext, transData.amount)
            }
        })
        bind(State.SIGNATURE.toString(), signatureAction)

        val printAction = PrintTask(currentContext, transData, PrintTask.genTransEndListener(this@SaleCompletionTrans, State.PRINT.toString()))
        bind(State.PRINT.toString(), printAction)

        this.getSupportAcquirers()

        gotoState(State.INPUT_TRANS_NO.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        result?.let {
            when (State.valueOf(currentState!!)) {
                State.INPUT_TRANS_NO            -> { afterInputTransNumber(it) }
                State.DISP_TRANS_DETAIL         -> { afterConfirmTransDetail(it) }
                State.INPUT_AMOUNT              -> { afterInputAmount(it)}
                State.DCC_GET_RATE              -> { afterDccGetRate(it)}
                State.DCC_CONFIRM               -> { afterDccConfirmed(it)}
                State.SIGNATURE                 -> { afterSignature(it) }
                State.PRINT                     -> { afterPrinted(it) }
            }
        }?:run{
            transEnd(ActionResult(TransResult.ERR_MISSING_INTERNAL_PROC_RESULT,null))
        }
    }

    private fun getSupportAcquirers() {
        val acqManager = FinancialApplication.getAcqManager()
        val acqs: ArrayList<Acquirer> = ArrayList()
        acqs.add(acqManager.findAcquirer(Constants.ACQ_KBANK))
//        acqs.add(acqManager.findAcquirer(Constants.ACQ_AMEX)) //todo wait for confirmation
        acqs.add(acqManager.findAcquirer(Constants.ACQ_UP))
        acqs.add(acqManager.findAcquirer(Constants.ACQ_DCC))
        supportAcquirers = acqs
    }


    fun afterInputTransNumber(result:ActionResult) {
        /*  ====================================================================================
                LIST OF SUPPORTED CARD
            ====================================================================================
              INTERNATIONAL CARD BRAND
                    VISA            : UNSUPPORTED
                    MASTERCARD      : UNSUPPORTED
                    JCB             : UNSUPPORTED
                    AMEX            : UNSUPPORTED   ** this separate as multi-application **
                    DINER           : NOT APPLICABLE
                    DCI             : NOT APPLICABLE
                    UPI             : SUPPORTED  (only Credit scheme)
            ====================================================================================
               LOCAL CARD BRAND
                    TPN                     : UNSUPPORTED
                    OTHER DEBIT CARD SCHEME : UNSUPPORTED
            ==================================================================================== */

        try {
            // from supported card list --- we dont need to check on multi-application
            var transNo : Long = -1

            val content = result.data as String?

            if (content == null) {
                val transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqsAndMerchant(supportAcquirers)
                transData?.let {
                    transNo = transData.traceNo
                    if (isVoidWithSTAN) {
                        transNo = transData.stanNo
                    }
                }
            } else {
                transNo = Utils.parseLongSafe(content, -1)
                transData.origTransNo = transNo
            }

            validateOriginalTrans(transNo)
            return;
        } catch (e: Exception) {
            e.printStackTrace()
        }
        transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS,null))
    }

    fun validateOriginalTrans(originalTransNo: Long) {

        // in case send default originalTransNo = -1
        if (originalTransNo <= 0) {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
            return
        }

        // load original trans. by support VoidWithStan
        if (isVoidWithSTAN) {
            originalTransData = FinancialApplication.getTransDataDbHelper().findTransDataByStanNoAndAcqs(originalTransNo, supportLastTransAcquirerList)
        } else {
            originalTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNoAndAcqs(originalTransNo, supportLastTransAcquirerList)
        }


        originalTransData?.let {
            // >> Incase we found the original transaction


            // 1. This section dont allow do any transaction on Locked-Batch-Host
            if (isSettleFail(it)) {
                transEnd(ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null))
                return
            }

            // 2. This section allow only pre-authorization trans
            if (!it.transType.equals(ETransType.PREAUTHORIZATION)) {
                transEnd(ActionResult(TransResult.ERR_SALE_COMPLETE_UNSUPPORTED, null))
                return
            }

            /* 3. This section allow only TPN host
                    SUPPORTED CARD
                        1. UPI - CREDIT     : SUPPORTED
                        2. UPI - DEBIT      : UNSUPPORTED
                        3. TPN - DEBIT      : UNSUPPORTED
             */
            if (containAcquirerName(it.acquirer.name)) {
                transEnd(ActionResult(TransResult.ERR_SALE_COMPLETE_UNSUPPORTED, null))
                return
            }

            // 4. This section dont allow to use original transaction with status VOIDED
            if (it.transState==TransData.ETransStatus.VOIDED) {
                transEnd(ActionResult(TransResult.ERR_HAS_VOIDED, null))
                return
            }

            // don't allow original transaction with status SALE_COMPLETED and ADJUSTED (for adjust assume that original state is sale completed)
            if (it.transState==TransData.ETransStatus.SALE_COMPLETED || it.transState==TransData.ETransStatus.ADJUSTED) {
                transEnd(ActionResult(TransResult.ERR_HAS_SALE_COMPLETED, null))
                return
            }

            // set trans.state & trans.type
//            transData.transState = it.transState
//            transData.transType = ETransType.PREAUTHORIZATION_CANCELLATION

            // duplicateOriginalTransData
            copyOrigTransData(it)

            gotoState(PreAuthCancellationTrans.State.DISP_TRANS_DETAIL.toString())

        }?:run {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
        }
    }

    fun isSettleFail(oriTransData: TransData) : Boolean {
        oriTransData.acquirer?.let {
            return Component.chkSettlementStatus(it.name)
        }
        return false;
    }

    fun afterConfirmTransDetail(result:ActionResult) {
        if(result.ret == TransResult.SUCC) {
            gotoState(State.INPUT_AMOUNT.toString())
        } else {
            transEnd(result)
        }
    }


    fun afterInputAmount(result:ActionResult) {
        if (result.ret.equals(TransResult.SUCC)) {
            saleCompInputAmount = result.data as Long
            if(saleCompInputAmount > originalMaxSaleAmount) {
                //transEnd(ActionResult(TransResult.ERR_SALE_COMP_TRANS_AMOUNT_EXCEED,null))
                val dismissListener = object: DialogInterface.OnDismissListener {
                    override fun onDismiss(p0: DialogInterface?) {
                        gotoState(State.INPUT_AMOUNT.toString())
                    }
                }
                DialogUtils.showErrMessage(currentContext, "Input Amount", getString(R.string.err_salecomp_trans_amount_exceed), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME)
                return
            }

            val dccAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC)
            if (originalTransData!!.isDccRequired && dccAcq.isEnable) {
                dccGetRateTransData = TransData(transData)
                FinancialApplication.getAcqManager().curAcq = dccAcq
                Component.transInit(dccGetRateTransData, dccAcq)

                dccGetRateTransData?.apply {
                    this.transType = ETransType.KBANK_DCC_GET_RATE
                    val f55 = EmvTags.getF55(emv, transType, false, this.pan)
                    this.sendIccData = Utils.bcd2Str(f55)
                }
                gotoState(State.DCC_GET_RATE.toString())
            } else {
                transData.amount = saleCompInputAmount.toString()
                saveSaleCompletionTransaction()
            }
        } else {
            transEnd(result)
        }
    }

    fun afterDccGetRate(result : ActionResult) {
        if (result.ret != TransResult.SUCC) {
            goBackToNormalSale()
        } else {
            transData.dccAmount = dccGetRateTransData!!.dccAmount
            transData.dccConversionRate = dccGetRateTransData!!.dccConversionRate
            transData.dccCurrencyCode = dccGetRateTransData!!.dccCurrencyCode
            transData.field63Byte = dccGetRateTransData!!.field63RecByte
            gotoState(State.DCC_CONFIRM.toString())
        }
    }

    fun afterDccConfirmed(result : ActionResult) {
        val isDcc = result.data as Boolean
        if (isDcc) { transData.amount = transData.dccAmount }
        saveSaleCompletionTransaction()
    }

    fun goBackToNormalSale() {
        transData.isDccRequired = false
        transData.amount = saleCompInputAmount.toString()
        FinancialApplication.getAcqManager().curAcq = transData.acquirer
        saveSaleCompletionTransaction()
    }

    fun afterSignature(result: ActionResult) {
        var signData : ByteArray? = if (result.data  is ByteArray) { result.data  as ByteArray } else {null}
        var signPath : ByteArray? = if (result.data1 is ByteArray) { result.data1 as ByteArray } else {null}

        signData?.let {
            if (it.size > 0) {
                transData.signData = signData
                transData.signPath = signPath

                FinancialApplication.getTransDataDbHelper().updateTransData(transData)
            }
        }

        gotoState(State.PRINT.toString())
    }

    fun afterPrinted(result: ActionResult) {
        if (result.ret.equals(TransResult.SUCC)) {
            transEnd(result)
        }
        else {
            dispResult(transType.transName, result, null)
            gotoState(State.PRINT.toString())
        }
    }

    fun saveSaleCompletionTransaction() {
        transData.transType = ETransType.SALE_COMPLETION
        transData.transState = TransData.ETransStatus.NORMAL
        // save trans data
        Component.saveOfflineTransNormalSale(transData)

        originalTransData!!.transState = TransData.ETransStatus.SALE_COMPLETED

        FinancialApplication.getTransDataDbHelper().updateTransData(originalTransData)

        toSignOrPrint()
    }

    // need electronic signature or send
    private fun toSignOrPrint() {
        if (transData.isPinVerifyMsg || !transData.isOnlineTrans && transData.isHasPin) {
            gotoState(TipAdjustTrans.State.PRINT.toString())
        } else {
            if (transData.enterMode == TransData.EnterMode.CLSS || transData.enterMode == TransData.EnterMode.SP200) {
                val aidInfo = FinancialApplication.getEmvDbHelper().findAID(transData.aid)
                aidInfo?.let {
                    if (Utils.parseLongSafe(transData.amount, 0) > (it.rdCVMLmt - 1)) {
                        transData.isSignFree = false
                    }
                }
            }

            if (transData.isSignFree) { // signature free
                gotoState(TipAdjustTrans.State.PRINT.toString())
            } else {
                val eSignature = FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE]
                if (eSignature && !transData.isTxnSmallAmt) {
                    gotoState(TipAdjustTrans.State.SIGNATURE.toString())
                } else {
                    gotoState(TipAdjustTrans.State.PRINT.toString()) // Skip SIGNATURE process
                }
            }
        }
    }

    private fun copyOrigTransData(origTransData: TransData) {
        val acquirer: Acquirer = origTransData.getAcquirer()
        FinancialApplication.getAcqManager().curAcq = acquirer
        Component.transInit(transData, acquirer)
        transData.amount = origTransData.getAmount()
        transData.origBatchNo = origTransData.getBatchNo()
        transData.origAuthCode = origTransData.getAuthCode()
        transData.origRefNo = origTransData.getRefNo()
        transData.origTransNo = origTransData.getTraceNo()
        transData.pan = origTransData.getPan()
        transData.expDate = origTransData.getExpDate()
        transData.acquirer = acquirer
        transData.issuer = origTransData.getIssuer()
        transData.cardSerialNo = origTransData.getCardSerialNo()
        transData.sendIccData = origTransData.getSendIccData()
        transData.origTransType = origTransData.getTransType()
        transData.enterMode = origTransData.getEnterMode()
        transData.aid = origTransData.getAid()
        transData.tvr = origTransData.getTvr()
        transData.tc = origTransData.getTc()
        transData.emvAppLabel = origTransData.getEmvAppLabel()
        transData.traceNo = origTransData.getTraceNo()
        transData.isTxnSmallAmt = origTransData.isTxnSmallAmt()
        transData.numSlipSmallAmt = origTransData.getNumSlipSmallAmt()
        transData.isPinVerifyMsg = origTransData.isPinVerifyMsg()
        transData.isSignFree = origTransData.isSignFree()
        transData.dateTime = origTransData.getDateTime()
        transData.origDateTime = origTransData.getDateTime()
        transData.refNo = origTransData.getRefNo()
        transData.track1 = origTransData.getTrack1()
        transData.branchID = origTransData.getBranchID()
        transData.offlineSendState = origTransData.getOfflineSendState()
        transData.track2 = origTransData.getTrack2()
        transData.authCode = origTransData.getAuthCode()

        // Extra for SaleCompletion
        transData.transType = ETransType.SALE_COMPLETION
        originalBasedAmount   = Utils.parseLongSafe(origTransData.getAmount(), 0L)
        originalMaxSaleAmount = (originalBasedAmount * ( 1F + saleCompPercent)).toLong()
    }
}