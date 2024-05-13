package com.pax.pay.base

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.io.Serializable

@DatabaseTable(tableName = MerchantProfile.TABLE)
class MerchantProfile : Serializable {

    companion object{
        const val TABLE = "merchant_profile"
        const val ID = "merchant_profile_id"
        const val MERCHANT_NAME = "merchant_name"
        const val MERCHANT_PRINT_NAME = "merchant_print_name"
        const val MERCHANT_PRINT_ADDRESS1 = "merchant_print_address"
        const val MERCHANT_PRINT_ADDRESS2 = "merchant_print_address1"
        const val MERCHANT_LOGO = "merchant_logo"
        const val MERCHANT_SCREEN_LOGO_PATH = "merchant_screen_logo_path"
    }

    @DatabaseField(canBeNull = false, columnName = ID, generatedId = true)
    var id: Int = 0

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = MERCHANT_NAME)
    var merchantLabelName: String = ""

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = MERCHANT_PRINT_NAME)
    var merchantPrintName: String = ""

    @DatabaseField(dataType = DataType.STRING, columnName = MERCHANT_PRINT_ADDRESS1)
    var merchantPrintAddress1: String = ""

    @DatabaseField(dataType = DataType.STRING, columnName = MERCHANT_PRINT_ADDRESS2)
    var merchantPrintAddress2: String  = ""

    @DatabaseField(dataType = DataType.STRING, columnName = MERCHANT_LOGO)
    var merchantLogo: String  = ""

    @DatabaseField(dataType = DataType.STRING, columnName = MERCHANT_SCREEN_LOGO_PATH)
    var merchantScreenLogoPath: String  = ""
}