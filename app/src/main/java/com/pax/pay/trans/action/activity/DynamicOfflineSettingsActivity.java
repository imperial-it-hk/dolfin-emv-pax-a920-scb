package com.pax.pay.trans.action.activity;

import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.BaseMenuActivity;
import com.pax.pay.trans.TransContext;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;

public class DynamicOfflineSettingsActivity extends BaseActivity {
    private String TAG="DYNAMIC-OFFLINE-SETTING";

    @Override
    protected int getLayoutId() { return R.layout.activity_dynamic_offline_settings; }


    Button btn_cancel                   = null;
    Button btn_save                     = null;
    EditText edtx_session_timeout       = null;
    EditText edtx_floorlimit_visa       = null;
    EditText edtx_floorlimit_mastercard = null;
    EditText edtx_floorlimit_jcb        = null;

    @Override
    protected void initViews() {
        enableBackAction(false);

        btn_cancel = (Button) findViewById(R.id.btn_cancel) ;
        btn_save   = (Button) findViewById(R.id.btn_save) ;
        edtx_session_timeout          = (EditText) findViewById(R.id.edtx_session_timeout) ;
        edtx_floorlimit_visa          = (EditText) findViewById(R.id.edtx_floor_limit_visacard) ;
        edtx_floorlimit_mastercard    = (EditText) findViewById(R.id.edtx_floor_limit_mastercard) ;
        edtx_floorlimit_jcb           = (EditText) findViewById(R.id.edtx_floor_limit_jcbcard) ;

        setDefaultValue();
        Log.d(TAG, " # DOL Setting UI is ready ");
    }


    private String ConvertLongToString(long val) {
        String lngStr = "";
        if (val > 100) {
            String tmpVal = String.valueOf(val);
            String prefix = String.valueOf(val/100);
            String decimalplace = tmpVal.substring(tmpVal.length()-2);
            lngStr = prefix + "." + decimalplace;
        } else {
            if (val >= 10) {
                lngStr = "0." + String.valueOf(val);
            } else {
                lngStr = "0.0" + String.valueOf(val);
            }
        }

        return lngStr;
    }

    private void setDefaultValue(){
        DynamicOffline.getInstance().loadParam();
        EditText currentObject = null ;
        currentObject = edtx_session_timeout;
        if ((DynamicOffline.getInstance().getSessionTimeout() == 0)) {
            currentObject.setText("30");
        }
        else {
            currentObject.setText(String.valueOf(DynamicOffline.getInstance().getSessionTimeout()) );
        }

        currentObject = edtx_floorlimit_visa;
        if ((DynamicOffline.getInstance().getVisaCardFloorlimit() == 0)) {
            currentObject.setText("1500.00");
        }
        else {
            currentObject.setText(ConvertLongToString(DynamicOffline.getInstance().getVisaCardFloorlimit()));
        }

        currentObject = edtx_floorlimit_mastercard;
        if ((DynamicOffline.getInstance().getMastercardFloorlimit() == 0)) {
            currentObject.setText("1500.00");
        }
        else {
            currentObject.setText(ConvertLongToString(DynamicOffline.getInstance().getMastercardFloorlimit()));
        }

        currentObject = edtx_floorlimit_jcb;
        if ((DynamicOffline.getInstance().getJcbCardFloorlimit() != 0)) {
            currentObject.setText("1500.00");
        }
        else {
            currentObject.setText(ConvertLongToString(DynamicOffline.getInstance().getJcbCardFloorlimit()));
        }
    }

//    private void setDefaultValue(){
//        DynamicOffline.getInstance().loadParam();
//        EditText currentObject = null ;
//        currentObject = edtx_session_timeout;       if ((DynamicOffline.getInstance().getSessionTimeout() != 150000))     { currentObject.setText(String.valueOf(DynamicOffline.getInstance().getSessionTimeout()) ); } else { currentObject.setText("30");}
//        currentObject = edtx_floorlimit_visa;       if ((DynamicOffline.getInstance().getVisaCardFloorlimit() != 150000)) { currentObject.setText(String.valueOf(DynamicOffline.getInstance().getVisaCardFloorlimit()/100) + "." + String.valueOf(DynamicOffline.getInstance().getVisaCardFloorlimit()).substring(String.valueOf(DynamicOffline.getInstance().getVisaCardFloorlimit()).length()-2)); } else { currentObject.setText("1500.00");}
//        currentObject = edtx_floorlimit_mastercard; if ((DynamicOffline.getInstance().getVisaCardFloorlimit() != 150000)) { currentObject.setText(String.valueOf(DynamicOffline.getInstance().getMastercardFloorlimit()/100)+ "." + String.valueOf(DynamicOffline.getInstance().getMastercardFloorlimit()).substring(String.valueOf(DynamicOffline.getInstance().getMastercardFloorlimit()).length()-2)); } else { currentObject.setText("1500.00");}
//        currentObject = edtx_floorlimit_jcb;        if ((DynamicOffline.getInstance().getVisaCardFloorlimit() != 150000)) { currentObject.setText(String.valueOf(DynamicOffline.getInstance().getJcbCardFloorlimit()/100)+ "." + String.valueOf(DynamicOffline.getInstance().getJcbCardFloorlimit()).substring(String.valueOf(DynamicOffline.getInstance().getJcbCardFloorlimit()).length()-2)); } else { currentObject.setText("1500.00");}
//    }

