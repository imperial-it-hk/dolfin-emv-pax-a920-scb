package th.co.bkkps.utils

import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.base.MerchantAcqProfile
import com.pax.pay.constant.Constants
import com.pax.pay.trans.model.MerchantProfileManager
import com.pax.pay.trans.model.TransData
import th.co.bkkps.extension.removeByName

object ERMUtil {
    fun removeAcquirerListByName(acquirers: MutableList<Acquirer>, acqName: String): MutableList<Acquirer> {
        return acquirers.removeByName(acqName)
    }

    fun getErmStoreCode(transData : TransData) : String? {
        var returnStoreCode : String? = null
        if (MerchantProfileManager.isMultiMerchantEnable()) {
            transData.ercmStoreCode?.let { ermStoreCodeForMultiMerchant->
                val priMercName = MerchantProfileManager.getPrimaryMerchantProfile()
                priMercName?.let { mercName->
                    returnStoreCode = MerchantProfileManager.getSpecificAcquirerFromMerchantName( mercName, Constants.ACQ_KBANK )?.merchantId
                } ?: run {
                    if (ermStoreCodeForMultiMerchant.equals("{MID}")) {
                    returnStoreCode = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)?.merchantId!!
                    } else {
                        returnStoreCode = ermStoreCodeForMultiMerchant
                    }
                }
            }?:run{
                returnStoreCode = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)?.merchantId!!
            }
        } else {
            transData.ercmStoreCode?.let { ermStoreCodeForSingleMerchant ->
                if (ermStoreCodeForSingleMerchant.equals("{MID}")) {
                    val acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK)
                    acq?.let { targAcq->
                        returnStoreCode = targAcq.merchantId!!
                    }?:run{
                        returnStoreCode = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)?.merchantId!!
                    }
                } else {
                    returnStoreCode = ermStoreCodeForSingleMerchant
                }
            }?:run{
                returnStoreCode = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)?.merchantId!!
            }
        }

        return returnStoreCode
    }
}