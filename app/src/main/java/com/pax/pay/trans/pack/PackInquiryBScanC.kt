package com.pax.pay.trans.pack

import com.pax.abl.core.ipacker.PackListener
import com.pax.dal.exceptions.PedDevException
import com.pax.gl.pack.exception.Iso8583Exception
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.TransData
import th.co.bkkps.utils.Log
import java.util.HashMap

open class PackInquiryBScanC(listener: PackListener?) : PackIso8583(listener) {

    @Throws(PedDevException::class)
    override fun pack(transData: TransData): ByteArray {
        try {
            setCommonData(transData)
            return pack(false, transData)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return "".toByteArray()
    }

    @Throws(Iso8583Exception::class)
    override fun setCommonData(transData: TransData) {
        setMandatoryData(transData)

        setBitData3(transData)

        setBitData4(transData)

        setBitData12(transData)

        setBitData13(transData)

        transData.nii = transData.acquirer.nii
        setBitData24(transData)

        setBitData41(transData)

        setBitData42(transData)

        setBitData62(transData)

        setBitData63(transData)
    }

    @Throws(Iso8583Exception::class)
    override fun setBitData63(transData: TransData) {
        val partnerID = Component.getPaddedStringRight(transData.walletPartnerID, 50, ' ')

        val refNo = "".padStart(20, ' ')

        if (transData.saleReference1 == null) {
            transData.saleReference1 = ""
        }
        if (transData.saleReference2 == null) {
            transData.saleReference2 = ""
        }

        val ref1 = Component.getPaddedStringRight(transData.saleReference1, 20, ' ')
        val ref2 = Component.getPaddedStringRight(transData.saleReference2, 20, ' ')
        val ref3 = "".padStart(20, ' ')

        val paymentType = "".padStart(10, ' ')

        val paymentTime = "".padStart(6, ' ')
        val paymentDate = "".padStart(4, ' ')

        val qrcode = Component.getPaddedStringRight(transData.qrCode, 400, ' ')

        setBitData("63", partnerID + refNo + ref1 + ref2 + ref3 + paymentType + paymentTime + paymentDate + qrcode)
    }

    override fun checkRecvData(
        map: HashMap<String?, ByteArray?>,
        transData: TransData,
        isCheckAmt: Boolean,
    ): Int {
        return super.checkRecvData(map, transData, false)
    }
}