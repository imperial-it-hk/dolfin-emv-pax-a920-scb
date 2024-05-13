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
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import th.co.bkkps.utils.Log;
import android.view.KeyEvent;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.TickTimer;
import com.pax.view.dialog.InputPwdDialog;
import com.pax.view.dialog.InputPwdDialog.OnPwdListener;

public class ActionInputPassword extends AAction {
    private Context context;
    private int maxLen;
    private String title;
    private String subTitle;
    private boolean allowCanceledOnTouchOutside = true;
    private int backKeyResult = TransResult.ERR_ABORTED;
    private boolean isDismissOnly = false;

    private ProcessRunnable processRunnable = null;

    public ActionInputPassword(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, int maxLen, String title, String subTitle) {
        this.context = context;
        this.maxLen = maxLen;
        this.title = title;
        this.subTitle = subTitle;
        this.allowCanceledOnTouchOutside = true;
    }

    public void setParam(int backKeyResult) {
        this.backKeyResult = backKeyResult;
    }

    public void setParam(Context context, int maxLen, String title, String subTitle, boolean allowCanceledOnTouchOutside) {
        this.context = context;
        this.maxLen = maxLen;
        this.title = title;
        this.subTitle = subTitle;
        this.allowCanceledOnTouchOutside = allowCanceledOnTouchOutside;
    }

    public void setParam(Context context, int maxLen, String title, String subTitle, boolean allowCanceledOnTouchOutside, boolean isDismiss) {
        this.context = context;
        this.maxLen = maxLen;
        this.title = title;
        this.subTitle = subTitle;
        this.allowCanceledOnTouchOutside = allowCanceledOnTouchOutside;
        this.isDismissOnly = isDismiss;
    }

    @Override
    protected void process() {
        processRunnable = new ProcessRunnable();
        FinancialApplication.getApp().runOnUiThreadDelay(processRunnable, 100);
    }

    @Override
    public void setResult(ActionResult result) {
        if (processRunnable != null && result.getRet() == TransResult.ERR_TIMEOUT)
            processRunnable.dialog.dismiss();
        else
            super.setResult(result);
    }

    private class ProcessRunnable implements Runnable {
        InputPwdDialog dialog = null;
        protected TickTimer tickTimer;

        ProcessRunnable() {
        }

        private void setOnKeyListener() {
            dialog.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if(keyCode == KeyEvent.KEYCODE_BACK && isDismissOnly) {
                        dialog.dismiss();
                        setResult(new ActionResult(TransResult.SUCC, "DismissDialogPass"));
                    } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                        tickTimer.stop();
                        setResult(new ActionResult(backKeyResult, null));
                        dialog.dismiss();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        dialog.dismiss();
                    }
                    return false;
                }
            });
            dialog.setCancelable(false);
        }

        private void setPwdListener() {
            dialog.setPwdListener(new OnPwdListener() {
                @Override
                public void onSucc(String data) {
                    tickTimer.stop();
                    setResult(new ActionResult(TransResult.SUCC, data));
                    dialog.dismiss();
                }

                @Override
                public void onErr() {
                    tickTimer.stop();
                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                    dialog.dismiss();
                }
            });
        }

        private void setOnCancelListener() {
            //AET-50
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    tickTimer.stop();
                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                    dialog.dismiss();
                }
            });

            dialog.setCanceledOnTouchOutside(allowCanceledOnTouchOutside); // AET-17
        }



        private void onTimerFinish() {
            dialog.dismiss();
            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
        }

        @Override
        public void run() {

            tickTimer = new TickTimer(new TickTimer.OnTickTimerListener() {
                @Override
                public void onTick(long leftTime) {
                    Log.i(TAG, "onTick:" + leftTime);
                }

                @Override
                public void onFinish() {
                    onTimerFinish();
                }
            });
            tickTimer.start(30);

            dialog = new InputPwdDialog(context, maxLen, title, subTitle);
            setOnKeyListener();
            setPwdListener();
            setOnCancelListener();
            dialog.show();
        }
    }
}
