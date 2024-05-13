package com.pax.pay.trans;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadManager;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterRedeemKbank;
import com.pax.pay.trans.action.ActionEnterRedeemKbank.RedeemKbankInfo;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.ReservedFieldHandle;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.util.HashMap;

public class RedemptionKbankTrans extends BaseTrans {
    private byte searchCardMode = -1; // search card mode
    private String title;

    private boolean needFallBack = false;
    private byte currentMode;
    private int cntTryAgain = 0;

    public RedemptionKbankTrans(Context context, ETransType transType, TransEndListener transListener) {
        super(context, transType, Constants.ACQ_REDEEM, transListener);
        setTitle(transType);
        setBackToMain(true);
    }

    private void setTitle(ETransType transType) {
        switch (transType) {
            case KBANK_REDEEM_PRODUCT:
                title = getString(R.string.menu_redeem_product);
                break;
            case KBANK_REDEEM_VOUCHER:
                title = getString(R.string.menu_redeem_voucher);
                break;
            case KBANK_REDEEM_VOUCHER_CREDIT:
                title = getString(R.string.menu_redeem_voucher_credit);
                break;
            case KBANK_REDEEM_DISCOUNT:
                title = getString(R.string.menu_redeem_discount);
                break;
            case KBANK_REDEEM_INQUIRY:
                title = getString(R.string.menu_redeem_inquiry);
                break;
        }
    }

