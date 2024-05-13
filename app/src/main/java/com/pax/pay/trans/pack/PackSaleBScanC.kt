package com.pax.pay.trans.pack

import com.pax.abl.core.ipacker.PackListener
import com.pax.dal.exceptions.PedDevException
import com.pax.gl.pack.exception.Iso8583Exception
import com.pax.pay.constant.Constants
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.TimeConverter
import th.co.bkkps.utils.Log
import java.security.SecureRandom

open class PackSaleBScanC (listener: PackListener?) : PackIso8583(listener) {

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
    override fun setCommonData (transData : TransData) {
        setMandatoryData(transData)

        if (transData.reversalStatus == TransData.ReversalStatus.REVERSAL) {
            packDupTrans(transData)
            return
        }

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

    private fun packDupTrans(transData : TransData) {
        setBitData3(transData)

        setBitData12(transData)

        setBitData13(transData)

        transData.nii = transData.acquirer.nii
        setBitData24(transData)

        setBitData41(transData)

        setBitData42(transData)

        setBitData63(transData)
    }

    override fun setBitData3(transData: TransData) {
        when (transData.reversalStatus) {
            TransData.ReversalStatus.REVERSAL -> setBitData("3", "000000")
            else -> setBitData("3", transData.transType.procCode)
        }
    }

    @Throws(Iso8583Exception::class)
    override fun setBitData63(transData: TransData) {
        val dateTime = TimeConverter.convert(
            transData.dateTime,
            Constants.TIME_PATTERN_TRANS,
            Constants.TIME_PATTERN_TRANS2
        )
        val random = randomNumber(12)

        val partnerID = (dateTime + random + transData.acquirer.terminalId).padEnd(50, ' ')
        transData.walletPartnerID = partnerID

        val refNo = "".padStart(20, ' ')
        transData.refNo = refNo;

        if (transData.saleReference1 == null) {
            transData.saleReference1 = ""
        }
        if (transData.saleReference2 == null) {
            transData.saleReference2 = ""
        }

        val ref1 = Component.getPaddedStringRight(transData.saleReference1, 20, ' ')
        val ref2 = Component.getPaddedStringRight(transData.saleReference2, 20, ' ')

        val paymentType = "".padStart(10, ' ')

        transData.qrCode = transData.qrCode.padEnd(400, ' ')
        transData.field63 = partnerID + refNo + ref1 + ref2 + paymentType + transData.qrCode

        when (transData.reversalStatus) {
            TransData.ReversalStatus.REVERSAL -> setBitData("63", partnerID + transData.refNo)
            else -> setBitData("63", partnerID + refNo + ref1 + ref2 + paymentType + transData.qrCode)
        }
    }

    private fun randomNumber(len: Int): String? {
        val num = "0123456789"
        val rnd = SecureRandom()
        val sb = StringBuilder(len)
        for (i in 0 until len) sb.append(num[rnd.nextInt(num.length)])
        return sb.toString()
    }
}