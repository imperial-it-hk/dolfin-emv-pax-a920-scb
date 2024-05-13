/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-29
 * Module Author: caowb
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.SelectAcqActivity;
import com.pax.pay.trans.action.activity.SelectTleAcqActivity;

public class ActionSelectTleAcquirer extends AAction {
    private Context context;
    private String title;
    private String content;

    /**
     * 子类构造方法必须调用super设置ActionStartListener
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionSelectTleAcquirer(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title,String content) {
        this.context = context;
        this.title = title;
        this.content = content; //using as flag to tell if this content is for TLE or UPI
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, SelectTleAcqActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
        bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
        bundle.putString(EUIParamKeys.CONTENT.toString(), content);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
}
