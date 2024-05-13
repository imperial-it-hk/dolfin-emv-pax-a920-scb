/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-1
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.view.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.KeyEvent;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.pay.PaymentActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.SettleTrans;
import com.pax.pay.utils.Utils;
import com.pax.settings.host.EDCFragment;
import com.pax.view.dialog.CustomAlertDialog.OnCustomClickListener;

import th.co.bkkps.utils.Log;

public class DialogUtils {

    private DialogUtils() {
        //do nothing
    }

    /**
     * 提示错误信息
     *
     * @param msg
     * @param listener
     * @param timeout
     */
    public static void showErrMessage(final Context context, final String title, final String msg,
                                      final OnDismissListener listener, final int timeout) {
        if (context == null) {
            return;
        }
        //
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Log.d("DialogUtils:", " isFinished : " + ((Activity) context).isFinishing());
                Log.d("DialogUtils:", " isDestroyed : " + ((Activity) context).isDestroyed());


                CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.ERROR_TYPE, true, timeout);
                try {
                    dialog.setTitleText(title);
                    dialog.setContentText(msg);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            return keyCode == KeyEvent.KEYCODE_BACK;
                        }
                    });
                    dialog.setOnDismissListener(listener);
                    dialog.show();
                    Device.beepErr();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static CustomAlertDialog showProcessingMessage(final Context context, final String title, final int timeout) {
        if (context == null) {
            return null;
        }
        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.PROGRESS_TYPE);

        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    dialog.showContentText(false);
                    dialog.setTitleText(title);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setTimeout(timeout);
                    dialog.show();
                    dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            return keyCode == KeyEvent.KEYCODE_BACK;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return dialog;
    }

    /**
     * 单行提示成功信息
     *
     * @param title
     * @param listener
     * @param timeout
     */
    public static void showSuccMessage(final Context context, final String title,
                                       final OnDismissListener listener, final int timeout) {
        if (context == null) {
            return;
        }
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.SUCCESS_TYPE, true, timeout);
                    dialog.showContentText(false);
                    dialog.setTitleText(context.getString(R.string.dialog_trans_succ_liff, title));
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                    dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            return keyCode == KeyEvent.KEYCODE_BACK;
                        }
                    });
                    dialog.setOnDismissListener(listener);
                    Device.beepOk();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * 退出当前应用
     */
    public static void showExitAppDialog(final Context context) {
        if (Utils.isDebugBuild()) {
            showConfirmDialog(context, context.getString(R.string.exit_app), null, new OnCustomClickListener() {
                @Override
                public void onClick(CustomAlertDialog alertDialog) {
                    alertDialog.dismiss();
                    Device.enableStatusBar(true);
                    Device.enableHomeRecentKey(true);
                    Intent intent = new Intent(context, PaymentActivity.class);
                    intent.putExtra(PaymentActivity.TAG_EXIT, true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    context.startActivity(intent);
                }
            });
        }
    }

    public static void showConfirmDialog(final Context context, final String content,
                                         final OnCustomClickListener cancelClickListener, final OnCustomClickListener confirmClickListener) {
        showConfirmDialog(context, content, cancelClickListener, confirmClickListener, CustomAlertDialog.eCustomAlertDialogButton.NEGATIVE);
    }
    public static void showConfirmDialog(final Context context, final String content,
                                         final OnCustomClickListener cancelClickListener, final OnCustomClickListener confirmClickListener, final CustomAlertDialog.eCustomAlertDialogButton targetButton) {
        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);

        final OnCustomClickListener clickListener = new OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                alertDialog.dismiss();
            }
        };

        dialog.setCancelClickListener(cancelClickListener == null ? clickListener : cancelClickListener);
        dialog.setConfirmClickListener(confirmClickListener == null ? clickListener : confirmClickListener);
        dialog.show();
        dialog.setNormalText(content);
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setFocusButton(targetButton);
    }

    /**
     * 应用更新或者参数更新提示，点击确定则进行直接结算
     */
    public static void showUpdateDialog(final Context context, final String prompt) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
        dialog.setCancelClickListener(new OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
            }
        });
        dialog.setConfirmClickListener(new OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                new SettleTrans(context, null).execute();
            }
        });
        dialog.show();
        dialog.setNormalText(prompt);
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
    }
}
