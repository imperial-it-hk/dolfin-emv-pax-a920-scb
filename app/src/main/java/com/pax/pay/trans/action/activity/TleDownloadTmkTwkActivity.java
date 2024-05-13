package com.pax.pay.trans.action.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.j256.ormlite.stmt.query.In;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.MainActivity;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.LoadTLETrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import th.co.bkkps.utils.Log;

public class TleDownloadTmkTwkActivity extends BaseActivity {

    private LoadTLETrans.Mode mode = LoadTLETrans.Mode.None;
    private ArrayList<String> selectedTleBankHost = new ArrayList<>();
    private int settleResult;

    private TickTimer delayStartTimer = null;
    private TickTimer.OnTickTimerListener onTimerTickListener = null;
    private TransProcessListenerImpl transProcessListener = null;

    private TextView txv_caption = null;
    private TextView txv_message = null;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_auto_settlement;
    }

    @Override
    protected void initViews() {
        txv_caption = findViewById(R.id.txv_caption);
        txv_message = findViewById(R.id.txv_waiting);

        txv_caption.setTextSize(24f);

        txv_caption.setText("Renew TWK");
        txv_message.setText("Please wait...");
    }

    private void displayText(String caption, String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (txv_caption!=null && caption!=null && !caption.isEmpty()) {
                    txv_caption.setText(caption);
                    txv_caption.invalidate();
                    txv_caption.requestLayout();

                }
                if (txv_message!=null && message!=null && !message.isEmpty()) {
                    txv_message.setText(message);
                    txv_message.invalidate();
                    txv_message.requestLayout();
                }
            }
        });
    }

    @Override
    protected void setListeners() {
        onTimerTickListener = new TickTimer.OnTickTimerListener() {
            @Override
            public void onTick(long leftTime) {
                // do nothing
            }

            @Override
            public void onFinish() {

                FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
                    @Override
                    public void run() {
                        ExecuteTleDownloadTwkAsyncTask executor = new ExecuteTleDownloadTwkAsyncTask();
                        executor.execute();
                    }
                }, 500L);

            }
        };
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        int exMode = bundle.getInt("Mode", 1);
        switch (exMode) {
            case 1  : mode = LoadTLETrans.Mode.DownloadTMK; break;
            case 2  : mode = LoadTLETrans.Mode.DownloadTWK; break;
            default : mode = LoadTLETrans.Mode.DownloadTMK; break;
        }

        selectedTleBankHost = bundle.getStringArrayList("HostList");
        settleResult = getIntent().getIntExtra("settleResult", 0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        delayStartTimer = new TickTimer(onTimerTickListener);
        delayStartTimer.start(1);
    }


    private class ExecuteTleDownloadTwkAsyncTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            downloadTwkProcess();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            txv_message.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }

        private void downloadTwkProcess() {
            ArrayList<Acquirer> successTwkList =  new ArrayList<>();
            List<String> disableTwkList = new ArrayList<>();

            for (String tleBankName : selectedTleBankHost) {
                if (tleBankName.equals(Utils.TLE_BANK_NAME___SCB)) {
                    successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP));
                    successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM));
                    continue;
                }
                publishProgress( tleBankName + ", downloading...");
                String strAcqName = Utils.TleBankAndAcquirerMapper(tleBankName);
                Acquirer mainTargetAcquirer = FinancialApplication.getAcqManager().findActiveAcquirer(strAcqName);
                if (mainTargetAcquirer!=null) {
                    ArrayList<Acquirer> subHostList = Utils.getSubHostByTleBankName(strAcqName, tleBankName);
                    Acquirer resultTWKAcq = doLoadTWK(strAcqName, subHostList);
                    if (resultTWKAcq!=null) {
                        successTwkList.add(resultTWKAcq);
                    } else {
                        break;
                    }
                } else {
                    disableTwkList.add(strAcqName);
                    break;
                }
            }

            if ((successTwkList.size() + disableTwkList.size()) == selectedTleBankHost.size()) {
                publishProgress( "Result : Success");
                displayResultDialog(TransResult.SUCC);
            } else {
                publishProgress( "Result : Failed");
                displayResultDialog(TransResult.ERR_TLE_REQUEST);
            }
        }

        private Acquirer doLoadTWK(@NonNull String targetAcqName, ArrayList<Acquirer> subHost) {
            try {
                Acquirer targetAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcqName);
                TransData transData = null;
                transData = Component.transInit();
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                transData.setTransType(ETransType.LOADTWK);
                transData.setAcquirer(targetAcquirer);
                FinancialApplication.getAcqManager().setCurAcq(targetAcquirer);

                publishProgress( "Connect to host...");
                int result = LoadTLETrans.AutoLoad_TMK_TWK(transData, null) ;
                if (result == TransResult.SUCC && transData.getField62()!=null) {
                    String dataFromDE62 = transData.getField62();
                    publishProgress( "Recover TWK");
                    result = LoadTLETrans.recoverTWK(targetAcquirer, dataFromDE62);
                    if (result==TransResult.SUCC) {
                        Component.incStanNo(transData);
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                        Acquirer resultTWKAcq = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
                        Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : TWK download success");
                        if (subHost !=null && subHost.size()>0) {
                            for (Acquirer localAcq : subHost) {
                                Acquirer processTWKRecoverAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());

                                publishProgress( "Recover TWK for " + localAcq.getName());
                                result = LoadTLETrans.recoverTWK(processTWKRecoverAcq, dataFromDE62);
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
                        publishProgress("Renew TWK", "Done, please wait");
                        return resultTWKAcq;
                    } else {
                        publishProgress( "Failed on Recover TWK");
                        Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TWK");
                    }
                } else {
                    publishProgress( "Failed on download TWK");
                    Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TWK");
                }
            } catch (Exception e) {
                publishProgress( "Unable to renew TWK");
                Log.e(SplashActivity.TAG, "Error TWK download :: " + e.getMessage());
            }
            return null;
        }
    }

    private void displayResultDialog(int result) {
        if (result == TransResult.SUCC) {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
                DialogUtils.showSuccMessage(TleDownloadTmkTwkActivity.this, ETransType.LOADTWK.getTransName(),
                        dialogInterface -> {
                            finish(new ActionResult(result, null));
                        },
                        Constants.SUCCESS_DIALOG_SHOW_TIME);
            } else {
                DialogUtils.showSuccMessage(TleDownloadTmkTwkActivity.this, ETransType.LOADTWK.getTransName(),
                        dialogInterface -> {
                            displaySettlementResult();
                        },
                        Constants.SUCCESS_DIALOG_SHOW_TIME);
            }
        } else {
            DialogUtils.showErrMessage(TleDownloadTmkTwkActivity.this, ETransType.LOADTWK.getTransName(),
                    TransResultUtils.getMessage(result),
                    dialogInterface -> {
                        displaySettlementResult();
                    },
                    Constants.FAILED_DIALOG_SHOW_TIME);
        }
    }

    private void displaySettlementResult() {
        if (settleResult == TransResult.SUCC) {
            DialogUtils.showSuccMessage(TleDownloadTmkTwkActivity.this, ETransType.SETTLE.getTransName(),
                    dialogInterface -> {
                        finish(new ActionResult(TransResult.ERR_ABORTED,null));//to skip dialog when transEnd
                    },
                    Constants.SUCCESS_DIALOG_SHOW_TIME);
        } else {
            DialogUtils.showErrMessage(TleDownloadTmkTwkActivity.this, ETransType.SETTLE.getTransName(),
                    TransResultUtils.getMessage(settleResult),
                    dialogInterface -> {
                        finish(new ActionResult(TransResult.ERR_ABORTED,null));//to skip dialog when transEnd
                    },
                    Constants.FAILED_DIALOG_SHOW_TIME);
        }
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        Log.d("TleDownloadTmkTwkActivity", "onFinish -- action is null = " + (action==null));
        if (action != null) {
            if (action.isFinished()) {return;}
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        }

        finish();
    }


