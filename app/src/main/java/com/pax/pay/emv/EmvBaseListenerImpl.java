/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-20
 * Module Author: linhb
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.emv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.ConditionVariable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import th.co.bkkps.utils.Log;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.TrackUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IEmvBase;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.gl.pack.ITlv;
import com.pax.gl.pack.exception.TlvException;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.emv.api.EMVApi;
import com.pax.jemv.emv.api.EMVCallback;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterAuthCode;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;


public class EmvBaseListenerImpl {
    private static final String TAG = EmvBaseListenerImpl.class.getSimpleName();
    protected Context context;
    protected ConditionVariable cv;
    protected int intResult;

    protected TransData transData;
    private IEmvBase emvBase;
    protected TransProcessListener transProcessListener;

    protected Issuer matchedIssuer;

    protected EmvBaseListenerImpl(Context context, IEmvBase emvBase, TransData transData, TransProcessListener listener) {
        this.context = context;
        this.transData = transData;
        this.emvBase = emvBase;
        this.transProcessListener = listener;
    }

    protected int updateTransDataFromKernel() {
        ETransType transType = transData.getTransType();
        String pan = transData.getPan();

        // read ARQC
        byte[] arqc = emvBase.getTlv(0xF26);
        if (arqc != null && arqc.length > 0) {
            transData.setArqc(Utils.bcd2Str(arqc));
        }

        // generate field 55 data
        byte[] f55 = EmvTags.getF55(emvBase, transType, false, pan);
        byte[] f55Dup = EmvTags.getF55(emvBase, transType, true, pan);

        // reversal process
        int ret = new Transmit().sendReversal(transProcessListener, transData.getAcquirer());

        transData.setSendIccData(Utils.bcd2Str(f55));
        if (f55Dup.length > 0) {
            transData.setDupIccData(Utils.bcd2Str(f55Dup));
        }

        return ret ;
    }

    protected void updateTransDataFromResp(ITlv.ITlvDataObjList list) throws EmvException {
        byte[] value91 = list.getValueByTag(0x91);
        if (value91 != null && value91.length > 0) {
            emvBase.setTlv(0x91, value91);
        }
        // set script 71
        byte[] value71 = list.getValueByTag(0x71);
        if (value71 != null && value71.length > 0) {
            emvBase.setTlv(0x71, value71);
        }

        // set script 72
        byte[] value72 = list.getValueByTag(0x72);
        if (value72 != null && value72.length > 0) {
            emvBase.setTlv(0x72, value72);
        }
    }

    protected void updateTransDataFromRespAmex(ITlv.ITlvDataObjList list) throws EmvException {
        // set script 91
        byte[] value91 = list.getValueByTag(0x91);
        if (value91 != null && value91.length > 0) {
            emvBase.setTlv(0x91, value91);
            // set script 8A
            byte[] value8A = new byte[2];
            System.arraycopy(value91, value91.length - 2, value8A, 0, 2);
            emvBase.setTlv(0x8A, value8A);
        } else {//If tag 91 (Issuer Authentication data is not present)
            // set script 8A
            switch (transData.getResponseCode().getCode()) {
                case "00":
                    emvBase.setTlv(0x8A, "00".getBytes());
                    break;
                case "01":
                    emvBase.setTlv(0x8A, "02".getBytes());
                    break;
                default:
                    emvBase.setTlv(0x8A, "05".getBytes());
                    break;
            }
        }

        // set script 71
        byte[] value71 = list.getValueByTag(0x71);
        if (value71 != null && value71.length > 0) {
            emvBase.setTlv(0x71, value71);
        }

        // set script 72
        byte[] value72 = list.getValueByTag(0x72);
        if (value72 != null && value72.length > 0) {
            emvBase.setTlv(0x72, value72);
        }
    }

