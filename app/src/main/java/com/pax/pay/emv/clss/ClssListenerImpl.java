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
package com.pax.pay.emv.clss;

import android.content.Context;
import android.os.ConditionVariable;

import com.pax.abl.utils.TrackUtils;
import com.pax.dal.ICardReaderHelper;
import com.pax.dal.entity.EReaderType;
import com.pax.dal.entity.PollingResult;
import com.pax.device.Device;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IClss;
import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;
import com.pax.eventbus.SearchCardEvent;
import com.pax.jemv.clcommon.Clss_ProgramID_II;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.device.model.ApduRespL2;
import com.pax.jemv.device.model.ApduSendL2;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvBaseListenerImpl;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

public class ClssListenerImpl extends EmvBaseListenerImpl implements IClssListener {
    private CTransResult result;
    private static final String TAG = "ClssListenerImpl";
    private IClss clss;

    private static final String MASTER_MCHIP = "A0000000041010";
    private static final String MASTER_MAESTRO = "A0000000043060";

    private boolean detect2ndTap = false;

    public ClssListenerImpl(Context context, IClss clss, TransData transData, TransProcessListener listener) {
        super(context, clss, transData, listener);
        this.clss = clss;
    }

    @Override
    public int onCvmResult(ECvmResult result) {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }
        intResult = 0;

        // TEST MODE
//        Acquirer acq = EmvTransProcess.getAcqFromAID();
//        if (acq != null && acq.isTestMode()) {
//            return intResult;
//        }

        if (result == ECvmResult.ONLINE_PIN || result == ECvmResult.ONLINE_PIN_SIG) {
            cv = new ConditionVariable();
            enterPin(true, 0, true);
            transData.setHasPin(true);
            transData.setOnlinePin(true);
            cv.block(); // for the Offline pin case, block it for make sure the PIN activity is ready, otherwise, may get the black screen.
        }

        // TODO for the case of e-sign, it requires upload e-sign 8583 message which is not supported by host for now, so we ignore it and do signature on receipt.