//    private void downloadTwkProcess() {
//        try {
//
//            Thread processTwkThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    ArrayList<Acquirer> successTwkList =  new ArrayList<>();
//                    List<String> disableTwkList = new ArrayList<>();
//
//                    for (String tleBankName : selectedTleBankHost) {
//                        if (tleBankName.equals(Utils.TLE_BANK_NAME___SCB)) {
//                            successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP));
//                            successTwkList.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM));
//                            continue;
//                        }
//                        displayText("RENEW TWK", tleBankName + ", downloading...");
//                        String strAcqName = Utils.TleBankAndAcquirerMapper(tleBankName);
//                        Acquirer mainTargetAcquirer = FinancialApplication.getAcqManager().findActiveAcquirer(strAcqName);
//                        if (mainTargetAcquirer!=null) {
//                            ArrayList<Acquirer> subHostList = Utils.getSubHostByTleBankName(strAcqName, tleBankName);
//                            Acquirer resultTWKAcq = TleDownloadTmkTwkActivity.this.doLoadTWK(strAcqName, subHostList);
//                            if (resultTWKAcq!=null) {
//                                successTwkList.add(resultTWKAcq);
//                            } else {
//                                break;
//                            }
//                        } else {
//                            disableTwkList.add(strAcqName);
//                            break;
//                        }
//                    }
//
//                    if ((successTwkList.size() + disableTwkList.size()) == selectedTleBankHost.size()) {
//                        displayText("RENEW TWK", "Result : Success");
//                        SystemClock.sleep(2000L);
//                        finish(new ActionResult(TransResult.SUCC,null));
//                    } else {
//                        displayText("RENEW TWK", "Result : Failed");
//                        SystemClock.sleep(2000L);
//                        finish(new ActionResult(TransResult.ERR_TLE_REQUEST,null));
//                    }
//                }
//            });
//            processTwkThread.start();
//            processTwkThread.join(60*1000);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }




