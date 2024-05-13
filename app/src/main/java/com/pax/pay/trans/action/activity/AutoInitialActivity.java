package com.pax.pay.trans.action.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadParamService;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.TerminalLockCheck;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.EReceiptStatusTrans;
import com.pax.pay.trans.LoadTMKTrans;
import com.pax.pay.trans.TleStatusTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionReIntialEReceipt;
import com.pax.pay.trans.action.ActionUpdateSp200;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.LoadTleMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSetConfig;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;
import th.co.bkkps.utils.Log;

public class AutoInitialActivity extends BaseActivity {
    private Context context = null;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_null;
    }

    @Override
    protected void initViews() {
        this.context = this;
    }

    @Override
    protected void loadParam() {
    }

    @Override
    protected void setListeners() {
        onActivityResult(REQ_PREREQUISITE_PROC, 0, null);
    }


    final int REQ_PREREQUISITE_PROC = 0;
    final int REQ_LOAD_INITIAL_ERCM = 1;
    final int REQ_PRINT_STATUS_ERCM = 2;     // ERM initial will call by itself
    final int REQ_LOAD_INIT_TMLKTWK = 3;
    //final int REQ_PRINT_STATUS__TLE = 4 ;     // TLE initial will call by itself
    final int REQ_SCB_LOAD_TLE = 4;
    final int REQ_UPDATE__FW__SP200 = 5;
    final int REQ_LOAD_SCB__TMK_TWK = 6;
    final int REQ_PROCESS__FINALIZE = 10;
    final int REQ_ASK_PERMIS_DOLFIN = 11;
    final int REQ_PRINT_LINKPOS_CMD = 12;
    final int REQ_SCB_DOWNLOAD_DONE = 100;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (isOnFinish) {
            return;
        }

        if (requestCode == 100) {
            LoadTleMsg.Response loadTleRes = (LoadTleMsg.Response) transAPI.onResult(requestCode, resultCode, data);

            if (loadTleRes != null) {
                Log.d("BpsApi", "getRspCode=" + loadTleRes.getRspCode());
                Log.d("BpsApi", "getStanNo=" + loadTleRes.getStanNo());
                Log.d("BpsApi", "getVoucherNo=" + loadTleRes.getVoucherNo());
                Component.incStanNo(loadTleRes.getStanNo());
                Component.incTraceNo(loadTleRes.getVoucherNo());
                if (loadTleRes.getRspCode() == TransResult.SUCC) {
                    Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
                    acq.setTMK("Y");
                    acq.setTWK("Y");
                    FinancialApplication.getAcqManager().updateAcquirer(acq);
                }
            }
        }

        switch (requestCode) {
            case REQ_PREREQUISITE_PROC:
                doPrerequisiteProcess();
                break;
            case REQ_LOAD_INITIAL_ERCM:
                ReInitialERCM(context);
                break;
            case REQ_PRINT_STATUS_ERCM:
                PrintERCMStatus(context, ercmInitResult);
                break;
            case REQ_ASK_PERMIS_DOLFIN:
                askForDolFinPermission(context);
                break;
            case REQ_SCB_LOAD_TLE:
                doScbLoadTLE();
                break;
            case REQ_LOAD_INIT_TMLKTWK:
            case 100:
                ReInitTLEAutoDownload(context);
                break;
            case REQ_UPDATE__FW__SP200:
                PushParamToSP200(context);
                break;
            case REQ_PROCESS__FINALIZE:
                doFinalizeProcess(context);
                break;
        }
    }

    boolean currKioskMode = false;

    protected void doPrerequisiteProcess() {

        // turn off kiosk mode
        currKioskMode = TerminalLockCheck.getInstance().getKioskMode();
        TerminalLockCheck.getInstance().setKioskMode(false);

        onActivityResult(REQ_LOAD_INITIAL_ERCM, 0, null);
    }

    protected void doFinalizeProcess(Context local_context) {
        Log.d("INIT*", "---------------------------------------------------------------------------------------> TURN FIRST_INITIAL_NEEDED : OFF");
        isOnFinish = true;
        Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, false);
        TerminalLockCheck.getInstance().setKioskMode(currKioskMode);
        Utils.restart();
        finish(local_context, new ActionResult(TransResult.SUCC, null));
    }

    private void autoGetTeidFromFile() {
        String jsonTe = null;
        String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
        File f = new File(tleFilePath);
        if (f.exists() && !f.isDirectory()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonNode = mapper.readTree(f);
                jsonTe = jsonNode.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("INIT*", "\t- - - - - - - - - - - - - - - - - - - - - -");
        Log.d("INIT*", "\t SCB-TLE-DOWNLOAD--START");
        ITransAPI transAPI = TransAPIFactory.createTransAPI();
        doScbTleProcess(context, transAPI, null, jsonTe);
    }


    public void doScbTleProcess(final Context context, final ITransAPI transAPI, final AAction currentAction, final String jsonTe) {
        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(
                action -> ((ActionScbUpdateParam) action).setParam(context)
        );
        actionScbUpdateParam.setEndListener((action, result) -> {
            TransContext.getInstance().getCurrentAction().setFinished(true);
            TransContext.getInstance().setCurrentAction(currentAction);
            isParamSuccess = result.getRet() == TransResult.SUCC;

            LoadTleMsg.Request loadTleRequest = new LoadTleMsg.Request();
            loadTleRequest.setJsonTe(jsonTe);
            transAPI.startTrans(context, loadTleRequest);
        });
        actionScbUpdateParam.execute();

    }

    private void onTransApiCallback(int requestCode, int resultCode, @Nullable Intent data) {
        LoadTleMsg.Response loadTleRes = (LoadTleMsg.Response) transAPI.onResult(requestCode, resultCode, data);

        if (loadTleRes != null) {
            Log.d("INIT*", "\t SCB-TLE-DOWNLOAD--END");
            Log.d("BpsApi", "getRspCode=" + loadTleRes.getRspCode());
            Log.d("BpsApi", "getStanNo=" + loadTleRes.getStanNo());
            Log.d("BpsApi", "getVoucherNo=" + loadTleRes.getVoucherNo());
            Component.incStanNo(loadTleRes.getStanNo());
            Component.incTraceNo(loadTleRes.getVoucherNo());
            if (loadTleRes.getRspCode() != TransResult.SUCC) {
                onActivityResult(REQ_LOAD_INIT_TMLKTWK, 0, null);
            }
            Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            acq.setTMK("Y");
            acq.setTWK("Y");
            FinancialApplication.getAcqManager().updateAcquirer(acq);
        }

        onActivityResult(REQ_LOAD_INIT_TMLKTWK, 0, null);
    }


    private EReceiptStatusTrans.ercmInitialResult ercmInitResult = EReceiptStatusTrans.ercmInitialResult.NONE;

    protected void ReInitialERCM(Context local_context) {
        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
            onActivityResult(REQ_PRINT_STATUS_ERCM, 0, null);
            return;
        }

        boolean result = FinancialApplication.getEReceiptDataDbHelper().deleteErmSessionKey();

        if (result) {
            ActionReIntialEReceipt reIntialEReceipt = new ActionReIntialEReceipt(new AAction.ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionReIntialEReceipt) action).setParam(local_context);
                }
            });
            reIntialEReceipt.setEndListener(new AAction.ActionEndListener() {
                @Override
                public void onEnd(AAction action, ActionResult result) {
                    Log.d("INIT*" + TAG, "INIT_ERM-STATUS--END");
                    if (TransContext.getInstance() != null) {
                        if (ActivityStack.getInstance() != null) {
                            ActivityStack.getInstance().popTo(AutoInitialActivity.class);
                        }
                        if (TransContext.getInstance().getCurrentAction() != null) {
                            TransContext.getInstance().getCurrentAction().setFinished(true);
                        }
                        //TransContext.getInstance().setCurrentAction(null);
                    }
                    ercmInitResult = (result.getRet() == TransResult.SUCC) ? EReceiptStatusTrans.ercmInitialResult.INIT_SUCCESS : EReceiptStatusTrans.ercmInitialResult.INIT_FAILED;
                    onActivityResult(REQ_PRINT_STATUS_ERCM, 0, null);
                }
            });
            reIntialEReceipt.execute();
            Log.d("INIT*" + TAG, "INIT_ERM-STATUS--START");
        } else {
            ercmInitResult = EReceiptStatusTrans.ercmInitialResult.CLEAR_SESSIONKEY_ERROR;
            onActivityResult(REQ_PRINT_STATUS_ERCM, 0, null);
        }
    }

    protected void PrintERCMStatus(Context local_context, EReceiptStatusTrans.ercmInitialResult ercmInitResult) {
        ATransaction.TransEndListener eReceiptStatusPrintEndListener = new ATransaction.TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                Log.d("INIT*" + TAG, "PRINT_ERM-STATUS--END");

                onActivityResult(REQ_ASK_PERMIS_DOLFIN, 0, null);
            }
        };
        EReceiptStatusTrans eReceiptStatusTrans = new EReceiptStatusTrans(local_context, ETransType.ERCEIPT_TERMINAL_REGISTRATION, eReceiptStatusPrintEndListener, ercmInitResult);
        eReceiptStatusTrans.execute();
        Log.d("INIT*" + TAG, "PRINT_ERM-STATUS--START");
    }


    protected void askForDolFinPermission(Context local_context) {
        Log.d("INIT*", " ---- DOLFIN ---- ");
        Acquirer dolfinAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
        boolean isDolfinPermsNotRequire = false;
        if (dolfinAcq != null) {
            if (dolfinAcq.isEnable()) {
                Log.d("INIT*", "\t Dolfin host was enabled ");
                if (DolfinApi.getInstance().getDolfinServiceBinded()) {
                    try {
                        ActionDolfinSetConfig dolfinSentConfigAction = new ActionDolfinSetConfig(new AAction.ActionStartListener() {
                            @Override
                            public void onStart(AAction action) {
                                ((ActionDolfinSetConfig) action).setParam(local_context);
                            }
                        });
                        dolfinSentConfigAction.setEndListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("INIT*", "\t Test Ask permission completed ");
                                onActivityResult(REQ_SCB_LOAD_TLE, 0, null);
                            }
                        });
                        dolfinSentConfigAction.execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.e("INIT*", "\t error ActionDolfinSetConfig");
                        isDolfinPermsNotRequire = true;
                    }
                } else {
                    // dofin host disabled
                    Log.d("INIT*", "\t Dolfin service wasn't binded ");
                    isDolfinPermsNotRequire = true;
                }
            } else {
                // dofin host disabled
                Log.d("INIT*", "\t Dolfin host was disabled ");
                isDolfinPermsNotRequire = true;
            }
        } else {
            // dolfin host is null
            Log.d("INIT*", "\t Dolfin host was not found ");
            isDolfinPermsNotRequire = true;
        }

        if (isDolfinPermsNotRequire) {
            Log.d("INIT*", "\t ask permission for dolfin app was skipped ");
            onActivityResult(REQ_SCB_LOAD_TLE, 0, null);
        }

        Log.d("INIT*", " ----------------- ");
    }

    private ProgressDialog progressDialog = null;
    int TLE_intState = -999;

    private void ReInitTLEAutoDownload(Context local_context) {
        DownloadParamService.TleAutoInitState TLE_state = DownloadParamService.TleAutoInitState.NONE;
        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
        boolean displayError = false;
        if (acquirerList.size() > 0) {
            String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
            File f = new File(tleFilePath);
            if (f.exists() && !f.isDirectory()) {
                displayError = false;
                TLE_intState = -999;
                TLE_state = DownloadParamService.TleAutoInitState.NONE;
            } else {
                displayError = true;
                TLE_intState = 3;
                TLE_state = DownloadParamService.TleAutoInitState.MISSING_TEID_FILE;
            }

            if (TLE_state == DownloadParamService.TleAutoInitState.NONE) {

                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog = ProgressDialog.show(AutoInitialActivity.this, "", "TLE downloading, please wait..");
                        progressDialog.setCancelable(false);
                    }
                });

                ATransaction.TransEndListener transEndListener = new ATransaction.TransEndListener() {
                    @Override
                    public void onEnd(ActionResult result) {
                        Log.d("INIT*", "ONEND--AUTO-LOAD-TLE");
                        AutoInitialActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        if (TransContext.getInstance() != null) {
                            if (ActivityStack.getInstance() != null) {
                                ActivityStack.getInstance().popTo(AutoInitialActivity.class);
                            }
                            if (TransContext.getInstance().getCurrentAction() != null) {
                                TransContext.getInstance().getCurrentAction().setFinished(true);
                            }
                            //TransContext.getInstance().setCurrentAction(null);
                        }
                        TLE_intState = (result.getRet() == TransResult.SUCC) ? 5 : 4;
                        printTLEStatusReport(local_context, TLE_intState);
                    }
                };
                LoadTMKTrans TLE_autoInit = new LoadTMKTrans(local_context, transEndListener, true);
                TLE_autoInit.execute();
            }
        } else {
            displayError = true;
            TLE_intState = 1;
            TLE_state = DownloadParamService.TleAutoInitState.TLE_ACQUIRER_NOT_FOUND;
        }

        if (displayError) {
            // set next state to process
            printTLEStatusReport(local_context, TLE_intState);
        }
    }

    private void printTLEStatusReport(Context local_context, int TleInitResult) {
        ATransaction.TransEndListener transEndListener = new ATransaction.TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                Log.d("INIT*", "ONEND--PRINT-TLE-STATUS--END");
                onActivityResult(REQ_UPDATE__FW__SP200, 0, null);
            }
        };
        TleStatusTrans tleStatusTrans = new TleStatusTrans(local_context, transEndListener, true, TleInitResult);
        tleStatusTrans.execute();
        Log.d("INIT*", "PRINT-TLE-STATUS--START");
    }


    private String TAG = "AutoInitActivity";

    protected void PushParamToSP200(Context local_context) {
        if (!SP200_serialAPI.getInstance().isSp200Enable()) {
            Log.d("INIT*" + TAG, "UPDATE_SP200-STATUS--DISABLED");
            onActivityResult(REQ_PROCESS__FINALIZE, 0, null);
            return;
        }

        ActionUpdateSp200 updateSp200 = new ActionUpdateSp200(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionUpdateSp200) action).setParam(local_context);
            }
        });
        updateSp200.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                Log.d("INIT*" + TAG, "UPDATE_SP200-STATUS--END");
                onActivityResult(REQ_PROCESS__FINALIZE, 0, null);
            }
        });
        FinancialApplication.getApp().setUpdateSp200Action(updateSp200);
        updateSp200.execute();
        Log.d("INIT*" + TAG, "UPDATE_SP200-STATUS--START");
    }

    boolean isParamSuccess;
    boolean isProcessRun;
    private ITransAPI transAPI;

    private void doScbLoadTLE() {
        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
        for (Acquirer local_acq : acquirerList) {
            local_acq.setTMK(null);
            local_acq.setTWK(null);
            FinancialApplication.getAcqManager().updateAcquirer(local_acq);
        }
        Device.eraseKeys();

        boolean scbTle = false;
        ArrayList<String> acqList = new ArrayList<String>();
        List<Acquirer> allAcqList = FinancialApplication.getAcqManager().findAllAcquirers();
        for (Acquirer local_acq : allAcqList) {
            if (local_acq.isEnable() && local_acq.isEnableTle()) {
                if (local_acq.getName().equals(Constants.ACQ_SCB_IPP)) {
                    scbTle = true;
                }
            }
        }

        if (!scbTle) {
            onActivityResult(REQ_LOAD_INIT_TMLKTWK, 0, null);
            return;
        }

        String jsonTe;
        transAPI = TransAPIFactory.createTransAPI();
        String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
        File f = new File(tleFilePath);
        if (f.exists() && !f.isDirectory()) {

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonNode = mapper.readTree(f);
                jsonTe = jsonNode.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            return;
        }

        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(
                action -> ((ActionScbUpdateParam) action).setParam(context)
        );
        actionScbUpdateParam.setEndListener((action, result) -> {
            isParamSuccess = result.getRet() == TransResult.SUCC;
            isProcessRun = false;
        });
        isProcessRun = true;
        actionScbUpdateParam.execute();

        while (isProcessRun) {
            SystemClock.sleep(500);
        }

        if (isParamSuccess) {
            LoadTleMsg.Request loadTleRequest = new LoadTleMsg.Request();
            loadTleRequest.setJsonTe(jsonTe);
            transAPI.startTrans(context, loadTleRequest);
        } else {
            onActivityResult(REQ_LOAD_INIT_TMLKTWK, 0, null);
        }
    }


    boolean isOnFinish = false;

    public void finish(Context local_context, ActionResult result) {
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                AAction action = FinancialApplication.getApp().getAutoInitAction();
                //AAction action = TransContext.getInstance().getCurrentAction();
                if (action != null) {
                    if (action.isFinished())
                        return;
                    action.setFinished(true);
                    quickClickProtection.start(); // AET-93
                    action.setResult(result);
                }
                //ActivityStack.getInstance().popTo(MainActivity.class);
                finish();
            }
        };

        try {
            if (result.getRet() != 0) {
                DialogUtils.showErrMessage(local_context, null, TransResultUtils.getMessage(result.getRet()), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            } else {
                DialogUtils.showSuccMessage(local_context, "", onDismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
            }
        } catch (Exception ex) {
            Log.d("INIT*", "AutoInitialActivity error :" + ex.getMessage());
        }
    }
}
