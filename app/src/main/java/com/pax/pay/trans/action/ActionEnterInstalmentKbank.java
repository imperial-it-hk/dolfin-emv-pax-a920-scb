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
import com.pax.pay.trans.action.activity.EnterInstalmentKbankActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

public class ActionEnterInstalmentKbank extends AAction {
    private Context context;
    private String title;
    private TransData transData;
    private String iPlanMode;

    public ActionEnterInstalmentKbank(ActionStartListener listener) {
        super(listener);
    }

    public static class InstalmentKbankInfo {
        private String instalmentPromotionKey;
        private String instalmeniSerialNum;
        private int intalmentTerms;
        private String instalmentAmount;
        private boolean instalmentPromoProduct;

        public InstalmentKbankInfo(String instalmentPromotionKey, String instalmeniSerialNum, int intalmentTerms, String instalmentAmount,  boolean instalmentPromoProduct){
            this.instalmentPromotionKey = instalmentPromotionKey;
            this.instalmeniSerialNum = instalmeniSerialNum;
            this.intalmentTerms = intalmentTerms;
            this.instalmentAmount = instalmentAmount;
            this.instalmentPromoProduct = instalmentPromoProduct;
        }

        public String getInstalmentPromotionKey() { return instalmentPromotionKey; }
        public void setInstalmentPromotionKey(String instalmentPromotionKey) { this.instalmentPromotionKey = instalmentPromotionKey; }
        public String getInstalmeniSerialNum() { return instalmeniSerialNum; }
        public void setInstalmeniSerialNum(String instalmeniSerialNum) { this.instalmeniSerialNum = instalmeniSerialNum; }
        public int getIntalmentTerms() { return intalmentTerms; }
        public void setIntalmentTerms(int intalmentTerms) { this.intalmentTerms = intalmentTerms; }
        public String getInstalmentAmount() { return instalmentAmount; }
        public void setInstalmentAmount(String instalmentAmount) { this.instalmentAmount = instalmentAmount; }
        public boolean isInstalmentPromoProduct() { return instalmentPromoProduct; }
        public void setInstalmentPromoProduct(boolean instalmentPromoProduct) { this.instalmentPromoProduct = instalmentPromoProduct; }
    }

    public void setParam(Context context, String title, TransData transData, String iPlanMode) {
        this.context = context;
        this.title = title;
        this.transData = transData;
        this.iPlanMode = iPlanMode;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterInstalmentKbankActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.TRANS_TYPE.toString(), transData.getTransType());
        intent.putExtra(Utils.getString(R.string.param_iplan_mode), iPlanMode);
        context.startActivity(intent);
    }
}
