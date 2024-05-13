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
package com.pax.pay.app.quickclick;

import android.os.SystemClock;

import com.pax.pay.app.FinancialApplication;

/**
 * auto recovered value for quick click protection
 *
 * @author Steven.W
 */
class AutoRecoveredValueSetter<T> {

    private T value;
    private T recoveredTo;
    private long timeoutMs;

    protected void setValue(T value) {
        this.value = value;
    }

    protected T getValue() {
        return value;
    }

    void setRecoverTo(T value) {
        this.recoveredTo = value;
    }

    void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    void recover() {
        this.value = recoveredTo;
    }

    void autoRecover() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(timeoutMs);
                setValue(recoveredTo);
            }
        });
    }

}
