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
package com.pax.pay.trans.transmit;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.BitmapFactory;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionEndListener;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.mac.EMac;
import com.pax.dal.entity.EPedType;
import com.pax.dal.exceptions.PedDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.model.TransData;
import com.pax.view.dialog.CustomAlertDialog;

public class TransProcessListenerImpl implements TransProcessListener {

    private static final String TAG = "TransProcessListener";

    private Context context;
    private CustomAlertDialog dialog;

    private IConvert convert = FinancialApplication.getConvert();
    private ConditionVariable cv;
    private boolean isShowMessage;
    private String title;
    private int result;

    public TransProcessListenerImpl(Context context) {
        this.context = context;
        this.isShowMessage = true;
    }

    public TransProcessListenerImpl(Context context, boolean isShowMessage) {
        this.context = context;
        this.isShowMessage = isShowMessage;
    }

    private void showDialog(final String message, final int timeout, final int alertType) {
        if (!isShowMessage) {
            return;
        }

        if (this.context != null) {
            if (this.context instanceof Activity) {
                Log.d("CONTEXT", "context is instance of 'Activity'");
                Activity activity = (Activity)context;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (dialog == null) {
                                dialog = new CustomAlertDialog(context, alertType);
                                dialog.show();
                                dialog.setCancelable(false);
                                if (alertType == CustomAlertDialog.WARN_TYPE) {
                                    dialog.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic16));
                                }
                            }
                            dialog.setTimeout(timeout);
                            dialog.setTitleText(title);
                            dialog.setContentText(message);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
            else {
                Log.d("CONTEXT", "context is not instance of 'Activity'");
            }
        }
//        FinancialApplication.getApp().runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                if (dialog == null) {
//                    dialog = new CustomAlertDialog(context, alertType);
//                    dialog.show();
//                    dialog.setCancelable(false);
//                    if (alertType == CustomAlertDialog.WARN_TYPE) {
//                        dialog.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic16));
//                    }
//                }
//                dialog.setTimeout(timeout);
//                dialog.setTitleText(title);
//                dialog.setContentText(message);
//            }
//        });
    }

    private void showDialog(final String message, final int timeout, final int alertType, final int msgSize) {
        if (!isShowMessage) {
            return;
        }
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (dialog == null) {
                        dialog = new CustomAlertDialog(context, alertType);
                        dialog.show();
                        dialog.setCancelable(false);
                        if (alertType == CustomAlertDialog.WARN_TYPE) {
                            dialog.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic16));
                        }
                    }
                    dialog.setTimeout(timeout);
                    dialog.setTitleText(title);
                    dialog.setContentText(message);
                    dialog.setContentTextSize(msgSize);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private int showDialog(final String message, final int timeout, final int alertType, final boolean showTitle, final boolean showConfirmCancelBtn) {
        result = TransResult.SUCC;
        if (!isShowMessage) {
            return result;
        }
        onHideProgress();
        cv = new ConditionVariable();
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (dialog == null) {
                        dialog = new CustomAlertDialog(context, alertType);
                        if (showConfirmCancelBtn) {
                            dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
                                @Override
                                public void onClick(CustomAlertDialog alertDialog) {
                                    try {
                                        dialog.dismiss();
                                    }
                                    finally {
                                        result = TransResult.ERR_USER_CANCEL;
                                        cv.open();
                                    }


                                }
                            });
                            dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                                @Override
                                public void onClick(CustomAlertDialog alertDialog) {
                                    try {
                                        dialog.dismiss();
                                    }
                                   finally {
                                        result = TransResult.SUCC;
                                        cv.open();
                                    }

                                }
                            });
                            dialog.showConfirmButton(true);
                            dialog.showCancelButton(true);
                        }
                        dialog.show();
                        if (alertType == CustomAlertDialog.WARN_TYPE) {
                            dialog.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic16));
                        }
                    }
                    if (showTitle) {
                        dialog.setTitleText(title);
                    }
                    dialog.setTimeout(timeout);
                    dialog.setContentText(message);
                    dialog.setCancelable(false);
                    dialog.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            cv.open();
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        cv.block();
        return result;
    }

    @Override
    public void onShowProgress(final String message, final int timeout) {
        showDialog(message, timeout, CustomAlertDialog.PROGRESS_TYPE);
    }

    @Override
    public int onShowProgress(String message, int timeout, boolean showTitle, boolean showConfirmCancelBtn) {
        return showDialog(message, timeout, CustomAlertDialog.PROGRESS_TYPE, showTitle, showConfirmCancelBtn);
    }

    @Override
    public void onShowWarning(String message, int timeout) {
        showDialog(message, timeout, CustomAlertDialog.WARN_TYPE);
    }

    @Override
    public void onShowWarning(String message, int timeout, int msgSize) {
        showDialog(message, timeout, CustomAlertDialog.WARN_TYPE, msgSize);
    }

    private int onShowMessage(final String message, final int timeout, final int alertType, final boolean confirmable) {
        if (!isShowMessage) {
            return 0;
        }
        onHideProgress();
        cv = new ConditionVariable();
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    CustomAlertDialog cfmDialog = new CustomAlertDialog(context, alertType, timeout);
                    cfmDialog.setContentText(message);
                    cfmDialog.show();
                    cfmDialog.showConfirmButton(confirmable);
                    cfmDialog.setOnDismissListener(new OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface arg0) {
                            cv.open();
                        }
                    });
                } catch (Exception ex) {
                   ex.printStackTrace();
                   cv.open();
                }
            }
        });

        cv.block();
        return 0;
    }

    @Override
    public int onShowNormalMessage(final String message, final int timeout, boolean confirmable) {
        return onShowMessage(message, timeout, CustomAlertDialog.NORMAL_TYPE, confirmable);
    }

    @Override
    public int onShowErrMessage(final String message, final int timeout, boolean confirmable) {
        Device.beepErr();
        return onShowMessage(message, timeout, CustomAlertDialog.ERROR_TYPE, confirmable);
    }

    @Override
    @NonNull
    public byte[] onCalcMac(byte[] data) {
        return EMac.EDC.getMac(FinancialApplication.getDal().getPed(EPedType.INTERNAL), Constants.INDEX_TAK, data);
    }

    @Override
    @NonNull
    public byte[] onEncTrack(byte[] track) {
        int len = track.length;
        String trackStr;
        if (len % 2 > 0) {
            trackStr = new String(track) + "0";
        } else {
            trackStr = new String(track);
        }

        byte[] trackData = new byte[8];

        byte[] bTrack = convert.strToBcd(trackStr, EPaddingPosition.PADDING_LEFT);
        System.arraycopy(bTrack, bTrack.length - trackData.length - 1, trackData, 0, trackData.length);
        try {
            byte[] block = Device.calcDes(bTrack);
            System.arraycopy(block, 0, bTrack, bTrack.length - block.length - 1, block.length);
        } catch (PedDevException e) {
            Log.e(TAG, "", e);
        }

        return convert.bcdToStr(bTrack).substring(0, len).getBytes();
    }

    @Override
    public void onHideProgress() {

        if (dialog != null) {
            SystemClock.sleep(200);
            try {
                dialog.dismiss();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            dialog = null;
        }
    }

    @Override
    public void onUpdateProgressTitle(String title) {
        if (!isShowMessage) {
            return;
        }
        this.title = title;
    }

    @Override
    public int onInputOnlinePin(final TransData transData) {
        cv = new ConditionVariable();
        result = 0;

        final String totalAmount = transData.getTransType().isSymbolNegative() ? "-" + transData.getAmount() : transData.getAmount();
        final String tipAmount = transData.getTransType().isSymbolNegative() ? null : transData.getTipAmount();

        ActionEnterPin actionEnterPin = new ActionEnterPin(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterPin) action).setParam(context,
                        transData.getTransType().getTransName(), transData.getPan(), true,
                        context.getString(R.string.prompt_pin),
                        context.getString(R.string.prompt_no_pin),
                        totalAmount, tipAmount,
                        ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);

            }
        });

        actionEnterPin.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult actionResult) {
                int ret = actionResult.getRet();
                if (ret == TransResult.SUCC) {
                    String data = (String) actionResult.getData();
                    transData.setPin(data);
                    if (data != null && !data.isEmpty()) {
                        transData.setHasPin(true);
                    } else {
                        transData.setHasPin(false);
                    }
                    result = 0;
                    cv.open();
                } else {
                    result = -1;
                    cv.open();
                }
                ActivityStack.getInstance().pop();
            }
        });
        actionEnterPin.execute();

        cv.block();
        return result;
    }

    @Override
    public String getTitle(){
        return this.title;
    }
}
