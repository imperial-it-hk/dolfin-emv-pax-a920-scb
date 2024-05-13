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

import android.content.Context;
import android.view.Gravity;

import com.pax.appstore.DownloadManager;
import com.pax.device.Device;
import com.pax.edc.BuildConfig;
import com.pax.edc.R;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.SettlementRegisterActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.settings.SysParam;

import java.util.List;

import th.co.bkkps.edc.receiver.process.SettleAlarmProcess;

import static com.pax.pay.utils.Utils.getString;

/**
 * receipt generator
 *
 * @author Steven.W
 */
class ReceiptGeneratorSystemParam extends ReceiptGeneratorParam implements IReceiptGenerator {

    ReceiptGeneratorSystemParam() {
        //do nothing
    }

    @Override
    protected IPage generatePage(Context context) {
        IPage page = Device.generatePage();
        SysParam sysParam = FinancialApplication.getSysParam();
        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        //merchant name
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("PARAMETER SETTINGS\n")
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress1)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*System param*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(" SERIAL NUMBER : " + DownloadManager.getInstance().getSn())
                        .setFontSize(FONT_SMALL_18)
                );
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(" APPLICATION VERSION :" + BuildConfig.VERSION_NAME)
                        .setFontSize(FONT_SMALL_18)
                );

        page.addLine().addUnit(page.createUnit()
                .setText(getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*ACQ param*/
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > ACQUIRER CONFIG"));
        page.addLine().addUnit(page.createUnit().setText(getString(R.string.receipt_double_line)).setGravity(Gravity.CENTER));
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        for (Acquirer acq : acquirers) {

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(String.valueOf(acq.getNii()) + "    " + acq.getName())
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText("STATUS : " + getStatus(acq.isEnable()))
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END)
                    );

            if (acq.isEnable()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TID: " + String.valueOf(acq.getTerminalId()))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                        )
                        .addUnit(page.createUnit()
                            .setText("MID: " + String.valueOf(acq.getMerchantId()))
                            .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.END)
                        );
                if (acq.getBillerIdPromptPay() != null) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("BILLER ID: " + String.valueOf(acq.getBillerIdPromptPay()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }
                if (acq.getBillerServiceCode() != null) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("SERVICE CODE: " + String.valueOf(acq.getBillerServiceCode()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TLE : " + getStatus(acq.isEnableTle()))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                        )
                        .addUnit(page.createUnit()
                                .setText("ERCM : " + ((acq.getName() == Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE || acq.getName() == Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE) ? "N/A" : getStatus(acq.isEnableUploadERM())))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.END)
                        );

                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(acq.getName() + "  IP-ADDRESS :-")
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                        );

                if (!String.valueOf(acq.getIp()).equals("null")) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("\t\tPRIMARY     : " + String.valueOf(acq.getIp()) + " : " + String.valueOf(acq.getPort()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }
                if (!String.valueOf(acq.getIpBak1()).equals("null")) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("\t\tBACKUP [1] : " + String.valueOf(acq.getIpBak1()) + " : " + String.valueOf(acq.getPortBak1()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }
                if (!String.valueOf(acq.getIpBak2()).equals("null")) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("\t\tBACKUP [2] : " + String.valueOf(acq.getIpBak2()) + " : " + String.valueOf(acq.getPortBak2()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }
                if (!String.valueOf(acq.getIpBak3()).equals("null")) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("\t\tBACKUP [3] : " + String.valueOf(acq.getIpBak3()) + " : " + String.valueOf(acq.getPortBak3()))
                                    .setFontSize(FONT_SMALL_18)
                            );
                }

                if (String.valueOf(acq.getIp()).equals("null")
                        && String.valueOf(acq.getIpBak1()).equals("null")
                        && String.valueOf(acq.getIpBak2()).equals("null")
                        && String.valueOf(acq.getIpBak3()).equals("null")) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(" === NO IP-ADDRESS LIST ===")
                                    .setFontSize(FONT_SMALL_18)
                            );
                }

                if(acq.getName().equals(Constants.ACQ_DOLFIN)){
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("ENABLE C SCAN B : " + getStatus(acq.isEnableCScanBMode()))
                                    .setFontSize(FONT_NORMAL)
                            );
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("DISPLAY QR TIMEOUT : " + String.valueOf(acq.getCScanBDisplayQrTimeout()))
                                    .setFontSize(FONT_NORMAL)
                            );
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("RETRY TIMES : " + String.valueOf(acq.getCScanBRetryTimes()))
                                    .setFontSize(FONT_NORMAL)
                            );
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("DELAY RETRY : " + String.valueOf(acq.getCScanBDelayRetry()))
                                    .setFontSize(FONT_NORMAL)
                            );
                }

                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("Enable Settle : " + SettlementRegisterActivity.Companion.isEnableSettleMode())
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                                .setWeight(3)
                        )
                        .addUnit(page.createUnit()
                                .setText("Time : " + ((acq.getSettleTime()==null) ? "-" : acq.getSettleTime()))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.END)
                                .setWeight(2)
                        );

                if (SettlementRegisterActivity.Companion.isEnableSettleMode()) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("Mode : " + SettleAlarmProcess.SettlementMode.Companion.getMode(SettlementRegisterActivity.Companion.getEDCSettlementMode()))
                                    .setFontSize(FONT_SMALL_18)
                                    .setGravity(Gravity.START)
                                    .setWeight(6)
                            );
                }

                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("last-settle on : " + ((acq.getLatestSettledDateTime()==null) ? "-" : acq.getLatestSettledDateTime()))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                        );

                boolean isSupportCtrlLimit = ControlLimitUtils.Companion.isSupportControlLimitHost(acq.getName());
                boolean isSupportPhoneInput = ControlLimitUtils.Companion.isAllowEnterPhoneNumber(acq.getName());
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("CtrlLimit : " + (isSupportCtrlLimit ? "Enabled" : "Disable"))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.START)
                                .setWeight(2)
                        )
                        .addUnit(page.createUnit()
                                .setText("Phone input : " + (isSupportPhoneInput ? "Enabled" : "Disable"))
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.END)
                                .setWeight(3)
                        );
            }

            page.addLine().addUnit(page.createUnit()
                    .setText(getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));
        }


        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.LEFT).setFontSize(FONT_SMALL_18).setText(" > OTHER COMMUNICATION SETTING"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        String tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("APN : " + tmpStr)
                        .setFontSize(FONT_SMALL_18)
                );


        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > OTHER SETTINGS"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        boolean tmp = false;
        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("SUPPORT SP200 : ")
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                    .setText(getStatus(tmp))
                    .setFontSize(FONT_SMALL_18)
                    .setGravity(Gravity.END)
        );
        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VOID BY STAN NO. : ")
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                        .setText(getStatus(tmp))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.END)
        );
        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_KIOSK_MODE);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ENABLE KIOSK MODE : " )
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                        .setText(getStatus(tmp))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.END)
                );
        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("PRINT GRAND TOTAL : " )
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                        .setText(getStatus(tmp))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.END)
                );

        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("PRINT QR/BARCODE : " )
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                        .setText(getStatus(tmp))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.END)
                );

        tmp = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_COD);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("PRINT BARCODE COD : " )
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
                )
                .addUnit(page.createUnit()
                        .setText(getStatus(tmp))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.END)
                );
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > LINKPOS CONFIG"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);
        if (tmpStr != null) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("LINKPOS MERCHANT  :  " )
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText(tmpStr)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END)
            );

            tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("PROTOCOL  :  ")
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText(tmpStr)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END)
            );



