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

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.appstore.DownloadManager;
import com.pax.device.UserParam;
import com.pax.edc.opensdk.TransResult;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.HashMap;

import th.co.bkkps.utils.Log;

public class PackSaleVoid extends PackIso8583 {

    private boolean isAmex;
    private boolean isRefund;

    public PackSaleVoid(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
//            setCommonData(transData);
//            setBitData60(transData);
            setMandatoryData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            }
            else
                return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                        || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName());
        isRefund = transData.getOrigTransType() == ETransType.REFUND;

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

        // [2]主账号
        setBitData2(transData);
        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);

        setBitData12(transData);
        setBitData13(transData);
        // [14]有效期
        setBitData14(transData);

        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData25(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();

       /* if (enterMode == TransData.EnterMode.MANUAL) {
            // 手工输入
             // [2]主账号
               setBitData2(transData);
            //[14]有效期
            //setBitData14(transData);

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {
            // 刷卡

            // [35]二磁道,[36]三磁道
            //setBitData35(transData);
            //setBitData36(transData);

            //[54]tip amount  by lixc
            //setBitData54(transData);

        } else */if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
            if (!isAmex) {//AMEX, not present
                // [23]卡序列号
                setBitData23(transData);
            }

            // [35]二磁道
//           setBitData35(transData);

            if (isRefund || (!Constants.ISSUER_TBA.equals(transData.getIssuer().getName()) && !isAmex)) {
                // field 55 ICC
                setBitData55(transData);
            }
        }

        setBitData37(transData);

        if (!isAmex) {//AMEX, not present
            setBitData38(transData);
        }

        // field 41 终端号
        setBitData41(transData);

        // field 42 商户号
        setBitData42(transData);

        setBitData62(transData);
        setBitData63(transData);

        if (transData.isDccRequired()) {
            setBitData6(transData);
            setBitData10(transData);
            setBitData51(transData);
        }
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if (isRefund && !isAmex) {
            setBitData("3", String.valueOf(Constants.ACQ_UP.equals(transData.getAcquirer().getName()) ? 220000 : 224000));
        } else if (transData.getOrigTransType() == ETransType.OFFLINE_TRANS_SEND || transData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT) {
            setBitData("3", "024000");
        } else {
            super.setBitData3(transData);
        }
    }

    @Override
    protected void setBitData4(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getOrigTransState() == TransData.ETransStatus.ADJUSTED
                && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENT) {
            setBitData("4", transData.getOrigAmount());
        } else {
            super.setBitData4(transData);
        }
    }

    @Override
    protected void setBitData6(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getOrigTransState() == TransData.ETransStatus.ADJUSTED
                && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENT) {
            setBitData("6", transData.getOrigDccAmount());
        } else {
            super.setBitData6(transData);
        }
    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getOrigRefNo());
    }

    /**
     * 设置撤销类交易 设置域
     * <p>
     * field 2, field 4, field 14,field 22, field 23,field 26,field 35,field 36,
     * <p>
     * field 37,field 38, field 49,field 53,field 61
     *
     * @param transData
     * @return
     */
//    @Override
//    protected void setCommonData(@NonNull TransData transData) throws Iso8583Exception {
//        setMandatoryData(transData);
//        super.setCommonData(transData);
//
//        // [37]原参考号
//        setBitData37(transData);
//
//        // [38]原授权码
//        setBitData38(transData);
//
//        // field 61
//        setBitData61(transData);
//
//    }
//
//    @Override
//    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
//        String f60 = Component.getPaddedNumber(transData.getBatchNo(), 6); // f60.2
//        f60 += "600";
//        setBitData("60", f60);
//    }
//
//    @Override
//    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
//        String f61 = "";
//        String temp = Component.getPaddedNumber(transData.getOrigBatchNo(), 6);
//        if (temp != null && !temp.isEmpty()) {
//            f61 += temp;
//        } else {
//            f61 += "000000";
//        }
//        temp = Component.getPaddedNumber(transData.getOrigTransNo(), 6);
//        if (temp != null && !temp.isEmpty()) {
//            f61 += temp;
//        } else {
//            f61 += "000000";
//        }
//        setBitData("61", f61);
//    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        boolean isKerryAPI = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KERRY_API);
        String tempBranch = transData.getBranchID();

        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            setBitData("63", PackSale.setBitData63Ref1Ref2(transData));
        }
        else if (isKerryAPI && tempBranch != null) {
            setBitData("63",transData.getBranchID());
        }
    }

    @Override
    protected int checkRecvData(@NonNull HashMap<String, byte[]> map, @NonNull TransData transData, boolean isCheckAmt) {
        if (transData.getOrigTransState() == TransData.ETransStatus.ADJUSTED
                && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENT) {
            byte[] data = map.get("4");
            if (data != null && data.length > 0) {
                String temp = new String(data);
                if (Utils.parseLongSafe(temp, 0) != Utils.parseLongSafe(transData.getOrigAmount(), 0)) {
                    return TransResult.ERR_TRANS_AMT;
                }
            }
            return TransResult.SUCC;
        } else {
            return super.checkRecvData(map, transData, isCheckAmt);
        }
    }
}
