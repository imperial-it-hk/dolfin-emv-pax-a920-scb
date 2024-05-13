package com.pax.pay.trans;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.SystemClock;
import android.widget.ProgressBar;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.receipt.IReceiptGenerator;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

public class EReceiptStatusTrans extends BaseTrans {
    private Context context = null;
    private ercmInitialResult previousErcmInitialStatus;

    public enum ercmInitialResult {NONE, CLEAR_SESSIONKEY_ERROR, INIT_FAILED, INIT_SUCCESS}

    public EReceiptStatusTrans(Context context, ETransType transType, TransEndListener transListener, ercmInitialResult previousErcmInitialStatus) {
        super(context, transType, transListener, true);
        this.context = context;
        this.previousErcmInitialStatus = previousErcmInitialStatus;
    }

    public EReceiptStatusTrans(Context context, ETransType transType, TransEndListener transListener, int ercmStatus) {
        super(context, transType, transListener, true);
        this.context = context;
        ercmInitialResult tmpResult = ercmInitialResult.NONE;
        if (ercmStatus==3) {
            tmpResult = ercmInitialResult.INIT_SUCCESS;
        } else if (ercmStatus==2) {
            tmpResult = ercmInitialResult.INIT_FAILED;
        } else if (ercmStatus==1) {
            tmpResult = ercmInitialResult.CLEAR_SESSIONKEY_ERROR;
        }
        this.previousErcmInitialStatus = tmpResult ;
    }


    @Override
    public void onActionResult(String currentState, ActionResult result) {
        transEnd(new ActionResult(TransResult.SUCC, null));
    }

    private String[] ERCM_DownloadMsg = null;

    private enum State {
        PRINT
    }

