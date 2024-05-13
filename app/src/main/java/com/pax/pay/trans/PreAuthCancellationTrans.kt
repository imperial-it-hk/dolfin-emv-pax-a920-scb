package com.pax.pay.trans

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.abl.utils.PanUtils
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.ActionDispTransDetail
import com.pax.pay.trans.action.ActionInputTransData
import com.pax.pay.trans.action.ActionInputTransData.EInputType
import com.pax.pay.trans.action.ActionTransOnline
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransMultiAppData
import com.pax.pay.trans.task.PrintTask
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TimeConverter
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import th.co.bkkps.amexapi.AmexTransAPI

class PreAuthCancellationTrans(val context: Context, val transType: ETransType, val transEndListener: TransEndListener) : BaseTrans(context, transType, transEndListener) {

    companion object{
        fun mapPreAuthDetails(context: Context, origTrans: TransData) : LinkedHashMap<String,String>{
            var map : LinkedHashMap<String,String> = LinkedHashMap<String, String>()
            val transFormattedDate  = TimeConverter.convert(origTrans.dateTime, Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY)
            val transType           = origTrans.transType.name
            val transAmount         = CurrencyConverter.convert(Utils.parseLongSafe(origTrans.amount, 0L))
            val maskedCardNo        = PanUtils.maskCardNo(origTrans.pan, origTrans.issuer.panMaskPattern)
            val transTraceNo        = Component.getPaddedNumber(origTrans.traceNo, 6)
            val transStanNo         = Component.getPaddedNumber(origTrans.stanNo, 6)
            val transAuthCode       = origTrans.authCode
            val transRefNo          = origTrans.refNo

            map.put(context.getString(R.string.history_detail_type), transType)
            map.put(context.getString(R.string.history_detail_amount), transAmount)
            map.put(context.getString(R.string.history_detail_card_no), maskedCardNo)
            map.put(context.getString(R.string.history_detail_auth_code), transAuthCode)
            map.put(context.getString(R.string.history_detail_ref_no), transRefNo)
            map.put(context.getString(R.string.history_detail_stan_no), transStanNo)
            map.put(context.getString(R.string.history_detail_trace_no), transTraceNo)
            map.put(context.getString(R.string.dateTime), transFormattedDate)
            return map
        }
    }

    var supportLastTransAcquirerList : ArrayList<Acquirer>
    var originalTransData : TransData? = null
    var multiAppLastTrans : TransMultiAppData? = null
    var isVoidWithSTAN : Boolean = false

    var originalTransNo : Long? = -1
    private var supportAcquirers: ArrayList<Acquirer>? = null

    enum class State {
        INPUT_TRANS_NO,
        DISP_TRANS_DETAIL,
        AMEX_API,
        MAG_ONLINE,
        OFFLINE_SEND,
        SIGNATURE,
        PRINT
    }

