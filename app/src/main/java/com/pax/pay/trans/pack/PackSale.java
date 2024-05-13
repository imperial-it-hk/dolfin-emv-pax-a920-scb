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

import th.co.bkkps.utils.Log;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.appstore.DownloadManager;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PackSale extends PackIsoBase {

    public PackSale(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else
                return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        setMandatoryData(transData);
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        boolean isReferral = false;
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());

        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else if (transData.getReferralStatus() == TransData.ReferralStatus.REFERRED) {
            entity.setFieldValue("m", "0220");
            isReferral = true;
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);
        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData25(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();

        if (enterMode == TransData.EnterMode.MANUAL) {
            setBitData2(transData);

            setBitData14(transData);

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {
            if (!isReferral) {
                setBitData35(transData);
            }

            if (enterMode == TransData.EnterMode.FALLBACK && !isAmex) {
                setBitData36(transData);
            }

            //[54]tip amount  by lixc
//            setBitData54(transData);

        } else if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
            // [2]主账号
//            setBitData2(transData);

            // [14]有效期
//            setBitData14(transData);

            if (!isAmex) {//AMEX, not present
                setBitData23(transData);
            }

            if (!isReferral) {
                setBitData35(transData);

                // field 55 ICC
                setBitData55(transData);
            }
        }

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        // [52]PIN
        setBitData52(transData);

        setBitData62(transData);

        if (isAmex && isReferral) {
            setBitData2(transData);
            setBitData12(transData);
            setBitData14(transData);
            setBitData38(transData);
        }

        setBitData63(transData);

        if (transData.isDccRequired()) {
            setBitData6(transData);
            setBitData10(transData);
            setBitData51(transData);
        }
    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getOrigAuthCode());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        /*if (transData.isVatb()
                && transData.getIssuer().getIssuerBrand().equals(Constants.ISSUER_BRAND_TBA)) {
            setBitData63Vatb(transData);
        }*/
        if (transData.isVatb() && (transData.getIssuer().getIssuerBrand().equals(Constants.ISSUER_BRAND_TBA) ||
                (transData.getVatAmount().equals("0000000000") && transData.getTaxAllowance().equals("0000000000") &&
                new String(transData.getMercUniqueValue()).equals("                    ") &&
                new String(transData.getCampaignType()).equals("000000")))) {//isVatB=true, issuer=TBA or only REF1 and REF2
            setBitData63Vatb(transData);
        }
        else if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            setBitData("63", setBitData63Ref1Ref2(transData));
        }
        else {
            setBitData63Kerry(transData);
        }

    }

    private void setBitData63Kerry(@NonNull TransData transData) throws Iso8583Exception {
        boolean isKerryAPI = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KERRY_API);
        String tempBranch = transData.getBranchID();
        // if no data on D8
        if (isKerryAPI && tempBranch != null) {
            String pdType = "CPAC";
            if (tempBranch.length() < 8) {
                tempBranch = Component.getPaddedStringRight(transData.getBranchID(), 8, ' ');
            } else if (tempBranch.length() > 8) {
                tempBranch = tempBranch.substring(0, 8);
            }
            String branchID = pdType + tempBranch;
            transData.setBranchID(branchID);
            setBitData("63", branchID);
        }
    }
    private enum VATB_DE63_MODE {PLAINT_TEXT_MODE,BYTE_MODE}
    private VATB_DE63_MODE DE63Mode = VATB_DE63_MODE.BYTE_MODE;
    private void setBitData63Vatb(@NonNull TransData transData) throws Iso8583Exception {
        String vatB = null;
        if (transData.getVatAmount().equals("0000000000") && transData.getTaxAllowance().equals("0000000000") &&
                new String(transData.getMercUniqueValue()).equals("                    ") &&
                new String(transData.getCampaignType()).equals("000000")) {
            ByteArrayOutputStream bAOs = new ByteArrayOutputStream();
            try {
                //bAOs.write(new byte[] {0x00, 0x42});
                bAOs.write("11".getBytes());
                bAOs.write(transData.getREF1());
                bAOs.write(transData.getREF2());
            } catch (IOException e) {
                e.printStackTrace();
            }
            setBitData("63", bAOs.toByteArray());
        } else if (new String(transData.getREF1()).isEmpty() && new String(transData.getREF2()).isEmpty()) {
            ByteArrayOutputStream bAOs = new ByteArrayOutputStream();
            try {
                //bAOs.write(new byte[] {0x00, 0x48});
                bAOs.write("12".getBytes());
                bAOs.write(transData.getVatAmount().getBytes());
                bAOs.write(transData.getTaxAllowance().getBytes());
                bAOs.write(transData.getMercUniqueValue());
                bAOs.write(transData.getCampaignType());
            } catch (IOException e) {
                e.printStackTrace();
            }
            setBitData("63", bAOs.toByteArray());
        } else {
            ByteArrayOutputStream bAOs = new ByteArrayOutputStream();
            try {
                //bAOs.write(new byte[] {0x00, (byte)0x88});
                bAOs.write("13".getBytes());
                bAOs.write(transData.getREF1());
                bAOs.write(transData.getREF2());
                bAOs.write(transData.getVatAmount().getBytes());
                bAOs.write(transData.getTaxAllowance().getBytes());
                bAOs.write(transData.getMercUniqueValue());
                bAOs.write(transData.getCampaignType());
            } catch (IOException e) {
                e.printStackTrace();
            }
            setBitData("63", bAOs.toByteArray());
        }
    }

    public static byte[] setBitData63Ref1Ref2(@NonNull TransData transData) {
        if (transData.getSaleReference1() == null) {
            transData.setSaleReference1("");
        }
        if (transData.getSaleReference2() == null) {
            transData.setSaleReference2("");
        }

        String ref1 = Component.getPaddedStringRight(transData.getSaleReference1(), 20, ' ');
        String ref2 = Component.getPaddedStringRight(transData.getSaleReference2(), 20, ' ');

        byte[] f63;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write("11".getBytes(StandardCharsets.UTF_8));
            bos.write(ref1.getBytes(StandardCharsets.UTF_8));
            bos.write(ref2.getBytes(StandardCharsets.UTF_8));
            f63 = bos.toByteArray();
        } catch (IOException e) {
            f63 = ("11" + ref1 + ref2).getBytes(StandardCharsets.UTF_8);
        }
        return f63;
    }
}
