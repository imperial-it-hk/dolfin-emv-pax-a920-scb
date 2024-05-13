package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.misc.BaseDaoEnabled;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.action.activity.DynamicOfflineActivity;
import com.pax.pay.trans.action.activity.DynamicOfflineSettingsActivity;
import com.pax.pay.trans.action.activity.EReceiptOtherSettingsActivity;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.DynamicOffline;

public class ActionDynamicOffline extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionDynamicOffline(ActionStartListener listener) {
        super(listener);
    }

    private Context context =null;
    private enumDynamicOfflineMode mode = enumDynamicOfflineMode.disabled;
    public enum enumDynamicOfflineMode{ enabled, disabled, settings}

    public void setParam(Context exContext, enumDynamicOfflineMode exMode){
        this.context= exContext;
        this.mode= exMode;
    }

    @Override
    protected void process() {
        switch (this.mode) {
            case enabled:
                break;
            case disabled:
                askForDisableDynamicOffline();
                break;
            case settings:
                openDynamicOfflineSettings();
                break;
        }
    }

    private void openDynamicOfflineSettings(){
        Intent intent = new Intent(context, DynamicOfflineSettingsActivity.class);
        context.startActivity(intent);
    }

    private void askForDisableDynamicOffline(){
        if (DynamicOffline.getInstance().isDynamicOfflineActiveStatus()==true) {
            DialogUtils.showConfirmDialog(context, Utils.getString(R.string.kbank_dynamic_offline_disable_alert_text),
                    new CustomAlertDialog.OnCustomClickListener() {
                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            setResult(new ActionResult(TransResult.ERR_USER_CANCEL,null));
                            alertDialog.dismiss();
                        }
                    }
                    , new CustomAlertDialog.OnCustomClickListener() {
                        @Override
                        public void onClick(CustomAlertDialog alertDialog) {
                            setResult(new ActionResult(TransResult.SUCC,null));
                            alertDialog.dismiss();
                        }
                    }
            );
        } else {
            setResult(new ActionResult(TransResult.DYNAMIC_OFFLINE_STILL_DISABLED,null));
        }

    }
}
