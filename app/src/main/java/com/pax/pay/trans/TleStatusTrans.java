package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadParamService;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.MerchantAcqProfileDb;
import com.pax.pay.trans.action.ActionDispMessage;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.receipt.IReceiptGenerator;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.edc.receiver.process.SettleAlarmProcess;
import th.co.bkkps.utils.Log;

public class TleStatusTrans extends BaseTrans {
    private Context context;
    private String[] statusMsg;
    private boolean isAutoDownloadMode;
    private int LastTLEDownloadStatus = -999;

    public TleStatusTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.TLE_STATUS, transListener);
        this.context = context;
        this.isAutoDownloadMode = false;
        this.LastTLEDownloadStatus = -999;
    }

    public TleStatusTrans(Context context, TransEndListener transListener, boolean isAutoDownloadMode, int LastTLEDownloadStatus) {
        super(context, ETransType.TLE_STATUS, transListener, true); // during use autodownload mode SilentSuccess used.
        this.context = context;
        this.isAutoDownloadMode = isAutoDownloadMode;
        this.LastTLEDownloadStatus = LastTLEDownloadStatus;
    }


    private String loadDefinition(int LastTLEDownloadStatus) {
        /*
           LastTLEDownloadStatus definition
             -999 : initial value (default value)
                0 : None
                1 : Acquirer TLE Not found
                2 : EreaseKey Failed
                3 : TEID File was not found
                4 : Download Failed
                5 : DownloadSuccess
        */
        switch (LastTLEDownloadStatus) {
            case -999:
            case 0:
                return "";
            case 1:
                return " ERR : missing TLE Acqiurer host";
            case 2:
                return " ERR : TLE EREASE KEY FAILED";
            case 3:
                return " ERR : missing TEID.json file";
            case 4:
                return " ERR : TLE DOWNLOAD FAILED";
            case 5:
                return " DOWNLOAD SUCCESS !!";
            default:
                return "";
        }
    }

    @Override
    protected void bindStateOnAction() {

        ActionDispMessage confirmInfoAction = new ActionDispMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                List<String> statusStr = generateStatusForPrint();
                ((ActionDispMessage) action).setParam(context, getString(R.string.trans_tle_status), statusStr);
            }
        });
        bind(TleStatusTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                if (isAutoDownloadMode) {
                    ((ActionPrintTransMessage) action).setParam(context, statusMsg, null, IReceiptGenerator.FONT_SMALL);
                } else {
                    ((ActionPrintTransMessage) action).setParam(context, statusMsg, null);
                }
            }
        });
        bind(TleStatusTrans.State.PRINT.toString(), printReceiptAction, true);

        if (isAutoDownloadMode) {
            List<String> statusStr = generateStatusForPrint();
            context = null;
            gotoState(TleStatusTrans.State.PRINT.toString());
        } else {
            gotoState(TleStatusTrans.State.TRANS_DETAIL.toString());
        }
    }

    enum State {
        TRANS_DETAIL,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        TleStatusTrans.State state = TleStatusTrans.State.valueOf(currentState);

        switch (state) {
            case TRANS_DETAIL:
                gotoState(State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
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

    public List<String> getStatusReportForPrint() {
        return generateStatusForPrint();
    }

    private List<String> generateStatusForPrint() {
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        List<String> list = new ArrayList<String>();
        String lineSplitterDouble = "===========================";

        String status;
        int idx = 0;
        int tle_all_host = 0;
        int tle_succ_host = 0;
        int tle_fail_host = 0;
        if (isAutoDownloadMode) {
            Log.d("INIT*", "PRINT--TLE--STATUS : Automatic mode selected");
            String lastStatusStr = loadDefinition(LastTLEDownloadStatus);
            list.add(lineSplitterDouble);
            list.add("   TLE STATUS : " + lastStatusStr);
            list.add(lineSplitterDouble);
            list.add(((isAutoDownloadMode) ? "> INITIAL TLE DOWNLOAD STATUS" : "         TLE DOWNLOAD STATUS"));
        } else {
            Log.d("INIT*", "PRINT--TLE--STATUS : Manual mode selected");
            list.add(lineSplitterDouble);
            list.add(((isAutoDownloadMode) ? " > INITIAL TLE DOWNLOAD STATUS" : "         TLE DOWNLOAD STATUS"));
        }

        Log.d("INIT*", "PRINT--TLE--STATUS : Acquirer listing");
        String KeyData = null;
        for (Acquirer i : acquirers) {
            if (i.isEnableTle()) {
                tle_all_host++;
                if (i.getTMK() != null && i.getTWK() != null) {
                    tle_succ_host++;
                    status = String.format("  %s. %s = %s", tle_all_host, i.getName(), String.valueOf("TRUE"));
                    KeyData = "TMK-ID = " + i.getTMK() + " | TWK-ID = " + i.getTWK() + " | KEY-ID = " + i.getKeyId();
                } else {
                    tle_fail_host++;
                    status = String.format("  %s. %s = %s", tle_all_host, i.getName(), String.valueOf("FALSE"));
                    KeyData = null;
                }
                list.add(status);
                //if (KeyData!= null) {list.add(KeyData);}
            }
        }
        list.add(lineSplitterDouble);
        list.add("> TOTAL TLE ACTIVED = " + tle_all_host + " HOST" + ((tle_all_host > 1) ? "S" : ""));
        list.add(">   - SUCCESS = " + tle_succ_host + " HOST" + ((tle_succ_host > 1) ? "S" : ""));
        if (tle_fail_host > 0) {
            list.add(" >   - FAILED = " + tle_fail_host + " HOST" + ((tle_fail_host > 1) ? "S" : ""));
        }
        list.add(lineSplitterDouble);
        if (FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME) != null) {
            String linkPoSMercName = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);
            boolean settlementReportPrint = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE);
            boolean auditRmentReportPrint = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE);
            list.add("   ECR/LINKPOS CONFIGURATIONS");
            list.add(lineSplitterDouble);
            if (!(linkPoSMercName.equals("DISABLE"))) {
                list.add("> Merchant : " + FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME));
                list.add("> Protocol : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL));
                list.add("- - - - - - - - - - - - - - - - - - - - - - - - - - -");
                list.add("> REPORT PRINT OPTIONS:-");
                list.add("  1.SETTLE REPORT : " + (settlementReportPrint ? "ENABLED" : "DISABLED"));
                list.add("  2.AUDIT REPORT : " + (auditRmentReportPrint ? "ENABLED" : "DISABLED"));
            } else {
                list.add(" STATUS : " + FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME) + "");
            }
            list.add(lineSplitterDouble);
        }