    init {
        // only 2 hosts support for KBank's PreAuthorization
        supportLastTransAcquirerList = arrayListOf( FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK) ,
                                                    FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP),
                                                    FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX))
        isVoidWithSTAN = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)
    }

    override fun bindStateOnAction() {
        val inputTransNoAction = ActionInputTransData( object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                val dispMsg : String = if(isVoidWithSTAN) {getString(R.string.prompt_input_stanno)} else getString(R.string.prompt_input_transno)
                (action as ActionInputTransData).setParam(currentContext, getString(R.string.menu_preauth_cancel)).setInputLine(dispMsg,EInputType.NUM, 6, false)
            }
        })
        bind(State.INPUT_TRANS_NO.toString(), inputTransNoAction, true)


        val dispTransDetailAction = ActionDispTransDetail(object : AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                originalTransData?.let {
                    val map : LinkedHashMap<String, String> = mapPreAuthDetails(currentContext, it)
                    ((action) as ActionDispTransDetail).setParam(currentContext, getString(R.string.menu_preauth_cancel), map)
                }?:run{
                    transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS,null))
                }
            }
        })
        bind(State.DISP_TRANS_DETAIL.toString(), dispTransDetailAction, false)


        val magTransOnlineAction = ActionTransOnline(object: AAction.ActionStartListener{
            override fun onStart(action: AAction?) {
                ((action) as ActionTransOnline).setParam(currentContext, transData)
            }
        })
        bind(State.MAG_ONLINE.toString(), magTransOnlineAction, false)


        val printAction = PrintTask(currentContext, transData, PrintTask.genTransEndListener(this@PreAuthCancellationTrans, State.PRINT.toString()))
        bind(State.PRINT.toString(), printAction)

        this.getSupportAcquirers()

        gotoState(State.INPUT_TRANS_NO.toString())
    }

    override fun onActionResult(currentState: String?, result: ActionResult?) {
        result?.let {
            when (State.valueOf(currentState!!)) {
                State.INPUT_TRANS_NO            -> { afterInputTransNumber(it) }
                State.DISP_TRANS_DETAIL         -> { afterConfirmTransDetail(it) }
                State.MAG_ONLINE                -> { afterMagOnline(it) }
                State.OFFLINE_SEND              -> { afterOfflineSent(it)}
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
                transEnd(ActionResult(TransResult.ERR_PREAUTH_CANCEL_UNSUPPORTED, null))
                return
            }

            /* 3. This section allow only TPN host
                    SUPPORTED CARD
                        1. UPI - CREDIT     : SUPPORTED
                        2. UPI - DEBIT      : UNSUPPORTED
                        3. TPN - DEBIT      : UNSUPPORTED
             */
            if (!it.acquirer.name.equals(Constants.ACQ_UP)) {
                transEnd(ActionResult(TransResult.ERR_PREAUTH_CANCEL_UNSUPPORTED, null))
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
            transData.transState = it.transState
            transData.transType = ETransType.PREAUTHORIZATION_CANCELLATION

            // duplicateOriginalTransData
            copyOrigTransData(it)

            gotoState(State.DISP_TRANS_DETAIL.toString())

        }?:run {
            transEnd(ActionResult(TransResult.ERR_NO_ORIG_TRANS, null))
        }
    }

    private fun voidTransByMultiApp(transMultiApp : TransMultiAppData, origTransNo: Long) {
        var tempOrigTransNo : Long = origTransNo
        if (origTransNo == -1L) {
            tempOrigTransNo = (if(isVoidWithSTAN) {transMultiApp.stanNo} else {transMultiApp.traceNo}) as Long
        }

        val amexAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
        if (amexAcq.isEnable
            && AmexTransAPI.getInstance().process.isAppInstalled(context)
            && Constants.ACQ_AMEX.equals(transMultiApp.acquirer.name)) {
            this.originalTransNo = tempOrigTransNo
            setTransRunning(true)
            gotoState(State.AMEX_API.toString())
        } else {
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
            if (!checkIsOfflineSent()) {
                toSignOrPrint()
            } else {
                gotoState(State.MAG_ONLINE.toString())
            }
        } else {
            transEnd(result)
        }
    }

    fun checkIsOfflineSent() : Boolean {
        val currTransType = transData.transType
        val oriTransType = transData.origTransType
        val isOfflineNormalSale = (currTransType.equals(ETransType.OFFLINE_TRANS_SEND)
                                    || oriTransType.equals(ETransType.OFFLINE_TRANS_SEND)
                                    || (ETransType.SALE.equals(currTransType) && transData.offlineSendState != null)
                                    || (ETransType.SALE.equals(oriTransType) && transData.offlineSendState != null))
        if (isOfflineNormalSale
            && transData.offlineSendState != TransData.OfflineStatus.OFFLINE_SENT
            && transData.offlineSendState != TransData.OfflineStatus.OFFLINE_VOIDED)   {

            transData.dateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
            transData.authCode = originalTransData!!.authCode
            transData.offlineSendState = TransData.OfflineStatus.OFFLINE_VOIDED
            transData.responseCode = originalTransData!!.responseCode
            FinancialApplication.getTransDataDbHelper().insertTransData(transData)

            originalTransData!!.voidStanNo = transData.stanNo
            originalTransData!!.dateTime = transData.dateTime
            originalTransData!!.transState = TransData.ETransStatus.VOIDED
            originalTransData!!.offlineSendState = TransData.OfflineStatus.OFFLINE_VOIDED
            FinancialApplication.getTransDataDbHelper().updateTransData(originalTransData!!)

            Component.incStanNo(transData)

            return false
        }

        return true
    }

    fun toSignOrPrint() {
        if (transData.isTxnSmallAmt && transData.numSlipSmallAmt == 0) {
            FinancialApplication.getTransDataDbHelper().updateTransData(transData)
        }
        gotoState(State.PRINT.toString())
        FinancialApplication.getTransDataDbHelper().updateTransData(transData)
    }

    fun afterMagOnline(result:ActionResult) {
        if (result.ret==TransResult.SUCC) {
            originalTransData?.apply {
                this.voidStanNo = transData.stanNo
                this.dateTime = transData.dateTime
                this.transState = TransData.ETransStatus.VOIDED
                this.offlineSendState = transData.offlineSendState

                val authCode = transData.authCode?.let{it}?:run{transData.origAuthCode}
                this.origAuthCode = authCode
                transData.authCode = authCode

                // update transData back
                FinancialApplication.getTransDataDbHelper().updateTransData(originalTransData)
                FinancialApplication.getTransDataDbHelper().updateTransData(transData)

                checkOfflineTrans()
            }
        } else {
            transEnd(result)
        }
    }

    fun checkOfflineTrans() {
        val acqs = arrayListOf<Acquirer>(transData.acquirer)
        val excludes = arrayListOf<TransData.OfflineStatus>(TransData.OfflineStatus.OFFLINE_SENT, TransData.OfflineStatus.OFFLINE_VOIDED)
        val offlineTransList = FinancialApplication.getTransDataDbHelper().findAllOfflineTransData(acqs, excludes)
        if ((originalTransData!!.transType.equals(ETransType.SALE) || originalTransData!!.transType.equals(ETransType.OFFLINE_TRANS_SEND))
            && !offlineTransList.isEmpty() && offlineTransList.get(0).id!=transData.id) {
            gotoState(State.OFFLINE_SEND.toString())
        }
        else {
            toSignOrPrint()
        }
    }

    fun afterOfflineSent(offlineSentResult:ActionResult) {
        toSignOrPrint()
    }

    fun afterSignature(result:ActionResult) {
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

    fun afterPrinted(result:ActionResult) {
        if (result.ret.equals(TransResult.SUCC)) {
            transEnd(result)
        }
        else {
            dispResult(transType.transName, result, null)
            gotoState(State.PRINT.toString())
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
    }


}