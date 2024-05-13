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

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.action.ActionDeletePreAuthExpired;
import com.pax.pay.trans.action.ActionEReceiptInfoUpload;
import com.pax.pay.trans.action.ActionESettleReportUpload;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionSelectAcquirer;
import com.pax.pay.trans.action.ActionSettle;
import com.pax.pay.trans.action.ActionTleDownloadTmkTwk;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.settings.SysParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

public class SettleTrans extends BaseTrans {

    private ArrayList<String> selectAcqs;
    private ArrayList<Acquirer> acquirers;
    private ArrayList<MerchantProfile> merchant;

    private boolean isNeedPassword = true;
    private boolean isNeedSelectAcqs = false;
    private boolean isBypassConfirmSettle = false;
    private boolean isAutoSettle = false;
    private boolean isEcrProcess;
    private boolean isSettleAllMerchants;

    ActionResult settleResult = null;

    public SettleTrans(Context context, TransEndListener listener) {
        super(context, ETransType.SETTLE, listener);
        this.isEcrProcess = false;
        this.isSettleAllMerchants = false;
    }

    public SettleTrans(Context context, boolean isNeedPassword, String nii, TransEndListener listener) {
        super(context, ETransType.SETTLE, listener);
        this.isNeedPassword = isNeedPassword;
        this.isEcrProcess = true;
        this.isSettleAllMerchants = false;

        if (nii == null) {
            isNeedSelectAcqs = true;
        } else {
            Log.d("Acquirer:", "Start.....");
            List<Acquirer> acqList = FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true);
            selectAcqs = new ArrayList();
            for (Acquirer acquirer : acqList) {
                if (nii.equals("999")) {
                    Log.d("Acquirer:", "RequestNII[" + nii + "]:Match--->[Name=" + acquirer.getName() + ", NII=" + acquirer.getNii() + "]");
                    selectAcqs.add(acquirer.getName());
                } else if (nii.contains(acquirer.getNii())) {
                    Log.d("Acquirer:", "RequestNII[" + nii + "]:Match--->[Name=" + acquirer.getName() + ", NII=" + acquirer.getNii() + "]");
                    selectAcqs.add(acquirer.getName());
                }
            }
            Log.d("Acquirer:", "End.....");
        }
    }

    public SettleTrans(Context context, boolean isNeedPassword, boolean isAllHost, String hostName, TransEndListener listener) {
        super(context, ETransType.SETTLE, listener);
        this.isNeedPassword = isNeedPassword;
        this.isEcrProcess = true;
        this.isSettleAllMerchants = false;

        Log.d("Acquirer:", "Start.....");
        List<Acquirer> acqList = FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true);
        selectAcqs = new ArrayList();
        for (Acquirer acquirer : acqList) {
            if (isAllHost) {
                Log.d("Acquirer:", "RequestNII[" + acquirer.getNii() + "]:Match--->[Name=" + acquirer.getName() + ", NII=" + acquirer.getNii() + "]");
                selectAcqs.add(acquirer.getName());
            }
            else if (hostName.equals(acquirer.getName())) {
                Log.d("Acquirer:", "RequestNII[" + acquirer.getNii() + "]:Match--->[Name=" + acquirer.getName() + ", NII=" + acquirer.getNii() + "]");
                selectAcqs.add(acquirer.getName());
            }
        }
        Log.d("Acquirer:", "End.....");
    }

    public SettleTrans(Context context, boolean isNeedPassword, boolean isBypassConfirmSettle, boolean isNeedSelectAcqs, String hostName, TransEndListener listener) {
        super(context, ETransType.SETTLE, listener);
        this.isNeedPassword = isNeedPassword;
        this.isNeedSelectAcqs = isNeedSelectAcqs;
        this.isBypassConfirmSettle = isBypassConfirmSettle;
        this.isSettleAllMerchants = false;

        selectAcqs = new ArrayList();
        selectAcqs.add(hostName);

        new SettleTrans(context, isNeedPassword, false, hostName, listener);
    }
    public SettleTrans(Context context, boolean isNeedPassword, boolean isBypassConfirmSettle, boolean isNeedSelectAcqs, ArrayList<String> hostNameList, TransEndListener listener) {
        super(context, ETransType.SETTLE, listener);
        this.isNeedPassword = isNeedPassword;
        this.isNeedSelectAcqs = isNeedSelectAcqs;
        this.isBypassConfirmSettle = isBypassConfirmSettle;
        this.isSettleAllMerchants = !hostNameList.isEmpty();

        selectAcqs = hostNameList;
    }

    public SettleTrans(Context context, boolean isNeedPassword, boolean isBypassConfirmSettle, boolean isNeedSelectAcqs, ArrayList<String> hostNameList, TransEndListener listener, Boolean isAutoSettle) {
        super(context, ETransType.SETTLE, listener);
        this.isNeedPassword = isNeedPassword;
        this.isNeedSelectAcqs = isNeedSelectAcqs;
        this.isBypassConfirmSettle = isBypassConfirmSettle;
        this.isAutoSettle = isAutoSettle;
        this.isSettleAllMerchants = false;

        selectAcqs = hostNameList;
    }

    @Override
    protected void bindStateOnAction() {

        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(getCurrentContext(), 6,
                        getString(R.string.prompt_settle_pwd), null);
            }
        });
        bind(State.INPUT_PWD.toString(), inputPasswordAction);

        ActionSelectAcquirer actionSelectAcquirer = new ActionSelectAcquirer(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSelectAcquirer) action).setParam(getCurrentContext(),
                        getString(R.string.settle_select_acquirer));
            }
        });
        bind(State.SELECT_ACQ.toString(), actionSelectAcquirer, true);

        // EReceipt & ESignature Upload
        ActionEReceiptInfoUpload eReceiptInfoUploadAction = new ActionEReceiptInfoUpload(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEReceiptInfoUpload) action).setParam(getCurrentContext(), acquirers, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_PRE_SETTLE);
            }
        });
        bind(State.SIGNATURE_UPLOAD.toString(), eReceiptInfoUploadAction, false);

        ActionSettle settleAction = new ActionSettle(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
                    ((ActionSettle) action).setParam(getCurrentContext(),
                            getString(R.string.menu_settle), selectAcqs, isEcrProcess, (isBypassConfirmSettle || isAutoSettle), isSettleAllMerchants, true);
                } else {
                    ((ActionSettle) action).setParam(getCurrentContext(),
                            getString(R.string.menu_settle), selectAcqs, isEcrProcess, (isBypassConfirmSettle || isAutoSettle), isSettleAllMerchants, false);
                }
            }

        });
        bind(State.SETTLE.toString(), settleAction, false);

        ActionDeletePreAuthExpired actionDeletePreAuthExpired = new ActionDeletePreAuthExpired(action ->
                ((ActionDeletePreAuthExpired) action).setParam(getCurrentContext())
        );
        bind(State.DELETE_PRE_AUTH_EXPIRED.toString(), actionDeletePreAuthExpired);

