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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.ConditionVariable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.pax.abl.utils.TrackUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IEmv;
import com.pax.eemv.IEmvListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.Amounts;
import com.pax.eemv.entity.CandList;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;
import com.pax.eventbus.SearchCardEvent;
import com.pax.jemv.emv.api.EMVApi;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import th.co.bkkps.utils.ArrayListUtils;
import th.co.bkkps.utils.Log;

public class EmvListenerImpl extends EmvBaseListenerImpl implements IEmvListener {

    private static final String TAG = EmvListenerImpl.class.getSimpleName();
    private IEmv emv;
    private Map<String, Object> mapParam;

    public EmvListenerImpl(Context context, IEmv emv, TransData transData, TransProcessListener listener) {
        super(context, emv, transData, listener);
        this.emv = emv;
    }

    @Override
    public final int onCardHolderPwd(final boolean isOnlinePin, final int offlinePinLeftTimes, byte[] pinData) {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        intResult = 0;

        // TEST MODE
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if (acq != null && acq.isTestMode() && isOnlinePin) {
            return intResult;
        }

        cv = new ConditionVariable();

        if (pinData != null && pinData[0] != 0) {
            return pinData[0];
        }

        enterPin(isOnlinePin, offlinePinLeftTimes, false);

        transData.setHasPin(true);
        transData.setOnlinePin(isOnlinePin);

        cv.block(); // for the Offline pin case, block it for make sure the PIN activity is ready, otherwise, may get the black screen.
        return intResult;
    }

    @Override
    public final boolean onChkExceptionFile() {
        Log.e(TAG, "onChkExceptionFile");
        byte[] track2 = emv.getTlv(TagsTable.TRACK2);
        String strTrack2 = TrackUtils.getTrack2FromTag57(track2, true);
        // 卡号
        String pan = TrackUtils.getPan(strTrack2);
        boolean ret = FinancialApplication.getCardBinDb().isBlack(pan);
        if (ret) {
            transProcessListener.onShowErrMessage(context.getString(R.string.emv_card_in_black_list), Constants.FAILED_DIALOG_SHOW_TIME, true);
            return true;
        }
        return false;
    }

