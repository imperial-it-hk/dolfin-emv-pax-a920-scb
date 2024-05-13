package th.co.bkkps.linkposapi.service.message

import android.content.Context
import com.pax.abl.core.ATransaction
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.ECR.EcrData
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.*
import com.pax.pay.trans.action.ActionSearchCard
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransTotal
import com.pax.pay.utils.TimeConverter
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import th.co.bkkps.linkposapi.LinkPOSApi
import th.co.bkkps.linkposapi.action.ActionLinkPosReport
import th.co.bkkps.linkposapi.model.LinkPOSModel
import th.co.bkkps.linkposapi.model.LinkPOSResponse
import th.co.bkkps.linkposapi.util.LinkPOSConvertMessage
import th.co.bkkps.utils.Log
import java.util.*
import kotlin.collections.HashMap
import kotlin.experimental.or

open class HypercomMessage internal constructor(val model: LinkPOSModel) : ProtocolMessage {
    companion object {
        const val TAG = "HypercomMessage"
        const val TRANS_CODE_SALE_ALL_TYPE = "20"
        const val TRANS_CODE_VOID_TYPE = "26"
        const val TRANS_CODE_SETTLEMENT_TYPE = "50"
        const val TRANS_CODE_SALE_CREDIT_TYPE = "56"
        const val TRANS_CODE_SALE_QR_PAYMENT = "70"
        const val TRANS_CODE_SUMMARY_TYPE = "90"
        const val TRANS_CODE_AUDIT_TYPE = "91"
        const val TRANS_CODE_TEST_COMMUNICATION_TYPE = "D0"

        const val TRANS_QR_PAYMENT_ALIPAY = "01"
        const val TRANS_QR_PAYMENT_WECHAT = "02"
        const val TRANS_QR_PAYMENT_KPLUS = "03"
        const val TRANS_QR_PAYMENT_QR_CREDIT = "04"

        val hostIndex = mapOf(
            Constants.ACQ_KBANK to 0,
            Constants.ACQ_DCC to 1,
            Constants.ACQ_SMRTPAY to 2,
            Constants.ACQ_REDEEM to 3,
            Constants.ACQ_UP to 4,
            Constants.ACQ_AMEX to 5,
            Constants.ACQ_KPLUS to 6,
            Constants.ACQ_WECHAT to 7,
            Constants.ACQ_ALIPAY to 8,
            Constants.ACQ_QR_CREDIT to 9
        )
    }

    override fun onProcess(mContext: Context) {
        try {
            if (Utils.parseLongSafe(model.request.amount, 0) > FinancialApplication.getSysParam().edcMaxAmt) {
                DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.err_amount_exceed_max_limit), null, Constants.FAILED_DIALOG_SHOW_TIME);
                rejectResponse("Amount greater than maximum limit")
                onFinish()
                return
            }

