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

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;

/**
 * 下载参数action
 *
 * @author Steven.W
 */
public class ActionTransPreDeal extends AAction {
    private Context context;
    private ETransType transType;

    public ActionTransPreDeal(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 设置action运行时参数
     *
     * @param context
     * @param transType
     */
    public void setParam(Context context, ETransType transType) {
        this.context = context;
        this.transType = transType;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new Runnable() {

            @Override
            public void run() {
                // 执行交易预处理
                int ret = Component.transPreDeal(context, transType);
                //这里执行完 transPreDeal 比较耗时，导致界面输完金额进入到刷卡界面会卡2秒
                setResult(new ActionResult(ret, null));
            }
        });
    }

}
