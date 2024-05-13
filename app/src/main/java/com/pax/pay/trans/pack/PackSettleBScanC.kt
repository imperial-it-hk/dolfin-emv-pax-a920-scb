package com.pax.pay.trans.pack

import com.pax.abl.core.ipacker.PackListener
import com.pax.dal.exceptions.PedDevException
import com.pax.gl.pack.exception.Iso8583Exception
import com.pax.pay.trans.model.TransData
import th.co.bkkps.utils.Log

class PackSettleBScanC(listener: PackListener?)  : PackIso8583(listener) {

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

        setBitData3(transData)

        setBitData12(transData)

        setBitData13(transData)

        transData.nii = transData.acquirer.nii
        setBitData24(transData)

        setBitData41(transData)

        setBitData42(transData)

        setBitData62(transData)

        setBitData63(transData)
    }
}