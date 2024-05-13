package com.pax.pay.trans.action.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;

import java.util.ArrayList;

import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.LoadTleMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.utils.Log;

public class ScbLoadTleActivity extends BaseActivity {
    private String jsonTe =  null;
    private ArrayList<String> scbSelectAcquirer;
    private ITransAPI transAPI;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_null;
    }

    @Override
    protected void initViews() {

    }

    @Override
    protected void setListeners() {

    }

    @Override
    protected void loadParam() {
        jsonTe = getIntent().getExtras().getString("SCB_JSON_TEID", null);
        scbSelectAcquirer = getIntent().getStringArrayListExtra("SCB_SELECT_ACQ");
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (jsonTe != null) {
            LoadTleMsg.Request loadTleRequest = new LoadTleMsg.Request();
            loadTleRequest.setJsonTe(jsonTe);
            loadTleRequest.setSelectAcq(scbSelectAcquirer);
            transAPI = TransAPIFactory.createTransAPI();
            transAPI.startTrans(this, loadTleRequest);
        } else {
            finish(new ActionResult(TransResult.ERR_TLE_NOT_LOAD,null));
        }
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

            handleScbTleStatus(loadTleRes.getExtraBundle());

            if (loadTleRes.getRspCode() != TransResult.SUCC) {
                finish(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
                return;
            }

        }
        finish(new ActionResult(TransResult.SUCC,null));
    }

    private void finish (ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action!= null) {
            if (!action.isFinished()) {
                action.setFinished(true);
            }
            action.setResult(result);
        }
    }

    private void handleScbTleStatus(Bundle bundle) {
        if (bundle != null) {
            int tleStatus = -1;
            if (bundle.containsKey(th.co.bkkps.bpsapi.Constants.SCB_TLE_STATUS)) {
                tleStatus = bundle.getInt(th.co.bkkps.bpsapi.Constants.SCB_TLE_STATUS, -1);
            }

            Acquirer ippAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            Acquirer redeemAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            if (scbSelectAcquirer.size() > 1) {
                if (ippAcq != null && redeemAcq != null) {
                    ippAcq.setTMK(null);
                    ippAcq.setTWK(null);
                    redeemAcq.setTMK(null);
                    redeemAcq.setTWK(null);
                    switch (tleStatus) {
                        case th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_IPP_SUCCESS:
                            ippAcq.setTMK("Y");
                            ippAcq.setTWK("Y");
                            break;
                        case th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_OLS_SUCCESS:
                            redeemAcq.setTMK("Y");
                            redeemAcq.setTWK("Y");
                            break;
                        case th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_ALL_SUCCESS:
                            ippAcq.setTMK("Y");
                            ippAcq.setTWK("Y");
                            redeemAcq.setTMK("Y");
                            redeemAcq.setTWK("Y");
                            break;
                    }
                }
            } else {
                if (ippAcq != null && tleStatus == th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_IPP_SUCCESS) {
                    ippAcq.setTMK("Y");
                    ippAcq.setTWK("Y");
                } else if (redeemAcq != null && tleStatus == th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_OLS_SUCCESS) {
                    redeemAcq.setTMK("Y");
                    redeemAcq.setTWK("Y");
                }
            }
            FinancialApplication.getAcqManager().updateAcquirer(ippAcq);
            FinancialApplication.getAcqManager().updateAcquirer(redeemAcq);
        }
    }
}
