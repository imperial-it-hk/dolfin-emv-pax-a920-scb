/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

/**
 * Transaction result. will be attached as {@link BaseResponse#rspCode}
 */
public class TransResult {
    private TransResult() {
        //do nothing
    }
    /**
     * transaction success
     */
    public static final int SUCC = 0;
    /**
     * settlement succeeded without doing batch upload
     */
    public static final int SUCC_NOREQ_BATCH = 1;
    /**
     * timeout
     */
    public static final int ERR_TIMEOUT = -1;
    /**
     * fail to connect
     */
    public static final int ERR_CONNECT = -2;
    /**
     * fail to send message
     */
    public static final int ERR_SEND = -3;
    /**
     * fail to receive message
     */
    public static final int ERR_RECV = -4;
    /**
     * fail to generate package
     */
    public static final int ERR_PACK = -5;
    /**
     * fail to parse package
     */
    public static final int ERR_UNPACK = -6;
    /**
     * format of package is wrong
     */
    public static final int ERR_PACKET = -7;
    /**
     * MAC of package is wrong
     */
    public static final int ERR_MAC = -8;
    /**
     * process code is unmatched
     */
    public static final int ERR_PROC_CODE = -9;
    /**
     * message type is unmatched
     */
    public static final int ERR_MSG = -10;
    /**
     * transaction amount is unmatched
     */
    public static final int ERR_TRANS_AMT = -11;
    /**
     * stan no is unmatched
     */
    public static final int ERR_STAN_NO = -12;
    /**
     * terminal is is unmatched
     */
    public static final int ERR_TERM_ID = -13;
    /**
     * merchant id is unmatched
     */
    public static final int ERR_MERCH_ID = -14;
    /**
     * no transaction
     */
    public static final int ERR_NO_TRANS = -15;
    /**
     * cannot find the original transaction
     */
    public static final int ERR_NO_ORIG_TRANS = -16;
    /**
     * transaction has been voided
     */
    public static final int ERR_HAS_VOIDED = -17;
    /**
     * transaction cannot be voided
     */
    public static final int ERR_VOID_UNSUPPORTED = -18;
    /**
     * comm channel error
     */
    public static final int ERR_COMM_CHANNEL = -19;
    /**
     * reject by host
     */
    public static final int ERR_HOST_REJECT = -20;
    /**
     * transaction aborted (without prompting any message)
     */
    public static final int ERR_ABORTED = -21;
    /**
     * transaction aborted（user cancel）
     */
    public static final int ERR_USER_CANCEL = -22;
    /**
     * need to settle before continue because of limits from the storage or currency change,etc.
     */
    public static final int ERR_NEED_SETTLE_NOW = -23;
    /**
     * need to settle before continue because of limits from the storage or currency change,etc.
     */
    public static final int ERR_NEED_SETTLE_LATER = -24;
    /**
     * need to settle before continue because of limits from the storage.
     */
    public static final int ERR_NO_FREE_SPACE = -25;
    /**
     * transaction is unsupported
     */
    public static final int ERR_NOT_SUPPORT_TRANS = -26;
    /**
     * card no is unmatched
     */
    public static final int ERR_CARD_NO = -27;
    /**
     * wrong password
     */
    public static final int ERR_PASSWORD = -28;
    /**
     * wrong parameter
     */
    public static final int ERR_PARAM = -29;

    /**
     * batch upload is not completed
     */
    public static final int ERR_BATCH_UP_NOT_COMPLETED = -31;
    /**
     * amount exceeded limit
     */
    public static final int ERR_AMOUNT = -33;
    /**
     * transaction is approved by host, but declined by card
     */
    public static final int ERR_CARD_DENIED = -34;
    /**
     * transaction cannot be adjusted
     */
    public static final int ERR_ADJUST_UNSUPPORTED = -36;
    /**
     * card is unsupported
     */
    public static final int ERR_CARD_UNSUPPORTED = -37;

    /**
     * expired card
     */
    public static final int ERR_CARD_EXPIRED = -38;

    /**
     * invalid card no
     */
    public static final int ERR_CARD_INVALID = -39;

    /**
     * Unsupported function
     */
    public static final int ERR_UNSUPPORTED_FUNC = -40;

    /**
     * fail to complete clss pre-process
     */
    public static final int ERR_CLSS_PRE_PROC = -41;

    /**
     * need to fall back
     */
    public static final int NEED_FALL_BACK = -42;

    /**
     * Invalid EMV QR code.
     */
    public static final int ERR_INVALID_EMV_QR = -43;

    /**
     * try to insert card again
     */
    public static final int ICC_TRY_AGAIN = -44;

    /**
     * Invalid APPR. CODE
     */
    public static final int ERR_PROMPT_INVALID_APPR_CODE = -45;

    /**
     * Wallet response UK
     */
    public static final int ERR_WALLET_RESP_UK = -46;

    /**
     * Advice error
     */
    public static final int ERR_ADVICE = -47;

