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
package com.pax.pay.constant;

import com.pax.gl.impl.IRgbToMonoAlgorithm;
import com.pax.pay.app.FinancialApplication;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * constants
 */
public class Constants {
    public static final Boolean isTOPS = true;
    /**
     * the period of showing dialog of successful transaction, unit: second
     */
    public static final int SUCCESS_DIALOG_SHOW_TIME = 2;
    /**
     * the period of showing dialog of failed transaction, unit: second
     */
    public static final int FAILED_DIALOG_SHOW_TIME = 7;// EDCBBLAND-395 Adjust time out error message to 7 sec.

    /**
     * MAX key index
     */
    public static final byte INDEX_TAK = 0x01;
    /**
     * PIN key index
     */
    public static final byte INDEX_TPK = 0x03;
    /**
     * DES key index
     */
    public static final byte INDEX_TDK = 0x05;

    /**
     * SSL cert
     */
    public static final String CACERT_PATH = FinancialApplication.getApp().getFilesDir() + File.separator + "cacert.pem";

    /**
     * name of printer font
     */
    public static final String FONT_NAME = "Roboto-Regular.ttf";
    public static final String FONT_NAME_MONO = "RobotoMono-Regular.ttf";
    public static final String FONT_NAME_TAHOMA_BOLD = "Tahoma-Bold.ttf";
    public static final String FONT_NAME_TAHOMA = "Tahoma-Regular.ttf";
    /**
     * date pattern of storage
     */
    public static final String TIME_PATTERN_TRANS = "yyyyMMddHHmmss";
    public static final String TIME_PATTERN_TRANS2 = "yyMMddHHmmss";
    public static final String TIME_PATTERN_TRANS3 = "HHmmssSSS";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATE_PATTERN_01 = "yyyyMMdd";
    public static final String TIME_PATTERN = "HH:mm";

    /**
     * date pattern of display
     */
    public static final String TIME_PATTERN_DISPLAY = "yyyy/MM/dd HH:mm:ss";

    public static final String TIME_PATTERN_DISPLAY2 = "MMM d, yyyy HH:mm";

    public static final String TIME_PATTERN_DISPLAY3 = "MMM d, yyyy HH:mm:ss";

    public static final String TIME_PATTERN_DISPLAY4 = "HH:mm:ss";

    public static final String DATE_PATTERN_DISPLAY = "MMM d, yyyy";

    public static final String DATE_PATTERN_DISPLAY2 = "dd";

    public static final String DATE_PATTERN_DISPLAY3 = "dd MMM yy";

    public static final String DATE_PATTERN_DISPLAY4 = "MMM d, yy";

    public static final String S66_HH_DD_DISPLAY = "66HHdd";


    /**
     * max of amount digit
     */
    public static final int AMOUNT_DIGIT = 12;

    public static final String ACQUIRER_NAME = "acquirer_name";



    public static final String PROMPT_SALT = "fc2dbf51dd6ba58f23e36f8006758f9f2f0c6348707fb38a0f8efca65273722fdebb412de9b95a5c43287d10a20b5db42cf97dfe9c12d82b4855a9f15cad241c";

    // KBANK CARD PAYMENT ACQUIRER
    public static final String ACQ_KBANK                = "KBANK";
    public static final String ACQ_UP                   = "TPN";
    public static final String ACQ_DCC                  = "DCC";
    public static final String ACQ_SMRTPAY              = "SMRTPAY";                            // INSTALMENT
    public static final String ACQ_SMRTPAY_BDMS         = "SMRTPAY_BDMS";                       // INSTALMENT
    public static final String ACQ_REDEEM               = "REDEEM";                             // REDEMPTION
    public static final String ACQ_REDEEM_BDMS          = "REDEEM_BDMS";                        // REDEMPTION

