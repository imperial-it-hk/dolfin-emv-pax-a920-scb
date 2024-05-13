package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.SaleTrans;
import com.pax.pay.trans.action.ActionClssPreProc;
import com.pax.pay.trans.action.ActionClssReadPanProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionGetT1CMemberID;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSearchCardForPan;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.INSERT;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.KEYIN;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.SWIPE;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.WAVE;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class GetT1CMember extends BaseTrans {


    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    private byte mode = INSERT | SWIPE;
    private boolean isGetThe1CardMemberID = false;

    public GetT1CMember(Context context, TransEndListener transListener) {
        super(context, ETransType.GET_T1C_MEMBER_ID, transListener, true);
        this.isGetThe1CardMemberID = true;
    }

    @Override
    protected void bindStateOnAction() {

        // on check if contactless is enabled, EDC will toggle wave mode for GETT1C also.
        if(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)){
            mode = (byte)(mode | WAVE);
        }

       // search card action
        ActionSearchCard searchCardAction = new ActionSearchCardForPan(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.trans_get_t1c), mode, "000000000001",
                        null, "", transData, 9);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, false);

        // emv process action
        ActionEmvReadCardProcess emvReadCardAction = new ActionEmvReadCardProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvReadCardProcess) action).setParam(getCurrentContext(), emv, transData, true);
            }
        });
        bind(State.EMV_READ_CARD.toString(), emvReadCardAction);

        ActionClssPreProc clssPreProcAction = new ActionClssPreProc(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssPreProc) action).setParam(clss, transData);
            }
        });
        bind(State.CLSS_PREPROC.toString(), clssPreProcAction, true);

        ActionClssReadPanProcess clssReadCardProcessAction = new ActionClssReadPanProcess(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssReadPanProcess) action).setParam(getCurrentContext(), clss, transData);
            }
        });
        bind(State.CLSS_READ_CARD.toString(), clssReadCardProcessAction);

        ActionGetT1CMemberID getT1CMemberIdAction = new ActionGetT1CMemberID(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetT1CMemberID) action).setParam(getCurrentContext(),  transData);
            }
        });
        bind(State.ONLINE_AYCAP_GET_T1C_MEMBER_ID.toString(), getT1CMemberIdAction,false);

        if ( ! isGetThe1CardMemberID) {
            transEnd(new ActionResult(TransResult.ERR_PARAM, null));
            return;
        }

        transData.setAmount("000000000001");

        if ((mode & SearchMode.WAVE) == SearchMode.WAVE) {
            gotoState(State.CLSS_PREPROC.toString());
        } else {
            gotoState(State.CHECK_CARD.toString());
        }
    }

    enum State {
        CHECK_CARD,
        EMV_READ_CARD,
        CLSS_PREPROC,
        CLSS_READ_CARD,
        ONLINE_AYCAP_GET_T1C_MEMBER_ID
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        State state = State.valueOf(currentState);
        int ret = result.getRet();

        switch (state) {
            case EMV_READ_CARD:
            case CLSS_READ_CARD:
                if (transData.getPan() != null){
                    //ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                    //transEnd(new ActionResult(TransResult.SUCC, null));
                    byte[] f55 = EmvTags.getF55(emv, transType, false, transData.getPan());
                    transData.setSendIccData(Utils.bcd2Str(f55));

                    sendInquiryAycapT1C();
                } else {
                    transListener = null;
                    transEnd(new ActionResult(TransResult.ERR_CARD_INVALID, null));
                }
                break;
            case CLSS_PREPROC:
                gotoState(State.CHECK_CARD.toString());
                break;
            case CHECK_CARD:
                if (ret != TransResult.SUCC){
                    transListener = null;
                    transEnd(new ActionResult(TransResult.ERR_CARD_INVALID, null));
                    return;
                }

                CardInformation cardInfo = (CardInformation) result.getData();
                byte currentMode;
                // enter card number manually
                currentMode = cardInfo.getSearchMode();
                saveCardInfo(cardInfo,transData);
                if (currentMode == SearchMode.INSERT) {
                    gotoState(SaleTrans.State.EMV_READ_CARD.toString());
                } else if (currentMode == SearchMode.WAVE) {
                    // AET-15
                    if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)) {
                        gotoState(SaleTrans.State.CLSS_READ_CARD.toString());
                    } else {
                        transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
                        return;
                    }
                } else if (currentMode == SearchMode.SP200) {
                    if (cardInfo.getEmvSP200().getPan() != null) {
                        transData.setPan(cardInfo.getEmvSP200().getPan());
                    }
                    //ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                    //transEnd(new ActionResult(TransResult.SUCC, null));
                } else if (currentMode == SWIPE) {
                    transData.setPan(cardInfo.getPan());
                    //ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                    //transEnd(new ActionResult(TransResult.SUCC, null));
                }
                break;
            case ONLINE_AYCAP_GET_T1C_MEMBER_ID:
                ECRProcReturn(null, new ActionResult(result.getRet(), null));
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    transEnd(new ActionResult(result.getRet(), null));
                }

                break;
            default:
                transEnd(result);
                break;
        }
    }

    @Override protected void transEnd(final ActionResult result) {
        emv.setListener(null);//no memory leak
        clss.setListener(null);//no memory leak
        super.transEnd(result);
    }


    protected void sendInquiryAycapT1C() {
        gotoState(State.ONLINE_AYCAP_GET_T1C_MEMBER_ID.toString());
    }
}
