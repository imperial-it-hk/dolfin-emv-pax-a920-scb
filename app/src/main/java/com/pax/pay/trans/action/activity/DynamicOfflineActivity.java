package com.pax.pay.trans.action.activity;

import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.BaseMenuActivity;
import com.pax.pay.trans.DynamicOfflineTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionDynamicOffline;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.model.ETransType;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;

public class DynamicOfflineActivity extends BaseMenuActivity {

    public String  getSysParam(SysParam.StringParam sysParam)  { return FinancialApplication.getSysParam().get(sysParam); }
    public Boolean getSysParam(SysParam.BooleanParam sysParam) { return FinancialApplication.getSysParam().get(sysParam); }

    @Override
    public MenuPage createMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(DynamicOfflineActivity.this, 9,3);
        //builder.addTransItem(getString(R.string.KBANK_DYNAMIC_OFFLINE_MENU_ENABLED), R.drawable.dnmoffln_offline, new DynamicOfflineTrans( DynamicOfflineActivity.this, ETransType.OFFLINE_TRANS_SEND,null));
        builder.addActionItem(getString(R.string.KBANK_DYNAMIC_OFFLINE_MENU_ENABLED), R.drawable.dnmoffln_offline, enableDynamicOfflineMode());
        builder.addActionItem(getString(R.string.KBANK_DYNAMIC_OFFLINE_MENU_DISABLED), R.drawable.dnmoffln_online, disableDynamicOfflineMode());
        return builder.create();
    }

    public AAction enableDynamicOfflineMode() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(DynamicOfflineActivity.this, 6, getString(R.string.prompt_dynamic_offline_pwd), null);
            }
        });
        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(TransContext.getInstance().getCurrentAction() != null) {TransContext.getInstance().getCurrentAction().setFinished(false);}//AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    }
                };

                boolean verifyPwd = DynamicOffline.getInstance().VerifyInputPassword((String)result.getData());
                if (verifyPwd==true) {
                    Log.d("DYNAMIC-OFFLINE", "[ENABLE-MENU] : PASSWORD VERIFIED = CORRECT");
                    DialogUtils.showSuccMessage(DynamicOfflineActivity.this, getString(R.string.kbank_verify_result_valid_password), dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    Log.d("DYNAMIC-OFFLINE", "[ENABLE-MENU] : PASSWORD VERIFIED = INCORRECT");
                    DialogUtils.showErrMessage(DynamicOfflineActivity.this, getString(R.string.kbank_verify_result), getString(R.string.kbank_verify_result_invalid_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });

        return inputPasswordAction;
    }

    public AAction disableDynamicOfflineMode(){
        ActionDynamicOffline actionDynamicOffline = new ActionDynamicOffline(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                 ((ActionDynamicOffline) action).setParam(DynamicOfflineActivity.this, ActionDynamicOffline.enumDynamicOfflineMode.disabled);
            }
        });
        actionDynamicOffline.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(TransContext.getInstance().getCurrentAction() != null) {TransContext.getInstance().getCurrentAction().setFinished(false);}//AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    }
                };
                if(result.getRet()== TransResult.SUCC) {
                    Log.d("DYNAMIC-OFFLINE", "[DISABLE-MENU] : DISABLED");
                    DynamicOffline.getInstance().resetDynamicOffline();
                    DialogUtils.showSuccMessage(DynamicOfflineActivity.this, getString(R.string.kbank_dynamic_offline_disable), dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else if(result.getRet()== TransResult.ERR_USER_CANCEL)  {
                    Log.d("DYNAMIC-OFFLINE", "[DISABLE-MENU] : USER CANCELLED");
                    DialogUtils.showErrMessage(DynamicOfflineActivity.this, getString(R.string.kbank_menu_dynamic_offline), getString(R.string.kbank_dynamic_offline_operation_cancelled_by_user), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                } else if (result.getRet()== TransResult.DYNAMIC_OFFLINE_STILL_DISABLED) {
                    Log.d("DYNAMIC-OFFLINE", "[DISABLE-MENU] : REJECTED (STATUS : DISABLED)");
                    DialogUtils.showErrMessage(DynamicOfflineActivity.this,  getString(R.string.kbank_menu_dynamic_offline_negative), getString(R.string.kbank_dynamic_offline_status_disable), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });


        return actionDynamicOffline;
    }

    @Override
    protected String getTitleString() {
        super.enableDisplayTitle(true);
        return getString(R.string.kbank_menu_dynamic_offline);
    }


}