	/**
     * TLE not load
     */
    public static final int ERR_TLE_NOT_LOAD = -48;

	/**
     * TLE request error
     */
    public static final int ERR_TLE_REQUEST = -49;

    /**
     * Load UPI error
     */
    public static final int ERR_UPI_LOAD = -50;

    /**
     * TMK already activate error for UPI
     */
    public static final int ERR_UPI_TMK_ACTIVATE = -51;

    /**
     * Logon UPI error
     */
    public static final int ERR_UPI_LOGON = -52;

    /**
     * Wallet host not response
     */
    public static final int ERR_NO_RESPONSE = -53;

    /**
     * Wallet connection error
     */
    public static final int ERR_COMMUNICATION = -54;

    /**
     * UPI not logon
     */
    public static final int ERR_UPI_NOT_LOGON = -55;

    /**
     * Call Issuer or Referral
     */
    public static final int ERR_REFERRAL_CALL_ISSUER = -56;

    /**
     * Download template failed
     */
    public static final int ERR_DOWNLOAD_FAILED = -57;

    /**
     * Process failed
     */
    public static final int ERR_PROCESS_FAILED = -58;

    /**
     * invalid template ID
     */
    public static final int ERR_INVALID_TEMPLATE_ID = -59;

    /**
     * Template ID exists
     */
    public static final int ERR_TEMPLATE_ID_EXISTS = -60;

    /**
     * invalid order ID
     */
    public static final int ERR_INVALID_ORDER_ID = -61;

    /**
     * invalid trance/invoice ID
     */
    public static final int ERR_INVALID_TRANCE_ID = -62;

    /**
     * Response Code is 95 in Settlement End
     */
    public static final int ERR_RECONCILE_FAILED = -63;

    /**
     * Settlement is not completed, need to settle firstly
     */
    public static final int ERR_SETTLE_NOT_COMPLETED = -64;

    /**
     * Host not found
     */
    public static final int ERR_HOST_NOT_FOUND = -65;

    /**
     * Settlement fail
     */
    public static final int ERR_SETTLEMENT_FAIL = -66;

    /**
     * Need to do MAG_ONLINE
     */
    public static final int ERR_NEED_MAG_ONLINE = -67;
    
	/**
     * Get QR fail
     */
    public static final int ERR_FAIL_GET_QR = -68;

    /**
     * SP200 not is use
     */
    public static final int ERR_SP200_NOT_USE = -69;

    /**
     * SP200 fail
     */
    public static final int ERR_SP200_FAIL = -70;


    public static final int OFFLINE_APPROVED = -71;

    public static final int ERR_UNSUPPORTED_TLE = -72;

    public static final int ERR_OFFLINE_UNSUPPORTED = -73;

    public static final int ERR_SP200_UPDATE_FAILED = -74;

    public static final int ERR_INVALID_APPR_CODE = -75;

    public static final int ERR_SCB_CONNECTION = -76;

    public static final int ERR_GEN_BITMAP_FAIL = -77;

    public static final int SP200_FIRMWARE_UPTODATE = -81;

    public static final int SP200_FIRMWARE_NO_AIP_FILE = -82;

    public static final int ERR_NO_LINKPOS_APP = -83;

    public static final int ERR_UNABLE_TO_INIT_LINKPOS_APP = -84;

    public static final int ERR_SP200_UPDATE_INTERNAL_FAILED = -85;

    public static final int ERR_NOT_ALLOW = -90;

    public static final int ERR_AMEX_API_TRANS_EXCEPTION = -91;

    public static final int ERR_NEED_FORWARD_TO_AMEX_API = -92;

    public static final int ERR_AMEX_APP_NOT_INSTALLED = -93;

    public static final int ERR_AMEX_PARAM_UPDATE_FAIL = -94;

    public static final int VERIFY_THAI_QR_PAY_RECEIPT_REQUIRED = -95;

    public static final int VERIFY_THAI_QR_PAY_RECEIPT_DE63_INVALID_LEN     =   -96;

    public static final int ERR_MISSING_FIELD63                             =   -97;

    public static final int QR_PROCESS_INQUIRY = -99;

    public static final int ERR_SP200_RESULT_FAILED                         =  -111;
    public static final int ERR_SP200_NOT_ENABLE                            =  -112;
    public static final int ERR_SP200_SEND_COMMAND_FAILED                   =  -113;

    public static final int ERR_HAS_ADJUSTED = -114;
    public static final int ERR_TRANS_NOW_ALLOW = -115;

    public static final int ERR_READ_CARD                                   =  -121;
    public static final int ERR_CARD_ENTRY_NOT_ALLOW                        =  -122;
    public static final int ERR_MISSING_INTERNAL_PROC_RESULT                =  -123;
    public static final int ERR_MISSING_CARD_INFORMATION                    =  -124;
    public static final int ERR_PREAUTH_CANCEL_UNSUPPORTED                  =  -125;

