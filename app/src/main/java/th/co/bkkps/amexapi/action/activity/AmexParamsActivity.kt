package th.co.bkkps.amexapi.action.activity

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransAPIFactory
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.utils.EReceiptUtils
import com.pax.settings.SysParam
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.bps_amexapi.ITransAPI
import th.co.bkkps.bps_amexapi.SettingMsg
import th.co.bkkps.utils.Log
import java.io.File
import java.io.InputStream

class AmexParamsActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiProcess?.apply {
            try {
                if (this.isAppInstalled(this@AmexParamsActivity)) {
                    Log.i(AmexAPIConstants.TAG, "Amex app Installed.")
                    val paramsJson = generateParamAsJsonString()
                    this.doUpdateParams(this@AmexParamsActivity, paramsJson!!)
                }
                else {
                    Log.e(AmexAPIConstants.TAG, "Amex app not installed.")
                    onActivityResult(AmexAPIConstants.REQUEST_PARAM_SETTING, TransResult.ERR_AMEX_APP_NOT_INSTALLED, null)
                }
            }
            catch (ex: Exception) {
                Log.e(AmexAPIConstants.TAG, "Generate params or update params fail", ex)
                onActivityResult(AmexAPIConstants.REQUEST_PARAM_SETTING, TransResult.ERR_AMEX_PARAM_UPDATE_FAIL, null)
            }
        } ?: run {
            Log.e(AmexAPIConstants.TAG, "Unable to create Amex API instance")
            onActivityResult(AmexAPIConstants.REQUEST_PARAM_SETTING, TransResult.ERR_AMEX_PARAM_UPDATE_FAIL, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AmexAPIConstants.REQUEST_PARAM_SETTING) {
            finish(ActionResult(resultCode, AmexAPIConstants.REQUEST_PARAM_SETTING))
        }
        else {
            val response = apiProcess.transAPI.onResult(requestCode, resultCode, data) as SettingMsg.Response?
            response?.let {
                Log.i(AmexAPIConstants.TAG, "PARAM response received")
                Log.d(AmexAPIConstants.TAG, "getRspCode=${it.rspCode}")
                finish(ActionResult(it.rspCode, AmexAPIConstants.REQUEST_PARAM_SETTING))
            } ?: run {
                Log.e(AmexAPIConstants.TAG, "PARAM response not received")
                finish(ActionResult(TransResult.ERR_PARAM, AmexAPIConstants.REQUEST_PARAM_SETTING))
            }
        }
    }

    fun finish(result: ActionResult?) {
//        val action = TransContext.getInstance().currentAction
//        action?.apply {
//            this.setResult(result)
//        }
        setResult(result!!.ret)
        finish()
    }

    private fun generateParamAsJsonString(): String? {
        val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
        acquirer?.let {
            if (it.isEnable) {
                val jsonArray = JSONArray()

                val commObject = JSONObject()
                commObject["type"] = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE)
                commObject["timeout"] = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT)
                commObject["apn"] = FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM)

                val edcObject = JSONObject()
                edcObject["merchantName"] = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN)
                edcObject["merchantAdd"] = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS)
                edcObject["merchantAdd1"] = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1)
                edcObject["receiptNo"] = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM)
                edcObject["stanNo"] = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO)
                edcObject["traceNo"] = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO)
                edcObject["language"] = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_LANGUAGE)
                edcObject["voidWithStan"] = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)
                edcObject["enableCtls"] = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)
                edcObject["enableKeyIn"] = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KEYIN)
                edcObject["enableSp200"] = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200)


                val edc_config_receipt_num = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM, 2)
                val ercm_onSuccess_receipt_num : Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP,0)
                val ercm_onFailed_receipt_num : Int = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD,0)
                val standalone_receipt_print_merc : Int = if ( edc_config_receipt_num == 2) 1 else 0
                val standalone_receipt_print_cust : Int = if ( edc_config_receipt_num == 2 || edc_config_receipt_num == 1 ) 1 else 0

                // ERM RECEIPT PRINT NUMBER
                //    0 = NONE
                //    1 = MERCHANT COPY ONLY
                //    2 = MERCHANT + CUSTOMER COPY
                //    3 = CUSTOMER COPY ONLY
                var ercm_succ_receipt_print_merc : Int = if ( ercm_onSuccess_receipt_num == 1 || ercm_onSuccess_receipt_num == 2) 1 else 0
                var ercm_succ_receipt_print_cust : Int = if ( ercm_onSuccess_receipt_num == 2 || ercm_onSuccess_receipt_num == 3 ) 1 else 0
                var ercm_fail_receipt_print_merc : Int = if ( ercm_onFailed_receipt_num == 1 || ercm_onFailed_receipt_num == 2) 1 else 0
                var ercm_fail_receipt_print_cust : Int = if ( ercm_onFailed_receipt_num == 2 || ercm_onFailed_receipt_num == 3 ) 1 else 0
                val ercm_enable_esig_flag = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE, false)
                val ercm_enable_upload_flag = (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE, false) && it.enableUploadERM)

                val amexAcquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX)
                val eReceiptAction = ActionEReceiptInfoUpload(null)
                if (!ercm_enable_esig_flag || !ercm_enable_upload_flag || !eReceiptAction.isErmReadyForUpload(amexAcquirer)) {
                    ercm_fail_receipt_print_merc = standalone_receipt_print_merc
                    ercm_fail_receipt_print_cust = standalone_receipt_print_cust
                    ercm_succ_receipt_print_merc = standalone_receipt_print_merc
                    ercm_succ_receipt_print_cust = standalone_receipt_print_cust
                }


                val acqObject = JSONObject()
                acqObject["name"] = it.name
                acqObject["terminalId"] = it.terminalId
                acqObject["merchantId"] = it.merchantId
                acqObject["nii"] = it.nii
                acqObject["ip"] = it.ip
                acqObject["port"] = it.port
                acqObject["ipBak1"] = it.ipBak1
                acqObject["portBak1"] = it.portBak1
                acqObject["ipBak2"] = it.ipBak2
                acqObject["portBak2"] = it.portBak2
                acqObject["ipBak3"] = it.ipBak3
                acqObject["portBak3"] = it.portBak3
                acqObject["isEnableTle"] = it.isEnableTle
                acqObject["isEnableKeyIn"] = it.isEnableKeyIn
                acqObject["enableErmUpload"] = ercm_enable_upload_flag
                acqObject["enableESignature"] = ercm_enable_esig_flag
                acqObject["numberOfReceiptStandAloneCust"] = standalone_receipt_print_cust
                acqObject["numberOfReceiptStandAloneMerc"] = standalone_receipt_print_merc
                acqObject["numberOfReceiptErmUploadSuccessCust"] = ercm_succ_receipt_print_cust
                acqObject["numberOfReceiptErmUploadSuccessMerc"] = ercm_succ_receipt_print_merc
                acqObject["numberOfReceiptErmUploadFailedCust"] = ercm_fail_receipt_print_cust
                acqObject["numberOfReceiptErmUploadFailedMerc"] = ercm_fail_receipt_print_merc
                acqObject["disclaimerErmFailedText"] = getString(R.string.footer_ereceipt_upload_not_success)
                acqObject["ermHeaderLogo"] = getPrintingHeaderLogo(acquirer.name, acquirer.nii)


                val jObject = JSONObject()
                jObject["comm"] = commObject
                jObject["edc"] = edcObject
                jObject["acquirer"] = acqObject

                jsonArray.add(jObject)

                return jsonArray.toString()
            }
        }
        return null
    }

    fun getPrintingHeaderLogo(acqName: String, nii : String) : ByteArray? {
        val ermPaxLogoPath : String? = EReceiptUtils.getERM_LogoDirectory(this);
        ermPaxLogoPath?.let {
            val ermDownloadLogoDirectory : File? = File(ermPaxLogoPath)
            ermDownloadLogoDirectory?.let{
                if (it.exists() && it.isDirectory) {
                    val hostfileName = "${File.separator}${nii}_${acqName}.bmp"
                    val hostLogoFile :File? = File("${ermPaxLogoPath}${hostfileName}")

                    hostLogoFile?.let{
                        if (it.exists() && it.isFile) {
                            return it.readBytes()
                        }
                    }

                    val deviceFileName = "${File.separator}receipt_logo.bmp"
                    val deviceLogoFile :File? = File("${ermPaxLogoPath}${deviceFileName}")
                    deviceLogoFile?.let{
                        if (it.exists() && it.isFile) {
                            return it.readBytes()
                        }
                    }
                }
            }
        }

        val logoStream : InputStream? = FinancialApplication.getApp().resources.assets.open("kBank_Default.bmp")
        logoStream?.let {
            return it.readBytes()
        }

        return null
    }
}