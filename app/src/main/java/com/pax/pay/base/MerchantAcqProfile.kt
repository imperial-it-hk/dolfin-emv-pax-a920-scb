package com.pax.pay.base

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.io.Serializable

@DatabaseTable(tableName = MerchantAcqProfile.TABLE)
class MerchantAcqProfile : Serializable {

    companion object{
        const val TABLE = "merchant_acq_profile"
        const val ID = "acq_profile_id"
        const val MERCHANT_NAME = "merchant_name"
        const val ACQ_HOST_NAME = "acqHostName"
        const val MERCHANT_ID = "merchantId"
        const val TERMINAL_ID = "terminalId"
        const val CURRENT_BATCH_NO = "currBatchNo"
    }

    @DatabaseField(canBeNull = false, columnName = ID, generatedId = true)
    var id: Int = 0

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = MERCHANT_NAME)
    var merchantName: String = ""

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = ACQ_HOST_NAME)
    var acqHostName: String = ""

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = MERCHANT_ID)
    var merchantId: String = ""

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = TERMINAL_ID)
    var terminalId: String = ""

    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = CURRENT_BATCH_NO)
    var currBatchNo: Int = 0
}