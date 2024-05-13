/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.record;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.ConditionVariable;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.ETransStatus;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.trans.receipt.PrintListener;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintAuditReport;
import com.pax.pay.trans.receipt.ReceiptPrintAutoSettlementOnExecution;
import com.pax.pay.trans.receipt.ReceiptPrintBitmap;
import com.pax.pay.trans.receipt.ReceiptPrintDetailReport;
import com.pax.pay.trans.receipt.ReceiptPrintEReceiptDetailReport;
import com.pax.pay.trans.receipt.ReceiptPrintEReceiptUploadStatus;
import com.pax.pay.trans.receipt.ReceiptPrintFailedTransDetail;
import com.pax.pay.trans.receipt.ReceiptPrintInstallmentBAYTrans;
import com.pax.pay.trans.receipt.ReceiptPrintInstalmentAmexTrans;
import com.pax.pay.trans.receipt.ReceiptPrintInstalmentKbankTrans;
import com.pax.pay.trans.receipt.ReceiptPrintPreAuthDetailReport;
import com.pax.pay.trans.receipt.ReceiptPrintQrSlip;
import com.pax.pay.trans.receipt.ReceiptPrintRedeemedTrans;
import com.pax.pay.trans.receipt.ReceiptPrintSettle;
import com.pax.pay.trans.receipt.ReceiptPrintSummary;
import com.pax.pay.trans.receipt.ReceiptPrintTotal;
import com.pax.pay.trans.receipt.ReceiptPrintTotalDetail;
import com.pax.pay.trans.receipt.ReceiptPrintTrans;
import com.pax.pay.trans.receipt.ReceiptPrintTransDetail;
import com.pax.pay.trans.receipt.ReceiptPrintTransForERM;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import th.co.bkkps.amexapi.action.ActionAmexReport;
import th.co.bkkps.amexapi.action.ActionAmexReprint;
import th.co.bkkps.amexapi.action.activity.AmexReportActivity;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintAuditReport;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintLastSettle;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintSummary;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintTran;
import th.co.bkkps.kcheckidAPI.trans.action.ActionGetKCheckIDRecordCount;
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDPrintReport;
import th.co.bkkps.scbapi.trans.action.ActionScbIppReport;
import th.co.bkkps.scbapi.trans.action.ActionScbIppReprint;
import th.co.bkkps.scbapi.trans.action.activity.ScbIppNoDisplayActivity;

public class Printer {

    private Printer() {

    }

    /**
     * print the last transaction
     *
     * @param activity context
     */
    public static int printLastTrans(final Activity activity) {
        TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransDataHistory();

        if (transData == null || !transData.getAcquirer().isEnable() ) {
            return TransResult.ERR_NO_TRANS;
        }

        if (ETransType.QR_VOID_WALLET == transData.getTransType() || ETransType.QR_VOID == transData.getTransType()) {
            transData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(transData.getOrigTransNo(), false);
        }

        if (transData.getAcquirer() != null) {
            switch (transData.getAcquirer().getName()) {
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    new ReceiptPrintRedeemedTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    new ReceiptPrintInstalmentKbankTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_AMEX_EPP:
                    new ReceiptPrintInstalmentAmexTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_DOLFIN:
                    final TransData finalTransData = transData;
                    ActionDolfinPrintTran actionDolfinPrintTran = new ActionDolfinPrintTran(new AAction.ActionStartListener() {
                        @Override
                        public void onStart(AAction action) {
                            ((ActionDolfinPrintTran) action).setParam(activity, String.valueOf(finalTransData.getTraceNo()));
                        }
                    });
                    actionDolfinPrintTran.execute();
                    break;
                case Constants.ACQ_SCB_IPP:
                case Constants.ACQ_SCB_REDEEM:
                    final TransData scbTrans = transData;
                    ActionScbIppReprint actionScbIppReprint = new ActionScbIppReprint(
                            action -> ((ActionScbIppReprint) action).setParam(activity, scbTrans.getTraceNo())
                    );
                    actionScbIppReprint.execute();
                    break;
                case Constants.ACQ_AMEX:
                    final TransData amexTrans = transData;
                    ActionAmexReprint actionAmexReprint = new ActionAmexReprint(
                            action -> ((ActionAmexReprint) action).setParam(activity, amexTrans.getTraceNo(), amexTrans.getTraceNo(), -1)
                    );
                    actionAmexReprint.execute();
                    break;
                default:
                    new ReceiptPrintTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
            }
        }
        return TransResult.SUCC;
    }

