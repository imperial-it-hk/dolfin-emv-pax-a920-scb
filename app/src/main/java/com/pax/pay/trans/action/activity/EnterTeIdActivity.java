/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.TerminalEncryptionParam;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Bank;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.settings.SysParam;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.LoadTleMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.scbapi.ScbIppService;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;
import th.co.bkkps.utils.Log;

public class EnterTeIdActivity extends BaseActivityWithTickForAction {

    private Button confirmBtn;

    private String navTitle;
    private boolean navBack;

    EditText te_id_text;
    EditText te_pin_text;

    private boolean autoTle;

    private boolean loadScbTle;
    private boolean isBypassMode;

    private List<EnterTeIdActivity.TE> tleid = null;

    private static boolean isParamSuccess = false;
    private boolean scbRecvResp = false;
    private ITransAPI transAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transAPI = TransAPIFactory.createTransAPI();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_te_id;
    }

    @Override
    protected void loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
        loadScbTle = getIntent().getBooleanExtra("SCB_TLE", false);
        isBypassMode = getIntent().getBooleanExtra("BYPASS_MODE", false);
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        te_id_text = (EditText) findViewById(R.id.te_id_input_text);
        te_pin_text = (EditText) findViewById(R.id.te_pin_input_text);
        confirmBtn = (Button) findViewById(R.id.te_confirm);
    }

    @Override
    protected void setListeners() {
        enableBackAction(navBack);
        confirmBtn.setOnClickListener(this);

        if (isBypassMode) {
            onConfirmResult();
            return;
        }
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.te_confirm) {
            onConfirmResult();
        }
    }

    private void onConfirmResult() {
        TE kbankTE = null;
        TE gcsTE = null;
        if (!te_id_text.getText().toString().isEmpty() || !te_pin_text.getText().toString().isEmpty()) {
            autoTle = false;
            // For PackLoadTMK
            FinancialApplication.getUserParam().setTE_ID(te_id_text.getText().toString());
            FinancialApplication.getUserParam().setTE_PIN(te_pin_text.getText().toString());
            // For PackKBankLoadTMK
            FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.KBANK, te_id_text.getText().toString(), te_pin_text.getText().toString()));
            // For PackBayLoadTMK
            FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.GCS, te_id_text.getText().toString(), te_pin_text.getText().toString()));
            // For SCB Load TLE
            FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.SCB, te_id_text.getText().toString(), te_pin_text.getText().toString()));
            // For AMEX Load TLE
            FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.AMEX, te_id_text.getText().toString(), te_pin_text.getText().toString()));
        }
        else {
            autoTle = true;
            // load te from file
            tleid = getTeidFromJsonFile();
            kbankTE = getRandomTeId(tleid, Bank.KBANK);
            gcsTE = getRandomTeId(tleid, Bank.GCS);

            if (kbankTE == null && gcsTE == null) {
                finish(new ActionResult(TransResult.ERR_PARAM, null));
                return;
            }
            else {
                // Just for Show
                te_id_text.setText(kbankTE.TE_ID);
                te_pin_text.setText(kbankTE.TE_PIN);
            }

            // For PackLoadTMK
            FinancialApplication.getUserParam().setTE_ID(kbankTE.TE_ID);
            FinancialApplication.getUserParam().setTE_PIN(kbankTE.TE_PIN);
            // For PackKBankLoadTMK
            FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.KBANK, kbankTE.TE_ID, kbankTE.TE_PIN));
            if (gcsTE != null) {
                // For PackBayLoadTMK
                FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.GCS, gcsTE.TE_ID, gcsTE.TE_PIN));
            }
        }

        //FinancialApplication.getUserParam().setTE_ID(te_id_text.getText().toString());
        //FinancialApplication.getUserParam().setTE_PIN(te_pin_text.getText().toString());

//        String jsonTe = null;

//        if (loadScbTle && ScbIppService.isSCBInstalled(this)) {