    protected EOnlineResult onlineProc() {
        try {
            int ret = updateTransDataFromKernel();
            if(ret != TransResult.SUCC){
                return EOnlineResult.REVERSAL_FAILED;
            }

            // online process
            boolean onlineDenial = false;
            if (transProcessListener != null) {
                //update STAN if reversal success
                String title = transProcessListener.getTitle();
                if(title != null && title.equals(Utils.getString(R.string.prompt_reverse))){
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                }
                transProcessListener.onUpdateProgressTitle(transData.getTransType().getTransName());
            }
            ret = new Online().online(transData, transProcessListener);
            Log.i(TAG, "Online  ret = " + ret);
            if (ret == TransResult.SUCC) {
                if (!"00".equals(transData.getResponseCode().getCode())) {
                    onlineDenial = true; // 联机拒绝
                }
            }
            else if (ret == TransResult.ERR_TLE_NOT_LOAD)
            {
                return EOnlineResult.TLE_FAILED;
            }
            else {
                // EDCBBLAND-383 Show error message when transaction failed during online transaction (not host reject)
                transProcessListener.onShowErrMessage(TransResultUtils.getMessage(ret), Constants.FAILED_DIALOG_SHOW_TIME, true);
                try{
                    if(ret != TransResult.ERR_RECV && getAidTlvTag().contains(Constants.AMEX_AID_PREFIX) && transData.getIssuer().getFloorLimit() == 0) {
                        //Only AMEX, If floor limit = 0, need to go the 2nd generate AC
                        return EOnlineResult.AMEX_ERR_GOTO_SECOND_GEN;
                    }

                    if(Long.parseLong(transData.getAmount()) <= transData.getIssuer().getFloorLimit()){
                        transData.setOrigAuthCode(Utils.getString( R.string.response_Y3_str));
                        transData.setAuthCode(Utils.getString( R.string.response_Y3_str));
                        // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                        return EOnlineResult.ONLINE_FAILED_NEED_CHK_REVERSAL;
                        //return EOnlineResult.FAILED;
                    }else{
                        return EOnlineResult.ABORT;
                    }
                }catch (Exception e){
                    Log.e(TAG, "", e);
                    return EOnlineResult.ABORT;
                }


                //***return EOnlineResult.ABORT;
            }

            String rspF55 = transData.getRecvIccData();
            Log.i(TAG, "rspF55 = " + rspF55);
            ITlv tlv = FinancialApplication.getPacker().getTlv();

            if (rspF55 != null && !rspF55.isEmpty()) {
                byte[] resp55 = Utils.str2Bcd(rspF55);
                //FIXME unpack may block the app
                if(getAidTlvTag().contains(Constants.AMEX_AID_PREFIX)) {
                    try {
                        ITlv.ITlvDataObjList list = EmvTags.unpackRspF55Amex(resp55);
                        updateTransDataFromRespAmex(list);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        return EOnlineResult.FAILED;
                    }
                } else {
                    ITlv.ITlvDataObjList list = tlv.unpack(resp55);
                    updateTransDataFromResp(list);
                }
            }

            if (onlineDenial) {
                FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                Device.beepErr();
                //AET-146 :ignore display error in online process, display in emv process
                return EOnlineResult.DENIAL;
            }
            // set auth code
            String authCode = transData.getAuthCode();
            if (authCode != null && !authCode.isEmpty()) {
                emvBase.setTlv(0x89, authCode.getBytes());
            }
            emvBase.setTlv(0x8A, "00".getBytes());
            // write transaction record
            transData.setOnlineTrans(true);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            return EOnlineResult.APPROVE;

        } catch (EmvException | TlvException e) {
            Log.e(TAG, "", e);
        } finally {
            if (transProcessListener != null)
                transProcessListener.onHideProgress();
        }

        return EOnlineResult.FAILED;
    }

    protected void enterPin(boolean isOnlinePin, int offlinePinLeftTimes, boolean isClss) {
        final String header;
        final String subHeader = context.getString(R.string.prompt_no_pin);

        final String totalAmount = transData.getTransType().isSymbolNegative() ? "-" + transData.getAmount() : transData.getAmount();
        final String tipAmount = transData.getTransType().isSymbolNegative() ? null : transData.getTipAmount();

        transData.setPinFree(false);
        if (isOnlinePin) { // online PIN
            header = context.getString(R.string.prompt_pin);
            doOnlineAction(header, subHeader, totalAmount, tipAmount, isClss);

        } else {
            header = context.getString(R.string.prompt_pin) + "(" + offlinePinLeftTimes + ")";
            doOfflineAction(header, subHeader, totalAmount, tipAmount, isClss);
        }
    }

