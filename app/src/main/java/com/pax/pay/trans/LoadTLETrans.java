package com.pax.pay.trans;

import android.app.ProgressDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.RSARecoverInfo;
import com.pax.device.TerminalEncryptionParam;
import com.pax.device.UserParam;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Bank;
import com.pax.pay.constant.Constants;
import com.pax.pay.ped.PedManager;
import com.pax.pay.splash.SplashListener;
import com.pax.pay.trans.action.ActionEnterTeId;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.action.ActionScbLoadTle;
import com.pax.pay.trans.action.ActionSelectTleBankHost;
import com.pax.pay.trans.action.ActionTleDownloadTmkTwk;
import com.pax.pay.trans.action.activity.EnterTeIdActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.component.KeyDataReadWriteJson;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.IReceiptGenerator;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.io.File;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import th.co.bkkps.amexapi.action.ActionAmexLoadLogOnTle;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;
import th.co.bkkps.scbapi.util.ScbUtil;
import th.co.bkkps.utils.Log;

public class LoadTLETrans extends BaseTrans {

    private ArrayList<String> selectedTleBankHost;
    private ArrayList<String> scbSelectAcquirer = new ArrayList<>();

    int AcquireIdx = 0;
    int successTLE = 0;

    String field62;
    String originalMerchantName = null ;
    boolean isRequireRevertToOrigMerc = false;
    boolean scbTle = false;
    boolean amexTle = false;
    boolean isAutoDownloadMode = false;
    ProgressDialog progressDialog = null;
    SplashListener updateUIListner = null;

    public enum Mode {None, DownloadTMK, DownloadTWK}
    private Mode mode = Mode.None;
    private Context context =null;

    public LoadTLETrans(Context context, TransEndListener listener, Mode mode) {
        super(context, ETransType.LOADTMK, listener);
        isAutoDownloadMode = false;
        this.context=context;
        this.mode=mode;
    }

    public LoadTLETrans(Context context, TransEndListener listener, Mode mode, boolean isAutoDownloadMode) {
        super(context, ETransType.LOADTMK, listener);
        super.setSilentMode(isAutoDownloadMode);
        this.isAutoDownloadMode = isAutoDownloadMode;
        this.context=context;
        this.mode=mode;
    }

    public void setSplashEndListener (SplashListener updateUIListner) {
        this.updateUIListner = updateUIListner;
    }


    @Override
    protected void bindStateOnAction() {
        ActionSelectTleBankHost actionSelectTleBankHost = new ActionSelectTleBankHost(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionSelectTleBankHost) action).setParam(getCurrentContext(), getString(R.string.tle_select_acquirer), getString(R.string.trans_tle_load));
            }
        });
        bind(LoadTLETrans.State.SELECT_ACQ.toString(), actionSelectTleBankHost, true);


        ActionEnterTeId enterTeId = new ActionEnterTeId(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterTeId) action).setParam(getCurrentContext(), getString(R.string.tle_te_title), scbTle, isAutoDownloadMode);
            }
        });
        bind(LoadTLETrans.State.ENTER_TEID.toString(), enterTeId, false);


        ActionScbUpdateParam scbUpdateParam = new ActionScbUpdateParam(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) { ((ActionScbUpdateParam) action).setParam(context);  }
        });
        bind(State.LOAD_SCB_JSON.toString(), scbUpdateParam, false);


        ActionScbLoadTle scbLoadTle = new ActionScbLoadTle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionScbLoadTle) action).setParam(context, getTeidFromFile(), scbSelectAcquirer);
            }
        });
        bind(State.Load_SCB_TLE.toString(), scbLoadTle, false);

        ActionAmexLoadLogOnTle amexLoadTle = new ActionAmexLoadLogOnTle(action -> (
                (ActionAmexLoadLogOnTle) action).setParam(context, getTeidFromFile())
        );
        bind(State.LOAD_AMEX_TLE.toString(), amexLoadTle, false);

        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                TleStatusTrans tleStatusTrans = new TleStatusTrans(null, null);
                String[] statusMsg = tleStatusTrans.getStatusReportForPrint().toArray(new String[0]);

                ((ActionPrintTransMessage) action).setParam(getCurrentContext(), statusMsg, null);
            }
        });
        bind(State.PRINT.toString(), printReceiptAction, true);

        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            originalMerchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            String masterMerchantName = MultiMerchantUtils.Companion.getMasterProfileName();
            isRequireRevertToOrigMerc = !(originalMerchantName.equals(masterMerchantName));

            if (masterMerchantName !=null && !masterMerchantName.isEmpty()) {
                MerchantProfileManager.INSTANCE.RestoreToSpecificMerchant(masterMerchantName);
            } else {
                transEnd(new ActionResult(TransResult.MULTI_MERCHANT_MASTER_PROFILE_MISSING, null));
            }
        }

        if (isAutoDownloadMode) {
            // set primaryAcquirerNameList = all acquirer
            updateUIListner.onUpdataUI("TLE Process", "Please wait, download stating...");
            doAutoDownloadTLEbySingleHost(Utils.getTLEPrimaryAcquirerList());
        } else {
            gotoState(LoadTLETrans.State.SELECT_ACQ.toString());
        }

    }

    enum State {
        SELECT_ACQ,
        ENTER_TEID,
        LOAD_SCB_JSON,
        Load_SCB_TLE,
        LOAD_AMEX_TLE,
        LOAD_TMK,
        LOAD_TWK,
        PRINT
    }

    private void forceProcessMode (String currentState) {
        if (currentState.equals(State.LOAD_TMK.toString())) {
            processLoadTMK(State.LOAD_TMK.toString());
        } else {
            processLoadTWK(State.LOAD_TWK.toString());
        }
    }
    private void processModeSelection (Mode exMode) {
        if (exMode==Mode.DownloadTMK) {
            processLoadTMK(State.LOAD_TMK.toString());
        } else {
            processLoadTWK(State.LOAD_TWK.toString());
        }
    }

