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

import android.os.SystemClock;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.Acquirer;
import com.pax.pay.utils.Utils;


class TcpDemoMode extends ATcp {
    @Override
    public int onConnect(Acquirer acquirer) {
        int ret = setCommParam();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        onShowMsg(Utils.getString(R.string.wait_connect));
        ret = connectDemo();
        return ret;
    }

    @Override
    public int onSend(byte[] data) {
        onShowMsg(Utils.getString(R.string.wait_send));
        SystemClock.sleep(1000);
        return TransResult.SUCC;
    }

    @Override
    public TcpResponse onRecv() {
        return new TcpResponse(TransResult.SUCC, null);
    }

    @Override
    public TcpResponse onRecv(Acquirer acquirer) {
        return new TcpResponse(TransResult.SUCC, null);
    }

    @Override
    public void onClose() {
        //do nothing
    }

    private int connectDemo() {
        SystemClock.sleep(1000);
        return TransResult.SUCC;
    }

}
