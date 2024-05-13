package th.co.bkkps.linkposapi.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkPOSModel (@Required @SerialName("PROTOCOL") val protocol: String,
                         @SerialName("REQUEST") val request: LinkPOSRequest,
                         @SerialName("RESPONSE") var response: LinkPOSResponse)

@Serializable
class LinkPOSRequest {
    @Required
    @SerialName("TRANS_CODE")
    var transCode: String? = null
    @SerialName("AMOUNT")
    var amount: String? = null
    @SerialName("TRACE_NO")
    var traceNo: String? = null
    @SerialName("NII")
    var nii: String? = null
    @SerialName("HOST_NAME")
    var hostName: String? = null
    @SerialName("QR_TYPE")
    var qrType: String? = null
    @SerialName("W1")
    var vatBAmount: String? = null
    @SerialName("W2")
    var vatBTaxAllowance: String? = null
    @SerialName("W3")
    var vatBMerUnique: String? = null
    @SerialName("W4")
    var vatBCampaignType: String? = null
    @SerialName("W5")
    var vatBRef1: String? = null
    @SerialName("W6")
    var vatBRef2: String? = null
    @SerialName("REF0")
    var ref0: String? = null
    @SerialName("REF1")
    var ref1: String? = null
    @SerialName("REF2")
    var ref2: String? = null
    @SerialName("MERCHANT_NUM")
    var merchantNum: String? = null
    @SerialName("ALL_HOST")
    var allHostCommand: Boolean = false
    @SerialName("VAT_REBATE")
    var vatBCommand: Boolean = false
}

@Serializable
class LinkPOSResponse {
    @SerialName("STATUS")
    var status: String? = null
    @SerialName("F00")
    var f00: String? = null
    @SerialName("F01")
    var f01: String? = null
    @SerialName("F02")
    var f02: String? = null
    @SerialName("F03")
    var f03: String? = null
    @SerialName("F04")
    var f04: String? = null
    @SerialName("F06")
    var f06: String? = null
    @SerialName("F16")
    var f16: String? = null
    @SerialName("F30")
    var f30: String? = null
    @SerialName("F31")
    var f31: String? = null
    @SerialName("F40")
    var f40: String? = null
    @SerialName("F41")
    var f41: String? = null
    @SerialName("F42")
    var f42: String? = null
    @SerialName("F43")
    var f43: String? = null
    @SerialName("F44")
    var f44: String? = null
    @SerialName("F45")
    var f45: String? = null
    @SerialName("F50")
    var f50: String? = null
    @SerialName("F65")
    var f65: String? = null
    @SerialName("D0")
    var d0: String? = null
    @SerialName("D1")
    var d1: String? = null
    @SerialName("D2")
    var d2: String? = null
    @SerialName("D3")
    var d3: String? = null
    @SerialName("D4")
    var d4: String? = null
    @SerialName("D5")
    var d5: String? = null
    @SerialName("D6")
    var d6: String? = null
    @SerialName("D7")
    var d7: String? = null
    @SerialName("D8")
    var d8: String? = null
    @SerialName("D9")
    var d9: String? = null
    @SerialName("E1")
    var e1: String? = null
    @SerialName("E2")
    var e2: String? = null
    @SerialName("E3")
    var e3: String? = null
    @SerialName("E4")
    var e4: String? = null
    @SerialName("FL")
    var fl: String? = null
    @SerialName("H1")
    var h1: String? = null
    @SerialName("H2")
    var h2: String? = null
    @SerialName("HN")
    var hn: String? = null
    @SerialName("HO")
    var ho: String? = null
    @SerialName("ZY")
    var zy: String? = null
    @SerialName("ZZ")
    var zz: String? = null
    @SerialName("R1")
    var r1: String? = null
    @SerialName("R2")
    var r2: String? = null
    @SerialName("R3")
    var r3: String? = null
    @SerialName("R4")
    var r4: String? = null
}