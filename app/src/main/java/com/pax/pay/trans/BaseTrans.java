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
package com.pax.pay.trans;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.ConditionVariable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionEndListener;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.TrackUtils;
import com.pax.dal.IPicc;
import com.pax.dal.entity.EPiccType;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.EmvImpl;
import com.pax.eemv.IClss;
import com.pax.eemv.IEmv;
import com.pax.eemv.clss.ClssImpl;
import com.pax.eemv.entity.AidParam;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.trans.action.ActionRemoveCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionTransPreDeal;
import com.pax.pay.trans.action.ActionUpdateParam;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.task.BaseTask;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.Utils;
import com.pax.sdk.Sdk;
import com.pax.settings.SysParam;

import java.util.List;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;

public abstract class BaseTrans extends BaseTask {
    // 当前交易类型
    protected ETransType transType;
    protected IEmv emv;
    protected IClss clss;
    protected TransData transData;
    //AET-160
    protected boolean needRemoveCard = false;
    //AET-199
    private Activity old;

    private boolean backToMain = false;
    private boolean bssMenu = false;
    private String acquirerName;

    private boolean isSilentSucc = false;


    protected AAction.ActionEndListener mECRrocReturnListener = null;

    /**
     * whether transaction is running, it's global for all transaction, if insert a transaction in one transaction, control the status itself
     */
    private static boolean isTransRunning = false;

    public BaseTrans(Context context, ETransType transType, TransEndListener transListener, boolean isSilentSucc) {
        this(context, transType, transListener);
        this.isSilentSucc = isSilentSucc;
    }

    public BaseTrans(Context context, ETransType transType, TransEndListener transListener) {
        super(context, transListener);
        this.transType = transType;
    }

    public BaseTrans(Context context, ETransType transType, String acquirerName, TransEndListener transListener) {
        super(context, transListener);
        this.transType = transType;
        this.acquirerName = acquirerName;
    }

    public int ErmLimitExceedCheck() {
        if (!DynamicOffline.getInstance().isDynamicOfflineActiveStatus()) {
            int maxErmAwaitEreceiptRecord = FinancialApplication.getSysParam().get(SysParam.NumberParam.MAX_LIMIT_ERM_ERECEPT_PENDING_UPLOAD, EReceiptUtils.ERM_MAX_PENDING_UPLOAD_ERECEIPT_NUMBER);
            if (TransDataDb.getInstance().findCountTransDataWithEReceiptUploadStatus(true) >= maxErmAwaitEreceiptRecord) {
                return TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR;
            }
        }

        return TransResult.SUCC;
    }

    public int IssuerSupportTransactionCheck (List<String> issuerNameList) {
        if (issuerNameList==null || issuerNameList.isEmpty()) {
            return TransResult.ERR_NOT_ALLOW;
        }

        for (String issuerName : issuerNameList) {
            if (FinancialApplication.getAcqManager().findIssuer(issuerName) == null) {
                return TransResult.ERR_NOT_ALLOW;
            }
        }

        return TransResult.SUCC;
    }

    public void setSilentMode(boolean isSilentSucc) {
        this.isSilentSucc=isSilentSucc;
    }

    /**
     * set transaction type
     *
     * @param transType
     */
    public void setTransType(ETransType transType) {
        this.transType = transType;
    }

    protected void setTransListener(TransEndListener transListener) {
        this.transListener = transListener;
    }

    // AET-251
    public BaseTrans setBackToMain(boolean backToMain) {
        this.backToMain = backToMain;
        return this;
    }

    //
    public void setECRProcReturnListener(AAction.ActionEndListener listener) {
        mECRrocReturnListener = listener;
    }