//            tmpStr = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_COMM_TYPE);
//            page.addLine()
//                    .addUnit(page.createUnit()
//                            .setText("COMM TYPE: " + tmpStr)
//                            .setFontSize(FONT_SMALL_18)
//                    );
            page.addLine().addUnit(page.createUnit().setText("1. Print options (LinkPOS mode):-").setFontSize(FONT_SMALL_18));
            boolean tmpBool = false;
            String tmpResult = "";
            try {
                tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE);
                tmpStr = "   1.1 Settlement report  :  " ;
                tmpResult = ((tmpBool) ? "Enable" : "Disable");
            } catch (Exception ex) {
                tmpStr = "   1.1 Settlement report  :  ";
                tmpResult = "missing";
            }
            page.addLine().addUnit(page.createUnit().setText(tmpStr).setFontSize(FONT_SMALL_18).setGravity(Gravity.START))
                          .addUnit(page.createUnit().setText(tmpResult).setFontSize(FONT_SMALL_18).setGravity(Gravity.END));

            try {
                tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE);
                tmpStr = "   1.2 Audit Report  :  " ;
                tmpResult = ((tmpBool) ? "Enable" : "Disable");
            } catch (Exception ex) {
                tmpStr = "   1.2 Audit Report  :  " ;
                tmpResult = "missing";
            }
            page.addLine().addUnit(page.createUnit().setText(tmpStr).setFontSize(FONT_SMALL_18).setGravity(Gravity.START))
                          .addUnit(page.createUnit().setText(tmpResult).setFontSize(FONT_SMALL_18).setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit().setText("2. Confirm Dialog Bypass (LinkPOS mode):-").setFontSize(FONT_SMALL_18));
            try {
                tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE);
                tmpStr = "   2.1 SETTLEMENT :  " ;
                tmpResult = ((tmpBool) ? "Enable" : "Disable");
            } catch (Exception ex) {
                tmpStr = "   2.1 SETTLEMENT :  " ;
                tmpResult = "missing";
            }
            page.addLine().addUnit(page.createUnit().setText(tmpStr).setFontSize(FONT_SMALL_18).setGravity(Gravity.START))
                          .addUnit(page.createUnit().setText(tmpResult).setFontSize(FONT_SMALL_18).setGravity(Gravity.END));

            try {
                tmpBool = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID);
                tmpStr = "   2.2 VOID :  " ;
                tmpResult = ((tmpBool) ? "Enable" : "Disable");
            } catch (Exception ex) {
                tmpStr = "   2.2 VOID :  ";
                tmpResult= "missing";
            }
            page.addLine().addUnit(page.createUnit().setText(tmpStr).setFontSize(FONT_SMALL_18).setGravity(Gravity.START))
                          .addUnit(page.createUnit().setText(tmpResult).setFontSize(FONT_SMALL_18).setGravity(Gravity.END));

        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("LINKPOS STATUS: Disable")
                            .setFontSize(FONT_SMALL_18)
                    );
        }



        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > TLE DOWNLOAD STATUS"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        String status;
        for (Acquirer i : acquirers) {
            if (i.isEnableTle()) {
                if (i.getTMK() != null && i.getTWK() != null) {
                    status = String.format("%s = %s", i.getName(), String.valueOf("TRUE"));
                } else {
                    status = String.format("%s = %s", i.getName(), String.valueOf("FALSE"));
                }
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(status)
                                .setFontSize(FONT_SMALL_18)
                        );
            }
        }



        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > EDC FILE CONFIG UPLOAD"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        int fileStatus = sysParam.get(SysParam.NumberParam.EDC_AID_FILE_UPLOAD_STATUS);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("AID: " + getFileUploadStatus(fileStatus))
                        .setFontSize(FONT_SMALL_18)
                );

        fileStatus = sysParam.get(SysParam.NumberParam.EDC_CARD_RANGE_FILE_UPLOAD_STATUS);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CARD RANGE: " + getFileUploadStatus(fileStatus))
                        .setFontSize(FONT_SMALL_18)
                );

        fileStatus = sysParam.get(SysParam.NumberParam.EDC_ISSUER_FILE_UPLOAD_STATUS);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ISSUER: " + getFileUploadStatus(fileStatus))
                        .setFontSize(FONT_SMALL_18)
                );

        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > E-RECEIPT CONFIG"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" E-SIGNATURE ENABLE\t: " + ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE)) ? "ON" : "OFF")));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" ERCM ENABLE\t\t\t\t: " + ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) ? "ON" : "OFF")));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" INITIAL STATUS\t\t\t\t: " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED)) == null ? "NO" + ((!EReceiptUtils.isFoundKbankPublicKeyFile()) ? " (missing PBK file)" : "") : "YES")));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" Details of ERM settings"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("   1. ERM Initial information"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         1.1 BANK : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE)) == null ? "-" : FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         1.2 MERC : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE)) == null ? "-" : FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         1.3 STORE : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE)) == null ? "-" : FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         1.4 KEY.VER : " + ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION)) == null ? "-" : FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("   2. ERM PAPER-RECEIPT PRINT"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         2.1 ON UPLOAD SUCCESS : " + NumberOfReceipt(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         2.2 ON UPLOAD FAIL : " + NumberOfReceipt(FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("   3. NEXT TRANSACTION UPLOAD : " + ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD)) ? "ON" : "OFF")));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("   4. ERM PRE-SETTLEMENT PRINTING"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         4.1 ON UPLOAD FAILED : " + FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE)));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("         4.2 PENDING LIST : " + (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS) ? "PRINT ALL" : "ONLY NEVER PRINT")));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("   "));
        int sequentNo = 0;
        String HostID = "";
        String HostName = "";
        String HostUploadActive = "";
        String strA = "";
        String strB = "";
        int maxCharPerLine = 27;
        int paddingLen = 0;
        List<Acquirer> acqLists = null;

        acqLists = FinancialApplication.getAcqManager().findCountEnableAcquirersWithERMStatus(true);
        if (acqLists.size() > 0) {
            page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText("------ERM ENABLED UPLOAD LIST------"));
            for (Acquirer acquirer : acqLists) {
                if (!acquirer.getName().equals(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE) && !acquirer.getName().equals(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)) {
                    sequentNo += 1;
                    HostID = EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()), 2, " ", Convert.EPaddingPosition.PADDING_LEFT);
                    HostName = acquirer.getName();
                    HostUploadActive = ((acquirer.getEnableUploadERM()) ? "ON" : "OFF");
                    strA = sequentNo + "." + HostName;
                    strB = "";//"  : " +  HostUploadActive;

                    status = strA + strB;
                    page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("\t\t" + status));
                }
            }
        }


        acqLists = FinancialApplication.getAcqManager().findCountEnableAcquirersWithERMStatus(false);
        if (acqLists.size() > 0) {
            page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText("-----ERM DISABLE UPLOAD LIST-----"));
            sequentNo = 0;        // reset sequent
            status = "";
            for (Acquirer acquirer : acqLists) {
                if (!acquirer.getName().equals(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE) && !acquirer.getName().equals(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)) {
                    sequentNo += 1;
                    HostID = EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()), 2, " ", Convert.EPaddingPosition.PADDING_LEFT);
                    HostName = acquirer.getName();
                    HostUploadActive = ((acquirer.getEnableUploadERM()) ? "ON" : "OFF");
                    strA = sequentNo + "." + HostName;
                    strB = "";//"  : " +  HostUploadActive;

                    status = strA + strB;
                    page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("\t\t" + status));
                }
            }
        }

        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_SMALL_18).setText(" \n"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" > EDC SHOW/HIDE MENU"));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setText(getString(R.string.receipt_double_line)));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  1. SALE ALL CREDIT\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SALE_CREDIT_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  2. SALE ALIPAY\t\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  3. SALE WECHAT\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WECHAT_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  4. SALE THAIQR\t\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KPLUS_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  5. SALE QRCREDIT\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_CREDIT_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  6. SALE DOLFIN\t\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  7. SALE SMATPAY\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SMART_PAY_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  8. SALE POINT REWARD\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_REDEEM_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText("  9. SALE SCB IPP\t\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SCB_IPP_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" 10. SALE SCB REDEEM\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SCB_REDEEM_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" 11. SALE AMEX EPP\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_AMEX_EPP_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" 12. SALE CT1 EPP\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CT1_EPP_MENU))));
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_SMALL_18).setText(" 13. VOID\t\t\t\t\t\t\t\t\t= " + castEnableDisable(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_MENU))));
        page.addLine().addUnit(page.createUnit().setText(" "));
        return page;
    }

    private String castEnableDisable(Boolean val) {
        return (val) ? " SHOW" : " HIDE";
    }

    private String isNullCheck(String input) {
        if (input == null) {
            return "-";
        }
        return input;
    }

    private String NumberOfReceipt(int value) {
        switch (value) {
            case 0:
                return "disable print";
            case 1:
                return "Mer. only";
            case 2:
                return "Mer. + Cus.";
            case 3:
                return "Cus. only";
            default:
                return " ";
        }
    }

    private String getYesNo(boolean value) {
        return value ? getString(R.string.yes) : getString(R.string.no);
    }

    private String getFileUploadStatus(int result) {
        switch (result) {
            case 0:
                return "File Uploaded.";
            case 1:
                return "Upload Failed.";
            default:
                return "No File Uploaded.";
        }
    }

    private String getStatus(boolean value) {
        return value ? "Enable" : "Disable";
    }

    private String getReaderKey(String key) {
        return key.equalsIgnoreCase(getString(R.string.reader_key_test)) ? "Test" : "Production";
    }

    @Override
    protected List<IPage> generatePages(Context context) {
        return null;
    }
}
