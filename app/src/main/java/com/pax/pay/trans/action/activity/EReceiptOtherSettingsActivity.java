package com.pax.pay.trans.action.activity;

import android.content.DialogInterface;
import android.text.InputFilter;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.InputFilterMinMax;
import com.pax.pay.utils.ToastUtils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.HashMap;

public class EReceiptOtherSettingsActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_electronic_receipt_other_settings_layout;
    }

    Button btn_cancel;
    Button btn_save;

    EditText tv_bank_code;
    EditText tv_store_code;
    EditText tv_merchant_code;
    private CheckBox chkboxEnableEReceipt;
    private CheckBox chkboxEnablePrintingAfterTxn;
    private CheckBox chkboxEnablePrintingPreSettlement;

    private boolean onInitStatus = true;
    private CheckBox chkPrintUploadedMerc;
    private CheckBox chkPrintUploadedCust;
    private CheckBox chkPrintUnableToUploadMerc;
    private CheckBox chkPrintUnableToUploadCust;
    private CheckBox chkNextTransUploadEnable;
    private CheckBox chkSettleForcePrintAllTrans;

    String title = "E-Receipt Other Settings";
    @Override
    protected void initViews() {
        onInitStatus =true;
        super.enableActionBar(true);
        enableDisplayTitle(true);
        enableBackAction(true);
        setPublicKey();
        setDefualtPrintingSlip();


        //this.title = getString(R.string.menu_verifone_erm_ereceipt_other_settings_menu);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_save = (Button) findViewById(R.id.btn_save);

        tv_bank_code = (EditText) findViewById(R.id.tx_edtx_bank_code);
        tv_store_code = (EditText) findViewById(R.id.tx_edtx_store_code);
        tv_merchant_code = (EditText) findViewById(R.id.tx_edtx_merchant_code);

        chkboxEnableEReceipt =              (CheckBox) findViewById(R.id.chk_enable_erm);
        chkboxEnablePrintingAfterTxn =      (CheckBox) findViewById(R.id.chk_enable_print_txn);
        chkboxEnablePrintingPreSettlement = (CheckBox) findViewById(R.id.chk_print_pre_settle);

        chkPrintUploadedMerc =              (CheckBox) findViewById(R.id.chk_print_uploaded_merc);
        chkPrintUploadedCust =              (CheckBox) findViewById(R.id.chk_print_uploaded_cust);
        chkPrintUnableToUploadMerc =        (CheckBox) findViewById(R.id.chk_print_upload_error_merc);
        chkPrintUnableToUploadCust =        (CheckBox) findViewById(R.id.chk_print_upload_error_cust);
        chkNextTransUploadEnable =          (CheckBox) findViewById(R.id.chk_print_support_pending_upload);
        chkSettleForcePrintAllTrans=        (CheckBox) findViewById(R.id.chk_print_force_settle_print_all);

        String local_bank_code = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE);
        String local_store_code = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE);
        String local_merchant_code = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE);
        if (local_bank_code != null)     {tv_bank_code.setText(local_bank_code);}
        if (local_store_code != null)    {tv_store_code.setText(local_store_code);}
        if (local_merchant_code != null) {tv_merchant_code.setText(local_merchant_code);}

        chkboxEnableEReceipt.setChecked(FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE));
        chkboxEnablePrintingAfterTxn.setChecked(FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN));

        boolean bool_presettle      = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE);
        boolean bool_forcePrintAll  = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS);
        chkboxEnablePrintingPreSettlement.setChecked(bool_presettle);
        if (bool_presettle) {
            chkSettleForcePrintAllTrans.setChecked(bool_forcePrintAll);
        }

        int NumSlipAfterERMUpload = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP);
        int NumSlipUploadERMError = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD);
        switch (NumSlipAfterERMUpload) {
            case 0 :
                chkPrintUploadedMerc.setChecked(false);
                chkPrintUploadedCust.setChecked(false);
                break;
            case 1 :
                chkPrintUploadedMerc.setChecked(true);
                chkPrintUploadedCust.setChecked(false);
                break;
            case 2 :
                chkPrintUploadedMerc.setChecked(true);
                chkPrintUploadedCust.setChecked(true);
                break;
            case 3 :
                chkPrintUploadedMerc.setChecked(false);
                chkPrintUploadedCust.setChecked(true);
                break;
        }
        switch (NumSlipUploadERMError) {
            case 0 :
                chkPrintUnableToUploadMerc.setChecked(false);
                chkPrintUnableToUploadCust.setChecked(false);
                break;
            case 1 :
                chkPrintUnableToUploadMerc.setChecked(true);
                chkPrintUnableToUploadCust.setChecked(false);
                break;
            case 2 :
                chkPrintUnableToUploadMerc.setChecked(true);
                chkPrintUnableToUploadCust.setChecked(true);
                break;
            case 3 :
                chkPrintUnableToUploadMerc.setChecked(false);
                chkPrintUnableToUploadCust.setChecked(true);
                break;
        }
        chkNextTransUploadEnable.setChecked(FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD));


        setOnDismissListener();
        if(absoluteEReceiptCheck()==true) {
            isBackFromAbsolutePaperlessMode=true;
        }
        onInitStatus=false;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        setResult(TransResult.ERR_USER_CANCEL);
        finish();
        return true;
    }

    private void setPublicKey() {
        HashMap<String,String> Hash = EReceiptUtils.detectEReceiptPBKFile();

        if(Hash.size() == 4) {
            // Extract PBK details
            byte[] prod_exp = Tools.str2Bcd(Hash.get("EXPONENT").replace(" " ,""));
            byte[] prod_mod = Tools.str2Bcd(Hash.get("MODULUS").replace(" " ,""));
            byte[] prod_hsh = Tools.str2Bcd(Hash.get("HASH").replace(" " ,""));

//            if (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE) == null) {
//                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE, Hash.get("BANK_CODE"));
//            }
//            if (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE) == null) {
//                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE, Hash.get("MERCHANT_CODE"));
//            }
//            if (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE) == null) {
//                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE, Hash.get("STORE_CODE"));
//            }
            if (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION) == null) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION, Hash.get("KEY_VERSION"));
            }
        }
    }

    private void setDefualtPrintingSlip() {
        if (FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP) == 0) {
            FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP, 3);
        }
        if (FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD) == 0) {
            FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD, 2);
        }
    }


    @Override
    protected void setListeners() {
        btn_cancel.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        chkboxEnablePrintingAfterTxn.setOnClickListener(this);
        chkboxEnablePrintingPreSettlement.setOnClickListener(this);

        chkPrintUploadedMerc.setOnClickListener(this);
        chkPrintUploadedCust.setOnClickListener(this);
        chkPrintUnableToUploadMerc.setOnClickListener(this);
        chkPrintUnableToUploadCust.setOnClickListener(this);
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void loadParam() {

    }

    @Override
    protected boolean onKeyBackDown() {
        setResult(TransResult.ERR_USER_CANCEL);
        finish();

        return true;
    }

    private DialogInterface.OnDismissListener  dismissListener;
    private void setOnDismissListener() {
        dismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActivityStack.getInstance().pop();
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
            }
        };
    }

    private boolean absoluteEReceiptCheck () {
        if (chkPrintUploadedMerc.isChecked()==false && chkPrintUploadedCust.isChecked()==false && chkPrintUnableToUploadMerc.isChecked()==false && chkPrintUnableToUploadCust.isChecked()==false) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.btn_save){
            boolean flag_validation_bankCode = false;
            boolean flag_validation_storeCode = false;
            boolean flag_validation_merchantCode =false;

            if(tv_bank_code.getText().toString().length()      == 0 )
                { ToastUtils.showMessage("Invalid Bank-code format."); } else {flag_validation_bankCode=true;}
            if((tv_store_code.getText().toString().length()    ==  0)  && (flag_validation_bankCode))
                { ToastUtils.showMessage("please insert Store code."); } else {flag_validation_storeCode=true;}
            if((tv_merchant_code.getText().toString().length() ==  0)  && (flag_validation_storeCode))
                { ToastUtils.showMessage("please insert Merchant code."); } else {flag_validation_merchantCode=true;}

            if (flag_validation_bankCode
                    && flag_validation_storeCode
                    && flag_validation_merchantCode)
                {
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE,true);
                    FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE,tv_bank_code.getText().toString());
                    FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE,tv_store_code.getText().toString());
                    FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE,tv_merchant_code.getText().toString());
                    FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER,FinancialApplication.getDownloadManager().getSn());

                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE, chkboxEnableEReceipt.isChecked());
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN, chkboxEnablePrintingAfterTxn.isChecked());

                    int numbSlipPrintingAfterERMUpload = NumbSlip(chkPrintUploadedMerc,       chkPrintUploadedCust);
                    int numbSlipPrintingUploadERMError = NumbSlip(chkPrintUnableToUploadMerc, chkPrintUnableToUploadCust);
                    boolean nextTransUpload = chkNextTransUploadEnable.isChecked();
                    FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP,                 numbSlipPrintingAfterERMUpload);
                    FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD,   numbSlipPrintingUploadERMError);
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD,  nextTransUpload);

                    boolean bool_presettle      = chkboxEnablePrintingPreSettlement.isChecked();
                    boolean bool_forceprintall  = chkSettleForcePrintAllTrans.isChecked();
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE, bool_presettle);
                    if (bool_presettle == true) {
                        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS, bool_forceprintall);
                    }
                    DialogUtils.showSuccMessage(EReceiptOtherSettingsActivity.this, "Result", dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                    setResult(TransResult.ERCM_OTHER_SETTING_SUCC, null);
                }
            else
                {
                return;
                }
        } else if (v.getId() == R.id.btn_cancel){
            DialogUtils.showErrMessage(EReceiptOtherSettingsActivity.this, "Result",getString(R.string.listener_verifone_erm_other_setting_user_cancel) ,dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
            setResult(TransResult.ERCM_OTHER_SETTING_USER_CANCEL,null);
        } else if (v.getId() == R.id.chk_enable_print_txn) {
            if(onInitStatus==false) {
                if (chkboxEnablePrintingAfterTxn.isChecked()==true) {
                    chkPrintUploadedMerc.setEnabled(true);
                    chkPrintUploadedCust.setEnabled(true);
                    chkPrintUnableToUploadMerc.setEnabled(true);
                    chkPrintUnableToUploadCust.setEnabled(true);
                } else {
                    chkPrintUploadedMerc.setEnabled(false);
                    chkPrintUploadedCust.setEnabled(false);
                    chkPrintUnableToUploadMerc.setEnabled(false);
                    chkPrintUnableToUploadCust.setEnabled(false);

                }
            }
        } else if (v.getId() == R.id.chk_print_pre_settle)  {
            if(onInitStatus==false) {
                if (chkboxEnablePrintingPreSettlement.isChecked()==true) {
                    chkSettleForcePrintAllTrans.setEnabled(true);
                } else {
                    chkSettleForcePrintAllTrans.setEnabled(false);

                }
            }
        } else if (v.getId() == R.id.chk_print_uploaded_merc || v.getId() == R.id.chk_print_uploaded_cust || v.getId() == R.id.chk_print_upload_error_merc || v.getId() == R.id.chk_print_upload_error_cust) {
            if(absoluteEReceiptCheck()==true) {
                isBackFromAbsolutePaperlessMode=true;
                ToastUtils.showMessage("Now, ERM is on absolute paperless mode");
            } else {
                if (isBackFromAbsolutePaperlessMode) {
                    isBackFromAbsolutePaperlessMode=false;
                    ToastUtils.showMessage("Absolute paperless mode disabled");
                }
            }
        }
    }
    private boolean isBackFromAbsolutePaperlessMode = false;

    private int NumbSlip(CheckBox MercChebox, CheckBox CustChebox){
        int returnInt = -1 ;
        if  (MercChebox.isChecked() == true) {
            if  (CustChebox.isChecked() == true) {
                returnInt= 2;
            } else {
                returnInt= 1;
            }
        } else {
            if  (CustChebox.isChecked() == true) {
                returnInt= 3;
            } else {
                returnInt= 0;
            }
        }
        return returnInt;
    }
}
