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

import android.os.Build;

import com.pax.dal.IDalCommManager;
import com.pax.dal.entity.ERoute;
import com.pax.dal.entity.LanParam;
import com.pax.dal.entity.MobileParam;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.gl.commhelper.IComm;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.RoutingTable;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

public abstract class ATcp {

    protected static final String TAG = "A TCP";

    protected IComm client;
    protected String hostIp;
    protected int hostPort;

    protected TransProcessListener transProcessListener;

    public abstract int onConnect(Acquirer acquirer);

    /**
     * 发送数据
     *
     * @param data
     * @return
     */
    public abstract int onSend(byte[] data);

    /**
     * 接收数据
     *
     * @return
     */
    public abstract TcpResponse onRecv();

    /**
     * Receive data
     *
     * @param acquirer The specific acquirer
     * @return
     */
    public abstract TcpResponse onRecv(Acquirer acquirer);

    /**
     * 关闭连接
     */
    public abstract void onClose();

    /**
     * 设置监听器
     *
     * @param listener
     */
    protected void setTransProcessListener(TransProcessListener listener) {
        this.transProcessListener = listener;
    }

    protected void onShowMsg(String msg) {
        if (transProcessListener != null) {
            transProcessListener.onShowProgress(msg, FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT));
        }
    }
    protected  void onHideProgress() {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }
    }

    protected void onShowMsg(String msg, int timeoutSec) {
        if (transProcessListener != null) {
            if(timeoutSec == 0)
                timeoutSec = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT);
            transProcessListener.onShowProgress(msg, timeoutSec);
        }
    }

    /**
     * 参数设置和路由选择
     *
     * @return
     */
    protected int setCommParam() {
        IDalCommManager commManager = FinancialApplication.getDal().getCommManager();
        SysParam sysParam = FinancialApplication.getSysParam();
        String commTypeStr = sysParam.get(SysParam.StringParam.COMM_TYPE);
        SysParam.Constant.CommType commType = SysParam.Constant.CommType.valueOf(commTypeStr);
        switch (commType) {
            case LAN:
                commManager.setLanParam(loadLanParam(sysParam));
                break;
            case MOBILE:
                // mobile参数设置
                commManager.setMobileParam(loadMobileParam(sysParam));
                break;
            case DEMO:
                onShowMsg(Utils.getString(R.string.wait_demo_mode), 5);
                return TransResult.SUCC;
            case WIFI:
                break;
            default:
                return TransResult.ERR_CONNECT;

        }

        onShowMsg(Utils.getString(R.string.wait_initialize_net), Constants.FAILED_DIALOG_SHOW_TIME);
        return TransResult.SUCC;
    }

    private MobileParam loadMobileParam(SysParam sysParam) {
        MobileParam param = new MobileParam();
        param.setApn(sysParam.get(SysParam.StringParam.MOBILE_APN));
        param.setPassword(sysParam.get(SysParam.StringParam.MOBILE_PWD));
        param.setUsername(sysParam.get(SysParam.StringParam.MOBILE_USER));
        return param;
    }

    private LanParam loadLanParam(SysParam sysParam) {
        LanParam lanParam = new LanParam();
        lanParam.setDhcp(sysParam.get(SysParam.BooleanParam.LAN_DHCP));
        lanParam.setDns1(sysParam.get(SysParam.StringParam.LAN_DNS1));
        lanParam.setDns2(sysParam.get(SysParam.StringParam.LAN_DNS2));
        lanParam.setGateway(sysParam.get(SysParam.StringParam.LAN_GATEWAY));
        lanParam.setLocalIp(sysParam.get(SysParam.StringParam.LAN_LOCAL_IP));
        lanParam.setSubnetMask(sysParam.get(SysParam.StringParam.LAN_NETMASK));
        return lanParam;
    }

    /**
     * 获取主机地址
     *
     * @return
     */
    protected String getMainHostIp() {
        return FinancialApplication.getAcqManager().getCurAcq().getIp();
    }

    /**
     * 获取主机端口
     *
     * @return
     */
    protected int getMainHostPort() {
        return FinancialApplication.getAcqManager().getCurAcq().getPort();
    }

    /**
     * 获取备份主机地址
     *
     * @return
     */
    protected String getBackHostIp() {
        return FinancialApplication.getAcqManager().getCurAcq().getIpBak1();
    }

    /**
     * 获取备份主机端口
     *
     * @return
     */
    protected int getBackHostPort() {
        return FinancialApplication.getAcqManager().getCurAcq().getPortBak1();
    }

    /**
     * 获取备份主机地址
     *
     * @return
     */
    protected String getBackHostIp2() {
        return FinancialApplication.getAcqManager().getCurAcq().getIpBak2();
    }

    /**
     * 获取备份主机端口
     *
     * @return
     */
    protected int getBackHostPort2() {
        return FinancialApplication.getAcqManager().getCurAcq().getPortBak2();
    }

    /**
     * 获取备份主机地址
     *
     * @return
     */
    protected String getBackHostIp3() {
        return FinancialApplication.getAcqManager().getCurAcq().getIpBak3();
    }

    /**
     * 获取备份主机端口
     *
     * @return
     */
    protected int getBackHostPort3() {
        return FinancialApplication.getAcqManager().getCurAcq().getPortBak3();
    }
}