    private void doOnlineAction(final String header, final String subHeader,
                                final String totalAmount, final String tipAmount, final boolean isClss) {
        ActionEnterPin actionEnterPin = new ActionEnterPin(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {

                byte[] track2 = emvBase.getTlv(TagsTable.TRACK2);
                if (track2 == null && isClss) {
                    track2 = emvBase.getTlv(TagsTable.TRACK2_1);
                }
                String strTrack2 = TrackUtils.getTrack2FromTag57(track2, true);
                String pan = TrackUtils.getPan(strTrack2);

                ((ActionEnterPin) action).setParam(context, transData.getTransType()
                                .getTransName(), pan, true, header, subHeader,
                        totalAmount, tipAmount, ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
            }
        });

        actionEnterPin.setEndListener(new OnlineEndAction());
        actionEnterPin.execute();
    }

    private class OnlineEndAction implements AAction.ActionEndListener {
        @Override
        public void onEnd(AAction action, ActionResult result) {
            int ret = result.getRet();
            if (ret == TransResult.SUCC) {
                String data = (String) result.getData();
                transData.setPin(data);
                if (data != null && !data.isEmpty()) {
                    transData.setHasPin(true);
                    intResult = EEmvExceptions.EMV_OK.getErrCodeFromBasement();
                } else {
                    intResult = EEmvExceptions.EMV_ERR_NO_PASSWORD.getErrCodeFromBasement(); // bypass
                }
            } else {
                intResult = EEmvExceptions.EMV_ERR_RSP.getErrCodeFromBasement();
            }
            if (cv != null)
                cv.open();
            ActivityStack.getInstance().popTo((Activity) context);
        }
    }

    private void doOfflineAction(final String header, final String subHeader,
                                 final String totalAmount, final String tipAmount, final boolean isClss) {
        ActionEnterPin actionEnterPin = new ActionEnterPin(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {

                byte[] holderNameBCD = emvBase.getTlv(0x5F20);
                byte[] track2 = emvBase.getTlv(TagsTable.TRACK2);
                if (track2 == null && isClss) {
                    track2 = emvBase.getTlv(TagsTable.TRACK2_1);
                }
                String strTrack2 = TrackUtils.getTrack2FromTag57(track2, true);
                String pan = TrackUtils.getPan(strTrack2);
                String cardholder = new String(holderNameBCD);
                cardholder = Utils.splitHolderName(cardholder.trim());
                transData.setPan(pan);
                transData.setTrack1(cardholder);

                ((ActionEnterPin) action).setParam(context, transData.getTransType()
                                .getTransName(), pan, true, header, subHeader,
                        totalAmount, tipAmount, ActionEnterPin.EEnterPinType.OFFLINE_PCI_MODE, transData);
            }
        });