    /**
     * print transaction detail
     *
     * @param activity context
     */
    //AET-112
    public static int printTransDetail(final String title, final Activity activity, final Acquirer acquirer) {
        List<TransData> record = getTransAuditReport(acquirer);

        if (record.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        return new ReceiptPrintTransDetail().print(title, record, acquirer, new PrintListenerImpl(activity));
    }

    /**
     * print total
     *
     * @param activity context
     */
    public static int printTransTotal(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory();
        if (allTrans.isEmpty() && FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX) == null) {
            return TransResult.ERR_NO_TRANS;
        }
        TransTotal total = null;
        ActionDolfinPrintSummary actionDolfinPrintSummary = new ActionDolfinPrintSummary(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_SUMMARY_REPORT)
        );
        boolean isAllAcqs;
        if (!(isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName()))) {//For each acquirer
            if(Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())){
                actionDolfinPrintSummary.execute();
                return TransResult.SUCC;
            } else if(Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
                actionScbIppReport.execute();
                return TransResult.SUCC;
            } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
                actionScbRedeemReport.execute();
                return TransResult.SUCC;
            } else if(Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
                actionAmexReport.execute();
                return TransResult.SUCC;
            } else {
                total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer, false);
                if (total == null || total.isZero()) {
                    return TransResult.ERR_NO_TRANS;
                }
                total.setAcquirer(acquirer);
                total.setMerchantID(acquirer.getMerchantId());
                total.setTerminalID(acquirer.getTerminalId());
                total.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
            }
        }

        List<TransData> record = getTransAuditReport(acquirer, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM, Constants.ACQ_AMEX);
        if (!record.isEmpty()) {
            new ReceiptPrintTotal().print(activity.getString(R.string.print_history_summary).toUpperCase(), total, false,
                    new PrintListenerImpl(activity));
        }

        if (isAllAcqs){
            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintSummary.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });


            actionDolfinPrintSummary.setEndListener((action, result) -> {
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> cv.open());

            cv.close();
            actionAmexReport.execute();
            cv.block();
        }

        return TransResult.SUCC;
    }

    static ConditionVariable cv = new ConditionVariable();
    static int kcheckidCountRecord = -1;
    public static int getKCheckIDRecordCount() {
        kcheckidCountRecord = 0;
        cv.close();
        ActionGetKCheckIDRecordCount actionGetKCheckIDRecordCount = new ActionGetKCheckIDRecordCount(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetKCheckIDRecordCount) action).setParam(FinancialApplication.getApp().getApplicationContext());
            }
        });
        actionGetKCheckIDRecordCount.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet()==0) {
                    kcheckidCountRecord = (int)result.getData();
                } else {
                    kcheckidCountRecord = 0 ;
                }
                cv.open();
            }
        });
        actionGetKCheckIDRecordCount.execute();
        cv.block(10000);
        return kcheckidCountRecord;
    }


    /**
     * Print Summary Report (Lemon Farm template)
     * @param activity
     * @param acquirer
     * @return
     */
    public static int printSummaryReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory();
        int kcheckidRecordCount = getKCheckIDRecordCount();
        if (allTrans.isEmpty() && kcheckidRecordCount==0) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintSummary actionDolfinPrintSummary = new ActionDolfinPrintSummary(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_SUMMARY_REPORT)
        );
        ActionKCheckIDPrintReport actionKCheckIDPrintReport = new ActionKCheckIDPrintReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionKCheckIDPrintReport) action).setParam(activity, ActionKCheckIDPrintReport.processMode.SUMMARY_REPORT);
            }
        });

        boolean isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName());
        if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintSummary.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if(Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_KCHECKID.equalsIgnoreCase(acquirer.getName())) {
            actionKCheckIDPrintReport.execute();
            return TransResult.SUCC;
        }

        PrintListenerImpl listener = new PrintListenerImpl(activity);
        int ret = new ReceiptPrintSummary(acquirer, listener).print();
        if (listener != null) {
            listener.onEnd();
        }

        if (isAllAcqs) {
            if (ret != TransResult.SUCC && ret != TransResult.ERR_NO_TRANS) {
                return ret;
            }

            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintSummary.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });


            actionDolfinPrintSummary.setEndListener((action, result) -> {
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> {
                if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionKCheckIDPrintReport.setEndListener((action, result) -> cv.open());

            cv.close();
            actionAmexReport.execute();
            cv.block();
        } else {
            return ret;
        }

        return TransResult.SUCC;
    }

    public static int printSummaryReport(final Activity activity, final Acquirer acquirer, final AAction currentAction) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory();
        int kcheckidRecordCount = getKCheckIDRecordCount();
        if (allTrans.isEmpty() && kcheckidRecordCount==0) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintSummary actionDolfinPrintSummary = new ActionDolfinPrintSummary(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_SUMMARY_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_SUMMARY_REPORT)
        );
        ActionKCheckIDPrintReport actionKCheckIDPrintReport = new ActionKCheckIDPrintReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionKCheckIDPrintReport) action).setParam(activity, ActionKCheckIDPrintReport.processMode.SUMMARY_REPORT);
            }
        });

        boolean isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName());
        if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintSummary.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if(Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_KCHECKID.equalsIgnoreCase(acquirer.getName())) {
            actionKCheckIDPrintReport.execute();
            return TransResult.SUCC;
        }

        PrintListenerImpl listener = new PrintListenerImpl(activity);
        int ret = new ReceiptPrintSummary(acquirer, listener).print();
        if (listener != null) {
            listener.onEnd();
        }

        if (isAllAcqs) {
            if (ret != TransResult.SUCC && ret != TransResult.ERR_NO_TRANS) {
                return ret;
            }

            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintSummary.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });


            actionDolfinPrintSummary.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionKCheckIDPrintReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            });

            cv.close();
            actionAmexReport.execute();
            cv.block();
        } else {
            return ret;
        }

        return TransResult.SUCC;
    }

    public static int printSummaryDolfinIppReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataByAcqsAndMerchant(false, Collections.singletonList(acquirer));
        if (allTrans.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintSummary actionDolfinPrintSummary = new ActionDolfinPrintSummary(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });

        actionDolfinPrintSummary.setEndListener((action, result) -> {
            cv.open();
        });

        actionDolfinPrintSummary.execute();
        cv.block();

        return TransResult.SUCC;
    }

    /**
     * print last settlement
     */
    public static int printLastBatch(final Activity activity, final Acquirer acquirer, AAction exAction) {
        return printLastBatch(activity, acquirer, exAction, false);
    }

    public static int printLastBatch(final Activity activity, final Acquirer acquirer, AAction exAction, boolean SkipPrintFlag) {
        TransTotal total = FinancialApplication.getTransTotalDbHelper().findLastTransTotal(null, true);

        ActionDolfinPrintLastSettle actionDolfinPrintLastSettle = new ActionDolfinPrintLastSettle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_LAST_BATCH, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_LAST_BATCH, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_LAST_BATCH)
        );

        if (total == null || !total.getAcquirer().isEnable()) {
            return TransResult.ERR_NO_TRANS;
        }
        boolean isAllAcqs;
        if (!(isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName()))) {//For each acquirer
            if(Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())){
                actionDolfinPrintLastSettle.execute();
                TransContext.getInstance().setCurrentAction(exAction);
                return TransResult.SUCC;
            } else if(Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())){
                actionScbIppReport.execute();
                TransContext.getInstance().setCurrentAction(exAction);
                return TransResult.SUCC;
            } else if(Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())){
                actionScbRedeemReport.execute();
                TransContext.getInstance().setCurrentAction(exAction);
                return TransResult.SUCC;
            } else if(Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())){
                actionAmexReport.execute();
                TransContext.getInstance().setCurrentAction(exAction);
                return TransResult.SUCC;
            } else {
                total = FinancialApplication.getTransTotalDbHelper().findLastTransTotal(acquirer, true);
                if (total == null) {
                    return TransResult.ERR_NO_TRANS;
                }
            }
        } else {//For all acquirers
            total = null;
        }
        new ReceiptPrintTotal().print(activity.getString(R.string.last_settle_title), total, true,
                new PrintListenerImpl(activity), SkipPrintFlag);

        if (isAllAcqs) {
            final ConditionVariable cv = new ConditionVariable();

            actionAmexReport.setEndListener((action, result) -> {
                actionDolfinPrintLastSettle.execute();
            });

            actionDolfinPrintLastSettle.setEndListener((action1, result1) -> {
                actionScbIppReport.execute();
            });

            actionScbIppReport.setEndListener((action2, result2) -> {
                actionScbRedeemReport.execute();
            });

            actionScbRedeemReport.setEndListener((action3, result3) -> cv.open());

            cv.close();
            actionAmexReport.execute();
            cv.block();
        } else {
            TransContext.getInstance().setCurrentAction(exAction);
        }
        return TransResult.SUCC;
    }

    public static int printLastBatchOnSettlement(final Activity activity, final Acquirer acquirer, AAction exAction, TransTotal exTransTotal) {
        return printLastBatchOnSettlement(activity, acquirer, exAction, exTransTotal, false);
    }

    public static int printLastBatchOnSettlement(final Activity activity, final Acquirer acquirer, AAction exAction, TransTotal exTransTotal, boolean SkipPrintFlag) {
        TransTotal total = (exTransTotal == null) ? FinancialApplication.getTransTotalDbHelper().findLastTransTotal(null, true) : exTransTotal;
        ActionDolfinPrintLastSettle actionDolfinPrintLastSettle = new ActionDolfinPrintLastSettle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/ }
        });
        if (total == null || !total.getAcquirer().isEnable()) {
            return TransResult.ERR_NO_TRANS;
        }
        if (!Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {//For each acquirer
            if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
                actionDolfinPrintLastSettle.execute();
                TransContext.getInstance().setCurrentAction(exAction);
                return TransResult.SUCC;
            } else {
                total = (exTransTotal == null) ? FinancialApplication.getTransTotalDbHelper().findLastTransTotal(null, true) : exTransTotal;//FinancialApplication.getTransTotalDbHelper().findLastTransTotal(acquirer, true);
                if (total == null) {
                    return TransResult.ERR_NO_TRANS;
                }
            }
        } else {//For all acquirers
            total = null;
        }
        new ReceiptPrintTotal().print(activity.getString(R.string.last_settle_title), total, true,
                new PrintListenerImpl(activity), SkipPrintFlag);
        actionDolfinPrintLastSettle.execute();
        TransContext.getInstance().setCurrentAction(exAction);
        return TransResult.SUCC;

    }

    /**
     * print last settlement for Acquirer
     */
    public static int printLastSettlement(final Activity activity, final Acquirer acquirer) {
        TransTotal total = FinancialApplication.getTransTotalDbHelper().findLastTransTotal(acquirer, true);
        if (total == null) {
            return TransResult.ERR_NO_TRANS;
        }
        new ReceiptPrintTotal().print(activity.getString(R.string.last_settle_title), total, true,
                new PrintListenerImpl(activity));
        return TransResult.SUCC;

    }

    /**
     * print PromptPay settlement for Acquirer
     */
    public static int printPromptPaySettlement(final Activity activity, TransTotal total) {
        return printPromptPaySettlement(activity, total, false);
    }

    public static int printPromptPaySettlement(final Activity activity, TransTotal total, boolean skipPrintFlag) {
        if (total == null) {
            return TransResult.ERR_NO_TRANS;
        }
        new ReceiptPrintTotal().print(activity.getString(R.string.settle_title), total, false,
                new PrintListenerImpl(activity), skipPrintFlag);
        return TransResult.SUCC;

    }

    // reprint
    public static void printTransAgain(final Activity activity, final TransData transData) {
        if (transData.getAcquirer() != null) {
            switch (transData.getAcquirer().getName()) {
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    new ReceiptPrintRedeemedTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    new ReceiptPrintInstalmentKbankTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_AMEX_EPP:
                    new ReceiptPrintInstalmentAmexTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_BAY_INSTALLMENT:
                    new ReceiptPrintInstallmentBAYTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
                case Constants.ACQ_SCB_IPP:
                case Constants.ACQ_SCB_REDEEM:
                    ActionScbIppReprint actionScbIppReprint = new ActionScbIppReprint(
                            action -> ((ActionScbIppReprint) action).setParam(activity, transData.getTraceNo())
                    );
                    actionScbIppReprint.execute();
                    break;
                default:
                    new ReceiptPrintTrans().print(transData, true, new PrintListenerImpl(activity));
                    break;
            }
        }
    }

    /**
     * print settlement
     */
    public static void printSettle(final Activity activity, String title, TransTotal total, boolean settleFail, String errorCode) {
        printSettle(activity, title, total, settleFail, errorCode, false);
    }

    public static void printSettle(final Activity activity, String title, TransTotal total, boolean settleFail, String errorCode, boolean SkipPrintFlag) {
        int result = FinancialApplication.getController().get(Controller.RESULT);
        String resultMsg;
        if (result == 1) {
            resultMsg = activity.getString(R.string.print_card_check);
        } else if (result == 2) {
            resultMsg = activity.getString(R.string.print_card_check_uneven);
        } else {
            resultMsg = activity.getString(R.string.print_card_check_err);
        }

        new ReceiptPrintSettle().print(title, resultMsg, total, settleFail, errorCode,
                new PrintListenerImpl(activity), SkipPrintFlag);
    }

    /**
     * print failed details
     */
    public static int printFailDetail(String title, final Activity activity) {
        List<ETransType> list = new ArrayList<>();
        list.add(ETransType.SALE);

        List<TransData.ETransStatus> filter = new ArrayList<>();
        filter.add(ETransStatus.VOIDED);
        filter.add(ETransStatus.ADJUSTED);

        //AET-95
        List<TransData> records = FinancialApplication.getTransDataDbHelper().findTransData(list, filter, FinancialApplication.getAcqManager().getCurAcq());
        List<TransData> details = new ArrayList<>();
        if (records.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        for (TransData record : records) {
            if (record.getOfflineSendState() != null && record.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENT) {
                details.add(record);
            }
        }

        if (details.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        new ReceiptPrintFailedTransDetail().print(title, details, new PrintListenerImpl(activity));
        return TransResult.SUCC;
    }

    /**
     * print total detail
     *
     * @param activity context
     */
    //AET-112
    public static int printTotalDetail(final String title, final Activity activity, final Acquirer acquirer, final boolean emptyTrans, TransTotal total, boolean isOnSettlement) {
        new ReceiptPrintTotalDetail().print(title, acquirer, new PrintListenerImpl(activity), 0, emptyTrans, total, false, isOnSettlement);
        return TransResult.SUCC;
    }

    public static int printTotalDetail(final String title, final Activity activity, final Acquirer acquirer, final boolean emptyTrans, TransTotal total, boolean isOnSettlement, boolean skipPrintFlag) {
        new ReceiptPrintTotalDetail().print(title, acquirer, new PrintListenerImpl(activity), 0, emptyTrans, total, false, isOnSettlement, skipPrintFlag);
        return TransResult.SUCC;
    }

    /**
     * print QR Slip
     */
    public static void printQrSlip(final Activity activity, final String termId, final String merId, final String amt, final String datetime, final String qrRef2,
                                   final Bitmap bitmapQr, final String billerServiceCode, final String acqName) {
        new ReceiptPrintQrSlip().print(termId, merId, amt, datetime, qrRef2, bitmapQr, billerServiceCode, acqName, new PrintListenerImpl(activity));
    }

    public static List<TransData> getTransAuditReport(final Acquirer acquirer, final String...exceptAcqName) {
        List<ETransType> list = new ArrayList<>();

        if (Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {
            list.add(ETransType.SALE);
            list.add(ETransType.OFFLINE_TRANS_SEND);
            list.add(ETransType.VOID);
            list.add(ETransType.REFUND);
            list.add(ETransType.BPS_QR_SALE_INQUIRY);
            list.add(ETransType.QR_SALE_WALLET);
            list.add(ETransType.SALE_WALLET);
            list.add(ETransType.QR_VOID_WALLET);
            list.add(ETransType.REFUND_WALLET);
            list.add(ETransType.QR_SALE_ALL_IN_ONE);
            list.add(ETransType.STATUS_INQUIRY_ALL_IN_ONE);
            list.add(ETransType.BPS_QR_INQUIRY_ID);
            list.add(ETransType.QR_INQUIRY);
            list.add(ETransType.QR_INQUIRY_WECHAT);
            list.add(ETransType.QR_INQUIRY_ALIPAY);
            list.add(ETransType.QR_INQUIRY_CREDIT);
            list.add(ETransType.QR_VOID_KPLUS);
            list.add(ETransType.QR_VOID_ALIPAY);
            list.add(ETransType.QR_VOID_WECHAT);
            list.add(ETransType.QR_VOID_CREDIT);
            list.add(ETransType.QR_MYPROMPT_SALE);
            list.add(ETransType.QR_MYPROMPT_VOID);
            list.add(ETransType.KBANK_REDEEM_VOID);
            list.add(ETransType.KBANK_REDEEM_PRODUCT);
            list.add(ETransType.KBANK_REDEEM_PRODUCT_CREDIT);
            list.add(ETransType.KBANK_REDEEM_VOUCHER);
            list.add(ETransType.KBANK_REDEEM_VOUCHER_CREDIT);
            list.add(ETransType.KBANK_REDEEM_DISCOUNT);
            list.add(ETransType.KBANK_SMART_PAY);
            list.add(ETransType.KBANK_SMART_PAY_VOID);
            list.add(ETransType.AMEX_INSTALMENT);
            list.add(ETransType.BAY_INSTALMENT);
            list.add(ETransType.DOLFIN_SALE);
            list.add(ETransType.DOLFIN_INSTALMENT);
            list.add(ETransType.DOLFIN_INSTALMENT_VOID);
        } else {
            switch (acquirer.getName()) {
                case Constants.ACQ_QR_PROMPT:
                    list.add(ETransType.BPS_QR_SALE_INQUIRY);
                    list.add(ETransType.BPS_QR_INQUIRY_ID);
                    break;
                case Constants.ACQ_QRC:
                    list.add(ETransType.QR_SALE_ALL_IN_ONE);
                    list.add(ETransType.STATUS_INQUIRY_ALL_IN_ONE);
                    break;
                case Constants.ACQ_WALLET:
                    list.add(ETransType.QR_SALE_WALLET);
                    list.add(ETransType.SALE_WALLET);
                    list.add(ETransType.REFUND_WALLET);
                    break;
                case Constants.ACQ_KPLUS:
                    list.add(ETransType.QR_INQUIRY);
                    list.add(ETransType.QR_VOID_KPLUS);
                    break;
                case Constants.ACQ_MY_PROMPT:
                    list.add(ETransType.QR_MYPROMPT_SALE);
                    list.add(ETransType.QR_MYPROMPT_VOID);
                    break;
                case Constants.ACQ_ALIPAY:
                    list.add(ETransType.QR_INQUIRY_ALIPAY);
                    list.add(ETransType.QR_VOID_ALIPAY);
                    break;
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    list.add(ETransType.QR_ALIPAY_SCAN);
                    list.add(ETransType.QR_VOID_ALIPAY);
                    break;
                case Constants.ACQ_WECHAT:
                    list.add(ETransType.QR_INQUIRY_WECHAT);
                    list.add(ETransType.QR_VOID_WECHAT);
                    break;
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    list.add(ETransType.QR_WECHAT_SCAN);
                    list.add(ETransType.QR_VOID_WECHAT);
                    break;
                case Constants.ACQ_QR_CREDIT:
                    list.add(ETransType.QR_INQUIRY_CREDIT);
                    list.add(ETransType.QR_VOID_CREDIT);
                    break;
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    list.add(ETransType.KBANK_REDEEM_VOID);
                    list.add(ETransType.KBANK_REDEEM_PRODUCT);
                    list.add(ETransType.KBANK_REDEEM_PRODUCT_CREDIT);
                    list.add(ETransType.KBANK_REDEEM_VOUCHER);
                    list.add(ETransType.KBANK_REDEEM_VOUCHER_CREDIT);
                    list.add(ETransType.KBANK_REDEEM_DISCOUNT);
                    break;
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                    list.add(ETransType.KBANK_SMART_PAY);
                    list.add(ETransType.KBANK_SMART_PAY_VOID);
                    break;
                case Constants.ACQ_AMEX_EPP:
                    list.add(ETransType.AMEX_INSTALMENT);
                    list.add(ETransType.VOID);
                    break;
                case Constants.ACQ_BAY_INSTALLMENT:
                    list.add(ETransType.BAY_INSTALMENT);
                    list.add(ETransType.VOID);
                    break;
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    list.add(ETransType.DOLFIN_INSTALMENT);
                    list.add(ETransType.DOLFIN_INSTALMENT_VOID);
                    break;
                default:
                    list.add(ETransType.SALE);
                    list.add(ETransType.DOLFIN_SALE);
                    list.add(ETransType.OFFLINE_TRANS_SEND);
                    list.add(ETransType.VOID);
                    list.add(ETransType.REFUND);
                    break;
            }
        }

        List<TransData.ETransStatus> filter = new ArrayList<>();
        if (Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {
            filter.add(ETransStatus.VOIDED);
            filter.add(ETransStatus.NORMAL);
        } else {
            if (!Constants.ACQ_WALLET.equals(acquirer.getName())
                    && !Constants.ACQ_QR_PROMPT.equals(acquirer.getName())
                    && !Constants.ACQ_QRC.equals(acquirer.getName())) {
                filter.add(ETransStatus.VOIDED);
            }
        }

        List<TransData> record;
        if (exceptAcqName != null) {
            record = FinancialApplication.getTransDataDbHelper().findTransDataExceptAcq(list, filter, true, exceptAcqName);
        } else {
            if (Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {
                record = FinancialApplication.getTransDataDbHelper().findTransData(list, filter, true);
            } else {
                record = FinancialApplication.getTransDataDbHelper().findTransData(list, filter, acquirer);
            }
        }

        return record;
    }

    /**
     * print Audit Report
     *
     * @param activity context
     */
    //AET-112
    public static int printAuditReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> record = getTransAuditReport(acquirer);

        if (record.isEmpty() && FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX) == null) {
            return TransResult.ERR_NO_TRANS;
        }

        TransTotal total = null;
        ActionDolfinPrintAuditReport actionDolfinPrintAuditReport = new ActionDolfinPrintAuditReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*Do Nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_AUDIT_REPORT)
        );

        boolean isAllAcqs;
        if ((isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName()))) {
            total = FinancialApplication.getTransTotalDbHelper().calcTotal(false);
        } else if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintAuditReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        }

        record = getTransAuditReport(acquirer, Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM, Constants.ACQ_AMEX);

        TransTotal tmpTransTotal = new TransTotal();
        tmpTransTotal.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
        tmpTransTotal.setAcquirer(acquirer);
        if (!record.isEmpty()) {
            try {
                long[] totalList = getSummaryFromTransData(record, acquirer);
                tmpTransTotal.setSaleTotalNum(totalList[0]);
                tmpTransTotal.setSaleTotalAmt(totalList[1]);
            } catch (Exception ex) {
            }
        }
        EcrData.instance.createAuditReportEcrResponse(tmpTransTotal);

        // EDC will dont print when EDC on Ecr\LinkPos mode and request to print audit report.
        if (!record.isEmpty()) {
            new ReceiptPrintAuditReport().print(record, total, acquirer, new PrintListenerImpl(activity));
        }


        if (isAllAcqs) {
            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintAuditReport.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });


            actionDolfinPrintAuditReport.setEndListener((action, result) -> {
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> cv.open());

            cv.close();
            actionAmexReport.execute();
            cv.block();
        }

        return TransResult.SUCC;
    }

    public static int printAuditReport(final Activity activity, final Acquirer acquirer, final AAction currentAction) {
        List<TransData> record = getTransAuditReport(acquirer);

        if (record.isEmpty() && FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX) == null) {
            return TransResult.ERR_NO_TRANS;
        }

        TransTotal total = null;
        ActionDolfinPrintAuditReport actionDolfinPrintAuditReport = new ActionDolfinPrintAuditReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*Do Nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_AUDIT_REPORT)
        );

        boolean isAllAcqs;
        if ((isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName()))) {
            total = FinancialApplication.getTransTotalDbHelper().calcTotal(false);
        } else if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintAuditReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        }

        record = getTransAuditReport(acquirer, Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM, Constants.ACQ_AMEX);

        TransTotal tmpTransTotal = new TransTotal();
        tmpTransTotal.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
        tmpTransTotal.setAcquirer(acquirer);
        if (!record.isEmpty()) {
            try {
                long[] totalList = getSummaryFromTransData(record, acquirer);
                tmpTransTotal.setSaleTotalNum(totalList[0]);
                tmpTransTotal.setSaleTotalAmt(totalList[1]);
            } catch (Exception ex) {
            }
        }
        EcrData.instance.createAuditReportEcrResponse(tmpTransTotal);

        // EDC will dont print when EDC on Ecr\LinkPos mode and request to print audit report.
        if (!record.isEmpty()) {
            new ReceiptPrintAuditReport().print(record, total, acquirer, new PrintListenerImpl(activity));
        }


        if (isAllAcqs) {
            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintAuditReport.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });


            actionDolfinPrintAuditReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            });

            cv.close();
            actionAmexReport.execute();
            cv.block();
        }

        return TransResult.SUCC;
    }

    public static int printDetailReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory();
        int kcheckidRecordCount = getKCheckIDRecordCount();
        if (allTrans.isEmpty() && kcheckidRecordCount==0) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintAuditReport actionDolfinPrintAuditReport = new ActionDolfinPrintAuditReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*Do Nothing*/ }
        });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_AUDIT_REPORT)
        );
        ActionKCheckIDPrintReport actionKCheckIDPrintReport = new ActionKCheckIDPrintReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionKCheckIDPrintReport) action).setParam(activity, ActionKCheckIDPrintReport.processMode.DETAIL_REPORT);
            }
        });



        boolean isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName());
        if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintAuditReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_KCHECKID.equalsIgnoreCase(acquirer.getName())) {
            actionKCheckIDPrintReport.execute();
            return TransResult.SUCC;
        }

        PrintListenerImpl listener = new PrintListenerImpl(activity);
        int ret = new ReceiptPrintDetailReport(acquirer, listener).print();
        if (listener != null) {
            listener.onEnd();
        }

        if (isAllAcqs) {
            if (ret != TransResult.SUCC && ret != TransResult.ERR_NO_TRANS) {
                return ret;
            }

            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintAuditReport.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });


            actionDolfinPrintAuditReport.setEndListener((action, result) -> {
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> {
                if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    cv.open();
                }
            });

            actionKCheckIDPrintReport.setEndListener((action, result) -> cv.open());

            cv.close();
            actionAmexReport.execute();
            cv.block();
        } else {
            return ret;
        }

        return TransResult.SUCC;
    }

    public static int printDetailReport(final Activity activity, final Acquirer acquirer, final AAction currentAction) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataHistory();
        int kcheckidRecordCount = getKCheckIDRecordCount();
        if (allTrans.isEmpty() && kcheckidRecordCount==0) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintAuditReport actionDolfinPrintAuditReport = new ActionDolfinPrintAuditReport(action -> {/*Do Nothing*/ });
        ActionScbIppReport actionScbIppReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_IPP)
        );
        ActionScbIppReport actionScbRedeemReport = new ActionScbIppReport(
                action -> ((ActionScbIppReport) action).setParam(activity, ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT, Constants.ACQ_SCB_REDEEM)
        );
        ActionAmexReport actionAmexReport = new ActionAmexReport(action ->
                ((ActionAmexReport) action).setParam(activity, AmexReportActivity.ReportType.PRN_AUDIT_REPORT)
        );
        ActionKCheckIDPrintReport actionKCheckIDPrintReport = new ActionKCheckIDPrintReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionKCheckIDPrintReport) action).setParam(activity, ActionKCheckIDPrintReport.processMode.DETAIL_REPORT);
            }
        });

        boolean isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName());
        if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())) {
            actionDolfinPrintAuditReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())) {
            actionScbIppReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())) {
            actionScbRedeemReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())) {
            actionAmexReport.execute();
            return TransResult.SUCC;
        } else if (Constants.ACQ_KCHECKID.equalsIgnoreCase(acquirer.getName())) {
            actionKCheckIDPrintReport.execute();
            return TransResult.SUCC;
        }

        PrintListenerImpl listener = new PrintListenerImpl(activity);
        int ret = new ReceiptPrintDetailReport(acquirer, listener).print();
        if (listener != null) {
            listener.onEnd();
        }

        if (isAllAcqs) {
            if (ret != TransResult.SUCC && ret != TransResult.ERR_NO_TRANS) {
                return ret;
            }

            final ConditionVariable cv = new ConditionVariable();

            Acquirer dolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            TransTotal dolfinTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(dolfin, false);

            Acquirer scb = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
            TransTotal scbTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scb, false);

            Acquirer scbRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
            TransTotal scbRedeemTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(scbRedeem, false);

            actionAmexReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (dolfinTotal != null && !dolfinTotal.isZero()) {
                    actionDolfinPrintAuditReport.execute();
                } else if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });


            actionDolfinPrintAuditReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbTotal != null && !scbTotal.isZero()) {
                    actionScbIppReport.execute();
                } else if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbIppReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (scbRedeemTotal != null && !scbRedeemTotal.isZero()) {
                    actionScbRedeemReport.execute();
                } else if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionScbRedeemReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(null);
                if (kcheckidRecordCount > 0) {
                    actionKCheckIDPrintReport.execute();
                } else {
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            });

            actionKCheckIDPrintReport.setEndListener((action, result) -> {
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            });

            cv.close();
            actionAmexReport.execute();
            cv.block();
        } else {
            return ret;
        }

        return TransResult.SUCC;
    }

    public static int printDetailDolfinIppReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransDataByAcqsAndMerchant(false, Collections.singletonList(acquirer));
        if (allTrans.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        ActionDolfinPrintAuditReport actionDolfinPrintAuditReport = new ActionDolfinPrintAuditReport(action -> {/*Do Nothing*/ });

        actionDolfinPrintAuditReport.setEndListener((action, result) -> {
            cv.open();
        });

        actionDolfinPrintAuditReport.execute();
        cv.block();

        return TransResult.SUCC;
    }

    public static int printPreAuthDetailReport(final Activity activity, final Acquirer acquirer) {
        List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllPreAuthTransaction(false);
        if (allTrans.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        PrintListenerImpl listener = new PrintListenerImpl(activity);
        int ret = new ReceiptPrintPreAuthDetailReport(acquirer, listener).print();
        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

    private static long[] getSummaryFromTransData(List<TransData> transDataList, Acquirer acquirer) {
        long tranSaleCount = 0;
        long transSaleAmount = 0;
        if (transDataList != null) {
            if (transDataList.size() > 0) {
                for (TransData tmpTransData : transDataList) {
                    if (tmpTransData.getAcquirer().getName().equals(acquirer.getName())) {
                        tranSaleCount += 1;
                        transSaleAmount += Long.parseLong(tmpTransData.getAmount());
                    }
                }
            }
        }

        return new long[]{tranSaleCount, transSaleAmount};
    }

    public static int printSpace(final Activity activity) {
        return printSpace(activity, false);
    }

    public static int printSpace(final Activity activity, boolean SkipPrintFlag) {
        new ReceiptPrintTotal().printSpace(new PrintListenerImpl(activity), SkipPrintFlag);
        return TransResult.SUCC;
    }

    /**
     * print settlement
     */
    public static void printSettleGrandTotal(final Activity activity, TransTotal total) {
        printSettleGrandTotal(activity, total, false);
    }

    public static void printSettleGrandTotal(final Activity activity, TransTotal total, boolean SkipPrintFlag) {
        new ReceiptPrintSettle().printGrandTotal(total, new PrintListenerImpl(activity), SkipPrintFlag);
    }

    /**
     * print bitmap
     */
    public static int printBitmap(final Activity activity, Bitmap bitmap) {
        //new ReceiptPrintBitmap().printBitmap( bitmap , new PrintListenerImpl(activity));
        return TransResult.SUCC;
    }

    public static void printAppNameVersion(final PrintListener listener, boolean forceEndListener) {
        printAppNameVersion(listener, forceEndListener, false);
    }

    public static void printAppNameVersion(final PrintListener listener, boolean forceEndListener, boolean SkipPrintFlag) {
        new ReceiptPrintBitmap().printAppNameVersion(listener, forceEndListener, SkipPrintFlag);
    }

    public static Bitmap printEReceiptSlip(@ReceiptPrintTransForERM.PrintType int printType, TransData transData, boolean isReprint, boolean withoutPrint, final Activity activity) {
        return new ReceiptPrintTransForERM().print(printType, transData, isReprint, true, withoutPrint, new PrintListenerImpl(activity));
    }

    public static int printERMReport(final Activity activity, long[] ermSuccessTotal, long[] ermUnsuccessfulTotal, long[] ermVoidSuccessTotal, long[] ermVoidUnsuccessfulTotal) {
        return printERMReport(activity, ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal, false);
    }

    public static int printERMReport(final Activity activity, long[] ermSuccessTotal, long[] ermUnsuccessfulTotal, long[] ermVoidSuccessTotal, long[] ermVoidUnsuccessfulTotal, boolean SkipPrintFlag) {
        return new ReceiptPrintEReceiptUploadStatus().print(new PrintListenerImpl(activity), ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal, SkipPrintFlag);
    }

    public static int printEReceiptDetailReport(final Activity activity, List<TransData> transData) {
        return new ReceiptPrintEReceiptDetailReport().print(new PrintListenerImpl(activity), transData);
    }

    public static int printAutoSettlementOnWakeUp(final Activity activity, List<String> hostList, String title) {
        return new ReceiptPrintAutoSettlementOnExecution().print(new PrintListenerImpl(activity), hostList, title);
    }

    public static int printEReceiptPreSettleUploadFailed(final Activity activity) {
        return new ReceiptPrintAutoSettlementOnExecution().printEReceiptOnFailedUpload(new PrintListenerImpl(activity));
    }

    public static int printSettleHostNotfound(final Activity activity) {
        return new ReceiptPrintAutoSettlementOnExecution().printHostNotFound(new PrintListenerImpl(activity));
    }
}
