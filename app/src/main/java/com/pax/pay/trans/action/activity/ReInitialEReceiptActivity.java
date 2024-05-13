package com.pax.pay.trans.action.activity;
import android.content.Context;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.splash.SplashListener;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.action.ActionReIntialEReceipt;

import th.co.bkkps.utils.Log;

public class ReInitialEReceiptActivity extends BaseActivity {
    Context context;
    AAction previousAction;

    @Override
    protected int getLayoutId() {return R.layout.activity_null; }

    @Override
    protected void initViews() {
        context = this;
        Device.enableBackKey(false);

        previousAction = TransContext.getInstance().getCurrentAction();


//        ActionReIntialEReceipt actionReIntialEReceipt = (ActionReIntialEReceipt) TransContext.getInstance().getCurrentAction();
//        if (actionReIntialEReceipt != null) {
//            actionReIntialEReceipt.setEndListener(ReInitialEReceiptActivity.this);
//        }
    }

    @Override
    protected void setListeners() {
        //do nothing
    }

    @Override
    protected void loadParam() {
        //do nothing
        //progressListener = (SplashListener) getIntent().getExtras().getParcelable("progressListener");
    }

    @Override
    protected String getTitleString() {
        return "Re-Initial ERCM";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_null);

        ActionEReceipt eReceiptActionInit = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.INIT_TERMINAL, ReInitialEReceiptActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
                ((ActionEReceipt) action).setExtraParam(true);
                Log.d("INIT*", "ERCM--AUTO--INIT--START");
            }
        });
        eReceiptActionInit.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                Log.d("INIT*", "ERCM--AUTO--INIT--END");
                eReceiptActionInit.setEndListener(null);
                finish(new ActionResult(result.getRet(), null));
//                if (result.getRet() == TransResult.SUCC) {
//                    ActionEReceipt sessionKeyDownloadInit = new ActionEReceipt(new AAction.ActionStartListener() {
//                        @Override
//                        public void onStart(AAction action) {
//                            ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.DL_SESSION_KEY_ALL_HOST, ReInitialEReceiptActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
//                            ((ActionEReceipt) action).setExtraParam(true);
//                            Log.d("INIT*", "ERCM--AUTO--INIT--SSK--START");
//                        }
//                    });
//                    sessionKeyDownloadInit.setEndListener(new AAction.ActionEndListener() {
//                        @Override
//                        public void onEnd(AAction action, ActionResult result) {
//                            Log.d("INIT*", "ERCM--AUTO--INIT--SSK--END");
//                            finish(new ActionResult(result.getRet(), null));
//                        }
//                    });
//                    sessionKeyDownloadInit.execute();
//                } else {
//                    finish(new ActionResult(result.getRet(), null));
//                }

//                ActionEReceipt eReceiptActionSessionKeyRenewal = new ActionEReceipt(new AAction.ActionStartListener() {
//                    @Override
//                    public void onStart(AAction action) {
//                        ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.DL_SESSION_KEY_ALL_HOST, ReInitialEReceiptActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
//                        ((ActionEReceipt) action).setExtraParam(true);
//                        Log.d("INIT*", "ERCM--AUTO--SSK--START");
//                    }
//                });
//                eReceiptActionSessionKeyRenewal.setEndListener(new AAction.ActionEndListener() {
//                    @Override
//                    public void onEnd(AAction action, ActionResult result) {
//                        Log.d("INIT*", "ERCM--AUTO--SSK--END");
//                        finish(new ActionResult(result.getRet(), null));
//                    }
//                });
//                eReceiptActionSessionKeyRenewal.execute();
            }
        });

        eReceiptActionInit.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Device.enableBackKey(true);
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        }

        finish();

        if (previousAction != null) {
            if (previousAction.isFinished()) {
                return;
            }
            previousAction.setFinished(true);
            quickClickProtection.start(); // AET-93
            previousAction.setResult(result);
        }
    }
}