//        list.add(" COMMUNICATION");
//        list.add("\t ACTIVE COMM TYPE : " + FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE));
//        list.add("\t - - - - - - - - - - - - - - - - - - - - - - - - -");
//        if (FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE).equals(SysParam.Constant.CommType.MOBILE.toString())) {
//            list.add("\t MOBILE");
//            list.add("\t\t> APN NAME : " + FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN));
//            list.add("\t\t> APN SYSTEM : " + FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM));
//            list.add("\t\t> APN SYSTEM : " + FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM));
//        } else if (FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE).equals(SysParam.Constant.CommType.LAN.toString())
//                    || FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE).equals(SysParam.Constant.CommType.WIFI.toString())) {
//            list.add("\t LAN / WiFi");
//            list.add("\t\t> MODE : " + (FinancialApplication.getSysParam().get(SysParam.BooleanParam.LAN_DHCP, false)? "DHCP" : "STATIC"));
//            list.add("\t\t> IPADDR : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LAN_LOCAL_IP));
//            list.add("\t\t> SUBNET : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LAN_NETMASK));
//            list.add("\t\t> GATEWAY : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LAN_GATEWAY));
//            list.add("\t\t> DNS1 : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LAN_DNS1));
//            list.add("\t\t> DNS2 : " + FinancialApplication.getSysParam().get(SysParam.StringParam.LAN_DNS2));
//        }
//        list.add(lineSplitterDouble);
//
//
//        list.add(" SETTLEMENT MODE :  " + FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_SETTLEMENT_MODE, SettleAlarmProcess.SettlementMode.DISABLE.getValue().toString()));
//        List<Acquirer> acqs = FinancialApplication.getAcqManager().findAllAcquirers();
//        for (Acquirer acq : acqs) {
//            list.add("\t" + (acqs.indexOf(acq)+1) + ". " + acq.getName() + " : " + ((acq.isEnable() ? " on " +  acq.getSettleTime() : " (disabled)")));
//        }
//        list.add(lineSplitterDouble);