    // KBANK QR PAYMENT ACQUIRER
    public static final String ACQ_KPLUS                = "THAI QR";                            // C-SCAN-B
    public static final String ACQ_MY_PROMPT            = "MY PROMPT";                          // B-SCAN-C
    public static final String ACQ_ALIPAY               = "ALIPAY";                             // C-SCAN-B
    public static final String ACQ_ALIPAY_B_SCAN_C      = "ALIPAY_B_SCAN_C";                    // B-SCAN-C
    public static final String ACQ_WECHAT               = "WECHAT";                             // C-SCAN-B
    public static final String ACQ_WECHAT_B_SCAN_C      = "WECHAT_B_SCAN_C";                    // B-SCAN-C
    public static final String ACQ_QR_CREDIT            = "QR_CREDIT";                          // C-SCAN-B

    // KBANK E-KYC ACQUIRER
    public static final String ACQ_KCHECKID = "K_CHECK_ID";

    // KBANK's PARTNER ACQUIRER
    public static final String ACQ_AMEX                 = "AMEX";                               // AMEX CHANG-THAI
    public static final String ACQ_AMEX_EPP             = "AMEX_EPP";                           // ** AMEX INSTALMENT **
    public static final String ACQ_BAY_INSTALLMENT      = "BAY_INSTALLMENT";                    // KRUNG-SRI FINANCIAL CONSUMER
    public static final String ACQ_DOLFIN               = "DOLFIN";                             // CENTRAL-JD WALLET
    public static final String ACQ_DOLFIN_INSTALMENT    = "DOLFIN-IPP";                         // CENTRAL-JD INSTALMENT SUPPORT K-EXPRESS CASH on DOLFIN APP
    public static final String ACQ_SCB_IPP              = "SCB_IPP";                            // SCB INSTALMENT
    public static final String ACQ_SCB_REDEEM           = "SCB_REDEEM";                         // SCB REDEMPTION


    //TODO: KiTty
    // Remove unuse
    public static final String ACQ_KBANK_BDMS = "BDMS";
    public static final String ACQ_WALLET = "WALLET";
    public static final String ISSUER_WALLET = "WALLET";
    public static final String ACQ_QR_PROMPT = "QR_PROMPT";
    public static final String ACQ_QRC = "QRC_HOST";
    public static final String ISSUER_PROMTPAY = "QR PROMPTPAY";

    public static final String ISSUER_UP = "UnionPay";
    public static final String ISSUER_JCB = "JCB-CARD";
    public static final String ISSUER_TBA = "TPN-CARD";
    public static final String ISSUER_BRAND_TBA = "TPN";
    public static final String ISSUER_QRC = "QRSALE";
    public static final String ISSUER_QRCREDIT = "QR CREDIT";

    public static final String ISSUER_VISA = "VISA-CARD";
    public static final String ISSUER_VISA_BDMS = "VISA-BDMS";
    public static final String ISSUER_MASTER = "MASTERCARD";
    public static final String ISSUER_AMEX = "AMEX-CARD";
	public static final String ISSUER_MY_PROMPT = "MY PROMPT";

    //KBANK
    public static final String ISSUER_KPLUS = "KPLUS";
    public static final String ISSUER_ALIPAY = "ALIPAY";
    public static final String ISSUER_WECHAT = "WECHAT";
    public static final String ISSUER_KPLUS_PROMPYPAY = "PROMPTPAY";

    public static final String ISSUER_QRC_VISA = "QRC_VISA";
    public static final String ISSUER_QRC_MASTERCARD = "QRC_MASTERCARD";
    public static final String ISSUER_QRC_UNIONPAY = "QRC_UNIONPAY";

    public static final String ISSUER_DOLFIN = "DOLFIN";
    public static final String ISSUER_SCB_IPP = "SCB_IPP";
    public static final String ISSUER_SCB_REDEEM = "SCB_REDEEM";

    public static final String ECR_QR_PAYMENT = "QR PAYMENT";
    public static final String ECR_SALE = "Sale";
    public static final String ECR_QR_SALE = "Promptpay";
    public static final String ECR_QR_VISA = "QR Sale";
    public static final String ECR_WALLET = "Wallet";
    public static final String ECR_DOLFIN = "Dolfin(B-Scan-C)";
	public static final String ECR_DOLFIN_C_SCAN_B = "Dolfin(C-Scan-B)";

