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
import java.util.List;
import java.util.Map;
import java.util.Set;

import th.co.bkkps.utils.Log;

public class ActionDispMessage extends AAction {
    private Context context;
    private List<String> list;
    private String title;

    public ActionDispMessage(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 参数设置
     *
     * @param context ：应用上下文
     * @param title   ：抬头
     * @param list
     */
    public void setParam(Context context, String title,List<String> list) {
        this.context = context;
        this.title = title;
        this.list = list;
    }

    private ArrayList<String> leftColumns = new ArrayList<>();
    private ArrayList<String> rightColumns = new ArrayList<>();
    @Override
    protected void process() {
        Log.d("ActionDispMessage" ,"ACTION--START--");
        updateColumns(list);

        Bundle bundle = new Bundle();
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
        bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
        bundle.putStringArrayList(EUIParamKeys.ARRAY_LIST_1.toString(), leftColumns);
        bundle.putStringArrayList(EUIParamKeys.ARRAY_LIST_2.toString(), rightColumns);

        Intent intent = new Intent(context, DispTransDetailActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void updateColumns(List<String> list) {
        for (String key : list) {
            leftColumns.add(key);
            rightColumns.add("");/*
                Object value = promptValue.get(key);
                if (value != null) {
                    rightColumns.add((String) value);
                } else {
                    rightColumns.add("");
                }*/

        }
    }
}
