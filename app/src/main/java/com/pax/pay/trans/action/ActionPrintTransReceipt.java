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
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.receipt.ReceiptPrintScanTrans;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintInstallmentBAYTrans;
import com.pax.pay.trans.receipt.ReceiptPrintInstalmentAmexTrans;
import com.pax.pay.trans.receipt.ReceiptPrintInstalmentKbankTrans;
import com.pax.pay.trans.receipt.ReceiptPrintRedeemedTrans;
import com.pax.pay.trans.receipt.ReceiptPrintTrans;
import com.pax.pay.trans.receipt.ReceiptPrintTransForERM;

public class ActionPrintTransReceipt extends AAction {
    private Context context;
    private TransData transData;
    private boolean isReprint;
    private boolean isSupportESignature;

    public ActionPrintTransReceipt(ActionStartListener listener) {
        super(listener);
        this.isSupportESignature = false;
    }

    public ActionPrintTransReceipt(ActionStartListener listener, boolean isSupportESignature) {
        super(listener);
        this.isSupportESignature = isSupportESignature;
    }

    public void setParam(Context context, TransData transData) {
        setParam(context, transData, false);
    }

    public void setParam(Context context, TransData transData, boolean isReprint) {
        this.context = context;
        this.transData = transData;
        this.isReprint = isReprint;
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runInBackground(new Runnable() {

            @Override
            public void run() {
                PrintListenerImpl listener = new PrintListenerImpl(context);
                switch (transData.getAcquirer().getName()) {
                    case Constants.ACQ_REDEEM:
                    case Constants.ACQ_REDEEM_BDMS:
                        if (isSupportESignature) {
                            new ReceiptPrintTransForERM().print(ReceiptPrintTransForERM.PrintType.REDEEM, transData, isReprint, false, false, listener);
                        } else {
                            new ReceiptPrintRedeemedTrans().print(transData, isReprint, listener);
                        }
                        break;
                    case Constants.ACQ_SMRTPAY:
                    case Constants.ACQ_SMRTPAY_BDMS:
                    case Constants.ACQ_DOLFIN_INSTALMENT:
                        if (isSupportESignature) {
                            new ReceiptPrintTransForERM().print(ReceiptPrintTransForERM.PrintType.SMARTPAY, transData, isReprint, false, false, listener);
                        } else {
                            new ReceiptPrintInstalmentKbankTrans().print(transData, isReprint, listener);
                        }
                        break;
                    case Constants.ACQ_AMEX_EPP:
                        if (isSupportESignature) {
                            new ReceiptPrintTransForERM().print(ReceiptPrintTransForERM.PrintType.AMEX_INSTALLMENT, transData, isReprint, false, false, listener);
                        } else {
                            new ReceiptPrintInstalmentAmexTrans().print(transData, isReprint, listener);
                        }
                        break;
                    case Constants.ACQ_BAY_INSTALLMENT:
                        if (isSupportESignature) {
                            new ReceiptPrintTransForERM().print(ReceiptPrintTransForERM.PrintType.BAY_INSTALLMENT, transData, isReprint, false, false, listener);
                        } else {
                            new ReceiptPrintInstallmentBAYTrans().print(transData, isReprint, listener);
                        }
                        break;
//                    case Constants.ACQ_ALIPAY:
//                    case Constants.ACQ_WECHAT:
//                        new ReceiptPrintScanTrans().print(transData, isReprint, listener);
//                        break;
                    default:
                        if (isSupportESignature) {
                            new ReceiptPrintTransForERM().print(ReceiptPrintTransForERM.PrintType.DEFAULT, transData, isReprint, false, false, listener);
                        } else {
                            new ReceiptPrintTrans().print(transData, isReprint, listener);
                        }
                        break;
                }
                setResult(new ActionResult(TransResult.SUCC, transData));
            }
        });
    }
}
