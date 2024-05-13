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
package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

/**
 * print total
 *
 * @author Steven.W
 */
public class ReceiptPrintTotal extends AReceiptPrint {

    private boolean isLastBatch;

    /**
     * printTransTotal, printLastBatch, printLastSettlement, printPromptPaySettlement
     *
     * @param title
     * @param transTotal
     * @param isLastBatch
     * @param listener
     * @return
     */
    public int print(String title, TransTotal transTotal, boolean isLastBatch, PrintListener listener) {
        return print(title, transTotal, isLastBatch, listener, false);
    }

    public int print(String title, TransTotal transTotal, boolean isLastBatch, PrintListener listener, boolean SkipPrintFlag) {
        this.listener = listener;
        this.isLastBatch = isLastBatch;
        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }

        if (transTotal == null) {
            List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
            int acqNum = 0;
            ArrayList<TransTotal> lastSettleList = new ArrayList<>();
            for (Acquirer acquirer : acquirers) {
                if (Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())
                        || Constants.ACQ_SCB_IPP.equalsIgnoreCase(acquirer.getName())
                        || Constants.ACQ_SCB_REDEEM.equalsIgnoreCase(acquirer.getName())
                        || Constants.ACQ_AMEX.equalsIgnoreCase(acquirer.getName())){
                    continue;
                }
                if (isLastBatch) {//For print last settlement
                    transTotal = FinancialApplication.getTransTotalDbHelper().findLastTransTotal(acquirer, true);
                } else {//For print before settlement
                    transTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer);
                    transTotal.setAcquirer(acquirer);
                    transTotal.setMerchantID(acquirer.getMerchantId());
                    transTotal.setTerminalID(acquirer.getTerminalId());
                    transTotal.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
                }
                if (transTotal != null) {
                    lastSettleList.add(transTotal);
                    boolean isWalletSettle = acquirer.getName().equals(Constants.ACQ_WALLET) && transTotal.isClosed();
                    if (!transTotal.isZero()) {
                        acqNum = !isLastBatch ? acqNum + 1 : 0;
                        printBitmapByAcquirer(title, acquirer.getName(), transTotal, acqNum, isLastBatch, SkipPrintFlag);
                        if (!transTotal.getAcquirer().getName().equals(Constants.ACQ_QRC)
                                && !transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM)
                                && !transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM_BDMS)
                                && !isWalletSettle) {// promtpay, settlement rabbit and settlement wallet don't need details
                            new ReceiptPrintTotalDetail().print("TOTAL BY ISSUER", acquirer, listener, acqNum, false, transTotal, isLastBatch, SkipPrintFlag);
                        }
                    }
                }
            }

            boolean isGrandTotal = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL);
            if (isGrandTotal) {
                if (!isLastBatch) {
                    printGrandTotal(null, SkipPrintFlag);
                } else {
                    TransTotal grandTotal = FinancialApplication.getTransTotalDbHelper().sumTransTotal(lastSettleList);
                    if (grandTotal != null) {
                        printGrandTotal(grandTotal, SkipPrintFlag);
                    }
                }
            }

            if (acqNum > 0) {
                Printer.printAppNameVersion(listener, false, SkipPrintFlag);
            }
            printStr("\n\n\n\n\n\n", SkipPrintFlag);
        } else {
            boolean isWalletSettle = transTotal.getAcquirer().getName().equals(Constants.ACQ_WALLET) && transTotal.isClosed();
            String acquirerName = transTotal.getAcquirer() != null ? transTotal.getAcquirer().getName() : "";
            printBitmapByAcquirer(title, acquirerName, transTotal, 999, isLastBatch, SkipPrintFlag); //acqNum 999 = summary 1 acq
            if (!transTotal.getAcquirer().getName().equals(Constants.ACQ_QRC)
                    && !transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM)
                    && !transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM_BDMS)
                    && !isWalletSettle) { // promtpay, settlement rabbit and settlement wallet don't need details
                new ReceiptPrintTotalDetail().print("TOTAL BY ISSUER", transTotal.getAcquirer(), listener, 999, false, transTotal, isLastBatch, SkipPrintFlag); //acqNum 999 = summary 1 acq
            }

            Printer.printAppNameVersion(listener, false, SkipPrintFlag);

            boolean isGrandTotal = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL);
            if (!isGrandTotal) {
                printStr("\n\n\n\n\n\n", SkipPrintFlag);
            }
        }

        if (listener != null) {
            listener.onEnd();
        }
        return 0;
    }

    private void printBitmapByAcquirer(String title, String acquirerName, TransTotal transTotal, int qcqNum, boolean isReprint) {
        printBitmapByAcquirer(title, acquirerName, transTotal, qcqNum, isReprint, false);
    }

    private void printBitmapByAcquirer(String title, String acquirerName, TransTotal transTotal, int qcqNum, boolean isReprint, boolean SkipPrintFlag) {
        if (!Constants.ACQ_WALLET.equals(acquirerName)) {
            ReceiptGeneratorTotal receiptGeneratorTotal = new ReceiptGeneratorTotal(title, null, transTotal, qcqNum, isLastBatch);
            if (transTotal.getAcquirer().getName().equals(Constants.ACQ_QRC)) {
                if (title.equals(Utils.getString(R.string.print_history_summary).toUpperCase()) && qcqNum == 1) {
                    Bitmap bitmap = null;
                    if (Constants.isTOPS) {
                        ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(title, null, transTotal, qcqNum, isLastBatch);
                        bitmap = receiptGeneratorTotalTOPS.generateBitmap();
                    } else {
                        bitmap = receiptGeneratorTotal.generateBitmap();
                    }
                    //Bitmap bitmap = receiptGeneratorTotal.generateBitmap();
                    if (bitmap != null) {
                        printBitmap(bitmap, SkipPrintFlag);
                        printReprint(receiptGeneratorTotal, isReprint, SkipPrintFlag);
                    }
                }
                if (Constants.isTOPS) {
                    ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(title, null, transTotal, qcqNum, isLastBatch);
                    printBitmap(receiptGeneratorTotalTOPS.generateBitmapPrompt(), SkipPrintFlag);
                } else {
                    printBitmap(receiptGeneratorTotal.generateBitmapPrompt(), SkipPrintFlag);
                }

            } else if (transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM) || transTotal.getAcquirer().getName().equals(Constants.ACQ_REDEEM_BDMS)) {
                Bitmap bitmap = null;
                if (Constants.isTOPS) {
                    ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(title, null, transTotal, qcqNum, isLastBatch);
                    bitmap = receiptGeneratorTotalTOPS.generateBitmapRedeemKbank();
                } else {
                    bitmap = receiptGeneratorTotal.generateBitmapRedeemKbank();
                }
                //Bitmap bitmap = receiptGeneratorTotal.generateBitmapRedeemKbank();
                if (bitmap != null) {
                    printBitmap(bitmap, SkipPrintFlag);
                    printReprint(receiptGeneratorTotal, isReprint, SkipPrintFlag);
                }
            } else {
                Bitmap bitmap = null;
                if (Constants.isTOPS) {
                    ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(title, null, transTotal, qcqNum, isLastBatch);
                    bitmap = receiptGeneratorTotalTOPS.generateBitmap();
                } else {
                    bitmap = receiptGeneratorTotal.generateBitmap();
                }
                //Bitmap bitmap = receiptGeneratorTotal.generateBitmap();
                if (bitmap != null) {
                    printBitmap(bitmap, SkipPrintFlag);
                }
            }
        } else {
            if (!acquirerName.isEmpty()) {
                if (title.equals(Utils.getString(R.string.print_history_summary).toUpperCase())) {
                    Bitmap bitmap = null;
                    if (Constants.isTOPS) {
                        ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(title, null, transTotal, qcqNum, isLastBatch);
                        bitmap = receiptGeneratorTotalTOPS.generateBitmap();
                    } else {
                        ReceiptGeneratorTotal receiptGeneratorTotal = new ReceiptGeneratorTotal(title, null, transTotal, qcqNum, isLastBatch);
                        bitmap = receiptGeneratorTotal.generateBitmap();
                    }
                    //Bitmap bitmap = receiptGeneratorTotal.generateBitmap();
                    if (bitmap != null) {
                        printBitmap(bitmap, SkipPrintFlag);
                    }
                } else {
                    ReceiptGeneratorTotalDetailWallet receiptGeneratorTotalDetailWallet = new ReceiptGeneratorTotalDetailWallet(transTotal.getAcquirer(), transTotal, isLastBatch);
                    Bitmap bitmap = receiptGeneratorTotalDetailWallet.generateBitmap();
                    if (bitmap != null) {
                        printBitmap(bitmap, SkipPrintFlag);
                    }
                }
            }
        }
    }

    public int printSpace(PrintListener listener) {
        return printSpace(listener, false);
    }

    public int printSpace(PrintListener listener, boolean SkipPrintFlag) {
        printStr("\n\n\n\n\n\n", SkipPrintFlag);
        this.listener = listener;
        /*if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }*/
        if (listener != null) {
            listener.onEnd();
        }
        return 0;
    }

    private void printGrandTotal(TransTotal transTotal) {
        printGrandTotal(transTotal, false);
    }

    private void printGrandTotal(TransTotal transTotal, boolean skipPrint) {
        ReceiptGeneratorTotal receiptGeneratorTotal = new ReceiptGeneratorTotal(null, null, transTotal, 0, isLastBatch);
        Bitmap bitmap = receiptGeneratorTotal.generateGrandTotal(transTotal);
        if (bitmap != null) {
            printBitmap(bitmap, skipPrint);
        }
    }

    private void printReprint(ReceiptGeneratorTotal receiptGeneratorTotal, boolean isReprint) {
        printReprint(receiptGeneratorTotal, isReprint, false);
    }

    private void printReprint(ReceiptGeneratorTotal receiptGeneratorTotal, boolean isReprint, boolean skipPrint) {
        if (isReprint) {
            Bitmap bitmap = receiptGeneratorTotal.generateReprint();
            printBitmap(bitmap, skipPrint);
        }
    }

}