//        ActionEReceiptInfoUpload ErmSettleReport = new ActionEReceiptInfoUpload(new ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionEReceiptInfoUpload) action).setParam(getCurrentContext(), ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_REPORT_FROM_FILE);
//            }
//        });
//        bind(State.UPLOAD_ERM_SETTLEMENT_REPORT.toString(), ErmSettleReport, false);

        ActionESettleReportUpload ermSettleReport = new ActionESettleReportUpload(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionESettleReportUpload) action).setParam(FinancialApplication.getApp().getApplicationContext(), settleResult.getRet());
            }
        });
        bind(State.UPLOAD_ERM_SETTLEMENT_REPORT.toString(), ermSettleReport, false);


        ActionTleDownloadTmkTwk actiontleDownloadTwk = new ActionTleDownloadTmkTwk(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTleDownloadTmkTwk) action).setParam(getCurrentContext(), LoadTLETrans.Mode.DownloadTWK, null, settleResult.getRet());
            }
        });
        bind(State.RELOAD_TWK.toString(), actiontleDownloadTwk, false);

        //结算是否需要输入密码
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY) && isNeedPassword) {
            gotoState(State.INPUT_PWD.toString());
        } else if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY) && (!isNeedPassword) && isNeedSelectAcqs) {
            gotoState(State.SELECT_ACQ.toString());
            // gotoState(State.SETTLE.toString());
        } else {
            if (checkEReceiptUpload()) {
                gotoState(State.SIGNATURE_UPLOAD.toString());
            } else {
                gotoState(State.SETTLE.toString());
            }
        }
    }

    enum State {
        INPUT_PWD,
        SELECT_ACQ,
        SIGNATURE_UPLOAD,
        SETTLE,
        DELETE_PRE_AUTH_EXPIRED,
        RELOAD_TWK,
        UPLOAD_ERM_SETTLEMENT_REPORT
    }

    @Override
    public void gotoState(String state) {
        if (state.equals(State.INPUT_PWD.toString())) {
            EcrData.instance.isOnHomeScreen = false;
        }
        super.gotoState(state);
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        State state = State.valueOf(currentState);
        switch (state) {
            case INPUT_PWD:
                if (result.getRet() != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true;
                    transEnd(result);
                }
                else {
                    String data = EncUtils.sha1((String) result.getData());
                    if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_SETTLE_PWD))) {
                        if (selectAcqs != null)
                            selectAcqs.clear();
                        EcrData.instance.isOnHomeScreen = true;
                        transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
                        return;
                    }
                    gotoState(State.SELECT_ACQ.toString());
                }
                break;
            case SELECT_ACQ:
                //noinspection unchecked
                selectAcqs = (ArrayList<String>) result.getData();

                if (checkEReceiptUpload()) {
                    gotoState(State.SIGNATURE_UPLOAD.toString());
                } else {
                    gotoState(State.SETTLE.toString());
                }
                break;
            case SIGNATURE_UPLOAD:
                if (result.getRet() != TransResult.SUCC) {
                    if (isAutoSettle) { Printer.printEReceiptPreSettleUploadFailed(ActivityStack.getInstance().top()); }
                    transEnd(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
                    return;
                }

                if (selectAcqs == null || selectAcqs.isEmpty()) {
//                    if (isAutoSettle) {Printer.printSettleHostNotfound(ActivityStack.getInstance().top());}
                    transEnd(new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null));
                } else {
                    gotoState(State.SETTLE.toString());
                }
                break;
            case SETTLE:
                if (result.getRet() == TransResult.ERR_USER_CANCEL && !isEcrProcess) {
                    gotoState(State.SELECT_ACQ.toString());
                }
                else if (isEcrProcess && (result.getRet() == TransResult.ERR_USER_CANCEL
                        || result.getRet() == TransResult.ERR_HOST_NOT_FOUND
                        || result.getRet() == TransResult.ERR_NO_TRANS)) {
                    transEnd(result);
                }
                else {
                    settleResult = result;
                    gotoState(State.DELETE_PRE_AUTH_EXPIRED.toString());
                }
                break;
            case DELETE_PRE_AUTH_EXPIRED:
                if (MultiMerchantUtils.Companion.isMasterMerchant()) {
                    gotoState(State.RELOAD_TWK.toString());
                } else {
                    afterReloadTwk();
                }
                break;
            case RELOAD_TWK:
                afterReloadTwk();
                break;
            case UPLOAD_ERM_SETTLEMENT_REPORT:
                transEnd(settleResult);
                break;
        }
    }

    private void afterReloadTwk() {
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
            gotoState(State.UPLOAD_ERM_SETTLEMENT_REPORT.toString());
        } else {
            transEnd(settleResult);
        }
    }
    

    boolean isAmexNeedUploadErm = false;
    boolean isKcheckIDNeedUploadErm = false;
    private boolean checkEReceiptUpload() {
        acquirers = new ArrayList<>();
        for (String acqName : selectAcqs) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acqName);
            if (acquirer != null) {
                if (acqName.equals(Constants.ACQ_AMEX)){
                    if (getExternalAppPendingUploadCount(acquirer) > 0) {
                        isAmexNeedUploadErm=true;
                        acquirers.add(acquirer);
                    }
                } else if (acqName.equals(Constants.ACQ_KCHECKID)){
                    if (getExternalAppPendingUploadCount(acquirer) > 0) {
                        isKcheckIDNeedUploadErm=true;
                        acquirers.add(acquirer);
                    }
                } else {
                    acquirers.add(acquirer);
                }
            }
        }
        return (FinancialApplication.getTransDataDbHelper().findAllEReceiptPending(acquirers) > 0) || (isAmexNeedUploadErm) || (isKcheckIDNeedUploadErm);
    }

    private int getExternalAppPendingUploadCount(Acquirer acquirer) {
        String path =  EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, acquirer);
        File dir = new File(path);
        int fileCounter = 0;
        if (dir.exists() && dir.isDirectory()) {
            if (dir.listFiles().length > 0) {
                for (File file: dir.listFiles()) {
                    if (file.getName().endsWith(".erm") && file.isFile()) {
                        fileCounter +=1 ;
                    }
                }
            }
        }

        return fileCounter;
    }
}