    public static final List<String> listAllPaymentAcq = Arrays.asList(Constants.ACQ_KBANK,
            ACQ_KPLUS,
            ACQ_UP,
            ACQ_ALIPAY,
            ACQ_WECHAT,
            ACQ_QR_CREDIT,
            ACQ_AMEX,
            ACQ_DCC,
            ACQ_SMRTPAY,
            ACQ_REDEEM,
            ACQ_DOLFIN,
            ACQ_SCB_IPP,
            ACQ_SCB_REDEEM,
            ACQ_KCHECKID
            );

    //VERIFONE-ERM
    public static final String ACQ_ERCM_KEY_MANAGEMENT_SERVICE      = "ERCM_KMS";
    public static final String ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE  = "ERCM_RMS";

    /**
     * App Store.
     */
    public static final String ACQ_PATH             = "acquirer_param.xml";
    public static final String ISSUER_PATH          = "issuer_param.xml";
    public static final String CARD_RANGE_PATH      = "cardtype_param.xml";
    public static final String RELATION_PATH        = "relation_param.xml";
    public static final String BUILD_TYPE_RELEASE   = "release";

    /**
     * Param Download
     */
    public static final String DOWNLOAD_PARAM_FILE_NAME = "sys_param.p";

    public static final String DN_PARAM_MER_NAME = "sys.reciept.merchant.name";
    public static final String DN_PARAM_MER_ADDR = "sys.reciept.merchant.addr";
    public static final String DN_PARAM_MER_ADDR1 = "sys.reciept.merchant.addr1";
    public static final String DN_PARAM_SLIP_LOGO = "sys.reciept.logo";
    public static final String DN_PARAM_E_SIGNATURE = "sys.esignature.enable";
    public static final String DN_PARAM_UPI_FORCE_APP_AUTO_SEL_FOR_DUO_BRAND = "sys.kbank.upi.force.app.auto.selection.duo.brand";
    public static final String DN_PARAM_GRANDTOTAL = "sys.grandtotal";
    public static final String DN_PARAM_LINKPOS_KERRYAPI = "sys.linkpos.kerryapi";
    public static final String DN_PARAM_WALLET_C_SCAN_B = "sys.wallet.c.scan.b";
    public static final String DN_PARAM_QR_BARCODE = "sys.qr.barcode.flag";
    public static final String DN_PARAM_KIOSK_MODE = "sys.kiosk.mode";
    public static final String DN_PARAM_KIOSK_TIMEOUT = "sys.kiosk.timeout";
    public static final String DN_PARAM_EDC_RECEIPT_NUM = "sys.receipt.num";
    public static final String DN_PARAM_SETTLEMENT_RECEIPT_NUM = "sys.settlement.receipt.num";
    public static final String DN_PARAM_AUDITREPORT_RECEIPT_NUM = "sys.auditreport.receipt.num";
    public static final String DN_PARAM_LINKPOS_BYPASS_CONFIRM_VOID = "sys.linkpos.bypass.confirm.void";
    public static final String DN_PARAM_LINKPOS_BYPASS_CONFIRM_SETTLE = "sys.linkpos.bypass.confirm.settle";
    public static final String DN_PARAM_EDC_CTLS_TRANS_LIMIT = "sys.ctls.trans.limit";
    public static final String DN_PARAM_EDC_MAX_AMT = "sys.max.amt";
    public static final String DN_PARAM_EDC_MIN_AMT = "sys.min.amt";
    public static final String DN_PARAM_EDC_UI_LANG = "sys.edc.lang";
    public static final String DN_PARAM_EDC_REFUND = "sys.refund.enable";
    public static final String DN_PARAM_EDC_DOUBLE_BLOCKED_TRANS_ENABLE = "sys.edc.double.blocked.trans.enable";
    public static final String DN_PARAM_QRTAG31_ENABLE = "sys.edc.qrtag31.enable";
    public static final String DN_PARAM_QRTAG31_OLD_STYLE_REPORT_ENABLE = "sys.edc.qrtag31.old.style.report.enable";
    public static final String DN_PARAM_QRTAG31_ECR_CARD_LABEL_MODE = "sys.edc..qrtag31.erc.card.label.mode";
    public static final String DN_PARAM_EDC_SETTLEMENT_MODE = "sys.settlement.mode";
    public static final String DN_PARAM_EDC_SETTLEMENT_TEST_ENBALE = "sys.settlement.test.enable";
    public static final String DN_PARAM_EDC_SETTLEMENT_TEST_TIME_INTERVAL = "sys.settlement.test.time.interval";
    public static final String DN_PARAM_EDC_PRINT_ON_EXECUTE_SETTLE = "sys.settlement.print.settle.onexecution";
    public static final String DN_PARAM_EDC_ENABLE_PREAUTH = "sys.special.feature.preauth.enable";
    public static final String DN_PARAM_EDC_PREAUTH_MAX_DAY_KEEP_TRANS = "sys.special.feature.preauth.numb.day.clear.trans";
    public static final String DN_PARAM_EDC_SALECOMP_MAX_PERCENT = "sys.special.feature.salecomp.max.pcnt";
    public static final String DN_PARAM_EDC_ENABLE_OFFLINE = "sys.special.feature.offline.enable";
    public static final String DN_PARAM_EDC_OFFLINE_PWD = "sys.special.feature.offline.password";
    public static final String DN_PARAM_EDC_ENABLE_TIP_ADJUSTMENT = "sys.special.feature.tip.adjust.enable";
    public static final String DN_PARAM_EDC_TIP_ADJUSTMENT_MAX_PERCENT = "sys.special.feature.tip.adjust.max.pcnt";
    public static final String DN_PARAM_EDC_ENABLE_REF1_REF2 = "sys.special.feature.input.reference1_2.enable";
    public static final String DN_PARAM_EDC_REF1_DISP_TEXT = "sys.special.feature.input.ref1.disp.text";
    public static final String DN_PARAM_EDC_REF2_DISP_TEXT = "sys.special.feature.input.ref2.disp.text";
    public static final String DN_PARAM_EDC_PREAUTH_PWD = "sys.special.feature.preauth.password";
    public static final String DN_PARAM_EDC_TIP_ADJUSTMENT_PWD = "sys.special.feature.tip.adjustment.password";



