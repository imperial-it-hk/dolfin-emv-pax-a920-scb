package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.settings.SysParam
import org.json.JSONObject
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.utils.Log

class KCheckIDConfigurationActivity : Activity() {

    companion object{
        const val TAG = "KCheckIDConfig"
    }

    lateinit var transAPI: ITransAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI =  TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        val reqMsg = ConfigurationMsg.Companion.Request()
        val jsonParam : String? = buildJsonParam()
        jsonParam?.let {
            reqMsg.setJsonSetting("?$it")
            transAPI.startTrans(this, reqMsg)
        }?:run{
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val resp = transAPI.onResult(requestCode, resultCode, data) as ConfigurationMsg.Companion.Response?

        resp?.let {
            if (it.getApiResultCode() == 0) {
                extractJsonData(it.getResponseMessageEN())
                finish(ActionResult(TransResult.SUCC, null))
            } else {
                finish(ActionResult(TransResult.ERR_PARAM, null))
            }
        }?:run{
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }

    fun extractJsonData(jsonConfig: String?)  {
        val jsonObj : JSONObject? = JSONObject(jsonConfig)
        val acq : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(com.pax.pay.constant.Constants.ACQ_KCHECKID)
        jsonObj?.let { cfg ->
            acq?.apiDomainName = cfg.getString("serviceEndPointIpaddr")
            acq?.apiPortNumber = cfg.getString("serviceEndPointPort").toInt()
            acq?.apiHostNameCheck = !cfg.getBoolean("serviceEndPointBypassHostNameCheck")
            acq?.apiCertificationCheck = !cfg.getBoolean("serviceEndPointBypassCertCheck")

            acq?.terminalId = cfg.getString("terminalID")
            acq?.merchantId = cfg.getString("merchantID")

            val sslEnabled = cfg.getBoolean("serviceEndPointEnabledSSL")
            acq?.sslType = if (sslEnabled) SysParam.Constant.CommSslType.SSL else SysParam.Constant.CommSslType.NO_SSL
            acq?.enableUploadERM = cfg.getBoolean("ercm_enable_flag")
            acq?.eKycDataEncryption = cfg.getBoolean("enableCardInfoEncryption")

            // --- T/O ---
            acq?.apiConnectTimeout =cfg.getInt("serviceEndPointConnectTimeout")
            acq?.apiReadTimeout =cfg.getInt("serviceEndPointReadTimeout")
            acq?.apiScreenTimeout =cfg.getInt("screenTimeoutSec")

            FinancialApplication.getAcqManager().updateAcquirer(acq)
        }?:run{
            Log.e(TAG, "Unable to cast jsonConfig to json object")
        }
    }

    fun buildJsonParam() : String? {
        val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KCHECKID)
        val transData = TransData()
        if (!acquirer.isEnable) {
            return null
        } else {
            Component.transInit(transData)
            transData.acquirer = acquirer
            transData.transType = ETransType.KCHECKID_DUMMY
        }

        val edc_config_receipt_num : Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM,0)
        val ercm_onSuccess_receipt_num : Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP,0)
        val ercm_onFailed_receipt_num : Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD,0)
        val non_ercm_receipt_print_merc : Int = if ( edc_config_receipt_num == 2) 1 else 0
        val non_ercm_receipt_print_cust : Int = if ( edc_config_receipt_num == 2 || edc_config_receipt_num == 1 ) 1 else 0

        // ERM RECEIPT PRINT NUMBER
        //    0 = NONE
        //    1 = MERCHANT COPY ONLY
        //    2 = MERCHANT + CUSTOMER COPY
        //    3 = CUSTOMER COPY ONLY
        val ercm_succ_receipt_print_merc : Int = if ( ercm_onSuccess_receipt_num == 1 || ercm_onSuccess_receipt_num == 2) 1 else 0
        val ercm_succ_receipt_print_cust : Int = if ( ercm_onSuccess_receipt_num == 2 || ercm_onSuccess_receipt_num == 3 ) 1 else 0
        val ercm_fail_receipt_print_merc : Int = if ( ercm_onFailed_receipt_num == 1 || ercm_onFailed_receipt_num == 2) 1 else 0
        val ercm_fail_receipt_print_cust : Int = if ( ercm_onFailed_receipt_num == 2 || ercm_onFailed_receipt_num == 3 ) 1 else 0
        var kcheckid_disp_language : String? = "THAI"

        val pref = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp())
        if (pref.contains("ENGLISH")) {
            kcheckid_disp_language = pref.getString("ENGLISH", "THAI")
        }

        val configObject = com.alibaba.fastjson.JSONObject()

        // this config may depend-on edc-current-language-setting
        configObject["language"] = kcheckid_disp_language

        // these setting depend-on parameter from acquirer config
        configObject["ip"] = acquirer.apiDomainName
        configObject["port"] = acquirer.apiPortNumber
        configObject["connect_timeout"] = if (acquirer.apiConnectTimeout <= 0) 5 else acquirer.apiConnectTimeout        // default  5 sec.
        configObject["read_timeout"] = if (acquirer.apiReadTimeout <= 0) 30 else acquirer.apiReadTimeout                // default 30 sec.
        configObject["enable_ssl"] = if (acquirer.sslType == SysParam.Constant.CommSslType.SSL) true else false
        configObject["enable_bypass_chk_name"] = !acquirer.apiHostNameCheck
        configObject["enable_bypass_chk_cert"] = !acquirer.apiCertificationCheck
        configObject["terminal_id"] = acquirer.terminalId
        configObject["merchant_id"] = acquirer.merchantId
        configObject["timeout"] = if (acquirer.apiScreenTimeout <= 0) 60 else acquirer.apiScreenTimeout                // default 60 sec.

        // these settings depend-on EDC Erm configuration
        configObject["enable_ercm_upload"] = Component.isAllowSignatureUpload(transData)
        configObject["merc_non_ercm_receipt_num"] = non_ercm_receipt_print_merc
        configObject["cust_non_ercm_receipt_num"] = non_ercm_receipt_print_cust
        configObject["merc_on_success_receipt_num"] = ercm_succ_receipt_print_merc
        configObject["cust_on_success_receipt_num"] = ercm_succ_receipt_print_cust
        configObject["merc_on_failed_receipt_num"] = ercm_fail_receipt_print_merc
        configObject["cust_on_failed_receipt_num"] = ercm_fail_receipt_print_cust

        // these setting use fix-code
        configObject["merc_name"] = "ธนาคารกสิกรไทย จำกัด(มหาชน)"
        configObject["merc_addr_1"] = "ใบบันทึกรายการ"
        configObject["merc_addr_2"] = " "
        configObject["receipt_dscmr_ln_1"] = "** ติดต่อ Call Center"
        configObject["receipt_dscmr_ln_2"] = "โทร. 0-2888-8888"
        configObject["receipt_dscmr_ln_3"] = "โปรดตรวจสอบรายการเพื่อความถูกต้อง"
        configObject["receipt_dscmr_ln_4"] = "*เอกสารสำคัญโปรดเก็บไว้เป็นหลักฐานชำระ*"
        configObject["receipt_dscmr_erm_upload_failed"] = this.getString(R.string.footer_ereceipt_upload_not_success)
        configObject["enable_disp_chd_info"] = false
        configObject["enable_show_reprint_button"] = false
        configObject["enable_show_personal_infos"] = false
        configObject["enable_show_only_succ_trans"] = true
        configObject["enable_chd_encryption"] = true

        return configObject.toString()
    }
}