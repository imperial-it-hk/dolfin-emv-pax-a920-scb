/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Xml;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.UILanguage;
import com.pax.pay.utils.Utils;
import com.pax.settings.spupgrader.SpUpgrader;
import com.pax.settings.spupgrader.Upgrade1To2;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import th.co.bkkps.utils.Log;

public class SysParam {

    private static final String TAG = "SysParam";

    private static final String UPGRADER_PATH = "com.pax.setting.spupgrader";

    private static final String IS_PARAM_FILE_EXIST = "IS_PARAM_FILE_EXIST";
    private static final String VERSION_TAG = "PARAM_VERSION";
    private static final int VERSION = 1;

    int exStanNo;
    int exTraceNo;
    String exCommType;
    int exCommTimeout;
    int exEdcReceiptNo;
    String exAcqName;
    int exSettlementReceiptNo;

    public enum NumberParam {
        COMM_TIMEOUT(Utils.getString(R.string.COMM_TIMEOUT)),
        MOBILE_LOGIN_WAIT_TIME(Utils.getString(R.string.MOBILE_LOGIN_WAIT_TIME)),
        MOBILE_HOST_PORT(Utils.getString(R.string.MOBILE_HOST_PORT)),
        MOBILE_HOST_PORT_BAK(Utils.getString(R.string.MOBILE_HOST_PORT_BAK)),
        LAN_HOST_PORT(Utils.getString(R.string.LAN_HOST_PORT)),
        LAN_HOST_PORT_BAK(Utils.getString(R.string.LAN_HOST_PORT_BAK)),
        COMM_REDIAL_TIMES(Utils.getString(R.string.COMM_REDIAL_TIMES)),
        EDC_RECEIPT_NUM(Utils.getString(R.string.EDC_RECEIPT_NUM)),
        EDC_SMTP_SSL_PORT(Utils.getString(R.string.EDC_SMTP_SSL_PORT)),
        EDC_SMTP_PORT(Utils.getString(R.string.EDC_SMTP_PORT)),
        EDC_REVERSAL_RETRY(Utils.getString(R.string.EDC_REVERSAL_RETRY)),
        MAX_TRANS_COUNT(Utils.getString(R.string.MAX_TRANS_COUNT)),
        MK_INDEX(Utils.getString(R.string.MK_INDEX)),
        MK_INDEX_MANUAL(Utils.getString(R.string.MK_INDEX_MANUAL)),
        OFFLINE_TC_UPLOAD_TIMES(Utils.getString(R.string.OFFLINE_TC_UPLOAD_TIMES)),  // 离线上送次数
        OFFLINE_TC_UPLOAD_NUM(Utils.getString(R.string.OFFLINE_TC_UPLOAD_NUM)), // 自动上送累计笔数
        EDC_STAN_NO(Utils.getString(R.string.EDC_STAN_NO)),
        EDC_TRACE_NO(Utils.getString(R.string.EDC_TRACE_NO)),
        EDC_NUM_OF_SLIP_LINEPAY(Utils.getString(R.string.EDC_NUM_OF_SLIP_LINEPAY)),
        MAX_LIMIT_ERM_ERECEPT_PENDING_UPLOAD(Utils.getString(R.string.MAX_LIMIT_ERM_ERECEPT_PENDING_UPLOAD)),

        QUICK_PASS_TRANS_PIN_FREE_AMOUNT(Utils.getString(R.string.QUICK_PASS_TRANS_PIN_FREE_AMOUNT)),
        QUICK_PASS_TRANS_SIGN_FREE_AMOUNT(Utils.getString(R.string.QUICK_PASS_TRANS_SIGN_FREE_AMOUNT)),

        SCREEN_TIMEOUT(Utils.getString(R.string.SCREEN_TIMEOUT)),
        PASSWORD_FAILED_COUNT(Utils.getString(R.string.PASSWORD_FAILED_COUNT)),
        EDC_AID_FILE_UPLOAD_STATUS(Utils.getString(R.string.EDC_AID_FILE_UPLOAD_STATUS)),
        EDC_CARD_RANGE_FILE_UPLOAD_STATUS(Utils.getString(R.string.EDC_CARD_RANGE_FILE_UPLOAD_STATUS)),
        EDC_ISSUER_FILE_UPLOAD_STATUS(Utils.getString(R.string.EDC_ISSUER_FILE_UPLOAD_STATUS)),
        EDC_KIOSK_TIMEOUT(Utils.getString(R.string.EDC_KIOSK_TIMEOUT)),
        VF_ERCM_NO_OF_SLIP(Utils.getString(R.string.VF_ERCM_NO_OF_SLIP)),
        VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD(Utils.getString(R.string.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD)),

        KBANK_DYNAMIC_OFFLINE_SESSION_MINUTE(Utils.getString(R.string.KBANK_DYNAMIC_OFFLINE_SESSION_MINUTE)),
        DOLFIN_QR_ON_SCREEN_TIMEOUT(Utils.getString(R.string.DOLFIN_QR_ON_SCREEN_TIMEOUT)),