    @Override
    protected void setListeners() {
        btn_cancel.setOnClickListener(this);
        btn_save.setOnClickListener(this);
    }

    @Override
    protected void loadParam() {

    }

//    @Override
//    protected String getTitleString() {
//        super.enableDisplayTitle(true);
//        return "Dynamic Offline Settings";
//    }

    @Override
    protected void onClickProtected(View v) {
        btn_save.setClickable(false);
        btn_cancel.setClickable(false);
        edtx_session_timeout.setActivated(false);
        edtx_floorlimit_visa.setActivated(false);
        edtx_floorlimit_mastercard.setActivated(false);
        edtx_floorlimit_jcb.setActivated(false);

        switch (v.getId()) {
            case R.id.btn_cancel :
                finish(new ActionResult(TransResult.ERR_USER_CANCEL,null));
                break;
            case R.id.btn_save :
                // check null
                if (edtx_session_timeout.getText().toString().trim().length()==0 ) { ToastUtils.showMessage("Please insert Dynamic-Offline session timeout."); ReEnableControls(); return;}
                if (edtx_session_timeout.getText().toString().trim().equals("0")) { ToastUtils.showMessage("Session Timeout cannot be zero."); ReEnableControls(); return;}
                if (edtx_floorlimit_visa.getText().toString().trim().length()==0) { ToastUtils.showMessage("Please insert VISA-CARD floor limit."); ReEnableControls(); return;}
                if (edtx_floorlimit_mastercard.getText().toString().trim().length()==0) { ToastUtils.showMessage("Please insert MASTERCARD floor limit."); ReEnableControls(); return;}
                if (edtx_floorlimit_jcb.getText().toString().trim().length()==0) { ToastUtils.showMessage("Please insert JCB-CARD floor limit."); ReEnableControls(); return;}


                // cast invalid format
                String tmpString = "";
                if (edtx_floorlimit_visa.getText().toString().indexOf(".") == -1)       { tmpString=edtx_floorlimit_visa.getText().toString() ;         edtx_floorlimit_visa.setText(tmpString + ".00");}
                if (edtx_floorlimit_mastercard.getText().toString().indexOf(".") == -1) { tmpString=edtx_floorlimit_mastercard.getText().toString() ;   edtx_floorlimit_mastercard.setText(tmpString + ".00");}
                if (edtx_floorlimit_jcb.getText().toString().indexOf(".") == -1)        { tmpString=edtx_floorlimit_jcb.getText().toString() ;          edtx_floorlimit_jcb.setText(tmpString + ".00");}

                int timeout = Utils.parseIntSafe(edtx_session_timeout.getText().toString(),30);
                long fl_vsc = Utils.parseLongSafe(edtx_floorlimit_visa.getText().toString().replace(".",""),150000);
                long fl_mcc = Utils.parseLongSafe(edtx_floorlimit_mastercard.getText().toString().replace(".",""),150000);
                long fl_jcb = Utils.parseLongSafe(edtx_floorlimit_jcb.getText().toString().replace(".",""),150000);

                if (edtx_session_timeout.getText().toString().trim().equals("")==false) {
                    DynamicOffline.getInstance().setSessionTimeout(timeout);
                }
                if (edtx_floorlimit_visa.getText().toString().trim().equals("")==false) {
                    DynamicOffline.getInstance().setVisaCardFloorlimit(fl_vsc);
                }
                if (edtx_floorlimit_mastercard.getText().toString().trim().equals("")==false) {
                    DynamicOffline.getInstance().setMastercardFloorlimit(fl_mcc);
                }
                if (edtx_floorlimit_jcb.getText().toString().trim().equals("")==false) {
                    DynamicOffline.getInstance().setJcbCardFloorlimit(fl_jcb);
                }

                finish(new ActionResult(TransResult.SUCC,null));
                break;
        }

        finish();

    }

    boolean save_rejection = false;

    private void ReEnableControls() {
        btn_save.setClickable(true);
        btn_cancel.setClickable(true);
        edtx_session_timeout.setActivated(true);
        edtx_floorlimit_visa.setActivated(true);
        edtx_floorlimit_mastercard.setActivated(true);
        edtx_floorlimit_jcb.setActivated(true);

    }


    @Override
    protected boolean onKeyBackDown() {
        setResult(TransResult.ERR_USER_CANCEL);
        finish();

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
        } else {
            super.finish();
        }
    }
}