        return intResult;
    }

    @Override
    public void onComfirmCardInfo(String track1, String track2, String track3) throws EmvException {
        byte[] holderName = clss.getTlv(0x5F20);
        if (holderName != null && holderName.length > 0) {
            String temp = new String(holderName);
            temp = Utils.splitHolderName(temp.trim());
            transData.setTrack1(temp);
        }
        transData.setTrack2(track2);
        transData.setTrack3(track3);

        String pan = TrackUtils.getPan(track2);
        transData.setPan(pan);

//        Issuer issuer = FinancialApplication.getAcqManager().findIssuerByPan(pan);
        String mIssuer = getIssuerBrandByAid();
        List<Issuer> matchedIssuerList = FinancialApplication.getAcqManager().findAllIssuerByPan(pan, mIssuer != null ? mIssuer : "");
        Issuer issuer = (matchedIssuerList != null && !matchedIssuerList.isEmpty()) ? matchedIssuerList.get(0) : null;
        issuer = isSelectIssuer(matchedIssuerList) ? matchedIssuer : issuer;
        if (issuer != null && FinancialApplication.getAcqManager().isIssuerSupported(issuer) && isCTLSSupportByIssuer(issuer)) {
            Log.i(TAG, "onConfirmCardNo[IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]");
            transData.setIssuer(issuer);
        } else {
            Log.i(TAG, issuer != null ? "onConfirmCardNo[EDC_Not_Support, IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]" : "onConfirmCardNo[EDC_No_Issuer]");
            throw new EmvException(EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT);
        }
        setTlvTag5A(transData.getPan());

        transData.setDccRequired(Component.isDccRequired(transData, pan, clss.getTlv(0x9F42)));

        Acquirer acquirer = Component.setAcqFromIssuer(transData);
        if (acquirer == null || !acquirer.isEnable()) {
            Log.i(TAG, "onConfirmCardNo[EDC_Not_matched_acquirer]");
            throw new EmvException(EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT);
        }


        String expDate = TrackUtils.getExpDate(transData.getTrack2());
        transData.setExpDate(expDate);
        if (!Component.isDemo() &&
                (!Issuer.validPan(transData.getIssuer(), pan) ||
                        !Issuer.validCardExpiry(transData.getIssuer(), expDate))) {
            throw new EmvException(EEmvExceptions.EMV_ERR_CLSS_CARD_EXPIRED);
        }

        //PanSeqNo
        byte[] value = clss.getTlv(TagsTable.PAN_SEQ_NO);
        if (value != null) {
            String cardSerialNo = Utils.bcd2Str(value);
            transData.setCardSerialNo(cardSerialNo.substring(0, value.length * 2));
        }

        value = clss.getTlv(TagsTable.APP_LABEL);
        if (value != null) {
            transData.setEmvAppLabel(new String(value));
        }

        if (holderName == null) {
            holderName = " ".getBytes();
        }
        byte[] IssuerName = clss.getTlv(0x9F12);
        String issuerCard = IssuerName != null ? new String(IssuerName) : null;
        float percent = transData.getTransType().isAdjustAllowed() ? transData.getIssuer().getAdjustPercent() : 0; //AET_247

        if(IssuerName == null && !holderName.equals(" ".getBytes()) && issuer.getName().equals(Constants.ISSUER_JCB)  ){
            //TEMP handle case cannot read IssuerName for JCB
            FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_UPDATE_CARD_INFO, new CardInfo(pan, new String(holderName), expDate, percent,"J/SMART CONTACT")));
        }else{
            if (IssuerName == null) {
                IssuerName = clss.getTlv(TagsTable.APP_LABEL);
                issuerCard = IssuerName != null ? new String(IssuerName) : "";
            }
            FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_UPDATE_CARD_INFO, new CardInfo(pan, new String(holderName), expDate, percent, issuerCard)));
        }

        try {
            Thread.sleep(2000); // 2 second
        } catch (InterruptedException ex) {
            // handle error
        }


        FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.ICC_CONFIRM_CARD_NUM));
    }

    @Override
    protected int updateTransDataFromKernel() {
        int ret = new Transmit().sendReversal(transProcessListener, transData.getAcquirer());
        ClssTransProcess.clssTransResultProcess(result, clss, transData);
        return ret;
    }

    @Override
    public EOnlineResult onOnlineProc(CTransResult result) {
        this.result = result;
        return onlineProc();
    }

    @Override
    public boolean onDetect2ndTap() {
        final ConditionVariable cv = new ConditionVariable();
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (transData.getEnterMode() == TransData.EnterMode.CLSS && transProcessListener != null) {
                    transProcessListener.onShowProgress(context.getString(R.string.prompt_wave_card), 30);
                }
                try {
                    //tap card
                    ICardReaderHelper helper = FinancialApplication.getDal().getCardReaderHelper();
                    helper.polling(EReaderType.PICC, 30 * 1000);
                    helper.stopPolling();
                    detect2ndTap = true;
                } catch (Exception e) {
                    Log.e(TAG, "", e);
                } finally {
                    if (transProcessListener != null)
                        transProcessListener.onHideProgress();
                    cv.open();
                }
            }
        });
        cv.block();
        return detect2ndTap;
    }

    @Override
    public byte[] onUpdateKernelCfg(String aid) {
        if (MASTER_MCHIP.equals(aid)) {
            return new byte[]{(byte) 0x20};
        } else if (MASTER_MAESTRO.equals(aid)) {
            return new byte[]{(byte) 0xA0};
        }
        return null;
    }

    @Override
    public int onIssScrCon() {
        ApduSendL2 apduSendL2 = new ApduSendL2();
        ApduRespL2 apduRespL2 = new ApduRespL2();
        byte[] sendCommand = new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
        System.arraycopy(sendCommand, 0, apduSendL2.command, 0, sendCommand.length);
        apduSendL2.lc = 14;
        String sendDataIn = "1PAY.SYS.DDF01";
        System.arraycopy(sendDataIn.getBytes(), 0, apduSendL2.dataIn, 0, sendDataIn.getBytes().length);
        apduSendL2.le = 256;
        int ret = (int) DeviceImplNeptune.getInstance().iccCommand(apduSendL2, apduRespL2);
        if (ret != RetCode.EMV_OK)
            return ret;

        if (apduRespL2.swa != (byte) 0x90 || apduRespL2.swb != 0x00)
            return RetCode.EMV_RSP_ERR;

        apduSendL2 = new ApduSendL2();
        apduRespL2 = new ApduRespL2();
        System.arraycopy(sendCommand, 0, apduSendL2.command, 0, sendCommand.length);
        apduSendL2.lc = 14;
        System.arraycopy(transData.getAid().getBytes(), 0, apduSendL2.dataIn, 0, transData.getAid().getBytes().length);
        apduSendL2.le = 256;
        ret = (int) DeviceImplNeptune.getInstance().iccCommand(apduSendL2, apduRespL2);
        if (ret != RetCode.EMV_OK)
            return ret;

        if (apduRespL2.swa != (byte) 0x90 || apduRespL2.swb != 0x00)
            return RetCode.EMV_RSP_ERR;

        return RetCode.EMV_OK;
    }

    @Override
    public void onPromptRemoveCard() {
        final ConditionVariable cv = new ConditionVariable();
        FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_REMOVE_CARD));
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                Device.removeCard(new Device.RemoveCardListener() {
                    @Override
                    public void onShowMsg(PollingResult result) {
                        if (transProcessListener != null) {
//                                transProcessListener.onShowNormalMessage(context.getString(R.string.wait_remove_card), Constants.SUCCESS_DIALOG_SHOW_TIME, false);
                            transProcessListener.onHideProgress();
                            transProcessListener.onShowWarning(context.getString(R.string.wait_remove_card), -1);
                        }
                    }
                });
                if (transProcessListener != null) {
                    transProcessListener.onHideProgress();
                }
                cv.open();
            }
        });
        cv.block();
    }

    private int retCallback = RetCode.EMV_OK;
    @Override
    public int onDisplaySeePhone() {
        FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_NOT_READY));
        retCallback = Device.detectCard(context.getString(R.string.prompt_clss_please_see_phone), -1, context);
        if (retCallback == TransResult.ERR_USER_CANCEL) {
            return RetCode.EMV_USER_CANCEL;
        }

        return retCallback;
    }

