package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

/**
 * Created by SORAYA S on 26-Apr-18.
 */

public class ShowQRRefActivity extends BaseActivityWithTickForAction {

    private TextView textQrRef;
    private TextView textHeader;
    private Button okQrButton;
    private Button cancelQrButton;

    private String title;
    private String msgRef;
    private TransData transData;
    private String acqName;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_show_qr_ref;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        textQrRef = (TextView) findViewById(R.id.text_qr_ref);
        textHeader = (TextView) findViewById(R.id.text_qr_ref_static);
        okQrButton = (Button) findViewById(R.id.ok_qr_ref_button);
        cancelQrButton = (Button) findViewById(R.id.cancel_qr_ref_button);

        if(msgRef != null && !msgRef.isEmpty()){
            textQrRef.setText(msgRef);
        }else {
            if (transData != null) {
                if (transData.getQrRef2() != null) {
                    textQrRef.setText(transData.getQrRef2());
                }else if(transData.getQrID() != null) {
                    textHeader.setText("QR ID");
                    textQrRef.setText(transData.getQrID());
                }else {
                    finish(new ActionResult(TransResult.ERR_NO_TRANS, null));
                    return;
                }
            }
        }
    }

    @Override
    protected void setListeners() {
        okQrButton.setOnClickListener(this);
        cancelQrButton.setOnClickListener(this);
    }

    @Override
    protected void onClickProtected(View v) {
        switch (v.getId()) {
            case R.id.ok_qr_ref_button:
                finish(new ActionResult(TransResult.SUCC, transData));
                break;
            case R.id.cancel_qr_ref_button:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                break;
        }
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        msgRef = getIntent().getStringExtra(Utils.getString(R.string.qr_trans_ref));
        acqName = getIntent().getStringExtra(Utils.getString(R.string.acquirer));
        if(msgRef == null) {
            TransData lastTransData;
            if (acqName.equals(Constants.ACQ_QR_PROMPT)) {
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
                lastTransData = FinancialApplication.getTransDataDbHelper().findLastTransPromptPayData(acquirer,false);
            }else {
                lastTransData = FinancialApplication.getTransDataDbHelper().findLastQRVisaTransData(true);
            }

            if (lastTransData == null) {
                finish(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            }else if (acqName.equals(Constants.ACQ_QRC) && lastTransData.getTransType() == ETransType.QR_VOID) {
                finish(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            } else {
                transData = lastTransData;
            }

        }
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
