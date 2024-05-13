package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import com.alibaba.fastjson.JSONObject
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.edc.kbank.bpskcheckidapi.BaseRequest
import com.pax.edc.kbank.bpskcheckidapi.ITransAPI
import com.pax.edc.kbank.bpskcheckidapi.TransAPIFactory
import com.pax.eemv.entity.Config
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.settings.SysParam
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.utils.Log

class KCheckIDSettingActivity() : Activity() {

    companion object {
        const val TAG = "KCheckIDSettingActivity"
    }

    lateinit var transAPI : ITransAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        val jsonString: String? = buildJsonParam()
        jsonString?:run {
            Log.d(TAG, "onCreate --- faile to create jsonConfigString")
            finish(ActionResult(TransResult.ERR_PROCESS_FAILED, null))
            return
        }

        val settingReq = ConfigurationMsg.Companion.Request()
        settingReq.setJsonSetting(jsonString)
        transAPI.startTrans(this, settingReq as BaseRequest)
        Log.d(TAG, "onCreate --- startTransCalled")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivity Result [RequestCode=$requestCode, Result=$resultCode] ")
        try {
            val resp : ConfigurationMsg.Companion.Response? = transAPI.onResult(requestCode, resultCode, data) as ConfigurationMsg.Companion.Response?

            resp?.let {
                Log.d(TAG, "onActivityResult --- setting sessionId is '${it.getSessionID()}'")
                finish(ActionResult(it.getApiResultCode(), it.getSessionID()))
            }?:run{
                Log.d(TAG, "onActivityResult --- responseData is null")
                finish(ActionResult(TransResult.ERR_PARAM, null))
            }
        } catch (e: Exception) {
            Log.d(TAG, "onActivityResult --- internal error during process the result")
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }
    }



    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) {
                action.setResult(result)
                return
            }
            action.isFinished = true
            action.setResult(result)
        }
        finish()
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
        val ercm_succ_receipt_print_merc : Int = if ( ercm_onSuccess_receipt_num == 1 || ercm_onSuccess_receipt_num == 2 ) 1 else 0
        val ercm_succ_receipt_print_cust : Int = if ( ercm_onSuccess_receipt_num == 2 || ercm_onSuccess_receipt_num == 3 ) 1 else 0
        val ercm_fail_receipt_print_merc : Int = if ( ercm_onFailed_receipt_num == 1 || ercm_onFailed_receipt_num == 2) 1 else 0
        val ercm_fail_receipt_print_cust : Int = if ( ercm_onFailed_receipt_num == 2 || ercm_onFailed_receipt_num == 3 ) 1 else 0
        var kcheckid_disp_language : String? = "THAI"

        val pref = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp())
        if (pref.contains("ENGLISH")) {
            kcheckid_disp_language = pref.getString("ENGLISH", "THAI")
        }

        val configObject = JSONObject()

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