    public static final int ERR_SALE_COMP_TRANS_AMOUNT_EXCEED               =  -126;
    public static final int ERR_HAS_SALE_COMPLETED                          =  -127;
    public static final int ERR_SALE_COMPLETE_UNSUPPORTED                   =  -128;

    public static final int ERR_OFFLINE_UPLOAD_FAIL                         =  -129;

    public static final int ERR_ECR_DUPLICATE_SALE_REFERENCE_ID             = -1000;


    // LINKPOS
    public static final int ECR_LINKPOS_TRANS_NOT_ALLOW                     = -1501;

    // FORCE SETTLE - QR CREDIT
    public static final int ERR_QR_CREDIT_MISSING_FORCE_SETTLE_TIME         = -1521;
    public static final int ERR_QR_CREDIT_FORCE_SETTLE_INTERNAL_PROCESS     = -1522;



    /**
     *  VERIFONE-ERM
     */
    public static final int ERCM_OTHER_SETTING_SUCC                         = -2000;
    public static final int ERCM_OTHER_SETTING_ERR                          = -2001;
    public static final int ERCM_OTHER_SETTING_USER_CANCEL                  = -2002;
    public static final int ERCM_ERROR_EXTRACT_FIELD63                      = -2003;
    public static final int ERCM_PBK_DOWNLOAD_DECLINED                      = -2004;
    public static final int ERCM_SSK_DOWNLOAD_DECLINED                      = -2005;
    public static final int ERCM_UPLOAD_FAIL                                = -2006;
    public static final int ERCM_UPLOAD_SESSIONKEY_RENEWAL_REQUIRED         = -2007;
    public static final int ERCM_UPLOAD_SESSIONKEY_RENEWAL_RETRY_ERROR      = -2008;
    public static final int ERCM_MAXIMUM_TRANS_EXCEED_ERROR                 = -2009;

    //  ERM SETTLEMENT REPORT UPLOAD
    public static final int ERCM_ESETTLE_REPORT_UPLOAD_FAIL                 = -2010;
    public static final int ERCM_ESETTLE_REPORT_NO_FILE_FOR_UPLOAD          = -2011;
    public static final int ERCM_ESETTLE_REPORT_STORAGE_NOT_FOUND           = -2012;
    public static final int ERCM_ESETTLE_REPORT_INVALID_DIRECTORY_TYPE      = -2014;

    public static final int ERCM_INITIAL_PROCESS_FAILED                     = -2015;
    public static final int ERCM_INITIAL_NO_PBK_FILE                        = -2016;
    public static final int ERCM_INITIAL_INFO_NOT_READY                     = -2017;
    public static final int ERCM_INITIAL_INFO_ERCM_DISABLED                 = -2018;
    public static final int ERCM_INITIAL_INFO_ESIG_DISABLED                 = -2019;
    public static final int ERCM_INITIAL_INFO_MISSING_BANK_CODE             = -2020;
    public static final int ERCM_INITIAL_INFO_MISSING_MERC_CODE             = -2021;
    public static final int ERCM_INITIAL_INFO_MISSING_STORE_CODE            = -2022;
    public static final int ERCM_INITIAL_INFO_MISSING_KEY_VERSION           = -2023;
    public static final int ERCM_INITIAL_INFO_HOST_KMS_DISABLED             = -2024;
    public static final int ERCM_INITIAL_INFO_HOST_RMS_DISABLED             = -2025;

    //  KBANK DYNAMIC OFFLINE
    public static final int DYNAMIC_OFFLINE_STILL_DISABLED                  = -2100;
    public static final int DYNAMIC_OFFLINE_TRANS_NOT_ALLOW                 = -2101;

    //  AYCAP T1C MEMBER ID INQUIRY
    public static final int T1C_INQUIRY_MEMBER_NO_CARD_RECORD               = -2150;
    public static final int T1C_INQUIRY_MEMBER_SYSTEM_MALFUNCTION           = -2151;

    // LEMONFARM & LAWSON ECR LINKPOS
    public static final int ECR_FOUND_DOUBLE_TRANSACTION_BLOCKED            = -2161;

    public static final int MULTI_APP_NO_NEED_TO_UPLOAD_ERM                 = -2201;
    public static final int MULTI_APP_NEED_TO_UPLOAD_ERM                    = -2202;

    public static final int PROMPT_INQUIRY                                  = -2200;


    public static final int PARAMETER_NOT_INIT                              = -2601;
    public static final int PARAMETER_XML_PARSE_FAILED                      = -2602;
    public static final int PARAMETER_MULTI_MERCHANT_FAILED                 = -2603;
    public static final int PARAMETER_INITIAL_FAILED                        = -2604;
    public static final int PARAMETER_NULL_INFO_IN_PARAM_FILE               = -2605;

    public static final int MULTI_MERCHANT_APPLY_MERC_INFO_FAILED           = -2700;
    public static final int MULTI_MERCHANT_MASTER_PROFILE_MISSING           = -2701;
    public static final int MULTI_MERCHANT_SELECTED_MERCHANT_ERROR          = -2702;

}