    public static final String DN_PARAM_ACQ_NAME = "acquirer.name";
    public static final String DN_PARAM_ACQ_TID = "acquirer.tid";
    public static final String DN_PARAM_ACQ_MID = "acquirer.mid";
    public static final String DN_PARAM_ACQ_NII = "acquirer.nii";
    public static final String DN_PARAM_ACQ_BATCH = "acquirer.batch";
    public static final String DN_PARAM_ACQ_IP = "acquirer.ip";
    public static final String DN_PARAM_ACQ_PORT = "acquirer.port";
    public static final String DN_PARAM_ACQ_IP_2ND = "acquirer.ip2nd";
    public static final String DN_PARAM_ACQ_PORT_2ND = "acquirer.port2nd";
    public static final String DN_PARAM_ACQ_IP_3RD = "acquirer.ip3rd";
    public static final String DN_PARAM_ACQ_PORT_3RD = "acquirer.port3rd";
    public static final String DN_PARAM_ACQ_IP_4TH = "acquirer.ip4th";
    public static final String DN_PARAM_ACQ_PORT_4TH = "acquirer.port4th";
    public static final String DN_PARAM_ACQ_TEST_MODE = "acquirer.test.mode";
    public static final String DN_PARAM_ACQ_ENABLE = "acquirer.enable";
    public static final String DN_PARAM_ACQ_BILLER_CODE = "acquirer.biller.code";
    public static final String DN_PARAM_ACQ_BILLER_ID = "acquirer.biller.id";
    public static final String DN_PARAM_ACQ_HEADER_LOGO = "acquirer.header.logo";
    public static final String DN_PARAM_ACQ_INST_MIN_AMT = "acquirer.instalment.min.amt";
    public static final String DN_PARAM_ACQ_INST_TERMS = "acquirer.instalment.terms";
    public static final String DN_PARAM_ACQ_UPLOAD_ERM_ENABLE = "acquirer.upload.erecipt.enable";
    public static final String DN_PARAM_ACQ_FORCE_SETTLEMENT_TIME = "acquirer.force.settlement.time";
    public static final String DN_PARAM_ACQ_STORE_ID = "acquirer.store.id";
    public static final String DN_PARAM_ACQ_STORE_NAME = "acquirer.store.name";
    public static final String DN_PARAM_ACQ_TLE_BANK_NAME = "acquirer.tlebank";

