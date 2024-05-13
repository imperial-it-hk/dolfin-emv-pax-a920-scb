package com.pax.pay.trans.action.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionRecoverTMK;
import com.pax.pay.trans.action.ActionUpdateSp200;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.pay.utils.models.SP200FirmwareInfos;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.Log;


public class Sp200UpdateActivity extends BaseActivity {
    Context context;
    private ProgressDialog progressDialog;
    private Thread threadHandleDevice;
    private AAction action;

    // use for set upgrade firmware target file on
    private SP200FirmwareInfos targFirmwareInfo = null;

    @Override
    protected void setListeners() {

    }

    @Override
    protected void loadParam() {
        //do nothing
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_null;
    }

    @Override
    protected void initViews() {
        context = this;
        Device.enableBackKey(false);
    }

    @Override
    protected String getTitleString() {
        return "UPDATE SP200 FIRMWARE";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_null);

        action = FinancialApplication.getApp().getUpdateSp200Action();
        Log.d("INIT*", "onCreate : TransContext.getInstance.getcurrentAction is " + ((action == null) ? "null" : "not null"));

        // get currentFirmware version from device & get latest Firmware version from file
        SP200FirmwareInfos currFwOnSp200Device = SP200_serialAPI.getInstance().getFirmwareFromSP200Device();
        SP200FirmwareInfos latestFwSp200Softfile = Utils.getSP200FirmwareInfos(".aip");
        boolean isNeedSP200FwUpdate = false;
        boolean isSoftFileExist = (latestFwSp200Softfile != null);
        boolean isVersionMatched = false;

        if (currFwOnSp200Device != null){
            // incase able to get current-firmware-version from SP200 device
            if (latestFwSp200Softfile != null) {
                isVersionMatched = (currFwOnSp200Device.FirmwareVersion.trim().equals(latestFwSp200Softfile.FirmwareVersion));
                isNeedSP200FwUpdate = !isVersionMatched;
            } else {
                // incase unable to get firmware version from device
                isNeedSP200FwUpdate = (latestFwSp200Softfile != null);
            }
        }
        else {
            // incase unable to get firmware version from device
            isNeedSP200FwUpdate = (latestFwSp200Softfile != null);
        }
        targFirmwareInfo = latestFwSp200Softfile;

        Log.d("SP200"," ");
        Log.d("SP200","======================================================");
        Log.d("SP200"," SP200 FIRMWARE-UPDATE DECISION FACTOR");
        Log.d("SP200","======================================================");
        Log.d("SP200"," 1. SP200 DEVICE INFO");
        Log.d("SP200","        VERSION : " + ((currFwOnSp200Device != null) ? currFwOnSp200Device.FirmwareVersion : "-"));
        Log.d("SP200"," ");
        Log.d("SP200"," 2. SOFTFILE FIRMWARE INFO" );
        Log.d("SP200","        VERSION  : " + ((latestFwSp200Softfile != null) ? latestFwSp200Softfile.FirmwareVersion : "-"));
        Log.d("SP200","        FILENAME : " + ((latestFwSp200Softfile != null) ? latestFwSp200Softfile.FirmwareName : "-"));
        Log.d("SP200"," ");
        Log.d("SP200","======================================================");
        Log.d("SP200"," [DECISION RESULT] : " + (isNeedSP200FwUpdate ? "REQUIRED FOR UPDATE !!" : "NOT REQUIRE (MATCHED)") );
        if (isNeedSP200FwUpdate && targFirmwareInfo != null && targFirmwareInfo.FirmwareName != null) {
            Log.d("SP200"," [USE-FIRMWARE-FILE] : '" + targFirmwareInfo.FirmwareName + "'" );
        }
        Log.d("SP200","======================================================");
        Log.d("SP200"," ");


        if (isNeedSP200FwUpdate){
            Log.d("SP200","Update status : --- start");
            progressDialog = ProgressDialog.show(this, "", "Please Wait..");
            progressDialog.setCancelable(false);

            threadHandleDevice = new Thread(new Runnable() {
                @Override
                public void run() {
                    int transResult = 0;
                    try {
                        int iRet = SP200_serialAPI.getInstance().updateSp200(targFirmwareInfo);
                        progressDialog.dismiss();
                        if (iRet == TransResult.SUCC) {
                            transResult = TransResult.SUCC;
                        } else {
                            transResult = TransResult.ERR_SP200_UPDATE_INTERNAL_FAILED;
                        }
                        //finish(new ActionResult(TransResult.SUCC, null));
                    } catch (Exception e) {
                        e.printStackTrace();
                        //finish(new ActionResult(TransResult.ERR_SP200_UPDATE_FAILED, null));
                        transResult = TransResult.ERR_SP200_UPDATE_FAILED;
                    }
                    finally {
                        Log.d("SP200","Update status : --- end");
                        finish(new ActionResult(transResult, null));
                    }
                }
            });
            threadHandleDevice.start();
        } else {
            Log.d("SP200","Sp200Update wasn't required");
            if (!isSoftFileExist) {
                finish(new ActionResult(TransResult.SP200_FIRMWARE_NO_AIP_FILE, null));
            } else {
                // it's no need to update cause by version was match between softfile & existing version on sp200 device
                finish(new ActionResult(TransResult.SP200_FIRMWARE_UPTODATE, null));
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Device.enableBackKey(true);
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    public void finish(ActionResult result) {
        if (action!=null && (action instanceof ActionUpdateSp200)) {
            action.setFinished(true);
            action.setResult(result);
            finish();
        } else {
            DialogInterface.OnDismissListener  onDismissListener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    onDialogDismiss(result, dialog);
                }
            };

            try {
                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(context, "", onDismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else if (result.getRet() == TransResult.SP200_FIRMWARE_UPTODATE) {
                    DialogUtils.showSuccMessage(context, "FIRMWARE UP-TO-DATE\n", onDismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(context, null, TransResultUtils.getMessage(result.getRet()), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            } catch (Exception ex) {
                Log.d("INIT*", String.format("exception: %1$s", ex.getMessage()));
            }
        }
    }

    private void onDialogDismiss(ActionResult result) {
        onDialogDismiss(result, null);
    }

    private void onDialogDismiss(ActionResult result, DialogInterface dialog) {
        boolean isExist = ActivityStack.getInstance().exists(AutoInitialActivity.class);
        Log.d("INIT*", String.format("AutoInitialActivity.class = %1$s", Boolean.toString(isExist)));
        finish();
        Log.d("INIT*", "finish() called.");
        if (action != null) {
            if (action.isFinished()) {
                Log.d("INIT*", "DismissListener : TransContext.getInstance.getcurrentAction was finished");
                return;
            }
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
            ActivityStack.getInstance().popTo(AutoInitialActivity.class);
        } else {
            Log.d("INIT*", "DismissListener : TransContext.getInstance.getcurrentAction is null");

        }
    }
}

