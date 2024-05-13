/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-26
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.menu;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionEndListener;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrProcessClass;
import com.pax.pay.MainActivity;
import com.pax.pay.SplashActivity;
import com.pax.pay.WizardActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.BPSPrintMsgTrans;
import com.pax.pay.trans.BPSPrintParamTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionChangeLanguage;
import com.pax.pay.trans.action.ActionChangeScreenSleep;
import com.pax.pay.trans.action.ActionDispSingleLineMsg;
import com.pax.pay.trans.action.ActionDynamicOffline;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionUpdateParam;
import com.pax.pay.trans.action.ActionUpdateSp200;
import com.pax.pay.trans.action.activity.eReceiptManagementActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

import java.io.File;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.activity.LanSettingActivity;
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDSettings;
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDUpdateParam;
import th.co.bkkps.scbapi.ScbIppService;

public class ManageMenuActivity extends BaseMenuActivity {

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(ManageMenuActivity.this, 12, 3)
//                .addActionItem(getString(R.string.trans_password), R.drawable.app_opermag, createInputPwdActionForManagePassword())
                // Lock Terminal don't need it
                //.addActionItem(getString(R.string.trans_lock), R.drawable.pwd_small, new ActionLockTerminal(null))
//                .addMenuItem(getString(R.string.settings_title), R.drawable.app_setting, WizardActivity.class)
                .addActionItem(getString(R.string.settings_title), R.drawable.app_setting, createInputPwdActionForConfigAndDynamicOffline(0))
                //.addMenuItem(getString(R.string.trans_history), R.drawable.app_query, TransQueryActivity.class)
                .addActionItem(getString(R.string.version), R.drawable.app_version, createDispActionForVersion())
                .addActionItem(getString(R.string.update_param), R.drawable.app_update_param, createActionForUpdateParam())
                //.addActionItem(getString(R.string.tsi), R.drawable.app_version, createDispActionForCheckTSI())
                .addActionItem(getString(R.string.trans_tle), R.drawable.app_manage, createInputPwdActionForTle())
//                .addTransItem(getString(R.string.trans_print_msg), R.drawable.app_print, new BPSPrintMsgTrans(ManageMenuActivity.this, null))
                .addActionItem(getString(R.string.trans_print_msg), R.drawable.app_print, createInputPwdActionForConfigAndDynamicOffline(2))
                .addActionItem(getString(R.string.menu_change_language), R.drawable.app_manage, createChangeLanguage())

                .addActionItem(getString(R.string.menu_change_screen_sleep), R.drawable.app_manage, createChangeScreenSleep())

                .addTransItem(getString(R.string.trans_print_param), R.drawable.app_print, new BPSPrintParamTrans(ManageMenuActivity.this, null))
//                .addActionItem(getString(R.string.kbank_menu_title_dynamic_offline), R.drawable.app_manage, DynamicOfflineSettings())
                .addActionItem(getString(R.string.kbank_menu_title_dynamic_offline), R.drawable.app_manage, createInputPwdActionForConfigAndDynamicOffline(1))
                .addMenuItem("LAN Setting", R.drawable.app_setting, LanSettingActivity.class);

                if (SP200_serialAPI.getInstance().isSp200Enable()) {
                    builder.addActionItem(getString(R.string.menu_update_sp200_param), R.drawable.app_sale, UpdateSp200());
                }

