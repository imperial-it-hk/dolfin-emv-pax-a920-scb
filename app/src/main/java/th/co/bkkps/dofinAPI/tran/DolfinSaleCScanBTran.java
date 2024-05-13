package th.co.bkkps.dofinAPI.tran;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.model.ETransType;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.dofinAPI.tran.action.ActionDolfinGetConfig;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSaleCScanB;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSetConfig;

public class DolfinSaleCScanBTran  extends BaseTrans {
    String mAmount = null;
    boolean isPrintQR = false;
    String respMsg = null;
    boolean isSaleErr = false;

    public DolfinSaleCScanBTran(Context context, TransEndListener transListener) {
        super(context, ETransType.DOLFIN_SALE, transListener);
        setBackToMain(true);
    }

    public DolfinSaleCScanBTran(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.DOLFIN_SALE, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }

    @Override
    protected void bindStateOnAction() {

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_sale), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionDolfinSaleCScanB actionDolfinSaleCScanB = new ActionDolfinSaleCScanB(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinSaleCScanB) action).setParam(getCurrentContext(), transData.getAmount(),transData,isPrintQR);
            }
        });
        bind(State.SALE_C_SCAN_B.toString(), actionDolfinSaleCScanB, true);

        ActionDolfinSetConfig configAction = new ActionDolfinSetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinSetConfig) action).setParam(getCurrentContext());
            }
        });
        bind(State.SENT_CONFIG.toString(), configAction, true);



        ActionDolfinGetConfig getConfig = new ActionDolfinGetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinGetConfig) action).setParam(getCurrentContext(),transData,isSaleErr);
            }
        });
        bind(State.GET_CONFIG.toString(), getConfig, true);

        if (mAmount == null) {
            isPrintQR = true;
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            isPrintQR = false;
            transData.setAmount(mAmount);
            setTransType(transData.getTransType());
            gotoState(State.SENT_CONFIG.toString());
        }
    }

    enum State{
        ENTER_AMOUNT,
        SENT_CONFIG,
        SALE_C_SCAN_B,
        GET_CONFIG
    }

    @Override
    public void onActionResult(String currentState, ActionResult result){
        int rc = result.getRet();
        State state = State.valueOf(currentState);

        switch (state) {
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                gotoState(State.SENT_CONFIG.toString());
                //gotoState(State.SALE.toString());
                break;
            case SENT_CONFIG:
                if((int)result.getData() != 0){
                    ECRProcReturn(null, new ActionResult((int)result.getData(), null));
                    respMsg = (String)result.getData1() != null ? (String)result.getData1() : null;
                    showRespMsg(result,respMsg);
                    return;
                }

                gotoState(State.SALE_C_SCAN_B.toString());
                break;
            case SALE_C_SCAN_B:
                int ret = (int)result.getData();
                ECRProcReturn(null, new ActionResult(ret, null));
                if(ret != 0){
                    isSaleErr = true;
                    respMsg = (String)result.getData1() != null ? (String)result.getData1() : null;
                    //showRespMsg(result,(String)result.getData1());
                }
                gotoState(State.GET_CONFIG.toString());
                //transEnd(result);
                break;
            case GET_CONFIG:
                if(isSaleErr){
                    showRespMsg(result,respMsg);
                    return;
                }
                transEnd(result);
                break;
            default:
                transEnd(result);
        }
    }

    private void showRespMsg(ActionResult result, String respMsg){
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        };
        DialogUtils.showErrMessage(getCurrentContext(), getString(R.string.trans_sale), respMsg, onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);

    }
}