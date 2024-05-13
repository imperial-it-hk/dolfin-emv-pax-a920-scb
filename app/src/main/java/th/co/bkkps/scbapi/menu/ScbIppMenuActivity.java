package th.co.bkkps.scbapi.menu;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.BaseMenuActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.MenuPage;

import th.co.bkkps.scbapi.ScbIppService;
import th.co.bkkps.scbapi.trans.ScbIppSaleTran;
import th.co.bkkps.scbapi.trans.ScbIppSaleTran.ScbIppType;
import th.co.bkkps.scbapi.trans.ScbIppVoidTran;
import th.co.bkkps.scbapi.trans.ScbVoidTran;

public class ScbIppMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        if (Component.chkSettlementStatus(Constants.ACQ_SCB_IPP)) {
            showErrorDialog(this, TransResultUtils.getMessage(TransResult.ERR_SETTLE_NOT_COMPLETED));
            return;
        }
    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(ScbIppMenuActivity.this, 6, 2)
                .addTransItem(getString(R.string.menu_scb_type_1), R.drawable.app_sale,
                        new ScbIppSaleTran(ScbIppMenuActivity.this, null, ScbIppType.MERCHANT))
                .addTransItem(getString(R.string.menu_scb_type_2), R.drawable.app_sale,
                        new ScbIppSaleTran(ScbIppMenuActivity.this, null, ScbIppType.CUSTOMER))
                .addTransItem(getString(R.string.menu_scb_type_3), R.drawable.app_sale,
                        new ScbIppSaleTran(ScbIppMenuActivity.this, null, ScbIppType.SPECIAL))
                .addTransItem(getString(R.string.menu_scb_type_4), R.drawable.app_sale,
                        new ScbIppSaleTran(ScbIppMenuActivity.this, null, ScbIppType.PROMO_FIX));
//                .addTransItem(getString(R.string.menu_void), R.drawable.app_void,
//                        new ScbVoidTran(ScbIppMenuActivity.this, null, null))
//                .addActionItem(getString(R.string.menu_report), R.drawable.app_query, doScbHistory());
        return builder.create();
    }

//    private AAction doScbHistory() {
//        return ScbIppService.executeHistoryMenu(ScbIppMenuActivity.this, tickTimer);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        ActivityStack.getInstance().popTo(MainActivity.class);
//    }
}