    @Override
    public final int onConfirmCardNo(final String cardno) {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        //Set Capability by AID
        byte[] aid = emv.getTlv(0x4F);
        if (aid != null && aid.length > 0) {
            Log.d(TAG, "onConfirmCardNo[AID selected=" + Utils.bcd2Str(aid) + "]");
            emv.setConfig(findCapability(aid));
        }

        //Issuer issuer = FinancialApplication.getAcqManager().findIssuerByPan(cardno);
        // EDCBBLAND-75 Change logic to separate issuer with AID for Chip cards
        String mIssuer = (String) mapParam.get("aidIssuerBrand");
        List<Issuer> matchedIssuerList = FinancialApplication.getAcqManager().findAllIssuerByPan(cardno, mIssuer != null ? mIssuer : "");
        Issuer issuer = (matchedIssuerList != null && !matchedIssuerList.isEmpty()) ? matchedIssuerList.get(0) : null;
        issuer = isSelectIssuer(matchedIssuerList) ? matchedIssuer : issuer;
        if (issuer != null && FinancialApplication.getAcqManager().isIssuerSupported(issuer)) {
            Log.i(TAG, "onConfirmCardNo[IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]");
            transData.setIssuer(issuer);
        } else {
            Log.i(TAG, issuer != null ? "onConfirmCardNo[EDC_Not_Support, IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]" : "onConfirmCardNo[EDC_No_Issuer]");
            intResult = EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT.getErrCodeFromBasement();
            return intResult;
        }

        transData.setDccRequired(Component.isDccRequired(transData, cardno, emv.getTlv(0x9F42)));
        //if (transData.isDccRequired()) {
            EmvTransProcess.saveCardInfoAndCardSeq(emv, transData);
        //}

        Acquirer acquirer = Component.setAcqFromIssuer(transData);
        if (acquirer == null || !acquirer.isEnable()) {
            Log.i(TAG, "onConfirmCardNo[EDC_Not_matched_acquirer]");
            intResult = EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT.getErrCodeFromBasement();
            return intResult;
        }

        cv = new ConditionVariable();

        byte[] holderNameBCD = emv.getTlv(0x5F20);
        if (holderNameBCD == null) {
            holderNameBCD = " ".getBytes();
        }
        byte[] expDateBCD = emv.getTlv(0x5F24);
        String expDate = Utils.bcd2Str(expDateBCD);
        byte[] IssuerName = emv.getTlv(0x9F12);
        String issuerCard = IssuerName != null ? new String(IssuerName) : null;
        float percent = transData.getTransType().isAdjustAllowed() ? transData.getIssuer().getAdjustPercent() : 0; //AET_247
       // FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.ICC_UPDATE_CARD_INFO, new CardInfo(cardno, new String(holderNameBCD), expDate, percent,new String(IssuerName))));

        if(IssuerName == null && !holderNameBCD.equals(" ".getBytes()) && issuer.getName().equals(Constants.ISSUER_JCB)  ){
            //TEMP handle case cannot read IssuerName for JCB
            FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.ICC_UPDATE_CARD_INFO, new CardInfo(cardno, new String(holderNameBCD), expDate, percent,"J/SMART CONTACT")));
        }else{
            if (IssuerName == null) {
                IssuerName = emv.getTlv(0x50);
                issuerCard = IssuerName != null ? new String(IssuerName) : "";
            }
            FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.ICC_UPDATE_CARD_INFO, new CardInfo(cardno, new String(holderNameBCD), expDate, percent, issuerCard)));
        }

        if (!Component.isDemo() &&
                (!Issuer.validPan(transData.getIssuer(), cardno) ||
                        !Issuer.validCardExpiry(transData.getIssuer(), expDate))) {
            intResult = EEmvExceptions.EMV_ERR_DATA.getErrCodeFromBasement();
            return intResult;
        }
        if (!transData.isOnlineTrans()) {
            transData.setExpDate(expDate.substring(0, 4));
        }

        try {
            Thread.sleep(2000); // 2 second
        } catch (InterruptedException ex) {
            // handle error
        }


        FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.ICC_CONFIRM_CARD_NUM));

        cv.block();

        return intResult;
    }

    @Override
    public final Amounts onGetAmounts() {
        Amounts amt = new Amounts();
        amt.setTransAmount(transData.getAmount());
        return amt;
    }

    @Override
    protected int updateTransDataFromKernel() {
        int ret = super.updateTransDataFromKernel();
        EmvTransProcess.saveCardInfoAndCardSeq(emv, transData);
        return ret;
    }

    @Override
    public EOnlineResult onOnlineProc() {
        return onlineProc();
    }

    public EOnlineResult onUpdateScriptResult() {
        return updateScriptResult();
    }

    @Override
    public final int onWaitAppSelect(final boolean isFirstSelect, final List<CandList> candList) {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        ApplicationSelection appSelection;
        (appSelection = new ApplicationSelection(candList)).process();
        if ((intResult = appSelection.resultIdx) < 0) {
            cv = new ConditionVariable();
            // ignore Sonar's lambda suggestion cuz the Sonar runs JAVA8, but EDC runs JAVA7,
            // there are same cases, ignore them as well.
            FinancialApplication.getApp().runOnUiThread(new SelectAppRunnable(isFirstSelect, appSelection.resultCandList));
            cv.block();
        }

        return intResult;
    }

    @Override
    public int setDe55ForReversal() throws EmvException {
        return setF55ForReversal();
    }

    @Override
    public boolean onChkForceSettlement() {
        return Component.chkSettlementStatus(transData.getAcquirer().getName());
    }

    @Override
    public int onChkIsOfflineTransSend() {
        Issuer issuer = transData.getIssuer();
        if (ETransType.OFFLINE_TRANS_SEND == transData.getTransType()) {
            if (issuer != null && issuer.isEnableOffline()) {
                //check aid to accept only credit card
                String[] visaMasterJcbCredit = ArrayUtils.addAll(
                        ArrayUtils.addAll(Constants.VISA_AID_CREDIT, Constants.MASTER_AID_CREDIT), Constants.JCB_AID_CREDIT);
                String[] upTpnCredit = ArrayUtils.addAll(Constants.UP_AID_CREDIT, Constants.TPN_AID_CREDIT);
                String[] allAidCreditSupport = ArrayUtils.addAll(visaMasterJcbCredit, upTpnCredit);
                byte[] aid = emv.getTlv(0x4F);
                String strAid = aid != null ? Utils.bcd2Str(aid) : null;
                if (ArrayListUtils.INSTANCE.isFoundItem(allAidCreditSupport, strAid)) {
                    onUpdateOfflineTransDataFromKernel();
                    return TransResult.SUCC;
                } else {
                    return TransResult.ERR_NOT_SUPPORT_TRANS;
                }
            } else {
                return TransResult.ERR_NOT_SUPPORT_TRANS;
            }
        }
        return TransResult.ERR_UNSUPPORTED_FUNC;
    }

    @Override
    public final int onProcessEnterAuthCode() {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        intResult = 0;

        cv = new ConditionVariable();

        cv.close();
        doEnterAuthCodeAction();

        cv.block();
        return intResult;
    }

    @Override
    public void onUpdateOfflineTransDataFromKernel() {
        EmvTransProcess.saveCardInfoAndCardSeq(emv, transData);
    }

    @Override
    public int onChkIsNotAllowRefundFullEmv() {
        Issuer issuer = transData.getIssuer();
        if (ETransType.REFUND == transData.getTransType()) {
            if (issuer != null && issuer.isAllowRefund()) {
                /*if (!Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())) {
                    onUpdateOfflineTransDataFromKernel();
                    return TransResult.SUCC;
                }*/ //Comment as KBank allow Refund Full EMV
                return TransResult.ERR_UNSUPPORTED_FUNC;
            } else {
                return TransResult.ERR_NOT_SUPPORT_TRANS;
            }
        }
        return TransResult.ERR_UNSUPPORTED_FUNC;
    }

    @Override
    public boolean onChkIsDynamicOffline() {
        if (Component.isAllowDynamicOffline(transData)) {
            onUpdateOfflineTransDataFromKernel();
            return true;
        }
        return false;
    }

    @Override
    public int onProcessEnterRefNo() {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }

        intResult = 0;

        /* //Remove for KBANK
        if (ETransType.REFUND == transData.getTransType() && Constants.ACQ_UP.equals(transData.getAcquirer().getName())) {
            cv = new ConditionVariable();
            cv.close();
            doEnterRefNoAction(transData.getTransType().getTransName());
            cv.block();
        }*/

        return intResult;
    }

    private class SelectAppRunnable implements Runnable {
        private final boolean isFirstSelect;
        private final List<CustomCandList> candList;

        SelectAppRunnable(final boolean isFirstSelect, final List<CustomCandList> candList) {
            this.isFirstSelect = isFirstSelect;
            this.candList = candList;
        }

        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (isFirstSelect) {
                builder.setTitle(context.getString(R.string.emv_application_choose));
            } else {
                SpannableString sstr = new SpannableString(context.getString(R.string.emv_application_choose_again));
                sstr.setSpan(new ForegroundColorSpan(Color.RED), 5, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setTitle(sstr);
            }
            String[] appNames = new String[candList.size()];
            for (int i = 0; i < appNames.length; i++) {
                appNames[i] = candList.get(i).getAppName();
            }
            builder.setSingleChoiceItems(appNames, -1, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    intResult = which;
                    intResult = candList.get(which).getActualIndex();
                    close(dialog);
                }
            });

            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            intResult = EEmvExceptions.EMV_ERR_USER_CANCEL.getErrCodeFromBasement();
                            close(dialog);
                        }
                    });
            builder.setCancelable(false);
            builder.create().show();
        }

        private void close(DialogInterface dialog) {
            dialog.dismiss();
            cv.open();
        }
    }


    public void offlinePinEnterReady() {
        cv.open();
    }

    public void cardNumConfigErr() {
        intResult = EEmvExceptions.EMV_ERR_USER_CANCEL.getErrCodeFromBasement();
        cv.open();
    }

    public void cardNumConfigSucc() {
        intResult = EEmvExceptions.EMV_OK.getErrCodeFromBasement();
        cv.open();
    }

    public void cardNumConfigSucc(String[] amount) {
        if (amount != null && amount.length == 2) {
            transData.setAmount(String.valueOf(CurrencyConverter.parse(amount[0])));
            transData.setTipAmount(String.valueOf(CurrencyConverter.parse(amount[1])));
        }
        cardNumConfigSucc();
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

    /**
     * find host by aid
     */
    private Map<String, Object> setCapabilityByAID(byte[] aid) {
        mapParam = new HashMap<>();
        List<AidParam> aidParams = emv.getAidParamList();

        String capability = "E0B0C8";
        for (AidParam a : aidParams) {
            if(Utils.bcd2Str(aid).contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                mapParam.put("termAppVersion", a.getVersion());
                mapParam.put("termCapability", a.getCapability());
                mapParam.put("aidIssuerBrand", a.getIssuerBrand());
                return mapParam;
            }
        }
        mapParam.put("termAppVersion", "".getBytes());
        mapParam.put("termCapability", capability);
        return mapParam;
    }

    /**
     * find Capability by aid
     */
    private Config findCapability(byte[] aid) {
        Config cfg = Component.genCommonEmvConfig();
        Map<String, Object> result = setCapabilityByAID(aid);
        String capability = (String) result.get("termCapability");
        cfg.setCapability(capability);
        cfg.setExCapability("E000F0A001");

//        if (Utils.bcd2Str(aid).contains(Constants.AMEX_AID_PREFIX)) {
            switch (transData.getTransType()) {
                case REFUND:
                    EMVApi.EMVSetTLVData((short) 0x9C, Utils.str2Bcd("20"), 1);
                    break;
                case PREAUTH:
                    EMVApi.EMVSetTLVData((short) 0x9C, Utils.str2Bcd("30"), 1);
                    break;
                default:
                    EMVApi.EMVSetTLVData((short) 0x9C, Utils.str2Bcd("00"), 1);
                    break;
            }
//        } else {
//            cfg.setTransType((byte) 0);
//        }

        byte[] cap = Utils.str2Bcd(capability);
        EMVApi.EMVSetTLVData((short) 0x9F33, cap, cap.length);

        byte[] termApp = (byte[]) result.get("termAppVersion");
        if (termApp != null && termApp.length > 0) {
            EMVApi.EMVSetTLVData((short) 0x9F09, termApp, termApp.length);
        }

        return cfg;
    }

    enum ForceAppSelected {
        OTHERS(""),
        TPN(Constants.TBA_AID_PREFIX);

        private final String value;

        ForceAppSelected(String value) {
            this.value = value;
        }
    }

    private class ApplicationSelection {
        List<CandList> candList;
        ForceAppSelected firstAppSelected;
        boolean hasUP;
        boolean hasAmex;
        boolean enableForceAppUP = false;


        int resultIdx;
        List<CustomCandList> resultCandList;

        ApplicationSelection(List<CandList> candList) {
            this.candList = candList;
        }

        void process() {
            enableForceAppUP = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD);
            if (chkDifferentAid(candList)) {
                Log.d(TAG, "ApplicationSelection.process[Different AID]");
                if (enableForceAppUP && hasUP) {
                    Log.d(TAG, "ApplicationSelection.process[forceAppSelectUP]");
                    resultIdx = forceAppSelectUP();
                } else {
                    Log.d(TAG, "ApplicationSelection.process[forceAppSelect]");
                    resultIdx = forceAppSelect();
                }
            } else {
                //Same Aid but multi application so return -1 and default candList, let cashier choose app select
                Log.d(TAG, "ApplicationSelection.process[Same AID]");
                resultIdx = -1;
                setCustomCandList();
            }
            Log.d(TAG, "ApplicationSelection.process[resultIdx="+resultIdx+"]");
        }

        ForceAppSelected getFirstAppSelected(String aidPrefix) {
            switch (aidPrefix) {
                case Constants.TBA_AID_PREFIX:
                    return ForceAppSelected.TPN;
                default:
                    return ForceAppSelected.OTHERS;
            }
        }

        boolean chkDifferentAid(List<CandList> candList) {
            byte[] firstApp = new byte[5];
            boolean isDiffApp = false;
            for(int i=0 ; i < candList.size() ; i++){
                Log.d(TAG, "AID: " + Utils.bcd2Str(candList.get(i).getAid()));
                if (!hasUP && Utils.bcd2Str(candList.get(i).getAid()).contains(Constants.UP_AID_PREFIX)) {
                    hasUP = true;
                }

                if (!hasAmex && Utils.bcd2Str(candList.get(i).getAid()).contains(Constants.AMEX_AID_PREFIX)) {
                    hasAmex = true;
                }

                if (i == 0) {
                    firstApp = Arrays.copyOf(candList.get(i).getAid(), 5);
                    firstAppSelected = getFirstAppSelected(Utils.bcd2Str(firstApp));//first index in the candidate list is the highest priority.
                } else {
                    byte[] currApp = Arrays.copyOf(candList.get(i).getAid(), 5);
                    if (!isDiffApp && !Arrays.equals(firstApp, currApp)) {
                        isDiffApp = true;
                    }
                }
            }
            return isDiffApp;
        }

        int forceAppSelectUP() {
            resultCandList = new ArrayList<>();
            for (CandList i : candList) {
                if ((hasAmex && Utils.bcd2Str(i.getAid()).contains(Constants.AMEX_AID_PREFIX))
                        || (!hasAmex && Utils.bcd2Str(i.getAid()).contains(Constants.UP_AID_PREFIX))) {
                    //if card contains aid AMEX and i(index) is AMEX then skip to choose UP
                    //if card does NOT contain AMEX and i(index) is UP then skip to choose other issuers
                    continue;
                }
                CustomCandList customCandList = new CustomCandList();
                customCandList.setAppName(i.getAppName());
                customCandList.setActualIndex(candList.indexOf(i));
                resultCandList.add(customCandList);
            }

            if (resultCandList.size() > 1) {
                return -1;
            }

            return resultCandList.get(0).getActualIndex();
        }

        int forceAppSelect() {
            setCustomCandList();

            if (firstAppSelected == ForceAppSelected.OTHERS) {
                Log.d(TAG, "ApplicationSelection.process.forceAppSelect[firstAppSelected=OTHERS, No need to force app select]");
                return -1;
            }

            Log.d(TAG, "ApplicationSelection.process.forceAppSelect[firstAppSelected=TPN, force select]");
            for(CandList i : candList){
                if(Utils.bcd2Str(i.getAid()).contains(firstAppSelected.value)){
                    return candList.indexOf(i);
                }
            }

            return -2;
        }

        void setCustomCandList() {
            resultCandList = new ArrayList<>();
            for (CandList i : candList) {
                CustomCandList customCandList = new CustomCandList();
                customCandList.setAppName(i.getAppName());
                customCandList.setActualIndex(candList.indexOf(i));
                resultCandList.add(customCandList);
            }
        }
    }

    private class CustomCandList {
        private byte[] appName;
        private int actualIndex;

        String getAppName() {
            return Tools.bytes2String(this.appName);
        }

        void setAppName(String appName) {
            this.appName = Tools.string2Bytes(appName);
        }

        int getActualIndex() {
            return actualIndex;
        }

        void setActualIndex(int actualIndex) {
            this.actualIndex = actualIndex;
        }
    }
}
