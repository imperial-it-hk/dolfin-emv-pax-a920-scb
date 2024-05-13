package com.pax.pay.trans.model

import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.base.MerchantAcqProfile
import com.pax.pay.base.MerchantProfile
import com.pax.pay.constant.Constants
import com.pax.pay.db.MerchantAcqProfileDb
import com.pax.pay.db.MerchantProfileDb
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils

object MerchantProfileManager {

    //
    // List of ACQ that support Multi Merchant
    //
    private val listAcq = listOf(Constants.ACQ_KBANK,
        Constants.ACQ_UP,
        Constants.ACQ_DCC,
        Constants.ACQ_SMRTPAY,
        Constants.ACQ_REDEEM,
        Constants.ACQ_KPLUS,
        Constants.ACQ_MY_PROMPT,
        Constants.ACQ_ALIPAY,
        Constants.ACQ_ALIPAY_B_SCAN_C,
        Constants.ACQ_WECHAT,
        Constants.ACQ_WECHAT_B_SCAN_C,
        Constants.ACQ_QR_CREDIT)
        //Constants.ACQ_AMEX, //not support as per TPK issue)

    private var currentMerchant = MerchantProfile()

    fun isMultiMerchantEnable(): Boolean {
        return MerchantProfileDb.findAllData()?.size!! > 1
    }

    fun updateTerminalIDAndMerchantID(acquerName: String,  TID: String, MID : String): Boolean {
        var mercAcquirerProfile = getSpecificAcquirerFromMerchantName(currentMerchant.merchantLabelName, acquerName)
        mercAcquirerProfile?.let {
            it.terminalId = TID
            it.merchantId = MID

            return MerchantAcqProfileDb.updateTerminalIDMerchantID(it)
        }?:run{
            return false
        }
    }

    fun getAllMerchant(): List<MerchantProfile>?{
        return MerchantProfileDb.findAllData()
    }

    fun getAllMerchant(ascMode: Boolean): List<MerchantProfile>?{
        return MerchantProfileDb.findAllData(ascMode)
    }

    fun getAcqFromMerchantName(merchantName : String): List<MerchantAcqProfile>?{
        return MerchantAcqProfileDb.findAcqFromMerchant(merchantName, null)
    }

    fun getSpecificAcquirerFromMerchantName(merchantName : String, acquerName: String?): MerchantAcqProfile?{
        val listMerchantAcqProfile = MerchantAcqProfileDb.findAcqFromMerchant(merchantName, acquerName)
        return (if (listMerchantAcqProfile.isNullOrEmpty() || listMerchantAcqProfile.size == 0) null else listMerchantAcqProfile.get(0))
    }

    fun getMerchantProfile(merchantName: String): MerchantProfile? {
        return MerchantProfileDb.findData(merchantName)
    }


    fun getMerchantProfile(merchantId: Int): MerchantProfile? {
        return MerchantProfileDb.findData(merchantId)
    }

    fun updateMerchantAcqBatch(acquerName: String?, batchNo: Int): Boolean{
        var mercAcquirerProfile = getSpecificAcquirerFromMerchantName(currentMerchant.merchantLabelName, acquerName)
        mercAcquirerProfile?.let {
            it.currBatchNo = batchNo

            return MerchantAcqProfileDb.updateTerminalIDMerchantID(it)
        }?:run{
            return false
        }
    }

    fun getPrimaryMerchantProfile() : String? {
        var resultList = listOf<MerchantProfile>()
        val listProfiles = MerchantProfileDb.findAllData(true)
        listProfiles?.let {
            if (it.size >= 1) {
                resultList = listOf<MerchantProfile>(it.get(0))
            }
        }

        if (resultList.size==1) {
            return resultList.get(0).merchantLabelName!!
        } else {
            return null
        }
    }
//
// current merchant that currently applyed but it may not same as save one
// sometime we temporary apply merchant such as report menu but when we back to main menu will restore to the save merchant.
//

    fun getCurrentMerchant(): String {
        return currentMerchant.merchantLabelName
    }

    fun getCurrentMerchantId(): Int {
        return currentMerchant.id
    }

    fun getSaveMerchant(): String {
        return FinancialApplication.getSysParam()[SysParam.StringParam.EDC_CURRENT_MERCHANT]
    }

    fun getCurrentMerchantLogo(): String {
        return currentMerchant.merchantLogo
    }
    
    fun getSupportAcq() : List<String>{
        return listAcq
    }

    fun isSupportAcq(aqcName : String) : Boolean {
        return listAcq.contains(aqcName)
    }

    fun applyProfileAndSave(merchantName: String){
        applyProfile(merchantName)
        FinancialApplication.getSysParam()[SysParam.StringParam.EDC_CURRENT_MERCHANT] = merchantName
    }

//
//  when back to main menu just restore to the save merchant.
//
    fun restoreCurrentMerchant(){
        val currentMerchantName = FinancialApplication.getSysParam()[SysParam.StringParam.EDC_CURRENT_MERCHANT]
        applyProfile(currentMerchantName)
    }

    fun RestoreToSpecificMerchant(targMercName : String){
        FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_CURRENT_MERCHANT, targMercName)
        applyProfile(targMercName)
    }


    fun applyNextProfile(): Int{
        getMerchantProfile(++currentMerchant.id)?.let {
            applyProfile(it.merchantLabelName)
            return 0
        }
        return -1 // no next profile
    }
    
    fun applyProfile(merchantName: String){
        var applyProfileResult  = TransResult.MULTI_MERCHANT_APPLY_MERC_INFO_FAILED
        try {
            val merchantProfile = getMerchantProfile(merchantName)

            merchantProfile?.let{
                FinancialApplication.getSysParam()[SysParam.StringParam.EDC_MERCHANT_NAME_EN] = merchantProfile.merchantPrintName
                FinancialApplication.getSysParam()[SysParam.StringParam.EDC_MERCHANT_ADDRESS] = merchantProfile.merchantPrintAddress1
                FinancialApplication.getSysParam()[SysParam.StringParam.EDC_MERCHANT_ADDRESS1] = merchantProfile.merchantPrintAddress2
            }

            val merchantAcqProfile = getAcqFromMerchantName(merchantName)
            merchantAcqProfile?.let{ it ->
                for (acqName in listAcq) {
                    val acq : Acquirer = AcqManager.getInstance().findAcquirer(acqName)!!
                    acq.isEnable = false
                    it.forEach {
                        if (it.acqHostName == acqName) {
                            acq.isEnable = true
                            acq.terminalId = it.terminalId
                            acq.merchantId = it.merchantId
                            acq.currBatchNo = it.currBatchNo
                        }
                    }
                    AcqManager.getInstance().updateAcquirer(acq)
                    currentMerchant = merchantProfile!!
                }

                applyProfileResult = TransResult.SUCC
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (applyProfileResult!=TransResult.SUCC) {
                DialogUtils.showErrMessage(ActivityStack.getInstance().top(), "Multi Merchant", "Failed to apply merchant info.", null, Constants.FAILED_DIALOG_SHOW_TIME)
            }
        }
    }

    fun clearTable(){
        MerchantProfileDb.clearTable()
        MerchantAcqProfileDb.clearTable()
    }
}
