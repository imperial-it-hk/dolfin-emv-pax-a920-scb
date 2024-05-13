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
package com.pax.pay.trans.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.pax.pay.app.FinancialApplication;

public class Controller {
    public static class Constant {
        public static final int YES = 1;
        public static final int NO = 0;
        /**
         * batch upload status
         */
        public static final int NO_ACQUIRER = -2;
        public static final int SETTLE = -1;
        public static final int WORKED = 0;
        public static final int CLEAR_TRANSDATA_REQUIRED = 1;
        public static final int CLEAR_TRANSTOTAL_REQUIRED = 2;
        public static final int SET_TRANSTOTAL_CLOSED_REQUIRED = 3;
        public static final int PRINT_SETTLE_REPORT_REQUIRED = 4;
        public static final int GENERATE_ERCM_REQUIRED = 5;

        private Constant() {
            //do nothing
        }
    }

    public static final String IS_FIRST_INITIAL_NEEDED = "IS_FIRST_INITIAL_NEEDED";
    public static final String IS_FIRST_RUN = "IS_FIRST_RUN";
    public static final String IS_FIRST_DOWNLOAD_PARAM_NEEDED = "IS_FIRST_DOWNLOAD_PARAM_NEEDED";
    public static final String LAST_AUTO_SETTLE_CHECK = "LAST_AUTO_SETTLE_CHECK";
    //add by xiawh if need setting wizard of not
    public static final String NEED_SET_WIZARD = "need_set_wizard";
    /**
     * is need download capk  NO: not need YES: need
     */
    public static final String NEED_DOWN_CAPK = "need_down_capk";
    /**
     * is need download aid NO: not need YES: need
     */
    public static final String NEED_DOWN_AID = "need_down_aid";
    /**
     * batch upload status {@link Constant#WORKED}not in batch upload ,
     */
    public static final String BATCH_UP_STATUS = "batch_up_status";
    /**
     * batch upload status {@link Constant#WORKED}not in batch upload ,
     */
    public static final String BATCH_UP_WALLET_STATUS = "batch_up_wallet_status";
    /**
     * batch upload status {@link Constant#WORKED}not in batch upload ,
     */
    public static final String BATCH_UP_LINEPAY_STATUS = "batch_up_linepay_status";
    /**
     * batch upload status {@link Constant#WORKED}not in batch upload ,
     */
    public static final String BATCH_UP_RABBIT_STATUS = "batch_up_rabbit_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for ACQ_KBANK
     */
    public static final String SETTLE_STATUS = "settle_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for AMEX
     */
    public static final String SETTLE_AMEX_STATUS = "settle_amex_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for UnionPay host
     */
    public static final String SETTLE_UP_STATUS = "settle_up_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Wallet
     */
    public static final String SETTLE_WALLET_STATUS = "settle_wallet_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for BSS_HOST
     */
    public static final String SETTLE_BBL_BSS_STATUS = "settle_bbl_bss_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for LinePay
     */
    public static final String SETTLE_LINEPAY_STATUS = "settle_linepay_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for REDEEM
     */
    public static final String SETTLE_REDEEM_STATUS = "settle_redeem_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for SMRTPAY
     */
    public static final String SETTLE_SMARTPAY_STATUS = "settle_smartpay_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for DCC
     */
    public static final String SETTLE_DCC_STATUS = "settle_dcc_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for AMEX Instalment
     */
    public static final String SETTLE_AMEX_EPP_STATUS = "settle_amex_epp_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Alipay
     */
    public static final String SETTLE_ALIPAY_STATUS = "settle_alipay_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Alipay B scan C
     */
    public static final String SETTLE_ALIPAY_B_SCAN_C_STATUS = "settle_alipay_b_scan_c_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for WeChat
     */
    public static final String SETTLE_WECHAT_STATUS = "settle_wechat_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for WeChat B scan C
     */
    public static final String SETTLE_WECHAT_B_SCAN_C_STATUS = "settle_wechat_b_scan_c_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Kplus
     */
    public static final String SETTLE_KPLUS_STATUS = "settle_kplus_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Bay Installment
     */
    public static final String SETTLE_BAY_INSTALLMENT_STATUS = "settle_bay_installment_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for QR Credit
     */
    public static final String SETTLE_QR_CREDIT_STATUS = "settle_qr_credit_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for Dolfin
     */
    public static final String SETTLE_DOLFIN_STATUS = "settle_dolfin_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for SCB Instalment
     */
    public static final String SETTLE_SCB_INSTALLMENT_STATUS = "settle_scb_installment_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for SCB Redeem
     */
    public static final String SETTLE_SCB_REDEEM_STATUS = "settle_scb_redeem_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for K Check ID
     */
    public static final String SETTLE_KCHECKID_STATUS = "settle_kcheckid_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for ACQ_KBANK_BDMS
     */
    public static final String SETTLE_KBANK_BDMS_STATUS = "settle_kbank_bdms_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for REDEEM_BDMS
     */
    public static final String SETTLE_REDEEM_BDMS_STATUS = "settle_redeem_bdms_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for SMRTPAY_BDMS
     */
    public static final String SETTLE_SMARTPAY_BDMS_STATUS = "settle_smartpay_bdms_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for SMRTPAY_BDMS
     */
    public static final String SETTLE_MY_PROMPT_STATUS = "settle_my_prompt_status";
    /**
     * settlement status {@link Constant#WORKED}is settlement success , {@link Constant#SETTLE}:is settlement fail, block to do transaction for DOLFIN INSTALMENT
     */
    public static final String SETTLE_DOLFIN_INSTALMENT_STATUS = "settle_dolfin_instalment_status";
    /**
     * check result
     */
    public static final String RESULT = "result";
    /**
     * batch upload number
     */
    public static final String BATCH_NUM = "batch_num";
    /**
     * batch upload number of Wallet transaction
     */
    public static final String BATCH_NUM_WALLET = "batch_num_wallet";
    /**
     * batch upload number of Line Pay transaction
     */
    public static final String BATCH_NUM_LINEPAY = "batch_num_linepay";
    /**
     * batch upload number of Line Pay transaction
     */
    public static final String BATCH_NUM_RABBIT = "batch_num_rabbit";
    /**
     * whether need to clear transaction record: NO: not clear, YES: clear
     */
    public static final String CLEAR_LOG = "clearLog";

