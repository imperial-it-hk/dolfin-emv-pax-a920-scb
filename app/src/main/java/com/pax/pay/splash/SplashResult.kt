package com.pax.pay.splash

class SplashResult {
    companion object {
        const val WAITING_TRANS_CALLBACK                        =  9
        const val SUCCESS                                       =  0

        // Download parameter
        const val DOWNLOAD_PARAM_THREAD_FAILED                  = -1
        const val DOWNLOAD_PARAM_FAILED                         = -2
        const val NO_PARAM_FILE                                 = -3
        const val DOWNLOAD_PARAM_HANDLE_SUCCESS_FAILED          = -4

        const val INITIAL_FAILED                                = -5

        // PARAM
        const val PARAMETER_NOT_INIT                            = -6
        const val PARAMETER_INITIAL_FAILED                      = -7
        const val PARAMETER_XML_PARSE_FAILED                    = -8
        const val PARAMETER_MULTI_MERCHANT_FAILED               = -9
        const val PARAMETER_NULL_INFO_IN_PARAM_FILE             = -10

        // ERCM download
        const val ERCM_STATUS_DISABLED                          = -11
        const val ERCM_STATUS_CLAER_SESSIONKEY_ERROR            = -12

        // DOLFIN ask permission
        const val DOLFIN_PERMISSION_ACQUIRER_NOT_FOUND          = -21
        const val DOLFIN_PERMISSION_ACQUIRER_DISABLED           = -22
        const val DOLFIN_PERMISSION_SERVICE_NOT_BINDED          = -23
        const val DOLFIN_PERMISSION_FAILED_TO_START_SERVICE     = -24

        // ERASE Key
        const val TLE_ERASE_KEY_FAILED                          = -31

        // SCB-IPP TLE
        const val TLE_SCB_HOST_NOT_FOUND                        = -41
        const val TLE_SCB_TEID_FILE_NOT_FOUND                   = -42
        const val TLE_SCB_TEID_READ_FILE_ERROR                  = -43
        const val TLE_SCB_TEID_WAS_EMPTY                        = -44
        const val TLE_SCB_UPDATE_PARAM_FAILED                   = -45
        const val TLE_SCB_TRANS_API_FACTORY_MISSING             = -46
        const val TLE_SCB_TRANS_API_UNABLE_TO_START             = -47
        const val TLE_SCB_TRANS_API_RESULT_MISSING              = -48
        const val TLE_SCB_TLE_DOWNLOAD_ERROR                    = -49
        const val TLE_SCB_TLE_UPDATE_ACQUIRER_NOT_FOUND         = -50
        const val TLE_SCB_SERVICE_NOT_BINDED                    = -81
        const val TLE_SCB_HOST_DISABLE_TLE                      = -82

        // AMEX TLE-TPK
        const val TLE_AMEX_HOST_NOT_FOUND                       = -71
        const val TLE_AMEX_TEID_FILE_NOT_FOUND                  = -72
        const val TLE_AMEX_TEID_WAS_EMPTY                       = -73
        const val TLE_AMEX_TRANS_API_RESULT_MISSING             = -74
        const val TLE_AMEX_SERVICE_NOT_BINDED                   = -75
        const val TLE_AMEX_HOST_DISABLE_TLE                     = -76

        // KBANK TLE
        const val TLE_KBANK_ERROR_ACQUIRER_NOT_FOUND            = -51
        const val TLE_KBANK_TEID_FILE_NOT_FOUND                 = -52
        const val TLE_KBANK_DOWNLOAD_FAILED                     = -53
        const val TLE_KBANK_DOWNLOAD_SUCCESS                    = -54
        const val TLE_KBANK_ACTIVE_ACQUIRER_NOT_FOUND           = -55
        const val TLE_KBANK_TEID_READ_FILE_ERROR                = -56

        // UPDATE SP200 FIRMWARE
        const val UPDATE_FIRMWARE_SP200_DISABLE                 = -61
        const val UPDATE_FIRMWARE_SP200_UPDATE_FAILED           = -62
        const val DEVICE_NOT_SUPPORT_SP200                      = -63
        const val SP200_INIT_FAILED                             = -64


        const val LAN_COMMUNICATION_NOT_READY                   = -90




        const val VAL_SUCCESS = "SUCCESS"
        const val VAL_DOWNLOAD_PARAM_THREAD_FAILED = "Failed, creating thread for download"
        const val VAL_DOWNLOAD_PARAM_FAILED = "Failed, downloading parameter"
        const val VAL_NO_PARAM_FILE = "No parameter file"
        const val VAL_ERCM_STATUS_DISABLED = "ERCM was disabled"
        const val VAL_ERCM_STATUS_CLAER_SESSIONKEY_ERROR = "ERCM clear session key failed"
        const val VAL_DOLFIN_PERMISSION_ACQUIRER_NOT_FOUND = "Dolfin acquirer was not found"
        const val VAL_DOLFIN_PERMISSION_ACQUIRER_DISABLED = "Dolfin acquirer was disabled"
        const val VAL_DOLFIN_PERMISSION_SERVICE_NOT_BINDED = "Dolfin service was not bind"
        const val VAL_DOLFIN_PERMISSION_FAILED_TO_START_SERVICE = ""
        const val VAL_TLE_ERASE_KEY_FAILED                          = -31
        const val VAL_TLE_SCB_HOST_NOT_FOUND                        = -41
        const val VAL_TLE_SCB_TEID_FILE_NOT_FOUND                   = -42
        const val VAL_TLE_SCB_TEID_READ_FILE_ERROR                  = -43
        const val VAL_TLE_SCB_TEID_WAS_EMPTY                        = -44
        const val VAL_TLE_SCB_UPDATE_PARAM_FAILED                   = -45
        const val VAL_TLE_SCB_TRANS_API_FACTORY_MISSING             = -46
        const val VAL_TLE_SCB_TRANS_API_FACTORY_INTERNAL_ERROR      = -47
        const val VAL_TLE_SCB_TRANS_API_RESULT_MISSING              = -48
        const val VAL_TLE_SCB_TLE_DOWNLOAD_ERROR                    = -49
        const val VAL_TLE_SCB_TLE_UPDATE_ACQUIRER_NOT_FOUND         = -50
        const val VAL_TLE_KBANK_ERROR_ACQUIRER_NOT_FOUND            = -51
        const val VAL_TLE_KBANK_TEID_FILE_NOT_FOUND                 = -52
        const val VAL_TLE_KBANK_DOWNLOAD_FAILED                     = -53
        const val VAL_TLE_KBANK_DOWNLOAD_SUCCESS                    = -54



        fun getErrorMessage(splashResult : Int) : String {
            lateinit var message : String
            try {

            } catch (e:Exception) {
                message = "Unknown error on SplashAcitivity (Code:{$splashResult})"
            }

            return message
        }

    }


}