//    @Override
//    public void onActionResult(String currentState, ActionResult result) {
//        LoadTLETrans.State state = LoadTLETrans.State.valueOf(currentState);
//        switch (state) {
//            case SELECT_ACQ:
//                selectedTleBankHost = (ArrayList<String>) result.getData();
//                if (selectedTleBankHost.size() > 0) {
//                    // got list of TLE Bank selected by user at least 1 Host
//                    int resultGetTEID = autoGetTeidFromFile();
//                    if (resultGetTEID==TransResult.SUCC) {
//                        processModeSelection(mode);
//                    } else {
//                        gotoState(State.ENTER_TEID.toString());
//                    }
//                } else {
//                    transEnd(new ActionResult(TransResult.ERR_TLE_NOT_LOAD,null));
//                }
//                break;
//            case ENTER_TEID:
//                if (result.getRet() == TransResult.SUCC) {
//                    processModeSelection(mode);
//                } else {
//                    transEnd(result);
//                }
//            case LOAD_TMK:
//                if (result.getRet() == TransResult.SUCC) {
//                    forceProcessMode(State.LOAD_TWK.toString());
//                } else {
//                    transEnd(result);
//                }
//                break;
//            case LOAD_TWK:
//                if (result.getRet() == TransResult.SUCC) {
//                    gotoState(State.PRINT.toString());
//                } else {
//                    transEnd(result);
//                }
//                break;
//            case PRINT:
//                transEnd(new ActionResult(TransResult.SUCC, null));
//                break;
//            default:
//                transEnd(result);
//                break;
//        }
//    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        LoadTLETrans.State state = LoadTLETrans.State.valueOf(currentState);
        switch (state) {
            case SELECT_ACQ:
                afterSelectAcquirer(result);
                break;
            case ENTER_TEID:
                if (result.getRet() == TransResult.SUCC) {
                    if (!scbTle && !amexTle) {
                        processModeSelection(mode);
                    } else if (scbTle) { // do scb first
                        gotoState(State.LOAD_SCB_JSON.toString());
                    } else if (amexTle) { // then amex
                        gotoState(State.LOAD_AMEX_TLE.toString());
                    }
                } else {
                    transEnd(result);
                }
                break;
            case LOAD_SCB_JSON:
                if (result.getRet()==TransResult.SUCC) {
                    Log.d(TAG, " Load Json-data from file successful");
                    gotoState(State.Load_SCB_TLE.toString());
                } else {
                    Log.d(TAG, " Failed to Load Json-data from file");
                    transEnd(result);
                }
                break;
            case Load_SCB_TLE:
                if (result.getRet()==TransResult.SUCC) {
                    if (selectedTleBankHost.size() > 1) {
                        Log.d(TAG, " SCB-IPP / SCB-REDEEM result success,  with multiple host download");
                        Log.d(TAG, "     --> download consequent host");
                        selectedTleBankHost.remove(Utils.TLE_BANK_NAME___SCB);
                        if (amexTle) {
                            gotoState(State.LOAD_AMEX_TLE.toString());
                        } else {
                            processModeSelection(mode);
                        }
                    } else {
                        Log.d(TAG, " SCB-IPP / SCB-REDEEM result success,  with single SCB host download");
                        transEnd(result);
                    }
                } else {
                    Log.d(TAG, " Failed to download SCB TLE");
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                }
                break;
            case LOAD_AMEX_TLE:
                if (result.getRet()==TransResult.SUCC) {
                    if (selectedTleBankHost.size() > 1) {
                        Log.d(TAG, " AMEX result success,  with multiple host download");
                        Log.d(TAG, "     --> download consequent host");
                        selectedTleBankHost.remove(Utils.TLE_BANK_NAME___AMEX);
                        processModeSelection(mode);
                    } else {
                        Log.d(TAG, " AMEX result success,  with single AMEX host download");
                        transEnd(result);
                    }
                } else {
                    Log.d(TAG, " Failed to download AMEX TLE");
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                }
                break;
            case LOAD_TMK:
                if (result.getRet() == TransResult.SUCC) {
                    // store temporary backup file
                    Log.d(TAG, " TMK download success");
                    KeyDataReadWriteJson.saveKeyDataToFile();
                    forceProcessMode(State.LOAD_TWK.toString());
                } else {
                    Log.d(TAG, " Failed to download TMK");
                    transEnd(result);
                }
                break;
            case LOAD_TWK:
                if (result.getRet() == TransResult.SUCC) {
                    Log.d(TAG, " TWK download success");
                    gotoState(State.PRINT.toString());
                } else {
                    Log.d(TAG, " Failed to download TWK");
                    transEnd(result);
                }
                break;
            case PRINT:
                Log.d(TAG, " TLE-Report was print");
                transEnd(new ActionResult(TransResult.SUCC, null));
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void afterSelectAcquirer (ActionResult result) {
        selectedTleBankHost = (ArrayList<String>) result.getData();
        if (!selectedTleBankHost.isEmpty()) {
            // got list of TLE Bank selected by user at least 1 Host
            int resultGetTEID = autoGetTeidFromFile();
            if (resultGetTEID==TransResult.SUCC) {
                if (selectedTleBankHost.contains(Utils.TLE_BANK_NAME___SCB)) {
                    Log.d(TAG, " Selected list contain SCB-IPP / SCB-REDEEM");
                    ArrayList<String> scbActiveAcquirer = ScbUtil.INSTANCE.getScbActiveTleAcquirer();
                    if (scbActiveAcquirer == null) {
                        if (selectedTleBankHost.size() == 1) {
                            transEnd(new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null));
                            return;
                        }
                        selectedTleBankHost.remove(Utils.TLE_BANK_NAME___SCB);
                    } else {
                        scbSelectAcquirer.addAll(scbActiveAcquirer);
                        gotoState(State.LOAD_SCB_JSON.toString());
                        return;
                    }
                }

                if (selectedTleBankHost.contains(Utils.TLE_BANK_NAME___AMEX)) {
                    Log.d(TAG, " Selected list contain AMEX");
                    gotoState(State.LOAD_AMEX_TLE.toString());
                } else {
                    Log.d(TAG, " Selected list wasn't contain SCB-IPP / SCB-REDEEM");
                    processModeSelection(mode);
                }
            } else {
                Log.d(TAG, " cannot get data from TEID file by automatic mode, --> ask user for TEID/PIN");
                if (selectedTleBankHost.contains(Utils.TLE_BANK_NAME___SCB)) {
                    Log.d(TAG, " Selected list contain SCB-IPP / SCB-REDEEM");
                    ArrayList<String> scbActiveAcquirer = ScbUtil.INSTANCE.getScbActiveTleAcquirer();
                    if (scbActiveAcquirer == null) {
                        if (selectedTleBankHost.size() == 1) {
                            transEnd(new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null));
                            return;
                        }
                        selectedTleBankHost.remove(Utils.TLE_BANK_NAME___SCB);
                    } else {
                        scbTle = true;
                        scbSelectAcquirer.addAll(scbActiveAcquirer);
                    }
                }

                if (selectedTleBankHost.contains(Utils.TLE_BANK_NAME___AMEX)) {
                    Log.d(TAG, " Selected list contain AMEX");
                    amexTle = true;
                }

                gotoState(State.ENTER_TEID.toString());
            }
        } else {
            Log.d(TAG, " Host selected list was empty.");
            transEnd(new ActionResult(TransResult.ERR_TLE_NOT_LOAD,null));
        }
    }


    private void displayDialog (String title) {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtils.showProcessingMessage( context, title, Constants.SUCCESS_DIALOG_SHOW_TIME);
            }
        });
    }

    private void processLoadTMK(String currentState) {
        displayDialog("TLE Load\nplease wait...");
        TerminalEncryptionParam gcsParam = FinancialApplication.getUserParam().getTEParam(Bank.GCS);
        if (gcsParam == null) {
            selectedTleBankHost.remove(Utils.TLE_BANK_NAME___BAY);
        }
        if (selectedTleBankHost == null || selectedTleBankHost.isEmpty()) {
            //If only select Bay and not found Bay TE ID/PIN
            transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        ArrayList<Acquirer> successTmkList =  new ArrayList<>();
        for (String tleBankName : selectedTleBankHost) {
            if (tleBankName.equals(Utils.TLE_BANK_NAME___SCB)) {
                successTmkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP));
                successTmkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM));
                continue;
            }
            String strAcqName = Utils.TleBankAndAcquirerMapper(tleBankName);
            ArrayList<Acquirer> subHostList = Utils.getSubHostByTleBankName(strAcqName, tleBankName);
            Acquirer resultTMKAcq = doLoadTMK(strAcqName, subHostList,true);
            if (resultTMKAcq!=null) {
                successTmkList.add(resultTMKAcq);
            } else {
                break;
            }
        }

        if (successTmkList.size() == selectedTleBankHost.size()) {
            onActionResult(currentState, new ActionResult(TransResult.SUCC,null));
        } else {
            onActionResult(currentState, new ActionResult(TransResult.ERR_TLE_REQUEST,null));
        }
    }

    private String getTeidFromFile() {
        String jsonTe;
        String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
        if (tleFilePath != null) {
            File f = new File(tleFilePath);
            if (f.exists() && !f.isDirectory()) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode jsonNode = mapper.readTree(f);
                    jsonTe = jsonNode.toString();
                    return jsonTe;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void processLoadTWK(String currentState) {
        displayDialog("TLE Load\nplease wait...");
        ArrayList<Acquirer> successTwkList =  new ArrayList<>();
        for (String tleBankName : selectedTleBankHost) {
            if (tleBankName.equals(Utils.TLE_BANK_NAME___SCB)) {
                successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP));
                successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM));
                continue;
            }
            String strAcqName = Utils.TleBankAndAcquirerMapper(tleBankName);
            ArrayList<Acquirer> subHostList = Utils.getSubHostByTleBankName(strAcqName, tleBankName);
            Acquirer resultTWKAcq = doLoadTWK(strAcqName, subHostList);
            if (resultTWKAcq!=null) {
                successTwkList.add(resultTWKAcq);
            } else {
                break;
            }
        }

        if (successTwkList.size() == selectedTleBankHost.size()) {
            onActionResult(currentState, new ActionResult(TransResult.SUCC,null));
        } else {
            onActionResult(currentState, new ActionResult(TransResult.ERR_TLE_REQUEST,null));
        }
    }

    private void updateProgress(String title, String dispMsg) {
        if (updateUIListner!=null) {
            updateUIListner.onUpdataUI(title, dispMsg);
        }
    }


    private ArrayList<Acquirer> SuccessList = new ArrayList<Acquirer>();
    private Acquirer doLoadTMK(@NonNull String targetAcquirerName, ArrayList<Acquirer> subHost, boolean forceDownloadTMK) {
        try {
            Acquirer targetAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcquirerName);
            boolean isHasTMKID = (targetAcquirer.getTMK()!=null);
            if (!isHasTMKID || forceDownloadTMK) {
                int result = LoadTLETrans.generateRSA();
                if (result == com.pax.edc.opensdk.TransResult.SUCC) {

                    updateProgress("Host : [" + targetAcquirerName+ "]", "RSA generate success");

                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                    transData.setTransType(ETransType.LOADTMK);
                    transData.setAcquirer(targetAcquirer);
                    FinancialApplication.getAcqManager().setCurAcq(targetAcquirer);

                    updateProgress("Host : [" + targetAcquirerName+ "]", "TMK downloading...");
                    result = LoadTLETrans.AutoLoad_TMK_TWK(transData);
                    if (result == com.pax.edc.opensdk.TransResult.SUCC && transData.getField62() != null) {

                        updateProgress("Host : [" + targetAcquirerName+ "]", "TMK download success");

                        String dataFromDE62 = transData.getField62();
                        result = recoverTMK(targetAcquirer, dataFromDE62);
                        if (result == com.pax.edc.opensdk.TransResult.SUCC) {
                            updateProgress("Host : [" + targetAcquirerName+ "]", "TMK Recover success");
                            Component.incStanNo(transData);
                            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                            Acquirer tmpTMKAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
                            Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + tmpTMKAcquirer.getName() + "] : TMK download success");

                            if (subHost !=null && subHost.size()>0) {
                                for (Acquirer localAcq : subHost) {
                                    Acquirer tmpTwKAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());
                                    result = recoverTMK(tmpTwKAcq, dataFromDE62);
                                    if (result == com.pax.edc.opensdk.TransResult.SUCC) {

                                        updateProgress("Host : [" + tmpTwKAcq.getName()+ "]", "TMK Recover success");

                                        Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + tmpTwKAcq.getName() + "] : TMK download success");
                                    } else {

                                        updateProgress("Host : [" + tmpTwKAcq.getName()+ "]", "TMK Recover failed");

                                        Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + tmpTwKAcq.getName() + "] : Failed on Recover TMK");
                                    }
                                }
                            } else {
                                Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + tmpTMKAcquirer.getName() + "] : -----------> process with no subordinated host");
                            }

                            return tmpTMKAcquirer;
                        } else {

                            updateProgress("Host : [" + targetAcquirerName+ "]", "TMK Recover failed");

                            Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TMK");
                        }
                    } else {

                        updateProgress("Host : [" + targetAcquirerName+ "]", "TMK download failed");

                        Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TMK");
                    }
                } else {

                    updateProgress("Host : [" + targetAcquirerName+ "]", "RSA generate failed");

                    Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on generate RSA");
                }
            } else {
                // this host contain existing TMKID

                updateProgress("Host : [" + targetAcquirerName+ "]", "TMK was exists for this acquirer");

                Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] has existing TMKID = [" + targetAcquirer.getTMK() + "]");
                return  FinancialApplication.getAcqManager().findAcquirer(targetAcquirerName);
            }
        } catch (Exception e) {
            Log.e(SplashActivity.TAG, "Error TMK download :: " + e.getMessage());
        }

        return null;
    }


    private Acquirer doLoadTWK(@NonNull String targetAcqName, ArrayList<Acquirer> subHost){
        return doLoadTWK(targetAcqName, subHost, null);
    }
    private Acquirer doLoadTWK(@NonNull String targetAcqName, ArrayList<Acquirer> subHost, Context externalContext) {
        try {
            Acquirer targetAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcqName);
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            transData.setTransType(ETransType.LOADTWK);
            transData.setAcquirer(targetAcquirer);
            FinancialApplication.getAcqManager().setCurAcq(targetAcquirer);

            int result = AutoLoad_TMK_TWK(transData) ;
            if (result == TransResult.SUCC && transData.getField62()!=null) {
                String dataFromDE62 = transData.getField62();
                result = recoverTWK(targetAcquirer, dataFromDE62);
                if (result==TransResult.SUCC) {
                    Component.incStanNo(transData);
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    Acquirer resultTWKAcq = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
                    Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : TWK download success");
                    if (subHost !=null && subHost.size()>0) {
                        for (Acquirer localAcq : subHost) {
                            Acquirer processTWKRecoverAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());
                            result = recoverTWK(processTWKRecoverAcq, dataFromDE62);
                            if (result == TransResult.SUCC) {
                                Acquirer resultTwKAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());
                                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + resultTwKAcq.getName() + "] : TWK download success");
                                Log.d(SplashActivity.TAG, "\t\t\t\t KEY_ID = " + resultTwKAcq.getKeyId());
                                Log.d(SplashActivity.TAG, "\t\t\t\t TMK_ID = " + resultTwKAcq.getTMK());
                                Log.d(SplashActivity.TAG, "\t\t\t\t TWK_ID = " + resultTwKAcq.getTWK());
                                Log.d(SplashActivity.TAG, "\t\t\t\t UPI_TMK_ID = " + resultTwKAcq.getUP_TMK());
                                Log.d(SplashActivity.TAG, "\t\t\t\t UPI_TWK_ID = " + resultTwKAcq.getUP_TWK());
                            } else {
                                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + processTWKRecoverAcq.getName() + "] : Failed on Recover TWK");
                            }
                        }
                    }
                    return resultTWKAcq;
                } else {
                    Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TWK");
                }
            } else {
                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TWK");
            }
        } catch (Exception e) {
            Log.e(SplashActivity.TAG, "Error TWK download :: " + e.getMessage());
        }
        return null;
    }

    private void doAutoDownloadTLEbySingleHost(ArrayList<String> primaryAcqNameList) {
        Log.d(SplashActivity.TAG, "\t\t\t\t>> TLE download process : start");
        String Tag = "INIT*";
        Log.d(Tag, "===========================================");
        Log.d(Tag, "          TLE AUTO DOWNLOAD LOG ");
        Log.d(Tag, "       --- SINGLE HOST PROCESS --- ");
        Log.d(Tag, "===========================================");

        if (primaryAcqNameList==null) {
            transEnd(new ActionResult(TransResult.ERR_TLE_NOT_LOAD, null));
        }

        try {
             SuccessList.clear();
             if (primaryAcqNameList != null && primaryAcqNameList.size() > 0) {
                 int resultGetTEID = autoGetTeidFromFile();
                 if (resultGetTEID == 0) {

                     TerminalEncryptionParam gcsParam = FinancialApplication.getUserParam().getTEParam(Bank.GCS);
                     if (gcsParam == null) {
                         primaryAcqNameList.remove(Constants.ACQ_BAY_INSTALLMENT);
                     }

                     for (int hostId = 0 ; hostId <= primaryAcqNameList.size() -1 ; hostId++ ) {
                         // load acquirer information
                         String mainAcquirerName = primaryAcqNameList.get(hostId);
                         Acquirer mainAcquirerInfo = FinancialApplication.getAcqManager().findActiveAcquirer(mainAcquirerName);
                         if (mainAcquirerInfo != null) {

                             updateProgress("Host : [" + mainAcquirerInfo.getName()+ "]", "downloading...");

                             // get TLEBankName from main target acquirer
                             String mainTleBankName = mainAcquirerInfo.getTleBankName();
                             // get subhost
                             ArrayList<Acquirer> subHostList = new ArrayList<Acquirer>();
                             if (mainTleBankName!=null) {
                                 subHostList = Utils.getSubHostByTleBankName(mainAcquirerName, mainTleBankName);
                             }

                             Acquirer resultTMKAcquirer = doLoadTMK(mainAcquirerInfo.getName(), subHostList,false);
                             if (resultTMKAcquirer!=null) {

                                 updateProgress("Host : [" + mainAcquirerInfo.getName()+ "]", "Please wait, download starting...");

                                 Acquirer resultTWKAcquirer = doLoadTWK(resultTMKAcquirer.getName(), subHostList);
                                 if (resultTWKAcquirer!=null) {
                                     SuccessList.add(resultTWKAcquirer);
                                 }
                             }
                         }
                         else {
                             // skip process this host
                             Log.d(SplashActivity.TAG, "Cannot find acquirer : [" + mainAcquirerName + "] in active acquirer list.");
                         }
                     }
                 }
                 else {
                     // skip process this host
                     Log.d(SplashActivity.TAG, "Cannot get TEID data from file.");
                 }
             }
        } catch (Exception e) {
            Log.d(SplashActivity.TAG, "Error on download TMK+TWK :: " + e.getMessage());
        }

        if (SuccessList.size() > 0 ) {
            transEnd(new ActionResult(TransResult.SUCC, null));
        } else {
            transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
        }
    }

    private class SingleKeyDownloadTleAcquirerInfo {
        boolean isContainTargetAcquirer;
        String alternateTLEAcquirerName;
        ArrayList<String> TLEAcquirerList;
    }

    private SingleKeyDownloadTleAcquirerInfo retrieveTleAcquireList(String targetAcquirereName) {
        boolean isContainTargetAcquirer = false;
        String altTleHostName = null;
        Acquirer targAcq = FinancialApplication.getAcqManager().findActiveAcquirer(targetAcquirereName);
        String tleBankName = (targAcq != null) ? targAcq.getTleBankName() : null;
        ArrayList<String> enabledTLEHostList = retrieveTleAcquireList();
        ArrayList<String> inScopTleHostName = new ArrayList<>();

//        enabledTLEHostList.remove(targetAcquirereName);
        // check and find which host contained TleBankName same with the TargetAcquirer
        for (String acqName : enabledTLEHostList) {
            Acquirer localAcq = FinancialApplication.getAcqManager().findActiveAcquirer(acqName);
            if (localAcq!=null
                    && localAcq.isEnable()
                    && localAcq.isEnableTle()
                    && localAcq.getTleBankName()!=null
                    && localAcq.getTleBankName().equals(tleBankName)) {
                inScopTleHostName.add(acqName);
            }
        }

        if (inScopTleHostName.size() > 0) {
            if ((inScopTleHostName.indexOf(targetAcquirereName) > -1)) {
                isContainTargetAcquirer = true;
            } else {
                altTleHostName = inScopTleHostName.get(0);
                isContainTargetAcquirer = false;
            }
        }

        SingleKeyDownloadTleAcquirerInfo infos = new SingleKeyDownloadTleAcquirerInfo();
        infos.isContainTargetAcquirer = isContainTargetAcquirer;
        infos.alternateTLEAcquirerName = altTleHostName;
        infos.TLEAcquirerList = enabledTLEHostList;

        return infos;
    }

    private ArrayList<String> getDistinctAllTleBankName() {
        ArrayList<String> tleBankName = new ArrayList<String>();
        List<Acquirer> allAcqList = FinancialApplication.getAcqManager().findAllAcquirers();
        for (Acquirer local_acq : allAcqList) {
            if (local_acq.isEnable() && local_acq.isEnableTle() && !tleBankName.contains(local_acq.getName())) {
                tleBankName.add(local_acq.getName());
            }
        }

        return tleBankName;
    }

    private ArrayList<String> retrieveTleAcquireList() {
        ArrayList<String> acqList = new ArrayList<String>();
        List<Acquirer> allAcqList = FinancialApplication.getAcqManager().findAllAcquirers();
        for (Acquirer local_acq : allAcqList) {
            if (local_acq.isEnable() && local_acq.isEnableTle()) {
                if (local_acq.getName().equals(Constants.ACQ_SCB_IPP)) {
                    scbTle = true;
                }
                acqList.add(local_acq.getName());
            }
        }
        return acqList;
    }


    private int autoGetTeidFromFile() {
        try {
            // load te from file
            List<EnterTeIdActivity.TE> tleid = null;
            tleid = EnterTeIdActivity.getTeidFromJsonFile();
            EnterTeIdActivity.TE kbankTE = EnterTeIdActivity.getRandomTeId(tleid, Bank.KBANK);
            EnterTeIdActivity.TE gcsTE = EnterTeIdActivity.getRandomTeId(tleid, Bank.GCS);



            if (kbankTE == null && gcsTE == null) {
                return TransResult.ERR_PARAM;
            }
            else {
                if (kbankTE!=null) {
                    // For PackLoadTMK
                    FinancialApplication.getUserParam().setTE_ID(kbankTE.TE_ID);
                    FinancialApplication.getUserParam().setTE_PIN(kbankTE.TE_PIN);
                    // For PackKBankLoadTMK
                    FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.KBANK, kbankTE.TE_ID, kbankTE.TE_PIN));
                }

                if (gcsTE != null) {
                    // For PackBayLoadTMK
                    FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.GCS, gcsTE.TE_ID, gcsTE.TE_PIN));
                }
            }
            return TransResult.SUCC;
        } catch (Exception e) {
            return TransResult.ERR_PROCESS_FAILED;
        }
    }

    public static int generateRSA() {
        boolean flag1, flag2;

        PedManager ped = FinancialApplication.getPedInstance();

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

        KeyPair kp = ped.genKeyPair(2048);
        ped.setKeyPair(kp);
        flag1 = ped.writeRSAKeyPublic(1);
        flag2 = ped.writeRSAKeyPrivate(2);

        if (kp != null || !flag1 || !flag2) {
            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();
            byte[] modulus = pubKey.getModulus().toByteArray();
            byte[] modByte = Arrays.copyOfRange(modulus, modulus.length - 256, modulus.length);
            String modStr = Tools.bcd2Str(modByte);
            FinancialApplication.getUserParam().setRSAKey(modStr);
            return TransResult.SUCC;
        } else {
            return TransResult.ERR_SEND;
        }
    }


    private static int onlineResult = TransResult.ERR_ABORTED;

    public static int AutoLoad_TMK_TWK(TransData transData) {
        return AutoLoad_TMK_TWK(transData, null);
    }
    public static int AutoLoad_TMK_TWK(TransData transData, TransProcessListenerImpl listener) {
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

        if (Constants.ACQ_SCB_IPP.equals(acq.getName()) || Constants.ACQ_SCB_REDEEM.equals(acq.getName())) {
            return TransResult.SUCC;
        } else {
            if (FinancialApplication.getAcqManager().getCurAcq().isEnableTle()) {
                try {
                    Thread onlineThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int ret = new Transmit().transmit(transData, listener);
                            if (listener!=null) listener.onHideProgress();
                            onlineResult = ret;

                        }
                    });
                    onlineThread.start();
                    onlineThread.join(60 * 1000);
                } catch (Exception ex) {
                    Log.d("INIT*", "Thread LOAD TMK TWK error : " + ex.getMessage());
                }
            } else {
                onlineResult = TransResult.ERR_ABORTED;
            }
        }

        return onlineResult;
    }

    private static byte[] pTMK_ID;

    public static int recoverTMK(Acquirer acquirer, String ex_DE62) {
        EPedType type = EPedType.INTERNAL;
        boolean flag;

        if (!acquirer.isEnableTle()) {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] bytes62 = Tools.str2Bcd(ex_DE62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62, 0, 4);
        byte[] Version = Arrays.copyOfRange(bytes62, 4, 6);
        byte[] DownloadType = Arrays.copyOfRange(bytes62, 6, 7);
        byte[] RespType = Arrays.copyOfRange(bytes62, 7, 8);
        byte[] AcqId = Arrays.copyOfRange(bytes62, 8, 11);
        byte[] RSARespDATA = Arrays.copyOfRange(bytes62, 11, 293);

        // RSA Data
        byte[] RSAKEY = Arrays.copyOfRange(RSARespDATA, 0, 256);
        byte[] RSAKCV = Arrays.copyOfRange(RSARespDATA, 256, 262);
        byte[] RSAIDENT = Arrays.copyOfRange(RSARespDATA, 262, 282);

        byte[] TMK_ID = Arrays.copyOfRange(RSAIDENT, 0, 4);


        PedManager ped = FinancialApplication.getPedInstance();
        RSARecoverInfo TMKKey = ped.RSADecrypt(2, RSAKEY);

        byte[] keyinfo = TMKKey.getData();
        //GET LAST TMK 16 bytes
        byte[] ltmk = Arrays.copyOfRange(keyinfo, keyinfo.length - 16, keyinfo.length);

        //int acqID = acq.getId();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        //int keyId = acquirer.getKeyId() > 0 ? acquirer.getKeyId() : Component.generateKeyId();

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.getTMKID(acquirer)), ltmk, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }

        String tmkStr = Tools.bytes2String(TMK_ID);
        acquirer.setKeyId(-1);
        acquirer.setTMK(tmkStr);
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        pTMK_ID = TMK_ID;
        return TransResult.SUCC;
    }






    private static byte[] pTWK_DEK;


    public static int recoverTWK(Acquirer acquirer, String ex_DE62) {
        EPedType type = EPedType.INTERNAL;
        boolean flag;
        int icnt;

        byte[] bytes62 = Tools.str2Bcd(ex_DE62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62, 0, 4);
        byte[] Version = Arrays.copyOfRange(bytes62, 4, 6);
        byte[] RespType = Arrays.copyOfRange(bytes62, 6, 7);
        byte[] TWK_ID = Arrays.copyOfRange(bytes62, 7, 11);

        byte[] TWK_DEK = Arrays.copyOfRange(bytes62, 11, 27);
        byte[] TWK_MAK = Arrays.copyOfRange(bytes62, 27, 43);
        byte[] TWK_DEK_KCV = Arrays.copyOfRange(bytes62, 43, 51);
        byte[] TWK_MAK_KCV = Arrays.copyOfRange(bytes62, 51, 59);
        byte[] REW_ACQ = Arrays.copyOfRange(bytes62, 59, 62);

        PedManager ped = FinancialApplication.getPedInstance();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        //int acqID = acq.getId();
        //int keyId = acquirer.getKeyId() > 0 ? acquirer.getKeyId() : Component.generateKeyId();

        byte[] twk_dek = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (byte) (UserParam.getTMKID(acquirer)), TWK_DEK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_dek, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] tmp = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
        byte[] dek_kcv = Tools.bcd2Str(tmp, PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(dek_kcv, TWK_DEK_KCV)) {
            return TransResult.ERR_TLE_REQUEST;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.getDEKID(acquirer)), twk_dek, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }


        byte[] twk_mak = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (UserParam.getTMKID(acquirer)), TWK_MAK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_mak, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] tmp2 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
        byte[] mak_kcv = Tools.bcd2Str(tmp2, PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(mak_kcv, TWK_MAK_KCV)) {
            return TransResult.ERR_TLE_REQUEST;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.getMAKID(acquirer)), twk_mak, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }

        if (acquirer.isEnableUpi()) {
            byte[] TPK_TAG = Arrays.copyOfRange(bytes62, 62, 64);
            byte[] TPK_TWK = Arrays.copyOfRange(bytes62, 64, 80);
            byte[] TPK_TWK_KCV = Arrays.copyOfRange(bytes62, 80, 88);

            byte[] tpk_twk = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (UserParam.getTMKID(acquirer)), TPK_TWK);

            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }

            byte[] tmp3 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
            byte[] tpk_kcv = Tools.bcd2Str(tmp3, PedManager.KCV_SIZE).getBytes();

            if (!Arrays.equals(tpk_kcv, TPK_TWK_KCV)) {
                return TransResult.ERR_UPI_LOGON;
            }

            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.getTPK_TDKID(acquirer)), tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }

            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TPK, (byte) (UserParam.getTPK_TPKID(acquirer)), tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }
        }

        String twkStr = Tools.bytes2String(TWK_ID);
        acquirer.setTWK(twkStr);
        acquirer.setKeyId(-1);
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        pTWK_DEK = TWK_DEK;
        return TransResult.SUCC;
    }

    @Override
    protected void transEnd(ActionResult result) {
        if (progressDialog!=null) {progressDialog.dismiss();}
        if (isRequireRevertToOrigMerc) {
            MerchantProfileManager.INSTANCE.RestoreToSpecificMerchant(originalMerchantName);
        }
        super.transEnd(result);
    }
}


