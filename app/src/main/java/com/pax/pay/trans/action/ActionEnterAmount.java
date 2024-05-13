/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-11
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.eemv.utils.Tools;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterAmountActivity;
import com.pax.pay.trans.model.TransData;

public class ActionEnterAmount extends AAction {
    private Context context;
    private String title;
    private boolean hasTip;
    private TransData origTransData;

    public ActionEnterAmount(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, boolean hasTip, TransData origTransData) {
        this.context = context;
        this.title = title;
        this.hasTip = hasTip;
        this.origTransData = origTransData;
    }

    public void setParam(Context context, String title, boolean hasTip) {
        this.context = context;
        this.title = title;
        this.hasTip = hasTip;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterAmountActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.HAS_TIP.toString(), hasTip);
        if (hasTip) {
            if (origTransData != null) {
                intent.putExtra(EUIParamKeys.TIP_PERCENT.toString(), origTransData.getIssuer().getAdjustPercent());
                intent.putExtra(EUIParamKeys.BASE_AMOUNT.toString(), origTransData.getAmount());
                if (origTransData.isDccRequired()) {
                    intent.putExtra(EUIParamKeys.DCC_REQUIRED.toString(), origTransData.isDccRequired());
                    intent.putExtra(EUIParamKeys.DCC_AMOUNT.toString(), origTransData.getDccAmount());
                    intent.putExtra(EUIParamKeys.DCC_CONVERSION_RATE.toString(), origTransData.getDccConversionRate());
                    intent.putExtra(EUIParamKeys.CURRENCY_NUMERIC.toString(), Tools.bytes2String(origTransData.getDccCurrencyCode()));
                }
            }
        }
        context.startActivity(intent);
    }

    public static class TipInformation {
        private final long newBaseAmount;
        private final long newDccBaseAmount;
        private final long tipAmount;
        private final long dccTipAmount;

        public TipInformation(long newBaseAmount, long newDccBaseAmount, long tipAmount, long dccTipAmount) {
            this.newBaseAmount = newBaseAmount;
            this.newDccBaseAmount = newDccBaseAmount;
            this.tipAmount = tipAmount;
            this.dccTipAmount = dccTipAmount;
        }

        public long getNewBaseAmount() {
            return newBaseAmount;
        }

        public long getNewDccBaseAmount() {
            return newDccBaseAmount;
        }

        public long getTipAmount() {
            return tipAmount;
        }

        public long getDccTipAmount() {
            return dccTipAmount;
        }
    }
}
