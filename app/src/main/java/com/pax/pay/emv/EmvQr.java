/*
 * ============================================================================
 * COPYRIGHT
 *               Pax CORPORATION PROPRIETARY INFORMATION
 *    This software is supplied under the terms of a license agreement or
 *    nondisclosure agreement with Pax Corporation and may not be copied
 *    or disclosed except in accordance with the terms in that agreement.
 *       Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 *  Module Date: 2017-8-7 4:54
 *  Module Author: liliang
 *  Description:
 *  ============================================================================
 */

package com.pax.pay.emv;

import android.util.ArrayMap;
import android.util.Base64;
import th.co.bkkps.utils.Log;

import com.pax.abl.utils.TrackUtils;
import com.pax.device.Device;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.ITlv;
import com.pax.gl.pack.exception.TlvException;
import com.pax.glwrapper.convert.IConvert;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import java.lang.reflect.Field;

public class EmvQr {

    public static final String TAG = "EmvQr";

    private static final String AID_DEBIT_APP = "A000000333010101";
    private static final String AID_CREDIT_APP = "A000000333010102";
    private static final String AID_QCREDIT_APP = "A000000333010103";

    private String mAid;
    private String mTrackData;
    private String mPan;
    private String mExpDate;
    private String mCardSeqNum;
    private String mIccData;

    private EmvQr() {

    }

    public static EmvQr decodeEmvQr(TransData transData, String emvQr) {
        if (emvQr == null) {
            return null;
        }

        try {
            byte[] bytes = Base64.decode(emvQr, Base64.DEFAULT);
            if (bytes == null) {
                return null;
            }

            Log.d(TAG, Tools.bcd2Str(bytes));

            return decodeEmvQrBcd(transData, bytes);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }

    private static EmvQr decodeEmvQrBcd(TransData transData, byte[] emvQr) {
        if (emvQr == null || emvQr.length == 0) {
            return null;
        }

        try {
            ITlv tlv = FinancialApplication.getPacker().getTlv();
            ITlv.ITlvDataObjList objList = tlv.unpack(emvQr);
            if (objList == null) {
                return null;
            }

            byte[] bytes = objList.getValueByTag(0x61);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            objList = tlv.unpack(bytes);

            EmvQr emvQrObj = new EmvQr();
            bytes = objList.getValueByTag(0x4F);
            emvQrObj.mAid = Utils.bcd2Str(bytes);

            bytes = objList.getValueByTag(TagsTable.TRACK2);
            emvQrObj.mTrackData = Utils.bcd2Str(bytes);
            if (emvQrObj.mTrackData.endsWith("F")) {
                emvQrObj.mTrackData = emvQrObj.mTrackData.substring(0, emvQrObj.mTrackData.length
                        () - 1);
            }
            emvQrObj.mPan = TrackUtils.getPan(emvQrObj.mTrackData);
            emvQrObj.mExpDate = TrackUtils.getExpDate(emvQrObj.mTrackData);

            bytes = objList.getValueByTag(0x5F34);
            emvQrObj.mCardSeqNum = Utils.bcd2Str(bytes);

            // CouponNum 0x9F60

            bytes = objList.getValueByTag(0x63);
            emvQrObj.mIccData = createIccData(transData, bytes);

            return emvQrObj;

        } catch (Exception e) {
            Log.e(TAG, "Failed to decode EMV QR.", e);
        }
        return null;
    }

    private static String createIccData(TransData transData, byte[] iccBytes) {
        if (iccBytes == null) {
            throw new IllegalArgumentException("iccBytes is null.");
        }

        Config cfg = Component.genCommonEmvConfig();

        ArrayMap<Integer, String> map = new ArrayMap<>(9);
        byte[] random = new byte[4];
        DeviceManager.getInstance().getRand(random, 4);
        map.put(0x9F37, Utils.bcd2Str(random));
        map.put(0x95, "0000000800");
        map.put(TagsTable.TRANS_DATE, Device.getTime("yyMMdd"));
        map.put(0x9C, "00");
        map.put(TagsTable.AMOUNT, transData.getAmount());
        map.put(TagsTable.CURRENCY_CODE, cfg.getTransCurrCode());
        map.put(TagsTable.COUNTRY_CODE, cfg.getCountryCode());
        map.put(TagsTable.AMOUNT_OTHER, "000000000000");
        map.put(TagsTable.TERMINAL_CAPABILITY, "E0E8C0");

        ITlv tlv = FinancialApplication.getPacker().getTlv();
        ITlv.ITlvDataObjList iccDataList;
        try {
            iccDataList = tlv.unpack(iccBytes);
            for (Integer tag : map.keySet()) {
                if (iccDataList.getByTag(tag) == null) {
                    ITlv.ITlvDataObj obj = tlv.createTlvDataObject();
                    obj.setTag(tag);
                    byte[] value = FinancialApplication.getConvert().strToBcd(map.get(tag),
                            IConvert.EPaddingPosition.PADDING_RIGHT);
                    obj.setValue(value);
                    iccDataList.addDataObj(obj);
                }
            }

            byte[] bytes = tlv.pack(iccDataList);
            return Utils.bcd2Str(bytes);
        } catch (TlvException e) {
            Log.e(TAG, "Failed to decode icc data from EMV QR.", e);
        }

        return null;
    }

    public boolean isUpiAid() {
        if (mAid == null) {
            return false;
        }

        switch (mAid) {
            case AID_DEBIT_APP:
            case AID_CREDIT_APP:
            case AID_QCREDIT_APP:
                return true;
            default:
                return false;
        }
    }

    public String getAid() {
        return mAid;
    }

    public String getTrackData() {
        return mTrackData;
    }

    public String getPan() {
        return mPan;
    }

    public String getExpireDate() {
        return mExpDate;
    }

    public String getCardSeqNum() {
        return mCardSeqNum;
    }

    public String getIccData() {
        return mIccData;
    }

    @Override
    public String toString() {
        Field[] fields = EmvQr.class.getDeclaredFields();
        StringBuilder stringBuilder = new StringBuilder();
        for (Field field : fields) {
            try {
                Object value = field.get(this);
                stringBuilder.append("[" + field.getName() + ":" + value + "]\n");
                Log.d(TAG, "[" + field.getName() + ":" + value + "]");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "", e);
            }
        }
        return stringBuilder.toString();
    }
}
