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
package com.pax.pay;

import android.os.Bundle;

import th.co.bkkps.utils.Log;

import android.view.MenuItem;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.trans.TransContext;
import com.pax.pay.utils.TickTimer;

public abstract class BaseActivityWithTickForAction extends BaseActivity {
    protected TickTimer tickTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tickTimer = new TickTimer(new TickTimer.OnTickTimerListener() {
            @Override
            public void onTick(long leftTime) {
                //Log.i(TAG, "onTick:" + leftTime);
                onTimerTick(leftTime);
            }

            @Override
            public void onFinish() {
                onTimerFinish();
            }
        });
        tickTimer.start();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        tickTimer.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        tickTimer.start();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickTimer.stop();
    }

    protected void onTimerTick(long timeleft) {

    }

    public void finish(ActionResult result) {
        tickTimer.stop();
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        } else {
            ActivityStack.getInstance().popTo(MainActivity.class);
            finish();
        }
    }

    public void finish(ActionResult result, boolean needFinish) {
        tickTimer.stop();
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished()) {
                return;
            }
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
            if (needFinish){
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_ABORTED, null));
        return true;
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    protected void onTimerFinish() {
        finish(new ActionResult(TransResult.ERR_TIMEOUT, null));
    }
}
