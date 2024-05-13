package com.pax.pay.trans;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.RSARecoverInfo;
import com.pax.device.TerminalEncryptionParam;
import com.pax.device.UserParam;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Bank;
import com.pax.pay.constant.Constants;
import com.pax.pay.ped.PedManager;
import com.pax.pay.trans.action.ActionEnterTeId;
import com.pax.pay.trans.action.ActionGenRSA;
import com.pax.pay.trans.action.ActionRecoverKBankLoadTMK;
import com.pax.pay.trans.action.ActionRecoverKBankLoadTWK;
import com.pax.pay.trans.action.ActionSelectTleAcquirer;
import com.pax.pay.trans.action.ActionTleTransOnline;
import com.pax.pay.trans.action.activity.EnterTeIdActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import th.co.bkkps.utils.Log;

public class LoadTMKTrans extends BaseTrans {



    private ArrayList<String> selectAcqs;

    int AcquireIdx = 0;
    int successTLE = 0;

    String field62;
    boolean scbTle = false;
    boolean isAutoDownloadMode=false;
    ProgressDialog progressDialog = null;

    public LoadTMKTrans(Context context, TransEndListener listener) {
        super(context, ETransType.LOADTMK, listener);
        isAutoDownloadMode=false;
    }

    public LoadTMKTrans(Context context, TransEndListener listener, boolean isAutoDownloadMode) {
        super(context, ETransType.LOADTMK, listener);
        super.setSilentMode(isAutoDownloadMode);
        this.isAutoDownloadMode = isAutoDownloadMode;
    }


    @Override
    protected void bindStateOnAction() {

        ActionSelectTleAcquirer actionSelecTletAcquirer = new ActionSelectTleAcquirer(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSelectTleAcquirer) action).setParam(getCurrentContext(),
                        getString(R.string.tle_select_acquirer),getString(R.string.trans_tle_load));
            }
        });
