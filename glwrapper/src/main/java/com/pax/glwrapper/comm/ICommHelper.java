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
package com.pax.glwrapper.comm;

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

public interface ICommHelper {
    IBtScanner getBtScanner();

    IBtLeScanner getBtLeScanner();

    ICommBt createBt(String identifier);

    ICommBt createBt(String identifier, boolean useBle);

    ISslKeyStore createSslKeyStore();

    ICommSslClient createSslClient(String host, int port, ISslKeyStore keystore);

    ICommTcpClient createTcpClient(String host, int port);

    IHttpClient createHttpClient();

    IHttpsClient createHttpsClient(ISslKeyStore keyStore);

    ITcpServer createTcpServer(int port, int maxTaskNum, ITcpServer.IListener listener);

    IBtServer createBtServer(int maxTaskNum, IBtServer.IListener listener);
}

/* Location:           D:\Android逆向助手_v2.2\PaxGL_V1.00.04_20170303.jar
 * Qualified Name:     com.pax.gl.comm.ICommHelper
 * JD-Core Version:    0.6.0
 */