    private static final String FILE_NAME = "control";

    public Controller() {
        if (!isFirstRun()) {
            return;
        }
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();

        editor.putBoolean(IS_FIRST_RUN, true);
        editor.putInt(NEED_DOWN_CAPK, Constant.YES);
        editor.putInt(NEED_DOWN_AID, Constant.YES);
//        editor.putInt(BATCH_UP_STATUS, Constant.NO);
//        editor.putInt(BATCH_UP_WALLET_STATUS, Constant.NO);
//        editor.putInt(BATCH_UP_LINEPAY_STATUS, Constant.NO);
//        editor.putInt(BATCH_UP_RABBIT_STATUS, Constant.NO);
        editor.putInt(SETTLE_STATUS, Constant.NO);
        editor.putInt(SETTLE_AMEX_STATUS, Constant.NO);
        editor.putInt(SETTLE_UP_STATUS, Constant.NO);
        editor.putInt(SETTLE_WALLET_STATUS, Constant.NO);
        editor.putInt(SETTLE_BBL_BSS_STATUS, Constant.NO);
        editor.putInt(SETTLE_LINEPAY_STATUS, Constant.NO);
        editor.putInt(SETTLE_REDEEM_STATUS, Constant.NO);
        editor.putInt(SETTLE_SMARTPAY_STATUS, Constant.NO);
        editor.putInt(SETTLE_DCC_STATUS, Constant.NO);
        editor.putInt(SETTLE_AMEX_EPP_STATUS, Constant.NO);
        editor.putInt(SETTLE_ALIPAY_STATUS, Constant.NO);
        editor.putInt(SETTLE_WECHAT_STATUS, Constant.NO);
        editor.putInt(SETTLE_KPLUS_STATUS, Constant.NO);
        editor.putInt(SETTLE_BAY_INSTALLMENT_STATUS, Constant.NO);
        editor.putInt(SETTLE_QR_CREDIT_STATUS, Constant.NO);
        editor.putInt(SETTLE_DOLFIN_STATUS, Constant.NO);
        editor.putInt(SETTLE_SCB_INSTALLMENT_STATUS, Constant.NO);
        editor.putInt(SETTLE_SCB_REDEEM_STATUS, Constant.NO);
        editor.putInt(SETTLE_KCHECKID_STATUS, Constant.NO);
        editor.putInt(SETTLE_KBANK_BDMS_STATUS, Constant.NO);
        editor.putInt(SETTLE_REDEEM_BDMS_STATUS, Constant.NO);
        editor.putInt(SETTLE_SMARTPAY_BDMS_STATUS, Constant.NO);
        editor.apply();
    }

    public int get(String key) {
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, Constant.NO);
    }

    public boolean getBoolean(String key) {
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        if (key.equals(IS_FIRST_RUN))
            return sp.getBoolean(key, true);
        return sp.getBoolean(key, false);
    }

    public void set(String key, int value) {
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void set(String key, boolean value) {
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void set(String key, String value) {
        SharedPreferences sp = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static boolean isFirstRun() {
        SharedPreferences control = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return control.getBoolean(IS_FIRST_RUN, true);
    }

    public static boolean isFirstInitNeeded() {
        SharedPreferences control = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return control.getBoolean(IS_FIRST_INITIAL_NEEDED, false);                      // this value will set to TRUE on DonwloadParamService download parameter by completed.
    }

    public static boolean isRequireDownloadParam() {
        SharedPreferences control = FinancialApplication.getApp().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return control.getBoolean(IS_FIRST_DOWNLOAD_PARAM_NEEDED, false);
    }
}