    public static final String DN_PARAM_ACQ_API_DOMAIN_NAME = "acquirer.api.domainname";
    public static final String DN_PARAM_ACQ_API_PORT_NUMBER = "acquirer.api.portnumber";
    public static final String DN_PARAM_ACQ_API_HOST_NAME_CHECK = "acquirer.api.hostname.check";
    public static final String DN_PARAM_ACQ_API_CERT_CHECK = "acquirer.api.cert.check";
    public static final String DN_PARAM_ACQ_API_CONNECT_TIMEOUT = "acquirer.api.connect.timeout";
    public static final String DN_PARAM_ACQ_API_READ_TIMEOUT = "acquirer.api.read.timeout";
    public static final String DN_PARAM_ACQ_API_SCREEN_TIMEOUT = "acquirer.api.screen.timeout";
    public static final String DN_PARAM_ACQ_SSL_ENABLED = "acquirer.ssl.enabled";

    public static final String DN_PARAM_ACQ_EKYC_CHD_ENCRYPTION = "acquirer.ekyc.chd.encryption";

    public static final String DN_PARAM_ACQ_SETTLE_TIME = "acquirer.settle.time";
    public static final String DN_PARAM_ACQ_ENABLE_SIGNATURE = "acquirer.enable.signature";

    public static final String DN_PARAM_ACQ_ENABLE_CONTROL_LIMIT = "acquirer.control.limit.enabled";
    public static final String DN_PARAM_ACQ_ENABLE_PHONE_INPUT = "acquirer.phone.input.enabled";

    public static final String DN_PARAM_ACQ = "acquirer.prompt.biller.id";

    public static final String DN_PARAM_RABBIT_KEY = "sys.rabbit.key";

    public static final String SLIP_LOGO_NAME = "reciept_logo.jpg";
    public static final String DN_PARAM_SLIP_LOGO_PATH = "recieptLogoPath";//Name of logo's path saved in sharedpreference

    public static final String DN_PARAM_SMALL_AMT_ISSUERNAME = "issuer.smallamount.issuername";
    public static final String DN_PARAM_SMALL_AMT_SUPPORT = "issuer.smallamount.support";
    public static final String DN_PARAM_SMALL_AMT_AMT = "issuer.smallamount.amount";
    public static final String DN_PARAM_SMALL_AMT_RECEIPT = "issuer.smallamount.receipt";
    public static final String DN_PARAM_REFUND_ISUUERNAME = "issuer.refund.issuername";
    public static final String DN_PARAM_REFUND_SUPPORT = "issuer.refund.support";
    public static final String DN_PARAM_TIP_ADJ_ISUUERNAME = "issuer.tip.adjust.issuername";
    public static final String DN_PARAM_TIP_ADJ_SUPPORT = "issuer.tip.adjust.support";
    public static final String DN_PARAM_TIP_ADJ_PERCENT = "issuer.tip.adjust.percent";

    public static final String DN_PARAM_PROTOCOL = "sys.linkpos.protocol";
    public static final String DN_PARAM_ECR_FOR_MERCHANT = "sys.linkpos.protocol.merchant";

    public static final String DN_PARAM_SCREEN_TIMEOUT = "sys.screen.timeout";
    public static final String DN_PARAM_CAMERA = "sys.camera";
    public static final String DN_PARAM_LINKPOS_COMM_TYPE = "sys.linkpos.commtype";
    public static final String DN_PARAM_FILENAME_ISSUER = "sys.edc.config.issuer";
    public static final String DN_PARAM_FILENAME_CARD_RANGE = "sys.edc.config.cardrange";
    public static final String DN_PARAM_FILENAME_AID = "sys.edc.config.aid";

