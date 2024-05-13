package com.pax.pay.trans.action.activity

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pax.abl.core.ActionResult
import com.pax.appstore.DownloadManager
import com.pax.edc.BuildConfig
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.SettlementRegisterActivity
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.receipt.IReceiptGenerator
import com.pax.pay.utils.ControlLimitUtils
import com.pax.pay.utils.EReceiptUtils
import com.pax.settings.SysParam
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess
import th.co.bkkps.edc.recycleradapter.EDCParamRecyclerAdapter

class DisplayEDCParamActivity: BaseActivityWithTickForAction() {
    private var navTitle: String? = null
    private var navBack = false
    private var acqTleStatusList: MutableList<String>? = null
    private var acqEnableErmList: MutableList<String>? = null
    private var acqDisableErmList: MutableList<String>? = null

    private val mapValue = linkedMapOf<String, EDCTextViwInfo?>()
    private lateinit var btnConfirm: Button
    private lateinit var imgLogo: ImageView
    private lateinit var recyclerView: RecyclerView

    override fun getLayoutId(): Int {
        return R.layout.activity_display_edc_param
    }

    override fun initViews() {
        btnConfirm = findViewById(R.id.confirm_btn)
        imgLogo = findViewById(R.id.edc_param_logo)

        val logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME)
        imgLogo.setImageBitmap(logo)

        recyclerView = findViewById(R.id.edc_param_recyclerview)

        generateHeaderLayout()
        generateAcquirerConfigLayout()
        generateOtherCommSetting()
        generateOtherSetting()
        generateLinkPosConfig()
        generateTleStatus()
        generateEdcFileConfig()
        generateEReceiptConfig()
        generateEdcMenuConfig()

        val adapter = EDCParamRecyclerAdapter(mapValue)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun setListeners() {
        enableBackAction(navBack)
        btnConfirm.setOnClickListener(this)
    }

