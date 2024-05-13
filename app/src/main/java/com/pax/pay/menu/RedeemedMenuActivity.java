package com.pax.pay.menu;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.RedemptionKbankTrans;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;

import th.co.bkkps.utils.DynamicOffline;

public class RedeemedMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        RedemptionKbankTrans kbankRedeemTrans = new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_INQUIRY,null);
        int ermExceededResult = kbankRedeemTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }


        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_REDEEM);
        Acquirer redeemBdms = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_REDEEM_BDMS);
        if ((acq == null || !acq.isEnable()) && (redeemBdms == null || !redeemBdms.isEnable())) {
            showMsgNotAllowed(RedeemedMenuActivity.this);
        }
    }

    @Override
    public MenuPage createMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(RedeemedMenuActivity.this, 6, 2)
                .addTransItem(getString(R.string.menu_redeem_product), R.drawable.app_adjust, new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_PRODUCT, null))
                .addTransItem(getString(R.string.menu_redeem_voucher), R.drawable.app_adjust, new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_VOUCHER, null))
                .addTransItem(getString(R.string.menu_redeem_voucher_credit), R.drawable.app_adjust, new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, null))
                .addTransItem(getString(R.string.menu_redeem_discount), R.drawable.app_adjust, new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_DISCOUNT, null))
                .addTransItem(getString(R.string.menu_redeem_inquiry), R.drawable.app_query, new RedemptionKbankTrans(RedeemedMenuActivity.this, ETransType.KBANK_REDEEM_INQUIRY, null));
        return builder.create();
    }
}
