package com.pax.pay.trans

import android.content.Context
import com.pax.abl.core.ActionResult
import com.pax.abl.utils.EncUtils
import com.pax.abl.utils.PanUtils
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.eemv.utils.Tools
import com.pax.pay.ECR.EcrData
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.*
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransData.ETransStatus
import com.pax.pay.trans.task.PrintTask
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TimeConverter
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import java.util.*
import kotlin.collections.ArrayList

class TipAdjustTrans(context: Context?, transListener: TransEndListener?) : BaseTrans(context, ETransType.ADJUST, transListener) {
    private var origTransNo: Long = 0L
    private var isNeedInputTransNo: Boolean = true
    private var origTransData: TransData? = null
    private var supportAcquirers: ArrayList<Acquirer>? = null

    constructor(context: Context?, transListener: TransEndListener?, origTransNo: Long): this(context, transListener) {
        this.origTransNo = origTransNo
        isNeedInputTransNo = false
    }

    private fun ermErrorExceedCheck() {
        if (FinancialApplication.getTransDataDbHelper().findCountTransDataWithEReceiptUploadStatus(true) >= 30) {
            transEnd(ActionResult(TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR, null))
            return
        }
    }

    enum class State {
        INPUT_PWD, ENTER_TRANSNO, TRANS_DETAIL, ENTER_AMOUNT, SIGNATURE, PRINT
    }

