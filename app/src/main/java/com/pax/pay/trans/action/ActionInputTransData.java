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
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.InputPanActivity;
import com.pax.pay.trans.action.activity.InputTransData1Activity;
import com.pax.pay.trans.action.activity.InputTransDataTipActivity;
import com.pax.pay.trans.action.activity.InputTransIDActivity;

import java.util.Map;

public class ActionInputTransData extends AAction {
    private Context context;
    private String title;
    private String prompt;
    private String mode;
    private String remark;
    private EInputType inputType;
    private int maxLen;
    private int minLen;
    private boolean useScanner;
    private boolean isGetLastTrans;
    private boolean isAuthZero;
    private Map<String, String> map;

    public ActionInputTransData(ActionStartListener listener) {
        super(listener);
    }

    public ActionInputTransData setParam(Context context, String title) {
        this.context = context;
        this.title = title;
        return this;
    }

    public ActionInputTransData setParam(Context context, String title, Map<String, String> map) {
        this.context = context;
        this.title = title;
        this.map = map;
        return this;
    }

    public ActionInputTransData setInputLine(String prompt, EInputType inputType, int maxLen, boolean isGetLastTrans) {
        return setInputLine(prompt, inputType, maxLen, 0, isGetLastTrans);
    }

    public ActionInputTransData setInputLine(String prompt, EInputType inputType, int maxLen, int minLen,
                                             boolean isGetLastTrans) {
        this.prompt = prompt;
        this.inputType = inputType;
        this.maxLen = maxLen;
        this.minLen = minLen;
        this.isGetLastTrans = isGetLastTrans;
        return this;
    }

    public ActionInputTransData setInputLine(String prompt, String remark, EInputType inputType, int maxLen, int minLen, boolean useScanner) {
        this.prompt = prompt;
        this.remark = remark;
        this.inputType = inputType;
        this.maxLen = maxLen;
        this.minLen = minLen;
        this.isGetLastTrans = false;
        this.useScanner = useScanner;
        return this;
    }

    public ActionInputTransData setInputLine1(String prompt, EInputType inputType, int maxLen, int minLen,
                                              boolean isGetLastTrans, boolean isAuthZero) {
        this.prompt = prompt;
        this.inputType = inputType;
        this.maxLen = maxLen;
        this.minLen = minLen;
        this.isGetLastTrans = isGetLastTrans;
        this.isAuthZero = isAuthZero;
        return this;
    }

    public ActionInputTransData setInputTransIDLine(String prompt, EInputType inputType, int maxLen, int minLen) {
        this.prompt = prompt;
        this.inputType = inputType;
        this.maxLen = maxLen;
        this.minLen = minLen;
        return this;
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runOnUiThread(new ProcessRunnable());
    }

    /**
     * 输入数据类型定义
     *
     * @author Steven.W
     */
    public enum EInputType {
        AMOUNT,
        NUM, // 数字
        TEXT, // 所有类型
        PHONE,
        EMAIL,
        PAN,
        TRANSID,
        TEMPLATEID,
        NUMOFSLIP,
    }

    private class ProcessRunnable implements Runnable {

        @Override
        public void run() {
            if (inputType == EInputType.PAN) {
                runPan();
            } else if (inputType == EInputType.AMOUNT) {
                runTipStyle();
            } else if (inputType == EInputType.TRANSID) {
                runTransIDStyle();
            } else {
                runStyle1();
            }
        }

        private void runPan() {
            Intent intent = new Intent(context, InputPanActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "INPUT PAN");
            bundle.putString(EUIParamKeys.PROMPT_1.toString(), prompt);
            bundle.putSerializable(EUIParamKeys.INPUT_TYPE.toString(), inputType);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

        private void runStyle1() {
            Intent intent = new Intent(context, InputTransData1Activity.class);
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putString(EUIParamKeys.PROMPT_1.toString(), prompt);
            bundle.putInt(EUIParamKeys.INPUT_MAX_LEN.toString(), maxLen);
            bundle.putInt(EUIParamKeys.INPUT_MIN_LEN.toString(), minLen);
            bundle.putSerializable(EUIParamKeys.INPUT_TYPE.toString(), inputType);
            bundle.putBoolean(EUIParamKeys.GET_LAST_TRANS_UI.toString(), isGetLastTrans);
            bundle.putBoolean(EUIParamKeys.INPUT_PADDING_ZERO.toString(), isAuthZero);
            if (remark != null)
                bundle.putString(EUIParamKeys.PROMPT_REMARK.toString(), remark);
            bundle.putBoolean(EUIParamKeys.USE_SCANNER.toString(), useScanner);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

        private void runTransIDStyle() {
            Intent intent = new Intent(context, InputTransIDActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putString(EUIParamKeys.PROMPT_1.toString(), prompt);
            bundle.putInt(EUIParamKeys.INPUT_MAX_LEN.toString(), maxLen);
            bundle.putInt(EUIParamKeys.INPUT_MIN_LEN.toString(), minLen);
            bundle.putSerializable(EUIParamKeys.INPUT_TYPE.toString(), inputType);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

        private void runTipStyle() {
            Intent intent = new Intent(context, InputTransDataTipActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
            if (map != null) {
                String totalAmount = map.get(context.getString(R.string.prompt_total_amount));
                String oriTips = map.get(context.getString(R.string.prompt_ori_tips));
                String adjustPercent = map.get(context.getString(R.string.prompt_adjust_percent));
                bundle.putString(EUIParamKeys.TRANS_AMOUNT.toString(), totalAmount);
                bundle.putString(EUIParamKeys.ORI_TIPS.toString(), oriTips);
                bundle.putFloat(EUIParamKeys.TIP_PERCENT.toString(), Float.valueOf(adjustPercent));
            }
            intent.putExtras(bundle);
            context.startActivity(intent);
        }
    }
}
