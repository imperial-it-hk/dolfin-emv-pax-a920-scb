package com.pax.pay.menu;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.LoadTLETrans;
import com.pax.pay.trans.LoadTMKTrans;
import com.pax.pay.trans.LoadTWKTrans;
import com.pax.pay.trans.TleStatusTrans;
import com.pax.pay.trans.ClearKeyTrans;
import com.pax.view.MenuPage;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.amexapi.trans.AmexLoadLogOnTpkTrans;

/**
 * Created by Adisorn S on 5-Apr-18.
 */

public class TleMenuActivity extends BaseMenuActivity {

    @Override
    public MenuPage createMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(TleMenuActivity.this, 9, 3);
        //builder.addTransItem(getString(R.string.trans_tle_load), R.drawable.app_void, new LoadTMKTrans(TleMenuActivity.this, null));
        //builder.addTransItem(getString(R.string.trans_tle_logon), R.drawable.app_void, new LoadTWKTrans(TleMenuActivity.this, null));
        builder.addTransItem(getString(R.string.trans_tle_load), R.drawable.app_void, new LoadTLETrans(TleMenuActivity.this,null, LoadTLETrans.Mode.DownloadTMK));
        builder.addTransItem(getString(R.string.trans_tle_logon), R.drawable.app_void, new LoadTLETrans(TleMenuActivity.this,null, LoadTLETrans.Mode.DownloadTWK));

        Acquirer activeAcquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
        if (activeAcquirer != null) {
            builder.addTransItem(getString(R.string.trans_tpk_load_logon), R.drawable.app_void, new AmexLoadLogOnTpkTrans(TleMenuActivity.this, null));
        }

        builder.addTransItem(getString(R.string.trans_tle_status), R.drawable.app_void, new TleStatusTrans(TleMenuActivity.this, null));
        builder.addTransItem(getString(R.string.trans_clear_key), R.drawable.app_void, new ClearKeyTrans(TleMenuActivity.this, null));
        return builder.create();
    }
}
