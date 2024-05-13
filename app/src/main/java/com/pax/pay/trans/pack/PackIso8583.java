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
package com.pax.pay.trans.pack;

import android.content.Context;
import android.content.SharedPreferences;

import th.co.bkkps.utils.Log;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.IPacker;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.abl.utils.EncUtils;
import com.pax.device.DeviceImplNeptune;
import com.pax.device.UserParam;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.MerchantAcqProfileDb;
import com.pax.pay.db.MerchantProfileDb;
import com.pax.pay.ped.PedManager;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class PackIso8583 implements IPacker<TransData, byte[]> {

    protected static final String TAG = "PackIso8583";

    private IIso8583 iso8583;
    protected IIso8583.IIso8583Entity entity;
    protected PackListener listener;
    protected Map<String, String> reqMsg;

    private static final List<Integer> fixField = Arrays.asList(37, 38, 39, 41, 42, 49, 51);
    private static final List<Integer> varyField2 = Arrays.asList(2, 35, 44, 45);
    private static final List<Integer> varyField3 = Arrays.asList(36, 48, 54, 55, 60, 61, 62, 63);

    private static final int LEN_HEADER = 5;
    private static final int LEN_BYTE_8583_MTI = 2;
    private static final int LEN_BYTE_8583_BITMAP = 8;

    private static final int TLE_HEADER_LEN = 41;

    private byte[] TLEFields;
    private byte[] TLEEncField;
    private byte[] workBuf;

    private static final Map<TransData.EnterMode, String> enterModeMap = new EnumMap<>(TransData.EnterMode.class);

    public static final String PROC_CODE_SALE_AMEX = "004000";
    public static final String PROC_CODE_REFUND_AMEX = "204000";
    private static final String PROC_CODE_PREAUTH_AMEX = "304000";
    private static final String PROC_CODE_SALEVOID_AMEX = "024000";
    private static final String PROC_CODE_REFUNDVOID_AMEX = "224000";
    private static final String PROC_CODE_TEST_LINK_AMEX = "994000";

    static {
        enterModeMap.put(TransData.EnterMode.MANUAL, "01");
        enterModeMap.put(TransData.EnterMode.SWIPE, "02");
        enterModeMap.put(TransData.EnterMode.INSERT, "05");
        enterModeMap.put(TransData.EnterMode.CLSS, "07");
        enterModeMap.put(TransData.EnterMode.FALLBACK, "80");
        enterModeMap.put(TransData.EnterMode.QR, "03");
        enterModeMap.put(TransData.EnterMode.SP200, "07");
    }

    public PackIso8583(PackListener listener) {
        this.listener = listener;
        initEntity();
    }

    // VERIFONE-ERM
    private boolean VF_ERCM_MODE_ENABLED = false;

    public boolean getIsErcmEnable() {
        return VF_ERCM_MODE_ENABLED;
    }

    public void setIsErcmEnable(boolean exBool) {
        VF_ERCM_MODE_ENABLED = exBool;
        try {
            if (exBool) {
                entity.loadTemplate(FinancialApplication.getApp().getResources().getAssets().open("ercm_8583.xml"));
            } else {
                entity.loadTemplate(FinancialApplication.getApp().getResources().getAssets().open("edc8583.xml"));
            }
        } catch (Iso8583Exception | IOException | XmlPullParserException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * 获取打包entity
     *
     * @return
     */
    private void initEntity() {
        TLEFields = null;
        iso8583 = FinancialApplication.getPacker().getIso8583();
        try {
            reqMsg = new HashMap<>();
            entity = iso8583.getEntity();
            entity.loadTemplate(FinancialApplication.getApp().getResources().getAssets().open("edc8583.xml"));
        } catch (Iso8583Exception | IOException | XmlPullParserException e) {
            Log.e(TAG, "", e);
        }
    }

    protected final void setBitData(String field, String value) throws Iso8583Exception {
        if (value != null && !value.isEmpty()) {
            entity.setFieldValue(field, value);
            reqMsg.put(field, value);
        }
    }

    protected final void setBitData(String field, byte[] value) throws Iso8583Exception {
        if (value != null && value.length > 0) {
            entity.setFieldValue(field, value);
            reqMsg.put(field, Utils.bcd2Str(value));
        }
    }

    protected final void setBitData(String field, String value, IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs) throws Iso8583Exception {
        if (value != null && !value.isEmpty()) {
            entity.setFieldValue(field, value).setFieldAttrs(field, iFieldAttrs);
            reqMsg.put(field, value);
        }
    }

    protected final boolean IsTransTLE(@NonNull TransData transData) {
        if (!FinancialApplication.getAcqManager().getCurAcq().isEnableTle())
            return false;

        if (transData.getTransType() == ETransType.LOADTMK
                || transData.getTransType() == ETransType.LOADTWK
                || transData.getTransType() == ETransType.LOAD_UPI_RSA
                || transData.getTransType() == ETransType.LOAD_UPI_TMK
                || transData.getTransType() == ETransType.ACT_UPI_TMK
                || transData.getTransType() == ETransType.LOAD_UPI_TWK) {
            return false;
        }
        return true;
    }

    private byte[] genERMTLEDHeader(TransData transData, byte[] input_msg_data, EReceiptLogoMapping ercmSessionKeyMapping) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int iLenRawData = 64;
        byte[] StrLenRawData = ((String) ((iLenRawData < 9) ? "00" : ((iLenRawData < 99) ? "0" : "")) + iLenRawData).getBytes();
        String sKek_type = "0"; //FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_TYPE);
        String sKek_version = ercmSessionKeyMapping.getKekVersion(); //FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION);

        byte[] tle_indicator = {0x45, 0x54, 0x4C, 0x45}; //ETLE
        byte[] version = {0x30, 0x31};//version
        byte[] bankCode = Component.getPaddedStringRight(transData.getERCMBankCode(), 20, '\u0000').getBytes();
        String sn = "EX-" + FinancialApplication.getDownloadManager().getSn();//todo
        byte[] tSN = EReceiptUtils.getInstance().GetSerialNumber(FinancialApplication.getDownloadManager().getSn()).getBytes();//Component.getPaddedStringRight(sn, 15, '\u0000').getBytes();
        byte[] encrypted_method = {0x34, 0x30, 0x30, 0x30};//encryption method (TDES in ECB mode, double key length)
        byte[] len_orig_msg = Component.getPaddedNumber(input_msg_data.length, 4).getBytes();//length of original message (unencrypted) //todo include TPDU???
        byte[] msg_type = Tools.str2Bcd(transData.getTransType().getMsgType());//msg type
        byte[] reserved = {0x30, 0x30, 0x30, 0x30, 0x30};//reserved slot
        byte[] kek_type = new byte[]{0x30};//sKek_type != null ? sKek_type.getBytes() : new byte[]{0x30};//kek type: 0: no session key in message, 1: 1024 bit RSA key, 2: 2048 bit RSA key
        byte[] kek_version = sKek_version.getBytes(); // /*sKek_version != null ? sKek_version.getBytes() : */new byte[]{0x30, 0x30, 0x30, 0x33};//kek version //todo get kek version

        byte[] header_length = {0x30, 0x30, 0x30};//temp header length
        int lenHeader = tle_indicator.length + version.length + bankCode.length + tSN.length + header_length.length +
                encrypted_method.length + len_orig_msg.length + msg_type.length + reserved.length + kek_type.length + kek_version.length;

        byte[] session_key_kcv = ercmSessionKeyMapping.getSessionKeyKCV();//Tools.str2Bcd(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_KCV));
        byte[] encrypted_session_key = ercmSessionKeyMapping.getSessionKeyEncrypted(); //Tools.str2Bcd(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ENC_DATA));

        switch (kek_type[0]) {
            case 48://type 0
                header_length = Component.getPaddedNumber(lenHeader, 3).getBytes();
                break;
            case 49://type 1
            case 50://type 2
                lenHeader += (session_key_kcv.length + encrypted_session_key.length);
                header_length = Component.getPaddedNumber(lenHeader, 3).getBytes();
                break;
        }

        outputStream.write(tle_indicator);
        outputStream.write(version);
        outputStream.write(bankCode);
        outputStream.write(tSN);
        outputStream.write(StrLenRawData);//(header_length);
        outputStream.write(encrypted_method);
        outputStream.write(len_orig_msg);
        outputStream.write(msg_type);
        outputStream.write(reserved);
        outputStream.write(kek_type);
        outputStream.write(kek_version);
        if (kek_type[0] != 48) {
            //outputStream.write(session_key_kcv);
            //outputStream.write(encrypted_session_key);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    protected byte[] packERMTLEDData(TransData transData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] packData = iso8583.pack();
            Log.i(TAG, "No Encrypt SEND:" + Tools.bcd2Str(packData));

            byte[] TPDU = Arrays.copyOfRange(packData, 0, 5);
            byte[] MSGTYPE_BITMAP_DE = Arrays.copyOfRange(packData, 5, packData.length);

            /* todo Start should remove if it stored in secure mem. */
            EReceiptLogoMapping ErcmLogoSskMapping = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerIndex(transData.getInitAcquirerIndex());

            byte[] session_key = ErcmLogoSskMapping.getSessionKeyClearText();
            DeviceImplNeptune dev = DeviceImplNeptune.getInstance();
            byte[] e_message_data = dev.desede(MSGTYPE_BITMAP_DE, session_key, 1, "DESede/ECB/PKCS5Padding");

            /* todo End */

//            byte[] MSGTYPE_BITMAP_DE_PKCS5 = genDataPKCS5Padding(MSGTYPE_BITMAP_DE);
//            byte[] e_message_data = tDesEncrypt(transData.getAcquirer(), MSGTYPE_BITMAP_DE_PKCS5);
//            FinancialApplication.getPedInstance().calcDes(PedManager.TRI_ENCRYPT4, (byte) iKey, MSGTYPE_BITMAP_DE); //todo use this if session key has been stored in secure mem.

            byte[] TLED_HEADER = genERMTLEDHeader(transData, MSGTYPE_BITMAP_DE, ErcmLogoSskMapping);

            outputStream.write(TPDU);
            outputStream.write(TLED_HEADER);
//            outputStream.write(MSGTYPE_BITMAP_DE);
            outputStream.write(e_message_data);
            saveArray(Utils.buildReqMsg(transData, reqMsg), "ReqMsg", getCurrentContext());
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
        return "".getBytes();
    }

    protected byte[] packWithTLE(TransData transData) {
        try {
            byte[] packData = iso8583.pack();
            //Log.d("Online", "RawData:" + Tools.bcd2Str(packData));
            byte[] result = packedTLE(packData);
            saveArray(Utils.buildReqMsg(transData, reqMsg), "ReqMsg", getCurrentContext());
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return result;
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @NonNull
    protected byte[] pack(boolean isNeedMac, TransData transData) {
        try {
            if (isNeedMac) {
                setBitData("64", new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
            }
            // for debug entity.dump();
            byte[] packData = iso8583.pack();

            if (isNeedMac) {
                if (packData == null || packData.length == 0) {
                    return "".getBytes();
                }

                int len = packData.length;

                byte[] calMacBuf = new byte[len - 11 - 8];//去掉header和mac
                System.arraycopy(packData, 11, calMacBuf, 0, len - 11 - 8);
                byte[] mac = listener.onCalcMac(calMacBuf);
                if (mac.length == 0) {
                    return "".getBytes();
                }
                System.arraycopy(mac, 0, packData, len - 8, 8);
            }
            saveArray(Utils.buildReqMsg(transData, reqMsg), "ReqMsg", getCurrentContext());
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return packData;
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    public int unpack(@NonNull TransData transData, final byte[] rsp) {

        HashMap<String, byte[]> map;
        Map<String, String> respMsg = new LinkedHashMap<>();
        try {
            map = iso8583.unpack(rsp, true);
            // 调试信息， 日志输入解包后数据
            entity.dump();
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return TransResult.ERR_UNPACK;
        }

        // 报文头
        byte[] header = map.get("h");
        // TPDU检查
        String rspTpdu = new String(header).substring(0, 10);
        String reqTpdu = transData.getTpdu();
        if (!rspTpdu.substring(2, 6).equals(reqTpdu.substring(6, 10))
                || !rspTpdu.substring(6, 10).equals(reqTpdu.substring(2, 6))) {
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return TransResult.ERR_UNPACK;
        }
        transData.setHeader(new String(header).substring(10));
        respMsg.put("tpdu", rspTpdu);

        String responseData = Utils.bcd2Str(rsp);
        respMsg.put("Msg Type", new String(responseData).substring(10, 14));

        ETransType transType = transData.getTransType();

        byte[] buff;
        // 检查39域应答码
        buff = map.get("39");
        if (buff == null && transType != ETransType.ECHO) {
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return TransResult.ERR_PACKET;
        } else if (buff != null) {
            transData.setResponseCode(FinancialApplication.getRspCode().parse(new String(buff)));
        }

        boolean isAmexVoidTrans = (Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) ||
                Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName())) &&
                (transData.getTransType() == ETransType.VOID || transData.getOrigTransType() == ETransType.VOID);

        // 检查返回包的关键域， 包含field4
        boolean isCheckAmt = true;
        if (transType == ETransType.SETTLE
                || isAmexVoidTrans
                || Constants.ACQ_REDEEM.equals(transData.getAcquirer().getName())
                || Constants.ACQ_REDEEM_BDMS.equals(transData.getAcquirer().getName())
                || Constants.ACQ_BAY_INSTALLMENT.equals(transData.getAcquirer().getName())
                || transType == ETransType.QR_VERIFY_PAY_SLIP) {
            isCheckAmt = false;
        }
        int ret = checkRecvData(map, transData, isCheckAmt);
        if (ret != TransResult.SUCC) {
            saveArray(new String[0], "RespMsg", getCurrentContext());
            return ret;
        }

        // field 2 主账号
        buff = map.get("2");
        if (buff != null && buff.length > 0) {
            respMsg.put("2", new String(buff));
        }

        // field 3 交易处理码
        buff = map.get("3");
        if (buff != null && buff.length > 0) {
            String origField3 = transData.getField3();
            respMsg.put("3", new String(buff));
            if (origField3 != null && !origField3.isEmpty() && !origField3.equals(new String(buff))) {
                saveArray(new String[0], "RespMsg", getCurrentContext());
                return TransResult.ERR_PROC_CODE;
            }
        }
        // field 4 交易金额
        buff = map.get("4");
        if (buff != null && buff.length > 0) {
            if (!isAmexVoidTrans) {
                transData.setAmount(new String(buff));
                respMsg.put("4", new String(buff));
            } else {
                respMsg.put("4", String.valueOf(Utils.parseLongSafe(new String(buff), 0)));
            }

        }

        // field 6, DCC Amount
        buff = map.get("6");
        if (buff != null && buff.length > 0) {
            transData.setDccAmount(new String(buff));
            respMsg.put("6", String.valueOf(Utils.parseLongSafe(new String(buff), 0)));
        }

        // field 10, DCC Conversion Rate
        buff = map.get("10");
        if (buff != null && buff.length > 0) {
            transData.setDccConversionRate(new String(buff));
            respMsg.put("10", String.valueOf(Utils.parseLongSafe(new String(buff), 0)));
        }

        // field 11 流水号
        buff = map.get("11");
        if (buff != null && buff.length > 0) {
            long tempStan = Utils.parseLongSafe(new String(buff), -1);
            transData.setStanNo(tempStan);
            respMsg.put("11", String.valueOf(tempStan));
        }

        // field 13 受卡方所在地日期
        String dateTime = "";
        String tmp13 = null;
        buff = map.get("13");
        if (buff != null) {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            dateTime = Integer.toString(year) + new String(buff);
            tmp13 = new String(buff);
        }
        // field 12 受卡方所在地时间
        buff = map.get("12");
        if (buff != null && buff.length > 0) {
            transData.setDateTime(dateTime + new String(buff));
            respMsg.put("12", new String(buff));
        }
        if (tmp13 != null) {
            respMsg.put("13", tmp13);
        }

        // field 14 卡有效期
        buff = map.get("14");
        if (buff != null && buff.length > 0) {
            String expDate = new String(buff);
            if (!"0000".equals(expDate)) {
                transData.setExpDate(expDate);
                respMsg.put("14", expDate);
            }
        }

        // field 17
        buff = map.get("17");
        if (buff != null && buff.length > 0) {
            transData.setTransYear(new String(buff));
        }

        // field 22
        buff = map.get("22");
        if (buff != null && buff.length > 0) {
            respMsg.put("22", new String(buff));
        }

        // field 23 卡片序列号
        buff = map.get("23");
        if (buff != null && buff.length > 0) {
            transData.setCardSerialNo(new String(buff));
            respMsg.put("23", new String(buff));
        }
        // field 24
        respMsg.put("24", transData.getNii());
        // field 25
        // field 26

        // field 28
        buff = map.get("28");
        if (buff != null && buff.length > 0) {
            transData.setField28(new String(buff));
            respMsg.put("28", new String(buff));
        }

        // field 35
        // field 36

        // field 37 检索参考号
        buff = map.get("37");
        if (buff != null && buff.length > 0) {
            transData.setRefNo(new String(buff));
            respMsg.put("37", new String(buff));
        }

        // field 38 授权码
        buff = map.get("38");
        if (buff != null && buff.length > 0) {
            transData.setAuthCode(new String(buff));
            respMsg.put("38", new String(buff));
        }

        // field 39
        if (transData.getResponseCode() != null) {
            respMsg.put("39", transData.getResponseCode().getCode());
        }

        // field 41
        respMsg.put("41", transData.getAcquirer().getTerminalId());

        // field 44
        buff = map.get("44");
        if (buff != null && buff.length > 11) {
            String temp = new String(buff).substring(0, 11).trim();
            transData.setIssuerCode(temp);
            if (buff.length > 11) {
                temp = new String(buff).substring(11).trim();
                transData.setAcqCode(temp);
                respMsg.put("44", temp);
            }
        }
        // field 48
        buff = map.get("48");
        if (buff != null && buff.length > 0) {
            transData.setField48(new String(buff));
            respMsg.put("48", new String(buff));
        }

        // field 51, DCC Currency Code
        buff = map.get("51");
        if (buff != null && buff.length > 0) {
            transData.setDccCurrencyCode(buff);
            respMsg.put("51", Tools.bytes2String(buff));
        }

        // field 52

        // field 53

        // field 54

        // field 55
        buff = map.get("55");
        if (buff != null && buff.length > 0) {
            transData.setRecvIccData(Utils.bcd2Str(buff));
            respMsg.put("55", Utils.bcd2Str(buff));
        }

        // field 57
        buff = map.get("57");
        if (buff != null && buff.length > 0) {
            respMsg.put("57", Utils.bcd2Str(buff));
        }

        // field 58

        // field 60 //TODO: KiTty below value should override somewhere else
        buff = map.get("60");
        if (buff != null && buff.length > 0) {
            String temp = new String(buff);
            if (transData.getTransType() != ETransType.QR_VERIFY_PAY_SLIP
                    && !transData.getAcquirer().getName().equals(Constants.ACQ_MY_PROMPT)) {
                if (transData.getTransType() != ETransType.GET_QR_KPLUS
                        && transData.getTransType() != ETransType.GET_QR_ALIPAY
                        && transData.getTransType() != ETransType.GET_QR_WECHAT
                        && transData.getTransType() != ETransType.QR_INQUIRY
                        && transData.getTransType() != ETransType.QR_INQUIRY_ALIPAY
                        && transData.getTransType() != ETransType.QR_INQUIRY_WECHAT
                        && transData.getTransType() != ETransType.QR_VOID
                        && transData.getTransType() != ETransType.QR_VOID_KPLUS
                        && transData.getTransType() != ETransType.QR_VOID_ALIPAY
                        && transData.getTransType() != ETransType.QR_VOID_WECHAT
                        && transData.getTransType() != ETransType.DOLFIN_INSTALMENT
                        && transData.getTransType() != ETransType.DOLFIN_INSTALMENT_INQUIRY
                        && transData.getTransType() != ETransType.DOLFIN_INSTALMENT_VOID
                ) {
                    if (temp.length() > 6) {
                        transData.setBatchNo(Utils.parseLongSafe(temp.substring(2, 8), -1));
                    } else {
                        transData.setBatchNo(Utils.parseLongSafe(temp, -1));
                    }
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(buff, 0, buff.length);
                transData.setField60RecByte(outputStream.toByteArray());
                respMsg.put("60", Utils.bcd2Str(outputStream.toByteArray()));
            }
        }
        // field 61 EDCBBLAND-110: Support wallet settle slip info.
        buff = map.get("61");
        if (buff != null && buff.length > 0) {
            transData.setField61RecByte(buff);
            respMsg.put("61", Utils.bcd2Str(buff));
        }

        // field 62
        buff = map.get("62");
        if (buff != null && buff.length > 0) {
            transData.setField62(Utils.bcd2Str(buff));
            transData.setBytesField62(buff);
            respMsg.put("62", Utils.bcd2Str(buff));
        }

        // field 63 -- Added by Cz for response message from PromptPay.
        buff = map.get("63");
        if (buff != null && buff.length > 0) {
            transData.setField63(Tools.bytes2String(buff));
            transData.setField63RecByte(buff);
            respMsg.put("63", Utils.bcd2Str(buff));
        }


        // field 64
        // 解包校验mac
        if (IsTransTLE(transData)) {
            byte[] pNonTLE = null;

            buff = map.get("64");
            if (buff != null && buff.length > 0 && listener != null) {
                try {
                    pNonTLE = RspUnpackTLEFields(rsp);
                } catch (Iso8583Exception e) {
                    Log.e(TAG, "", e);
                }

                byte[] pData = new byte[pNonTLE.length - LEN_HEADER - 8];

                System.arraycopy(pNonTLE, LEN_HEADER, pData, 0, pNonTLE.length - LEN_HEADER - 8);

                //int keyIdID = FinancialApplication.getAcqManager().getCurAcq().getKeyId();
                //byte[] mac = EncUtils.SHA_X919(pData, keyIdID);
                byte[] mac = EncUtils.SHA_X919(pData, FinancialApplication.getAcqManager().getCurAcq());
                Arrays.fill(mac, mac.length / 2, mac.length, (byte) 0);
                respMsg.put("64", Utils.bcd2Str(buff));
                if (!FinancialApplication.getConvert().isByteArrayValueSame(buff, 0, mac, 0, 8)) {
                    saveArray(new String[0], "RespMsg", getCurrentContext());
                    return TransResult.ERR_MAC;
                }
            }
        } else {
            if (rsp.length >= 19) {
                byte[] data = new byte[rsp.length - 11 - 8];
                System.arraycopy(rsp, 11, data, 0, data.length);
                buff = map.get("64");
                if (buff != null && buff.length > 0 && listener != null) {
                    byte[] mac = listener.onCalcMac(data);
                    if (!FinancialApplication.getConvert().isByteArrayValueSame(buff, 0, mac, 0, 8)) {
                        saveArray(new String[0], "RespMsg", getCurrentContext());
                        return TransResult.ERR_MAC;
                    }
                }
            }
        }
        saveArray(Utils.buildRespMsg(transData, respMsg), "RespMsg", getCurrentContext());
        return TransResult.SUCC;
    }

    /**
     * 设置公共数据
     * <p>
     * 设置域： h,m, field 3, field 25, field 41,field 42
     *
     * @param transData
     * @return
     */
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3/25 交易处理码/服务码
        setBitData3(transData);
        if (transType != ETransType.LOADTMK &&
                transType != ETransType.LOAD_UPI_RSA &&
                transType != ETransType.LOAD_UPI_TMK &&
                transType != ETransType.ACT_UPI_TMK &&
                transType != ETransType.LOAD_UPI_TWK) {
            setBitData25(transData);
        }

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        if (transType != ETransType.LOAD_UPI_RSA &&
                transType != ETransType.LOAD_UPI_TMK &&
                transType != ETransType.ACT_UPI_TMK)
            setBitData24(transData);

        // field 41 终端号
        setBitData41(transData);

        // field 42 商户号
        if (transType != ETransType.LOADTMK && transType != ETransType.LOADTWK) {
            setBitData42(transData);
        }
    }

    /**
     * 设置field 2, 4, 11, 14, 22, 23, 26, 35,36,49,52, 53
     *
     * @param transData
     * @return
     */
    protected void setCommonData(@NonNull TransData transData) throws Iso8583Exception {
        TransData.EnterMode enterMode = transData.getEnterMode();

        if (enterMode == TransData.EnterMode.MANUAL) {
            // 手工输入
            // [2]主账号,[14]有效期
            setBitData2(transData);

            setBitData14(transData);

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {
            // 刷卡

            // [35]二磁道,[36]三磁道
            setBitData35(transData);
            setBitData36(transData);

            //[54]tip amount  by lixc
            setBitData54(transData);

        } else if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS) {
            // [2]主账号
            setBitData2(transData);

            // [14]有效期
            setBitData14(transData);

            // [23]卡序列号
            setBitData23(transData);

            // [35]二磁道
            setBitData35(transData);
        }

        // field amount
        setBitData4(transData);

        // field 11 流水号
        setBitData11(transData);

        // field 22 服务点输入方式码
        setBitData22(transData);

        // [52]PIN
        setBitData52(transData);
    }

    /**
     * 设置金融类数据
     * <p>
     * 设置域
     * <p>
     * field 2, field 4,field 14, field 22,field 23,field 26, field 35,field 36,field 49, field 52,field 53, field 55
     *
     * @param transData
     */
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        setMandatoryData(transData);
        setCommonData(transData);
        // field 55 ICC
        setBitData55(transData);
    }

    /**
     * 检查请求和返回的关键域field4, field11, field41, field42
     *
     * @param map        解包后的map
     * @param transData  请求
     * @param isCheckAmt 是否检查field4
     * @return
     */
    protected int checkRecvData(@NonNull HashMap<String, byte[]> map, @NonNull TransData transData, boolean isCheckAmt) {
        // 交易金额
        if (isCheckAmt && !checkAmount(map, transData)) {
            return TransResult.ERR_TRANS_AMT;
        }

        // 校验11域
        if (!checkStanNo(map, transData))
            return TransResult.ERR_STAN_NO;

        /* No need to check based on monitor */
//        if (!checkTerminalId(map, transData))
//            return TransResult.ERR_TERM_ID;
//
//        if (!checkMerchantId(map, transData))
//            return TransResult.ERR_MERCH_ID;

        return TransResult.SUCC;
    }

    protected void setBitHeader(@NonNull TransData transData) throws Iso8583Exception {
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
    }

    protected void setBitData1(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData2(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("2", transData.getPan());
    }

    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if ((Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName())) &&
                transData.getTransType() != ETransType.SETTLE && transData.getTransType() != ETransType.SETTLE_END) {
            setBitData("3", setProcCodeAmex(transData));
        } else if (transData.getTransType() == ETransType.VOID && transData.getOrigTransType() == ETransType.REFUND) {
            setBitData("3", "220000");
        } else {
            setBitData("3", transData.getTransType().getProcCode());
        }
    }

    protected void setBitData4(@NonNull TransData transData) throws Iso8583Exception {
        String value = ((Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()))
                && transData.getTransType() == ETransType.VOID) ? "0" : transData.getAmount();
        setBitData("4", value);
    }

    protected void setBitData5(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData6(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("6", transData.getDccAmount());
    }

    protected void setBitData7(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData8(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData9(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData10(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("10", transData.getDccConversionRate());
    }

    protected void setBitData11(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("11", String.valueOf(transData.getStanNo()));
    }

    protected void setBitData12(@NonNull TransData transData) throws Iso8583Exception {
        String temp = transData.getDateTime();
        if (temp != null && !temp.isEmpty()) {
            String date = temp.substring(4, 8);
            String time = temp.substring(8, temp.length());
            setBitData("12", time);
            setBitData("13", date);
        }
    }

    protected void setBitData13(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData14(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("14", transData.getExpDate());
    }

    protected void setBitData15(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData16(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData17(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData18(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData19(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData20(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData21(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
//        setBitData("22", getInputMethod(transData.getEnterMode(), transData.isHasPin()));
        String value = getInputMethodByAcq(transData);
       /* String value = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) ?
                            getInputMethodAmex(transData.getEnterMode(), transData.isHasPin()) :
                            enterModeMap.get(transData.getEnterMode()) + "1";*/
        if (value != null && !value.isEmpty()) {
            IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
            setBitData("22", value, iFieldAttrs22);
        }
    }

    protected void setBitData23(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("23", transData.getCardSerialNo());
    }

    protected void setBitData24(@NonNull TransData transData) throws Iso8583Exception {
        if(ControlLimitUtils.Companion.isEnableControlLimit(transData.getAcquirer().getName())) {
            setBitData("24", UserParam.CTRL_LIMIT_NII);
        } else {
            setBitData("24", transData.getNii());
        }
    }

    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        String value = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()) ?
                setServiceCodeAmex(transData) : transData.getTransType().getServiceCode();
        setBitData("25", value);
    }

    protected void setBitData26(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData27(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData28(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("28", transData.getField28());
    }

    protected void setBitData29(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData30(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData31(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData32(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData33(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData34(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData35(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("35", transData.getTrack2());
    }

    protected void setBitData36(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("36", transData.getTrack3());
    }

    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getRefNo());
    }

    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getOrigAuthCode());
    }

    protected void setBitData39(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData40(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData41(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("41", transData.getAcquirer().getTerminalId());
    }

    protected void setBitData42(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("42", transData.getAcquirer().getMerchantId());
    }

    protected void setBitData43(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData44(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData45(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData48(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("48", transData.getField48());
    }

    protected void setBitData51(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("51", transData.getDccCurrencyCode());
    }

    protected void setBitData52(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.isHasPin() && transData.getPin() != null) {
            setBitData("52", FinancialApplication.getConvert().strToBcd(transData.getPin(), EPaddingPosition.PADDING_LEFT));
        }
    }

    protected void setBitData54(@NonNull TransData transData) throws Iso8583Exception {
        String f54 = "000000000000";
        if (transData.getTipAmount() != null) {
            f54 = transData.getTipAmount().length() < 12 ?
                    Component.getPaddedString(transData.getTipAmount(), 12, '0') :
                    transData.getTipAmount();
        }
        setBitData("54", f54);
    }

    protected void setBitData55(@NonNull TransData transData) throws Iso8583Exception {
        String temp = transData.getSendIccData();
        if (temp != null && temp.length() > 0) {
            setBitData("55", FinancialApplication.getConvert().strToBcd(temp, EPaddingPosition.PADDING_LEFT));
        }
    }

    protected void setBitData58(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData59(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName());
        //comment below coz Batch upload of all credit acquirers use format filed 60 same as AMEX
//        if ((isAmex && transData.getTransType() == ETransType.BATCH_UP) || transData.getTransType() == ETransType.TCADVICE){
        if (transData.getTransType() == ETransType.BATCH_UP || transData.getTransType() == ETransType.TCADVICE) {
            String msgType = isAmex && transData.isReferralSentSuccess() ? "0220" : transData.getOrigTransType().getMsgType();
            String f60 = msgType + Component.getPaddedNumber(transData.getOrigTransNo(), 6) + String.format("%-12s", "");
            setBitData("60", f60);
        } else {
            setBitData("60", Component.getPaddedNumber(transData.getBatchNo(), 6));
        }
    }

    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    // set field 61
    protected void setBitData61Byte(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("61", transData.getField61Byte());
    }

    protected void setBitData62(@NonNull TransData transData) throws Iso8583Exception {
        String traceNumber = Utils.getStringPadding(transData.getTraceNo(),6, "0", Convert.EPaddingPosition.PADDING_LEFT);

        if(ControlLimitUtils.Companion.isEnableControlLimit(transData.getAcquirer().getName())) {
            String oriTransNii = Utils.getStringPadding(transData.getAcquirer().getNii(),4, "0", Convert.EPaddingPosition.PADDING_LEFT);

            if (transData.getPhoneNum()==null) { transData.setPhoneNum(""); }
            String phoneNo     = Utils.getStringPadding(transData.getPhoneNum(),10, " ", Convert.EPaddingPosition.PADDING_LEFT);

            String ctrlLimitDE62Data = traceNumber + oriTransNii + phoneNo;
            setBitData("62", ctrlLimitDE62Data);
        } else {
            setBitData("62", traceNumber);
        }
    }

    // set field 63
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", transData.getField63());
    }

    // set field 63
    protected void setBitData63Byte(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", transData.getField63Byte());
    }

    /**
     * @param enterMode
     * @param hasPin
     * @return
     */
    protected final String getInputMethod(TransData.EnterMode enterMode, boolean hasPin) {
        if (enterMode == null) //AET-40
            return null;
        String inputMethod;
        try {
            inputMethod = enterModeMap.get(enterMode);
        } catch (Exception e) {
            Log.w(TAG, "", e);
            return null;
        }

        if (hasPin) {
            inputMethod += "1";
        } else {
            inputMethod += "2";
        }

        return inputMethod;
    }

    private boolean checkAmount(@NonNull final HashMap<String, byte[]> map, @NonNull final TransData transData) {
        byte[] data = map.get("4");
        if (data != null && data.length > 0) {
            String temp = new String(data);
            if (Utils.parseLongSafe(temp, 0) != Utils.parseLongSafe(transData.getAmount(), 0)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkStanNo(@NonNull final HashMap<String, byte[]> map, @NonNull final TransData transData) {
        byte[] data = map.get("11");
        if (data != null && data.length > 0 && transData.getTransType() != ETransType.ECHO) {
            String temp = new String(data);
            if (!temp.equals(Component.getPaddedNumber(transData.getStanNo(), 6))) {
                return false;
            }
        }
        return true;
    }

    private boolean checkTerminalId(@NonNull final HashMap<String, byte[]> map, @NonNull final TransData transData) {
        byte[] data = map.get("41");
        if (data != null && data.length > 0) {
            String temp = new String(data);
            if (!temp.equals(transData.getAcquirer().getTerminalId())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMerchantId(@NonNull final HashMap<String, byte[]> map, @NonNull final TransData transData) {
        byte[] data = map.get("42");
        if (data != null && data.length > 0) {
            String temp = new String(data);
            if (!temp.equals(transData.getAcquirer().getMerchantId())) {
                return false;
            }
        }
        return true;
    }

    private String setServiceCodeAmex(TransData transData) {
        if (transData.getTransType() == ETransType.PREAUTH) {
            return "00";
        } else {
            return transData.getReferralStatus() == TransData.ReferralStatus.REFERRED ? "06" : transData.getTransType().getServiceCode();
        }
    }

    private final String setProcCodeAmex(TransData transData) {
        ETransType transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();
        String procCode;
        if (transType == ETransType.VOID) {
            procCode = transData.getOrigTransType() != ETransType.REFUND ? PROC_CODE_SALEVOID_AMEX : PROC_CODE_REFUNDVOID_AMEX;//Void of Sale(024000), Refund Void(224000)
        } else if (transType == ETransType.PREAUTH || origTransType == ETransType.PREAUTH) {
            procCode = PROC_CODE_PREAUTH_AMEX;
        } else if (transType == ETransType.REFUND || origTransType == ETransType.REFUND) {
            procCode = PROC_CODE_REFUND_AMEX;
        } else if (transType == ETransType.SALE || transType == ETransType.OFFLINE_TRANS_SEND ||
                origTransType == ETransType.SALE || origTransType == ETransType.OFFLINE_TRANS_SEND ||
                transType == ETransType.AMEX_INSTALMENT) {
            procCode = PROC_CODE_SALE_AMEX;
        } else if (transType == ETransType.ECHO) {
            procCode = PROC_CODE_TEST_LINK_AMEX;
        } else {
            procCode = transType != null ? transType.getProcCode() : "";
        }
        return procCode;
    }

    /**
     * @param transData
     * @return
     */
    protected final String getInputMethodByAcq(TransData transData) {
        if (transData == null) //AET-40
            return null;
        String inputMethod;
        TransData.EnterMode enterMode = transData.getEnterMode();
        ETransType transType = transData.getTransType();
        String acq = transData.getAcquirer().getName();
        try {
            if (Constants.ACQ_AMEX.equals(acq) || Constants.ACQ_AMEX_EPP.equals(acq)) {
                inputMethod = getInputMethodAmex(transData.getEnterMode(), transData.isHasPin());
            } else if (Constants.ACQ_UP.equals(acq)) {
                if (!transType.equals(ETransType.VOID)) {
                    /*1 - PinOnline, 2 - PinOffline, No Pin*/
                    inputMethod = transData.isHasPin() && transData.getPin() != null && transData.isOnlinePin() ?
                            enterModeMap.get(enterMode) + "1" :
                            enterModeMap.get(enterMode) + "2";
                } else {
                    //for KBANK only - need to set '2' for last digit
                    inputMethod = enterModeMap.get(enterMode) + "2";
                }
            } else {
                inputMethod = enterModeMap.get(enterMode) + "1";
            }

        } catch (Exception e) {
            Log.w(TAG, "", e);
            return null;
        }

        return inputMethod;
    }

    final String getInputMethodByIssuer(TransData transData) {
        if (transData == null)
            return null;
        String inputMethod = null;
        TransData.EnterMode enterMode = transData.getEnterMode();
        String issuer = transData.getIssuer().getName();
        try {
            switch (issuer) {
                case Constants.ISSUER_AMEX:
                    inputMethod = getInputMethodAmex(transData.getEnterMode(), transData.isHasPin());
                    break;
                case Constants.ISSUER_UP:
                    inputMethod = getInputMethodUpTbaDci(transData, transData.getEnterMode());
                    break;
                case Constants.ISSUER_TBA:
                    inputMethod = getInputMethodUpTbaDci(transData, transData.getEnterMode());
                    break;
                default:
                    inputMethod = enterModeMap.get(enterMode) + "2";
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }
        return inputMethod;
    }

    /**
     * @param enterMode
     * @param hasPin
     * @return
     */
    protected final String getInputMethodAmex(TransData.EnterMode enterMode, boolean hasPin) {
        if (enterMode == null) //AET-40
            return null;

        String inputMethod = "5";//0,5,6,8
        String panMode;
        switch (enterMode) {
            case INSERT:
                panMode = "5";
                break;
            case CLSS:
            case SP200:
                panMode = "9";
                break;
            case FALLBACK:
                inputMethod = "6";
                panMode = "2";//If with 4DBC Entry (security code), 6
                break;
            case SWIPE:
                panMode = "2";//If with 4DBC Entry (security code), 6
                break;
            case MANUAL:
                panMode = "1";//If with 4DBC Entry (security code), 7
                break;
            default:
                panMode = "2";
                break;
        }

        /*String pinCapability = "3";
        if (hasPin) {
            pinCapability = "1";
        }

        EmvParam emvParam = new EmvParam();
        EMVApi.EMVGetParameter(emvParam);
        if ("E0F0C8".equals(Utils.bcd2Str(emvParam.capability))) {// pin capable
            pinCapability = "1";
        } else if ("E0B0C8".equals(Utils.bcd2Str(emvParam.capability))) {// offline pin capable
            pinCapability = "3";
        }*/

        return inputMethod + panMode + "3";
    }

    private String getInputMethodUpTbaDci(TransData transData, TransData.EnterMode enterMode) {
        String inputMethod;
        if (transData.getTransType() != ETransType.VOID &&
                transData.getTransType() != ETransType.KBANK_REDEEM_VOID &&
                transData.getTransType() != ETransType.KBANK_SMART_PAY_VOID) {
            /*1 - PinOnline, 2 - PinOffline, No Pin*/
            inputMethod = transData.isHasPin() && transData.getPin() != null && transData.isOnlinePin() ?
                    enterModeMap.get(enterMode) + "1" :
                    enterModeMap.get(enterMode) + "2";
        } else {
            //for KBANK only - need to set '2' for last digit
            inputMethod = enterModeMap.get(enterMode) + "2";
        }
        return inputMethod;
    }

    protected void UnpackTLEFields(byte[] pData) {
        int bitNum, index;
        int bitMask;
        byte[] bitValue;

        byte[] BitMap = new byte[LEN_BYTE_8583_BITMAP];
        System.arraycopy(pData, LEN_HEADER + LEN_BYTE_8583_MTI, BitMap, 0, LEN_BYTE_8583_BITMAP);

        HashMap<String, byte[]> map;
        try {
            map = iso8583.unpack(pData, true);
        } catch (Iso8583Exception e) {
            return;
        }

        bitMask = 0x80;
        index = 0;
        for (bitNum = 1; bitNum <= 64; bitNum++) {
            if ((BitMap[index] & bitMask) > 0) {
                if (UserParam.encField.contains(bitNum)) {
                    bitValue = map.get(String.valueOf(bitNum));

                    String valueStr = Tools.hexToAscii(Tools.bcd2Str(bitValue));

                    // TAG
                    int tag = (bitNum & 7) | ((bitNum & 56) << 1);

                    byte[] tagByte = Tools.str2Bcd(Integer.toHexString(tag));
                    int len = valueStr.length();
                    byte[] lenByte;
                    byte[] valueByte = new byte[200];

                    if (varyField2.contains(bitNum)) {
                        lenByte = Tools.str2Bcd(Integer.toHexString(((len + 1) / 2) + 1));
                        valueByte = Utils.concat(lenByte, Tools.str2Bcd(String.valueOf(len)));
                        valueByte = Utils.concat(valueByte, Tools.str2Bcd2(valueStr));
                    } else if (varyField3.contains(bitNum)) {
                        lenByte = Tools.str2Bcd(Integer.toHexString(((len + 1) / 2) + 2));
                        if (len < 100) {
                            lenByte = Utils.concat(Tools.str2Bcd(Integer.toHexString(0)), lenByte);
                        }
                        valueByte = Utils.concat(lenByte, Tools.str2Bcd(valueStr));
                    } else if (!varyField2.contains(bitNum) && !varyField3.contains(bitNum)) {
                        if (fixField.contains(bitNum)) {
                            lenByte = Tools.str2Bcd(Integer.toHexString(len));
                            valueByte = Utils.concat(lenByte, Tools.string2Bytes(valueStr));
                        } else {
                            lenByte = Tools.str2Bcd(Integer.toHexString((len + 1) / 2));
                            valueByte = Utils.concat(lenByte, Tools.str2Bcd(valueStr));
                        }
                    }

                    byte[] result = Utils.concat(tagByte, valueByte);
                    TLEFields = Utils.concat(TLEFields, result);
                }
            }

            bitMask >>= 1;
            if (bitMask == 0) {
                bitMask = 0x80;
                index++;
            }
        }
    }


    protected byte[] RspUnpackTLEFields(byte[] pData) throws Iso8583Exception {
        int bitNum, index;
        int bitMask;
        byte[] bitValue;

        byte[] BitMap = new byte[LEN_BYTE_8583_BITMAP];
        System.arraycopy(pData, LEN_HEADER + LEN_BYTE_8583_MTI, BitMap, 0, LEN_BYTE_8583_BITMAP);

        HashMap<String, byte[]> map;

        map = iso8583.unpack(pData, true);

        TLEFields = null;

        entity.resetAllFieldsValue();

        byte[] header = map.get("h");
        entity.setFieldValue("h", header);

        byte[] msgtype = map.get("m");
        entity.setFieldValue("m", msgtype);

        bitMask = 0x80;
        index = 0;
        for (bitNum = 1; bitNum <= 64; bitNum++) {
            if ((BitMap[index] & bitMask) > 0) {
                bitValue = map.get(String.valueOf(bitNum));
                if (bitNum == 57) {
                    TLEFields = Utils.concat(TLEFields, bitValue);
                } else {
                    entity.setFieldValue(String.valueOf(bitNum), bitValue);
                }
            }

            bitMask >>= 1;
            if (bitMask == 0) {
                bitMask = 0x80;
                index++;
            }
        }

        byte[] result = iso8583.pack();

        return result;
    }

    protected void RepackTLEFields(byte[] pData) throws Iso8583Exception {
        int bitNum, bitMask, index;
        byte[] bitValue;

        byte[] BitMap = new byte[LEN_BYTE_8583_BITMAP];
        System.arraycopy(pData, LEN_HEADER + LEN_BYTE_8583_MTI, BitMap, 0, LEN_BYTE_8583_BITMAP);

        bitMask = 0x80;
        index = 0;
        for (bitNum = 1; bitNum <= 64; bitNum++) {
            if (bitNum == 57) {
                setBitData("57", TLEEncField);
            } else if ((BitMap[index] & bitMask) > 0) {
                if (UserParam.encField.contains(bitNum)) {
                    entity.resetFieldValue(String.valueOf(bitNum));
                }
            }

            bitMask >>= 1;
            if (bitMask == 0) {
                bitMask = 0x80;
                index++;
            }
        }
    }


    protected byte[] packedTLE(byte[] packData) throws Iso8583Exception {

        int cycle, icnt, icnt2, headlen, keyID, index, TLEDataLen, TLEEncLen;
        byte[] TLEEncData, eData;

        //Set MAC bit map 64
        packData[LEN_HEADER + LEN_BYTE_8583_MTI + (LEN_BYTE_8583_BITMAP - 1)] |= 0x01;

        byte[] pData = new byte[packData.length - LEN_HEADER];

        System.arraycopy(packData, LEN_HEADER, pData, 0, packData.length - LEN_HEADER);

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        keyID = acq.getKeyId();

        PedManager ped = FinancialApplication.getPedInstance();
        if (acq.getTWK() == null)
            return "".getBytes();

        //byte[] mac = EncUtils.SHA_X919(pData, keyID);
        byte[] mac = EncUtils.SHA_X919(pData, acq);
        Arrays.fill(mac, mac.length / 2, mac.length, (byte) 0);
        packData = Utils.concat(packData, mac);
        setBitData("64", mac);

        UnpackTLEFields(packData);

        if (TLEFields == null || TLEFields.length == 0)
            return "".getBytes();

        //todo: need to check UP acquirer to use the 2nd tag
        Acquirer curAcq = FinancialApplication.getAcqManager().getCurAcq();
        String headStr = "HTLE0400000000000202000010000000000000000";
        if (Constants.ACQ_UP.equals(curAcq.getName()))
            headStr = "HTLE04000000000002020000100000000P0000000";

        byte[] head = headStr.getBytes();

        byte[] nii;
        if (Constants.ACQ_BAY_INSTALLMENT.equals(curAcq.getName())
                || Constants.ACQ_AYCAP_T1C_HOST.equals(curAcq.getName())) {//todo improvement
            nii = UserParam.KMSAQ02.getBytes();
        } else {
            nii = UserParam.KMSAQ01.getBytes();
        }

        byte[] tid = null;
        if (acq.getTleBankName().equals(Constants.ACQ_KBANK)) {
            String masterTID = MultiMerchantUtils.Companion.getMasterAcquirerTID(Constants.ACQ_KBANK);
            if (masterTID==null) { return "".getBytes(); }
            tid = masterTID.getBytes();
        } else {
            tid = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_BAY_INSTALLMENT).getTerminalId().getBytes();
        }

        //byte[] tid = FinancialApplication.getAcqManager().getCurAcq().getTerminalId().getBytes();
        Log.d(SplashActivity.TAG, "\t\t\t\tTWK-ID = " + acq.getTWK());
        Log.d(SplashActivity.TAG, "\t\t\t\tKEY-ID = " + acq.getKeyId());
        byte[] twkID = Tools.string2Bytes(acq.getTWK());

        if (nii == null || tid == null || twkID == null)
            return "".getBytes();

        System.arraycopy(nii, 0, head, 6, nii.length);
        System.arraycopy(tid, 0, head, 9, tid.length);
        System.arraycopy(twkID, 0, head, 21, twkID.length);

        TLEDataLen = TLEFields.length;

        byte[] lenTLEByte = new byte[2];
        if (TLEDataLen < 100) {
            lenTLEByte = Tools.str2Bcd("00" + String.valueOf(TLEDataLen));
        } else {
            lenTLEByte = Tools.str2Bcd(String.valueOf(TLEDataLen));
        }

        head[29] = 0x00;
        head[30] = 0x00;
        head[31] = lenTLEByte[0];
        head[32] = lenTLEByte[1];

        cycle = TLEDataLen / 8;
        if (TLEDataLen % 8 > 0) {
            cycle++;
        }

        TLEEncLen = cycle * 8;

        TLEEncData = new byte[TLEEncLen + 8];
        System.arraycopy(TLEFields, 0, TLEEncData, 0, TLEFields.length);

        index = 0;

        for (icnt = 0; icnt < cycle; icnt++) {
            eData = ped.calcDes(PedManager.TRI_ENCRYPT, (byte) (UserParam.getDEKID(acq)), Arrays.copyOfRange(TLEEncData, index, index + 8));
            System.arraycopy(eData, 0, TLEEncData, index, 8);

            for (icnt2 = 0; icnt2 < 8; icnt2++) {
                TLEEncData[index + 8 + icnt2] ^= TLEEncData[index + icnt2];
            }
            index = index + 8;
        }

        int lenTLEEnc = TLE_HEADER_LEN + TLEEncLen;

        TLEEncField = new byte[lenTLEEnc];

        System.arraycopy(head, 0, TLEEncField, 0, TLE_HEADER_LEN);
        System.arraycopy(TLEEncData, 0, TLEEncField, TLE_HEADER_LEN, TLEEncLen);

        RepackTLEFields(packData);

        byte[] result = iso8583.pack();

        //update turn off BITMAP for encrypt flds
        int field, bitMapByte, bitMark;
        for (icnt = 0; icnt < UserParam.encField.size(); icnt++) {
            field = UserParam.encField.get(icnt);

            bitMapByte = field / 8;
            bitMark = 0x80 >> ((field % 8) - 1);
            result[LEN_HEADER + LEN_BYTE_8583_MTI + bitMapByte] &= (~bitMark);
        }

        //Set MAC bit map 57
        result[LEN_HEADER + LEN_BYTE_8583_MTI + (LEN_BYTE_8583_BITMAP - 1)] |= 0x81;

        return result;
    }


    public void saveArray(String[] array, String arrayName, Context mContext) {
        if (mContext==null) {mContext = FinancialApplication.getApp().getApplicationContext();}
        SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName + "_size", array.length);
        for (int i = 0; i < array.length; i++)
            editor.putString(arrayName + "_" + i, array[i]);
        editor.commit();
    }

    private Context getCurrentContext() {
        return ActivityStack.getInstance().top();
    }

    protected void packControlLimit(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("24", UserParam.CTRL_LIMIT_NII);


    }
}
