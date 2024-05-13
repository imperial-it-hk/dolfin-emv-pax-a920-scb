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

import th.co.bkkps.utils.Log;

import com.pax.gl.commhelper.ISslKeyStore;
import com.pax.glwrapper.comm.ICommHelper;

import java.io.IOException;
import java.io.InputStream;

class TcpSsl extends TcpNoSsl {

    private InputStream keyStoreStream;

    TcpSsl(InputStream keyStoreStream) {
        this.keyStoreStream = keyStoreStream;
    }

    @Override
    protected void connectSub(ICommHelper commHelper, String hostIp, int port) {
        ISslKeyStore keyStore = commHelper.createSslKeyStore();
        if (keyStoreStream != null) {
            try {
                keyStoreStream.reset();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
        keyStore.setTrustStore(keyStoreStream);
        client = commHelper.createSslClient(hostIp, port, keyStore);
    }
}