//            StringBuilder sb = new StringBuilder();
//            try {
//                InputStream is = getAssets().open("teid.json");
//                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
//
//                while ((jsonTe = br.readLine()) != null) {
//                    sb.append(jsonTe);
//                }
//                jsonTe = sb.toString();
//                br.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


//            if (autoTle) {
//                String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
//                File f = new File(tleFilePath);
//                if (f.exists() && !f.isDirectory()) {
//
//                    ObjectMapper mapper = new ObjectMapper();
//                    try {
//                        JsonNode jsonNode = mapper.readTree(f);
//                        jsonTe = jsonNode.toString();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        finish();
//                        return;
//                    }
//                } else {
//                    finish(new ActionResult(TransResult.SUCC, this));
//                }
//            } else {
//                jsonTe = "{\"TLE\" : [{\"BANK_NAME\": \"SCB\",\"TE_ID\": \""+te_id_text.getText()+"\",\"TE_PIN\": \""+te_pin_text.getText()+"\"}]}";
//            }
//
//            doScbTleProcess(EnterTeIdActivity.this,transAPI, TransContext.getInstance().getCurrentAction(), jsonTe);
//        } else {
            finish(new ActionResult(TransResult.SUCC, this));
//        }
    }



    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    public static class TE {
        private TE() {
            BANK_NAME = "";
            TE_ID = "";
            TE_PIN = "";
        }

        public String BANK_NAME;
        public String TE_ID;
        public String TE_PIN;
    }

    private static final String TleJsonFileName = "teid.json";

    public static List<EnterTeIdActivity.TE> getTeidFromJsonFile() {

//        AssetManager am = FinancialApplication.getApp().getResources().getAssets();
//        InputStream is = null;


//        try {
//            InputStream stream = FinancialApplication.getApp().getAssets().open("teid.json");
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode jsonNode = mapper.readTree(stream);
//            String arrayString = jsonNode.get("TLE").toString();
//            tleid = Arrays.asList(mapper.readValue(arrayString, TE[].class));
//        }
//        catch (IOException ex) {
//            ex.printStackTrace();
//        }

        String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
        if (tleFilePath != null) {
            File f = new File(tleFilePath);
            if (f.exists() && !f.isDirectory()) {

                ObjectMapper mapper = new ObjectMapper();
                try {
                    //is = am.open(TleJsonFileName);

                    JsonNode jsonNode = mapper.readTree(f);
                    String arrayString = jsonNode.get("TLE").toString();
                    return Arrays.asList(mapper.readValue(arrayString, TE[].class));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return  null;
    }

    private TE getRandomTeId() {
        TE te = null;
        if (tleid != null) {
            Random rand = new Random();
            te = tleid.get(rand.nextInt(tleid.size()));
        }
        return te;
    }

    public static TE getRandomTeId(List<TE> tleList, Bank bank) {
        TE te = null;

        try {
            List<TE> list = new ArrayList<>();
            for (TE _te: tleList) {
                if (_te.BANK_NAME.compareToIgnoreCase(bank.name()) == 0) {
                    list.add(_te);
                }
            }

            if (list.size() > 0) {
                Random rand = new Random();
                te = list.get(rand.nextInt(list.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return te;
    }

    public static void doScbTleProcess(final Context context,final ITransAPI transAPI, final AAction currentAction, final String jsonTe) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LoadTleMsg.Response loadTleRes = (LoadTleMsg.Response) transAPI.onResult(requestCode, resultCode, data);

        if (loadTleRes != null) {
            Log.d("BpsApi", "getRspCode="+loadTleRes.getRspCode());
            Log.d("BpsApi", "getStanNo="+loadTleRes.getStanNo());
            Log.d("BpsApi", "getVoucherNo="+loadTleRes.getVoucherNo());
            Component.incStanNo(loadTleRes.getStanNo());
            Component.incTraceNo(loadTleRes.getVoucherNo());

            if (loadTleRes.getRspCode() != TransResult.SUCC) {
                finish(new ActionResult(TransResult.SUCC, FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO),
                        FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO)));
                return;
            }

        }

        finish(new ActionResult(TransResult.SUCC, FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO),
                FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO)));
    }
}