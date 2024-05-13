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
package com.pax.pay.utils;

import android.util.SparseIntArray;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;

public class TransResultUtils {

    private static final SparseIntArray messageMap = new SparseIntArray();

    static {
        messageMap.put(TransResult.SUCC, R.string.dialog_trans_succ);
        messageMap.put(TransResult.ERR_TIMEOUT, R.string.err_timeout);
        messageMap.put(TransResult.ERR_CONNECT, R.string.err_connect);
        messageMap.put(TransResult.ERR_SEND, R.string.err_send);
        messageMap.put(TransResult.ERR_RECV, R.string.err_recv);
        messageMap.put(TransResult.ERR_PACK, R.string.err_pack);
        messageMap.put(TransResult.ERR_UNPACK, R.string.err_unpack);
        messageMap.put(TransResult.ERR_PACKET, R.string.err_packet);
        messageMap.put(TransResult.ERR_MAC, R.string.err_mac);
        messageMap.put(TransResult.ERR_PROC_CODE, R.string.err_proc_code);
        messageMap.put(TransResult.ERR_MSG, R.string.err_msg);
        messageMap.put(TransResult.ERR_TRANS_AMT, R.string.err_trans_amt);
        messageMap.put(TransResult.ERR_STAN_NO, R.string.err_stan_no);
        messageMap.put(TransResult.ERR_TERM_ID, R.string.err_term_id);
        messageMap.put(TransResult.ERR_MERCH_ID, R.string.err_merch_id);
        messageMap.put(TransResult.ERR_NO_TRANS, R.string.err_no_trans);
        messageMap.put(TransResult.ERR_NO_ORIG_TRANS, R.string.err_no_orig_trans);
        messageMap.put(TransResult.ERR_HAS_VOIDED, R.string.err_has_voided);
        messageMap.put(TransResult.ERR_VOID_UNSUPPORTED, R.string.err_un_voided);
        messageMap.put(TransResult.ERR_COMM_CHANNEL, R.string.err_comm_channel);
        messageMap.put(TransResult.ERR_HOST_REJECT, R.string.err_host_reject);
        messageMap.put(TransResult.ERR_USER_CANCEL, R.string.err_user_cancel);
        messageMap.put(TransResult.ERR_NEED_SETTLE_NOW, R.string.err_need_settle_now);
        messageMap.put(TransResult.ERR_NEED_SETTLE_LATER, R.string.err_need_settle_later);
        messageMap.put(TransResult.ERR_NO_FREE_SPACE, R.string.err_no_free_space);
        messageMap.put(TransResult.ERR_NOT_SUPPORT_TRANS, R.string.err_unsupported_trans);
        messageMap.put(TransResult.ERR_BATCH_UP_NOT_COMPLETED, R.string.err_batch_up_break_need_continue);
        messageMap.put(TransResult.ERR_CARD_NO, R.string.err_original_cardno);
        messageMap.put(TransResult.ERR_PASSWORD, R.string.err_password);
        messageMap.put(TransResult.ERR_PARAM, R.string.err_param);
        messageMap.put(TransResult.ERR_AMOUNT, R.string.err_amount);
        messageMap.put(TransResult.ERR_CARD_DENIED, R.string.err_card_denied);
        messageMap.put(TransResult.ERR_ADJUST_UNSUPPORTED, R.string.err_unadjusted_unsupported);
        messageMap.put(TransResult.ERR_CARD_UNSUPPORTED, R.string.err_card_unsupported);
        messageMap.put(TransResult.ERR_CARD_EXPIRED, R.string.err_expired_card);
        messageMap.put(TransResult.ERR_CARD_INVALID, R.string.err_card_pan);
        messageMap.put(TransResult.ERR_UNSUPPORTED_FUNC, R.string.err_unsupported_func);
        messageMap.put(TransResult.ERR_CLSS_PRE_PROC, R.string.err_clss_preproc_fail);
        messageMap.put(TransResult.ERR_INVALID_EMV_QR, R.string.err_invalid_emv_qr);
        messageMap.put(TransResult.ERR_PROMPT_INVALID_APPR_CODE, R.string.err_prompt_invalid_appr_code);
        messageMap.put(TransResult.ERR_TLE_NOT_LOAD, R.string.err_tle_not_load);
        messageMap.put(TransResult.ERR_TLE_REQUEST, R.string.err_tle_request);
        messageMap.put(TransResult.ERR_UPI_LOAD, R.string.err_load_upi);
        messageMap.put(TransResult.ERR_UPI_TMK_ACTIVATE, R.string.err_tmk_activate_upi);
        messageMap.put(TransResult.ERR_UPI_LOGON, R.string.err_logon_upi);
        messageMap.put(TransResult.ERR_NO_RESPONSE, R.string.err_no_response);
        messageMap.put(TransResult.ERR_COMMUNICATION, R.string.err_communication);
        messageMap.put(TransResult.ERR_UPI_NOT_LOGON, R.string.err_upi_not_logon);
        messageMap.put(TransResult.ERR_REFERRAL_CALL_ISSUER, R.string.response_01);
        messageMap.put(TransResult.ERR_DOWNLOAD_FAILED, R.string.err_download_failed);
        messageMap.put(TransResult.ERR_PROCESS_FAILED, R.string.err_process_failed);
        messageMap.put(TransResult.ERR_INVALID_TEMPLATE_ID, R.string.err_invalid_template_id);
        messageMap.put(TransResult.ERR_TEMPLATE_ID_EXISTS, R.string.err_template_id_exists);
        messageMap.put(TransResult.ERR_INVALID_ORDER_ID, R.string.err_invalid_order_id);
        messageMap.put(TransResult.ERR_INVALID_TRANCE_ID, R.string.err_invalid_trace_id);
        messageMap.put(TransResult.ERR_RECONCILE_FAILED, R.string.settle_err_reconcile);
        messageMap.put(TransResult.ERR_SETTLE_NOT_COMPLETED, R.string.has_trans_for_settle);
        messageMap.put(TransResult.ERR_HOST_NOT_FOUND, R.string.err_host_not_found);
        messageMap.put(TransResult.ERR_SETTLEMENT_FAIL, R.string.settle_fail);
        messageMap.put(TransResult.ERR_FAIL_GET_QR, R.string.get_qr_fail);
        messageMap.put(TransResult.ERR_SP200_NOT_USE, R.string.err_sp200_not_use);
        messageMap.put(TransResult.ERR_SP200_FAIL, R.string.err_sp200_fail);
        messageMap.put(TransResult.ERR_SP200_UPDATE_FAILED, R.string.err_sp200_fail);
        messageMap.put(TransResult.ERCM_OTHER_SETTING_SUCC, R.string.listener_verifone_erm_other_setting_failure);
        messageMap.put(TransResult.ERCM_OTHER_SETTING_ERR, R.string.listener_verifone_erm_other_setting_failure);
        messageMap.put(TransResult.ERCM_OTHER_SETTING_USER_CANCEL, R.string.listener_verifone_erm_other_setting_user_cancel);
        messageMap.put(TransResult.ERCM_ERROR_EXTRACT_FIELD63, R.string.listener_verifone_erm_error_extract_field_63);
        messageMap.put(TransResult.ERCM_PBK_DOWNLOAD_DECLINED, R.string.listener_verifone_erm_publickey_download_failure);
        messageMap.put(TransResult.ERCM_SSK_DOWNLOAD_DECLINED, R.string.listener_verifone_erm_sessionkey_renewal_failure);
        messageMap.put(TransResult.ERCM_INITIAL_PROCESS_FAILED, R.string.listener_verifone_erm_initial_process_failure);
        messageMap.put(TransResult.ERCM_INITIAL_INFO_NOT_READY, R.string.listener_verifone_erm_initial_info_not_ready);
        messageMap.put(TransResult.ERCM_UPLOAD_FAIL, R.string.msg_erm_upload_fail);
        messageMap.put(TransResult.ERCM_UPLOAD_SESSIONKEY_RENEWAL_REQUIRED, R.string.trans_ereceipt_upload_sessionkey_renewal_required);
        messageMap.put(TransResult.ERCM_UPLOAD_SESSIONKEY_RENEWAL_RETRY_ERROR, R.string.trans_ereceipt_upload_sessionkey_renewal_retry_error);
        messageMap.put(TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR, R.string.ereceipt_upload_trans_maximum_exceed);
        messageMap.put(TransResult.DYNAMIC_OFFLINE_STILL_DISABLED, R.string.kbank_error_dynamic_offline_still_disabled);
        messageMap.put(TransResult.DYNAMIC_OFFLINE_TRANS_NOT_ALLOW, R.string.kbank_error_dynamic_offline_trans_not_allow);
        messageMap.put(TransResult.T1C_INQUIRY_MEMBER_NO_CARD_RECORD, R.string.t1c_inquiry_member_id_no_card_record);
        messageMap.put(TransResult.T1C_INQUIRY_MEMBER_SYSTEM_MALFUNCTION, R.string.t1c_inquiry_member_id_system_malfunction);
        messageMap.put(TransResult.ERR_OFFLINE_UNSUPPORTED, R.string.err_offline_unsupported);
        messageMap.put(TransResult.ERCM_ESETTLE_REPORT_UPLOAD_FAIL, R.string.msg_erm_esettle_report_upload_failed);
        messageMap.put(TransResult.ERCM_ESETTLE_REPORT_NO_FILE_FOR_UPLOAD, R.string.msg_erm_esettle_report_no_file_for_upload);
        messageMap.put(TransResult.ERCM_ESETTLE_REPORT_STORAGE_NOT_FOUND, R.string.msg_erm_esettle_report_storage_not_found);
        messageMap.put(TransResult.ERCM_ESETTLE_REPORT_INVALID_DIRECTORY_TYPE, R.string.msg_erm_esettle_report_invalid_dir_type);
        messageMap.put(TransResult.ERR_INVALID_APPR_CODE, R.string.err_invalid_appr_code);
        messageMap.put(TransResult.ERR_SCB_CONNECTION, R.string.err_scb_no_connect);
        messageMap.put(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, R.string.err_amex_api_forward_to_amex_app);
        messageMap.put(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, R.string.err_amex_api_txn_exception);
        messageMap.put(TransResult.ERR_AMEX_APP_NOT_INSTALLED, R.string.err_amex_app_not_installed);
        messageMap.put(TransResult.ERR_AMEX_PARAM_UPDATE_FAIL, R.string.err_amex_param_update_fail);
        messageMap.put(TransResult.ERR_ECR_DUPLICATE_SALE_REFERENCE_ID, R.string.err_ecr_duplicate_sale_reference_id);
        messageMap.put(TransResult.ECR_FOUND_DOUBLE_TRANSACTION_BLOCKED, R.string.err_ecr_double_trans_blocked);
        messageMap.put(TransResult.ERR_NO_LINKPOS_APP, R.string.err_no_linkpos_app);
        messageMap.put(TransResult.ERR_UNABLE_TO_INIT_LINKPOS_APP, R.string.err_unable_init_linkpos_app);
        messageMap.put(TransResult.ERR_QR_CREDIT_MISSING_FORCE_SETTLE_TIME, R.string.edc_qr_credit_missing_focrce_settle_time);
        messageMap.put(TransResult.ERR_QR_CREDIT_FORCE_SETTLE_INTERNAL_PROCESS, R.string.edc_qr_credit_focrce_settle_time_check_failed);
        messageMap.put(TransResult.ERR_NOT_ALLOW, R.string.err_not_allowed);
        messageMap.put(TransResult.ERR_SP200_UPDATE_INTERNAL_FAILED,R.string.err_sp200_internal_update_failed);
        messageMap.put(TransResult.ERR_HAS_ADJUSTED, R.string.err_has_adjusted);
        messageMap.put(TransResult.ERR_TRANS_NOW_ALLOW, R.string.err_trans_not_allow);
        messageMap.put(TransResult.ERR_READ_CARD,R.string.err_read_card);
        messageMap.put(TransResult.ERR_CARD_ENTRY_NOT_ALLOW, R.string.err_card_entry_not_allow);
        messageMap.put(TransResult.ERR_MISSING_INTERNAL_PROC_RESULT, R.string.err_missing_internal_result);
        messageMap.put(TransResult.ERR_MISSING_CARD_INFORMATION, R.string.err_missing_card_information);
        messageMap.put(TransResult.ERR_PREAUTH_CANCEL_UNSUPPORTED, R.string.err_preauth_cancel_unsupported);
        messageMap.put(TransResult.ERR_SALE_COMP_TRANS_AMOUNT_EXCEED,R.string.err_salecomp_trans_amount_exceed);
        messageMap.put(TransResult.ERR_HAS_SALE_COMPLETED,R.string.err_has_sale_completed);
        messageMap.put(TransResult.ERR_SALE_COMPLETE_UNSUPPORTED, R.string.err_sale_complete_unsupported);
        messageMap.put(TransResult.MULTI_MERCHANT_APPLY_MERC_INFO_FAILED,R.string.err_multi_merchant_apply_profile_failed);
        messageMap.put(TransResult.MULTI_MERCHANT_SELECTED_MERCHANT_ERROR,R.string.err_multimerchant_selected_profile_error);
        messageMap.put(TransResult.ERR_OFFLINE_UPLOAD_FAIL, R.string.err_offline_upload_fail);
    }

    private TransResultUtils() {

    }

    public static String getMessage(int ret) {
        String message;

//        if(ret > -3000) {
        int resourceId = messageMap.get(ret, -1);
        if (resourceId == -1) {
            message = Utils.getString(R.string.err_undefine) + "[" + ret + "]";
        } else {
            message = Utils.getString(resourceId);
        }
//        }
//        else {
//            message = BssResultUtils.getMessage(ret);
//        }
        return message;
    }
}
