package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.ped.PedManager;
import com.pax.pay.trans.action.ActionSelectTleAcquirer;
import com.pax.pay.trans.action.ActionUpiTransOnline;
import com.pax.pay.trans.action.ActionRecoverUPILoadRSA;
import com.pax.pay.trans.action.ActionRecoverUPILoadTMK;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.settings.SysParam;

import java.util.ArrayList;

public class LoadUPITrans extends BaseTrans {

    private ArrayList<String> selectAcqs;

    int AcquireIdx = 0;
    int successUP = 0;

    byte[] field62;
    byte[] PKCS = new byte[245];

    public LoadUPITrans(Context context, TransEndListener listener) {
        super(context, ETransType.LOAD_UPI_RSA, listener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionSelectTleAcquirer actionSelecTletAcquirer = new ActionSelectTleAcquirer(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSelectTleAcquirer) action).setParam(getCurrentContext(),
                        getString(R.string.tle_select_acquirer),getString(R.string.trans_upi_load));
            }
        });
        bind(LoadUPITrans.State.SELECT_ACQ.toString(), actionSelecTletAcquirer, true);

        // online action
        ActionUpiTransOnline transOnlineAction = new ActionUpiTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(acq);
                ((ActionUpiTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LoadUPITrans.State.REQUEST_RSA.toString(), transOnlineAction, false);

        ActionRecoverUPILoadRSA recUPILoadRSA = new ActionRecoverUPILoadRSA(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverUPILoadRSA) action).setParam(getCurrentContext(), field62, PKCS);
            }
        });

        bind(LoadUPITrans.State.RECOVER_RSA.toString(), recUPILoadRSA, false);

        // online action
        ActionUpiTransOnline transOnlineAction2 = new ActionUpiTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionUpiTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LoadUPITrans.State.REQUEST_TMK.toString(), transOnlineAction2, false);

        ActionRecoverUPILoadTMK recUPILoadTMK = new ActionRecoverUPILoadTMK(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverUPILoadTMK) action).setParam(getCurrentContext(), field62, PKCS);
            }
        });

        bind(LoadUPITrans.State.RECOVER_TMK.toString(), recUPILoadTMK, false);

        // online action
        ActionUpiTransOnline transOnlineAction3 = new ActionUpiTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionUpiTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LoadUPITrans.State.REQUEST_ACT_TMK.toString(), transOnlineAction3, false);

        gotoState(LoadUPITrans.State.SELECT_ACQ.toString());

    }

    enum State {
        SELECT_ACQ,
        REQUEST_RSA,
        RECOVER_RSA,
        REQUEST_TMK,
        RECOVER_TMK,
        REQUEST_ACT_TMK
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        LoadUPITrans.State state = LoadUPITrans.State.valueOf(currentState);
        switch (state) {
            case SELECT_ACQ:
                AcquireIdx = 0;
                successUP = 0;
                selectAcqs = (ArrayList<String>) result.getData();
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(FinancialApplication.getAcqManager().getCurAcq());
                gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                break;
            case REQUEST_RSA:
                PKCS = Gen_BBL_RSA_PKCS1_15();
                if (result.getRet() == TransResult.SUCC && transData.getBytesField62()!=null && PKCS!=null) {
                    field62 =  transData.getBytesField62();
                    gotoState(LoadUPITrans.State.RECOVER_RSA.toString());
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
                            transEnd(new ActionResult(TransResult.ERR_UPI_LOAD, null));
                        }
                        return;
                    }
                    else {
                        gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                    }
                }
                break;
            case RECOVER_RSA:
                if (result.getRet() == TransResult.SUCC) {
                    transData.setBytesField62((byte[]) result.getData());
                    Component.incStanNo(transData);
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTransType(ETransType.LOAD_UPI_TMK);
                    gotoState(LoadUPITrans.State.REQUEST_TMK.toString());
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
                            transEnd(new ActionResult(TransResult.ERR_UPI_LOAD, null));
                        }
                        return;
                    }
                    else {
                        Component.incStanNo(transData);
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                        transData.setTransType(ETransType.LOAD_UPI_TMK);
                        gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                    }
                }
                break;
            case REQUEST_TMK:
                if (result.getRet() == TransResult.SUCC && transData.getBytesField62() != null) {
                    field62 =  transData.getBytesField62();
                    gotoState(LoadUPITrans.State.RECOVER_TMK.toString());
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
                            transEnd(new ActionResult(TransResult.ERR_UPI_LOAD, null));
                        }
                        return;
                    }
                    else {
                        transData.setTransType(ETransType.LOAD_UPI_TMK);
                        gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                    }
                }
                break;
            case RECOVER_TMK:
                if (result.getRet() == TransResult.SUCC) {
                    Component.incStanNo(transData);
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTransType(ETransType.ACT_UPI_TMK);
                    gotoState(LoadUPITrans.State.REQUEST_ACT_TMK.toString());
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
                            transEnd(new ActionResult(TransResult.ERR_UPI_LOAD, null));
                        }
                        return;
                    }
                    else {
                        Component.incStanNo(transData);
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                        transData.setTransType(ETransType.LOAD_UPI_TMK);
                        gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                    }
                }
                break;
            case REQUEST_ACT_TMK:
                if (result.getRet() == TransResult.SUCC && transData.getResponseCode() != null && transData.getResponseCode().getCode() == "00")
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
                        transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
                    }
                    return;
                }

                Component.incStanNo(transData);
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                transData.setTransType(ETransType.LOAD_UPI_TMK);
                gotoState(LoadUPITrans.State.REQUEST_RSA.toString());
                break;
        }
    }

    private byte[] Gen_BBL_RSA_PKCS1_15() {
        final int BBL_PADDING_STR_LEN = 235;

        byte[] result = new byte[256];

        byte[] ZERO_1st = new byte[]{(byte) 0x00};   // 0x00
        byte[] BlockType = new byte[]{(byte) 0x02};   // 0x02
        byte[] PaddingStr = new byte[BBL_PADDING_STR_LEN];    // Non Zero Padding (Random with no 0)
        byte[] ZERO_2nd = new byte[]{(byte) 0x00};   // 0x00
        byte[] DESKey = new byte[16];
        byte[] CCV_Hi = new byte[1];
        byte[] CCV_Lo = new byte[1];

        int icnt, icnt2;
        byte[] randVal = new byte[8];

        DeviceImplNeptune dev = DeviceImplNeptune.getInstance();

        icnt = 0;
        while (icnt < BBL_PADDING_STR_LEN) {
            dev.getRand(randVal, 8);
            for (icnt2 = 0; icnt2 < 8; icnt2++) {
                if (randVal[icnt2] != 0 && icnt < BBL_PADDING_STR_LEN) {
                    PaddingStr[icnt] = randVal[icnt2];
                    icnt++;
                }
            }
        }

        dev.getRand(randVal, 8);
        System.arraycopy(randVal,0,DESKey,0,8);
        dev.getRand(randVal, 8);
        System.arraycopy(randVal,0,DESKey,8,8);

        //TEST
        //for (icnt = 0; icnt < BBL_PADDING_STR_LEN; icnt ++)
        //{
        //    PaddingStr[icnt] = (byte)0xff;
        //}
        //TEST
        //DESKey = new byte[]{0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, 0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef};

        PedManager ped = FinancialApplication.getPedInstance();
        ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 97, DESKey, ECheckMode.KCV_NONE, null );

        byte[] DESResult = ped.calcDes(PedManager.TRI_ENCRYPT, (byte) 97, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );

        CCV_Hi[0] = DESResult[0];
        CCV_Lo[0] = DESResult[1];

        int index = 0;

        System.arraycopy(ZERO_1st,0, result, index, ZERO_1st.length);
        index+=ZERO_1st.length;
        System.arraycopy(BlockType,0, result, index, BlockType.length);
        index+=BlockType.length;
        System.arraycopy(PaddingStr,0, result, index, PaddingStr.length);
        index+=PaddingStr.length;
        System.arraycopy(ZERO_2nd,0, result, index, ZERO_2nd.length);
        index+=ZERO_2nd.length;
        System.arraycopy(DESKey,0, result, index, DESKey.length);
        index+=DESKey.length;
        System.arraycopy(CCV_Hi,0, result, index, CCV_Hi.length);
        index+=CCV_Hi.length;
        System.arraycopy(CCV_Lo,0, result, index, CCV_Lo.length);

        return result;
    }
}