//        ActionSelectTleBankHost actionSelectTleBankHost = new ActionSelectTleBankHost(new AAction.ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionSelectTleBankHost) action).setParam(getCurrentContext(), getString(R.string.tle_select_acquirer), getString(R.string.trans_tle_load));
//            }
//        });
        bind(LoadTMKTrans.State.SELECT_ACQ.toString(), actionSelecTletAcquirer, true);

        ActionEnterTeId enterTeId = new ActionEnterTeId(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterTeId) action).setParam(getCurrentContext(),
                        getString(R.string.tle_te_title), scbTle, isAutoDownloadMode);
            }
        });

        bind(LoadTMKTrans.State.ENTER_TEID.toString(), enterTeId, false);

        ActionGenRSA genRSA = new ActionGenRSA(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionGenRSA) action).setParam(getCurrentContext(),
                        getString(R.string.tle_te_title));
            }
        });

        bind(LoadTMKTrans.State.GEN_RSA.toString(), genRSA, true);

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

        bind(LoadTMKTrans.State.REQUEST_TMK.toString(), transOnlineAction, false);

        ActionRecoverKBankLoadTMK recTMK = new ActionRecoverKBankLoadTMK(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverKBankLoadTMK) action).setParam(getCurrentContext(), field62);
            }
        });

        bind(LoadTMKTrans.State.RECOVER_TMK.toString(), recTMK, false);

        // online action
        ActionTleTransOnline transOnlineAction2 = new ActionTleTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTleTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(LoadTMKTrans.State.REQUEST_TWK.toString(), transOnlineAction2, false);

        ActionRecoverKBankLoadTWK recTWK = new ActionRecoverKBankLoadTWK(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionRecoverKBankLoadTWK) action).setParam(getCurrentContext(), field62);
            }
        });
        bind(LoadTMKTrans.State.RECOVER_TWK.toString(), recTWK, false);

        if (isAutoDownloadMode) {
            // set primaryAcquirerNameList = all acquirer
            doAutoDownloadTLEbySingleHost(Utils.getTLEPrimaryAcquirerList());


//            AcquireIdx = 0;
//            successTLE = 0;
//            SystemClock.sleep(1000);
//            selectAcqs = retrieveTleAcquireList();
//            gotoState(State.ENTER_TEID.toString());
        } else {
            gotoState(LoadTMKTrans.State.SELECT_ACQ.toString());
        }

    }

    enum State {
        SELECT_ACQ,
        ENTER_TEID,
        GEN_RSA,
        REQUEST_TMK,
        RECOVER_TMK,
        REQUEST_TWK,
        RECOVER_TWK,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        LoadTMKTrans.State state = LoadTMKTrans.State.valueOf(currentState);
        switch (state) {
            case SELECT_ACQ:
                AcquireIdx = 0;
                successTLE = 0;
                selectAcqs = (ArrayList<String>) result.getData();
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(AcquireIdx));
                FinancialApplication.getAcqManager().setCurAcq(acq);
                transData.setAcquirer(FinancialApplication.getAcqManager().getCurAcq());

                for (String dummyAcq : selectAcqs) {
                    if (Constants.ACQ_SCB_IPP.equals(dummyAcq)) {
                        scbTle = true;
                    }
                }
                gotoState(LoadTMKTrans.State.ENTER_TEID.toString());
                break;
            case ENTER_TEID:
                // if only SCB
                if(1 == selectAcqs.size() && scbTle){
                    transEnd(result);
                    return;
                }

                if (result.getData() != null && result.getData1() != null) { // if data/data1 is not null, result from scb need to set new stan/trace no.
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                }

                if (result.getRet() == TransResult.SUCC) {
                    gotoState(LoadTMKTrans.State.GEN_RSA.toString());
                }
                break;
            case GEN_RSA:
                if (result.getRet() == TransResult.SUCC) {
                    gotoState(LoadTMKTrans.State.REQUEST_TMK.toString());
                }
                break;
            case REQUEST_TMK:
                if (result.getRet() == TransResult.SUCC && transData.getField62() != null) {
                    field62 =  transData.getField62();
                    gotoState(LoadTMKTrans.State.RECOVER_TMK.toString());
                }
                else if (result.getRet() != TransResult.SUCC && result.getRet() != TransResult.ERR_UNSUPPORTED_TLE) {
                    transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
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
                        gotoState(LoadTMKTrans.State.REQUEST_TMK.toString());
                    }
                }
                break;
            case RECOVER_TMK:
                if (result.getRet() == TransResult.SUCC) {
                    Component.incStanNo(transData);
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTransType(ETransType.LOADTWK);
                    gotoState(LoadTMKTrans.State.REQUEST_TWK.toString());
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
                        Component.incStanNo(transData);
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                        gotoState(LoadTMKTrans.State.REQUEST_TMK.toString());
                    }
                }
                break;
            case REQUEST_TWK:
                if (result.getRet() == TransResult.SUCC) {
                    field62 =  transData.getField62();
                    gotoState(LoadTMKTrans.State.RECOVER_TWK.toString());
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
                        transData.setTransType(ETransType.LOADTMK);
                        gotoState(LoadTMKTrans.State.REQUEST_TMK.toString());
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
                transData.setTransType(ETransType.LOADTMK);
                gotoState(LoadTMKTrans.State.REQUEST_TMK.toString());
                break;
        }
    }

    private ArrayList<Acquirer> SuccessList = new ArrayList<Acquirer>();
    private void doSequentAutoDownload() {
        String Tag = "INIT*";
        Log.d(Tag , "===========================================");
        Log.d(Tag , "          TLE AUTO DOWNLOAD LOG ");
        Log.d(Tag , "===========================================");

        AcquireIdx = 0;
        successTLE = 0;
        SystemClock.sleep(1000);

        selectAcqs = retrieveTleAcquireList();
        Log.d(Tag , " FOUND TLE-ACUIRER = " + selectAcqs.size() );

        SystemClock.sleep(1000);

        int teid_ret = autoGetTeidFromFile();
        Log.d(Tag , " GET TEID FROM FILE RESULT = " + teid_ret );
        Log.d(Tag , "- - - - - - - - - - - - - - - - - - - - - -");

        int rsa__ret = -999;
        int tmk__ret = -999;
        int tmkReret = -999;
        int twk__ret = -999;
        int twkReret = -999;
        String DE62 = null;

        if (teid_ret==TransResult.SUCC) {
            // set TransData
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));

            rsa__ret = -999;
            SystemClock.sleep(1000);
            rsa__ret = generateRSA();
            Log.d(Tag , " GENERATE RSA RESULT = " + rsa__ret );
            if (rsa__ret == TransResult.SUCC) {
                for (int acq_index = 0 ; acq_index < selectAcqs.size() ; acq_index++) {
                    Acquirer curr_acquirer = FinancialApplication.getAcqManager().findAcquirer(selectAcqs.get(acq_index));

                    if(Constants.ACQ_SCB_IPP.equals(curr_acquirer.getName())){
                        continue;
                    }

                    FinancialApplication.getAcqManager().setCurAcq(curr_acquirer);

                    Log.d(Tag , "- - - - - - - - - - - - - - - - - - - - - -");
                    // re-initData
                    tmk__ret = -999 ;
                    tmkReret = -999 ;
                    twk__ret = -999 ;
                    twkReret = -999 ;
                    DE62     =  null;

                    transData.setTransType(ETransType.LOADTMK);
                    transData.setAcquirer(curr_acquirer);
                    SystemClock.sleep(1000);

                    tmk__ret = AutoLoad_TMK_TWK(transData);
                    Log.d(Tag , "\t\tTARGET ACQUIRER INDEX [" + (acq_index+1) + "/" +  selectAcqs.size() + "] : " + curr_acquirer.getName() + " (" + curr_acquirer.getId() + ")");
                    Log.d(Tag , "\t\t\tTMK DOWNLOAD" );

                    if(tmk__ret == TransResult.SUCC) {
                        if (transData.getField62() != null) {
                            DE62 =  transData.getField62();
                            Log.d(Tag , "\t\t\t\tDE62 : " + DE62);
                        }
                        SystemClock.sleep(1000);

                        tmkReret = recoverTMK(curr_acquirer, DE62);
                        Log.d(Tag , "\t\t\t\tRecover TMK Result : " + tmkReret);

                        if(tmkReret == TransResult.SUCC) {
                            Component.incStanNo(transData);
                            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                            transData.setTransType(ETransType.LOADTWK);

                            DE62 = null;
                            SystemClock.sleep(1000);
                            twk__ret = AutoLoad_TMK_TWK(transData);
                            Log.d(Tag , "\t\t\tTWK DOWNLOAD" );

                            if (twk__ret == TransResult.SUCC) {
                                if (transData.getField62() != null) {
                                    DE62 =  transData.getField62();
                                    Log.d(Tag , "\t\t\t\tDE62 : " + DE62);
                                }
                                SystemClock.sleep(1000);

                                twkReret = recoverTWK(curr_acquirer,DE62);
                                Log.d(Tag, "\t\t\t\tRecover TWK Result : " + twkReret);
                                if(twk__ret == TransResult.SUCC) {
                                    SuccessList.add(curr_acquirer);

                                    Component.incStanNo(transData);
                                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                                    transData.setTransType(ETransType.LOADTMK);
                                }
                            } else {
                                Log.d(Tag , "\t\t\t\tDOWNLOAD FAILED -- CODE : " + twk__ret);
                                break;
                            }
                        }
                    }  else {
                        Log.d(Tag , "\t\t\t\tDOWNLOAD FAILED -- CODE : " + tmk__ret);
                        break;
                    }
                }
            }
        }

        if(SuccessList.size() > 0) {
            transEnd(new ActionResult(TransResult.SUCC,null));
        } else {
            transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST,null));
        }
    }

    private ArrayList<Acquirer> getSubHostByTleBankName(String targAcqName, String targetTleBankName) {
        List<Acquirer> acqList = FinancialApplication.getAcqManager().findAllAcquirers();
        ArrayList<Acquirer> expotAcqList = new ArrayList<Acquirer>();
        if (acqList != null && acqList.size() >0) {
            for (Acquirer localAcquire : acqList) {
                if (localAcquire.isEnable()
                        && localAcquire.isEnableTle()
                        && localAcquire.getTleBankName()!=null
                        && localAcquire.getTleBankName().equals(targetTleBankName)
                        && !localAcquire.getName().equals(targAcqName))  {
                    expotAcqList.add(localAcquire);
                }
            }
        } else {
            expotAcqList = new ArrayList<Acquirer>();
        }

        return expotAcqList;
    }

    private Acquirer doLoadTMK(@NonNull Acquirer targetAcquirer) {
        try {
            boolean isHasTMKID = (targetAcquirer.getTMK()!=null);
            if (!isHasTMKID) {
                int result = generateRSA();
                if (result == TransResult.SUCC) {
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                    transData.setTransType(ETransType.LOADTMK);
                    transData.setAcquirer(targetAcquirer);
                    FinancialApplication.getAcqManager().setCurAcq(targetAcquirer);

                    result = AutoLoad_TMK_TWK(transData);
                    if (result == TransResult.SUCC && transData.getField62() != null) {
                        String dataFromDE62 = transData.getField62();
                        result = recoverTMK(targetAcquirer, dataFromDE62);
                        if (result == TransResult.SUCC) {
                            Component.incStanNo(transData);
                            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                            Acquirer tmpTMKAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
                            Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : TMK download success");
                            return tmpTMKAcquirer;
//                            Acquirer tmpTWKAcquirer = doLoadTWK(targetAcquirer);
//                            if (tmpTWKAcquirer != null) {
//                                return tmpTWKAcquirer;
//                            }
                        } else {
                            Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TMK");
                        }
                    } else {
                        Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TMK");
                    }
                } else {
                    Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on generate RSA");
                }
            } else {
                // this host contain existing TMKID
                Log.d(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] has existing TMKID = [" + targetAcquirer.getTMK() + "]");
            }
        } catch (Exception e) {
            Log.e(SplashActivity.TAG, "Error TMK download :: " + e.getMessage());
        }

        return null;
    }

    private Acquirer doLoadTWK(@NonNull Acquirer targetAcquirer, boolean isMainAcquirer) {
        try {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            transData.setTransType(ETransType.LOADTWK);
            transData.setAcquirer(targetAcquirer);
            int result = AutoLoad_TMK_TWK(transData) ;
            if (result == TransResult.SUCC && transData.getField62()!=null) {
                String dataFromDE62 = transData.getField62();

                if (!isMainAcquirer) {
                    // set keyindex before start recoverTWK
                    int currentKeyIndex =FinancialApplication.getAcqManager().findCurrentMaxKeyIndex();
                    result = recoverTWK(targetAcquirer, dataFromDE62, currentKeyIndex);
                } else {
                    result = recoverTWK(targetAcquirer, dataFromDE62);
                }

                if (result==TransResult.SUCC) {
                    Component.incStanNo(transData);
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    targetAcquirer = FinancialApplication.getAcqManager().findAcquirer(targetAcquirer.getName());
                    return targetAcquirer;
                } else {
                    Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on Recover TWK");
                }
            } else {
                Log.e(SplashActivity.TAG, "\t\tAcquirer : [" + targetAcquirer.getName() + "] : Failed on download TWK");
            }
        } catch (Exception e) {
            Log.e(SplashActivity.TAG, "Error TWK download :: " + e.getMessage());
        }
        return null;
    }

    private void doAutoDownloadTLEbySingleHost(ArrayList<String> primaryAcqNameList) {
        Log.d(SplashActivity.TAG, "\t\t\t\t>> TLE download process : start");
        String Tag = "INIT*";
        Log.d(Tag, "===========================================");
        Log.d(Tag, "          TLE AUTO DOWNLOAD LOG ");
        Log.d(Tag, "       --- SINGLE HOST PROCESS --- ");
        Log.d(Tag, "===========================================");

        if (primaryAcqNameList==null) {
            transEnd(new ActionResult(TransResult.ERR_TLE_NOT_LOAD, null));
        }

        try {
             SuccessList.clear();
             if (primaryAcqNameList != null && primaryAcqNameList.size() > 0) {
                 int resultGetTEID = autoGetTeidFromFile();
                 if (resultGetTEID == 0) {
                     for (int hostId = 0 ; hostId <= primaryAcqNameList.size() -1 ; hostId++ ) {
                         // load acquirer information
                         String mainAcquirerName = primaryAcqNameList.get(hostId);
                         Acquirer mainAcquirerInfo = FinancialApplication.getAcqManager().findActiveAcquirer(mainAcquirerName);
                         if (mainAcquirerInfo != null) {
                             // get TLEBankName from main target acquirer
                             String mainTleBankName = mainAcquirerInfo.getTleBankName();
                             // get subhost
                             ArrayList<Acquirer> subHostList = new ArrayList<Acquirer>();
                             if (mainTleBankName!=null) {
                                 subHostList = getSubHostByTleBankName(mainAcquirerName, mainTleBankName);
                             }

                             Acquirer resultTMKAcquirer = doLoadTMK(mainAcquirerInfo);
                             if (resultTMKAcquirer!=null) {
                                 if (subHostList!=null && subHostList.size() > 0) {
                                     for (Acquirer applyTMKAcq : subHostList) {
                                         applyTMKAcq.setKeyId(resultTMKAcquirer.getKeyId());
                                         applyTMKAcq.setTMK(resultTMKAcquirer.getTMK());
                                         applyTMKAcq.setTWK(resultTMKAcquirer.getTWK());
                                         applyTMKAcq.setUP_TMK(resultTMKAcquirer.getUP_TMK());
                                         applyTMKAcq.setUP_TWK(resultTMKAcquirer.getUP_TWK());
                                         FinancialApplication.getAcqManager().updateAcquirer(applyTMKAcq);
                                     }
                                 }

                                 Acquirer resultTWKAcquirer = doLoadTWK(resultTMKAcquirer, true) ;
                                 if (resultTWKAcquirer!=null) {
                                     SuccessList.add(resultTWKAcquirer);
                                     if (subHostList!=null && subHostList.size() > 0) {
                                         for (Acquirer applyTMKAcq : subHostList) {
                                             resultTWKAcquirer = doLoadTWK(applyTMKAcq, false);
                                             if (resultTWKAcquirer !=null) { SuccessList.add(resultTWKAcquirer);}
                                         }
                                     }
                                 }
                             } else {
                                 Log.d(SplashActivity.TAG, "Download TMK/TWK for host [" +mainAcquirerName+ "] was failed");
                             }
                         }
                         else {
                             // skip process this host
                             Log.d(SplashActivity.TAG, "Cannot find acquirer : [" + mainAcquirerName + "] in active acquirer list.");
                         }
                     }
                 }
                 else {
                     // skip process this host
                     Log.d(SplashActivity.TAG, "Cannot get TEID data from file.");
                 }
             }
        } catch (Exception e) {
            Log.d(SplashActivity.TAG, "Error on download TMK+TWK :: " + e.getMessage());
        }

        if (SuccessList.size() > 0 ) {
            transEnd(new ActionResult(TransResult.SUCC, null));
        } else {
            transEnd(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
        }
    }



    private class SingleKeyDownloadTleAcquirerInfo {
        boolean isContainTargetAcquirer;
        String alternateTLEAcquirerName;
        ArrayList<String> TLEAcquirerList;
    }

    private SingleKeyDownloadTleAcquirerInfo retrieveTleAcquireList(String targetAcquirereName) {
        boolean isContainTargetAcquirer = false;
        String altTleHostName = null;
        Acquirer targAcq = FinancialApplication.getAcqManager().findActiveAcquirer(targetAcquirereName);
        String tleBankName = (targAcq != null) ? targAcq.getTleBankName() : null;
        ArrayList<String> enabledTLEHostList = retrieveTleAcquireList();
        ArrayList<String> inScopTleHostName = new ArrayList<>();

//        enabledTLEHostList.remove(targetAcquirereName);
        // check and find which host contained TleBankName same with the TargetAcquirer
        for (String acqName : enabledTLEHostList) {
            Acquirer localAcq = FinancialApplication.getAcqManager().findActiveAcquirer(acqName);
            if (localAcq!=null
                    && localAcq.isEnable()
                    && localAcq.isEnableTle()
                    && localAcq.getTleBankName()!=null
                    && localAcq.getTleBankName().equals(tleBankName)) {
                inScopTleHostName.add(acqName);
            }
        }

        if (inScopTleHostName.size() > 0) {
            if ((inScopTleHostName.indexOf(targetAcquirereName) > -1)) {
                isContainTargetAcquirer = true;
            } else {
                altTleHostName = inScopTleHostName.get(0);
                isContainTargetAcquirer = false;
            }
        }

        SingleKeyDownloadTleAcquirerInfo infos = new SingleKeyDownloadTleAcquirerInfo();
        infos.isContainTargetAcquirer = isContainTargetAcquirer;
        infos.alternateTLEAcquirerName = altTleHostName;
        infos.TLEAcquirerList = enabledTLEHostList;

        return infos;
    }

    private ArrayList<String> getDistinctAllTleBankName() {
        ArrayList<String> tleBankName = new ArrayList<String>();
        List<Acquirer> allAcqList = FinancialApplication.getAcqManager().findAllAcquirers();
        for (Acquirer local_acq : allAcqList) {
            if (local_acq.isEnable() && local_acq.isEnableTle() && !tleBankName.contains(local_acq.getName())) {
                tleBankName.add(local_acq.getName());
            }
        }

        return tleBankName;
    }

    private ArrayList<String> retrieveTleAcquireList() {
        ArrayList<String> acqList = new ArrayList<String>();
        List<Acquirer> allAcqList = FinancialApplication.getAcqManager().findAllAcquirers() ;
        for (Acquirer local_acq : allAcqList) {
            if(local_acq.isEnable() && local_acq.isEnableTle()) {
                if (local_acq.getName().equals(Constants.ACQ_SCB_IPP)) {
                    scbTle = true;
                }
                acqList.add(local_acq.getName());
            }
        }
        return acqList;
    }


    private int autoGetTeidFromFile() {
        // load te from file
        List<EnterTeIdActivity.TE> tleid = null;
        tleid = EnterTeIdActivity.getTeidFromJsonFile();
        EnterTeIdActivity.TE kbankTE = EnterTeIdActivity.getRandomTeId(tleid, Bank.KBANK);
        EnterTeIdActivity.TE gcsTE   = EnterTeIdActivity.getRandomTeId(tleid, Bank.GCS);

        if (kbankTE == null && gcsTE == null) {
            return TransResult.ERR_PARAM;
        }

        // For PackLoadTMK
        FinancialApplication.getUserParam().setTE_ID(kbankTE.TE_ID);
        FinancialApplication.getUserParam().setTE_PIN(kbankTE.TE_PIN);
        // For PackKBankLoadTMK
        FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.KBANK, kbankTE.TE_ID, kbankTE.TE_PIN));
        // For PackBayLoadTMK
        FinancialApplication.getUserParam().putTEParam(new TerminalEncryptionParam(Bank.GCS, gcsTE.TE_ID, gcsTE.TE_PIN));

        String jsonTe = null;

