package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.pay.trans.BPSQrCodeSaleTrans;
import com.pax.pay.trans.BPSQrInquiryTrans;
import com.pax.pay.trans.QrInquiryTrans;
import com.pax.pay.trans.QrSaleTrans;
import com.pax.view.MenuPage;

/**
 * Created by WITSUTA A on 4/20/2018.
 */

public class PromptPayQRSaleMenuActivity extends BaseMenuActivity {
    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(PromptPayQRSaleMenuActivity.this, 4, 2)
                .addTransItem("Prompt Pay", R.drawable.app_sale, new BPSQrCodeSaleTrans(PromptPayQRSaleMenuActivity.this, null))
                .addTransItem(getString(R.string.menu_qr_sale_inquiry), R.drawable.app_sale, new BPSQrInquiryTrans(PromptPayQRSaleMenuActivity.this, null));
        return builder.create();
    }
}


