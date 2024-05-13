package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.action.ActionSelectTleAcquirer;
import com.pax.pay.trans.action.ActionUpiTransOnline;
import com.pax.pay.trans.action.ActionRecoverUPILoadTWK;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.settings.SysParam;

import java.util.ArrayList;

public class LogonUPITrans extends BaseTrans {

    private ArrayList<String> selectAcqs;

    int AcquireIdx = 0;
    int successUP = 0;

    private Acquirer acq = null;

    byte[] field62;

    public LogonUPITrans(Context context, TransEndListener listener) {
        super(context, ETransType.LOAD_UPI_TWK, listener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionSelectTleAcquirer actionSelectTleAcquirer = new ActionSelectTleAcquirer(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSelectTleAcquirer) action).setParam(getCurrentContext(),
                        getString(R.string.tle_select_acquirer),getString(R.string.trans_upi_logon));
            }
        });
        bind(LogonUPITrans.State.SELECT_ACQ.toString(), actionSelectTleAcquirer, true);

        // online action
        ActionUpiTransOnline transOnlineAction = new ActionUpiTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(acq);
                ((ActionUpiTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LogonUPITrans.State.REQUEST_TWK.toString(), transOnlineAction, false);

        ActionRecoverUPILoadTWK recTWK = new ActionRecoverUPILoadTWK(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverUPILoadTWK) action).setParam(getCurrentContext(), field62);
            }
        });

        bind(LogonUPITrans.State.RECOVER_TWK.toString(), recTWK, false);

        gotoState(LogonUPITrans.State.SELECT_ACQ.toString());

    }

    enum State {
        SELECT_ACQ,
        REQUEST_TWK,
        RECOVER_TWK
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        LogonUPITrans.State state = LogonUPITrans.State.valueOf(currentState);
        switch (state) {
            case SELECT_ACQ:
                AcquireIdx = 0;
                successUP = 0;
                selectAcqs = (ArrayList<String>) result.getData();
                acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(FinancialApplication.getAcqManager().getCurAcq());
                gotoState(LogonUPITrans.State.REQUEST_TWK.toString());
                break;
            case REQUEST_TWK:
                if (result.getRet() == TransResult.SUCC && transData.getBytesField62() != null) {
                    field62 =  transData.getBytesField62();
                    gotoState(LogonUPITrans.State.RECOVER_TWK.toString());
                }
                else
                {
                    AcquireIdx++;
                    if (AcquireIdx == selectAcqs.size())
                    {
                        selectAcqs = null;
                        if (successUP>0) {
                            transEnd(new ActionResult(TransResult.SUCC, null));
                        }
                        else
                        {
                            acq.setUP_TWK(null);
                            FinancialApplication.getAcqManager().updateAcquirer(acq);
                            transEnd(new ActionResult(TransResult.ERR_UPI_LOGON, null));
                        }
                        return;
                    }
                    else {
                        gotoState(LogonUPITrans.State.REQUEST_TWK.toString());
                    }
                }
                break;
            case RECOVER_TWK:
                if (result.getRet() == TransResult.SUCC)
                {
                    successUP++;
                }

                AcquireIdx++;
                if (AcquireIdx == selectAcqs.size())
                {
                    selectAcqs = null;
                    if (successUP>0) {
                        transEnd(new ActionResult(TransResult.SUCC, null));
                    }
                    else
                    {
                        acq.setUP_TWK(null);
                        FinancialApplication.getAcqManager().updateAcquirer(acq);
                        transEnd(new ActionResult(TransResult.ERR_UPI_LOGON, null));
                    }
                    return;
                }

                Component.incStanNo(transData);
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                gotoState(LogonUPITrans.State.REQUEST_TWK.toString());
                break;
        }
    }
}
