package com.pax.pay.utils

import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants

class ControlLimitUtils {

    companion object {

        val WHITELIST_HOST : ArrayList<String> = arrayListOf<String>(Constants.ACQ_KBANK, Constants.ACQ_UP, Constants.ACQ_SMRTPAY, Constants.ACQ_SMRTPAY_BDMS, Constants.ACQ_DOLFIN_INSTALMENT)

        fun isSupportControlLimitHost(hostName : String) : Boolean {
            return WHITELIST_HOST.contains(hostName)
        }

        fun isAllowEnterPhoneNumber(hostName: String) : Boolean {
            val acquirer : Acquirer? = FinancialApplication.getAcqManager().findAcquirer(hostName)
            acquirer?.let {
                return (it.isEnable  && it.enableControlLimit && it.enablePhoneNumberInput)
            }?: run {
                return false
            }
        }

        fun isEnableControlLimit(hostName: String) : Boolean {
            val acquirer : Acquirer? = FinancialApplication.getAcqManager().findAcquirer(hostName)
            acquirer?.let {
                return (it.isEnable  && it.enableControlLimit)
            }?: run {
                return false
            }
        }

        fun getEnabledControlLimitHostCount() : Int {
            return ((FinancialApplication.getAcqManager().findAllAcquirers()).filter{ it.isEnable && it.enableControlLimit }).count()
        }

        fun getEnabledControlLimitHosts() : List<Acquirer> {
            return ((FinancialApplication.getAcqManager().findAllAcquirers()).filter{ it.isEnable && it.enableControlLimit }) as List<Acquirer>
        }
    }
}