    override fun bindStateOnAction() {
        //input manager password
        val inputPasswordAction = ActionInputPassword { action ->
            (action as ActionInputPassword).setParam(
                currentContext, 6,
                getString(R.string.prompt_tip_adjust_pwd), null
            )
        }
        bind(State.INPUT_PWD.toString(), inputPasswordAction)

        //input original trance no
        val enterTransNoAction = ActionInputTransData { action ->
            //todo support stan no. ??
            (action as ActionInputTransData).setParam(
                currentContext,
                getString(R.string.menu_adjust)
            ).setInputLine(
                getString(R.string.prompt_input_transno),
                ActionInputTransData.EInputType.NUM,
                6,
                true
            )
        }
        bind(State.ENTER_TRANSNO.toString(), enterTransNoAction, true)

        //Display original trans detail
        val confirmInfoAction = ActionDispTransDetail { action ->
            val map = linkedMapOf<String, String>()
            mapCreditDetails(map)

            (action as ActionDispTransDetail).setParam(
                currentContext,
                getString(R.string.menu_adjust),
                map
            )
        }
        bind(State.TRANS_DETAIL.toString(), confirmInfoAction, true)

        //Enter Amount + Tip Amount
        val amountAction = ActionEnterAmount { action ->
            (action as ActionEnterAmount).setParam(
                currentContext,
                getString(R.string.menu_adjust), true, origTransData
            )
        }
        bind(State.ENTER_AMOUNT.toString(), amountAction, true)

        val signatureAction = ActionSignature { action ->
            (action as ActionSignature).setParam(
                currentContext, transData!!.amount,
                !Component.isAllowSignatureUpload(transData)
            )
        }
        bind(State.SIGNATURE.toString(), signatureAction)

        val printTask = PrintTask(
            currentContext,
            transData,
            PrintTask.genTransEndListener(this@TipAdjustTrans, State.PRINT.toString())
        )
        bind(State.PRINT.toString(), printTask)

        // ERM Maximum Exceed Transaction check
        ermErrorExceedCheck()

        this.getSupportAcquirers()

        when {
            FinancialApplication.getSysParam()[SysParam.BooleanParam.OTHTC_VERIFY] && isNeedInputTransNo -> {
                gotoState(State.INPUT_PWD.toString())
            }
            isNeedInputTransNo -> {
                gotoState(State.ENTER_TRANSNO.toString())
            }
            else -> {
                //todo handle for amex if required
                validateOrigTransData(origTransNo)
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
                    onInputPwd(result)
                }
            }
            State.ENTER_TRANSNO -> {
                onEnterTransNo(result!!)
            }
            State.TRANS_DETAIL -> {
                gotoState(State.ENTER_AMOUNT.toString())
            }
            State.ENTER_AMOUNT -> {
                onEnterAmount(result!!)
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
        if (data != FinancialApplication.getSysParam()[SysParam.StringParam.SEC_TIP_ADJUSTMENT_PWD]) {
            EcrData.instance.isOnHomeScreen = true
            transEnd(ActionResult(TransResult.ERR_PASSWORD, null))
            return
        }
        if (isNeedInputTransNo) { // need to input trans no.
            gotoState(State.ENTER_TRANSNO.toString())
        } else { // not need to input trans no.
            //todo handle for amex if required
            validateOrigTransData(origTransNo)
        }
    }

    private fun onEnterTransNo(result: ActionResult) {
        val content = result.data as String?
//        multiAppLastTrans = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData()//todo handle for amex if required
        var transNo: Long = -1
        if (content == null) {
            val transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqsAndMerchant(supportAcquirers)
            transData?.let {
                transNo = transData.traceNo
//                if (FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND]) {//todo support stan no.
//                    transNo = transData.stanNo
//                }
            }
            //todo handle for amex if required
//            if (multiAppLastTrans != null) {
//                if (FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND]) {
//                    if (transNo < multiAppLastTrans.getStanNo()) {
//                        goToVoidTransByMultiApp(multiAppLastTrans, multiAppLastTrans.getStanNo())
//                        return
//                    }
//                } else {
//                    if (transNo < multiAppLastTrans.getTraceNo()) {
//                        goToVoidTransByMultiApp(multiAppLastTrans, multiAppLastTrans.getTraceNo())
//                        return
//                    }
//                }
//            }
        } else {
            transNo = Utils.parseLongSafe(content, -1)
            origTransNo = transNo
        }
        validateOrigTransData(transNo)
    }

    private fun validateOrigTransData(origTransNo: Long) {
        if (origTransNo <= 0) {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
            return
        }

        val excludeTrans = listOf(ETransType.PREAUTH, ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION)
        origTransData = /*if (FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND]) {//todo support stan no.
            FinancialApplication.getTransDataDbHelper().findTransDataByStanNoAndAcqs(origTransNo, supportAcquirers)
        } else {*/
            FinancialApplication.getTransDataDbHelper().findTransDataByTraceNoAndAcqs(origTransNo, supportAcquirers, excludeTrans)
//        }

        if (origTransData == null) {
            // trans not exist
//            val transMultiAppData: TransMultiAppData? //todo handle for amex if required
//            transMultiAppData =
//                if (FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND]) {
//                    FinancialApplication.getTransMultiAppDataDbHelper()
//                        .findTransDataByStanNo(origTransNo)
//                } else {
//                    FinancialApplication.getTransMultiAppDataDbHelper()
//                        .findTransDataByTraceNo(origTransNo)
//                }
//            if (transMultiAppData != null) {
//                goToVoidTransByMultiApp(transMultiAppData, origTransNo)
//            } else {
                transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
//            }
            return
        }

        if (isSettleFail()) {
            // Last settlement not success, need to settle firstly
            transEnd(ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null))
            return
        }

        origTransData?.let {
            val trType = it.transType

            if (!FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_TIP_ADJUST]
                || !it.issuer.isEnableAdjust || !trType.isAdjustAllowed) {
                transEnd(ActionResult(TransResult.ERR_ADJUST_UNSUPPORTED, null))
                return
            }

            //  has voided transaction can not adjust
            val trStatus = origTransData!!.transState
            if (trStatus == ETransStatus.VOIDED || trType === ETransType.VOID) {
                transEnd(ActionResult(TransResult.ERR_HAS_VOIDED, null))
                return
            }

            //  has adjusted
            if (trStatus == ETransStatus.ADJUSTED || trType === ETransType.ADJUST) {
                transEnd(ActionResult(TransResult.ERR_HAS_ADJUSTED, null))
                return
            }

            copyOrigTransData()
            gotoState(State.TRANS_DETAIL.toString())
        } ?: run {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
        }
    }

    private fun onEnterAmount(result: ActionResult) {
        var newTotalAmount: Long = 0L
        var tipAmount: Long = 0L
        var newDccTotalAmount: Long = 0L
        var dccTipAmount: Long = 0L

        val tipInformation = result.data as ActionEnterAmount.TipInformation?
        tipInformation?.let {
            newTotalAmount = it.newBaseAmount
            tipAmount = it.tipAmount
            newDccTotalAmount = it.newDccBaseAmount
            dccTipAmount = it.dccTipAmount
        } ?: run {
            newTotalAmount = result.data as Long
            tipAmount = result.data1 as Long
        }

        origTransData?.apply orig@ {
            transData.apply {
                val origAmount = this@orig.amount
                val origDccAmount = this@orig.dccAmount

                //base amount and tip
                if (this.isDccRequired) {
                    this.amount = newTotalAmount.toString(10)
                    this.tipAmount = tipAmount.toString(10)
                    this.dccAmount = newDccTotalAmount.toString(10)
                    this.dccTipAmount = dccTipAmount.toString(10)
                } else {
                    this.amount = newTotalAmount.toString(10)
                    this.tipAmount = tipAmount.toString(10)
                }

                // update original transaction record
                //handle stan no.
                if (this@orig.offlineSendState == null || this@orig.offlineSendState == TransData.OfflineStatus.OFFLINE_SENT) {
                    this.origStanNo = this@orig.stanNo
                    this.stanNo = FinancialApplication.getSysParam()[SysParam.NumberParam.EDC_STAN_NO].toLong() //todo
                    Component.incStanNo(transData)
                }

                this.origAmount = origAmount
                this.origDccAmount = origDccAmount
                this.origAuthCode = this.authCode
                this.isOnlineTrans = false
                this.origDateTime = transData.dateTime
                this.offlineSendState = TransData.OfflineStatus.OFFLINE_NOT_SENT
                this.reversalStatus = TransData.ReversalStatus.NORMAL
                Component.chkTxnIsSmallAmt(this)
                FinancialApplication.getTransDataDbHelper().insertTransData(this)

                if (this@orig.transType == ETransType.OFFLINE_TRANS_SEND ||
                        ((this@orig.transType == ETransType.SALE || this@orig.transType == ETransType.SALE_COMPLETION) && this@orig.offlineSendState != null)) {
                    this@orig.offlineSendState = TransData.OfflineStatus.OFFLINE_ADJUSTED
                }
                //set status as adjusted
                this@orig.transState = ETransStatus.ADJUSTED
//                this@orig.offlineSendState = null
//                this@orig.isOnlineTrans = true
                this@orig.origAmount = origAmount
                this@orig.origDccAmount = origDccAmount
                this@orig.amount = this.amount
                this@orig.adjustedAmount = this.amount
                this@orig.adjustedDccAmount = this.dccAmount
                this@orig.tipAmount = this.tipAmount
                this@orig.dccTipAmount = this.dccTipAmount
                this@orig.isTxnSmallAmt = this.isTxnSmallAmt
                this@orig.numSlipSmallAmt = this.numSlipSmallAmt
                FinancialApplication.getTransDataDbHelper().updateTransData(this@orig)

                toSignOrPrint()
            }
        } ?: run {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
        }
    }

    // need electronic signature or send
    private fun toSignOrPrint() {
        if (transData.isPinVerifyMsg || !transData.isOnlineTrans && transData.isHasPin) {
            gotoState(State.PRINT.toString())
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
                gotoState(State.PRINT.toString())
            } else {
                val eSignature = FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE]
                if (eSignature && !transData.isTxnSmallAmt) {
                    gotoState(State.SIGNATURE.toString())
                } else {
                    gotoState(State.PRINT.toString()) // Skip SIGNATURE process
                }
            }
        }
    }

    private fun onSignature(result: ActionResult) {
        // save signature data
        val signData = result.data as ByteArray?
        val signPath = result.data1 as ByteArray?

        signData?.let {
            if (it.isNotEmpty()) {
                origTransData!!.signData = it
                origTransData!!.signPath = signPath
                transData!!.signData = it
                transData!!.signPath = signPath
                // update trans data，save signature
                FinancialApplication.getTransDataDbHelper().updateTransData(origTransData)
                FinancialApplication.getTransDataDbHelper().updateTransData(transData)
            }
        }

        // if terminal does not support signature ,card holder does not sign or time out，print preview directly.
        gotoState(State.PRINT.toString())
    }

    // set original trans data
    private fun copyOrigTransData() {
        origTransData?.let {
            val acquirer = it.acquirer
            FinancialApplication.getAcqManager().curAcq = acquirer
            Component.transInit(transData, acquirer)
            transData.apply {
                this.amount = it.amount
                this.origBatchNo = it.batchNo
                this.origAuthCode = it.authCode
                this.origRefNo = it.refNo
                this.origTransNo = it.traceNo
                this.pan = it.pan
                this.expDate = it.expDate
                this.acquirer = acquirer
                this.issuer = it.issuer
                this.cardSerialNo = it.cardSerialNo
                this.sendIccData = it.sendIccData
                this.origTransType = it.transType
                this.enterMode = it.enterMode
                this.aid = it.aid
                this.tvr = it.tvr
                this.tc = it.tc
                this.emvAppLabel = it.emvAppLabel
                this.traceNo = it.traceNo
                this.isTxnSmallAmt = it.isTxnSmallAmt //EDCBBLAND-426 support small amount
                this.numSlipSmallAmt = it.numSlipSmallAmt //EDCBBLAND-426 support small amount
                this.isPinVerifyMsg = it.isPinVerifyMsg //EDCBBLAND-467 show pin verify msg on receipt if original trans. have Offline/Online PIN.
                this.isSignFree = it.isSignFree //EDCBBLAND-467 remove signature part on receipt if original trans. have Offline/Online PIN.
                this.dateTime = it.dateTime //EDCBBLAND-604 Fix issue send incorrect datetime
                this.origDateTime = it.dateTime
                this.refNo = it.refNo
                this.track1 = it.track1
                this.branchID = it.branchID
                this.offlineSendState = it.offlineSendState
                this.posNo_ReceiptNo = it.posNo_ReceiptNo
                this.cashierName = it.cashierName
                this.authCode = it.authCode
                this.isHasPin = it.isHasPin
            }
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

    private fun isSettleFail(): Boolean {
        var acqName: String? = null
        origTransData?.let {
            acqName = it.acquirer.name
        }
        return Component.chkSettlementStatus(acqName)
    }

    private fun mapCreditDetails(map: LinkedHashMap<String, String>): LinkedHashMap<String, String> {
        origTransData?.let {
            val transType: String = it.transType.transName
            var amount = CurrencyConverter.convert(
                Utils.parseLongSafe(it.amount, 0), it.currency
            )
            transData?.apply {
                this.enterMode = it.enterMode
                this.track1 = it.track1
                this.track2 = it.track2
                this.track3 = it.track3
                this.cardSerialNo = it.cardSerialNo
                this.sendIccData = it.sendIccData
                this.dupIccData = it.dupIccData
                this.emvAppName = it.emvAppName
                this.emvAppLabel = it.emvAppLabel
                this.aid = it.aid
                this.tvr = it.tvr
                this.tc = it.tc
                this.traceNo = it.traceNo
                this.isDccRequired = it.isDccRequired
                this.dccAmount = it.dccAmount
                this.dccConversionRate = it.dccConversionRate
                this.dccCurrencyCode = it.dccCurrencyCode
                this.dccCurrencyName = it.dccCurrencyName
                this.field63Byte = it.field63Byte
            }

            // date and time
            val formattedDate = TimeConverter.convert(
                it.dateTime, Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY
            )
            map[getString(R.string.history_detail_type)] = transType
            map[getString(R.string.history_detail_amount)] = amount

            if (it.isDccRequired) {
                val currencyNumeric = Tools.bytes2String(it.dccCurrencyCode)
                amount = CurrencyConverter.convert(
                    Utils.parseLongSafe(it.dccAmount, 0), currencyNumeric
                )
                val exRate: Double = if (it.dccConversionRate != null) it.dccConversionRate.toDouble() / 10000 else 0.0
                map[getString(R.string.history_detail_dcc_ex_rate)] = String.format(Locale.getDefault(), "%.4f", exRate)
                map[Utils.getString(R.string.history_detail_dcc_amount, CurrencyConverter.getCurrencySymbol(currencyNumeric, false))] = amount
            }

            map[getString(R.string.history_detail_card_no)] = PanUtils.maskCardNo(it.pan, it.issuer.panMaskPattern)
            map[getString(R.string.history_detail_auth_code)] = it.authCode
            map[getString(R.string.history_detail_ref_no)] = it.refNo ?: ""
            map[getString(R.string.history_detail_stan_no)] = Component.getPaddedNumber(it.stanNo, 6)
            map[getString(R.string.history_detail_trace_no)] = Component.getPaddedNumber(it.traceNo, 6)
            map[getString(R.string.dateTime)] = formattedDate
        }
        return map
    }
}