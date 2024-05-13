package com.pax.pay.trans

import android.content.Context
import android.util.Log
import com.pax.abl.core.ActionResult
import com.pax.edc.BuildConfig
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.*
import com.pax.pay.trans.action.ActionInputTransData.EInputType
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.ReservedFieldHandle
import com.pax.pay.trans.model.TransData.EnterMode
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TransResultUtils
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam


class DolfinInstalmentTrans(
        context: Context,
        iPlanMode: String = "",
        private val isPromo: Boolean = false,
        transListener: TransEndListener?
) : InstalmentKbankTrans(context, ETransType.DOLFIN_INSTALMENT, Constants.ACQ_DOLFIN_INSTALMENT, iPlanMode, transListener) {
    private val TAG = "DolfinInstalmentTrans"
    private var processInquiry = true;

    enum class State { ENTER_AMOUNT, ENTER_IPP_TERM, ENTER_SUPPLIER, ENTER_PRODUCT, ENTER_SN, CONFIRM_DETAILS, SCAN_CODE, MAG_ONLINE, CONFIRM_INQUIRY, SIGNATURE, PRINT }


    private fun nextState(gotoState: State): String {
        var nextState = gotoState

        if (nextState == State.ENTER_SUPPLIER)
            if (!isPromo)
                nextState = State.ENTER_IPP_TERM
            else if (transData.skuCode.isNotBlank())
                nextState = State.ENTER_PRODUCT

        if (nextState == State.ENTER_PRODUCT)
            if (transData.productCode.isNotBlank())
                nextState = State.ENTER_SN

        if (nextState == State.ENTER_SN)
            if (transData.instalmentSerialNo.isNotBlank())
                nextState = State.ENTER_IPP_TERM

        if (nextState == State.ENTER_IPP_TERM)
            if (transData.instalmentPaymentTerm > 0)
                nextState = State.ENTER_AMOUNT

        if (nextState == State.ENTER_AMOUNT)
            if (transData.amount.isNotBlank())
                nextState = State.SCAN_CODE

        return nextState.toString()
    }

    override fun bindStateOnAction() {
        super.bindStateOnAction()

        title = getString(R.string.menu_dolfin_instalment)
        val resId = Utils.getResId("menu_instalment_" + iPlanMode, "string")
        if (resId > 0)
            title = Utils.getString(resId)


        transData.amount = ""
        transData.isInstalmentPromoProduct = isPromo
        transData.instalmentIPlanMode = iPlanMode
        transData.instalmentPaymentTerm = 0
        transData.instalmentSerialNo = ""
        transData.productCode = ""
        transData.skuCode = ""


        val enterSupplierAction = ActionInputTransData { action ->
            (action as ActionInputTransData).setParam(currentContext, title)
                    .setInputLine(getString(R.string.instalment_supplier_code), "5 digits", EInputType.NUM, 5, 5, false)
        }
        bind(State.ENTER_SUPPLIER.toString(), enterSupplierAction, true)

        val enterCodeAction = ActionInputTransData { action ->
            (action as ActionInputTransData).setParam(currentContext, title)
                    .setInputLine(getString(R.string.instalment_product_code), "9 digits", EInputType.NUM, 9, 9, false)
        }
        bind(State.ENTER_PRODUCT.toString(), enterCodeAction, true)

        val enterSNAction = ActionInputTransData { action ->
            (action as ActionInputTransData).setParam(currentContext, title)
                    .setInputLine(getString(R.string.instalment_serial_number), context.getString(R.string.hint_keyin_or_scan_qr), EInputType.NUM, 20, 0, true)
        }
        bind(State.ENTER_SN.toString(), enterSNAction, true)

        //val scanCodeAction = ActionScanCode(null)
        //bind(State.SCAN_CODE.toString(), scanCodeAction, true)

        val getQrFromKPlusReceiptAction = ActionGetQrFromKPlusReceipt { action ->
            (action as ActionGetQrFromKPlusReceipt).setParam(currentContext, transData)
        }
        bind(State.SCAN_CODE.toString(), getQrFromKPlusReceiptAction)

        val enterIppTermAction = ActionInputTransData { action ->
            (action as ActionInputTransData).setParam(currentContext, title)
                    .setInputLine(getString(R.string.payment_terms), "1 - 99", EInputType.NUM, 2, 1, false)
        }
        bind(State.ENTER_IPP_TERM.toString(), enterIppTermAction, true)

        val amountAction = ActionEnterAmount { action ->
            (action as ActionEnterAmount).setParam(currentContext, title, false)
        }
        bind(State.ENTER_AMOUNT.toString(), amountAction, true)

        // confirm information
        val confirmInfoAction = ActionDispTransDetail { action ->
            val map: LinkedHashMap<String, String> = LinkedHashMap()
            map[getString(R.string.history_detail_type)] = Component.getTransByIPlanMode(transData)
            map[getString(R.string.history_detail_amount)] = CurrencyConverter.convert(Utils.parseLongSafe(transData.amount, 0), transData.currency)
            map[getString(R.string.payment_terms)] = transData.instalmentPaymentTerm.toString() + " months"
            if (isPromo) {
                map[getString(R.string.instalment_supplier_code)] = transData.skuCode
                map[getString(R.string.instalment_product_code)] = transData.productCode
                if (transData.instalmentSerialNo.isNotBlank())
                    map[getString(R.string.instalment_serial_number)] = transData.instalmentSerialNo
            }
            (action as ActionDispTransDetail).setParam(currentContext, title, map, FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID, false])
        }
        bind(State.CONFIRM_DETAILS.toString(), confirmInfoAction, true)

        // online action
        val transOnlineAction = ActionQrSaleInquiry { action ->
            (action as ActionQrSaleInquiry).setParam(currentContext, title, transData)
        }
        bind(State.MAG_ONLINE.toString(), transOnlineAction)

        val confirmInquiry = ActionCheckQRDolfinIpp { action ->
            (action as ActionCheckQRDolfinIpp).setParam(currentContext, title, transData)
        }
        bind(State.CONFIRM_INQUIRY.toString(), confirmInquiry)

        gotoState(nextState(State.ENTER_SUPPLIER))
    }


    override fun onActionResult(currentState: String?, result: ActionResult?) {
        Log.i(TAG, "onActionResult : $currentState")
        var next = ""

        when (currentState) {
            State.ENTER_AMOUNT.toString() -> {// 输入交易金额后续处理
                transData.amount = result?.data.toString()
                next = nextState(State.SCAN_CODE)
            }

            State.ENTER_IPP_TERM.toString() -> {
                (result?.data as? String)?.let {
                    transData.instalmentPaymentTerm = it.toInt()
                }
                if (transData.instalmentPaymentTerm > 0)
                    next = nextState(State.ENTER_AMOUNT)
            }

            State.ENTER_PRODUCT.toString() -> {
                (result?.data as? String)?.let {
                    transData.productCode = it
                }
                if (transData.productCode.isNotBlank())
                    next = nextState(State.ENTER_SN)
            }

            State.ENTER_SN.toString() -> {
                (result?.data as? String)?.let {
                    transData.instalmentSerialNo = it
                }
                next = nextState(State.ENTER_IPP_TERM)
            }

            State.ENTER_SUPPLIER.toString() -> {
                (result?.data as? String)?.let {
                    transData.skuCode = it
                }
                if (transData.skuCode.isNotBlank())
                    next = nextState(State.ENTER_PRODUCT)
            }

            State.SCAN_CODE.toString() -> {
                if (result?.ret == TransResult.SUCC) {
                    (result.data as? String)?.let {
                        transData.qrCode = it.trim()
                        transData.enterMode = EnterMode.QR
                        FinancialApplication.getAcqManager()?.let {
                            it.curAcq = it.findAcquirer(Constants.ACQ_DOLFIN_INSTALMENT)
                            transData.acquirer = it.curAcq
                            transData.issuer = it.findIssuer(Constants.ISSUER_DOLFIN)
                            transData.batchNo = it.curAcq.currBatchNo.toLong()
                            transData.tpdu = "600" + it.curAcq.nii + "0000"
                        }
                    }
                }
                if (!transData.qrCode.isNullOrEmpty())
                    next = nextState(State.CONFIRM_DETAILS)
            }

            State.CONFIRM_DETAILS.toString() -> {
                next = nextState(State.MAG_ONLINE)
            }

            State.CONFIRM_INQUIRY.toString() -> {
                if (result?.ret != TransResult.SUCC)
                    processInquiry = false
                next = nextState(State.MAG_ONLINE)
            }

            else -> {
                if (currentState == State.MAG_ONLINE.toString()) {
                    transData.transType = ETransType.DOLFIN_INSTALMENT

                    if (result?.ret == TransResult.SUCC) {
                        if (!BuildConfig.BUILD_TYPE.equals(Constants.BUILD_TYPE_RELEASE, ignoreCase = true) && transData.bytesField62 != null) {
                            transData.field63RecByte = transData.bytesField62 + transData.field63RecByte
                        }
                        val productInfo = ReservedFieldHandle.unpackReservedField(transData.field63RecByte, ReservedFieldHandle.dolfinIpp_f63_response, true)
                        productInfo?.get(ReservedFieldHandle.FieldTables.CARD_MASKING)?.let {
                            transData.pan = String(it).trim()
                        }
                    }

                    if (result?.ret == TransResult.QR_PROCESS_INQUIRY || result?.ret == TransResult.ERR_TIMEOUT) {
                        if (processInquiry) {
                            transData.transType = ETransType.DOLFIN_INSTALMENT_INQUIRY
                            next = nextState(State.CONFIRM_INQUIRY)
                        } else {
                            result?.ret = TransResult.ERR_USER_CANCEL
                        }
                    }
                }

                if (next == "") {
                    super.onActionResult(currentState, result)
                    return
                }
            }
        }

        if (next.isNotEmpty())
            gotoState(next)
        else
            transEnd(result)
    }
}
