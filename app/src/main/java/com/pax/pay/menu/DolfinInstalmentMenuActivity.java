package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.DolfinInstalmentTrans;
import com.pax.pay.trans.InstalmentKbankTrans;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.MenuPage;

public class DolfinInstalmentMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        InstalmentKbankTrans kbankInstalmentTrans = new InstalmentKbankTrans(this, null, null);
        int ermExceededResult = kbankInstalmentTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_DOLFIN_INSTALMENT);
        if (acq == null || !acq.isEnable()) {
            showMsgNotAllowed(this);
        }
    }

    @Override
    public MenuPage createMenuPage() {
        int level = getIntent().getIntExtra(EUIParamKeys.NAV_LEVEL.toString(), 0);
        MenuPage.Builder builder = new MenuPage.Builder(this, 6, 2);

        if (level == 0) {
            builder.addMenuItem(getString(R.string.menu_instalment_01), R.drawable.app_sale, DolfinInstalmentMenuActivity.class, 1)
                    .addMenuItem(getString(R.string.menu_instalment_02), R.drawable.app_sale, DolfinInstalmentMenuActivity.class, 2)
                    .addTransItem(getString(R.string.menu_instalment_03), R.drawable.app_sale, new DolfinInstalmentTrans(this, "03", true, null))
                    .addTransItem(getString(R.string.menu_instalment_04), R.drawable.app_sale, new DolfinInstalmentTrans(this, "04", true, null));
        }

        if (level == 1) {
            builder.addTransItem("General Product", R.drawable.app_sale, new DolfinInstalmentTrans(this, "01", false, null))
                    .addTransItem("Promo Product", R.drawable.app_sale, new DolfinInstalmentTrans(this, "01", true, null));
        }

        if (level == 2) {
            builder.addTransItem("General Product", R.drawable.app_sale, new DolfinInstalmentTrans(this, "02", false, null))
                    .addTransItem("Promo Product", R.drawable.app_sale, new DolfinInstalmentTrans(this, "02", true, null));
        }
        return builder.create();
    }
}