//    @Override
//    public int onDisplaySeePhone() {
//        final ConditionVariable cv = new ConditionVariable();
//        FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_NOT_READY));
//        FinancialApplication.getApp().runInBackground(new Runnable() {
//            @Override
//            public void run() {
//                retCallback = Device.detectCard(new Device.RemoveCardWithResultListener() {
//                    @Override
//                    public int onShowMsg(PollingResult result) {
//                        if (transProcessListener != null) {
//                            transProcessListener.onHideProgress();
//                            retCallback = transProcessListener.onShowWarning(context.getString(R.string.prompt_clss_please_see_phone), -1, true, cv);
//                        }
//                        return retCallback;
//                    }
//                });
//                if (transProcessListener != null) {
//                    transProcessListener.onHideProgress();
//                }
//                cv.open();
//            }
//        });
//        cv.block();
//
//        if (retCallback == TransResult.ERR_USER_CANCEL) {
//            return RetCode.EMV_USER_CANCEL;
//        }
//
//        return retCallback;
//    }

    @Override
    public void onSetClssTypeMode(int transactionPath) {
        byte[] aid = clss.getTlv(0x4F);
        if (aid != null) {
            String clssAid = Utils.bcd2Str(aid);
            if (clssAid.contains(Constants.AMEX_AID_PREFIX)) {
                if (transData.getClssTypeMode() <= 0) {
                    transData.setClssTypeMode(transactionPath);
                }
                return;
            }
        }
        transData.setClssTypeMode(transactionPath);
    }

    @Override
    public boolean onChkForceSettlement() {
        return Component.chkSettlementStatus(transData.getAcquirer().getName());
    }

    public static class CardInfo {
        private String cardNum;
        private String holderName;
        private String expDate;
        private float adjustPercent;
        private String issuerCard;


        CardInfo(String cardNum, String holderName, String expDate, float adjustPercent, String issuerCard) {
            this.cardNum = cardNum;
            this.holderName = holderName;
            this.expDate = expDate;
            this.adjustPercent = adjustPercent;
            this.issuerCard = issuerCard;
        }

        public String getCardNum() {
            return cardNum;
        }

        public String getHolderName() {
            return holderName;
        }

        public String getExpDate() {
            return expDate;
        }

        public float getAdjustPercent() {
            return adjustPercent;
        }

        public String getIssuer() {
            return issuerCard;
        }
    }

    @Override
    public List<Clss_ProgramID_II> onGetProgramId() {
        List<ClssProgramId> clssProgramIdList = Utils.readObjFromJSON("clssProgramId.json", ClssProgramId.class);
        if (clssProgramIdList.isEmpty()) {
            return null;
        }
        List<Clss_ProgramID_II> list = new ArrayList<>();
        for (ClssProgramId i : clssProgramIdList) {
            list.add(clssExpressGetProgramInfo(i));
        }
        return list;
    }

    @Override
    public boolean onChkIsDynamicOffline() {
        return Component.isAllowDynamicOffline(transData);
    }

    @Override
    public int onProcessEnterRefNo() {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        intResult = 0;

        /*  //Remove for KBANK
        if (ETransType.REFUND == transData.getTransType() && Constants.ACQ_UP.equals(transData.getAcquirer().getName())) {
            cv = new ConditionVariable();
            cv.close();
            doEnterRefNoAction(transData.getTransType().getTransName());
            cv.block();
        }*/

        return intResult;
    }

    private Clss_ProgramID_II clssExpressGetProgramInfo(ClssProgramId clssProgramId) {
        Clss_ProgramID_II value = new Clss_ProgramID_II();
        value.aucRdClssTxnLmt = Tools.str2Bcd(clssProgramId.getAucRdClssTxnLmt());
        value.aucRdCVMLmt = Tools.str2Bcd(clssProgramId.getAucRdCVMLmt());
        value.aucRdClssFLmt = Tools.str2Bcd(clssProgramId.getAucRdClssFLmt());
        value.aucTermFLmt = Tools.str2Bcd(clssProgramId.getAucTermFLmt());
        value.aucProgramId = Tools.str2Bcd(clssProgramId.getAucProgramId());
        value.ucPrgramIdLen = (byte) clssProgramId.getUcPrgramIdLen();
        value.ucRdClssFLmtFlg = (byte) clssProgramId.getUcRdClssFLmtFlg();
        value.ucRdClssTxnLmtFlg = (byte) clssProgramId.getUcRdClssTxnLmtFlg();
        value.ucRdCVMLmtFlg = (byte) clssProgramId.getUcRdCVMLmtFlg();
        value.ucTermFLmtFlg = (byte) clssProgramId.getUcTermFLmtFlg();
        value.ucStatusCheckFlg = (byte) clssProgramId.getUcStatusCheckFlg();
        value.ucAmtZeroNoAllowed = (byte) clssProgramId.getUcAmtZeroNoAllowed();
        value.ucDynamicLimitSet = (byte) clssProgramId.getUcDynamicLimitSet();
        value.ucRFU = (byte) clssProgramId.getUcRFU();
        return value;
    }


    private String getIssuerBrandByAid() {
        List<AidParam> aidParams = clss.getAidParamList();
        byte[] aid = clss.getTlv(0x4F);
        for (AidParam a : aidParams) {
            if(aid!=null && Utils.bcd2Str(aid).contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                return a.getIssuerBrand();
            }
        }
        return "";
    }

    private void setTlvTag5A(String track2Pan) throws EmvException {
        byte[] aid = clss.getTlv(0x4F);
        String clssAid = Utils.bcd2Str(aid);
        if (!clssAid.contains(Constants.UP_AID_PREFIX)) {
            // Not QuickPass, no need to set tag 5A
            return;
        }
        clss.setTlv(TagsTable.APP_PAN, Tools.str2Bcd(track2Pan));
    }

    private boolean isCTLSSupportByIssuer(Issuer issuer) {
        switch (issuer.getIssuerBrand()){
            case Constants.ISSUER_VISA:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_VISA);
            case Constants.ISSUER_MASTER:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_MASTER);
            case Constants.ISSUER_JCB:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_JCB);
            case Constants.ISSUER_UP:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_UP);
            case Constants.ISSUER_BRAND_TBA:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_TPN);
            case Constants.ISSUER_AMEX:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_AMEX);
        }
        return false;
    }
}