        actionEnterPin.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (cv != null)
                    cv.open();
                ActivityStack.getInstance().popTo((Activity) context);
            }
        });
        actionEnterPin.execute();
    }

    private String getAidTlvTag() {
        byte[] aid = emvBase.getTlv(0x4F);
        return (aid != null && aid.length > 0) ? Utils.bcd2Str(aid) : "";
    }

    protected int setF55ForReversal() throws EmvException {
        int ret = 0;
        //Only Diners card will update tag95, 9F10, 9F26 and send reversal after card decline in 2nd generate AC
        if (getAidTlvTag().contains(Constants.DINERS_AID_PREFIX)) {
            ByteArray tag95 = new ByteArray();
            if (EMVApi.EMVGetTLVData((short) 0x95, tag95) == RetCode.EMV_OK) {
                byte[] value = new byte[tag95.length];
                System.arraycopy(tag95.data, 0, value, 0, tag95.length);
                emvBase.setTlv(0x95, value);
            }

            ByteArray tag9F10 = new ByteArray();
            if (EMVApi.EMVGetTLVData((short) 0x9F10, tag9F10) == RetCode.EMV_OK) {
                byte[] value = new byte[tag9F10.length];
                System.arraycopy(tag9F10.data, 0, value, 0, tag9F10.length);
                emvBase.setTlv(0x9F10, value);
            }

            ByteArray tag9f26 = new ByteArray();
            if (EMVApi.EMVGetTLVData((short) 0x9F26, tag9f26) == RetCode.EMV_OK) {
                byte[] value = new byte[tag9f26.length];
                System.arraycopy(tag9f26.data, 0, value, 0, tag9f26.length);
                emvBase.setTlv(0x9F26, value);
            }

            byte[] f55 = EmvTags.getF55(emvBase, transData.getTransType(), true, transData.getPan());
            transData.setSendIccData(Utils.bcd2Str(f55));
            transData.setDupIccData(Utils.bcd2Str(f55));
            transData.setReversalStatus(TransData.ReversalStatus.PENDING);
            transData.setDupReason(TransData.DUP_REASON_OTHERS);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        return ret;
    }

    protected EOnlineResult updateScriptResult() {
        try {
            int ret = 0;

            ByteArray scriptResult = new ByteArray();
            int retScriptResult = EMVCallback.EMVGetScriptResult(scriptResult);
            //todo: kbank, no need to sent update script back to host
            return EOnlineResult.APPROVE;
            /*if(retScriptResult == RetCode.EMV_OK){
                // Send Update ScriptResult Message.
                String acqName = transData.getAcquirer().getName();
                if(acqName.equals(Constants.ACQ_UP)){
                    // online process
                    if (transProcessListener != null) {
                        byte[] f55 = EmvTags.getF55(emvBase, transData.getTransType(), false, transData.getPan());
                        int retScript = new Transmit().sendIssuerScriptUpdate(transProcessListener,transData, f55);
                        if(retScript == RetCode.EMV_OK){
                            return EOnlineResult.APPROVE;
                        }
                    }
                } else {// EDCBBLAND-447 For Other Issuers, no need to update script result and always return APPROVE.
                    return EOnlineResult.APPROVE;
                }
            }*/
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            if (transProcessListener != null)
                transProcessListener.onHideProgress();
        }

        return EOnlineResult.FAILED;
    }

    protected void doEnterAuthCodeAction() {
        ActionEnterAuthCode enterAuthCodeAction = new ActionEnterAuthCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterAuthCode) action).setParam(context,
                        Utils.getString(R.string.menu_offline),
                        Utils.getString(R.string.prompt_auth_code),
                        transData.getAmount());
            }
        });
        enterAuthCodeAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                int ret = result.getRet();
                if (ret == TransResult.SUCC) {
                    String data = (String) result.getData();
                    transData.setOrigAuthCode(data);
                    transData.setAuthCode(data);
                    intResult = EEmvExceptions.EMV_OK.getErrCodeFromBasement();
                } else if (ret == TransResult.ERR_USER_CANCEL) {
                    intResult = EEmvExceptions.EMV_ERR_USER_CANCEL.getErrCodeFromBasement();
                } else if (ret == TransResult.ERR_TIMEOUT) {
                    intResult = EEmvExceptions.EMV_ERR_TIMEOUT.getErrCodeFromBasement();
                } else {
                    intResult = EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement();
                }
                cv.open();
            }
        });
        enterAuthCodeAction.execute();
    }

    protected void doEnterRefNoAction(final String title) {
        ActionInputTransData enterRefNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(context, title)
                        .setInputLine1(Utils.getString(R.string.prompt_input_orig_refer), ActionInputTransData.EInputType.TRANSID, 12, 1, false, false);
            }
        });
        enterRefNoAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                int ret = result.getRet();
                if (ret == TransResult.SUCC) {
                    String data = (String) result.getData();
                    transData.setOrigRefNo(data);
                    transData.setRefNo(data);
                    intResult = EEmvExceptions.EMV_OK.getErrCodeFromBasement();
                } else if (ret == TransResult.ERR_USER_CANCEL) {
                    intResult = EEmvExceptions.EMV_ERR_USER_CANCEL.getErrCodeFromBasement();
                } else if (ret == TransResult.ERR_TIMEOUT) {
                    intResult = EEmvExceptions.EMV_ERR_TIMEOUT.getErrCodeFromBasement();
                } else {
                    intResult = EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement();
                }
                cv.open();
                ActivityStack.getInstance().popTo((Activity) context);
            }
        });
        enterRefNoAction.execute();
    }

    protected boolean isSelectIssuer(List<Issuer> issuersList) {
        matchedIssuer = null;//clear default value
        if (issuersList != null && issuersList.size() > 1) {
            cv = new ConditionVariable();
            FinancialApplication.getApp().runOnUiThread(new SelectIssuerRunnable(true, issuersList));
            cv.block();
            return true;
        }
        return false;
    }

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
}
