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

public class EmvSP200 {

    public static final String TAG = "EmvSP200";

    private int iAidLen;
    private int iClssType;
    private int iClssMode;
    private int iIccDataLen;
    private int iResult;

    private String mAid;
    private String mPanSeqNo;
    private String mPan;
    private String mEmvPan;
    private String mIccData;
    private String mTrackData;
    private String mExpDate;
    private String mAppLabel;
    private String mAppPreferName;
    private String mTvr;
    private String mAppCrypto;
    private String mHolderName;
    private String mCRC;

    private boolean bSignFree;
    private boolean bPinFree;

    public EmvSP200() {
    }

    public String getAid() { return mAid; }
    public void setAid(String mAid) { this.mAid = mAid; }

    public int getAidLen() { return iAidLen; }
    public void setAidLen(int iAidLen) { this.iAidLen = iAidLen; }

    public int getClssType() { return iClssType; }
    public void setClssType(int iClssType) { this.iClssType = iClssType; }

    public int getClssMode() { return iClssMode; }
    public void setClssMode(int iClssMode) { this.iClssMode = iClssMode; }

    public String getPanSeqNo() { return mPanSeqNo; }
    public void setPanSeqNo(String mPanSeqNo) { this.mPanSeqNo = mPanSeqNo; }

    public String getPan() { return mPan; }
    public void setPan(String mPan ) { this.mPan = mPan; }

    public String getEmvPan() { return mEmvPan; }
    public void setEmvPan(String mEmvPan) { this.mEmvPan = mEmvPan; }

    public String getIccData() { return mIccData; }
    public void setIccData(String mIccData) { this.mIccData = mIccData; }

    public int getIccDataLen() { return iIccDataLen; }
    public void setIccDataLen(int iIccDataLen) { this.iIccDataLen = iIccDataLen; }

    public String getTrackData() { return mTrackData; }
    public void setTrackData(String mTrackData) { this.mTrackData = mTrackData; }

    public String getExpDate() { return mExpDate; }
    public void setExpDate(String mExpDate ) { this.mExpDate = mExpDate; }

    public String getAppLabel() { return mAppLabel; }
    public void setAppLabel(String mAppLabel) { this.mAppLabel = mAppLabel; }

    public String getAppPreferName() { return mAppPreferName; }
    public void setAppPreferName(String mAppPreferName) { this.mAppPreferName = mAppPreferName; }

    public String getTvr() { return mTvr; }
    public void setTvr(String mTvr) { this.mTvr = mTvr; }

    public String getAppCrypto() { return mAppCrypto; }
    public void setAppCrypto(String mAppCrypto) { this.mAppCrypto = mAppCrypto; }

    public String getHolderName() { return mHolderName; }
    public void setHolderName(String mHolderName) { this.mHolderName = mHolderName; }

    public boolean isSignFree() { return bSignFree; }
    public void setSignFree(boolean bSignFree) { this.bSignFree = bSignFree; }

    public boolean isPinFree() { return bPinFree; }
    public void setPinFree(boolean bPinFree) { this.bPinFree = bPinFree; }

    public int getiResult() { return iResult; }
    public void setiResult(int iResult) { this.iResult = iResult; }

    public String getCRC() { return mCRC; }
    public void setCRC(String mCRC) { this.mCRC = mCRC; }
}
