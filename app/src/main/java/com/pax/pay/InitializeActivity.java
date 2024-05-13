/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-27
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.AbsoluteSizeSpan;
import android.view.Window;
import android.widget.EditText;

import com.pax.abl.utils.EncUtils;
import com.pax.appstore.DownloadManager;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.db.MerchantAcqProfileDb;
import com.pax.pay.db.MerchantProfileDb;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.emv.EmvCapk;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.component.KeyDataReadWriteJson;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.util.List;

import th.co.bkkps.utils.Log;

public class InitializeActivity extends BaseActivity {

    public static final int REQ_WIZARD = 1;

    private EditText edtPwd;

    public static boolean onCheckInit(Activity activity, int requestCode) {
        if (FinancialApplication.getController().isFirstRun()) {
            Intent intent = new Intent(activity, InitializeActivity.class);
            activity.startActivityForResult(intent, requestCode);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        //start wizard activity, Skip password insert by Kui
        Intent intent = new Intent(InitializeActivity.this, WizardActivity.class);
        startActivityForResult(intent, REQ_WIZARD);

    }

    @Override
    protected void loadParam() {
        // do nothing
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_initialize_pwd_layout;
    }

    @Override
    protected void initViews() {
        enableActionBar(false);
        edtPwd = (EditText) findViewById(R.id.operator_pwd_edt);
        SpannableString ss = new SpannableString(Utils.getString(R.string.plz_enter_pwd));
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(32, true);
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        edtPwd.setHint(new SpannedString(ss));
        edtPwd.requestFocus();

    }

    @Override
    protected void setListeners() {
        edtPwd.setOnEditorActionListener(new PwdActionListener());
    }

    private class PwdActionListener extends EditorActionListener {
        @Override
        public void onKeyOk() {
            process();
        }

        @Override
        public void onKeyCancel() {
            exit();
        }
        /**
        * check password
        */
        private void process() {
            String password = edtPwd.getText().toString().trim();
            if (password.isEmpty()) {
                edtPwd.setFocusable(true);
                edtPwd.requestFocus();
                return;
            }
            if (!EncUtils.sha1(password).equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TERMINAL_PWD))) {
                ToastUtils.showMessage(R.string.error_password);
                edtPwd.setText("");
                edtPwd.setFocusable(true);
                edtPwd.requestFocus();
                return;
            }

            //start wizard activity
            Intent intent = new Intent(InitializeActivity.this, WizardActivity.class);
            startActivityForResult(intent, REQ_WIZARD);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        // exit app
        exit();
        return true;
    }

    protected void onReqWizard() {
        //remain for process result
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        insertAcquirer();
        insertMerchantProfile();
        Component.insertTransTypeMapping();
        initEMVParam();
        FinancialApplication.getController().set(Controller.IS_FIRST_RUN, false);
        KeyDataReadWriteJson.updateKeyDataToAcquirers();
        finish();
    }

