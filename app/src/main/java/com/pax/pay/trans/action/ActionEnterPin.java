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

import com.pax.abl.core.AAction;
import com.pax.abl.utils.PanUtils;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterPinActivity;
import com.pax.pay.trans.action.activity.EnterPinTestActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

public class ActionEnterPin extends AAction {
    private Context context;
    private String title;
    private String pan;
    private String header;
    private String subHeader;
    private String totalAmount;
    private String tipAmount;
    private boolean isSupportBypass;
    private EEnterPinType enterPinType;

    public ActionEnterPin(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 脱机pin时返回的结果
     *
     * @author Steven.W
     */
    public static class OfflinePinResult {
        // SW1 SW2
        byte[] respOut;
        int ret;

        public byte[] getRespOut() {
            return respOut;
        }

        public void setRespOut(byte[] respOut) {
            this.respOut = respOut;
        }

        public int getRet() {
            return ret;
        }

        public void setRet(int ret) {
            this.ret = ret;
        }
    }

    public void setParam(Context context, String title, String pan, boolean supportBypass, String header,
                         String subHeader, String totalAmount, String tipAmount, EEnterPinType enterPinType, TransData transData) {
        this.context = context;
        this.title = title;
        this.pan = pan;
        this.isSupportBypass = supportBypass;
        this.header = header;
        this.subHeader = subHeader;
        this.totalAmount = totalAmount;
        this.tipAmount = tipAmount; //AET-81
        this.enterPinType = enterPinType;
        Component.setTransDataInstance(transData);
    }

    public enum EEnterPinType {
        ONLINE_PIN, // 联机pin
        OFFLINE_PLAIN_PIN, // 脱机明文pin
        OFFLINE_CIPHER_PIN, // 脱机密文pin
        OFFLINE_PCI_MODE, //JEMV PCI MODE, no callback for offline pin
    }

    @Override
    protected void process() {
  //      Intent intent = new Intent(context,
  //              Utils.isAutoTestBuild() ? EnterPinTestActivity.class : EnterPinActivity.class);
        Intent intent = new Intent(context, EnterPinActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.PROMPT_1.toString(), header);
        intent.putExtra(EUIParamKeys.PROMPT_2.toString(), subHeader);
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), totalAmount);
        intent.putExtra(EUIParamKeys.TIP_AMOUNT.toString(), tipAmount); //AET-81
        intent.putExtra(EUIParamKeys.ENTERPINTYPE.toString(), enterPinType);
        intent.putExtra(EUIParamKeys.PANBLOCK.toString(), PanUtils.getPanBlock(pan, PanUtils.X9_8_WITH_PAN));
        intent.putExtra(EUIParamKeys.SUPPORTBYPASS.toString(), isSupportBypass);
        context.startActivity(intent);
    }

}
