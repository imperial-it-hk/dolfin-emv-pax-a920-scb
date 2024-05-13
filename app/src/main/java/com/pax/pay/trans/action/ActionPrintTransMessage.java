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
import android.graphics.Bitmap;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintBitmap;
import com.pax.pay.trans.receipt.ReceiptPrintTrans;
import com.pax.pay.trans.receipt.ReceiptPrintTransMessage;

public class ActionPrintTransMessage extends AAction {
    private Context context;
    private TransData transData;
    private boolean isReprint;
    private String[] respMsg;
    private String[] reqMsg;
    private Bitmap bitmap = null;
    private int fontSize;
    private boolean useCustomFontSize;
    private boolean autoInitMode = false;

    public ActionPrintTransMessage(ActionStartListener listener) {
        super(listener);
    }


    public void setParam(Context context, String[] reqMsg, String[] respMsg) {
        this.context = context;
        this.reqMsg = reqMsg;
        this.respMsg = respMsg;
    }

    public void setParam(Context context, String[] reqMsg, String[] respMsg, int fontSize) {
        this.context = context;
        this.reqMsg = reqMsg;
        this.respMsg = respMsg;
        this.fontSize = fontSize;
        this.useCustomFontSize = true;
    }

    public void setParam(Context context, String[] reqMsg, String[] respMsg, int fontSize, boolean autoInitMode) {
        this.context = context;
        this.reqMsg = reqMsg;
        this.respMsg = respMsg;
        this.fontSize = fontSize;
        this.useCustomFontSize = true;
        this.autoInitMode = autoInitMode;
    }

    public void setParam(Context context, Bitmap Bitmap) {
        this.context = context;
        this.bitmap = Bitmap;
    }

    @Override
    protected void process() {
        try {
            if (TransContext.getInstance().getCurrentAction().isFinished()) {
                return;
            }
            if (bitmap == null) {
                FinancialApplication.getApp().runInBackground(new Runnable() {

                    @Override
                    public void run() {
                        PrintListenerImpl listener = new PrintListenerImpl(context);
                        if (useCustomFontSize) {
                            new ReceiptPrintTransMessage().print(reqMsg, respMsg, listener, fontSize);
                        } else {
                            new ReceiptPrintTransMessage().print(reqMsg, respMsg, listener);
                        }
                        if (!autoInitMode) {
                            if (TransContext.getInstance() != null) {
                                TransContext.getInstance().getCurrentAction().setFinished(true); //AET-229
                            }
                        }

                        setResult(new ActionResult(TransResult.SUCC, transData));
                    }
                });
            } else {
                FinancialApplication.getApp().runInBackground(new Runnable() {

                    @Override
                    public void run() {
                        PrintListenerImpl listener = new PrintListenerImpl(context);
                        new ReceiptPrintBitmap().printBitmap(bitmap, listener);
                        if (!autoInitMode) {
                            if (TransContext.getInstance() != null) {
                                TransContext.getInstance().getCurrentAction().setFinished(true); //AET-229
                            }
                        }
                        setResult(new ActionResult(TransResult.SUCC, transData));
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

}