//        boolean multiMercEnable = MerchantProfileManager.INSTANCE.isMultiMerchantEnable();
//        list.add(" MULTI-MERCHANT ENABLE = " + multiMercEnable);
//        if (multiMercEnable) {
//            List<MerchantProfile> mProfs = MerchantProfileManager.INSTANCE.getAllMerchant();
//            list.add(" ACTIVE PROFILE COUNT : " + mProfs.size());
//            if (mProfs!=null || !mProfs.isEmpty()) {
//                for (MerchantProfile mProf : mProfs) {
//
//                    list.add(" MERCHANT NO. " + (mProfs.indexOf(mProf)+1));
//                    list.add(" "  + mProf.getMerchantName() + "'");
//                    list.add(" "  + mProf.getMerchantAddress() + "'");
//                    list.add(" "  + mProf.getMerchantAddress1() + "'");
//
//                    List<MerchantAcqProfile> mAcqProfs = MerchantAcqProfileDb.INSTANCE.findAcqFromMerchant(mProf.getMerchantName(), null);
//                    if (mAcqProfs==null || mAcqProfs.isEmpty()) { continue; }
//                    for (MerchantAcqProfile mAcqProf : mAcqProfs) {
//                        for (String acqName : MerchantProfileManager.INSTANCE.getSupportAcq()) {
//                            boolean enableStatus = false;
//                            if (acqName.equals(mAcqProf.getAcqHostName())){ enableStatus=true; }
//                            list.add(" "  + (MerchantProfileManager.INSTANCE.getSupportAcq().indexOf(acqName)+1)+ ". " + acqName + ((enableStatus) ? "" : " (disabled)")  );
//                            list.add("    TID : "  + mAcqProf.getTerminalId());
//                            list.add("    MID : "  + mAcqProf.getMerchantId());
//                            list.add("    BATCH NO. : "  + mAcqProf.getCurrBatchNo());
//                            list.add(" - - - - - - - - - - - - - - - - - - - - - -");
//                        }
//                    }
//                }
//            }
//        }




        statusMsg = new String[list.size()+4];
        for (String str : list) {
            statusMsg[idx++] = str;
        }

        Log.d("INIT*", "PRINT--TLE--STATUS : summarized");

        if (list.size() == 1) {//Show error msg if no acquirer enable TLE
            list.add(getString(R.string.err_no_support_tle));
            statusMsg[idx++] = getString(R.string.err_no_support_tle);
        }
        else {
            Acquirer acquirer = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
            if (acquirer != null && acquirer.isEnableTle()) {
                list.add(((isAutoDownloadMode) ? " > INITIAL TPK DOWNLOAD STATUS": " > TPK DOWNLOAD STATUS"));
                if (acquirer.getUP_TMK()!=null && acquirer.getUP_TWK()!=null) {
                    status = String.format("  %s. %s = %s", 1, acquirer.getName(), "TRUE");
                } else {
                    status = String.format("  %s. %s = %s", 1, acquirer.getName(), "FALSE");
                }
                list.add(status);
                list.add("==================================");
                statusMsg[idx++] = list.get(list.size() - 3);
                statusMsg[idx++] = list.get(list.size() - 2);
                statusMsg[idx] = list.get(list.size() - 1);
            }
        }

        return list;
    }

    private String castBoolean(Boolean val) {
        return (val) ? "ENABLED" : "DISABLED";
    }
}