                if (EReceiptUtils.isFoundKbankPublicKeyFile()
                        || FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED) != null) {
                    if (SplashActivity.Companion.detectDeveloperMode()) {
                        builder.addMenuItem(getString(R.string.menu_verifone_erm_management_menu), R.drawable.vf_erm_settings, eReceiptManagementActivity.class);
                    }
                    builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_initial_menu), R.drawable.vf_erm_initial_proc, TerminalRegistration());
                }

                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
                Acquirer acqScbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
                if (MultiMerchantUtils.Companion.isMasterMerchant() && (acquirer != null && acquirer.isEnable()) ||
                        (acqScbRedeem != null && acqScbRedeem.isEnable())) {
                    builder.addActionItem(getString(R.string.settings_scb_config), R.drawable.app_setting, doScbConfig());
                }

                acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX);
                if (MultiMerchantUtils.Companion.isMasterMerchant() && (acquirer != null && acquirer.isEnable())) {
                    builder.addActionItem(getString(R.string.settings_amex_config), R.drawable.app_setting, doAmexConfig());
                }

                if (MultiMerchantUtils.Companion.isMasterMerchant() && (FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KCHECKID).isEnable())) {
                    builder.addActionItem(getString(R.string.menu_kcheckid_config_menu), R.drawable.app_setting, KCheckIDConfigs());
                }

                if(Utils.isSandboxBuild())
                    builder.addActionItem(getString(R.string.menu_close_app), R.drawable.app_close, createInputPwdActionForCloseApp());

        return builder.create();
    }

    private AAction UpdateSp200(){
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 8,
                        getString(R.string.prompt_update_sp200_pwd), null);
            }
        });
        inputPasswordAction.setEndListener(new ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks

                if (result.getRet() != TransResult.SUCC) {
                    return;
                }

                ActionUpdateSp200 actionUpdateSp200 = new ActionUpdateSp200(new AAction.ActionStartListener() {
                    @Override
                    public void onStart(AAction action) {
                        ((ActionUpdateSp200) action).setParam(ManageMenuActivity.this);
                    }
                });
                actionUpdateSp200.execute();
            }
        });

        return inputPasswordAction;
    }

    private AAction DynamicOfflineSettings() {


        ActionDynamicOffline dynamicOfflineActions = new ActionDynamicOffline(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionDynamicOffline) action).setParam(ManageMenuActivity.this, ActionDynamicOffline.enumDynamicOfflineMode.settings);
            }
        });
        dynamicOfflineActions.setEndListener(new ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    }
                };

                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(ManageMenuActivity.this, "Success", dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(ManageMenuActivity.this, "Cancelled",
                            "operation has been cancel by user", dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });

        return dynamicOfflineActions;
    }

    private AAction createDispActionForVersion() {
        ActionDispSingleLineMsg displayInfoAction = new ActionDispSingleLineMsg(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDispSingleLineMsg) action).setParam(ManageMenuActivity.this,
                        getString(R.string.version), getString(R.string.app_version), FinancialApplication.getVersion(), 60);
            }
        });

        displayInfoAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
            }
        });

        return displayInfoAction;
    }

    private AAction createDispActionForCheckTSI() {
        final TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransData();
        String tsi;
        try {
            tsi = transData.getTsi();
        } catch (Exception e) {
            tsi = "";
        }
        final String finalTsi = tsi;
        ActionDispSingleLineMsg displayInfoAction = new ActionDispSingleLineMsg(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDispSingleLineMsg) action).setParam(ManageMenuActivity.this,
                        getString(R.string.tsi), getString(R.string.app_tsi), finalTsi, 60);
            }
        });

        displayInfoAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
            }
        });

        return displayInfoAction;
    }

    private AAction createActionForUpdateParam() {
        ActionUpdateParam actionUpdateParam = new ActionUpdateParam(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionUpdateParam) action).setParam(ManageMenuActivity.this, true);
            }
        });
        actionUpdateParam.setEndListener(new ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
            }
        });

        return actionUpdateParam;
    }

    private AAction createInputPwdActionForSettings() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 8,
                        getString(R.string.prompt_sys_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks

                if (result.getRet() != TransResult.SUCC) {
                    return;
                }
/*
                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_SYS_PWD))) {
                    DialogUtils.showErrMessage(ManageMenuActivity.this, getString(R.string.settings_title),
                            getString(R.string.err_password), null, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }

 */
                Intent intent = new Intent(ManageMenuActivity.this, WizardActivity.class);
                startActivity(intent);

            }
        });

        return inputPasswordAction;

    }

    private AAction createInputPwdActionForManagePassword() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 6,
                        getString(R.string.prompt_merchant_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks

                if (result.getRet() != TransResult.SUCC) {
                    finish(result);
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_MERCHANT_PWD))) {
                    DialogUtils.showErrMessage(ManageMenuActivity.this, getString(R.string.trans_password),
                            getString(R.string.err_password), new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    finish();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                Intent intent = new Intent(ManageMenuActivity.this, PasswordMenuActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EUIParamKeys.NAV_TITLE.toString(), getString(R.string.trans_password));
                bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        return inputPasswordAction;
    }

    private AAction createInputPwdActionForTle() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 6,
                        getString(R.string.tle_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {

                if (result.getRet() != TransResult.SUCC) {
                    TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                    TransContext.getInstance().setCurrentAction(null); //fix leaks
                    ActivityStack.getInstance().popTo(MainActivity.class);
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                    DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                            TransContext.getInstance().setCurrentAction(null); //fix leaks
                            ActivityStack.getInstance().popTo(MainActivity.class);
                        }
                    };

                    DialogUtils.showErrMessage(ManageMenuActivity.this, getString(R.string.settings_title),
                            getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                Intent intent = new Intent(ManageMenuActivity.this, TleMenuActivity.class);
                startActivity(intent);
            }
        });

        return inputPasswordAction;

    }

    private AAction createChangeLanguage() {
        ActionChangeLanguage changeLanguageAction = new ActionChangeLanguage(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionChangeLanguage) action).setParam(ManageMenuActivity.this,
                        getString(R.string.menu_change_language), false);
            }
        });

        changeLanguageAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
                if (result.getRet() != TransResult.SUCC) {
                    return;
                }
            }
        });
        return changeLanguageAction;
    }

    private AAction createChangeScreenSleep() {
        ActionChangeScreenSleep changeScreenSleep = new ActionChangeScreenSleep(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionChangeScreenSleep) action).setParam(ManageMenuActivity.this,
                        getString(R.string.menu_change_screen_sleep), false);
            }
        });

        changeScreenSleep.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
                if (result.getRet() != TransResult.SUCC) {
                    return;
                }
            }
        });
        return changeScreenSleep;
    }

    private AAction createDispActionForLinkPOSProtocolResult() {
        String result = null;
        try {
            if (FinancialApplication.getEcrProcess().mProtoFilter != null) {
                EcrProcessClass.PROTOCOL_RESULT protocolResult = FinancialApplication.getEcrProcess().mProtoFilter.getProtocolResult();
                result = protocolResult != null ? protocolResult.toString() : "";
            }
        } catch (Exception e) {
            result = "";
        }
        final String finalResult = result;
        ActionDispSingleLineMsg displayInfoAction = new ActionDispSingleLineMsg(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDispSingleLineMsg) action).setParam(ManageMenuActivity.this,
                        getString(R.string.protocol), getString(R.string.protocol_result), finalResult, 60);
            }
        });

        displayInfoAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                ActivityStack.getInstance().pop();
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
            }
        });

        return displayInfoAction;
    }

    private AAction createInputPwdActionForCloseApp() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 6,
                        getString(R.string.prompt_merchant_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {

                if (result.getRet() != TransResult.SUCC) {
                    TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                    TransContext.getInstance().setCurrentAction(null); //fix leaks
                    ActivityStack.getInstance().popTo(MainActivity.class);
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                    DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                            TransContext.getInstance().setCurrentAction(null); //fix leaks
                            ActivityStack.getInstance().popTo(MainActivity.class);
                        }
                    };

                    DialogUtils.showErrMessage(ManageMenuActivity.this, getString(R.string.settings_title),
                            getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                Device.enableBackKey(true);
                Device.enableHomeRecentKey(true);
                Device.enableStatusBar(true);

                finishAffinity();
                System.exit(0);
            }
        });

        return inputPasswordAction;

    }

    private AAction createInputPwdActionForConfigAndDynamicOffline(int selectedMenu) {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(action -> {
            tickTimer.stop();
            ((ActionInputPassword) action).setParam(ManageMenuActivity.this, 6,
                    getString(R.string.prompt_terminal_pwd), null);
        });

        inputPasswordAction.setEndListener((action, result) -> {
            TransContext.getInstance().setCurrentAction(null); //fix leaks
/*
            if (result.getRet() != TransResult.SUCC) {
                finish(result);
                return;
            }

            String data = EncUtils.sha1((String) result.getData());
            if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_CONFIG_PWD))) {
                DialogUtils.showErrMessage(ManageMenuActivity.this, getString(R.string.trans_password),
                        getString(R.string.err_password), arg0 -> finish(), Constants.FAILED_DIALOG_SHOW_TIME);
                return;
            }


 */
            switch (selectedMenu) {
                case 0:
                    Intent intent = new Intent(ManageMenuActivity.this, WizardActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(EUIParamKeys.NAV_TITLE.toString(), getString(R.string.settings_title));
                    bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
                case 1:
                    DynamicOfflineSettings().execute();
                    break;
                case 2:
                    new BPSPrintMsgTrans(ManageMenuActivity.this, null).execute();
                    break;
            }
        });

        return inputPasswordAction;
    }

    private AAction KCheckIDConfigs() {
        ActionKCheckIDUpdateParam updateParamAction = new ActionKCheckIDUpdateParam(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionKCheckIDUpdateParam)action).setParam(ManageMenuActivity.this);
            }
        });
        updateParamAction.setEndListener(new ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet()==TransResult.SUCC) {
                    ActionKCheckIDSettings actionKCheckIDSettings = new ActionKCheckIDSettings(new AAction.ActionStartListener() {
                        @Override
                        public void onStart(AAction action) {
                            ((ActionKCheckIDSettings) action).setParam(ManageMenuActivity.this);
                        }
                    }) ;
                    actionKCheckIDSettings.setEndListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            if (result.getRet() == TransResult.SUCC) {
                                DialogUtils.showSuccMessage(ManageMenuActivity.this, " ", null, Constants.SUCCESS_DIALOG_SHOW_TIME);
                            } else {
                                DialogUtils.showErrMessage(ManageMenuActivity.this, "KCheckID settings error", "save configuration failed", null, Constants.FAILED_DIALOG_SHOW_TIME);
                            }
                            finish();
                        }
                    });
                    actionKCheckIDSettings.execute();
                } else {
                    finish();
                }
            }
        });

        return updateParamAction;
    }

    private AAction TerminalRegistration() {
        if (SysParam.getInstance().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION) == null) {
            ActionEReceipt eReceipt = new ActionEReceipt(null);
            eReceipt.readAndSetKeyVersion();
        }

        ActionEReceipt eReceiptAction = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                String path = EReceiptUtils.getERM_LogoDirectory(FinancialApplication.getApp().getApplicationContext());
                File dirFile = new File(path);
                if (!dirFile.exists() && !dirFile.mkdir()) {
                    ToastUtils.showMessage(ManageMenuActivity.this, "Sorry, Loading resources\ntarget logo directory was missing");
                } else {
                    if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE)) {
                        ToastUtils.showMessage(ManageMenuActivity.this, "E-Signature mode disabled, please turn on");
                    } else {
                        boolean has_KMS_host = eReceiptManagementActivity.check_ercm_kms_acquirer_exists();
                        boolean has_RMS_host = eReceiptManagementActivity.check_ercm_rms_acquirer_exists();
                        if (has_KMS_host && has_RMS_host) {
                            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
                                if (FinancialApplication.getAcqManager().findCountEnableAcquirersWithEnableERM() > 0) {
                                    ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.INIT_TERMINAL, ManageMenuActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
                                } else {
                                    ToastUtils.showMessage(ManageMenuActivity.this, "ERM-Active-host wasn't found");
                                }
                            } else {
                                ToastUtils.showMessage(ManageMenuActivity.this, "Sorry, ERM Upload has been disabled");
                            }
                        } else {
                            ToastUtils.showMessage(ManageMenuActivity.this, "Missing ERCM-KMS or ERCM-RMS host\nplease check your PAX-Store config.");
                        }
                    }
                }
            }
        });
        eReceiptAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(ManageMenuActivity.this, " ", null, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(ManageMenuActivity.this, "ERCM Initial Error", Utils.getString(TransResult.ERCM_INITIAL_PROCESS_FAILED), null, Constants.FAILED_DIALOG_SHOW_TIME);
                }
                finish();
            }
        });

        return eReceiptAction;
    }

    private AAction doScbConfig() {
        return ScbIppService.executeConfigMenu(ManageMenuActivity.this, tickTimer);
    }

    private AAction doAmexConfig() {
        return AmexTransAPI.getInstance().getProcess().doConfig(ManageMenuActivity.this, tickTimer);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ActivityStack.getInstance().popTo(MainActivity.class);
    }
}
