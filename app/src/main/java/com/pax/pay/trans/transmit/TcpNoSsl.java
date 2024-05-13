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

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.gl.commhelper.exception.CommException;
import com.pax.glwrapper.comm.ICommHelper;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class TcpNoSsl extends ATcp {
    private static final int maximumReachCount = 4;

    @Override
    public int onConnect(Acquirer acquirer) {
        int ret = setCommParam();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        //Backup 4-Ips
         TransactionIPAddressCollection collection = acquirer.getAvailableIPAddressCollection();
        if (collection.getItemCount() == 0 ) {
            return TransResult.ERR_CONNECT;
        }
        int lastUsedCurrentID = FinancialApplication.getAcqManager().getAcquirerCurrentHostId(acquirer);
        if (lastUsedCurrentID != -1 && collection.isContainIndex(lastUsedCurrentID)) {
            collection.setSelectedIndex(lastUsedCurrentID);
        } else {
            collection.setSelectedIndex(0);
        }

        try {
            Log.d("BACKUP-IP", String.format("EDC is using IP settings at index = %1$s", collection.get(collection.getSelectedIndex()).getIndex()));
        } catch (Exception e) {

        }

        Log.d("BACKUP-IP", "=========================================");
        Log.d("BACKUP-IP", String.format("    %1$s IPADDRESS/PORT TABLE",acquirer.getName().toUpperCase()));
        Log.d("BACKUP-IP", "=========================================");
        Log.d("BACKUP-IP", "        IP-ADDRESS      PORT             " );
        for (int index=0; index < collection.getItemCount(); index++) {
            try {
                TransactionIPAddress ipAddressTable = collection.get(index);
                Log.d("BACKUP-IP", String.format("  [%1$s] %2$s   %3$s", ipAddressTable.getIndex(), ipAddressTable.getIpAddress(), ipAddressTable.getPort()) );
            } catch (Exception e) {

            }

        }
        Log.d("BACKUP-IP", "=========================================");

        for (int index=0; index < collection.getItemCount(); index++) {
            try {
                TransactionIPAddress txnIPAddress = collection.get();

                Log.d("BACKUP-IP", String.format("%1$s now use currentIpAddressIndex = %2$s >> Connecting to IP-%3$s:%4$d", acquirer.getName(), txnIPAddress.getIndex(), txnIPAddress.getIpAddress(), txnIPAddress.getPort()));
                //Log.d("BACKUP-IP",acquirer.getName() +  " now use currentIpAddressIndex = " +currentHostID + " >> Connecting to IP-" + txnIPAddress.getIpAddress() + " : " + txnIPAddress.getPort());
                int dialogTimeout = 10;
                String message = "";
                if (index == 0 ) {
                    //onShowMsg(Utils.getString(R.string.wait_connect),dialogTimeout);
                    message = Utils.getString(R.string.wait_connect);
                }
                else {
                    //onShowMsg(Utils.getString(R.string.wait_connect).replace(",","(" + (index) + " of " + (maximumReachCount) + ")"),dialogTimeout);
                    message = Utils.getString(R.string.wait_connect).replace(",","(" + (index) + " of " + (maximumReachCount) + ")");
                }
                onShowMsg(message, dialogTimeout);

                ret = connect(txnIPAddress.getIpAddress(), txnIPAddress.getPort(), dialogTimeout*1000,  acquirer.getRecvTimeout() * 1000);
                Log.d("BACKUP-IP","TCP-NoSSL-Connection Result : " + ret );
                onHideProgress();
                if ((ret != TransResult.ERR_CONNECT && index == maximumReachCount) || ret==TransResult.SUCC) {
                    Log.d("BACKUP-IP","Begin--Save--ActiveHostIPIndex");
                    Log.d("BACKUP-IP","\tHostName : " + acquirer.getName());
                    Log.d("BACKUP-IP","\tHostIpAddessIndex : " + txnIPAddress.getIndex());
                    acquirer.setCurrentIpAddressID(txnIPAddress.getIndex());
                    FinancialApplication.getAcqManager().updateAcquirer(acquirer); ;
                    //if (updateResult) {Log.d("BACKUP-IP","Save currentIpAddressIndex value " + targetConnectedIpAddressIndex + " for target acquirer : " + acquirer.getName());}
                    collection.next();
                    break;
                }
                else {
                    collection.next();
                }
            }
            catch (Exception ex) {
                Log.d("BACKUP-IP", ex.getMessage());

                collection.next();
            }
        }

//        for (int index=0 ; index <= maximumReachCount ; index++) {
//            currReach = index;
//            switch (currentHostID) {
//                case 0 :
//                    hostIp   = acquirer.getIp();
//                    hostPort = acquirer.getPort();
//                    break;
//                case 1 :
//                    hostIp   = acquirer.getIpBak1();
//                    hostPort = acquirer.getPortBak1();
//                    break;
//                case 2 :
//                    hostIp   = acquirer.getIpBak2();
//                    hostPort = acquirer.getPortBak2();
//                    break;
//                case 3 :
//                    hostIp   = acquirer.getIpBak3();
//                    hostPort = acquirer.getPortBak3();
//                    break;
//            }
//            Log.d("BACKUP-IP",acquirer.getName() +  " now use currentIpAddressIndex = " +currentHostID + " >> Connecting to IP-" + hostIp + " : " + hostPort);
//            int dialogTimeout = 10;
//            if (index == 0 ) {
//                onShowMsg(Utils.getString(R.string.wait_connect),dialogTimeout);
//            } else {
//                onShowMsg(Utils.getString(R.string.wait_connect).replace(",","(" + ( currReach) + " of " + (maximumReachCount) + ")"),dialogTimeout);
//            }
//            ret = connect(hostIp, hostPort, dialogTimeout * 1000);
//            Log.d("BACKUP-IP","TCP-NoSSL-Connection Result : " + ret );
//            onHideProgress();
//            if ((ret != TransResult.ERR_CONNECT && index == maximumReachCount) || ret==TransResult.SUCC) {
//                Log.d("BACKUP-IP","Begin--Save--ActiveHostIPIndex");
//                Log.d("BACKUP-IP","\tHostName : " + acquirer.getName());
//                Log.d("BACKUP-IP","\tHostIpAddessIndex : " + targetConnectedIpAddressIndex);
//                acquirer.setCurrentIpAddressID(targetConnectedIpAddressIndex);
//                FinancialApplication.getAcqManager().updateAcquirer(acquirer); ;
//                //if (updateResult) {Log.d("BACKUP-IP","Save currentIpAddressIndex value " + targetConnectedIpAddressIndex + " for target acquirer : " + acquirer.getName());}
//                break;
//            } else {
//                currentHostID = (currentHostID==3) ? 0 : currentHostID +1;
//                //FinancialApplication.getAcqManager().getCurAcq().setCurrentBackupAddressId(currentHostID);
//                currentHostID=acquirer.getCurrentBackupAddressIdWithIncreaseValue();
//            }
//        }
        return ret;
    }

    @Override
    public int onSend(byte[] data) {
        try {
            onShowMsg(Utils.getString(R.string.wait_send));
            client.send(data);
            return TransResult.SUCC;
        } catch (CommException e) {
            Log.e(TAG, "", e);
        }
        return TransResult.ERR_SEND;
    }

    @Override
    public TcpResponse onRecv() {
        try {
            onShowMsg(Utils.getString(R.string.wait_recv));
            byte[] lenBuf = client.recv(2);
            if (lenBuf == null || lenBuf.length != 2) {
                return new TcpResponse(TransResult.ERR_RECV, null);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = (((lenBuf[0] << 8) & 0xff00) | (lenBuf[1] & 0xff));
            byte[] rsp = client.recv(len);
            if (rsp == null || rsp.length != len) {
                return new TcpResponse(TransResult.ERR_RECV, null);
            }
            baos.write(rsp);
            rsp = baos.toByteArray();
            return new TcpResponse(TransResult.SUCC, rsp);
        } catch (IOException | CommException e) {
            Log.e(TAG, "", e);
        }

        return new TcpResponse(TransResult.ERR_RECV, null);
    }

    @Override
    public TcpResponse onRecv(Acquirer acquirer) {
        try {
            onShowMsg(Utils.getString(R.string.wait_recv), acquirer.getRecvTimeout());
            byte[] lenBuf = client.recv(2);
            if (lenBuf == null || lenBuf.length != 2) {
                return new TcpResponse(TransResult.ERR_RECV, null);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = (((lenBuf[0] << 8) & 0xff00) | (lenBuf[1] & 0xff));
            byte[] rsp = client.recv(len);
            if (rsp == null || rsp.length != len) {
                return new TcpResponse(TransResult.ERR_RECV, null);
            }
            baos.write(rsp);
            rsp = baos.toByteArray();
            return new TcpResponse(TransResult.SUCC, rsp);
        } catch (IOException | CommException e) {
            Log.e(TAG, "", e);
        }

        return new TcpResponse(TransResult.ERR_RECV, null);
    }

    @Override
    public void onClose() {
        try {
            client.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    private int connect(String hostIp, int port, int timeout) {
        if (hostIp == null || !Utils.checkIp(hostIp)) {
            return TransResult.ERR_CONNECT;
        }

        ICommHelper commHelper = FinancialApplication.getGl().getCommHelper();
        connectSub(commHelper, hostIp, port);
        client.setConnectTimeout(timeout);
        client.setRecvTimeout(timeout);
        try {
            client.connect();
            return TransResult.SUCC;
        } catch (CommException e) {
            Log.e(TAG, "", e);
        }
        return TransResult.ERR_CONNECT;
    }

    private int connect(String hostIp, int port, int timeout, int recvTimeout) {
        if (hostIp == null || !Utils.checkIp(hostIp)) {
            return TransResult.ERR_CONNECT;
        }

        ICommHelper commHelper = FinancialApplication.getGl().getCommHelper();
        connectSub(commHelper, hostIp, port);
        client.setConnectTimeout(timeout);
        client.setRecvTimeout(recvTimeout);
        try {
            client.connect();
            return TransResult.SUCC;
        } catch (CommException e) {
            Log.e(TAG, "", e);
        }
        return TransResult.ERR_CONNECT;
    }

    protected void connectSub(ICommHelper commHelper, String hostIp, int port) {
        client = commHelper.createTcpClient(hostIp, port);
    }

}