    @Override
    protected void bindStateOnAction() {
        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionPrintTransMessage) action).setParam(context, ERCM_DownloadMsg, null, IReceiptGenerator.FONT_SMALL);
            }
        });
        bind(State.PRINT.toString(), printReceiptAction, true);


        ERCM_DownloadMsg = GenerateERCMStatus();
        gotoState(State.PRINT.toString());
    }

    private enum DateTimeType {DATE, TIME}

    private String getDateTime(String exDateTime, DateTimeType type) {
        if (exDateTime == null) {
            return nullCast(exDateTime);
        }
        if (type == DateTimeType.DATE) {
            return String.format("%s/%s/%s",
                    exDateTime.substring(6, 8),
                    exDateTime.substring(4, 6),
                    exDateTime.substring(0, 4));
        } else {
            return String.format("%s:%s:%s",
                    exDateTime.substring(8, 10),
                    exDateTime.substring(10, 12),
                    exDateTime.substring(12, 14));
        }

    }

    private String getSlipNumbDesc(int slipNumb) {
        switch (slipNumb) {
            case 0:
                return "No printing required";
            case 1:
                return "MERC Copy only";
            case 2:
                return "MERC + CUST Copy";
            case 3:
                return "CUST Copy only";
            default:
                return "";
        }
    }

    private String[] GenerateFailedPrint() {
        List<String> printList = new ArrayList<String>();
        printList.add("==================================");
        printList.add("              ERCM INITIAL REPORT ");
        printList.add("==================================");
        printList.add(" ERCM STATUS : " + ((previousErcmInitialStatus == ercmInitialResult.INIT_FAILED) ? "INITIALIZE FAILED" : "CLEAR SESSION KEY FAILED"));
        printList.add("==================================");

        String[] local_ercmStatus = new String[printList.size()];
        int index = 0;
        for (String str : printList) {
            local_ercmStatus[index++] = str;
        }

        return local_ercmStatus;
    }

    private String nullCast(String exString) {
        if (exString == null) {
            return " ** missing ** ";
        } else {
            return exString;
        }
    }

    ProgressDialog progressDialog = null;

    private String[] GenerateERCMStatus() {

        // During Fail on download
        //if (previousErcmInitialStatus != ercmInitialResult.INIT_SUCCESS) { return GenerateFailedPrint();}

        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findAllAcquirers();
        List<String> HeaderList = new ArrayList<String>();
        List<String> printList = new ArrayList<String>();
        List<String> FooterList = new ArrayList<String>();
        int hostnumber = 0;
        int countRegister = 0;
        int countUnregister = 0;
        int countDisable = 0;
        int countUnsupported = 0;

        List<String> initiated_host = new ArrayList<String>();
        List<String> uninitiated_host = new ArrayList<String>();
        List<String> disabled_host = new ArrayList<String>();
        List<String> unsupported_host = new ArrayList<String>();

        HeaderList.add("==================================");
        HeaderList.add("          ERCM INITIAL REPORT ");
        HeaderList.add("==================================");
        HeaderList.add(" ERCM CONFIG");
        HeaderList.add("  > EDC S/N  : " + nullCast(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER)));
        HeaderList.add("  > ERCM FLAG ENABLED   : " + FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE));
        HeaderList.add("  > E-SIGNATURE ENABLED : " + FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE));
        HeaderList.add("  > BANK  : " + nullCast(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE)));
        HeaderList.add("  > MERC  : " + nullCast(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE)));
        HeaderList.add("  > STORE : " + nullCast(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE)));
        HeaderList.add("  > KEY VER. : " + nullCast(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION)));
        HeaderList.add("- - - - - - - - - - - - - - - - - - - - - - - - - - -");
        HeaderList.add(" PRINTING CONFIG");
        HeaderList.add("  > PRINT AFTER TRANS : " + (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN) ? "ON" : "OFF"));
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN)) {
            HeaderList.add("      1. SUCCESS = " + getSlipNumbDesc(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP)));
            HeaderList.add("      2. FAILED = " + getSlipNumbDesc(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD)));
        }
        HeaderList.add("  > NEXT TRANS UPLOAD : " + (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD) ? "ON" : "OFF"));
        HeaderList.add("  > PRINT ON PRESETTLE FAILED");
        HeaderList.add("       = " + (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS) ? "FORCE PRINT ALL RECORDS" : "NEVER PRINT RECORDS ONLY"));
        HeaderList.add("-----------------------------------------");

        initiated_host.add("1 REGISTERED HOST");

        //uninitiated_host.add("----------------------------------");
        uninitiated_host.add("2 PENDING-REGISTER HOST");

        //disabled_host.add(   "----------------------------------");
        disabled_host.add("3 DISABLED UPLOAD HOST");

        //unsupported_host.add("----------------------------------");
        unsupported_host.add("4 UNSUPPORTED ERCM HOST");


        if (acquirerList.size() > 0) {
            for (Acquirer acquirer : acquirerList) {
                if (acquirer.getName().equals(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
                        || acquirer.getName().equals(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)) {
                    // Skip on ERCM_KMS // ERCM_RMS
                    break;
                }

                EReceiptLogoMapping result;
                if (acquirer.isEnable()) {
                    hostnumber += 1;
                    if (acquirer.getEnableUploadERM()) {
                        try {
                            result = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerIndex(String.valueOf(acquirer.getId()));
                        } catch (Exception e) {
                            result = null;
                        }
                        if (result != null) {
                            // Able to intialize
                            countRegister += 1;
                            printList.add(String.format(" %s. %s = %s", hostnumber, acquirer.getName(), "true"));
                            initiated_host.add(String.format("   1.%s %s (NII=%s)", countRegister, acquirer.getName(), acquirer.getNii()));
                        } else {
                            // Unable to initalize ERCM
                            countUnregister += 1;
                            printList.add(String.format(" %s. %s = %s", hostnumber, acquirer.getName(), "false"));
                            uninitiated_host.add(String.format("   2.%s %s (NII=%s)", countUnregister, acquirer.getName(), acquirer.getNii()));
                        }
                    } else {
                        // Disabled upload Acquirer
                        countDisable += 1;
                        printList.add(String.format(" %s. %s = %s", hostnumber, acquirer.getName(), "disabled upload"));
                        disabled_host.add(String.format("   3.%s %s (NII=%s)", countDisable, acquirer.getName(), acquirer.getNii()));
                    }
                } else {
                    countUnsupported += 1;
                    printList.add(String.format(" %s. %s = %s", hostnumber, acquirer.getName(), "unsupported"));
                    unsupported_host.add(String.format("   4.%s %s (NII=%s)", countUnsupported, acquirer.getName(), acquirer.getNii()));
                }
            }
        }

        if (countRegister == 0) {
            initiated_host.add("         ** No host record **\n");
        }
        if (countUnregister == 0) {
            uninitiated_host.add("         ** No host record **\n");
        }
        if (countDisable == 0) {
            disabled_host.add("         ** No host record **\n");
        }
        if (countUnsupported == 0) {
            unsupported_host.add("         ** No host record **\n");
        }

        String StatusStr = null;
        if (previousErcmInitialStatus == ercmInitialResult.INIT_SUCCESS) {
            StatusStr = ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED) == null) ? "NOT READY" : "READY");
        } else if (previousErcmInitialStatus == ercmInitialResult.INIT_FAILED) {
            StatusStr = "INITIALIZE FAILED";
        } else if (previousErcmInitialStatus == ercmInitialResult.CLEAR_SESSIONKEY_ERROR) {
            StatusStr = "CLEAR SESSION KEY FAILED";
        }

        FooterList.add("==================================");
        FooterList.add(" ERCM STATUS : " + StatusStr);
        FooterList.add("                DATE : " + getDateTime(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED), DateTimeType.DATE));
        FooterList.add("                TIME : " + getDateTime(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED), DateTimeType.TIME));
        FooterList.add("    - - - - - - - - - - - - - - - - - - - - - - - -");
        FooterList.add("    INITIATED = " + countRegister + " HOST" + ((countRegister > 1) ? "S" : ""));
        if (countUnregister > 0) {
            FooterList.add("    PENDING INITAL = " + countUnregister + " HOST" + ((countUnregister > 1) ? "S" : ""));
        }
        if (countDisable > 0) {
            FooterList.add("    DISABLED UPLOAD = " + countDisable + " HOST" + ((countDisable > 1) ? "S" : ""));
        }
        //FooterList.add("    UNSUPPORTED ERCM = " + countUnsupported + " HOST" + ((countUnsupported >1) ? "S" :""));
        FooterList.add("==================================");
        FooterList.add(" TOTAL =" + hostnumber + " HOST" + ((hostnumber > 1) ? "S" : ""));
        FooterList.add("==================================");

        String[] local_ercmStatus = null;
        if (hostnumber > 0) {
            //local_ercmStatus = new String[HeaderList.size() + printList.size() + FooterList.size() +1];
            local_ercmStatus = new String[HeaderList.size() + printList.size() + FooterList.size() + 1 + initiated_host.size() + uninitiated_host.size() + disabled_host.size() + unsupported_host.size()];
            int index = 0;
            for (String str : HeaderList) {
                local_ercmStatus[index++] = str;
            }
            //for (String str: printList) { local_ercmStatus[index++] = str; }

            for (String str : initiated_host) {
                local_ercmStatus[index++] = str;
            }
            for (String str : uninitiated_host) {
                local_ercmStatus[index++] = str;
            }
            for (String str : disabled_host) {
                local_ercmStatus[index++] = str;
            }
            //for (String str: unsupported_host) { local_ercmStatus[index++] = str; }

            for (String str : FooterList) {
                local_ercmStatus[index++] = str;
            }
        } else {
            local_ercmStatus = new String[]{};
        }

        return local_ercmStatus;
    }
}
