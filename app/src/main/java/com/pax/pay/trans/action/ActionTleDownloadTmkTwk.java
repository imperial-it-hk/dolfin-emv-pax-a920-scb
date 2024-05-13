package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.LoadTLETrans;
import com.pax.pay.trans.action.activity.TleDownloadTmkTwkActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;

import th.co.bkkps.utils.Log;

public class ActionTleDownloadTmkTwk extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionTleDownloadTmkTwk(ActionStartListener listener) {
        super(listener);
    }

    private Context context;
    private LoadTLETrans.Mode mode = LoadTLETrans.Mode.None;
    private ArrayList<String> selectedTleBankHost = new ArrayList<>();
    private int settleResult;

    public void setParam (Context context, LoadTLETrans.Mode mode, ArrayList<String> selectedTleBankHost, int settleResult) {
        this.context = context;
        this.mode = mode;
        this.selectedTleBankHost = (selectedTleBankHost==null) ? Utils.getTLEPrimaryAcquirerList(Utils.Mode.TleBankName) :  selectedTleBankHost;
        this.settleResult = settleResult;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, TleDownloadTmkTwkActivity.class);
        intent.putExtra("Mode", ((mode== LoadTLETrans.Mode.DownloadTMK) ? 1: 2));
        intent.putExtra("HostList", selectedTleBankHost);
        intent.putExtra("settleResult", settleResult);
        context.startActivity(intent);
    }
}