    private void exit() {
        DialogUtils.showExitAppDialog(InitializeActivity.this);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_WIZARD) {
            onReqWizard();
        }
    }

    private int initEMVParam() {
        Log.d(TAG, "Initial App: IS_FIRST_RUN = true");
        ToastUtils.showMessage(R.string.emv_param_load);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                // emv公钥下载
                Controller controller = FinancialApplication.getController();
                if (controller.get(Controller.NEED_DOWN_CAPK) == Controller.Constant.YES) {
                    EmvCapk.load(Utils.readObjFromJSON("capk.json", EmvCapk.class));
                    controller.set(Controller.NEED_DOWN_CAPK, Controller.Constant.NO);
                }
                // emv 参数下载
                if (controller.get(Controller.NEED_DOWN_AID) == Controller.Constant.YES) {
                    EmvAid.load(Utils.readObjFromJSON("aid.json", EmvAid.class));
                    DownloadManager.getInstance().updateCtlsTransLimit(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_CTLS_TRANS_LIMIT, "150000"));
                    controller.set(Controller.NEED_DOWN_AID, Controller.Constant.NO);
                }
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.showMessage(R.string.emv_init_succ);
                    }
                });
            }
        });
        return TransResult.SUCC;
    }

    private void insertAcquirer() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<Acquirer> allAcquirer = acqManager.findAllAcquirers();
        if(allAcquirer.isEmpty()){
            List<Acquirer> acquirers = Utils.readObjFromJSON("acquirer.json", Acquirer.class);
            List<Issuer> issuers = Utils.readObjFromJSON("issuer.json", Issuer.class);
            List<CardRange> cardRanges = Utils.readObjFromJSON("card_range.json", CardRange.class);

            for (Acquirer acquirer : acquirers)
                acqManager.insertAcquirer(acquirer);

            FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, acquirers.get(0).getName());
            acqManager.setCurAcq(acquirers.get(0));

            for (Issuer issuer : issuers) {
                acqManager.insertIssuer(issuer);
                for (Acquirer acquirer : acquirers)
                    acqManager.bind(acquirer, issuer);
                for (CardRange cardRange : cardRanges) {
                    if (cardRange.getIssuerName().equals(issuer.getName())) {
                        cardRange.setIssuer(issuer);
                        acqManager.insertCardRange(cardRange);
                    }
                }
            }
        }
    }

    //
    // Multi-Merchant init
    //
    private void insertMerchantProfile() {
        List<MerchantProfile> allMerchantProfiles = MerchantProfileDb.INSTANCE.findAllData();
        if(allMerchantProfiles.isEmpty()){
            List<MerchantProfile> merchantProfiles = Utils.readObjFromJSON("merchant_profile.json", MerchantProfile.class);
            List<MerchantAcqProfile> merchantAcqProfiles = Utils.readObjFromJSON("merchant_acq_profile.json", MerchantAcqProfile.class);

            SysParam sysParam = FinancialApplication.getSysParam();
            // Add Main profile to index 0
            MerchantProfile defaultMerchantProfile = new MerchantProfile();

            String defaultMerchantName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            // check is any same name in merchant_profile.json
            // if yes then replace <this for testing only>
            Boolean isDefaultMerchSet = false;
            for (MerchantProfile merchantProfile : merchantProfiles) {
                if (merchantProfile.getMerchantLabelName().equals(defaultMerchantName)) {
                    isDefaultMerchSet = true;
                    break;
                }
            }

            if(!isDefaultMerchSet) {
                defaultMerchantProfile.setMerchantLabelName(defaultMerchantName);
                String address = "";
                if (sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS) != null) {
                    address = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
                }
                defaultMerchantProfile.setMerchantPrintAddress1(address);

                address = "";
                if (sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1) != null) {
                    address = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
                }
                defaultMerchantProfile.setMerchantPrintAddress2(address);
                defaultMerchantProfile.setMerchantLogo("kasikornbanklogo");

                MerchantProfileDb.INSTANCE.insertData(defaultMerchantProfile);
            }

            for (MerchantProfile merchantProfile : merchantProfiles)
                MerchantProfileDb.INSTANCE.insertData(merchantProfile);

            AcqManager acqManager = FinancialApplication.getAcqManager();
            List<Acquirer> acquirers = acqManager.findAllAcquirers();
            for (Acquirer acquirer : acquirers) {
                if (MerchantProfileManager.INSTANCE.isSupportAcq(acquirer.getName())){
                    MerchantAcqProfile defaultMerchantAcqProfile = new MerchantAcqProfile();
                    defaultMerchantAcqProfile.setMerchantName(defaultMerchantProfile.getMerchantLabelName());
                    defaultMerchantAcqProfile.setAcqHostName(acquirer.getName());
                    defaultMerchantAcqProfile.setMerchantId(acquirer.getMerchantId());
                    defaultMerchantAcqProfile.setTerminalId(acquirer.getTerminalId());
                    defaultMerchantAcqProfile.setCurrBatchNo(acquirer.getCurrBatchNo());
                    MerchantAcqProfileDb.INSTANCE.insertData(defaultMerchantAcqProfile);
                }
            }

            for (MerchantAcqProfile merchantAcqProfile : merchantAcqProfiles) {
                MerchantAcqProfileDb.INSTANCE.insertData(merchantAcqProfile);
            }
        }

        List<MerchantProfile> merchantProfiles = MerchantProfileManager.INSTANCE.getAllMerchant();
        assert merchantProfiles != null;
        MerchantProfileManager.INSTANCE.applyProfileAndSave(merchantProfiles.get(0).getMerchantLabelName());

    }
}
