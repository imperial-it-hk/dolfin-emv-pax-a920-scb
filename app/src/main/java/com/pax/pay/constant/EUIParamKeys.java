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
package com.pax.pay.constant;

public enum EUIParamKeys {
    /**
     * 提示信息1
     */
    PROMPT_1,
    /**
     * 提示信息2
     */
    PROMPT_2,
    /**
     *
     */
    PROMPT_REMARK,
    /**
     * 输入1数据类型, {@link com.pax.pay.trans.action.ActionInputTransData.EInputType}
     */
    INPUT_TYPE,
    /**
     * 输入1数据最大长度
     */
    INPUT_MAX_LEN,
    /**
     * 输入数据最小长度
     */
    INPUT_MIN_LEN,
    /**
     * 显示内容
     */
    CONTENT,
    /**
     * transaction type
     */
    TRANS_TYPE,
    /**
     * 交易金额
     */
    TRANS_AMOUNT,
    /**
     * tip amount
     */
    TIP_AMOUNT,
    /**
     * 交易日期
     */
    TRANS_DATE,

    /**
     * 寻卡界面类型
     */
    SEARCH_CARD_UI_TYPE,

    /**
     * 是否可直接撤销最后一笔交易
     */
    GET_LAST_TRANS_UI,
    /**
     *
     */
    USE_SCANNER,
    /**
     * 电子签名特征码
     */
    SIGN_FEATURE_CODE,

    /**
     * 列表1的值
     */
    ARRAY_LIST_1,
    /**
     * 列表2的值
     */
    ARRAY_LIST_2,

    /**
     * 导航栏抬头
     */
    NAV_TITLE,
    /**
     * 导航栏是否显示返回按钮
     */
    NAV_BACK,
    /**
     *
     */
    NAV_LEVEL,
    /**
     * 寻卡模式
     */
    CARD_SEARCH_MODE,
    /**
     * 寻卡界面显示授权码
     */
    AUTH_CODE,
    /**
     * 寻卡界面刷卡提醒
     */
    SEARCH_CARD_PROMPT,
    /**
     * 界面定时器
     */
    TIKE_TIME,
    /**
     * 卡号
     */
    PANBLOCK,
    /**
     * 凭密
     */
    SUPPORTBYPASS,
    /**
     * 输密类型
     */
    ENTERPINTYPE,
    /**
     *
     */
    OPTIONS,
    RSA_PIN_KEY,
    /**
     * 输入内容自动补零
     */
    INPUT_PADDING_ZERO,
    /**
     * 交易查询界面支持交易
     */
    SUPPORT_DO_TRANS,
    /**
     * 原交易小费
     */
    ORI_TIPS,

    /**
     * has tip
     */
    HAS_TIP,

    /**
     * tip percent
     */
    TIP_PERCENT,

    /**
     * base amount
     */
    BASE_AMOUNT,

    /**
     * dcc required
     */
    DCC_REQUIRED,

    /**
     * dcc amount
     */
    DCC_AMOUNT,

    /**
     * dcc exchange rate
     */
    DCC_CONVERSION_RATE,

    /**
     * currency numeric
     */
    CURRENCY_NUMERIC,

    /**
     * card mode
     */
    CARD_MODE,

    /**
     * acquirer name
     */
    ACQUIRER_NAME,

    /**
     * issuer name
     */
    ISSUER_NAME,

    /**
     * print bitmap
     */
    BITMAP,

    ECR_PROCESS,
    ECR_NII,

    /**
     * support dual card mode:allow to use magnetic on chip card
     */
    SUPP_DUAL_CARD,

    /**
     * Flag for print report after settlement
     */
    SUPPORT_E_RECEIPT,
    BYPASS_CONFIRM_SETTLE,
    SCB_IPP_TYPE,
    SCB_TRACE_NO,
    SCB_IPP_STATE_LINK_TYPE,
    SCB_IPP_REPORT_TYPE,
    SCB_REDEEM_MODE,
    SCB_ACQ_NAME,
    LINKPOS_STR_TRANS_AMOUNT,
    LINKPOS_STR_BRANCH_ID,
    LINKPOS_BOL_AUTO_EXECUTION_MODE,
    LINKPOS_BOL_INCLUDE_CARD_PAYMENT,

    LINKPOS_STR_SEARCH_CARD_MODE,
    LINKPOS_BOL_INCLUDE_QR_PAYMENT,
    LINKPOS_STR_QR_PAYMENT_MODE,
    LINKPOS_STR_REFERENCE_SALE_ID,
    LINKPOS_BOL_QR_MANUAL_INQUIRY_ENABLED,
    LINKPOS_STR_DEFAULT_REJECT_RESPONSE_CODE,

    KCHECKID_SESSION_ID,
    KCHECKID_TRANS_AGENT_ID,
    KCHECKID_ERCM_UPLOAD_RESULT,

    KCHECKID_ERCM_ERECEIPT_DATA,
    KCHECKID_ERCM_REFERENCE_ID,

    QR_INQUIRY_COUNTER,

    VERIFY_STATE,
    TRANS_ID,

    SETTLE_ALL_MERCHANTS,
}