        SCREEN_TIME_OUT_SEARCH_CARD(Utils.getString(R.string.SCREEN_TIME_OUT_SEARCH_CARD)),
        EDC_QR_TAG_31_ECR_RETURN_MODE(Utils.getString(R.string.EDC_QR_TAG_31_ECR_RETURN_MODE)),
        THAI_QR_INQUIRY_MAX_COUNT_FOR_SHOW_VERIFY_QR_BUTTON(Utils.getString(R.string.THAI_QR_INQUIRY_MAX_COUNT_FOR_SHOW_VERIFY_QR_BUTTON)),

        EDC_SUPPORT_REF1_2_MODE(Utils.getString(R.string.EDC_SUPPORT_REF1_2_MODE)),
        EDC_MAX_PERCENTAGE_TIP_ADJUST(Utils.getString(R.string.EDC_MAX_PERCENTAGE_TIP_ADJUST)),
        EDC_MAX_PERCENTAGE_SALE_COMPLETION(Utils.getString(R.string.EDC_MAX_PERCENTAGE_SALE_COMPLETION)),
        EDC_NUMBER_OF_DAY_KEEP_PREAUTH_TRANS(Utils.getString(R.string.EDC_NUMBER_OF_DAY_KEEP_PREAUTH_TRANS)),
        ACQ_ALIWECHAT_B_SCAN_C_NII(Utils.getString(R.string.ACQ_ALIWECHAT_B_SCAN_C_NII)),
        ;

        private String str;

