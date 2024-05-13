package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.pay.trans.PromptPayOfflineLastTrans;
import com.pax.pay.trans.PromptPayOnlineLastTrans;
import com.pax.view.MenuPage;

/**
 * Created by WITSUTA A on 4/19/2018.
 */

public class PromtPayQRReportLastTransActivity extends BaseMenuActivity{
    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(PromtPayQRReportLastTransActivity.this, 4, 2)
                .addTransItem(getString(R.string.menu_qr_last_online), R.drawable.app_sale,  new PromptPayOnlineLastTrans(PromtPayQRReportLastTransActivity.this, null))
                .addTransItem(getString(R.string.menu_qr_last_offline), R.drawable.app_sale, new PromptPayOfflineLastTrans(PromtPayQRReportLastTransActivity.this, null));

        return builder.create();
    }
}