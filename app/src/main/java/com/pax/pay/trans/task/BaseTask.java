/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-7-31
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.task;

import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionEndListener;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.Log;

public abstract class BaseTask extends ATransaction {
    protected Context context;
    /**
     * transaction listener
     */
    protected TransEndListener transListener;

    private String currentState;

    public BaseTask(Context context, TransEndListener transListener) {
        super();
        this.context = context;
        this.transListener = transListener;
    }

    /**
     * transaction result prompt
     */
    protected void transEnd(final ActionResult result) {
        clear(); // no memory leak
        TransContext.getInstance().setCurrentAction(null);
        if (transListener != null) {
            transListener.onEnd(result);
        }
    }

    /**
     * transaction result prompt and deal with remove card
     *
     * @param transName
     * @param result
     * @param dismissListener
     */
    protected void dispResult(String transName, final ActionResult result, OnDismissListener dismissListener) {
        if (result.getRet() == TransResult.SUCC) {
            DialogUtils.showSuccMessage(getCurrentContext(), transName, dismissListener,
                    Constants.SUCCESS_DIALOG_SHOW_TIME);
        } else if (result.getRet() == TransResult.ERR_ABORTED
                    || result.getRet() == TransResult.ERR_HOST_REJECT) {
            // ERR_ABORTED AND ERR_HOST_REJECT  not prompt error message
            if (dismissListener != null)
                dismissListener.onDismiss(null);
        } else {
            DialogUtils.showErrMessage(getCurrentContext(), transName,
                    TransResultUtils.getMessage(result.getRet()), dismissListener,
                    Constants.FAILED_DIALOG_SHOW_TIME);
        }
    }

    protected void bind(String state, AAction action, final boolean forceEndWhenFail) {
        super.bind(state, action);
        if (action != null) {
            action.setEndListener(new ActionEndListener() {

                @Override
                public void onEnd(AAction action, final ActionResult result) {
                    FinancialApplication.getApp().runOnUiThread(new ActionEndRunnable(forceEndWhenFail, result));
                }
            });
        }
    }

    private class ActionEndRunnable implements Runnable {
        final boolean forceEndWhenFail;
        final ActionResult result;

        ActionEndRunnable(final boolean forceEndWhenFail, final ActionResult result) {
            this.forceEndWhenFail = forceEndWhenFail;
            this.result = result;
        }

        @Override
        public void run() {
           onEndRun(forceEndWhenFail, result);
        }

        private void onEndRun(final boolean forceEndWhenFail, final ActionResult result) {
            try {
                if (forceEndWhenFail && result.getRet() != TransResult.SUCC) {
                    transEnd(result);
                } else {
                    onActionResult(currentState, result);
                }
            } catch (Exception e) {
                Log.w(TAG, "", e);
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        }
    }

    @Override
    protected void bind(String state, AAction action) {
        this.bind(state, action, false);
    }

    @Override
    public void gotoState(String state) {
        this.currentState = state;
        super.gotoState(state);
    }

    @NonNull
    protected String getString(@StringRes int redId) {
        return context.getString(redId);
    }

    /**
     * deal action result
     *
     * @param currentState ：current State
     * @param result       ：current action result
     */
    public abstract void onActionResult(String currentState, ActionResult result);

    protected Context getCurrentContext() {
        return ActivityStack.getInstance().top();
    }
}
