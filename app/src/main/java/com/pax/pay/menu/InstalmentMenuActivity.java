package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.InstalmentKbankTrans;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;

public class InstalmentMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        InstalmentKbankTrans kbankInstalmentTrans = new InstalmentKbankTrans(InstalmentMenuActivity.this, null,null);
        int ermExceededResult = kbankInstalmentTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        Acquirer acqSmartPay = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_SMRTPAY);
        Acquirer acqSmartPayBdms = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_SMRTPAY_BDMS);
        if ((acqSmartPay == null || !acqSmartPay.isEnable()) && (acqSmartPayBdms == null || !acqSmartPayBdms.isEnable())) {
            showMsgNotAllowed(InstalmentMenuActivity.this);
        }

    }

    @Override
    public MenuPage createMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(InstalmentMenuActivity.this, 6, 2)
                .addTransItem(getString(R.string.menu_instalment_01), R.drawable.app_sale, new InstalmentKbankTrans(InstalmentMenuActivity.this, "01", null))
                .addTransItem(getString(R.string.menu_instalment_02), R.drawable.app_sale, new InstalmentKbankTrans(InstalmentMenuActivity.this, "02", null))
                .addTransItem(getString(R.string.menu_instalment_03), R.drawable.app_sale, new InstalmentKbankTrans(InstalmentMenuActivity.this, "03", null))
                .addTransItem(getString(R.string.menu_instalment_04), R.drawable.app_sale, new InstalmentKbankTrans(InstalmentMenuActivity.this, "04", null));
        return builder.create();
    }
}