    public static final String DN_PARAM_COMM_TYPE = "sys.comm.type";
    public static final String DN_PARAM_APN_TRANS = "sys.apn.trans";
    public static final String DN_PARAM_APN_SYSTEM = "sys.apn.system";
    public static final String DN_PARAM_LAN_DHCP = "sys.lan.static";
    public static final String DN_PARAM_LAN_LOCAL_IP = "sys.lan.localip";
    public static final String DN_PARAM_LAN_SUBNET = "sys.lan.subnet";
    public static final String DN_PARAM_LAN_GATEWAY = "sys.lan.gateway";
    public static final String DN_PARAM_LAN_DNS1 = "sys.lan.dns1";
    public static final String DN_PARAM_LAN_DNS2 = "sys.lan.dns2";

    public static final String DN_PARAM_IMG_ON_RECEIPT = "sys.img.on.receipt";
    public static final String DN_PARAM_IMG_ON_RECEIPT_FILE = "sys.img.on.receipt.file";
    public static final String DN_PARAM_IMG_ON_RECEIPT_FILE_NAME = "img_on_receipt.jpg";
    public static final String DN_PARAM_IMG_ON_RECEIPT_FILE_PATH = "imgOnReceiptPath";//Name of imgage's path saved in sharedpreference

    //Contactless
    public static final String DN_PARAM_CONTACTLESS_ENABLE = "sys.contactless.enable";
    public static final String DN_PARAM_CONTACTLESS_VISA = "sys.contactless.visa";
    public static final String DN_PARAM_CONTACTLESS_MASTER = "sys.contactless.master";
    public static final String DN_PARAM_CONTACTLESS_JCB = "sys.contactless.jcb";
    public static final String DN_PARAM_CONTACTLESS_UP = "sys.contactless.up";
    public static final String DN_PARAM_CONTACTLESS_TPN = "sys.contactless.tpn";
    public static final String DN_PARAM_CONTACTLESS_AMEX = "sys.contactless.amex";

    public static final String DN_PARAM_ENABLE_KEYIN = "sys.keyin.enable";
    public static final String DN_PARAM_VOID_WITH_STAND = "sys.void.with.stand.enable";
    public static final String DN_PARAM_ENABLE_QR_BARCODE_ALIWECHAT = "sys.qr.barcode.ali.wechat.enable";
    public static final String DN_PARAM_ENABLE_B_SCAN_C_ALIWECHAT = "sys.alipay.wechat.b.scan.c.enable";
    public static final String DN_PARAM_ALIWECHAT_B_SCAN_C_NII = "sys.alipay.wechat.b.scan.c.nii";

    public static final String DN_PARAM_ERCM_PBK = "sys.ercm.pbk";
    public static final String DN_PARAM_ERCM_ENABLE = "sys.ercm.enable";
    public static final String DN_PARAM_ERCM_NEXT_TRANS_UPLOAD_ENABLE = "sys.ercm.next.trans.upload.enable";
    public static final String DN_PARAM_ERCM_FORCE_SETTLE_PRINT_ALL_ERM_PENDING_ENABLE = "sys.ercm.force.settle.print.all.erm.pending.enable";
    public static final String DN_PARAM_ERCM_PRN_AFTER_TXN = "sys.ercm.prn.after.txn";
    public static final String DN_PARAM_ERCM_PRN_PRE_SETTLE = "sys.ercm.print.pre.settle";
    public static final String DN_PARAM_ERCM_SLIP_NUM = "sys.ercm.slip.num";
    public static final String DN_PARAM_ERCM_SLIP_NUM_UNABLE_UPLOAD = "sys.ercm.slip.num.unable.upload";
    public static final String DN_PARAM_ERCM_MAX_PENDING_ERECEIPT_UPLOAD_AWAIT = "sys.ercm.max.pending.ereceipt.upload.await";
    public static final String DN_PARAM_SUPPORT_SP200 = "sys.support.sp200";
    public static final String DN_PARAM_ENABLE_QR_COD = "sys.qr.cod.enable";
    public static final String DN_PARAM_PWD_ADMIN = "sys.pwd.admin";
    public static final String DN_PARAM_TLE_PARAM= "sys.tle.param";

