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

import com.pax.abl.utils.TrackUtils;
import com.pax.dal.entity.EPiccType;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.edc.R;
import com.pax.eemv.IEmv;
import com.pax.eemv.IEmvListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;

public class EmvTransProcess {

    private static final String TAG = "EmvTransProcess";

    private static final byte ONLINEPIN_CVM = (byte) 0x80;
    private static final byte SIGNATURE_CVM = 0x40;
    private static final byte CD_CVM = (byte) 0x80;
    private static final byte NO_CVM = 0x00;

    private static final String UNIONPAY_DEBITAID = "A000000333010101";
    private static final String UNIONPAY_CREDITAID = "A000000333010102";
    private static final String UNIONPAY_QUASICREDITAID = "A000000333010103";

    private static IEmv emv;

    private static List<AidParam> aidParamList;

    public EmvTransProcess(IEmv emv) {
        this.emv = emv;
    }

    public CTransResult transProcess(TransData transData, IEmvListener listener) throws EmvException {
        emv.setListener(listener);
        CTransResult result = emv.process(Component.toInputParam(transData));
        Log.i(TAG, "EMV PROC:" + result.toString());
        return result;
    }

    public void readCardProcess(TransData transData, IEmvListener listener) throws EmvException {
        emv.setListener(listener);
        emv.readCardProcess(Component.toInputParam(transData));
    }

    public CTransResult afterReadCardProcess(TransData transData, IEmvListener listener) throws EmvException {
        emv.setListener(listener);
        CTransResult result = emv.afterReadCardProcess(Component.toInputParam(transData));
        Log.i(TAG, "AFTER READ CARD EMV PROC:" + result.toString());
        return result;
    }

    /**
     * EMV init, set aid, capk and emv config
     */
    public void init() {
        try {
            emv.init();
            aidParamList = EmvAid.toAidParams();
            emv.setConfig(genEmvConfig());
        } catch (EmvException e) {
            Log.e(TAG, "", e);
        }
        emv.setAidParamList(aidParamList);
        emv.setCapkList(EmvCapk.toCapk());
    }

    private static Config genEmvConfig() {
        Config cfg = Component.genCommonEmvConfig();
        //cfg.setCapability("E0F0C8");
        cfg.setCapability("E0B0C8");
        cfg.setExCapability("E000F0A001");
        cfg.setTransType((byte) 0);
        return cfg;
    }

    private static void emvOfflineApprovedCase(ETransResult result, IEmv emv, TransData transData) {
        if (ETransType.OFFLINE_TRANS_SEND != transData.getTransType()) {
            try {
                if (transData.getAuthCode() == null) {
                    emv.setTlv(0x8A, "Y1".getBytes());
                    transData.setOrigAuthCode(Utils.getString(R.string.response_Y1_str));
                    transData.setAuthCode(Utils.getString(R.string.response_Y1_str));
                }

            } catch (EmvException e) {
                Log.w(TAG, "", e);
            }
        }
        // set result
        transData.setEmvResult(result);
        // convert field 55 to transData
        String pan = transData.getPan();
        byte[] f55 = EmvTags.getF55(emv, transData.getTransType(), false, pan);
        byte[] f55Dup = EmvTags.getF55(emv, transData.getTransType(), true, pan);

        transData.setSendIccData(Utils.bcd2Str(f55));
        if (f55Dup.length > 0) {
            transData.setDupIccData(Utils.bcd2Str(f55Dup));
        }
    }

    private static void emvOnlineApprovedCase(ETransResult result, IEmv emv, TransData transData) {
        ETransType transType = transData.getTransType();
        String pan = transData.getPan();
        byte[] f55 = EmvTags.getF55(emv, transType, false, pan);
        transData.setSendIccData(Utils.bcd2Str(f55));
        transData.setEmvResult(result);
    }

    private static void emvArqcCase(ETransResult result, IEmv emv, TransData transData) {
        saveCardInfoAndCardSeq(emv, transData);
        transData.setEmvResult(result);

        if (result == ETransResult.ARQC) {
            generateF55AfterARQC(emv, transData);
        }

        try {
            FinancialApplication.getDal().getPicc(EPiccType.INTERNAL).close();
        } catch (PiccDevException e) {
            Log.e(TAG, "", e);
        }
    }

    private static void emvOfflineDeniedCase(ETransResult result, IEmv emv, TransData transData) {
        try {
            emv.setTlv(0x8A, "Z1".getBytes());
        } catch (EmvException e) {
            Log.e(TAG, "", e);
        }
        Component.incStanNo(transData);
        transData.setEmvResult(result);
    }

