package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.pay.trans.BPSQrDynamicTrans;
import com.pax.pay.trans.BPSQrStaticTrans;
import com.pax.view.MenuPage;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class DisplayQrMenuActivity extends BaseMenuActivity {

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(DisplayQrMenuActivity.this, 9, 3)
                .addTransItem(getString(R.string.trans_dynamic_qr), R.drawable.app_sale, new BPSQrDynamicTrans(DisplayQrMenuActivity.this, null))
                .addTransItem(getString(R.string.trans_static_qr), R.drawable.app_sale, new BPSQrStaticTrans(DisplayQrMenuActivity.this, null));

        return builder.create();
    }

}