    public static final String DN_PARAM_DOL_SHOW_MENU ="sys.dynamic.offline.session.show.menu";
    public static final String DN_PARAM_DOL_SESSION_TIMEOUT ="sys.dynamic.offline.session.timeout";
    public static final String DN_PARAM_DOL_FLOOR_LIMIT_VSC ="sys.dynamic.offline.floorlimit.visacard";
    public static final String DN_PARAM_DOL_FLOOR_LIMIT_MCC ="sys.dynamic.offline.floorlimit.mastercard";
    public static final String DN_PARAM_DOL_FLOOR_LIMIT_JCB ="sys.dynamic.offline.floorlimit.jcbcard";

    public static final String DN_PARAM_ERCM_BANK_CODE  = "sys.ercm.bankcode";
    public static final String DN_PARAM_ERCM_MERC_CODE  = "sys.ercm.merchantcode";
    public static final String DN_PARAM_ERCM_STORE_CODE = "sys.ercm.storecode";

    public static final String DN_PARAM_ENABLE_SALE_CREDIT_MENU                 = "sys.menu.sale.credit.enable";
    public static final String DN_PARAM_ENABLE_VOID_MENU                        = "sys.menu.void.enable";
    public static final String DN_PARAM_ENABLE_KPLUS_MENU                       = "sys.menu.kplus.enable";
    public static final String DN_PARAM_ENABLE_ALIPAY_MENU                      = "sys.menu.alipay.enable";
    public static final String DN_PARAM_ENABLE_WECHAT_MENU                      = "sys.menu.wechat.enable";
    public static final String DN_PARAM_ENABLE_QR_CREDIT_MENU                   = "sys.menu.qrcredit.enable";
    public static final String DN_PARAM_ENABLE_SMART_PAY_MENU                   = "sys.menu.smartpay.enable";
    public static final String DN_PARAM_ENABLE_REDEEM_MENU                      = "sys.menu.redeem.enable";
    public static final String DN_PARAM_ENABLE_CT1_EPP_MENU                     = "sys.menu.ct1epp.enable";
    public static final String DN_PARAM_ENABLE_AMEX_EPP_MENU                    = "sys.menu.amexepp.enable";
    public static final String DN_PARAM_ENABLE_SCB_IPP_MENU                     = "sys.menu.scbipp.enable";
    public static final String DN_PARAM_ENABLE_SCB_REDEEM_MENU                  = "sys.menu.scbredeem.enable";
    public static final String DN_PARAM_ENABLE_DOLFIN_MENU                      = "sys.menu.dolfin.enable";
    public static final String DN_PARAM_ENABLE_MYPROMPT_MENU                    = "sys.menu.myPrompt.enable";
    public static final String DN_PARAM_ENABLE_DOLFIN_IPP_MENU                  = "sys.menu.dolfinipp.enable";
    public static final String DN_PARAM_ENABLE_ALIPAY_BSCANC_MENU               = "sys.menu.alipay.b.scan.c.enable";
    public static final String DN_PARAM_ENABLE_BSCANC_WECHAT_MENU               = "sys.menu.wechat.b.scan.c.enable";


    public static final String DN_PARAM_TIME_OUT_SEARCH_CARD = "sys.timeout.search.card";

    // THAI-QR EXTRA CONFIG
    public static final String DN_PARAM_MAX_NUMB_OF_INQUIRY_SHOW_VERIFY_QR_BTN  = "sys.thai.qr.max.inquiry.show.verify.qr.button";