//        if (scbTle && ScbIppService.isSCBInstalled(context)) {
//            if (isAutoDownloadMode) {
//                String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
//                File f = new File(tleFilePath);
//                if (f.exists() && !f.isDirectory()) {
//
//                    ObjectMapper mapper = new ObjectMapper();
//                    try {
//                        JsonNode jsonNode = mapper.readTree(f);
//                        jsonTe = jsonNode.toString();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        return TransResult.ERR_PARAM;
//                    }
//                } else {
//                    return TransResult.SUCC;
//                }
//            }
//            ITransAPI transAPI = TransAPIFactory.createTransAPI();
//            EnterTeIdActivity.doScbTleProcess(context, transAPI, TransContext.getInstance().getCurrentAction(), jsonTe);
//        }
        return TransResult.SUCC;
    }

    public static int generateRSA () {
        boolean flag1, flag2;

        PedManager ped = FinancialApplication.getPedInstance();

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

        KeyPair kp = ped.genKeyPair(2048);
        ped.setKeyPair(kp);
        flag1 = ped.writeRSAKeyPublic(1);
        flag2 = ped.writeRSAKeyPrivate(2);

        if (kp != null || !flag1 || !flag2)
        {
            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();
            byte[] modulus = pubKey.getModulus().toByteArray();
            byte[] modByte = Arrays.copyOfRange(modulus,modulus.length - 256, modulus.length);
            String modStr = Tools.bcd2Str(modByte);
            FinancialApplication.getUserParam().setRSAKey(modStr);
            return  TransResult.SUCC;
        } else {
            return  TransResult.ERR_SEND;
        }
    }


    private static int onlineResult = TransResult.ERR_ABORTED ;
    public static int AutoLoad_TMK_TWK(TransData transData) {
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if (Constants.ACQ_SCB_IPP.equals(acq.getName())){
            return TransResult.SUCC;
        }
        else {
            if (FinancialApplication.getAcqManager().getCurAcq().isEnableTle()) {
                try {
                    Thread onlineThread =  new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int ret = new Transmit().transmit(transData, null);
                            onlineResult = ret;
                        }
                    });
                    onlineThread.start();
                    onlineThread.join(60*1000);
                } catch (Exception ex) {
                    Log.d("INIT*","Thread LOAD TMK TWK error : " + ex.getMessage());
                }