    private static void emvOnlineDenied(ETransResult result, IEmv emv, TransData transData) {
        //do nothing for now
    }

    private static void emvOnlineApprovedCardDeniedCase(ETransResult result, IEmv emv, TransData transData) {
        String pan = transData.getPan();
        byte[] f55 = EmvTags.getF55forPosAccpDup(emv, pan);
        if (f55.length > 0) {
            TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
            if (dupTransData != null) {
                dupTransData.setDupIccData(Utils.bcd2Str(f55));
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            }

        }
    }

    /**
     * EMV result process
     *
     * @param result    {@link ETransResult}
     * @param transData {@link TransData}
     */
    public static void emvTransResultProcess(ETransResult result, IEmv emv, TransData transData) {
        saveTvrTsi(emv, transData);
        if (result == ETransResult.OFFLINE_APPROVED) {
            emvOfflineApprovedCase(result, emv, transData);
        } else if (result == ETransResult.ONLINE_APPROVED) {
            emvOnlineApprovedCase(result, emv, transData);
        } else if (result == ETransResult.ARQC || result == ETransResult.SIMPLE_FLOW_END) {
            emvArqcCase(result, emv, transData);
        } else if (result == ETransResult.ONLINE_DENIED) {
            emvOnlineDenied(result, emv, transData);
        } else if (result == ETransResult.OFFLINE_DENIED) {
            emvOfflineDeniedCase(result, emv, transData);
        } else if (result == ETransResult.ONLINE_CARD_DENIED) {
            emvOnlineApprovedCardDeniedCase(result, emv, transData);
        }
    }

    /**
     * after ARQC, generate field 55
     *
     * @param transData {@link TransData}
     */
    private static void generateF55AfterARQC(IEmv emv, TransData transData) {
        ETransType transType = transData.getTransType();
        String pan = transData.getPan();
        byte[] f55 = EmvTags.getF55(emv, transType, false, pan);
        transData.setSendIccData(Utils.bcd2Str(f55));

        byte[] arqc = emv.getTlv(0x9F26);
        if (arqc != null && arqc.length > 0) {
            transData.setArqc(Utils.bcd2Str(arqc));
        }

        byte[] f55Dup = EmvTags.getF55(emv, transType, true, pan);
        if (f55Dup.length > 0) {
            transData.setDupIccData(Utils.bcd2Str(f55Dup));
        }
    }

    /**
     * save track data
     *
     * @param transData {@link TransData}
     */
    static void saveCardInfoAndCardSeq(IEmv emv, TransData transData) {
        //AID
        byte[] aid = emv.getTlv(0x4F);
        String strAid = (aid != null && aid.length > 0) ? Utils.bcd2Str(aid) : "";

        byte[] track2 = emv.getTlv(TagsTable.TRACK2);
        String strTrack2;
        if (strAid.contains(Constants.DINERS_AID_PREFIX)) {
            String pan = TrackUtils.getPan(Utils.bcd2Str(track2));
            strTrack2 = pan.length() < 19 ? TrackUtils.getTrack2FromTag57(track2, true) : TrackUtils.getTrack2FromTag57(track2, false);
        } else {
            strTrack2 = TrackUtils.getTrack2FromTag57(track2, true);
        }
        transData.setTrack2(strTrack2);
        // card no
        String pan = TrackUtils.getPan(strTrack2);
        transData.setPan(pan);
        // exp date
        byte[] expDate = emv.getTlv(0x5F24);
        if (expDate != null && expDate.length > 0) {
            String temp = Utils.bcd2Str(expDate);
            transData.setExpDate(temp.substring(0, 4));
        }
        byte[] cardSeq = emv.getTlv(0x5F34);
        if (cardSeq != null && cardSeq.length > 0) {
            String temp = Utils.bcd2Str(cardSeq);
            transData.setCardSerialNo(temp.substring(0, 2));
        }
        //Cardholder name
        byte[] holderName = emv.getTlv(0x5F20);
        if (holderName != null && holderName.length > 0) {
            String temp = new String(holderName);
            TransData.EnterMode enterMode = transData.getEnterMode();
            if (enterMode == TransData.EnterMode.SWIPE) {
                temp = TrackUtils.getHolderName(temp);
                temp = Utils.splitHolderName(temp.trim());
            }else{
                temp = Utils.splitHolderName(temp.trim());
            }
            transData.setTrack1(temp);
        }
    }

