/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-5-23
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.glwrapper.impl;

import android.content.Context;

import com.pax.gl.commhelper.IBtLeScanner;
import com.pax.gl.commhelper.IBtScanner;
import com.pax.gl.commhelper.IBtServer;
import com.pax.gl.commhelper.ICommBt;
import com.pax.gl.commhelper.ICommSslClient;
import com.pax.gl.commhelper.ICommTcpClient;
import com.pax.gl.commhelper.IHttpClient;
import com.pax.gl.commhelper.IHttpsClient;
import com.pax.gl.commhelper.ISslKeyStore;
import com.pax.gl.commhelper.ITcpServer;
import com.pax.gl.commhelper.impl.PaxGLComm;
import com.pax.glwrapper.comm.ICommHelper;

class CommHelperImp implements ICommHelper {

    private PaxGLComm comm;

    CommHelperImp(Context context) {
        comm = PaxGLComm.getInstance(context);
    }

    public PaxGLComm getComm() {
        return comm;
    }

    @Override
    public IBtScanner getBtScanner() {
        return comm.getBtScanner();
    }

    @Override
    public IBtLeScanner getBtLeScanner() {
        return comm.getBtLeScanner();
    }

    @Override
    public ICommBt createBt(String identifier) {
        return comm.createBt(identifier);
    }

    @Override
    public ICommBt createBt(String identifier, boolean useBle) {
        return comm.createBt(identifier, useBle);
    }

    @Override
    public ISslKeyStore createSslKeyStore() {
        return comm.createSslKeyStore();
    }

    @Override
    public ICommSslClient createSslClient(String host, int port, ISslKeyStore keyStore) {
        return comm.createSslClient(host, port, keyStore);
    }

    @Override
    public ICommTcpClient createTcpClient(String host, int port) {
        return comm.createTcpClient(host, port);
    }

    @Override
    public IHttpClient createHttpClient() {
        return comm.createHttpClient();
    }

    @Override
    public IHttpsClient createHttpsClient(ISslKeyStore keyStore) {
        return comm.createHttpsClient(keyStore);
    }

    @Override
    public ITcpServer createTcpServer(int port, int maxTaskNum, ITcpServer.IListener listener) {
        return comm.createTcpServer(port, maxTaskNum, listener);
    }

    @Override
    public IBtServer createBtServer(int maxTaskNum, IBtServer.IListener listener) {
        return comm.createBtServer(maxTaskNum, listener);
    }
}