//                FinancialApplication.getApp().runInBackground(new Runnable() {
//                    @Override
//                    public void run() {
//                        //if (transData.getAcquirer() != null && transData.getAcquirer().isEnableTle()) {
//                            int ret = new Transmit().transmit(transData, null);
//                            onlineResult = ret;
//                        //} else {
//                        //    onlineResult = TransResult.ERR_ABORTED;
//                        //}
//                    }
//                });
            } else {
                onlineResult = TransResult.ERR_ABORTED ;
            }
        }

        return onlineResult;
    }

    private static byte[] pTMK_ID ;
    protected static int recoverTMK (Acquirer acquirer, String ex_DE62) {
        EPedType type = EPedType.INTERNAL;
        boolean flag;

        if (!acquirer.isEnableTle()) {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] bytes62 = Tools.str2Bcd(ex_DE62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62,0,4);
        byte[] Version = Arrays.copyOfRange(bytes62,4,6);
        byte[] DownloadType = Arrays.copyOfRange(bytes62,6,7);
        byte[] RespType = Arrays.copyOfRange(bytes62,7,8);
        byte[] AcqId = Arrays.copyOfRange(bytes62,8,11);
        byte[] RSARespDATA = Arrays.copyOfRange(bytes62,11,293);

        // RSA Data
        byte[] RSAKEY = Arrays.copyOfRange(RSARespDATA,0,256);
        byte[] RSAKCV = Arrays.copyOfRange(RSARespDATA,256,262);
        byte[] RSAIDENT = Arrays.copyOfRange(RSARespDATA,262,282);

        byte[] TMK_ID = Arrays.copyOfRange(RSAIDENT,0,4);


        PedManager ped = FinancialApplication.getPedInstance();
        RSARecoverInfo TMKKey = ped.RSADecrypt(2,RSAKEY);

        byte[] keyinfo = TMKKey.getData();
        //GET LAST TMK 16 bytes
        byte[] ltmk = Arrays.copyOfRange(keyinfo, keyinfo.length - 16, keyinfo.length );

        //int acqID = acq.getId();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        int keyId = acquirer.getKeyId() > 0 ? acquirer.getKeyId() : Component.generateKeyId();

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TMK_INDEX + keyId), ltmk, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            return TransResult.ERR_TLE_REQUEST;
        }

        String tmkStr = Tools.bytes2String(TMK_ID);
        acquirer.setKeyId(keyId);
        acquirer.setTMK(tmkStr);
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        pTMK_ID = TMK_ID;
        return TransResult.SUCC;
    }