    private static void saveTvrTsi(IEmv emv, TransData transData) {
        // TVR
        byte[] tvr = emv.getTlv(0x95);
        if (tvr != null && tvr.length > 0) {
            transData.setTvr(Utils.bcd2Str(tvr));
        }
        // ATC
        byte[] atc = emv.getTlv(0x9F36);
        if (atc != null && atc.length > 0) {
            transData.setAtc(Utils.bcd2Str(atc));
        }
        //
        // TSI
        byte[] tsi = emv.getTlv(0x9B);
        if (tsi != null && tsi.length > 0) {
            transData.setTsi(Utils.bcd2Str(tsi));
        }
        // TC
        byte[] tc = emv.getTlv(0x9F26);
        if (tc != null && tc.length > 0) {
            transData.setTc(Utils.bcd2Str(tc));
        }

        // AppLabel
        byte[] appLabel = emv.getTlv(0x50);
        if (appLabel != null && appLabel.length > 0) {
            transData.setEmvAppLabel(new String(appLabel));
        }
        // AppName
        byte[] appName = emv.getTlv(0x9F12);
        if (appName != null && appName.length > 0) {
            transData.setEmvAppName(new String(appName));
        }
        // AID
        byte[] aid = emv.getTlv(0x4F);
        if (aid != null && aid.length > 0) {
            transData.setAid(Utils.bcd2Str(aid));
        }
    }

    /**
     * @return true-no PIN false-unknown
     */
    private static boolean clssCDCVMProcss(IEmv emv) {
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.QUICK_PASS_TRANS_CDCVM_FLAG)) {

            byte[] value = emv.getTlv(0x9F6C);
            if ((value[1] & CD_CVM) == CD_CVM && (value[0] & ONLINEPIN_CVM) != ONLINEPIN_CVM) {
                return true;
            }

        }

        return false;
    }

    /**
     * QPBOC need to check if online PIN is required, only foreign card need to check by value of tag 9F6C,
     * local card is default to have PIN
     *
     * @return true/false
     */
    public static boolean isQpbocNeedOnlinePin(IEmv emv) {
        if (!isCupOutSide(emv)) {
            return true;
        }

        byte[] value = emv.getTlv(0x9F6C);
        return (value[0] & ONLINEPIN_CVM) == ONLINEPIN_CVM;
    }

    /**
     * check if is a dual currency CUP card
     */
    private static boolean isCupOutSide(IEmv emv) {
        int[] tags = new int[]{0x9F51, 0xDF71}; // tag9F51：第一货币 tagDF71：第二货币
        int flag = 0;
        byte[] val = null;
        for (int tag : tags) {
            val = emv.getTlv(tag);
            if (val == null) {
                continue;
            }
            flag = 1; // 能获取到货币代码值
            if ("0156".equals(Utils.bcd2Str(val))) {
                return false;
            }
        }

        return !(val == null && flag == 0);
    }

    /**
     * credit or debit
     *
     * @param aid aid
     */
    private static boolean isCredit(String aid) {
        if (UNIONPAY_DEBITAID.equals(aid)) { // debit
            return false;
        } else if (UNIONPAY_CREDITAID.equals(aid)) { // credit
            return true;
        } else //  quasi credit
            return UNIONPAY_QUASICREDITAID.equals(aid);
    }

    /**
     * @param transData {@link TransData}
     * @return true-no PIN false-unknown
     */
    public static boolean clssQPSProcess(IEmv emv, TransData transData) {

        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.QUICK_PASS_TRANS_PIN_FREE_SWITCH)) {
            return false;
        }
        TransData.EnterMode enterMode = transData.getEnterMode();
        if (enterMode != TransData.EnterMode.CLSS) {
            return false;
        }
        int limitAmount = FinancialApplication.getSysParam().get(SysParam.NumberParam.QUICK_PASS_TRANS_PIN_FREE_AMOUNT);
        String amount = transData.getAmount().replace(".", "");
        // card type
        byte[] aid = emv.getTlv(0x4F);
        if (aid == null) {
            return false;
        }
        boolean isCredit = isCredit(Utils.bcd2Str(aid));
        boolean pinFree;
        ETransType transType = transData.getTransType();
        if (ETransType.SALE.equals(transType)
                || ETransType.PREAUTH.equals(transType)) {
            pinFree = clssCDCVMProcss(emv);
            transData.setCDCVM(pinFree);
            if (pinFree) {
                return true;
            }
            if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.QUICK_PASS_TRANS_FLAG)) {
                return false;
            }

            if (isCupOutSide(emv)) { // 外卡
                // 贷记或准贷记卡: 小于免密限额则免输密码 借记卡：依据卡片与终端协商结果

                if (!isCredit) { // 借记卡
                    return false;
                }
                // 贷记卡或准贷记卡处理
                return (Integer.parseInt(amount) <= limitAmount);
            } else { // 内卡
                return (Integer.parseInt(amount) <= limitAmount);
            }
        }

        return false;

    }
}
