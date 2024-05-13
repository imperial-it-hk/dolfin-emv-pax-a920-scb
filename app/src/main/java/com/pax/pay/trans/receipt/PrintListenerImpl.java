/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-26
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.receipt;

import android.content.Context;
import android.content.DialogInterface;
import android.os.ConditionVariable;
import android.view.KeyEvent;

import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.CustomAlertDialog.OnCustomClickListener;

public class PrintListenerImpl implements PrintListener {

    private Context context;
    private CustomAlertDialog showMsgDialog;
    private CustomAlertDialog confirmDialog;
    private ConditionVariable cv;
    private Status result = Status.OK;

    public PrintListenerImpl(Context context) {
        this.context = context;
    }

    @Override
    public void onShowMessage(final String title, final String message) {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (showMsgDialog == null) {
                    try {
                        showMsgDialog = new CustomAlertDialog(context, CustomAlertDialog.PROGRESS_TYPE);
                        showMsgDialog.show();
                        showMsgDialog.setCancelable(false);
                        showMsgDialog.setCanceledOnTouchOutside(false);
                        showMsgDialog.setTitleText(title);
                        showMsgDialog.setContentText(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    showMsgDialog.setTitleText(title);
                    showMsgDialog.setContentText(message);
                }
            }
        });
    }

    @Override
    public Status onConfirm(final String title, final String message,final int timeout) {
        cv = new ConditionVariable();
        result = Status.OK;
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (confirmDialog != null) {
                        confirmDialog.dismiss();
                    }
                    if (context == null && ActivityStack.getInstance().top()!=null) {
                        context = ActivityStack.getInstance().top();
                    }
                    confirmDialog = new CustomAlertDialog(context, CustomAlertDialog.ERROR_TYPE);
                    confirmDialog.show();
                    confirmDialog.setTimeout(timeout);
                    confirmDialog.setTitleText(title);
                    confirmDialog.setContentText(message);
                    confirmDialog.setCancelable(false);
                    confirmDialog.setCanceledOnTouchOutside(false);
                    confirmDialog.showCancelButton(true);
                    confirmDialog.setCancelClickListener(new OnCustomClickListener() {

                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            result = Status.CANCEL;
                            alertDialog.dismiss();
                        }
                    });
                    confirmDialog.showConfirmButton(true);
                    confirmDialog.setConfirmClickListener(new OnCustomClickListener() {

                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            result = Status.CONTINUE;
                            alertDialog.dismiss();
                        }
                    });

                    confirmDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK){
                                result = Status.CANCEL;
                                dialog.dismiss();
                            } else if (keyCode == KeyEvent.KEYCODE_ENTER){
                                result = Status.CONTINUE;
                                dialog.dismiss();
                            }
                            return false;
                        }
                    });

                    confirmDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (result == Status.OK) {
                                result = Status.CANCEL;
                            }
                            if (cv != null) {
                                cv.open();
                            }
                        }
                    });
                    try {
                        confirmDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    cv.open();
                }
            }
        });
        cv.block();
        return result;
    }

    @Override
    public Status onPrintNext(final String title, final String message) {
        cv = new ConditionVariable();
        result = Status.OK;
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (confirmDialog != null) {
                    confirmDialog.dismiss();
                }
                confirmDialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
                confirmDialog.show();
                confirmDialog.setTimeout(5);
                confirmDialog.setTitleText(title);
                confirmDialog.setContentText(message);
                confirmDialog.setCancelable(true);
                confirmDialog.setCanceledOnTouchOutside(false);
                confirmDialog.showCancelButton(true);
                confirmDialog.setCancelClickListener(new OnCustomClickListener() {

                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        result = Status.CANCEL;
                        alertDialog.dismiss();
                    }
                });
                confirmDialog.showConfirmButton(true);
                confirmDialog.setConfirmClickListener(new OnCustomClickListener() {

                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        result = Status.CONTINUE;
                        alertDialog.dismiss();
                    }
                });

                confirmDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK){
                            result = Status.CANCEL;
                            dialog.dismiss();
                        } else if (keyCode == KeyEvent.KEYCODE_ENTER){
                            result = Status.CONTINUE;
                            dialog.dismiss();
                        }
                        return false;
                    }
                });


                confirmDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (cv != null) {
                            cv.open();
                        }
                    }
                });

                confirmDialog.show();

            }
        });
        cv.block();
        return result;
    }

    @Override
    public void onEnd() {
        if (showMsgDialog != null) {
            showMsgDialog.dismiss();
        }
        if (confirmDialog != null) {
            confirmDialog.dismiss();
        }
    }

}
