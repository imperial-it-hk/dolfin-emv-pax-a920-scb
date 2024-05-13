package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.action.ActionRecoverKBankLoadTWK;
import com.pax.pay.trans.action.ActionSelectTleAcquirer;
import com.pax.pay.trans.action.ActionTleTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.settings.SysParam;

import java.util.ArrayList;

public class LoadTWKTrans extends BaseTrans {

    private ArrayList<String> selectAcqs;

    int AcquireIdx = 0;
    int successTLE = 0;

    String field62;

    public LoadTWKTrans(Context context, TransEndListener listener) {
        super(context, ETransType.LOADTWK, listener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionSelectTleAcquirer actionSelectTleAcquirer = new ActionSelectTleAcquirer(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSelectTleAcquirer) action).setParam(getCurrentContext(),
                        getString(R.string.tle_select_acquirer), getString(R.string.trans_tle_logon));

            }
        });
        bind(LoadTWKTrans.State.SELECT_ACQ.toString(), actionSelectTleAcquirer, true);

        // online action
        ActionTleTransOnline transOnlineAction = new ActionTleTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(acq);
                ((ActionTleTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LoadTWKTrans.State.REQUEST_TWK.toString(), transOnlineAction, false);

        ActionRecoverKBankLoadTWK recTWK = new ActionRecoverKBankLoadTWK(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverKBankLoadTWK) action).setParam(getCurrentContext(), field62);
            }
        });

        bind(LoadTWKTrans.State.RECOVER_TWK.toString(), recTWK, false);

        gotoState(LoadTWKTrans.State.SELECT_ACQ.toString());

    }

    enum State {
        SELECT_ACQ,
        REQUEST_TWK,
        RECOVER_TWK
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        LoadTWKTrans.State state = LoadTWKTrans.State.valueOf(currentState);
        switch (state) {
            case SELECT_ACQ:
                AcquireIdx = 0;
                successTLE = 0;
                selectAcqs = (ArrayList<String>) result.getData();
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(FinancialApplication.getAcqManager().getCurAcq());
                gotoState(LoadTWKTrans.State.REQUEST_TWK.toString());
                break;
            case REQUEST_TWK:
                if (result.getRet() == TransResult.SUCC && transData.getField62() != null) {
                    field62 =  transData.getField62();
                    gotoState(LoadTWKTrans.State.RECOVER_TWK.toString());
                }
                else
                {
                    AcquireIdx++;
                    if (selectAcqs != null && AcquireIdx == selectAcqs.size())
                    {
                        selectAcqs = null;
                        if (successTLE>0) {
                            transEnd(new ActionResult(TransResult.SUCC, null));
                        }
                        else
                        {
                            transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
                        }
                        return;
                    }
                    else {
                        gotoState(LoadTWKTrans.State.REQUEST_TWK.toString());
                    }
                }
                break;
            case RECOVER_TWK:
                if (result.getRet() == TransResult.SUCC)
                {
                    successTLE++;
                }

                AcquireIdx++;
                if (selectAcqs != null && AcquireIdx == selectAcqs.size())
                {
                    selectAcqs = null;
                    if (successTLE>0) {
                        transEnd(new ActionResult(TransResult.SUCC, null));
                    }
                    else
                    {
                        transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
                    }
                    return;
                }

                Component.incStanNo(transData);
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                gotoState(LoadTWKTrans.State.REQUEST_TWK.toString());
                break;
        }
    }
}
