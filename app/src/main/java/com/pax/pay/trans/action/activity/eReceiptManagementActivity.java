package com.pax.pay.trans.action.activity;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.BaseMenuActivity;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionDispMessage;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class eReceiptManagementActivity extends BaseMenuActivity {

    @Override
    public MenuPage createMenuPage() {
        return RecreateMenuPage();
    }

    private MenuPage RecreateMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(eReceiptManagementActivity.this, 9, 3);


        builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_initial_menu), R.drawable.vf_erm_initial_proc,          TerminalRegistration());

        if(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED) != null) {
            builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_download_session_key_menu), R.drawable.vf_erm_download_session_key,   SessionKeyRenewal());
        }

        builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_other_settings_menu), R.drawable.vf_erm_othe_setting,                 OtherSettingsMenu() );
        builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_active_erm_host_menu), R.drawable.vf_erm_view_active_host,            ViewActiveERMHost() );

        if(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED) != null) {
            builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_report_list_menu), R.drawable.vf_erm_ereceipt_report_list, callEReceiptPrintStatusReportAction());
        }

        //builder.addActionItem(getString(R.string.menu_verifone_erm_ereceipt_e_settlement_menu), R.drawable.vf_erm_settings, callESettleManualUpload());
        return builder.create();
    }

    private AAction callESettleManualUpload (){

        ActionEReceipt eReceiptAction = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                int hasUnsettle = hasUnsettlementFile();
                if (hasUnsettle == 0 || hasUnsettle == -999) {
                    ToastUtils.showMessage(eReceiptManagementActivity.this,"E-Settlement Receipt wasn't found");
                } else {
                    // todo: add operation manual upload e-settlement receipt to ERCM-Server
                }
            }
        });
        return eReceiptAction;
    }

    private int hasUnsettlementFile() {
        String path = "/sdcard/PAX/BPSLoader/ERCM/UnsettlementList";
        if (new File(path).exists() == true ){
            if (new File(path).listFiles().length == 0) {
                return 0;
            } else {
                return new File(path).listFiles().length;
            }
        } else {
            return -999;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (EReceiptUtils.detectEReceiptPBKFile().size() > 0) {
            createMenuPage();
        } else {
            EReceiptUtils.getInstance().showMsgErmError(
                    eReceiptManagementActivity.this,
                    CustomAlertDialog.NORMAL_TYPE,
                    getString(R.string.ereceipt_menu_config_missing_detected),
                    Constants.FAILED_DIALOG_SHOW_TIME);
            return;
        }

    }


    private AAction TerminalRegistration () {
        ActionEReceipt eReceiptAction = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                String path = EReceiptUtils.getERM_LogoDirectory(FinancialApplication.getApp().getApplicationContext());
                File dirFile = new File(path);
                if (!dirFile.exists() && !dirFile.mkdir()) {
                ToastUtils.showMessage(eReceiptManagementActivity.this,"Sorry, Loading resources\ntarget logo directory was missing");
                } else {
                    if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE) == false) {
                        ToastUtils.showMessage(eReceiptManagementActivity.this,"E-Signature mode disabled, please turn on");
                    } else {
                        boolean has_KMS_host = check_ercm_kms_acquirer_exists();
                        boolean has_RMS_host = check_ercm_rms_acquirer_exists();
                        if ( has_KMS_host &&  has_RMS_host) {
                            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {
                                if(FinancialApplication.getAcqManager().findCountEnableAcquirersWithEnableERM() > 0) {
                                    ((ActionEReceipt)action).setParam(ActionEReceipt.eReceiptMode.INIT_TERMINAL, eReceiptManagementActivity.this,FinancialApplication.getDownloadManager().getSn(),null);
                                } else {
                                    ToastUtils.showMessage(eReceiptManagementActivity.this,"ERM-Active-host wasn't found");
                                }
                            } else {
                                ToastUtils.showMessage(eReceiptManagementActivity.this,"Sorry, ERM Upload has been disabled");
                            }
                        } else {
                            ToastUtils.showMessage(eReceiptManagementActivity.this,"Missing ERCM-KMS or ERCM-RMS host\nplease check your PAX-Store config.");
                        }
                    }
                }
            }
        });
        eReceiptAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                finish();
            }
        });

        return eReceiptAction;
    }

    public static boolean check_ercm_kms_acquirer_exists () {
        return FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE) != null;
    }
    public static boolean check_ercm_rms_acquirer_exists () {
        return FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE) != null;
    }

    private AAction SessionKeyRenewal () {
        ActionEReceipt sessionKeyRenewal = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE) == false) {
                ToastUtils.showMessage(eReceiptManagementActivity.this,"E-Signature mode disabled, please turn on");
            } else {
                boolean has_KMS_host = check_ercm_kms_acquirer_exists();
                boolean has_RMS_host = check_ercm_rms_acquirer_exists();
                if ( has_KMS_host &&  has_RMS_host) {
                    ((ActionEReceipt)action).setParam(ActionEReceipt.eReceiptMode.DL_SESSION_KEY_ALL_HOST, eReceiptManagementActivity.this, FinancialApplication.getDownloadManager().getSn() ,null);
                } else {
                    ToastUtils.showMessage(eReceiptManagementActivity.this,"Missing ERCM-KMS or ERCM-RMS host, please check");
                }
            }
            }
        });
        sessionKeyRenewal.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                finish();
            }
        });

        return sessionKeyRenewal;
    }

    private AAction OtherSettingsMenu () {
        ActionEReceipt actionEReceipt = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEReceipt)action).setParam(ActionEReceipt.eReceiptMode.OTHER_SETTING, eReceiptManagementActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
            }
        });
        actionEReceipt.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                finish();
            }
        });

        return actionEReceipt;
    }

    String[] statusMsg = new String[0];
    private AAction ViewActiveERMHost() {
        ActionDispMessage confirmInfoAction = new ActionDispMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {

                List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
                List<String> list = new ArrayList<String>();
                statusMsg = new String[acquirers.size()+12];

                int ind=12;
                String status;

                list.add("*************************************");
                list.add(" ERM UPLOAD ENABLE : " + ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) ? "ON" : "OFF"));
                list.add(" INITIAL STATUS : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED))==null ? "NO" : "YES"));
                list.add("*************************************");
                list.add("  - BANK : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE)));
                list.add("  - MERC : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE)));
                list.add("  - STORE : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE)));
                list.add("  - KEY VER : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION)));
                list.add("*************************************");
                list.add("===========================");
                statusMsg[0] = "*************************************";
                statusMsg[1] = " ERM UPLOAD ENABLE : " + ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) ? "ON" : "OFF");
                statusMsg[2] = " INITIAL STATUS : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED))==null ? "NO" : "YES");
                statusMsg[3] = "*************************************";
                statusMsg[4] = " > BANK : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
                statusMsg[5] = " > MERC : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE));
                statusMsg[6] = " > STORE : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE));
                statusMsg[7] = " > KEY VER : " + isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION));
                statusMsg[8] = "*************************************";
                statusMsg[9] = "         DETAIL ERM HOST            ";
                statusMsg[10] = "===========================";

                int sequentNo=0;
                String HostID="";
                String HostName="";
                String HostUploadActive="";
                String strA="";
                String strB ="";
                int maxCharPerLine = 27;
                int paddingLen=0;
                for (Acquirer acquirer : acquirers) {
                    if ( ! acquirer.getName().equals(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE) && !acquirer.getName().equals(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)) {
                        sequentNo+=1;
                        HostID = EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()),   2," ", Convert.EPaddingPosition.PADDING_LEFT);
                        HostName = acquirer.getName();
                        HostUploadActive = ((acquirer.getEnableUploadERM()) ? "ON" : "OFF");
                        strA = sequentNo + "." + HostName;
                        strB = "  : " +  HostUploadActive;


                        status = strA + strB;
                        list.add(status);
                        statusMsg[ind++] = status;
                    }
                }
                list.add("===========================");
                statusMsg[ind++] = "===========================";
                ((ActionDispMessage) action).setParam(eReceiptManagementActivity.this, getString(R.string.menu_verifone_erm_active_host_status), list);
            }
        });
        confirmInfoAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet()==TransResult.SUCC) {
                    ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
                        @Override
                        public void onStart(AAction action) {
                            ((ActionPrintTransMessage) action).setParam(eReceiptManagementActivity.this, statusMsg, null);
                        }
                    });
                    printReceiptAction.setEndListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            fixActivityLeak();
                        }
                    });
                    printReceiptAction.execute();
                } else {
                    fixActivityLeak();
                }
            }
        });
        return confirmInfoAction;
    }

    private String isNullCheck (String input) { if(input==null) { return "-";} else {return input;}}
    private void fixActivityLeak () {
        ActivityStack.getInstance().popTo(eReceiptManagementActivity.this);
        if (TransContext.getInstance().getCurrentAction() != null ) {
            TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
        }
        if (TransContext.getInstance() != null ){
            TransContext.getInstance().setCurrentAction(null); //fix leaks
        }
        finish();
    }

    private AAction callEReceiptPrintStatusReportAction() {
        ActionEReceipt actionEReceipt = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.ERECEIPT_REPORT, eReceiptManagementActivity.this, null, null);
            }
        });
        actionEReceipt.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                ActivityStack.getInstance().popTo(eReceiptManagementActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks

                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(eReceiptManagementActivity.this, "ERM Report", null, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(eReceiptManagementActivity.this, "ERM Report", TransResultUtils.getMessage(result.getRet()), null, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });

        return actionEReceipt;
    }
}