    @Override
    protected void bindStateOnAction() {
        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), title, searchCardMode, transData.getAmount(),
                        null, Utils.getString(R.string.prompt_default_searchCard_prompt), transData,true);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, true);

        ActionEmvReadCardProcess emvReadCardAction = new ActionEmvReadCardProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvReadCardProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(InstalmentKbankTrans.State.EMV_READ_CARD.toString(), emvReadCardAction);

        //todo: enter redemption data action
        ActionEnterRedeemKbank enterRedeemKbankAction = new ActionEnterRedeemKbank(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterRedeemKbank) action).setParam(getCurrentContext(), title,transData);
            }
        });
        bind(State.ENTER_REDEEMED_DATA.toString(), enterRedeemKbankAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }

        });
        bind(State.MAG_ONLINE.toString(), transOnlineAction);

        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount(), !Component.isAllowSignatureUpload(transData));
            }
        });
        bind(State.SIGNATURE.toString(), signatureAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(RedemptionKbankTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // ERM Maximum TransExceed Check
        int ErmExeccededResult = ErmLimitExceedCheck();
        if (ErmExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ErmExeccededResult,null));
            return;
        }

        if (searchCardMode == -1) {
            searchCardMode = Component.getCardReadMode(transType);
        }
        transData.setAmount("0");
        gotoState(State.CHECK_CARD.toString());
    }

    enum State {
        CHECK_CARD,
        EMV_READ_CARD,
        ENTER_REDEEMED_DATA,
        ENTER_REF1_REF2,
        MAG_ONLINE,
        SIGNATURE,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);
        switch (state) {
            case CHECK_CARD:
                onCheckCard(result);
                break;
            case EMV_READ_CARD:
                onEmvReadCard(result);
                break;
            case ENTER_REDEEMED_DATA:
                onEnterRedeemedData(result);
                break;
            case ENTER_REF1_REF2:
                String ref1 = null, ref2 = null;
                if (result.getData() != null) {
                    ref1 = result.getData().toString();
                }
                if (result.getData1() != null) {
                    ref2 = result.getData1().toString();
                }

                transData.setSaleReference1(ref1);
                transData.setSaleReference2(ref2);

                gotoState(State.MAG_ONLINE.toString());
                break;
            case MAG_ONLINE:
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                afterOnlineTrans();
                break;
            case SIGNATURE:
                // save signature data
                byte[] signData = (byte[]) result.getData();
                byte[] signPath = (byte[]) result.getData1();

                if (signData != null && signData.length > 0/* &&
                        signPath != null && signPath.length > 0*/) {
                    transData.setSignData(signData);
                    transData.setSignPath(signPath);
                    // update trans data，save signature
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                }
                gotoState(State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void onCheckCard(ActionResult result) {
        CardInformation cardInfo = (ActionSearchCard.CardInformation) result.getData();
        saveCardInfo(cardInfo, transData);

        if (needFallBack) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }

        currentMode = cardInfo.getSearchMode();
        if (currentMode == ActionSearchCard.SearchMode.SWIPE
             || currentMode == ActionSearchCard.SearchMode.KEYIN) {
            switch (transType) {
                case KBANK_REDEEM_INQUIRY:
                    gotoState(State.MAG_ONLINE.toString());
                    break;
                default:
                    needRemoveCard = false;
                    gotoState(State.ENTER_REDEEMED_DATA.toString());
                    break;
            }
        } else if (currentMode == ActionSearchCard.SearchMode.INSERT) {
            needRemoveCard = true;
            gotoState(State.EMV_READ_CARD.toString());
        } else {
            transEnd(result);
        }
    }


    private void onEmvReadCard(ActionResult result) {
        int ret = result.getRet();
        if(ret == TransResult.ICC_TRY_AGAIN) {
            cntTryAgain++;
            if(cntTryAgain == 3) {
                needFallBack = true;
                searchCardMode &= 0x01;
                showMsgDialog(getCurrentContext(), getString(R.string.prompt_fallback) + getString(R.string.prompt_swipe_card));
            } else {
                showTryAgainDialog(getCurrentContext());
            }
            return;
        } else if (ret == TransResult.NEED_FALL_BACK) {
            needFallBack = true;
            searchCardMode &= 0x01;
            gotoState(SaleTrans.State.CHECK_CARD.toString());
            return;
        } else if (ret != TransResult.SUCC) {
            transEnd(result);
            return;
        }

        switch (transType) {
            case KBANK_REDEEM_INQUIRY:
                gotoState(State.MAG_ONLINE.toString());
                break;
            default:
                gotoState(State.ENTER_REDEEMED_DATA.toString());
                break;
        }
    }

    private void onEnterRedeemedData(ActionResult result) {
        //todo redeemed data
        RedeemKbankInfo redeemKbankInfo = (RedeemKbankInfo)result.getData();
        transData.setProductCode(redeemKbankInfo.getRedeemProductCd());
        transData.setProductQty(redeemKbankInfo.getRedeemQuantity());
        transData.setRedeemedPoint(redeemKbankInfo.getRedeemPoints());
        transData.setRedeemedAmount(redeemKbankInfo.getRedeemAmount());
        if (transType == ETransType.KBANK_REDEEM_DISCOUNT) {
            transData.setRedeemedDiscountType(redeemKbankInfo.getDiscountTypeVal());
        }

        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            gotoState(State.ENTER_REF1_REF2.toString());
        } else {
            gotoState(State.MAG_ONLINE.toString());
        }
    }

    private void afterOnlineTrans() {
        HashMap<ReservedFieldHandle.FieldTables, byte[]> res = ReservedFieldHandle.unpackReservedField(transData.getField63RecByte(), ReservedFieldHandle.redeemed_response, true);
        if (res != null) {
            String sale_amt = new String(res.get(ReservedFieldHandle.FieldTables.SALES_AMT));
            String net_sale_amt = new String(res.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT));
            String redeem_amt = new String(res.get(ReservedFieldHandle.FieldTables.REDEEMED_AMT));
            String redeemPt = new String(res.get(ReservedFieldHandle.FieldTables.REDEEMED_PT));
            String prod_qty = new String(res.get(ReservedFieldHandle.FieldTables.QTY));
            transData.setRedeemedTotal(sale_amt);
            transData.setRedeemedCredit(net_sale_amt);
            transData.setRedeemedAmount(redeem_amt);
            transData.setRedeemedPoint(Integer.parseInt(redeemPt));
            transData.setProductQty(Integer.parseInt(prod_qty));
            if (transType == ETransType.KBANK_REDEEM_PRODUCT && Utils.parseLongSafe(net_sale_amt, 0) > 0) {
                // If field 63 response msg has credit amt, update transType to Product+Credit
                transData.setTransType(ETransType.KBANK_REDEEM_PRODUCT_CREDIT);
            }
            toSignOrPrint();
        } else { // trans. approved but response field 63 is null
            FinancialApplication.getTransDataDbHelper().deleteTransData(transData.getId());
            transEnd(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
        }
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        /*if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            transEnd(new ActionResult(TransResult.SUCC, null));
            return;
        }*/ // May not use in Redemption

        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())
                || !transData.getAcquirer().isSignatureRequired()){
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        }else{
            transData.setSignFree(false);
            boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
            if (eSignature && !transData.isTxnSmallAmt() && transType != ETransType.KBANK_REDEEM_INQUIRY ) {
                gotoState(State.SIGNATURE.toString());
            }else {
                //gotoState(State.SIGNATURE.toString());
                gotoState(State.PRINT.toString()); // Skip SIGNATURE process
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private void showTryAgainDialog(final Context context) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE, 5000);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                //gotoState(State.CHECK_CARD.toString());
            }
        });
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show();
        dialog.setNormalText(getString(R.string.prompt_try_again) + getString(R.string.prompt_insert_card));
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                gotoState(SaleTrans.State.CHECK_CARD.toString());
            }
        });
    }

    private void showMsgDialog(final Context context, final String msg) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                //gotoState(State.CHECK_CARD.toString());
            }
        });
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show();
        dialog.setNormalText(msg);
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                gotoState(SaleTrans.State.CHECK_CARD.toString());
            }
        });
    }

    @Override protected void transEnd(final ActionResult result) {
        searchCardMode = Component.getCardReadMode(transType);
        super.transEnd(result);
    }
}
