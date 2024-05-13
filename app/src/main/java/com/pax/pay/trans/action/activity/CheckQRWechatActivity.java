package com.pax.pay.trans.action.activity;

import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

/**
 * Created by Nannaphat S on 8/3/2019.
 */

public class CheckQRWechatActivity extends BaseActivityWithTickForAction {

    private Button checkQrButton;
    private Button cancelQrButton;

    private String title;
    private TransData transData;
    private ActionInputPassword inputPasswordAction = null;
    private final AAction currentAction = TransContext.getInstance().getCurrentAction();

    private TextView text_check_qr;

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

        text_check_qr = (TextView) findViewById(R.id.text_check_qr);
        text_check_qr.setText(getString(R.string.show_qr_text));
        checkQrButton.requestFocus();
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
                TransContext.getInstance().setCurrentAction(currentAction); //fix leaks
                finish(new ActionResult(TransResult.SUCC, null));
                break;
            case R.id.cancel_check_qr_button:
                runInputPwdAction();
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
                /*setResult(100);
                finish();*/
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    private void runInputPwdAction() {
        inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(CheckQRWechatActivity.this, 6,
                        getString(R.string.prompt_cancel_pwd), null, true);
                ((ActionInputPassword) action).setParam(TransResult.ERR_USER_CANCEL);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() != TransResult.SUCC) {
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_CANCEL_PWD))) {
                    DialogUtils.showErrMessage(CheckQRWechatActivity.this, getString(R.string.pwd_cancel),
                            getString(R.string.err_password), null, Constants.FAILED_DIALOG_SHOW_TIME);
                    TransContext.getInstance().setCurrentAction(currentAction); //fix leaks
                } else {
                    onComfirmCancel();
                }

            }
        });

        inputPasswordAction.execute();
    }

    private void onComfirmCancel(){
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransContext.getInstance().setCurrentAction(currentAction);
//                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(CheckQRWechatActivity.this);
//                int ret = new Transmit().sendReversalQR(transProcessListenerImpl,transData.getAcquirer());
//                transProcessListenerImpl.onHideProgress();
//                if(ret == TransResult.SUCC) {
                    finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
//                } else {
//                    finish(new ActionResult(ret, null));
//                }
            }
        });
    }

    @Override
    protected boolean onKeyBackDown() {
        runInputPwdAction();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode,event);
        if (keyCode == KeyEvent.KEYCODE_BACK){
            runInputPwdAction();
        } else if (keyCode == KeyEvent.KEYCODE_ENTER){
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
            TransContext.getInstance().setCurrentAction(currentAction); //fix leaks
            finish(new ActionResult(TransResult.SUCC, null));
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkQrButton.requestFocus();
    }
}