//    public static int recoverTWK(Acquirer acquirer, String ex_DE62) {
//        EPedType type = EPedType.INTERNAL;
//        boolean flag;
//        int icnt;
//
//        byte[] bytes62 = Tools.str2Bcd(ex_DE62);
//
//        // Field 62 data
//        byte[] TLEIndicator = Arrays.copyOfRange(bytes62, 0, 4);
//        byte[] Version = Arrays.copyOfRange(bytes62, 4, 6);
//        byte[] RespType = Arrays.copyOfRange(bytes62, 6, 7);
//        byte[] TWK_ID = Arrays.copyOfRange(bytes62, 7, 11);
//
//        byte[] TWK_DEK = Arrays.copyOfRange(bytes62, 11, 27);
//        byte[] TWK_MAK = Arrays.copyOfRange(bytes62, 27, 43);
//        byte[] TWK_DEK_KCV = Arrays.copyOfRange(bytes62, 43, 51);
//        byte[] TWK_MAK_KCV = Arrays.copyOfRange(bytes62, 51, 59);
//        byte[] REW_ACQ = Arrays.copyOfRange(bytes62, 59, 62);
//
//        PedManager ped = FinancialApplication.getPedInstance();
//        FinancialApplication.getAcqManager().setCurAcq(acquirer);
//        //int acqID = acq.getId();
//        int keyId = acquirer.getKeyId() > 0 ? acquirer.getKeyId() : Component.generateKeyId();
//
//        byte[] twk_dek = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (byte) (UserParam.TMK_INDEX + keyId), TWK_DEK);
//
//        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_dek, ECheckMode.KCV_NONE, null);
//        if (!flag) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//        byte[] tmp = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
//        byte[] dek_kcv = Tools.bcd2Str(tmp, PedManager.KCV_SIZE).getBytes();
//
//        if (!Arrays.equals(dek_kcv, TWK_DEK_KCV)) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.TWK_DEK_INDEX + keyId), twk_dek, ECheckMode.KCV_NONE, null);
//        if (!flag) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//
//        byte[] twk_mak = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (UserParam.TMK_INDEX + keyId), TWK_MAK);
//
//        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_mak, ECheckMode.KCV_NONE, null);
//        if (!flag) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//        byte[] tmp2 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
//        byte[] mak_kcv = Tools.bcd2Str(tmp2, PedManager.KCV_SIZE).getBytes();
//
//        if (!Arrays.equals(mak_kcv, TWK_MAK_KCV)) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.TWK_MAK_INDEX + keyId), twk_mak, ECheckMode.KCV_NONE, null);
//        if (!flag) {
//            return TransResult.ERR_TLE_REQUEST;
//        }
//
//        if (acquirer.isEnableUpi()) {
//            byte[] TPK_TAG = Arrays.copyOfRange(bytes62, 62, 64);
//            byte[] TPK_TWK = Arrays.copyOfRange(bytes62, 64, 80);
//            byte[] TPK_TWK_KCV = Arrays.copyOfRange(bytes62, 80, 88);
//
//            byte[] tpk_twk = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (UserParam.TMK_INDEX + keyId), TPK_TWK);
//
//            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, tpk_twk, ECheckMode.KCV_NONE, null);
//            if (!flag) {
//                return TransResult.ERR_UPI_LOGON;
//            }
//
//            byte[] tmp3 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
//            byte[] tpk_kcv = Tools.bcd2Str(tmp3, PedManager.KCV_SIZE).getBytes();
//
//            if (!Arrays.equals(tpk_kcv, TPK_TWK_KCV)) {
//                return TransResult.ERR_UPI_LOGON;
//            }
//
//            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.getTPK_TDKID(acq)), tpk_twk, ECheckMode.KCV_NONE, null);
//            if (!flag) {
//                return TransResult.ERR_UPI_LOGON;
//            }
//
//            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TPK, (byte)(UserParam.getTPK_TPKID(acq)), tpk_twk, ECheckMode.KCV_NONE, null);
//            if (!flag) {
//                return TransResult.ERR_UPI_LOGON;
//            }
//        }
//
//        String twkStr = Tools.bytes2String(TWK_ID);
//        acquirer.setTWK(twkStr);
//        acquirer.setKeyId(keyId);
//        FinancialApplication.getAcqManager().updateAcquirer(acquirer);
//
//        pTWK_DEK = TWK_DEK;
//        return TransResult.SUCC;
//    }
    public static int recoverTWK(Acquirer acquirer,String ex_DE62) {
        return recoverTWK(acquirer, ex_DE62, -999);
    }
    public static int recoverTWK(Acquirer acquirer, String ex_DE62, int newKeyID) {
        EPedType type = EPedType.INTERNAL;
        boolean flag;
        int icnt;

        byte[] bytes62 = Tools.str2Bcd(ex_DE62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62,0,4);
        byte[] Version = Arrays.copyOfRange(bytes62,4,6);
        byte[] RespType = Arrays.copyOfRange(bytes62,6,7);
        byte[] TWK_ID = Arrays.copyOfRange(bytes62,7,11);

        byte[] TWK_DEK = Arrays.copyOfRange(bytes62,11,27);
        byte[] TWK_MAK = Arrays.copyOfRange(bytes62,27,43);
        byte[] TWK_DEK_KCV = Arrays.copyOfRange(bytes62,43,51);
        byte[] TWK_MAK_KCV = Arrays.copyOfRange(bytes62,51,59);
        byte[] REW_ACQ = Arrays.copyOfRange(bytes62,59,62);

        PedManager ped = FinancialApplication.getPedInstance();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        //int acqID = acq.getId();
        int keyId = acquirer.getKeyId() > 0 ? acquirer.getKeyId() : Component.generateKeyId();
        int saveKeyId = (newKeyID==-999) ? keyId : newKeyID ;

        byte[] twk_dek  = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (byte)(UserParam.TMK_INDEX + keyId), TWK_DEK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_dek, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] tmp = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] dek_kcv = Tools.bcd2Str(tmp,PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(dek_kcv,TWK_DEK_KCV))
        {
            return TransResult.ERR_TLE_REQUEST;
        }


        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.TWK_DEK_INDEX + saveKeyId), twk_dek, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }


        byte[] twk_mak  = ped.calcDes(PedManager.TRI_DECRYPT, (byte)(UserParam.TMK_INDEX + keyId), TWK_MAK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_mak, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            return TransResult.ERR_TLE_REQUEST;
        }

        byte[] tmp2 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] mak_kcv = Tools.bcd2Str(tmp2,PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(mak_kcv,TWK_MAK_KCV))
        {
            return TransResult.ERR_TLE_REQUEST;
        }


        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.TWK_MAK_INDEX + saveKeyId), twk_mak, ECheckMode.KCV_NONE, null);
        if (!flag) {
            return TransResult.ERR_TLE_REQUEST;
        }

        if (acquirer.isEnableUpi())
        {
            byte[] TPK_TAG = Arrays.copyOfRange(bytes62, 62, 64);
            byte[] TPK_TWK = Arrays.copyOfRange(bytes62, 64, 80);
            byte[] TPK_TWK_KCV = Arrays.copyOfRange(bytes62, 80, 88);

            byte[] tpk_twk = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (UserParam.TMK_INDEX + keyId), TPK_TWK);

            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }

            byte[] tmp3 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV);
            byte[] tpk_kcv = Tools.bcd2Str(tmp3, PedManager.KCV_SIZE).getBytes();

            if (!Arrays.equals(tpk_kcv, TPK_TWK_KCV)) {
                return TransResult.ERR_UPI_LOGON;
            }


            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) (UserParam.TWK_UPI_IDX_BASE + saveKeyId), tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }

            flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TPK, (byte) (UserParam.TPK_UPI_IDX_BASE + saveKeyId), tpk_twk, ECheckMode.KCV_NONE, null);
            if (!flag) {
                return TransResult.ERR_UPI_LOGON;
            }
        }

        String twkStr  = Tools.bytes2String(TWK_ID);
        acquirer.setTWK(twkStr);
        acquirer.setKeyId(keyId);
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        //pTWK_DEK = TWK_DEK;
        return TransResult.SUCC;
    }

}


