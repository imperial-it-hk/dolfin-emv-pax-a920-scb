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

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.IPacker;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import th.co.bkkps.utils.Log;

public class Online {

    private static final String TAG = "Online";
    protected IPacker<TransData, byte[]> dupPackager;
    private HttpsURLConnection httpsConn = null;
    private TransProcessListener listener;
    private ATcp tcp;
    private IPacker<TransData, byte[]> packager;
    private byte[] sendData;
    private byte[] ExtSendData;
    private boolean useExternalField63 = false;

    public int online(TransData transData, final TransProcessListener listener) {
        return this.online(transData, listener, true, true);
    }

    public int online(TransData transData, final TransProcessListener listener, final boolean reconnect, final boolean forceClose) {
        try {
            this.listener = listener;
            onShowMsg(Utils.getString(R.string.wait_process));

            if (transData.getAcquirer() == null || !transData.getAcquirer().isEnable()) {
                return TransResult.ERR_HOST_NOT_FOUND;
            }

            ETransType transType = transData.getTransType();


            // 准备打包器
            packager = transType.getPackager(new PackListenerImpl(listener, transData.getAcquirer()));
            dupPackager = transType.getDupPackager(new PackListenerImpl(listener, transData.getAcquirer()));

            if (Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) && Component.isDemo()) {
                //For AMEX, test certification only DEMO communication type mode.
                return this.commDemoAmex(transData, transType);
            } else {
                int ret = doConnect(transData.getAcquirer(), reconnect);
                if (ret != TransResult.SUCC)
                    return ret;

                ret = doPack(transData);
                if (ret != TransResult.SUCC) {
                    return ret;
                }

                // 联机交易标识
                transData.setOnlineTrans(true);

                // VERIFONE-ERM
                // Replace existing [senData] with External ISO8583 Format
                //Log.i(EReceiptUtils.TAG, "before replace DE63 SendData Len=" + sendData.length +" bytes.");
                //if(useExternalField63 == true){ sendData = EReceiptUtils.getInstance().ReplaceBit63(sendData, EReceiptUtils.getInstance().getSize(transData.getERCMLogoImagePath()) );}
                //Log.i(EReceiptUtils.TAG, "after replace DE63 SendData Len=" + sendData.length +" bytes.");

                // 发送数据
                Log.i(TAG, "SEND:" + transType.toString());
                Log.i(TAG, "SEND:" + Utils.bcd2Str(sendData).substring(4));
                ret = tcp.onSend(sendData);
                if (ret != 0) {
                    return TransResult.ERR_SEND;
                }

                // 冲正处理
                initReversalStatus(transData);
                if (transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL) {
                    ERMRequiredUpload(transData);
                }

                // EDCBBLAND-426 [Minor] Implement small amount feature.
                Component.chkTxnIsSmallAmt(transData);

                // 接收数据
                TcpResponse tcpResponse = tcp.onRecv(transData.getAcquirer());//Modified by Cz, pass acquirer param to specific receive timeout.
                Log.i(TAG, "RECV:" + ((tcpResponse.getData() != null) ? Utils.bcd2Str(tcpResponse.getData()) : ""));

                if (tcpResponse.getRetCode() != TransResult.SUCC) {
                    // 更新冲正原因
                    increaseStanNo(transData);
                    if (dupPackager != null && transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL &&
                            transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE &&
                            transData.getTransType() != ETransType.PROMPT_ADV) {
                        transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                        transData.setDupReason(TransData.DUP_REASON_NO_RECV);
                        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    }
                    return TransResult.ERR_RECV;
                }

                // Kui move to apply with real Trace/Stan
                increaseStanNo(transData);

                return doUnpack(transData, tcpResponse);
            }
        } finally {
            if (forceClose) {
                close();
            }
        }
    }

    private int doConnect(Acquirer acquirer, boolean reconnect) {
        if (reconnect) {
            tcp = getTcpClient(acquirer);
            if (tcp == null)
                return TransResult.ERR_CONNECT;
        }
        tcp.setTransProcessListener(listener);
        if (reconnect) {
            // 连接
            int ret = tcp.onConnect(acquirer);
            if (ret != 0) {
                return TransResult.ERR_CONNECT;
            }
        }
        return TransResult.SUCC;
    }

    public byte[] getForceUseExternalSendData() {
        return ExtSendData;
    }

    public void setForceUseExternalSendData(byte[] exFlag) {
        ExtSendData = exFlag;
    }

    public boolean getReplaceExternalField63() {
        return useExternalField63;
    }

    public void setReplaceExternalField63(boolean exFlag) {
        useExternalField63 = exFlag;
    }

    private int doPack(TransData transData) {
        // 打包
        byte[] req;
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            req = dupPackager.pack(transData);
        } else {
            req = packager.pack(transData);
        }

        if (req.length == 0) {
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

            ETransType transType = transData.getTransType();
            // Check Load Tle
            if (acq.isEnableTle() && isTransSupportTle(transData.getTransType())) {
                if (acq.getTWK() == null)
                    return TransResult.ERR_TLE_NOT_LOAD;
            }

            // Check Load Upi
            /*if (acq.isEnableUpi() && !acq.isTestMode() &&
                    transData.getTransType() == ETransType.LOAD_UPI_TWK)
            {
                if (acq.getTWK()==null)
                    return TransResult.ERR_UPI_NOT_LOGON;
            }*/

        }

        if (req.length == 0) {
            return TransResult.ERR_PACK;
        }
        sendData = new byte[2 + req.length];
        sendData[0] = (byte) (req.length / 256);
        sendData[1] = (byte) (req.length % 256);
        System.arraycopy(req, 0, sendData, 2, req.length);
        return TransResult.SUCC;
    }

    private int doUnpack(TransData transData, TcpResponse tcpResponse) {
        if (TcpDemoMode.class.equals(tcp.getClass())) {
            createDummyRecvData(transData);
            return TransResult.SUCC;
        }

        //Log.i(TAG, "RECV:" + Utils.bcd2Str(tcpResponse.getData()));
        if (dupPackager != null && transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            return dupPackager.unpack(transData, tcpResponse.getData());
        }
        int ret = packager.unpack(transData, tcpResponse.getData());
        // 更新冲正原因
        if (ret == TransResult.ERR_MAC && dupPackager != null &&
                transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL &&
                transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE &&
                transData.getTransType() != ETransType.PROMPT_ADV) {
            transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            transData.setDupReason(TransData.DUP_REASON_MACWRONG);
            transData.setDateTime(transData.getDateTime());
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        // 如果39域返回null,删除冲正文件, 或者解包3， 4， 11， 41，42域与请求不同时，删除冲正(BCTC要求下笔交易不发冲正)
        if (isErrPackage(ret) || isErrField(ret)) {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
        }
        if (transData.getResponseCode() != null) {
            ETransType transType = transData.getTransType();
            if ("00".equals(transData.getResponseCode().getCode()) ||
                    ("95".equals(transData.getResponseCode().getCode()) && transType == ETransType.SETTLE_END)) {
                if (!checkApprCodeBay(transData)) {
                    return TransResult.ERR_INVALID_APPR_CODE;
                }
                if (transType != ETransType.BPS_QR_SALE_INQUIRY
                        && transType != ETransType.PROMPT_ADV) {//Modified by Cz to check transType for PromptPay before increasing traceNo.
                    // Kui move to apply with real Trace
                    increaseTraceNo(transData);
                }
            }
        }
        return ret;
    }

    private boolean checkApprCodeBay(TransData transData) {
        if (Constants.ACQ_BAY_INSTALLMENT.compareTo(transData.getAcquirer().getName()) == 0 &&
                (transData.getTransType() == ETransType.BAY_INSTALMENT || transData.getTransType() == ETransType.VOID) &&
                (transData.getAuthCode() == null || transData.getAuthCode().trim().length() == 0)) {
            transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            transData.setDupReason(TransData.DUP_REASON_OTHERS);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            return false;
        }
        return true;
    }

    private void increaseStanNo(TransData transData) {
        //AET-32、AET31
        // AET-126只要发送成功保存了交易就要增加流水号，避免流水号复用
        // 冲正交易不需要增加流水号
        // trans by GG translate
        // AET-126 only needs to increase the serial number as long as the transaction is successfully saved, avoiding the reuse of serial numbers.
        // No need to increase serial number to correct transactions

       /* original
       ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL &&
                transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE &&
                (transType != ETransType.OFFLINE_TRANS_SEND) &&
                (transType != ETransType.SETTLE) &&
                (transType != ETransType.BATCH_UP)) {
            Component.incTransNo();
        }*/

        Component.incStanNo(transData);
    }

    private void increaseTraceNo(TransData transData) {
        Component.incTraceNo(transData);

    }

    private void updateStanNo(TransData transData) {
        //update current STAN after it was increased after reversal
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

    }

    private void ERMRequiredUpload(TransData transData) {
        if (Component.isAllowSignatureUpload(transData)) {
            transData.seteReceiptUploadStatus(TransData.UploadStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
    }

    protected void initReversalStatus(TransData transData) {
        if (dupPackager != null
                && transData.getReversalStatus() == TransData.ReversalStatus.NORMAL
                && transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE
                && transData.getReferralStatus() != TransData.ReferralStatus.REFERRED
                && transData.getTransType() != ETransType.PROMPT_ADV) {
            ETransType transType = transData.getTransType();
            ETransType origTransType = transData.getOrigTransType();
            boolean isVoidTrans = ETransType.VOID == transType;
            boolean isOfflineNormalSale = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == origTransType
                    || (ETransType.SALE == transType && transData.getOfflineSendState() != null)
                    || (ETransType.SALE == origTransType && transData.getOfflineSendState() != null);
            // 保存冲正
            if (!isOfflineNormalSale || (isVoidTrans && transData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT)) {
                transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            }

            if (transData.getOfflineSendState() != null && !isVoidTrans)
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            else
                FinancialApplication.getTransDataDbHelper().insertTransData(transData);
        }
    }

    private boolean isErrPackage(int ret) {
        return ret == TransResult.ERR_PACKET;
    }

//    private void chkTxnIsSmallAmt(TransData transData) {
//        // check acquirer & issuer is enabled small amount flag or not.
//        Issuer issuer = transData.getIssuer();
//        if (transData.getTransType() == ETransType.SALE && transData.getAcquirer()!=null && transData.getAcquirer().isEnableSmallAmt()
//                && issuer!=null && issuer.isEnableSmallAmt()) {
//            // If enabled, need to keep flag for each transaction to support reprint slip process.
//            transData.setTxnSmallAmt(Utils.parseLongSafe(transData.getAmount(), 0) <= issuer.getSmallAmount());
//            transData.setNumSlipSmallAmt(issuer.getNumberOfReceipt());
//        }
//    }

    private boolean isErrField(int ret) {
        return ret == TransResult.ERR_PROC_CODE || ret == TransResult.ERR_TRANS_AMT
                || ret == TransResult.ERR_STAN_NO || isErrId(ret);
    }

    private boolean isErrId(int ret) {
        return ret == TransResult.ERR_TERM_ID || ret == TransResult.ERR_MERCH_ID;
    }

    public void close() {
        if (tcp != null) {
            tcp.onClose();
        }
    }

    private void onShowMsg(String msg) {
        if (listener != null) {
            listener.onShowProgress(msg, FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT));
        }
    }

    private ATcp getTcpClient(Acquirer acquirer) {
        if (Component.isDemo()) {
            return new TcpDemoMode();
        } else if (SysParam.Constant.CommSslType.SSL == acquirer.getSslType()) {
            InputStream inputStream = null;
            try {
                File file = new File(Constants.CACERT_PATH);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                }
            } catch (IOException e) {
                Log.e("Online", "", e);
                return null;
            }
            return new TcpSsl(inputStream);
        } else {
            return new TcpNoSsl();
        }
    }

    private void createDummyRecvData(TransData transData) {
        if (!ETransType.VOID.equals(transData.getTransType())) {
            transData.setAuthCode(transData.getDateTime().substring(8));
        } else {
            transData.setAuthCode("      ");
        }

        transData.setRefNo(transData.getDateTime().substring(2));
        transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
        if (ETransType.SETTLE.equals(transData.getTransType())) {
            transData.setResponseCode(FinancialApplication.getRspCode().parse("95"));
        }
        // 测试交易上送， 模拟平台下发脚本
        String rspF55 = "72289F1804AABBCCDD86098424000004AABBCCDD86098418000004AABBCCDD86098416000004AABBCCDD";
        transData.setRecvIccData(rspF55);
    }

    private int commDemoAmex(TransData transData, ETransType transType) {
        int ret = doPack(transData);
        if (ret != TransResult.SUCC)
            return ret;

        // 联机交易标识
        transData.setOnlineTrans(true);

        // 发送数据
        Log.i(TAG, "SEND:" + Utils.bcd2Str(sendData));
        ret = this.commURLConnection(sendData, transData);

        return ret;
    }

    private int openURLConnection(HttpsURLConnection httpsConn, String strUrl, int connTimeout) {
        try {
            URL url = new URL(strUrl);
            this.httpsConn = (HttpsURLConnection) url.openConnection();

            this.httpsConn.setRequestProperty("POST", "/IPPayments/inter/CardAuthorization.do HTTP/1.1");
            this.httpsConn.setRequestProperty("Accept-Language", "en-us");
            this.httpsConn.setRequestProperty("Content-Type", "plain/text");
            this.httpsConn.setRequestProperty("User-Agent", "Application");
            this.httpsConn.setRequestProperty("Host", "qwww318.americanexpress.com");
            this.httpsConn.setRequestProperty("Cache-Control", "no-cache");
            this.httpsConn.setRequestProperty("Connection", "close");//Keep-Alive
            this.httpsConn.setRequestProperty("origin", "BPS");
            this.httpsConn.setRequestProperty("region", "JAPA");
            this.httpsConn.setRequestProperty("country", "764");
            this.httpsConn.setRequestProperty("RtInd", "050");
            this.httpsConn.setRequestProperty("MerchNbr", "1810060202");
            this.httpsConn.setRequestProperty("message", "EDC JAPA");
            this.httpsConn.setDoInput(true);
            this.httpsConn.setDoOutput(true);
            this.httpsConn.setUseCaches(false);
            this.httpsConn.setConnectTimeout(connTimeout);
            this.httpsConn.setReadTimeout(connTimeout);

            onShowMsg(Utils.getString(R.string.wait_connect));

            this.httpsConn.connect();
            return TransResult.SUCC;
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
        return TransResult.ERR_CONNECT;
    }

    private int commURLConnection(byte[] sendData, TransData transData) {
        try {
            int ret = openURLConnection(httpsConn, "https://qwww318.americanexpress.com/IPPayments/inter/CardAuthorization.do", 30000);
            if (ret != TransResult.SUCC) {
                return ret;
            }

            String authReqParam = "AuthorizationRequestParam=";
            DataOutputStream out = new DataOutputStream(httpsConn.getOutputStream());
            out.writeBytes(authReqParam + Tools.bcd2Str(sendData).substring(4));
//            out.writeBytes(authReqParam + "600007000002003020058020C00204004000000000003000000107055300070037374245001741007D241220117101234500000036363630303033393136353535363433343220202020200086C1C7D5E200016E578737C59D63A40706020103A420029150E45B0102000000800018062900000000003000076407643C00000000000000008008A00000002501080100014403020000000000BC78FCF800BC50ECA8000006303030313037");
            Log.e(TAG, out.toString());
            out.flush();
            out.close();

            // 冲正处理
            initReversalStatus(transData);

            onShowMsg(Utils.getString(R.string.wait_recv));
            int status = httpsConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = new BufferedInputStream(httpsConn.getInputStream());
                BufferedReader responseStream = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStream.readLine()) != null) {
                    stringBuilder.append(line).append("");
                }
                responseStream.close();
                String responseMsg = stringBuilder.toString();
                httpsConn.disconnect();
                ETransType transType = transData.getTransType();
                if (transType != ETransType.BPS_QR_SALE_INQUIRY && transType != ETransType.PROMPT_ADV) {//Modified by Cz to check transType for PromptPay before increasing traceNo.
                    // Kui move to apply with real Trace/Stan
                    increaseStanNo(transData);
                }

                return doUnpackAMEX(transData, responseMsg);
            }
            return TransResult.SUCC;
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } finally {
            if (httpsConn != null) {
                httpsConn.disconnect();
            }
        }
        increaseStanNo(transData);
        if (dupPackager != null && transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL && transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE) {
            transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            transData.setDupReason(TransData.DUP_REASON_NO_RECV);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        return TransResult.ERR_RECV;
    }

    private int doUnpackAMEX(TransData transData, String msgResponse) {
        Log.i(TAG, "RECV:" + msgResponse);
        if (dupPackager != null && transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            return dupPackager.unpack(transData, Tools.str2Bcd(msgResponse));
        }
        int ret = packager.unpack(transData, Tools.str2Bcd(msgResponse));
        // 更新冲正原因
        if (ret == TransResult.ERR_MAC && dupPackager != null &&
                transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL && transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE) {
            transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            transData.setDupReason(TransData.DUP_REASON_MACWRONG);
            transData.setDateTime(transData.getDateTime());
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        // 如果39域返回null,删除冲正文件, 或者解包3， 4， 11， 41，42域与请求不同时，删除冲正(BCTC要求下笔交易不发冲正)
        if (isErrPackage(ret) || isErrField(ret)) {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
        }
        if (transData.getResponseCode() != null) {
            ETransType transType = transData.getTransType();
            if ("00".equals(transData.getResponseCode().getCode()) ||
                    ("95".equals(transData.getResponseCode().getCode()) && transType == ETransType.SETTLE_END)) {
                if (transType != ETransType.BPS_QR_SALE_INQUIRY && transType != ETransType.PROMPT_ADV) {//Modified by Cz to check transType for PromptPay before increasing traceNo.
                    // Kui move to apply with real Trace
                    increaseTraceNo(transData);
                }
            }
        }
        return ret;
    }

    private boolean isTransSupportTle(ETransType transType) {
        return transType == ETransType.SALE || transType == ETransType.OFFLINE_TRANS_SEND || transType == ETransType.REFUND ||
                transType == ETransType.KBANK_REDEEM_PRODUCT || transType == ETransType.KBANK_REDEEM_PRODUCT_CREDIT ||
                transType == ETransType.KBANK_REDEEM_VOUCHER || transType == ETransType.KBANK_REDEEM_VOUCHER_CREDIT ||
                transType == ETransType.KBANK_REDEEM_DISCOUNT || transType == ETransType.KBANK_SMART_PAY ||
                transType == ETransType.DOLFIN_INSTALMENT || transType == ETransType.DOLFIN_INSTALMENT_INQUIRY;
    }

    private class PackListenerImpl implements PackListener {
        private TransProcessListener listener;
        private Acquirer acquirer;

        PackListenerImpl(TransProcessListener listener, Acquirer acquirer) {
            this.listener = listener;
            this.acquirer = acquirer;
        }

        @Override
        @NonNull
        public byte[] onCalcMac(byte[] data) {
            if (listener != null) {
                return listener.onCalcMac(data);
            }
            Log.v(TAG, "listener == null");
            return "".getBytes();
        }

        @Override
        @NonNull
        public byte[] onEncTrack(byte[] track) {
            if (listener != null) {
                return listener.onEncTrack(track);
            }
            return "".getBytes();
        }

        @NonNull
        @Override
        public String onGetAcqName() {
            return acquirer.getName();
        }

    }
}
