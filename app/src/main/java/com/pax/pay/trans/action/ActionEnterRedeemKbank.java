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
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterAmountActivity;
import com.pax.pay.trans.action.activity.EnterRedeemKbankActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

public class ActionEnterRedeemKbank extends AAction {
    private Context context;
    private String title;
    private TransData transData;

    public ActionEnterRedeemKbank(ActionStartListener listener) {
        super(listener);
    }

    public static class RedeemKbankInfo {
        private String redeemProductCd;
        private int redeemQuantity;
        private int redeemPoints;
        private String redeemAmount;
        private String discountTypeVal;

        public RedeemKbankInfo(String redeemProductCd, int redeemQuantity, int redeemPoints, String redeemAmount, String discountTypeVal){
            this.redeemProductCd = redeemProductCd;
            this.redeemQuantity = redeemQuantity;
            this.redeemPoints = redeemPoints;
            this.redeemAmount = redeemAmount;
            this.discountTypeVal = discountTypeVal;
        }

        public String getRedeemProductCd() {return redeemProductCd;}
        public void setRedeemProductCd(String redeemProductCd) {this.redeemProductCd = redeemProductCd;}
        public int getRedeemQuantity() {return redeemQuantity;}
        public void setRedeemQuantity(int redeemQuantity) {this.redeemQuantity = redeemQuantity;}
        public int getRedeemPoints() {return redeemPoints;}
        public void setRedeemPoints(int redeemPoints) {this.redeemPoints = redeemPoints;}
        public String getRedeemAmount() {return redeemAmount;}
        public void setRedeemAmount(String redeemAmount) {this.redeemAmount = redeemAmount;}
        public String getDiscountTypeVal() {
            return discountTypeVal;
        }
    }

    public void setParam(Context context, String title, TransData transData) {
        this.context = context;
        this.title = title;
        this.transData = transData;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterRedeemKbankActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(Utils.getString(R.string.param_card_type), transData.getIssuer().getName());
        intent.putExtra(Utils.getString(R.string.param_card_num), transData.getPan());
        intent.putExtra(Utils.getString(R.string.param_card_exp), transData.getExpDate());
        intent.putExtra(EUIParamKeys.TRANS_TYPE.toString(), transData.getTransType());
        context.startActivity(intent);
    }
}
