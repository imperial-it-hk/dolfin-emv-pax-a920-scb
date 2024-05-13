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
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.DispTransDetailActivity;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class ActionDispTransDetail extends AAction {
    private Context context;
    private Map<String, String> map;
    private String title;
    private boolean isBypassConfirm = false;

    public ActionDispTransDetail(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 参数设置
     *
     * @param context ：应用上下文
     * @param title   ：抬头
     * @param map     ：确认信息
     */
    public void setParam(Context context, String title, Map<String, String> map) {
        this.context = context;
        this.title = title;
        this.map = map;
    }
    public void setParam(Context context, String title, Map<String, String> map, boolean isBypassConfirm) {
        this.context = context;
        this.title = title;
        this.map = map;
        this.isBypassConfirm = isBypassConfirm;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runOnUiThread(new ProcessRunnable(map));
    }

    private class ProcessRunnable implements Runnable {
        private ArrayList<String> leftColumns = new ArrayList<>();
        private ArrayList<String> rightColumns = new ArrayList<>();

        ProcessRunnable(Map<String, String> promptValue) {
            updateColumns(promptValue);
        }


        @Override
        public void run() {

            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
            bundle.putStringArrayList(EUIParamKeys.ARRAY_LIST_1.toString(), leftColumns);
            bundle.putStringArrayList(EUIParamKeys.ARRAY_LIST_2.toString(), rightColumns);
            bundle.putBoolean("BYPASS_CONFIRM", isBypassConfirm);

            Intent intent = new Intent(context, DispTransDetailActivity.class);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

        private void updateColumns(Map<String, String> promptValue) {
            Set<String> keys = promptValue.keySet();
            for (String key : keys) {
                leftColumns.add(key);
                Object value = promptValue.get(key);
                if (value != null) {
                    rightColumns.add((String) value);
                } else {
                    rightColumns.add("");
                }

            }
        }
    }
}
