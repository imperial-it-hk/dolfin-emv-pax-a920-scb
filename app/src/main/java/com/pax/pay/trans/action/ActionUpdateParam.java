/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-10-26
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.CustomAlertDialog.OnCustomClickListener;
import com.pax.view.dialog.DialogUtils;

public class ActionUpdateParam extends AAction {
    private Context context;
    private boolean disNoParam;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionUpdateParam(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, boolean disNoParam) {
        this.context = context;
        this.disNoParam = disNoParam;
    }

    @Override
    protected void process() {
        if (FinancialApplication.getDownloadManager().hasUpdateParam()) {
            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DialogUtils.showConfirmDialog(context, context.getString(R.string.update_param_now_or_not), new OnCustomClickListener() {
                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            alertDialog.dismiss();
                            setResult(new ActionResult(TransResult.SUCC, null));
                        }
                    }, new OnCustomClickListener() {
                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            alertDialog.dismiss();
                            if (FinancialApplication.getTransDataDbHelper().countOf() == 0) {
                                FinancialApplication.getDownloadManager().updateData();
                                DialogUtils.showSuccMessage(context, context.getString(R.string.update_param), new OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                                        Utils.restart();
                                    }
                                }, Constants.SUCCESS_DIALOG_SHOW_TIME);
                            } else {
                                DialogUtils.showErrMessage(context, null, context.getString(R.string.param_need_update_please_settle), new OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                                    }
                                }, Constants.FAILED_DIALOG_SHOW_TIME);
                            }
                        }
                    });
                }
            });
        } else if (disNoParam) {
            DialogUtils.showErrMessage(context, null, context.getString(R.string.no_update_param), new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                }
            }, Constants.FAILED_DIALOG_SHOW_TIME);
        } else {
            setResult(new ActionResult(TransResult.SUCC, null));
        }
    }
}