    override fun loadParam() {
        navTitle = intent.getStringExtra(EUIParamKeys.NAV_TITLE.toString())
        navBack = intent.getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false)
    }

    override fun getTitleString(): String {
        return navTitle!!
    }

    override fun onResume() {
        super.onResume()
        tickTimer.stop()
    }

    override fun onClickProtected(v: View?) {
        if (v!!.id == R.id.confirm_btn) {
            finish(ActionResult(TransResult.SUCC, null))
        }
    }

    override fun onKeyBackDown(): Boolean {
        finish(ActionResult(TransResult.ERR_USER_CANCEL, null))
        return true
    }

    private fun generateHeaderLayout() {
        val merName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN)
        val merAddress = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS)
        val merAddress1 = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1)
        mapValue["MER_NAME"] = EDCTextViwInfo(merName ?: "", IReceiptGenerator.FONT_SMALL.toFloat(), Gravity.CENTER)
        mapValue["MER_ADDR"] = EDCTextViwInfo(merAddress ?: "", IReceiptGenerator.FONT_SMALL.toFloat(), Gravity.CENTER)
        mapValue["MER_ADDR1"] = EDCTextViwInfo(merAddress1 ?: "", IReceiptGenerator.FONT_SMALL.toFloat(), Gravity.CENTER)
        mapValue["H_SINGLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_one_line), IReceiptGenerator.FONT_NORMAL_26.toFloat(), Gravity.CENTER)
        mapValue["SERIAL_NO"] = EDCTextViwInfo(" SERIAL NUMBER : ${DownloadManager.getInstance().sn}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(),
            Gravity.START
        )
        mapValue["APP_VERSION"] = EDCTextViwInfo(" APPLICATION VERSION : ${BuildConfig.VERSION_NAME}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(),
            Gravity.START
        )
        mapValue["H_SINGLE_LINE_2"] = EDCTextViwInfo(getString(R.string.receipt_one_line), IReceiptGenerator.FONT_NORMAL_26.toFloat(), Gravity.CENTER)
    }

    private fun generateAcquirerConfigLayout() {
        mapValue["ACQ_TITLE"] = EDCTextViwInfo("\n > ACQUIRER CONFIG", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["ACQ_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        val acquirers = FinancialApplication.getAcqManager().findEnableAcquirer()
        acquirers?.let {
            acqTleStatusList = mutableListOf()
            acqEnableErmList = mutableListOf()
            acqDisableErmList = mutableListOf()

            for (i in acquirers.indices) {
                val acq = acquirers[i]

                mapValue["ACQ_INFO1_$i"] = EDCTextViwInfo(combine2Units(
                    "${acq.nii}    ${acq.name}",
                    "STATUS : ${getStatus(acq.isEnable)}"
                ), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

                if (acq.isEnable) {
                    mapValue["ACQ_INFO2_$i"] = EDCTextViwInfo(
                        combine2Units("TID: ${acq.terminalId}", "MID: ${acq.merchantId}"),
                        IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                    )

                    mapValue["ACQ_INFO3_$i"] = EDCTextViwInfo(
                        combine2Units(
                            "TLE: ${getStatus(acq.isEnableTle)}", "ERCM: ${
                                if (acq.name == Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE
                                    || acq.name == Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE
                                ) {
                                    "N/A"
                                } else {
                                    getStatus(acq.isEnableUploadERM)
                                }
                            }"
                        ), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                    )

                    mapValue["ACQ_INFO4_$i"] = EDCTextViwInfo(
                        "${acq.name}  IP-ADDRESS :-",
                        IReceiptGenerator.FONT_SMALL_16.toFloat(),
                        Gravity.START
                    )

                    acq.ip?.let {
                        mapValue["ACQ_INFO5_$i"] = EDCTextViwInfo(
                            "\t\tPRIMARY     : $it : ${acq.port}",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(),
                            Gravity.START
                        )
                    }
                    acq.ipBak1?.let {
                        mapValue["ACQ_INFO6_$i"] = EDCTextViwInfo(
                            "\t\tBACKUP [1] : $it : ${acq.portBak1}",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(),
                            Gravity.START
                        )
                    }
                    acq.ipBak2?.let {
                        mapValue["ACQ_INFO7_$i"] = EDCTextViwInfo(
                            "\t\tBACKUP [2] : $it : ${acq.portBak2}",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(),
                            Gravity.START
                        )
                    }
                    acq.ipBak3?.let {
                        mapValue["ACQ_INFO8_$i"] = EDCTextViwInfo(
                            "\t\tBACKUP [3] : $it : ${acq.portBak3}",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(),
                            Gravity.START
                        )
                    }

                    if (acq.ip == null && acq.ipBak1 == null
                        && acq.ipBak2 == null && acq.ipBak3 == null
                    ) {
                        mapValue["ACQ_INFO9_$i"] = EDCTextViwInfo(
                            " === NO IP-ADDRESS LIST ===",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(),
                            Gravity.START
                        )
                    }

                    mapValue["ACQ_INFO10_$i"] = EDCTextViwInfo(
                        combine2Units(
                            "Enable Settle : ${SettlementRegisterActivity.isEnableSettleMode()}",
                            "Time : ${acq.settleTime ?: "-"}"
                        ), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                    )

                    if (SettlementRegisterActivity.isEnableSettleMode()) {
                        mapValue["ACQ_INFO11_$i"] = EDCTextViwInfo(
                            "Mode : ${
                                SettleAlarmProcess.SettlementMode.getMode(
                                    SettlementRegisterActivity.getEDCSettlementMode()
                                )
                            }",
                            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                        )
                    }

                    mapValue["ACQ_INFO12_$i"] = EDCTextViwInfo(
                        "last-detect(Auto/Force)Settle on : ${acq.latestSettledDateTime ?: "-"}",
                        IReceiptGenerator.FONT_SMALL_16.toFloat(),
                        Gravity.START
                    )

                    mapValue["ACQ_INFO13_$i"] = EDCTextViwInfo(
                        "CtrlLimit : ${
                            getStatus(
                                ControlLimitUtils.isSupportControlLimitHost(acq.name)
                            )
                        }", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                    )
                    mapValue["ACQ_INFO14_$i"] = EDCTextViwInfo(
                        "Phone input : ${
                            getStatus(
                                ControlLimitUtils.isAllowEnterPhoneNumber(acq.name)
                            )
                        }", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START
                    )
                    mapValue["ACQ_SINGLE_LINE_$i"] = EDCTextViwInfo(
                        getString(R.string.receipt_one_line),
                        IReceiptGenerator.FONT_NORMAL_26.toFloat(),
                        Gravity.CENTER
                    )

                    if (acq.isEnableTle) {
                        val tleStatus = if (acq.tmk != null && acq.twk != null) {
                            "${acq.name} = TRUE"
                        } else {
                            "${acq.name} = FALSE"
                        }
                        acqTleStatusList!!.add(tleStatus)
                    }

                    if (acq.name != Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE
                        && acq.name != Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE) {
                        if (acq.isEnableUploadERM) {
                            acqEnableErmList!!.add(acq.name)
                        } else {
                            acqDisableErmList!!.add(acq.name)
                        }
                    }
                }
            }
        }
    }

    private fun generateOtherCommSetting() {
        mapValue["OTH_COMM_TITLE"] = EDCTextViwInfo("\n > OTHER COMMUNICATION SETTING", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_COMM_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        mapValue["OTH_COMM_APN"] = EDCTextViwInfo("APN : ${FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN)}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
    }

    private fun generateOtherSetting() {
        mapValue["OTH_TITLE"] = EDCTextViwInfo("\n > OTHER SETTINGS", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        mapValue["OTH_SP200"] = EDCTextViwInfo(combine2Units("SUPPORT SP200 : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_VOID_STAN"] = EDCTextViwInfo(combine2Units("VOID BY STAN NO. : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_KIOSK_MODE"] = EDCTextViwInfo(combine2Units("ENABLE KIOSK MODE : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_KIOSK_MODE))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_PRN_GRAND"] = EDCTextViwInfo(combine2Units("PRINT GRAND TOTAL : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_PRN_QR_BARCODE"] = EDCTextViwInfo(combine2Units("PRINT QR/BARCODE : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["OTH_PRN_BARCODE_COD"] = EDCTextViwInfo(combine2Units("PRINT BARCODE COD : ", getStatus(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_COD))),
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
    }

    private fun generateLinkPosConfig() {
        mapValue["LNK_POS_TITLE"] = EDCTextViwInfo("\n > LINKPOS CONFIG", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["LNK_POS_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        var tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME)
        tmpStr?.let { merc ->
            mapValue["LNK_POS_MERC"] = EDCTextViwInfo(combine2Units("LINKPOS MERCHANT  :  ", merc), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL)
            tmpStr?.let {
                mapValue["LNK_POS_PROTOCOL"] = EDCTextViwInfo(combine2Units("PROTOCOL  :  ", it), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            }

            mapValue["LNK_POS_PRN_OPTIONS"] = EDCTextViwInfo("1. Print options (LinkPOS mode):-", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            var tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE, false)
            mapValue["LNK_POS_SETTLE_REPORT"] = EDCTextViwInfo(combine2Units("   1.1 Settlement report  :  ", getStatus(tmpBool)), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE, false)
            mapValue["LNK_POS_AUDIT_REPORT"] = EDCTextViwInfo(combine2Units("   1.2 Audit Report  :  ", getStatus(tmpBool)), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            mapValue["LNK_POS_BYPASS"] = EDCTextViwInfo("2. Confirm Dialog Bypass (LinkPOS mode):-", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE, false)
            mapValue["LNK_POS_BYPASS_SETTLE"] = EDCTextViwInfo(combine2Units("   2.1 SETTLEMENT :  ", getStatus(tmpBool)), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

            tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID, false)
            mapValue["LNK_POS_BYPASS_VOID"] = EDCTextViwInfo(combine2Units("   2.2 VOID :  ", getStatus(tmpBool)), IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        } ?: run {
            mapValue["LNK_POS_STATUS"] = EDCTextViwInfo("LINKPOS STATUS: Disable", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        }
    }

    private fun generateTleStatus() {
        mapValue["TLE_TITLE"] = EDCTextViwInfo("\n > TLE DOWNLOAD STATUS", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["TLE_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        acqTleStatusList?.let { it ->
            for (i in it.indices) {
                mapValue["TLE_STATUS_$i"] = EDCTextViwInfo(it[i], IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            }
        }
    }

    private fun generateEdcFileConfig() {
        mapValue["FILE_CONFIG_TITLE"] = EDCTextViwInfo("\n > EDC FILE CONFIG UPLOAD", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["FILE_CONFIG_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        var fileStatus: Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_AID_FILE_UPLOAD_STATUS)
        mapValue["FILE_CONFIG_AID"] = EDCTextViwInfo("AID: ${getFileUploadStatus(fileStatus)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        fileStatus = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_CARD_RANGE_FILE_UPLOAD_STATUS)
        mapValue["FILE_CONFIG_CARD_RANGE"] = EDCTextViwInfo("CARD RANGE: ${getFileUploadStatus(fileStatus)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        fileStatus = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_ISSUER_FILE_UPLOAD_STATUS)
        mapValue["FILE_CONFIG_ISSUER"] = EDCTextViwInfo("ISSUER: ${getFileUploadStatus(fileStatus)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
    }

    private fun generateEReceiptConfig() {
        mapValue["ERECEIPT_CONFIG_TITLE"] = EDCTextViwInfo("\n > E-RECEIPT CONFIG", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["ERECEIPT_CONFIG_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        var tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE, false)
        mapValue["ERECEIPT_CONFIG_ENABLE"] = EDCTextViwInfo(" E-SIGNATURE ENABLE\t: ${getStatusOnOff(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE, false)
        mapValue["ERECEIPT_CONFIG_ERCM"] = EDCTextViwInfo(" ERCM ENABLE\t\t\t\t: ${getStatusOnOff(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        var tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED)
        tmpStr = tmpStr?.let {
            "YES"
        } ?: run {
            val strPublicKey = if (!EReceiptUtils.isFoundKbankPublicKeyFile()) {
                "(missing PBK file)"
            } else {
                ""
            }
            "NO $strPublicKey"
        }
        mapValue["ERECEIPT_CONFIG_INIT_STATUS"] = EDCTextViwInfo(" INITIAL STATUS\t\t\t\t: $tmpStr", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        mapValue["ERECEIPT_CONFIG_ERM_SETTING"] = EDCTextViwInfo(" Details of ERM settings", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["ERECEIPT_CONFIG_ERM_INIT_INFO"] = EDCTextViwInfo("   1. ERM Initial information", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE)
        mapValue["ERECEIPT_CONFIG_ERM_INIT_INFO_BANK"] = EDCTextViwInfo("         1.1 BANK : ${getStringParam(tmpStr)}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE)
        mapValue["ERECEIPT_CONFIG_ERM_INIT_INFO_MERC"] = EDCTextViwInfo("         1.2 MERC : ${getStringParam(tmpStr)}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE)
        mapValue["ERECEIPT_CONFIG_ERM_INIT_INFO_STORE"] = EDCTextViwInfo("         1.3 STORE : ${getStringParam(tmpStr)}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION)
        mapValue["ERECEIPT_CONFIG_ERM_INIT_INFO_KEY_VER"] = EDCTextViwInfo("         1.4 KEY.VER : ${getStringParam(tmpStr)}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRN"] = EDCTextViwInfo("   2. ERM PAPER-RECEIPT PRINT", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRN_ON_SUCC"] = EDCTextViwInfo("         2.1 ON UPLOAD SUCCESS : ${numberOfReceipt(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP))}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRN_ON_FAIL"] = EDCTextViwInfo("         2.2 ON UPLOAD FAIL : ${numberOfReceipt(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD))}",
            IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD, false)
        mapValue["ERECEIPT_CONFIG_NEXT_TXN"] = EDCTextViwInfo("   3. NEXT TRANSACTION UPLOAD : ${getStatusOnOff(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRE_SETT_PRN"] = EDCTextViwInfo("   4. ERM PRE-SETTLEMENT PRINTING", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE, false)
        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRE_SETT_PRN_ON_FAIL"] = EDCTextViwInfo("         4.1 ON UPLOAD FAILED : ${getStatus(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS, false)
        tmpStr = if (tmpBool) {
            "PRINT ALL"
        } else {
            "ONLY NEVER PRINT"
        }
        mapValue["ERECEIPT_CONFIG_ERM_RECEIPT_PRE_SETT_PRN_PENDING_LIST"] = EDCTextViwInfo("         4.2 PENDING LIST : $tmpStr", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        acqEnableErmList?.let { it ->
            mapValue["ERECEIPT_CONFIG_ENABLE_UPLOAD"] = EDCTextViwInfo("------ERM ENABLED UPLOAD LIST------", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            for (i in it.indices) {
                tmpStr = "${i+1}.${it[i]}"
                mapValue["ERECEIPT_CONFIG_ENABLE_UPLOAD_$i"] = EDCTextViwInfo("\t\t$tmpStr", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            }
        }

        acqDisableErmList?.let { it ->
            mapValue["ERECEIPT_CONFIG_DISABLE_UPLOAD"] = EDCTextViwInfo("-----ERM DISABLE UPLOAD LIST-----", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            for (i in it.indices) {
                tmpStr = "${i+1}.${it[i]}"
                mapValue["ERECEIPT_CONFIG_DISABLE_UPLOAD_$i"] = EDCTextViwInfo("\t\t$tmpStr", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
            }
        }
    }

    private fun generateEdcMenuConfig() {
        mapValue["MENU_CONFIG_TITLE"] = EDCTextViwInfo("\n > EDC SHOW/HIDE MENU", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
        mapValue["MENU_CONFIG_DOUBLE_LINE_1"] = EDCTextViwInfo(getString(R.string.receipt_double_line), IReceiptGenerator.FONT_SMALL_19.toFloat(), Gravity.CENTER)

        var tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SALE_CREDIT_MENU)
        mapValue["MENU_CONFIG_SALE_ALL_CREDIT"] = EDCTextViwInfo("  1. SALE ALL CREDIT\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_MENU)
        mapValue["MENU_CONFIG_SALE_ALIPAY"] = EDCTextViwInfo("  2. SALE ALIPAY\t\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WECHAT_MENU)
        mapValue["MENU_CONFIG_SALE_WECHAT"] = EDCTextViwInfo("  3. SALE WECHAT\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KPLUS_MENU)
        mapValue["MENU_CONFIG_SALE_KPLUS"] = EDCTextViwInfo("  4. SALE THAIQR\t\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_CREDIT_MENU)
        mapValue["MENU_CONFIG_SALE_QR_CREDIT"] = EDCTextViwInfo("  5. SALE QRCREDIT\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_MENU)
        mapValue["MENU_CONFIG_SALE_DOLFIN"] = EDCTextViwInfo("  6. SALE DOLFIN\t\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SMART_PAY_MENU)
        mapValue["MENU_CONFIG_SALE_SMARTPAY"] = EDCTextViwInfo("  7. SALE SMATPAY\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_REDEEM_MENU)
        mapValue["MENU_CONFIG_SALE_REDEEM"] = EDCTextViwInfo("  8. SALE POINT REWARD\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SCB_IPP_MENU)
        mapValue["MENU_CONFIG_SCB_IPP"] = EDCTextViwInfo("  9. SALE SCB IPP\t\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SCB_REDEEM_MENU)
        mapValue["MENU_CONFIG_SCB_REDEEM"] = EDCTextViwInfo("  10. SALE SCB REDEEM\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_AMEX_EPP_MENU)
        mapValue["MENU_CONFIG_AMEX_EPP"] = EDCTextViwInfo(" 11. SALE AMEX EPP\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CT1_EPP_MENU)
        mapValue["MENU_CONFIG_CT1_EPP"] = EDCTextViwInfo(" 12. SALE CT1 EPP\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)

        tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_MENU)
        mapValue["MENU_CONFIG_VOID"] = EDCTextViwInfo(" 13. VOID\t\t\t\t\t\t\t\t\t= ${getStatusShowHide(tmpBool)}", IReceiptGenerator.FONT_SMALL_16.toFloat(), Gravity.START)
    }

    data class EDCTextViwInfo(val text: String, val textSize: Float, val textAlignment: Int)

    private fun getStatus(flag: Boolean): String {
        return if (flag) {
            "Enable"
        } else {
            "Disable"
        }
    }

    private fun getStatusOnOff(flag: Boolean): String {
        return if (flag) {
            "ON"
        } else {
            "OFF"
        }
    }

    private fun getStatusShowHide(flag: Boolean): String {
        return if (flag) {
            "SHOW"
        } else {
            "HIDE"
        }
    }

    private fun getStringParam(paramValue: String?): String {
        return paramValue ?: "-"
    }

    private fun getFileUploadStatus(result: Int): String {
        return when (result) {
            0 -> "File Uploaded."
            1 -> "Upload Failed."
            else -> "No File Uploaded."
        }
    }

    private fun numberOfReceipt(value: Int): String {
        return when (value) {
            0 -> "disable print"
            1 -> "Mer. only"
            2 -> "Mer. + Cus."
            3 -> "Cus. only"
            else -> ""
        }
    }

    private fun combine2Units(unit1: String, unit2: String): String {
        val maxPerLine: Int = 45
        val totalLength = unit1.length + unit2.length
        val spaces = maxPerLine - totalLength
        return if (spaces > 0) {
            "$unit1${"".padStart(spaces, ' ')}$unit2"
        } else {
            "$unit1$unit2"
        }
    }
}