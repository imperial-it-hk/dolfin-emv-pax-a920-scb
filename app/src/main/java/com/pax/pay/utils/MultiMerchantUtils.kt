package com.pax.pay.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pax.pay.app.FinancialApplication
import com.pax.pay.db.MerchantAcqProfileDb
import com.pax.pay.db.MerchantProfileDb
import com.pax.pay.trans.model.MerchantProfileManager
import com.pax.settings.SysParam
import java.io.InputStream

class MultiMerchantUtils {
    companion object {
        fun getCurrentMerchantName(): String? {
            return FinancialApplication.getSysParam()[SysParam.StringParam.EDC_CURRENT_MERCHANT, null]
        }

        fun getMasterAcquirerTID(acqName: String) : String? {
            var returnVal : String? = null
            try {
                if (MerchantProfileManager.isMultiMerchantEnable()) {
                    val mercName = MerchantProfileDb.findFirstRecord()?.merchantLabelName
                    mercName?.let {
                        val masterAcquirer = MerchantAcqProfileDb.findAcqFromMerchant(it, acqName)
                        masterAcquirer?.let{
                            returnVal = it.get(0).terminalId
                        }?:run {
                            throw Exception()
                        }
                    }?:run {
                        throw Exception()
                    }
                } else {
                    returnVal = FinancialApplication.getAcqManager().findAcquirer(acqName).terminalId
                }
            } catch (e: Exception) {
                e.printStackTrace()
                returnVal = null
            }

            return returnVal
        }

        fun getMasterProfileName() : String? {
            var returnVal : String? = null
            try {
                if (MerchantProfileManager.isMultiMerchantEnable()) {
                    returnVal = MerchantProfileDb.findFirstRecord()?.merchantLabelName
                } else {
                    throw Exception()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                returnVal = null
            }

            return returnVal
        }

        fun getMerchantScreenLogoBitmap(exMercName : String?) : Bitmap? {
            var screenLogoBitmap : Bitmap? = null
            lateinit var targMercName : String
            if (exMercName==null) {
                targMercName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_CURRENT_MERCHANT)
            } else {
                targMercName = exMercName
            }

            val mercProfile = MerchantProfileManager.getMerchantProfile(targMercName)
            mercProfile?.let {
                screenLogoBitmap = Utils.getImageFromPath(it.merchantScreenLogoPath)
            }

            screenLogoBitmap?:run{
                try {
                    val inputStream : InputStream = FinancialApplication.getApp().resources.assets.open("kbank_screen_logo.png")
                    screenLogoBitmap = BitmapFactory.decodeStream(inputStream)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            return screenLogoBitmap
        }

        fun isMasterMerchant() : Boolean {
            if (MerchantProfileManager.isMultiMerchantEnable()) {
                return (this.getMasterProfileName().equals(MerchantProfileManager.getCurrentMerchant()))
            } else {
                return true
            }
        }

        fun isDuplicate(list : List<String>) : Boolean {
            if (list.size<=1) {
                return false
            } else {
                val mapCounter = list.groupingBy { it } .eachCount().filter { it.value > 1 }
                mapCounter.let {
                    return (!it.isEmpty())
                }
            }
        }

    }

}