package com.pax.pay.trans.action.activity;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

/**
 * Created by WITSUTA A on 5/9/2018.
 */

public class CheckQRActivity extends BaseActivityWithTickForAction {

    private Button checkQrButton;
    private Button cancelQrButton;

    private String title;
    private TransData transData;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_check_qr;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        checkQrButton = (Button) findViewById(R.id.check_check_qr_button);
        cancelQrButton = (Button) findViewById(R.id.cancel_check_qr_button);
    }

    @Override
    protected void setListeners() {
        checkQrButton.setOnClickListener(this);
        cancelQrButton.setOnClickListener(this);
    }

    @Override
    protected void onClickProtected(View v) {
        switch (v.getId()) {
            case R.id.check_check_qr_button:
                transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
                finish(new ActionResult(TransResult.SUCC, null));
                break;
            case R.id.cancel_check_qr_button:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(CheckQRActivity.this);
                        int ret = new Transmit().sendReversalWallet(transProcessListenerImpl);
                        transProcessListenerImpl.onHideProgress();
                        if(ret == TransResult.SUCC) {
                            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                        } else {
                            finish(new ActionResult(ret, null));
                        }
                    }
                });
                break;
        }
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        transData = Component.getTransDataInstance();
    }

    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(100);
                finish();
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }
}
