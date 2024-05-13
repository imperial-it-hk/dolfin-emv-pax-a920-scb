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
package com.pax.pay.emv;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.eemv.IEmvBase;
import com.pax.eemv.entity.AidParam;
import com.pax.gl.pack.ITlv;
import com.pax.gl.pack.exception.TlvException;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.emv.api.EMVCallback;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmvTags {

    private static final String TAG = "EmvTags";
    /*not soterd
    static final int[] SALE = {0x9F26, 0x9F27, 0x9F10, 0x9F37, 0x9F36, 0x95, 0x9A, 0x9B, 0x9C, 0x9F02, 0x5F2A,
            0x5F34,0x82, 0x9F1A, 0x9F03, 0x9F33, 0x9F34, 0x9F35, 0x9F1E, 0x84, 0x9F09, 0x9F41, 0x9F63, 0x9F53};*/
    static final int[] SALE = {0x5F2A, 0x5F34, 0x82, 0x84, 0x95, 0x9A, 0x9B, 0x9C, 0x9F02, 0x9F03,  0x9F09, 0x9F10, 0x9F1A,
            0x9F1E, 0x9F26, 0x9F27, 0x9F33, 0x9F34, 0x9F35, 0x9F36, 0x9F37,  0x9F41, 0x9F53, 0x9F63, 0xDF31};
    static final int[] SALE_DINERS = {0x5F2A, 0x5F34, 0x82, 0x84, 0x95, 0x9A, 0x9B, 0x9C, 0x9F02, 0x9F03,  0x9F09, 0x9F10, 0x9F1A,
            0x9F1E, 0x9F26, 0x9F27, 0x9F33, 0x9F34, 0x9F35, 0x9F36, 0x9F37,  0x9F41, 0x9F53, 0x9F63};
    static final int[] PBOC_OFFLINE = {0x9F26, 0x9F27, 0x9F10, 0x9F37, 0x9F36, 0x95, 0x9A, 0x9C, 0x9F02,
            0x5F2A, 0x82, 0x9F1A, 0x9F03, 0x9F33, 0x9F1E, 0x84, 0x9F09, 0x9F41, 0x9F34, 0x9F35, 0x9F63, 0x8A};
    static final int[] AUTH = {0x9F26, 0x9F27, 0x9F10, 0x9F37, 0x9F36, 0x95, 0x9A, 0x9C, 0x9F02, 0x5F2A,
            0x82, 0x9F1A, 0x9F03, 0x9F33, 0x9F34, 0x9F35, 0x9F1E, 0x84, 0x9F09, 0x9F41, 0x9F63};

    static final byte[] SALE_AMEX_HEADER = {(byte) 0xC1, (byte) 0xC7, (byte) 0xD5, (byte) 0xE2, (byte) 0x00, (byte) 0x01};
    static final EmvDynamicTags[] SALE_AMEX = {
            new EmvDynamicTags(0x9F26, "DE55_LEN_FIXED", 8),
            new EmvDynamicTags(0x9F10, "DE55_LEN_VAR1", 33),
            new EmvDynamicTags(0x9F37, "DE55_LEN_FIXED", 4),
            new EmvDynamicTags(0x9F36, "DE55_LEN_FIXED", 2),
            new EmvDynamicTags(0x95, "DE55_LEN_FIXED", 5),
            new EmvDynamicTags(0x9A, "DE55_LEN_FIXED", 3),
            new EmvDynamicTags(0x9C, "DE55_LEN_FIXED", 1),
            new EmvDynamicTags(0x9F02, "DE55_LEN_FIXED", 6),
            new EmvDynamicTags(0x5F2A, "DE55_LEN_FIXED", 2),
            new EmvDynamicTags(0x9F1A, "DE55_LEN_FIXED", 2),
            new EmvDynamicTags(0x82, "DE55_LEN_FIXED", 2),
            new EmvDynamicTags(0x9F03, "DE55_LEN_FIXED", 6),
            new EmvDynamicTags(0x5F34, "DE55_LEN_FIXED", 1),
            new EmvDynamicTags(0x9F27, "DE55_LEN_FIXED", 1),
            new EmvDynamicTags(0x9F06, "DE55_LEN_VAR1", 16),
            new EmvDynamicTags(0x9F09, "DE55_LEN_FIXED", 2),
            new EmvDynamicTags(0x9F34, "DE55_LEN_FIXED", 3),
            new EmvDynamicTags(0x9F0E, "DE55_LEN_FIXED", 5),
            new EmvDynamicTags(0x9F0F, "DE55_LEN_FIXED", 5),
            new EmvDynamicTags(0x9F0D, "DE55_LEN_FIXED", 5)
    };
    /**
     * reversal
     */
    static final int[] DUP = {0x95, 0x9F10, 0x9F1E, 0xDF31};
    static final int[] DUP_DINERS = {0x5F2A, 0x5F34, 0x82, 0x84, 0x95, 0x9A, 0x9B, 0x9C, 0x9F02, 0x9F03,  0x9F09, 0x9F10, 0x9F1A,
            0x9F1E, 0x9F26, 0x9F27, 0x9F33, 0x9F34, 0x9F35, 0x9F36, 0x9F37,  0x9F41, 0x9F53, 0x9F63};

    /**
     * 交易承兑但卡片拒绝时发起的冲正
     */
    static final int[] POS_ACCEPT_DUP = {0x95, 0x9F10, 0x9F1E, 0x9F36, 0xDF31};

    private EmvTags() {

    }

    /**
     * generate field 55 by transaction type
     *
     * @param transType type
     * @param isDup     is reversal
     * @return data of field 55
     */
    @NonNull
    public static byte[] getF55(IEmvBase emv, ETransType transType, boolean isDup , String pan) {
        byte[] aid = emv.getTlv(0x4F);
        String strAid = (aid != null && aid.length > 0) ? Utils.bcd2Str(aid) : "";

        switch (transType) {
            case OFFLINE_TRANS_SEND:
            case GET_T1C_MEMBER_ID:
            case KBANK_SMART_PAY:
            case REFUND:
            case SALE:
                if (strAid.contains(Constants.DINERS_AID_PREFIX)) {
                    if (isDup) {
                        return getValueList(emv, DUP_DINERS, pan);
                    }

                    return getValueList(emv, SALE_DINERS, pan);
                } else if (strAid.contains(Constants.AMEX_AID_PREFIX)) {
                    if (isDup) {
                        return getValueList(emv, (EmvDynamicTags[]) null, pan);
                    }

                    return getValueList(emv, SALE_AMEX, pan);
                } else {
                    if (isDup) {
                        return getValueList(emv, DUP, pan);
                    }

                    return getValueList(emv, SALE, pan);
                }
            case PREAUTHORIZATION:
                if (strAid.contains(Constants.AMEX_AID_PREFIX)) {
                    if (isDup) {
                        return getValueList(emv, (EmvDynamicTags[]) null, pan);
                    }
                    return getValueList(emv, SALE_AMEX, pan);
                } else {
                    if (isDup) {
                        return getValueList(emv, DUP, pan);
                    }
                    return getValueList(emv, SALE, pan);
                }
            case PREAUTH:
                if (strAid.contains(Constants.AMEX_AID_PREFIX)) {
                    if (isDup) {
                        return getValueList(emv, (EmvDynamicTags[]) null, pan);
                    }
                    return getValueList(emv, SALE_AMEX, pan);
                } else {
                    if (isDup) {
                        return getValueList(emv, DUP, pan);
                    }
                    return getValueList(emv, AUTH, pan);
                }
            case AMEX_INSTALMENT:
                if (isDup) {
                    return getValueList(emv, (EmvDynamicTags[]) null, pan);
                }
                return getValueList(emv, SALE_AMEX, pan);
            default:
                break;
        }
        return "".getBytes();
    }

    @NonNull
    public static byte[] getF55forPosAccpDup(IEmvBase emv, String pan) {
        return getValueList(emv, POS_ACCEPT_DUP, pan);
    }

    @NonNull
    private static byte[] getValueList(IEmvBase emv, int[] tags, String pan) {
        if (tags == null || tags.length == 0) {
            return "".getBytes();
        }

        ITlv tlv = FinancialApplication.getPacker().getTlv();
        ITlv.ITlvDataObjList tlvList = tlv.createTlvDataObjectList();
        for (int tag : tags) {
            byte[] value = emv.getTlv(tag);
            //Custom Tag for UP
            if(tag == 0xdf31){
                List<AidParam> aidParams = emv.getAidParamList();
                byte[] aid = emv.getTlv(0x4F);
                String acq = null;
                for (AidParam a : aidParams) {
                    if(aid!=null && Utils.bcd2Str(aid).contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                        acq =  a.getAcqHostName();
                    }
                }

                if(acq != null && acq.equalsIgnoreCase(Constants.ACQ_UP)){
                    ByteArray scriptResult = new ByteArray();
                    int ret = EMVCallback.EMVGetScriptResult(scriptResult);
                    if(ret == RetCode.EMV_OK){
                        value = new byte[scriptResult.length];
                        System.arraycopy(scriptResult.data, 0, value, 0, scriptResult.length);
                    }else{
                        continue;
                    }
                }else{
                    continue;
                }
            }
            if (value == null || value.length == 0) {
                if (tag == 0x9f03) {
                    value = new byte[6];
                } else  if (tag == 0x9f53 && pan != null && pan.startsWith("5")) {
                    //add 0x9f53 for MASTER
                    value = Utils.str2Bcd("52");
                }else {
                    continue;
                }
            }
            try {
                ITlv.ITlvDataObj obj = tlv.createTlvDataObject();
                obj.setTag(tag);
                obj.setValue(value);
                tlvList.addDataObj(obj);
            } catch (Exception e) {
                Log.i(TAG, "", e);
            }
        }

        try {
            return tlv.pack(tlvList);
        } catch (TlvException e) {
            Log.e(TAG, "", e);
        }

        return "".getBytes();

    }

    @NonNull
    private static byte[] getValueList(IEmvBase emv, EmvDynamicTags[] tags, String pan) {
        if (tags == null || tags.length == 0) {
            return "".getBytes();
        }
        List<EmvDynamicTags> tlvList = new ArrayList<>();
        for (EmvDynamicTags tag : tags) {
            byte[] value = emv.getTlv(tag.getTag());
            if (value == null || value.length == 0) {
                if (tag.getTag() == 0x9f03) {
                    value = new byte[6];
                } else  if (tag.getTag() == 0x9f53 && pan != null && pan.startsWith("5")) {
                    //add 0x9f53 for MASTER
                    value = Utils.str2Bcd("52");
                } else{
                    continue;
                }
            }
            try {
                tag.setValue(value);
                tlvList.add(tag);
            } catch (Exception e) {
                Log.i(TAG, "", e);
            }
        }

        ByteBuffer bufPack;
        (bufPack = ByteBuffer.allocate(102400)).clear();
        bufPack.order(ByteOrder.BIG_ENDIAN);
        for (int t = 0; t < tlvList.size(); ++t) {
            EmvDynamicTags data = tlvList.get(t);

            byte[] val;
            if ((val = data.getValue()) == null) {
                val = new byte[0];
            }
            byte[] len;
            if ("DE55_LEN_VAR1".equals(data.getTypeLength())) {
                (len = new byte[1])[0] = (byte) data.getValue().length;
                bufPack.put(len);
            }
            bufPack.put(val);
        }
        bufPack.flip();
        byte[] tlvPack = new byte[bufPack.limit()];
        bufPack.get(tlvPack);

        ByteArrayOutputStream packStream = new ByteArrayOutputStream();
        try {
            packStream.write(SALE_AMEX_HEADER);
            packStream.write(tlvPack);
            return packStream.toByteArray();
        } catch (IOException e) {
            Log.i(TAG, "", e);
        }
        return "".getBytes();
    }

    protected static ITlv.ITlvDataObjList unpackRspF55Amex(byte[] rsp55) throws Exception {
        int lenRsp = rsp55.length;
        if (lenRsp < 6) {
            throw new Exception("AMEX Invalid Bit55");
        }

        byte[] respHeader = new byte[6];
        System.arraycopy(rsp55, 0, respHeader, 0, 6);
        if (!Arrays.equals(SALE_AMEX_HEADER, respHeader)) {
            throw new Exception("AMEX Bit55 Header mismatch");
        }

        ITlv tlv = FinancialApplication.getPacker().getTlv();
        ITlv.ITlvDataObjList tlvList = tlv.createTlvDataObjectList();

        try {
            int idx = 6;
            while (idx < lenRsp) {
                ITlv.ITlvDataObj tlvData = tlv.createTlvDataObject();
                int start, end, next;
                if (idx == 6) {
                    next = (start = idx + 1) + (end = rsp55[idx] & 127);
                    byte[] value = new byte[end];
                    System.arraycopy(rsp55, start, value, 0, end);
                    tlvData.setTag(0x91);
                    tlvData.setValue(value);
                } else {
                    byte[] tag = new byte[1];
                    System.arraycopy(rsp55, idx + 1, tag, 0, 1);
                    next = (start = idx + 3) + (end = rsp55[idx + 2] & 127);
                    byte[] value = new byte[end];
                    System.arraycopy(rsp55, start, value, 0, end);
                    tlvData.setTag(tag);
                    tlvData.setValue(value);
                }
                tlvList.addDataObj(tlvData);
                idx = next;
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
            throw new Exception("AMEX Unpack Error Bit55");
        }
        return tlvList;
    }
}