        NumberParam(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum StringParam {
        /**
         * communication method
         */
        COMM_TYPE(Utils.getString(R.string.COMM_TYPE)),
        COMM_MERC_NAME(Utils.getString(R.string.COMM_MERC_NAME)),

        /**
         * mobile network
         */
        MOBILE_TEL_NO(Utils.getString(R.string.MOBILE_TEL_NO)),
        MOBILE_APN(Utils.getString(R.string.MOBILE_APN)),
        MOBILE_APN_SYSTEM(Utils.getString(R.string.MOBILE_APN_SYSTEM)),
        MOBILE_USER(Utils.getString(R.string.MOBILE_USER)),
        MOBILE_PWD(Utils.getString(R.string.MOBILE_PWD)),
        MOBILE_SIM_PIN(Utils.getString(R.string.MOBILE_SIM_PIN)),
        MOBILE_AUTH(Utils.getString(R.string.MOBILE_AUTH)),
        MOBILE_HOST_IP(Utils.getString(R.string.MOBILE_HOST_IP)),
        MOBILE_HOST_IP_BAK(Utils.getString(R.string.MOBILE_HOST_IP_BAK)),
        MOBILE_DOMAIN_NAME(Utils.getString(R.string.MOBILE_DOMAIN_NAME)),

        // 以太网参数
        LAN_LOCAL_IP(Utils.getString(R.string.LAN_LOCAL_IP)),
        LAN_NETMASK(Utils.getString(R.string.LAN_NETMASK)),
        LAN_GATEWAY(Utils.getString(R.string.LAN_GATEWAY)),
        LAN_DNS1(Utils.getString(R.string.LAN_DNS1)),
        LAN_DNS2(Utils.getString(R.string.LAN_DNS2)),
        LAN_HOST_IP(Utils.getString(R.string.LAN_HOST_IP)),
        LAN_HOST_IP_BAK(Utils.getString(R.string.LAN_HOST_IP_BAK)),

        EDC_MERCHANT_NAME_EN(Utils.getString(R.string.EDC_MERCHANT_NAME_EN)),
        EDC_MERCHANT_ADDRESS(Utils.getString(R.string.EDC_MERCHANT_ADDRESS)),
        EDC_MERCHANT_ADDRESS1(Utils.getString(R.string.EDC_MERCHANT_ADDRESS1)),
        EDC_CURRENCY_LIST(Utils.getString(R.string.EDC_CURRENCY_LIST)),
        EDC_LANGUAGE(Utils.getString(R.string.EDC_LANGUAGE)),
        EDC_PED_MODE(Utils.getString(R.string.EDC_PED_MODE)),
        EDC_DEFAULT_CAMERA(Utils.getString(R.string.EDC_DEFAULT_CAMERA)),

        EDC_SMTP_HOST(Utils.getString(R.string.EDC_SMTP_HOST)),
        EDC_SMTP_USERNAME(Utils.getString(R.string.EDC_SMTP_USERNAME)),
        EDC_SMTP_PASSWORD(Utils.getString(R.string.EDC_SMTP_PASSWORD)),
        EDC_SMTP_FROM(Utils.getString(R.string.EDC_SMTP_FROM)),
        KEY_ALGORITHM(Utils.getString(R.string.KEY_ALGORITHM)),

        SEC_SYS_PWD(Utils.getString(R.string.SEC_SYS_PWD)),
        SEC_MERCHANT_PWD(Utils.getString(R.string.SEC_MERCHANT_PWD)),
        SEC_CONFIG_PWD(Utils.getString(R.string.SEC_CONFIG_PWD)),
        SEC_TERMINAL_PWD(Utils.getString(R.string.SEC_TERMINAL_PWD)),
        SEC_VOID_PWD(Utils.getString(R.string.SEC_VOID_PWD)),
        SEC_REFUND_PWD(Utils.getString(R.string.SEC_REFUND_PWD)),
        SEC_ADJUST_PWD(Utils.getString(R.string.SEC_ADJUST_PWD)),
        SEC_SETTLE_PWD(Utils.getString(R.string.SEC_SETTLE_PWD)),
        SEC_TLE_PWD(Utils.getString(R.string.SEC_TLE_PWD)),
        SEC_CANCEL_PWD(Utils.getString(R.string.SEC_CANCEL_PWD)),
        SEC_DYNAMIC_OFFLINE_PWD(Utils.getString(R.string.SEC_DYNAMIC_OFFLINE_PWD)),
        SEC_OFFLINE_PWD(Utils.getString(R.string.SEC_OFFLINE_PWD)),
        SEC_PREAUTH_PWD(Utils.getString(R.string.SEC_PREAUTH_PWD)),
        SEC_TIP_ADJUSTMENT_PWD(Utils.getString(R.string.SEC_TIP_ADJUSTMENT_PWD)),

        ACQ_NAME(Utils.getString(R.string.ACQ_NAME)),  //当前收单行名字

        MK_VALUE(Utils.getString(R.string.MK_VALUE)),
        PK_VALUE(Utils.getString(R.string.PK_VALUE)),
        AK_VALUE(Utils.getString(R.string.AK_VALUE)),
        RABBIT_KEY(Utils.getString(R.string.RABBIT_KEY)),
        LINKPOS_PROTOCOL(Utils.getString(R.string.LINKPOS_PROTOCOL)),
        LINKPOS_COMM_TYPE(Utils.getString(R.string.LINKPOS_COMM_TYPE)),
        SAVE_FILE_PATH_PARAM(Utils.getString(R.string.SAVE_FILE_PATH_PARAM)),
        EDC_KERRY_API_URL(Utils.getString(R.string.EDC_KERRY_API_URL)),
        EDC_CURRENT_MERCHANT(Utils.getString(R.string.EDC_CURRENT_MERCHANT)),

        VERIFONE_ERCM_TERMINAL_SERIALNUMBER(Utils.getString(R.string.VF_ERCM_TERMINAL_SERIALNUMBER)),
        VERIFONE_ERCM_BANK_CODE(Utils.getString(R.string.VF_ERCM_BANK_CODE)),
        VERIFONE_ERCM_STORE_CODE(Utils.getString(R.string.VF_ERCM_STORE_CODE)),
        VERIFONE_ERCM_MERCHANT_CODE(Utils.getString(R.string.VF_ERCM_MERCHANT_CODE)),
        VERIFONE_ERCM_TERMINAL_INTIATED(Utils.getString(R.string.VF_ERCM_TERMINAL_INITIATED)),

        VERIFONE_ERCM_KEK_VERSION(Utils.getString(R.string.VF_ERCM_KEK_VERSION)),
        VERIFONE_ERCM_KEK_TYPE(Utils.getString(R.string.VF_ERCM_KEK_TYPE)),
        VERIFONE_ERCM_SESSION_KEY_KCV(Utils.getString(R.string.VF_ERCM_SESSION_KEY_KCV)),
        VERIFONE_ERCM_SESSION_KEY_ORI_DATA(Utils.getString(R.string.VF_ERCM_SESSION_KEY_ORI_DATA)),
        VERIFONE_ERCM_SESSION_KEY_ENC_DATA(Utils.getString(R.string.VF_ERCM_SESSION_KEY_ENC_DATA)),


        DYNAMIC_OFFLINE_MODE_DATETIME(Utils.getString(R.string.KBANK_DYNAMIC_OFFLINE_DATETIME)),
        TLE_PARAMETER_FILE_PATH(Utils.getString(R.string.TLE_PARAMETER_FILE_PATH)),

        EDC_CTLS_TRANS_LIMIT(Utils.getString(R.string.EDC_CTLS_TRANS_LIMIT)),
        EDC_MAXIMUM_AMOUNT(Utils.getString(R.string.EDC_MAXIMUM_AMOUNT)),
        EDC_MINIMUM_AMOUNT(Utils.getString(R.string.EDC_MINIMUM_AMOUNT)),

        EDC_SETTLEMENT_MODE(Utils.getString(R.string.EDC_SETTLEMENT_MODE)),
        EDC_SETTLE_MODE_TESTING_TIME_INTERVAL(Utils.getString(R.string.EDC_SETTLE_MODE_TESTING_TIME_INTERVAL)),

        EDC_DISP_TEXT_REF1(Utils.getString(R.string.EDC_DISP_TEXT_REF1)),
        EDC_DISP_TEXT_REF2(Utils.getString(R.string.EDC_DISP_TEXT_REF2)),
        ;


        private String str;

        StringParam(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum BooleanParam {
        LAN_DHCP(Utils.getString(R.string.LAN_DHCP)),
        MOBILE_KEEP_ALIVE(Utils.getString(R.string.MOBILE_KEEP_ALIVE)),

        EDC_CONFIRM_PROCESS(Utils.getString(R.string.EDC_CONFIRM_PROCESS)),
        EDC_SUPPORT_TIP(Utils.getString(R.string.EDC_SUPPORT_TIP)), // support tip
        SUPPORT_USER_AGREEMENT(Utils.getString(R.string.SUPPORT_USER_AGREEMENT)), // Support user instructions to read
        EDC_SUPPORT_REFUND(Utils.getString(R.string.EDC_SUPPORT_REFUND)),

        EDC_ENABLE_PAPERLESS(Utils.getString(R.string.EDC_ENABLE_PAPERLESS)),
        EDC_SMTP_ENABLE_SSL(Utils.getString(R.string.EDC_SMTP_ENABLE_SSL)),

        OTHTC_VERIFY(Utils.getString(R.string.OTHTC_VERIFY)),  // Enter the supervisor password to cancel the return transaction

        /**
         * Bank Card QuickPass Parameters
         **/
        QUICK_PASS_TRANS_PIN_FREE_SWITCH(Utils.getString(R.string.QUICK_PASS_TRANS_PIN_FREE_SWITCH)),
        QUICK_PASS_TRANS_FLAG(Utils.getString(R.string.QUICK_PASS_TRANS_FLAG)),
        QUICK_PASS_TRANS_SWITCH(Utils.getString(R.string.QUICK_PASS_TRANS_SWITCH)),

        QUICK_PASS_TRANS_CDCVM_FLAG(Utils.getString(R.string.QUICK_PASS_TRANS_CDCVM_FLAG)),

        QUICK_PASS_TRANS_SIGN_FREE_FLAG(Utils.getString(R.string.QUICK_PASS_TRANS_SIGN_FREE_FLAG)),

        TTS_SALE(Utils.getString(R.string.TTS_SALE)),
        TTS_VOID(Utils.getString(R.string.TTS_VOID)),
        TTS_REFUND(Utils.getString(R.string.TTS_REFUND)),
        TTS_PREAUTH(Utils.getString(R.string.TTS_PREAUTH)),
        TTS_ADJUST(Utils.getString(R.string.TTS_ADJUST)),

        IS_SETTLED_RABBIT(Utils.getString(R.string.IS_SETTLED_RABBIT)),
        IS_DOWNLOADED_CD(Utils.getString(R.string.IS_DOWNLOADED_CD)),
        IS_TLE_AUTO_INIT_REQUIRED(Utils.getString(R.string.IS_DOWNLOADED_CD)),

        EDC_ENABLE_E_SIGNATURE(Utils.getString(R.string.EDC_ENABLE_E_SIGNATURE)),
        EDC_ENABLE_KERRY_API(Utils.getString(R.string.EDC_ENABLE_KERRY_API)),
        EDC_ENABLE_GRAND_TOTAL(Utils.getString(R.string.EDC_ENABLE_GRAND_TOTAL)),
        NEED_UPDATE_PARAM(Utils.getString(R.string.NEED_UPDATE_PARAM)),
        FLAG_UPDATE_PARAM(Utils.getString(R.string.FLAG_UPDATE_PARAM)),
        NEED_CONSEQUENT_PARAM_INITIAL(Utils.getString(R.string.NEED_CONSEQUENT_PARAM_INITIAL)),
        NEED_UPDATE_SCREEN_TIMEOUT(Utils.getString(R.string.NEED_UPDATE_SCREEN_TIMEOUT)),
        NEED_SWITCH_APN(Utils.getString(R.string.NEED_SWITCH_APN)),
        EDC_ENABLE_WALLET_C_SCAN_B(Utils.getString(R.string.EDC_ENABLE_WALLET_C_SCAN_B)),
        EDC_ENABLE_QR_BARCODE(Utils.getString(R.string.EDC_ENABLE_QR_BARCODE)),
        EDC_KIOSK_MODE(Utils.getString(R.string.EDC_KIOSK_MODE)),
        EDC_ENABLE_IMG_ON_END_RECEIPT(Utils.getString(R.string.EDC_ENABLE_IMG_ON_END_RECEIPT)),

        //contactless
        EDC_ENABLE_CONTACTLESS(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS)),
        EDC_ENABLE_CONTACTLESS_VISA(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_VISA)),
        EDC_ENABLE_CONTACTLESS_MASTER(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_MASTER)),
        EDC_ENABLE_CONTACTLESS_JCB(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_JCB)),
        EDC_ENABLE_CONTACTLESS_UP(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_UP)),
        EDC_ENABLE_CONTACTLESS_TPN(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_TPN)),
        EDC_ENABLE_CONTACTLESS_AMEX(Utils.getString(R.string.EDC_ENABLE_CONTACTLESS_AMEX)),

        EDC_ENABLE_KEYIN(Utils.getString(R.string.EDC_ENABLE_KEYIN)),
        EDC_ENABLE_QR_BARCODE_ALIPAY_WECHAT(Utils.getString(R.string.EDC_ENABLE_QR_BARCODE_ALIPAY_WECHAT)),

        EDC_ENABLE_LOGGLY(Utils.getString(R.string.EDC_ENABLE_LOGGLY)),
        EDC_ENABLE_VOID_WITH_STAND(Utils.getString(R.string.EDC_ENABLE_VOID_WITH_STAND)),

        VF_ERCM_ENABLE(Utils.getString(R.string.VF_ERCM_ENABLE)),
        VF_ERCM_ENABLE_PRINT_AFTER_TXN(Utils.getString(R.string.VF_ERCM_ENABLE_PRINT_AFTER_TXN)),
        VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD(Utils.getString(R.string.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD)),
        VF_ERCM_ENABLE_PRINT_PRE_SETTLE(Utils.getString(R.string.VF_ERCM_ENABLE_PRINT_PRE_SETTLE)),
        VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS(Utils.getString(R.string.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS)),
        EDC_SUPPORT_SP200(Utils.getString(R.string.EDC_SUPPORT_SP200)),
        EDC_ENABLE_QR_BARCODE_COD(Utils.getString(R.string.EDC_ENABLE_QR_BARCODE_COD)),


        DYNAMIC_OFFLINE_MODE_ENABLED(Utils.getString(R.string.KBANK_DYNAMIC_OFFLINE_ENABLED)),
        KBANK_DYNAMIC_OFFLINE_SHOW_MENU(Utils.getString(R.string.KBANK_DYNAMIC_OFFLINE_SHOW_MENU)),

        EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD(Utils.getString(R.string.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD)) ,      // Enable force selection True=Force select, False=Manual select by user

        EDC_ENABLE_SALE_CREDIT_MENU(Utils.getString(R.string.EDC_ENABLE_SALE_CREDIT_MENU)),
        EDC_ENABLE_VOID_MENU(Utils.getString(R.string.EDC_ENABLE_VOID_MENU)),
        EDC_ENABLE_KPLUS_MENU(Utils.getString(R.string.EDC_ENABLE_KPLUS_MENU)),
        EDC_ENABLE_ALIPAY_MENU(Utils.getString(R.string.EDC_ENABLE_ALIPAY_MENU)),
        EDC_ENABLE_WECHAT_MENU(Utils.getString(R.string.EDC_ENABLE_WECHAT_MENU)),
        EDC_ENABLE_QR_CREDIT_MENU(Utils.getString(R.string.EDC_ENABLE_QR_CREDIT_MENU)),
        EDC_ENABLE_SMART_PAY_MENU(Utils.getString(R.string.EDC_ENABLE_SMART_PAY_MENU)),
        EDC_ENABLE_REDEEM_MENU(Utils.getString(R.string.EDC_ENABLE_REDEEM_MENU)),
        EDC_ENABLE_CT1_EPP_MENU(Utils.getString(R.string.EDC_ENABLE_CT1_EPP_MENU)),
        EDC_ENABLE_AMEX_EPP_MENU(Utils.getString(R.string.EDC_ENABLE_AMEX_EPP_MENU)),
        EDC_ENABLE_SCB_IPP_MENU(Utils.getString(R.string.EDC_ENABLE_SCB_IPP_MENU)),
        EDC_ENABLE_SCB_REDEEM_MENU(Utils.getString(R.string.EDC_ENABLE_SCB_REDEEM_MENU)),
        EDC_ENABLE_DOLFIN_MENU(Utils.getString(R.string.EDC_ENABLE_DOLFIN_MENU)),
        EDC_ENABLE_KCHECKID_MAIN_MENU(Utils.getString(R.string.EDC_ENABLE_KCHECKID_MAIN_MENU)),
        EDC_ENABLE_KCHECKID_SUB_INQUIRY_MENU(Utils.getString(R.string.EDC_ENABLE_KCHECKID_SUB_INQUIRY_MENU)),
        EDC_ENABLE_MYPROMPT_MENU(Utils.getString(R.string.EDC_ENABLE_MYPROMPT_MENU)),
        EDC_ENABLE_DOLFIN_IPP_MENU(Utils.getString(R.string.EDC_ENABLE_DOLFIN_IPP_MENU)),
        EDC_ENABLE_ALIPAY_BSCANC_MENU(Utils.getString(R.string.EDC_ENABLE_ALIPAY_BSCANC_MENU)),
        EDC_ENABLE_WECHAT_BSCANC_MENU(Utils.getString(R.string.EDC_ENABLE_WECHAT_BSCANC_MENU)),

        EDC_SETTLEMENT_RECEIPT_ENABLE(Utils.getString(R.string.EDC_SETTLEMENT_RECEIPT_ENABLE)),
        EDC_AUDITREPORT_RECEIPT_ENABLE(Utils.getString(R.string.EDC_AUDITREPORT_RECEIPT_ENABLE)),
        EDC_DOUBLE_BLOCKED_TRANS_ENABLE(Utils.getString(R.string.EDC_DOUBLE_BLOCKED_TRANS_ENABLE)),
        
        EDC_LINKPOS_BYPASS_CONFIRM_VOID(Utils.getString(R.string.EDC_LINKPOS_BYPASS_CONFIRM_VOID)),
        EDC_LINKPOS_BYPASS_CONFIRM_SETTLE(Utils.getString(R.string.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE)),

        EDC_QR_TAG_31_ENABLE(Utils.getString(R.string.EDC_QR_TAG_31_ENABLE)),
        EDC_QR_TAG_31_REPORT_GROUPING_OLD_STYLE(Utils.getString(R.string.EDC_QR_TAG_31_REPORT_GROUPING_OLD_STYLE)),


        EDC_ENABLE_SETTLE_MODE_TESTING(Utils.getString(R.string.EDC_ENABLE_SETTLE_MODE_TESTING)),
        EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE(Utils.getString(R.string.EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE)),


        EDC_ENABLE_PREAUTH(Utils.getString(R.string.EDC_ENABLE_PREAUTH)),
        EDC_ENABLE_OFFLINE(Utils.getString(R.string.EDC_ENABLE_OFFLINE)),
        EDC_ENABLE_TIP_ADJUST(Utils.getString(R.string.EDC_ENABLE_TIP_ADJUST)),


        ;


        private String str;

        BooleanParam(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    private static SysParam mSysParam;

    private static UpdateListener updateListener;

    private SysParam() {
        load(); // 加载参数内容到SysParam中
    }

    public static synchronized SysParam getInstance() {
        if (mSysParam == null) {
            mSysParam = new SysParam();
        }
        return mSysParam;
    }

    public interface UpdateListener {
        void onErr(String prompt);
    }

    public static void setUpdateListener(UpdateListener listener) {
        updateListener = listener;
    }

    // 系统参数加载，如果sp中不存在则添加
    private void load() {
        // 设置默认参数值
        Log.d(TAG, "SysParam load()");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        Editor editor = sharedPreferences.edit();

        if (isParamFileExist()) {
            try {
                for (int i = getVersion(); i < VERSION; ++i) {
                    SpUpgrader.upgrade(editor, i, i + 1, UPGRADER_PATH);
                    editor.putInt(SysParam.VERSION_TAG, i + 1);
                    editor.apply();
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "", e);
            }
            setDefaultLanguage(editor);
            return;
        }

        if (editor != null) {
            Log.d(TAG, "SysParam load() start parsing");
            PullParseService pps = new PullParseService(editor);
            pps.parse();

            editor.putBoolean(SysParam.IS_PARAM_FILE_EXIST, true);
            editor.putInt(SysParam.VERSION_TAG, VERSION);

            //special tags
            int commTimeout = Integer.parseInt(FinancialApplication.getApp().getResources().getStringArray(R.array.edc_connect_time_entries)[0]);
            set(editor, NumberParam.COMM_TIMEOUT, commTimeout);
            // 通讯方式
            set(editor, StringParam.COMM_TYPE, Constant.CommType.MOBILE.toString());

            set(editor, StringParam.EDC_CURRENCY_LIST, "Thai (Thailand)");

            // Set default EDC language if needed :: Begin
            setDefaultLanguage(editor);
            // Set default EDC language if needed :: End


            set(editor, StringParam.EDC_PED_MODE, FinancialApplication.getApp().getResources().getStringArray(R.array.edc_ped_mode_value_entries)[0]);

            set(editor, StringParam.KEY_ALGORITHM, Constant.Des.TRIP_DES.toString()); // 密钥算法

            if (VERSION >= 2) {
                new Upgrade1To2().upgrade(editor);
            }

            editor.apply();
            Log.d(TAG, "SysParam load() end parsing");
        }
    }

    private void setDefaultLanguage(Editor editor) {
        // Author: Patchara K.
        // To fix the issue that when cashier turn off and turn on the EDC, the language settings was set to "THAI".
        // This was because we binded the EDC's language settings to the currency that is always be "THAI".
        // ---------------------------------------------------------------------------------------------------------
        // Set default EDC language if needed :: Begin
        String language = UILanguage.ENGLISH.getDisplay();
        try {
            language = get(StringParam.EDC_LANGUAGE);

            if (language.compareToIgnoreCase(UILanguage.ENGLISH.getDisplay()) != 0
                    && language.compareToIgnoreCase(UILanguage.THAI.getDisplay()) != 0) {
                language = UILanguage.ENGLISH.getDisplay();
            }
        }
        catch (Exception ex) {
            language = UILanguage.ENGLISH.getDisplay();
        }
        finally {
            set(editor, StringParam.EDC_LANGUAGE, language);
        }
        // Set default EDC language if needed :: End
    }

    public void loadNewPrefParam() {
        Log.d("DownloadManager", "SysParam loadNewDefaultParam()");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        Editor editor = sharedPreferences.edit();

        if (editor != null) {
            Log.d("DownloadManager", "SysParam loadNewDefaultParam() start parsing");

            exStanNo = get(NumberParam.EDC_STAN_NO);
            exTraceNo = get(NumberParam.EDC_TRACE_NO);
            exCommType = get(StringParam.COMM_TYPE);
            exCommTimeout = get(NumberParam.COMM_TIMEOUT);
            exEdcReceiptNo = get(NumberParam.EDC_RECEIPT_NUM);
            exAcqName = get(StringParam.ACQ_NAME);

            PullParseService pps = new PullParseService(editor);
            pps.parse();

            editor.putBoolean(SysParam.IS_PARAM_FILE_EXIST, true);
            editor.putInt(SysParam.VERSION_TAG, VERSION);

            setDefaultParam(editor);

            // Set default EDC language if needed :: Begin
            setDefaultLanguage(editor);
            // Set default EDC language if needed :: End


            set(editor, StringParam.EDC_PED_MODE, FinancialApplication.getApp().getResources().getStringArray(R.array.edc_ped_mode_value_entries)[0]);

            set(editor, StringParam.KEY_ALGORITHM, Constant.Des.TRIP_DES.toString()); // 密钥算法

            editor.apply();
            Log.d("DownloadManager", "SysParam loadNewDefaultParam() end parsing");
        }
    }

    public synchronized int get(NumberParam name) {
        return get(name, 0);
    }

    public synchronized int get(NumberParam name, int defValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        String temp = sharedPreferences.getString(name.toString(), null);
        if (temp != null) {
            try {
                return Integer.parseInt(temp);
            } catch (NumberFormatException e) {
                Log.w(TAG, "", e);
            }
        }
        return defValue;
    }

    public synchronized String get(StringParam name) {
        return get(name, null);
    }

    public synchronized String get(StringParam name, String defValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        return sharedPreferences.getString(name.toString(), defValue);
    }

    public synchronized boolean get(BooleanParam name) {
        return get(name, false);
    }

    public synchronized boolean get(BooleanParam name, boolean defValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        return sharedPreferences.getBoolean(name.toString(), defValue);
    }

    public synchronized long get(NumberParam name, long defValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        return sharedPreferences.getLong(name.toString(), defValue);
    }

    private synchronized void set(Editor editor, String name, String value) {
        editor.putString(name, value);
        editor.apply();
    }

    private synchronized void set(Editor editor, String name, boolean value) {
        editor.putBoolean(name, value);
        editor.apply();
    }

    public synchronized void set(NumberParam name, int value) {
        // Please use Component.incTraceNo(transData); to update  EDC_TRACE_NO
        // Please use Component.incStanNo(transData); to update  EDC_STAN_NO
        if(!name.equals(NumberParam.EDC_STAN_NO) && !name.equals(NumberParam.EDC_TRACE_NO) ){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
            Editor editor = sharedPreferences.edit();
            set(editor, name, value);
        }
    }

    public synchronized void set(NumberParam name, long value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        Editor editor = sharedPreferences.edit();
        editor.putLong(name.toString(), value);
    }

    private synchronized void set(Editor editor, NumberParam name, int value) {
        set(editor, name.toString(), String.valueOf(value));
    }

    public synchronized void set(StringParam name, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        Editor editor = sharedPreferences.edit();
        set(editor, name, value);
    }

    private synchronized void set(Editor editor, StringParam name, String value) {
        set(editor, name.toString(), value);
    }

    public synchronized void set(BooleanParam name, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        Editor editor = sharedPreferences.edit();
        set(editor, name, value);
    }

    private synchronized void set(Editor editor, BooleanParam name, boolean value) {
        set(editor, name.toString(), value);
    }

    public synchronized void set(NumberParam name, int value, boolean flag) {
        // use to update EDC_STAN_NO or EDC_TRACE_NO only
        if( flag && (name.equals(NumberParam.EDC_STAN_NO) || name.equals(NumberParam.EDC_TRACE_NO))){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
            Editor editor = sharedPreferences.edit();
            set(editor, name, value);
        }
    }

    private boolean isParamFileExist() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        return sharedPreferences.getBoolean(SysParam.IS_PARAM_FILE_EXIST, false);
    }

    private int getVersion() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        return sharedPreferences.getInt(SysParam.VERSION_TAG, 1);
    }

    public long getCtlsTransLimit() {
        return Utils.parseLongSafe(getCtlsTransLimitAsString(), 0);
    }

    public String getCtlsTransLimitAsString() {
        String strMaxAmt = get(StringParam.EDC_CTLS_TRANS_LIMIT, "150000");
        strMaxAmt = strMaxAmt.replaceAll("\\D+", "");
        return strMaxAmt;
    }

    public long getEDCMaxAmt() {
        String strMaxAmt = get(StringParam.EDC_MAXIMUM_AMOUNT);
        strMaxAmt = strMaxAmt.replaceAll("\\D+", "");
        return Utils.parseLongSafe(strMaxAmt, 0);
    }

    public long getEDCMinAmt() {
        String strMinAmt = get(StringParam.EDC_MINIMUM_AMOUNT);
        strMinAmt = strMinAmt.replaceAll("\\D+", "");
        return Utils.parseLongSafe(strMinAmt, 0);
    }

    public static class Constant {
        /**
         * 通讯类型
         */
        public enum CommType {
            LAN(Utils.getString(R.string.wifi)),
            MOBILE(Utils.getString(R.string.mobile)),
            WIFI(Utils.getString(R.string.wifi)),
            DEMO(Utils.getString(R.string.demo)),;

            private final String str;

            CommType(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        /**
         * SSL
         */
        public enum CommSslType {
            NO_SSL(Utils.getString(R.string.NO_SSL)),
            SSL(Utils.getString(R.string.SSL)),;

            private final String str;

            CommSslType(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        /**
         * des算法
         */
        public enum Des {
            DES(Utils.getString(R.string.keyManage_menu_des)),
            TRIP_DES(Utils.getString(R.string.keyManage_menu_3des));

            private final String str;

            Des(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        public enum LinkPosCommType {
            LINK_POS_WIFI(Utils.getString(R.string.linkpos_tcp)),
            LINK_POS_SERIAL(Utils.getString(R.string.linkpos_usb_to_serial));

            private final String str;

            LinkPosCommType(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        private Constant() {
            //do nothing
        }
    }

    private static class PullParseService {
        private Map<String, Integer> intMap = new HashMap<>();
        private Map<String, Boolean> boolMap = new HashMap<>();
        private Map<String, String> stringMap = new HashMap<>();
        private Editor editor;

        PullParseService(Editor editor) {
            this.editor = editor;
        }

        private void setIntOrString(String tag, String value) {
            try {
                int intVal = Integer.parseInt(value);
                intMap.put(tag, intVal);
            } catch (NumberFormatException e) {
                stringMap.put(tag, value);
            }
        }

        private String safeNextText(XmlPullParser parser) throws XmlPullParserException, IOException {
            String result = parser.nextText();
            if (parser.getEventType() != XmlPullParser.END_TAG) {
                parser.nextTag();
            }
            return result;
        }

        private void setTag(XmlPullParser parser) throws XmlPullParserException, IOException {
            if ("string".equals(parser.getName())) {//判断开始标签元素是否是string
                setIntOrString(parser.getAttributeValue(0), safeNextText(parser));
            } else if ("boolean".equals(parser.getName())) {
                boolMap.put(parser.getAttributeValue(0), Boolean.valueOf(parser.getAttributeValue(1)));
            }
        }

        void parse() {
            try {
                InputStream in = FinancialApplication.getApp().getResources().openRawResource(R.raw.pref);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(in, "UTF-8");
                int event = parser.getEventType();//产生第一个事件
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (XmlPullParser.START_TAG == event) {//判断当前事件是否是标签元素开始事件
                        setTag(parser);
                    }
                    event = parser.next();//进入下一个元素并触发相应事件
                }//end while
            } catch (IOException | XmlPullParserException e) {
                Log.e(TAG, "", e);
            }
            for (Map.Entry<String, Integer> i : intMap.entrySet()) {
                editor.putString(i.getKey(), String.valueOf(i.getValue()));
            }

            for (Map.Entry<String, Boolean> i : boolMap.entrySet()) {
                editor.putBoolean(i.getKey(), i.getValue());
            }

            for (Map.Entry<String, String> i : stringMap.entrySet()) {
                editor.putString(i.getKey(), i.getValue());
            }
        }
    }

    private void setDefaultParam(Editor editor) {
        set(editor, StringParam.EDC_CURRENCY_LIST, "Thai (Thailand)");

        if (exStanNo > 0) { set(editor, NumberParam.EDC_STAN_NO, exStanNo); }
        if (exTraceNo > 0) { set(editor, NumberParam.EDC_TRACE_NO, exTraceNo); }
        if (exEdcReceiptNo > 0) { set(editor, NumberParam.EDC_RECEIPT_NUM, exEdcReceiptNo); }

        if (exCommTimeout == 0) {
            int commTimeout = Integer.parseInt(FinancialApplication.getApp().getResources().getStringArray(R.array.edc_connect_time_entries)[0]);
            set(editor, NumberParam.COMM_TIMEOUT, commTimeout);
        } else {
            set(editor, NumberParam.COMM_TIMEOUT, exCommTimeout);
        }

        try {
            if (exCommType == null) {
                throw new Exception("Set pref default COMM_TYPE");
            }
            set(editor, StringParam.COMM_TYPE, exCommType);
        } catch (Exception e) {
            Log.e("DownloadManager", "", e.getMessage());
        }

        try {
            if (exAcqName == null) {
                throw new Exception("Set pref default ACQ_NAME");
            }
            set(editor, StringParam.ACQ_NAME, exAcqName);
        } catch (Exception e) {
            Log.e("DownloadManager", "", e.getMessage());
        }
    }
}
