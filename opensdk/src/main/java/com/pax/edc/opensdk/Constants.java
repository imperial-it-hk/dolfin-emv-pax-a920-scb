/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-18
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Hide from Javadoc
 */
public class Constants {
    @IntDef({PRE_AUTH, SALE, VOID, REFUND, SETTLE, REPRINT_TRANS, REPRINT_TOTAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommandType {
    }

    public static final int PRE_AUTH = 1; //default
    public static final int SALE = 2;
    public static final int VOID = 3;
    public static final int REFUND = 4;
    public static final int SETTLE = 5;
    public static final int REPRINT_TRANS = 6;
    public static final int REPRINT_TOTAL = 7;

    public static final int REQUEST_CODE = 100;

    static final String SDK_VERSION = "_edc_sdk_version";
    static final int SDK_VERSION_VALUE = 1;
    static final String COMMAND_TYPE = "_edc_command_type";
    static final String APP_ID = "_edc_app_id";
    static final String PACKAGE_NAME = "com.pax.edc";
    static final String URI = "pax_edc://com.pax.edc.payment.entry";
    static final String ACTION = "android.pax.edc.payment.entry";
    static final String APP_PACKAGE = "_edc_app_package";
    static class Req {
        static final String REQ_AMOUNT = "_edc_request_amount";
        static final String REQ_TIP_AMOUNT = "_edc_request_tip_amount";
        static final String REQ_ORIGINAL_REF_NO = "_edc_request_org_ref_no";
        static final String REQ_ORIGINAL_DATE = "_edc_request_org_date";
        static final String REQ_VOUCHER_NO = "_edc_request_voucher_no";

        static final String REQ_REPRINT_TYPE = "_edc_request_reprint_type";

        private Req(){
            //do nothing
        }
    }

    static class Resp {
        static final String RSP_CODE = "_edc_response_code";
        static final String RSP_MSG = "_edc_response_message";

        static final String RSP_MERCHANT_NAME = "_edc_response_merchant_name";
        static final String RSP_MERCHANT_ID = "_edc_response_merchant_id";
        static final String RSP_TERMINAL_ID = "_edc_response_terminal_id";
        static final String RSP_CARD_NO = "_edc_response_card_no";
        static final String RSP_CARD_TYPE = "_edc_response_card_type";
        static final String RSP_VOUCHER_NO = "_edc_response_voucher_no";
        static final String RSP_BATCH_NO = "_edc_response_batch_no";
        static final String RSP_ISSUER_NAME = "_edc_response_issuer_name";
        static final String RSP_ACQUIRER_NAME = "_edc_response_acquirer_name";
        static final String RSP_REF_NO = "_edc_response_ref_no";
        static final String RSP_TRANS_TIME = "_edc_response_trans_time";
        static final String RSP_AMOUNT = "_edc_response_amount";
        static final String RSP_AUTH_CODE = "_edc_response_auth_code";
        static final String RSP_CH_SIGNATURE = "_edc_response_cardholder_signature";
        static final String RSP_CH_SIGNATURE_PATH = "_edc_response_cardholder_signature_path";

        private Resp(){
            //do nothing
        }
    }
}