    //Dolfin
    public static final String DN_PARAM_DOLFIN_IS_ENABLE_C_SCAN_B_MODE = "dolfin.is.enable.cscanb.mode";
    public static final String DN_PARAM_DOLFIN_C_SCAN_B_DISPLAY_QR_TIMEOUT = "dolfin.cscanb.display.qr.timeout";
    public static final String DN_PARAM_DOLFIN_C_SCAN_B_RETRY_TIMES = "dolfin.cscanb.retry.times";
    public static final String DN_PARAM_DOLFIN_C_SCAN_B_DELAY_RETRY = "dolfin.cscanb.delay.retry";

    // Multi-Merchant Parameter
    public static final String DN_PARAM_MULTI_MERC_ENABLE = "merchant.enable";
    public static final String DN_PARAM_MULTI_MERC_LABEL_NAME = "merchant.label.name";
    public static final String DN_PARAM_MULTI_MERC_PRINT_NAME = "merchant.print.name";
    public static final String DN_PARAM_MULTI_MERC_PRINT_ADDR_LN1 = "merchant.address1";
    public static final String DN_PARAM_MULTI_MERC_PRINT_ADDR_LN2 = "merchant.address2";
    public static final String DN_PARAM_MULTI_MERC_LOGO = "merchant.menu.logo";
    public static final String DN_PARAM_MULTI_MERC_SCREEN_LOGO = "merchant.screen.logo";


    /**
     *  AYCAP
     **/
    public static final String[] AYCAP_CARD_RANGE_ISSUER = {"CENTRAL-MC"};
    public static final String ACQ_AYCAP_T1C_HOST = "AYCAP_T1C";

    /**
     * default pattern of pan mask.
     */
    public static final String PAN_MASK_PATTERN1 = "(?<=\\d{4})\\d(?=\\d{6})";
    public static final String PAN_MASK_PATTERN2 = "[0-9]";
    public static final String PAN_MASK_PATTERN3 = "";
    public static final String DEF_PAN_MASK_PATTERN = "(?<=\\d{6})\\d(?=\\d{4})";
    public static final String PAN_MASK_PATTERN4 = "\\d(?=\\d{4})";
    public static final String PAN_MASK_PATTERN5 = "(?<=\\d{8})\\d(?=\\d{4})";

    public static final String AMEX_AID_PREFIX = "A000000025";
    public static final String DINERS_AID_PREFIX = "A000000152";
    public static final String TBA_AID_PREFIX = "A000000677";
    public static final String JCB_AID_PREFIX = "A000000065";
    public static final String VISA_AID_PREFIX = "A000000003";
    public static final String MASTER_AID_PREFIX = "A000000004";
    public static final String UP_AID_PREFIX = "A000000333";

    public static final String[] VISA_AID_CREDIT = new String[]{"A0000000031010", "A000000003101001"};
    public static final String[] VISA_AID_DEBIT = new String[]{"A000000003101002", "A0000000032010"};
    public static final String[] MASTER_AID_CREDIT = new String[]{"A0000000041010", "A00000000410101213", "A00000000410101215"};
    public static final String[] JCB_AID_CREDIT = new String[]{"A0000000651010"};
    public static final String[] UP_AID_CREDIT = new String[]{"A000000333010102", "A000000333010103"};
    public static final String[] UP_AID_DEBIT = new String[]{"A000000333010101"};
    public static final String[] TPN_AID_CREDIT = new String[]{"A000000677010102", "A000000677010103"};
    public static final String[] TPN_AID_DEBIT = new String[]{"A000000677010101"};

    public static final int NOTIFICATION_ID_PARAM = 999;
    //Linkpos card no prefix
    public static final String PREFIX_CARD_NO_DOLFIN = "88";

    public static final IRgbToMonoAlgorithm rgb2MonoAlgo = new IRgbToMonoAlgorithm() {
        @Override
        public int evaluate(int r, int g, int b) {
            int v = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            // set new pixel color to output bitmap
            if (v < 200) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    /**
     * Language
     */
    public static final String USER_LANG = "USER_LANG";

    private Constants() {
        //do nothing
    }
    /**
     * Dolfin
     */
    public static final String DOLFIN_ERR = "no_connection";
}