//    private Acquirer doLoadTWK(@NonNull String targetAcqName, ArrayList<Acquirer> subHost) {
//        //TransProcessListenerImpl listener = new TransProcessListenerImpl(this);
//
//        try {
//            Acquirer targetAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcqName);
//            TransData transData = null;
//            transData = Component.transInit();
//            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
//            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
//            transData.setTransType(ETransType.LOADTWK);
//            transData.setAcquirer(targetAcquirer);
//            FinancialApplication.getAcqManager().setCurAcq(targetAcquirer);
//
//            displayText("Renew TWK", "Connect to host...");
//            int result = LoadTLETrans.AutoLoad_TMK_TWK(transData, null) ;
//            if (result == TransResult.SUCC && transData.getField62()!=null) {
//                String dataFromDE62 = transData.getField62();
//                displayText("Renew TWK", "Recover TWK");
//                result = LoadTLETrans.recoverTWK(targetAcquirer, dataFromDE62);
//                if (result==TransResult.SUCC) {
//                    Component.incStanNo(transData);
//                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
//                    Acquirer resultTWKAcq = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
//                    Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : TWK download success");
//                    if (subHost !=null && subHost.size()>0) {
//                        for (Acquirer localAcq : subHost) {
//                            Acquirer processTWKRecoverAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());
//
//                            displayText("Renew TWK", "Recover TWK for " + localAcq.getName());
//                            result = LoadTLETrans.recoverTWK(processTWKRecoverAcq, dataFromDE62);
//                            if (result == TransResult.SUCC) {
//                                Acquirer resultTwKAcq = FinancialApplication.getAcqManager().findAcquirer(localAcq.getName());
//                                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + resultTwKAcq.getName() + "] : TWK download success");
//                                Log.d(SplashActivity.TAG, "\t\t\t\t KEY_ID = " + resultTwKAcq.getKeyId());
//                                Log.d(SplashActivity.TAG, "\t\t\t\t TMK_ID = " + resultTwKAcq.getTMK());
//                                Log.d(SplashActivity.TAG, "\t\t\t\t TWK_ID = " + resultTwKAcq.getTWK());
//                                Log.d(SplashActivity.TAG, "\t\t\t\t UPI_TMK_ID = " + resultTwKAcq.getUP_TMK());
//                                Log.d(SplashActivity.TAG, "\t\t\t\t UPI_TWK_ID = " + resultTwKAcq.getUP_TWK());
//                            } else {
//                                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + processTWKRecoverAcq.getName() + "] : Failed on Recover TWK");
//                            }
//                        }
//                    }
//                    displayText("Renew TWK", "Done, please wait");
//                    return resultTWKAcq;
//                } else {
//                    displayText("Renew TWK", "Failed on Recover TWK");
//                    Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TWK");
//                }
//            } else {
//                displayText("Renew TWK", "Failed on download TWK");
//                Log.d(SplashActivity.TAG, "\t\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TWK");
//            }
//        } catch (Exception e) {
//            displayText("Renew TWK", "Unable to renew TWK");
//            Log.e(SplashActivity.TAG, "Error TWK download :: " + e.getMessage());
//        }
//        return null;
//    }

}
