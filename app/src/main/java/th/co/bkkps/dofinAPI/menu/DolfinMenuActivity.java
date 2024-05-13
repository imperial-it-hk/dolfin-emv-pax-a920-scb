package th.co.bkkps.dofinAPI.menu;

import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.BaseMenuActivity;
import com.pax.pay.menu.DolfinInstalmentMenuActivity;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionSendAdvice;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.utils.Utils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.tran.DolfinSaleTran;
import th.co.bkkps.dofinAPI.tran.DolfinVoidTran;


public class DolfinMenuActivity extends BaseMenuActivity {
    @Override
    protected void onResume() {
        super.onResume();

        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_DOLFIN);

        if (acq == null || !acq.isEnable())
            acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_DOLFIN_INSTALMENT);

        if (acq == null || !acq.isEnable()) {
            showMsgNotAllowed(DolfinMenuActivity.this);
        }
    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(DolfinMenuActivity.this, 6, 2);

        if (DolfinApi.getInstance().getDolfinServiceBinded())
            //.addMenuItem(getString(R.string.menu_qr_sale), R.drawable.app_sale, PromptPayQRSaleMenuActivity.class)
            builder.addTransItem(getString(R.string.menu_sale), R.drawable.dolfin_icon, new DolfinSaleTran(DolfinMenuActivity.this, null))
                    //.addTransItem("VOID", R.drawable.app_void, new DolfinVoidTran(DolfinMenuActivity.this, null))
                    // .addMenuItem(getString(R.string.menu_qr_report), R.drawable.app_query, PromptPayQRReportMenuActivity.class)
                    .addActionItem("REFUND", R.drawable.app_refund, SendAdvicePromptpay());

        if (Utils.isEnableDolfinInstalment())
            builder.addMenuItem(getString(R.string.menu_dolfin_instalment), R.drawable.app_sale, DolfinInstalmentMenuActivity.class);

        return builder.create();
    }

    private AAction SendAdvicePromptpay() {

        final Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
        ActionSendAdvice sendAdvice = new ActionSendAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionSendAdvice) action).setParam(DolfinMenuActivity.this,
                        "QR Advice", null, acquirer);
            }
        });

        sendAdvice.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                // Fixed EDCBBLAND-362
                DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityStack.getInstance().popTo(MainActivity.class);
                        TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                    }
                };

                if (result.getRet() == 0) {
                    DialogUtils.showSuccMessage(DolfinMenuActivity.this, getString(R.string.menu_qr_advice), onDismissListener,
                            Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(DolfinMenuActivity.this, getString(R.string.menu_qr_advice), getString(R.string.err_no_trans), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });
        return sendAdvice;
    }
}