    protected void ECRProcReturn(AAction action, final ActionResult result) {

        // For linkPOS
        Component.setTransDataInstance(transData);
        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        if (mECRrocReturnListener != null) {
            EcrData.instance.setEcrData(transData, sysParam, acquirer, result);
            mECRrocReturnListener.onEnd(action, result);
        }

    }
    /**
     * transaction result prompt
     */
    @Override
    protected void transEnd(final ActionResult result) {
        Component.setTransDataInstance(transData);
        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        if (mECRrocReturnListener != null) {
            EcrData.instance.setEcrData(transData, sysParam, acquirer, result);
        }

        Log.i(TAG, transType.toString() + " TRANS--END--");
        clear(); // no memory leak

        if (result.getRet() == TransResult.SUCC
             && isSilentSucc) {
            FinancialApplication.getApp().runInBackground(new DismissRunnable(result));
        } else {
            dispResult(transType.getTransName(), result, new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface arg0) {
                    FinancialApplication.getApp().runInBackground(new DismissRunnable(result));
                }
            });
        }
        setTransRunning(false);
    }

    private class DismissRunnable implements Runnable {
        private final ActionResult result;

        DismissRunnable(ActionResult result) {
            this.result = result;
        }

        @Override
        public void run() {
            removeCard();
            try {
                IPicc picc = FinancialApplication.getDal().getPicc(EPiccType.INTERNAL);
                picc.close();
            } catch (PiccDevException e) {
                Log.e(TAG, "", e);
            }

            if (transListener != null) {
                transListener.onEnd(result);
            }

            if (backToMain) {
                ActivityStack.getInstance().popTo(MainActivity.class);
            } else if (old == null) {
                ActivityStack.getInstance().pop();
            } else {
                ActivityStack.getInstance().popTo(old);
            }

            TransContext.getInstance().setCurrentAction(null);
        }

        /**
         * remove card check, need start thread when call this function
         */
        private void removeCard() {
            // avoid prompting warning message for some no card transaction, like settlement
            //AET-160
            if (!needRemoveCard)
                return;

            new ActionRemoveCard(new ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionRemoveCard) action).setParam(getCurrentContext(), transType.getTransName());
                }
            }).execute();
        }
    }

    /**
     * override execute， add function to judge whether transaction check is running and add transaction pre-deal
     */
    @Override
    public void execute() {
        Log.i(TAG, transType.toString() + " TRANS--START--");
        if (isTransRunning()) {
            setTransRunning(false);
            return;
        }
        setTransRunning(true);
        old = ActivityStack.getInstance().top();
        if (Sdk.isPaxDevice()) {
            emv = new EmvImpl().getEmv();
            clss = new ClssImpl().getClss();
        }

        // transData initial
        transData = Component.transInit();
        transData.setEcrProcess(mECRrocReturnListener != null);

        // check the last settlement, If not success, need to settle firstly.
        if (Component.chkSettlementStatus(acquirerName)) {
            transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED,  null));
            return;
        }

        ActionTransPreDeal preDealAction = new ActionTransPreDeal(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransPreDeal) action).setParam(getCurrentContext(), transType);
            }
        });
        preDealAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                transData.setTransType(transType);
                Device.enableStatusBar(false);
                Device.enableHomeRecentKey(false);

                if (transType != ETransType.SETTLE) {
                    ActionUpdateParam actionUpdateParam = new ActionUpdateParam(new ActionStartListener() {
                        @Override
                        public void onStart(AAction action) {
                            ((ActionUpdateParam) action).setParam(getCurrentContext(), false);
                        }
                    });

                    actionUpdateParam.setEndListener(new ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            if (result.getRet() != TransResult.SUCC) {
                                transEnd(result);
                                return;
                            }
                            exe();
                        }
                    });
                    actionUpdateParam.execute();
                } else {
                    exe();
                }
            }
        });
        preDealAction.execute();
    }

    /**
     * execute father execute()
     */
    private void exe() {
        super.execute();
    }

    /**
     * get transaction running status
     *
     * @return
     */
    public static boolean isTransRunning() {
        return isTransRunning;
    }

    /**
     * set transaction running status
     *
     * @param isTransRunning
     */
    public static void setTransRunning(boolean isTransRunning) {
        BaseTrans.isTransRunning = isTransRunning;
    }

    /**
     * save card information and input type after search card
     *
     * @param cardInfo
     * @param transData
     */
    public void saveCardInfo(CardInformation cardInfo, TransData transData) {
        // manual input card number
        byte mode = cardInfo.getSearchMode();
        if (mode == SearchMode.KEYIN) {
            transData.setPan(cardInfo.getPan());
            transData.setExpDate(cardInfo.getExpDate());
            transData.setEnterMode(EnterMode.MANUAL);
            transData.setIssuer(cardInfo.getIssuer());
            setAcqFromIssuer(transData);
        } else if (mode == SearchMode.SWIPE) {
            String temp = TrackUtils.getHolderName(cardInfo.getTrack1());
            if (temp!=null) {
                transData.setTrack1(Utils.splitHolderName(temp.trim()));
            }
            else
            {
                transData.setTrack1(temp);
            }
            transData.setTrack2(cardInfo.getTrack2());
            transData.setTrack3(cardInfo.getTrack3());
            transData.setPan(cardInfo.getPan());
            transData.setExpDate(TrackUtils.getExpDate(cardInfo.getTrack2()));
            transData.setEnterMode(EnterMode.SWIPE);
            transData.setIssuer(cardInfo.getIssuer());
            setAcqFromIssuer(transData);
        } else if (mode == SearchMode.INSERT || mode == SearchMode.WAVE) {
            transData.setEnterMode(mode == SearchMode.INSERT ? EnterMode.INSERT : EnterMode.CLSS);
        } else if (mode == SearchMode.QR) {
            transData.setEnterMode(EnterMode.QR);
        }
    }

    @Override
    public void gotoState(String state) {
        Log.i(TAG, transType.toString() + " ACTION--" + state + "--start");
        super.gotoState(state);
    }

    /**
     * set Acquirer from card info
     *
     * @param transData
     */
    private void setAcqFromIssuer(TransData transData){
        transData.setDccRequired(Component.isDccRequired(transData, transData.getPan(), null));

        Acquirer acquirer = Component.setAcqFromIssuer(transData);

        if (acquirer == null || !acquirer.isEnable()) {
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return;
        }

        //Check if last settlement not success
        if (Component.chkSettlementStatus(acquirer.getName())) {
            transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
        }
    }

    void saveSP200Info(EmvSP200 emvsp200) {

        String mIssuer = getIssuerBrandByAid(Convert.getInstance().strToBcd(emvsp200.getAid(), Convert.EPaddingPosition.PADDING_RIGHT));
        List<Issuer> matchedIssuerList = FinancialApplication.getAcqManager().findAllIssuerByPan(emvsp200.getPan(), mIssuer != null ? mIssuer : "");
        Issuer issuer = (matchedIssuerList != null && !matchedIssuerList.isEmpty()) ? matchedIssuerList.get(0) : null;
        issuer = isSelectIssuer(matchedIssuerList) ? matchedIssuer : issuer;
        if (issuer != null && FinancialApplication.getAcqManager().isIssuerSupported(issuer) && isCTLSSupportByIssuer(issuer)) {
            Log.i(TAG, "onConfirmCardNo[IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]");
            transData.setIssuer(issuer);
        } else {
            Log.i(TAG, issuer != null ? "onConfirmCardNo[EDC_Not_Support, IssuerName=" + issuer.getIssuerName() + ", IssuerBrand=" + issuer.getIssuerBrand() + "]" : "onConfirmCardNo[EDC_No_Issuer]");
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return;
        }

        Acquirer acquirer = Component.setAcqFromIssuer(transData);
        if (acquirer == null || !acquirer.isEnable()) {
            Log.i(TAG, "onConfirmCardNo[EDC_Not_matched_acquirer]");
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return;
        }

        //Check if last settlement not success
        if (Component.chkSettlementStatus(acquirer.getName())) {
            transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
            return;
        }

        if (!Issuer.validPan(issuer, emvsp200.getPan())) {
            transEnd(new ActionResult(TransResult.ERR_CARD_INVALID, null));
            return;
        }

        if (!Issuer.validCardExpiry(issuer, emvsp200.getExpDate())) {
            transEnd(new ActionResult(TransResult.ERR_CARD_EXPIRED, null));
            return;
        }

        if (emvsp200.getExpDate()==null && emvsp200.getTrackData()!=null) {
            try {
                String expDate = emvsp200.getTrackData().split("=")[1].substring(0,4);
                emvsp200.setExpDate(expDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        transData.setAid(emvsp200.getAid());
        transData.setClssTypeMode(emvsp200.getClssMode());
        transData.setEmvAppLabel(emvsp200.getAppLabel());
        transData.setEmvAppName(emvsp200.getAppPreferName());
        transData.setTc(emvsp200.getAppCrypto());
        transData.setTvr(emvsp200.getTvr());
        transData.setCardSerialNo(emvsp200.getPanSeqNo());
        transData.setSendIccData(emvsp200.getIccData());
        transData.setPan(emvsp200.getPan());
        transData.setExpDate(emvsp200.getExpDate());
        transData.setTrack2(emvsp200.getTrackData());
        transData.setEnterMode(TransData.EnterMode.SP200);
        transData.setTrack1(emvsp200.getHolderName());

        if (!emvsp200.isPinFree()) {//CVM Need Online PIN
            boolean isSupportOnlinePin = isSupportOnlinePinByAid(transData.getAid());
            if (isSupportOnlinePin) {//Support Online PIN
                transData.setSignFree(emvsp200.isSignFree());
                transData.setPinFree(emvsp200.isPinFree());
            } else {//Not support Online PIN
                transData.setSignFree(false);//Signature Required
                transData.setPinFree(true);//PIN Not required
            }
        } else {//CVM Not required Online PIN
            transData.setSignFree(emvsp200.isSignFree());
            transData.setPinFree(emvsp200.isPinFree());
        }
    }

    //
    // Helper Function Need to
    //

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

    private String getIssuerBrandByAid(byte[] aid) {
        List<AidParam> aidParams = clss.getAidParamList();

        for (AidParam a : aidParams) {
            if(aid!=null && Utils.bcd2Str(aid).contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                return a.getIssuerBrand();
            }
        }
        return "";

    }

    private boolean isSupportOnlinePinByAid(String aid) {
        try {
            List<AidParam> aidParams = clss.getAidParamList();

            for (AidParam a : aidParams) {
                if (aid != null && aid.contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                    return a.getOnlinePin();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error when get flag online pin from aid", ex);
        }
        return false;
    }

    private ConditionVariable cv;

    private boolean isSelectIssuer(List<Issuer> issuersList) {
        matchedIssuer = null;//clear default value
        if (issuersList != null && issuersList.size() > 1) {
            cv = new ConditionVariable();
            cv.close();
            FinancialApplication.getApp().runOnUiThread(new SelectIssuerRunnable(true, issuersList));
            cv.block();
            return true;
        }
        return false;
    }

    private Issuer matchedIssuer;

    private class SelectIssuerRunnable implements Runnable {
        private final boolean isFirstSelect;
        private final List<Issuer> issuersList;


        SelectIssuerRunnable(final boolean isFirstSelect, final List<Issuer> issuersList) {
            this.isFirstSelect = isFirstSelect;
            this.issuersList = issuersList;
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
            String[] issuers = new String[issuersList.size()];
            for (int i = 0; i < issuers.length; i++) {
                issuers[i] = issuersList.get(i).getName();
                Log.i(TAG, "SelectIssuerRunnable[issuers=" + issuers[i] + "]");
            }
            builder.setSingleChoiceItems(issuers, -1, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    matchedIssuer = issuersList.get(which);
                    Log.i(TAG, "SelectIssuerRunnable[matchedIssuer=" + matchedIssuer + "]");
                    close(dialog);
                }
            });

            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "SelectIssuerRunnable[User not select]");
                            close(dialog);
                            return;
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

    //
    // End Helper Function
    //
}