            if (Utils.parseLongSafe(model.request.amount, 0) < FinancialApplication.getSysParam().edcMinAmt) {
                DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.err_amount_below_min_limit), null, Constants.FAILED_DIALOG_SHOW_TIME);
                rejectResponse("Amount less than minimum limit")
                onFinish()
                return
            }

            when (model.request.transCode) {
                TRANS_CODE_SALE_ALL_TYPE, TRANS_CODE_SALE_CREDIT_TYPE -> {
                    when (model.request.vatBCommand) {
                        true -> doSaleVatB(mContext)
                        false -> doSale(mContext)
                    }
                }
                TRANS_CODE_VOID_TYPE -> doVoid(mContext)
                TRANS_CODE_SETTLEMENT_TYPE -> doSettle(mContext)
                TRANS_CODE_SALE_QR_PAYMENT -> doSaleQrPayment(mContext)
                TRANS_CODE_SUMMARY_TYPE -> doSummaryReport(mContext)
                TRANS_CODE_AUDIT_TYPE -> doAuditReport(mContext)
            }
        }
        catch (ex: Exception) {
            rejectResponse("Unexpected Error")
            onFinish()
        }
    }

    override fun onFinish() {
        val jsonResponse = LinkPOSConvertMessage.modelToJson(model)
        jsonResponse?.let {
            Log.d(TAG, "Sending response jsonMessage=$it")
            LinkPOSApi.sendResponse(it)
        }
        FinancialApplication.EcrProcess.resetFlag()
    }

    private fun doSale(mContext: Context) {
        val mode = ActionSearchCard.SearchMode.SWIPE or ActionSearchCard.SearchMode.INSERT or ActionSearchCard.SearchMode.WAVE or ActionSearchCard.SearchMode.KEYIN
        val saleTrans = SaleTrans(mContext, model.request.amount, mode, null, true)
        { result ->
            Log.d(TAG, "onDataRcv:cancelSaleSendResponse ret =" + result.ret)
            if (result.ret != TransResult.SUCC) {
                rejectResponse(result.ret)
                onFinish()
            }
        }

        saleTrans.setECRProcReturnListener { _, result ->
            if (result.ret == TransResult.SUCC) {
                successTransResponse()
                onFinish()
            }
        }

        saleTrans.execute()
    }

    private fun doSaleVatB(mContext: Context) {
        val transEndListener = ATransaction.TransEndListener { result ->
            Log.d(TAG, "onDataRcv:cancelSaleVatBSendResponse ret =" + result.ret)
            if (result.ret != TransResult.SUCC) {
                rejectResponse(result.ret)
                onFinish()
            }
        }

        val mode = ActionSearchCard.SearchMode.SWIPE or ActionSearchCard.SearchMode.INSERT or ActionSearchCard.SearchMode.WAVE or ActionSearchCard.SearchMode.KEYIN
        val saleTrans = SaleTrans(mContext, model.request.amount, mode, null, true, transEndListener, true,
                model.request.vatBRef1?.toByteArray(),
                model.request.vatBRef2?.toByteArray(),
                model.request.vatBAmount?.toByteArray(),
                model.request.vatBTaxAllowance?.toByteArray(),
                model.request.vatBMerUnique?.toByteArray(),
                model.request.vatBCampaignType?.toByteArray()
        )

        saleTrans.setECRProcReturnListener { _, result ->
            if (result.ret == TransResult.SUCC) {
                successTransResponse()
                onFinish()
            }
        }

        saleTrans.execute()
    }

    private fun doSaleQrPayment(mContext: Context) {
        val transEndListener = ATransaction.TransEndListener { result ->
            Log.d(TAG, "onDataRcv:cancelQrPaymentSendResponse ret =" + result.ret)
            if (result.ret != TransResult.SUCC) {
                rejectResponse(result.ret)
                onFinish()
            }
        }
        val qrPaymentTrans = when (model.request.qrType) {
            TRANS_QR_PAYMENT_ALIPAY -> {
                AlipayQrSaleTrans(mContext, model.request.amount, null, false, transEndListener)
            }
            TRANS_QR_PAYMENT_WECHAT -> {
                WechatQrSaleTrans(mContext, model.request.amount, null, false, transEndListener)
            }
            TRANS_QR_PAYMENT_KPLUS -> {
                KplusQrSaleTrans(mContext, model.request.amount, null, false, transEndListener)
            }
            TRANS_QR_PAYMENT_QR_CREDIT -> {
                QRCreditSaleTrans(mContext, model.request.amount, null, false, transEndListener)
            }
            else -> null
        }

        qrPaymentTrans?.apply {
            this.setECRProcReturnListener { _, result ->
                if (result.ret == TransResult.SUCC) {
                    successSaleQrPaymentTransResponse()
                    onFinish()
                }
            }

            this.execute()
        } ?: run {
            rejectResponse("Invalid QR Type")
            onFinish()
        }
    }

    private fun doVoid(mContext: Context) {
        val saleVoidTrans = SaleVoidTrans(mContext, Utils.parseLongSafe(model.request.traceNo, 0)) { result ->
            Log.d(TAG, "onDataRcv:cancelSaleVoidSendResponse ret =" + result.ret)
            if (result.ret != TransResult.SUCC) {
                rejectResponse(result.ret)
                onFinish()
            }
        }

        saleVoidTrans.setECRProcReturnListener { _, result ->
            if (result.ret == TransResult.SUCC) {
                val origTrans: TransData? = if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                    FinancialApplication.getTransDataDbHelper().findTransDataByStanNo(
                            Utils.parseLongSafe(model.request.traceNo, -1), false)
                } else {
                    FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(
                            Utils.parseLongSafe(model.request.traceNo, -1), false)
                }
                origTrans?.let {
                    if (it.acquirer?.name.equals(Constants.ACQ_KPLUS) ||
                            it.acquirer?.name.equals(Constants.ACQ_ALIPAY) ||
                            it.acquirer?.name.equals(Constants.ACQ_WECHAT) ||
                            it.acquirer?.name.equals(Constants.ACQ_QR_CREDIT)) {
                        successSaleQrPaymentTransResponse()
                    } else {
                        successTransResponse()
                    }
                } ?: run {
                    successTransResponse()
                }
                onFinish()
            }
        }

        saleVoidTrans.execute()
    }

    private fun doSettle(mContext: Context) {
        val settleTrans = when (model.request.allHostCommand) {
            true -> {
                if (!model.request.nii.equals("999")) {
                    null
                } else {
                    SettleTrans(mContext, false, true, model.request.nii!!) { result ->
                        Log.d(TAG, "onDataRcv:SettleAllHostSendResponse ret =" + result.ret)
                        if (EcrData.instance.settleAllHostData == null || EcrData.instance.settleAllHostData.isEmpty()) {
                            rejectResponse("Host not found")
                        } else {
                            val merchantNum = model.request.merchantNum ?: "001"
                            val mapHostData = EcrData.instance.settleAllHostData.toSortedMap()
                            var hostTotal = ""
                            var batchStatus = ""
                            for ((index, hostData) in mapHostData) {
                                val total = hostData.total
                                val hostId = index.toString(10).padStart(3, '0')
                                val saleCount = total.saleTotalNum.toString(10).padStart(3, '0')
                                val saleAmount = total.saleTotalAmt.toString(10).padStart(12, '0')
                                val refundCount = total.refundTotalNum.toString(10).padStart(3, '0')
                                val refundAmount = total.refundTotalAmt.toString(10).padStart(12, '0')
                                val batchTotal = saleCount + saleAmount + refundCount + refundAmount

                                val bNull = ByteArray(24)
                                Arrays.fill(bNull, 0x00)

                                hostTotal += "H${hostId}M${merchantNum}${batchTotal.padEnd(96, '0')}${String(bNull)}"
                                batchStatus += "${hostId}${total.acquirer.nii.padStart(3, '0')}${String(hostData.status)}"
                            }
                            successSettleAllHostResponse(hostTotal, batchStatus)
                        }
                        EcrData.instance.settleAllHostData = HashMap()
                        onFinish()
                    }
                }
            }
            false -> {
                SettleTrans(mContext, false, false, model.request.hostName!!) { result ->
                    Log.d(TAG, "onDataRcv:SettleEachHostSendResponse ret =" + result.ret)
                    when {
                        EcrData.instance.settleRespCode contentEquals byteArrayOf(0x30, 0x30) -> {
                            successSettleSingleHostResponse()
                        }
                        else -> {
                            rejectSettleSingleHostResponse(result.ret)
                        }
                    }
                    resetSettleSingleHostData()
                    onFinish()
                }
            }
        }

        settleTrans?.apply {
            this.setECRProcReturnListener { _, _ ->
                resetSettleSingleHostData()
                EcrData.instance.settleAllHostData = emptyMap()
            }

            this.execute()
        } ?: run {
            if (model.request.allHostCommand) {
                rejectResponse("Invalid Request field value")
            } else {
                rejectResponse("No Transaction")
            }
            onFinish()
        }
    }

    private fun doSummaryReport(mContext: Context) {
        when (model.request.allHostCommand) {
            true -> setBatchTotalAllHost(mContext)
            false -> setBatchTotalByHost(mContext)
        }
    }

    private fun setBatchTotalByHost(mContext: Context) {
        val acquirer = FinancialApplication.getAcqManager().findAcquirer(model.request.hostName)
        acquirer?.let { acq ->
            when (acq.isEnable) {
                true -> {
                    val total = FinancialApplication.getTransTotalDbHelper().calcTotal(acq, false)
                    total?.apply {
                        when (isZero) {
                            true -> {
                                rejectResponse("No Transaction")
                                onFinish()
                            }
                            false -> {
                                this.acquirer = acq
                                this.dateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
                                this.terminalID = acq.terminalId
                                this.merchantID = acq.merchantId
                                this.batchNo = acq.currBatchNo
                                val saleCount = saleTotalNum.toString(10).padStart(3, '0')
                                val saleAmount = saleTotalAmt.toString(10).padStart(12, '0')
                                val refundCount = refundTotalNum.toString(10).padStart(3, '0')
                                val refundAmount = refundTotalAmt.toString(10).padStart(12, '0')
                                val batchTotal = saleCount + saleAmount + refundCount + refundAmount

                                val actionLinkPosReport = ActionLinkPosReport { action ->
                                    (action as ActionLinkPosReport).setParam(mContext, "Summary Report", model.request.transCode!!, acq.name)
                                }
                                actionLinkPosReport.setEndListener { _, result ->
                                    TransContext.getInstance().currentAction.isFinished = false
                                    TransContext.getInstance().currentAction = null
                                    if (result.ret == TransResult.SUCC) {
                                        successSummaryResponse(this, batchTotal.padEnd(96, '0'))
                                    } else {
                                        rejectResponse("Error printing summary report")
                                    }
                                    onFinish()
                                }
                                actionLinkPosReport.execute()
                            }
                        }
                    } ?: run {
                        rejectResponse("No Transaction")
                        onFinish()
                    }
                }
                false -> {
                    rejectResponse("Host not found")
                    onFinish()
                }
            }
        } ?: run {
            rejectResponse("Host not found")
            onFinish()
        }
    }

    private fun setBatchTotalAllHost(mContext: Context) {
        val allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory()
        if (allTrans.isEmpty()) {
            rejectResponse("No Transaction")
            onFinish()
        }
        else {
            var saleTotalNum: Long = 0; var saleTotalAmt: Long = 0
            var refundTotalNum: Long = 0; var refundTotalAmt: Long = 0
            val acquirerList = FinancialApplication.getAcqManager().findEnableAcquirer()
            for (acq in acquirerList) {
                val total = FinancialApplication.getTransTotalDbHelper().calcTotal(acq, false)
                saleTotalNum += total.saleTotalNum
                saleTotalAmt += total.saleTotalAmt
                refundTotalNum += total.refundTotalNum
                refundTotalAmt += total.refundTotalAmt
            }
            val saleCount = saleTotalNum.toString(10).padStart(3, '0')
            val saleAmount = saleTotalAmt.toString(10).padStart(12, '0')
            val refundCount = refundTotalNum.toString(10).padStart(3, '0')
            val refundAmount = refundTotalAmt.toString(10).padStart(12, '0')
            val batchTotal = saleCount + saleAmount + refundCount + refundAmount

            val acquirer = Acquirer()
            acquirer.apply {
                this.nii = "999"
                this.name = Utils.getString(R.string.acq_all_acquirer)
            }

            val total = TransTotal()
            total.apply {
                this.acquirer = acquirer
                this.dateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
                this.terminalID = "".padEnd(8, '0')
                this.merchantID = "".padEnd(15, '0')
                this.batchNo = 1
            }

            val actionLinkPosReport = ActionLinkPosReport { action ->
                (action as ActionLinkPosReport).setParam(mContext, "Summary Report", model.request.transCode!!, acquirer.name)
            }
            actionLinkPosReport.setEndListener { _, result ->
                TransContext.getInstance().currentAction.isFinished = false
                TransContext.getInstance().currentAction = null
                if (result.ret == TransResult.SUCC) {
                    successSummaryResponse(total, batchTotal.padEnd(96, '0'))
                } else {
                    rejectResponse("Error printing summary report")
                }
                onFinish()
            }
            actionLinkPosReport.execute()
        }
    }

    private fun doAuditReport(mContext: Context) {
        when (model.request.allHostCommand) {
            true -> setAuditBatchTotalAllHost(mContext)
            false -> setAuditBatchTotalByHost(mContext)
        }
    }

    private fun setAuditBatchTotalByHost(mContext: Context) {
        val acquirer = FinancialApplication.getAcqManager().findAcquirer(model.request.hostName)
        acquirer?.let { acq ->
            when (acq.isEnable) {
                true -> {
                    val total = FinancialApplication.getTransTotalDbHelper().calcTotal(acq, false)
                    total?.apply {
                        when (isZero) {
                            true -> {
                                rejectResponse("No Transaction")
                                onFinish()
                            }
                            false -> {
                                this.acquirer = acq
                                this.dateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
                                this.terminalID = acq.terminalId
                                this.merchantID = acq.merchantId
                                this.batchNo = acq.currBatchNo
                                val saleCount = saleTotalNum.toString(10).padStart(3, '0')
                                val saleAmount = saleTotalAmt.toString(10).padStart(12, '0')
                                val refundCount = refundTotalNum.toString(10).padStart(3, '0')
                                val refundAmount = refundTotalAmt.toString(10).padStart(12, '0')
                                val batchTotal = saleCount + saleAmount + refundCount + refundAmount

                                val actionLinkPosReport = ActionLinkPosReport { action ->
                                    (action as ActionLinkPosReport).setParam(mContext, "Detail Report", model.request.transCode!!, acq.name)
                                }
                                actionLinkPosReport.setEndListener { _, result ->
                                    TransContext.getInstance().currentAction.isFinished = false
                                    TransContext.getInstance().currentAction = null
                                    if (result.ret == TransResult.SUCC) {
                                        successSummaryResponse(total, batchTotal.padEnd(96, '0'))
                                    } else {
                                        rejectResponse("Error printing detail report")
                                    }
                                    onFinish()
                                }
                                actionLinkPosReport.execute()
                            }
                        }
                    } ?: run {
                        rejectResponse("No Transaction")
                        onFinish()
                    }
                }
                false -> {
                    rejectResponse("Host not found")
                    onFinish()
                }
            }
        } ?: run {
            rejectResponse("Host not found")
            onFinish()
        }
    }

    private fun setAuditBatchTotalAllHost(mContext: Context) {
        val allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory()
        if (allTrans.isEmpty()) {
            rejectResponse("No Transaction")
            onFinish()
        }
        else {
            var saleTotalNum: Long = 0; var saleTotalAmt: Long = 0
            var refundTotalNum: Long = 0; var refundTotalAmt: Long = 0
            val acquirerList = FinancialApplication.getAcqManager().findEnableAcquirer()
            for (acq in acquirerList) {
                val total = FinancialApplication.getTransTotalDbHelper().calcTotal(acq, false)
                saleTotalNum += total.saleTotalNum
                saleTotalAmt += total.saleTotalAmt
                refundTotalNum += total.refundTotalNum
                refundTotalAmt += total.refundTotalAmt
            }
            val saleCount = saleTotalNum.toString(10).padStart(3, '0')
            val saleAmount = saleTotalAmt.toString(10).padStart(12, '0')
            val refundCount = refundTotalNum.toString(10).padStart(3, '0')
            val refundAmount = refundTotalAmt.toString(10).padStart(12, '0')
            val batchTotal = saleCount + saleAmount + refundCount + refundAmount

            val acquirer = Acquirer()
            acquirer.apply {
                this.nii = "999"
                this.name = Utils.getString(R.string.acq_all_acquirer)
            }

            val total = TransTotal()
            total.apply {
                this.acquirer = acquirer
                this.dateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
                this.terminalID = "".padEnd(8, '0')
                this.merchantID = "".padEnd(15, '0')
                this.batchNo = 1
            }

            val actionLinkPosReport = ActionLinkPosReport { action ->
                (action as ActionLinkPosReport).setParam(mContext, "Detail Report", model.request.transCode!!, acquirer.name)
            }
            actionLinkPosReport.setEndListener { _, result ->
                TransContext.getInstance().currentAction.isFinished = false
                TransContext.getInstance().currentAction = null
                if (result.ret == TransResult.SUCC) {
                    successSummaryResponse(total, batchTotal.padEnd(96, '0'))
                } else {
                    rejectResponse("Error printing detail report")
                }
                onFinish()
            }
            actionLinkPosReport.execute()
        }
    }

    fun successTransResponse() {
        val response = LinkPOSResponse()
        response.status = "SUCCESS"
        setSuccessResponseCode(response)
        setField02(response, "APPROVE")
        setFieldD0(response)
        setField03(response)
        setField04(response)
        setField01(response)
        setField65(response)
        setField16(response)
        setFieldD1(response)
        setFieldD2(response)
        setField30(response)
        setField31(response)
        setField50(response)
        setFieldD3(response)
        setFieldD4(response)
        setFieldD5(response)
        setField40(response)
        model.response = response
    }

    fun successSaleQrPaymentTransResponse() {
        val response = LinkPOSResponse()
        response.status = "SUCCESS"
        setSuccessResponseCode(response)
        setField02(response, "APPROVE")
        setFieldD0(response)
        setField03(response)
        setField04(response)
        setField01(response)
        setField65(response)
        setField16(response)
        setFieldD1(response)
//        setFieldD2QrPayment(response)
        setFieldD2(response)
        setField30QrPayment(response)
        setField31(response)
        setField50(response)
        setFieldD3(response)
        setFieldD4(response)
        setFieldD5(response)
        setField40(response)
        model.response = response
    }

    fun rejectResponse(ret: Int) {
        val response = LinkPOSResponse()
        response.status = "REJECT"
        setRejectResponseCode(response, ret)
        setField02(response, "REJECT")
        setFieldD0(response)
        setField03(response)
        setField04(response)
        model.response = response
    }

    private fun successSettleSingleHostResponse() {
        EcrData.instance.TermID = EcrData.instance.settleTermId
        EcrData.instance.MerID = EcrData.instance.settleMerId
        EcrData.instance.BatchNo = EcrData.instance.settleBatchNo
        EcrData.instance.DateByte = EcrData.instance.settleDate
        EcrData.instance.TimeByte = EcrData.instance.settleTime
        EcrData.instance.HYPER_COM_HN_NII = EcrData.instance.settleHN

        val response = LinkPOSResponse()
        response.status = "SUCCESS"
        setSuccessResponseCode(response)
        setField02(response, "BATCH CLOSED")
        setFieldD0(response)
        setField16(response)
        setFieldD1(response)
        setField50(response)
        setField03(response)
        setField04(response)
        setFieldHN(response)
        setFieldHO(response)
        model.response = response
    }

    private fun rejectSettleSingleHostResponse(ret: Int) {
        val respCode = String(EcrData.instance.settleRespCode)
        val respText = when (respCode) {
            "CL" -> "Transaction Cancelled"
            "ND" -> "No Transaction"
            else -> {
                when (ret) {
                    TransResult.ERR_HOST_NOT_FOUND -> "Host not found"
                    else -> "Settlement Fail"
                }
            }
        }

        val response = LinkPOSResponse()
        response.status = "REJECT"
        setRejectResponseCode(response, respCode)
        setField02(response, respText.toUpperCase(Locale.getDefault()))
        setFieldD0(response)
        setField03(response)
        setField04(response)
        model.response = response
    }

    private fun successSettleAllHostResponse(hostTotal: String, batchStatus: String) {
        val response = LinkPOSResponse()
        response.status = "SUCCESS"
        setSuccessResponseCode(response)
        setFieldZY(response, hostTotal)
        setFieldZZ(response, batchStatus)
        model.response = response
    }

    private fun resetSettleSingleHostData() {
        EcrData.instance.settleTermId = ByteArray(6)
        EcrData.instance.settleMerId = ByteArray(15)
        EcrData.instance.settleBatchNo = ByteArray(6)
        EcrData.instance.settleDate = ByteArray(6)
        EcrData.instance.settleTime = ByteArray(6)
        EcrData.instance.settleHN = ByteArray(3)
        EcrData.instance.singleBatchTotal = ByteArray(0)
    }

    private fun successSummaryResponse(total: TransTotal, batchTotal: String) {
        val response = LinkPOSResponse()
        response.status = "SUCCESS"
        setSuccessResponseCode(response)
        setField02(response, "SUCCESS")
        setFieldD0(response)
        setField16(response, total.terminalID)
        setFieldD1(response, total.merchantID)
        setField50(response, total.batchNo.toString(10).padStart(6, '0'))
        setField03(response, getFormatDateTime(total.dateTime, "yyMMdd")!!)
        setField04(response, getFormatDateTime(total.dateTime, "HHmmss")!!)
        setFieldHN(response, total.acquirer.nii)
        setFieldHO(response, batchTotal)
        model.response = response
    }

    fun rejectResponse(respCode: String) {
        val deviceDateTime = Device.getTime(Constants.TIME_PATTERN_TRANS)
        val response = LinkPOSResponse()
        response.status = "REJECT"
        setRejectResponseCode(response, "ND")
        setField02(response, respCode.toUpperCase(Locale.getDefault()))
        setFieldD0(response)
        setField03(response, getFormatDateTime(deviceDateTime, "yyMMdd")!!)
        setField04(response, getFormatDateTime(deviceDateTime, "HHmmss")!!)
        model.response = response
    }

    private fun getFormatDateTime(dateTime: String, pattern: String): String? {
        return TimeConverter.convert(dateTime, Constants.TIME_PATTERN_TRANS, pattern)
    }

    private fun setRejectResponseCode(response: LinkPOSResponse, ret: Int): LinkPOSResponse {
        response.f00 = when {
            ret == TransResult.ERR_USER_CANCEL -> "CL"
            ret != TransResult.SUCC || EcrData.instance.RespCode contentEquals byteArrayOf(0x52, 0x42) -> "ND"
            else -> String(EcrData.instance.RespCode)
        }
        return response
    }

    private fun setRejectResponseCode(response: LinkPOSResponse, respCode: String): LinkPOSResponse {
        response.f00 = respCode
        return response
    }

    private fun setSuccessResponseCode(response: LinkPOSResponse): LinkPOSResponse {
        response.f00 = "00"
        return response
    }

    private fun setField02(response: LinkPOSResponse, respText: String): LinkPOSResponse {
        response.f02 = respText
        return response
    }

    private fun setFieldD0(response: LinkPOSResponse): LinkPOSResponse {
        val sysParam = FinancialApplication.getSysParam()
        response.d0 = (sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN) ?: " ") + "\n" +
                (sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS) ?: " ") + "\n" +
                (sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1) ?: " ")
        return response
    }

    private fun setField03(response: LinkPOSResponse): LinkPOSResponse {
        response.f03 = String(EcrData.instance.DateByte)
        return response
    }

    private fun setField03(response: LinkPOSResponse, date: String): LinkPOSResponse {
        response.f03 = date
        return response
    }

    private fun setField04(response: LinkPOSResponse): LinkPOSResponse {
        response.f04 = String(EcrData.instance.TimeByte)
        return response
    }

    private fun setField04(response: LinkPOSResponse, time: String): LinkPOSResponse {
        response.f04 = time
        return response
    }

    private fun setField01(response: LinkPOSResponse): LinkPOSResponse {
        response.f01 = String(EcrData.instance.ApprovalCode)
        return response
    }

    private fun setField65(response: LinkPOSResponse): LinkPOSResponse {
        response.f65 = String(EcrData.instance.TraceNo)
        return response
    }

    private fun setField16(response: LinkPOSResponse): LinkPOSResponse {
        response.f16 = String(EcrData.instance.TermID)
        return response
    }

    private fun setField16(response: LinkPOSResponse, tid: String): LinkPOSResponse {
        response.f16 = tid
        return response
    }

    private fun setFieldD1(response: LinkPOSResponse): LinkPOSResponse {
        response.d1 = String(EcrData.instance.MerID)
        return response
    }

    private fun setFieldD1(response: LinkPOSResponse, mid: String): LinkPOSResponse {
        response.d1 = mid
        return response
    }

    private fun setFieldD2(response: LinkPOSResponse): LinkPOSResponse {
        response.d2 = String(EcrData.instance.CardIssuerName)
        return response
    }

    private fun setFieldD2QrPayment(response: LinkPOSResponse): LinkPOSResponse {
        response.d2 = when (model.request.qrType) {
            TRANS_QR_PAYMENT_ALIPAY -> Constants.ISSUER_ALIPAY
            TRANS_QR_PAYMENT_WECHAT -> Constants.ISSUER_WECHAT
            TRANS_QR_PAYMENT_KPLUS -> Constants.ECR_QR_PAYMENT
            TRANS_QR_PAYMENT_QR_CREDIT -> Constants.ISSUER_QRCREDIT
            else -> ""
        }
        return response
    }

    private fun setField30(response: LinkPOSResponse): LinkPOSResponse {
        response.f30 = String(EcrData.instance.HyperComCardNo)
        return response
    }

    private fun setField30QrPayment(response: LinkPOSResponse): LinkPOSResponse {
        response.f30 = String(EcrData.instance.qr_TransID)
        return response
    }

    private fun setField31(response: LinkPOSResponse): LinkPOSResponse {
        response.f31 = "XXXX"
        return response
    }

    private fun setField50(response: LinkPOSResponse): LinkPOSResponse {
        response.f50 = String(EcrData.instance.BatchNo)
        return response
    }

    private fun setField50(response: LinkPOSResponse, batchNo: String): LinkPOSResponse {
        response.f50 = batchNo
        return response
    }

    private fun setFieldD3(response: LinkPOSResponse): LinkPOSResponse {
        response.d3 = String(EcrData.instance.RefNo)
        return response
    }

    private fun setFieldD4(response: LinkPOSResponse): LinkPOSResponse {
        response.d4 = String(EcrData.instance.CardIssuerID)
        return response
    }

    private fun setFieldD5(response: LinkPOSResponse): LinkPOSResponse {
        response.d5 = "".padEnd(26, 'X')
        return response
    }

    private fun setField40(response: LinkPOSResponse): LinkPOSResponse {
        response.f40 = String(EcrData.instance.transAmount)
        return response
    }

    private fun setFieldHN(response: LinkPOSResponse): LinkPOSResponse {
        response.hn = String(EcrData.instance.HYPER_COM_HN_NII)
        return response
    }

    private fun setFieldHN(response: LinkPOSResponse, nii: String): LinkPOSResponse {
        response.hn = nii
        return response
    }

    private fun setFieldHO(response: LinkPOSResponse): LinkPOSResponse {
        response.ho = String(EcrData.instance.singleBatchTotal)
        return response
    }

    private fun setFieldHO(response: LinkPOSResponse, batchTotal: String): LinkPOSResponse {
        response.ho = batchTotal
        return response
    }

    private fun setFieldZY(response: LinkPOSResponse, hostTotal: String): LinkPOSResponse {
        response.zy = hostTotal
        return response
    }

    private fun setFieldZZ(response: LinkPOSResponse, batchStatus: String): LinkPOSResponse {
        response.zz = batchStatus
        return response
    }
}