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
package com.pax.pay.trans.model;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.edc.R;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.pack.PackBatchUp;
import com.pax.pay.trans.pack.PackBatchUpNotice;
import com.pax.pay.trans.pack.PackScanWechat;
import com.pax.pay.trans.pack.PackBayBatchUp;
import com.pax.pay.trans.pack.PackBayEcho;
import com.pax.pay.trans.pack.PackBayLoadTMK;
import com.pax.pay.trans.pack.PackBayLoadTWK;
import com.pax.pay.trans.pack.PackBayReversal;
import com.pax.pay.trans.pack.PackBaySaleVoid;
import com.pax.pay.trans.pack.PackBaySettle;
import com.pax.pay.trans.pack.PackBps;
import com.pax.pay.trans.pack.PackDccGetRateKbank;
import com.pax.pay.trans.pack.PackEReceiptSessionKeyRenewal;
import com.pax.pay.trans.pack.PackEReceiptSettleUpload;
import com.pax.pay.trans.pack.PackEReceiptTerminalRegistration;
import com.pax.pay.trans.pack.PackEReceiptUpload;
import com.pax.pay.trans.pack.PackEReceiptUploadForMultiApp;
import com.pax.pay.trans.pack.PackEcho;
import com.pax.pay.trans.pack.PackGetQR;
import com.pax.pay.trans.pack.PackGetQRAlipay;
import com.pax.pay.trans.pack.PackGetQRCredit;
import com.pax.pay.trans.pack.PackGetQRWechat;
import com.pax.pay.trans.pack.PackGetQrInfo;
import com.pax.pay.trans.pack.PackGetT1CMemberID;
import com.pax.pay.trans.pack.PackInquiryBScanC;
import com.pax.pay.trans.pack.PackInstalmentBatchUpDolfin;
import com.pax.pay.trans.pack.PackInstalmentBatchUpKbank;
import com.pax.pay.trans.pack.PackInstalmentDolfin;
import com.pax.pay.trans.pack.PackInstalmentDolfinInquiry;
import com.pax.pay.trans.pack.PackInstalmentKbank;
import com.pax.pay.trans.pack.PackInstalmentSettleKbank;
import com.pax.pay.trans.pack.PackInstalmentTcAdviceKbank;
import com.pax.pay.trans.pack.PackInstalmentDolfinVoid;
import com.pax.pay.trans.pack.PackInstalmentVoidKbank;
import com.pax.pay.trans.pack.PackIso8583;
import com.pax.pay.trans.pack.PackKBankLoadTMK;
import com.pax.pay.trans.pack.PackKBankLoadTWK;
import com.pax.pay.trans.pack.PackOfflineBat;
import com.pax.pay.trans.pack.PackOfflineTransSend;
import com.pax.pay.trans.pack.PackPreAuth;
import com.pax.pay.trans.pack.PackPreAuthorizationCancelV2;
import com.pax.pay.trans.pack.PackPreAuthorizationV1;
import com.pax.pay.trans.pack.PackPreAuthorizationV2;
import com.pax.pay.trans.pack.PackPromptpayVoid;
import com.pax.pay.trans.pack.PackQRSale;
import com.pax.pay.trans.pack.PackQRSaleAllInOne;
import com.pax.pay.trans.pack.PackQRSaleWallet;
import com.pax.pay.trans.pack.PackQrAlipaySettle;
import com.pax.pay.trans.pack.PackQrSettle;
import com.pax.pay.trans.pack.PackQrVoid;
import com.pax.pay.trans.pack.PackQrWechatSettle;
import com.pax.pay.trans.pack.PackRedeemBatchUpKbank;
import com.pax.pay.trans.pack.PackRedeemSaleKbank;
import com.pax.pay.trans.pack.PackRedeemSettleKbank;
import com.pax.pay.trans.pack.PackRedeemVoidKbank;
import com.pax.pay.trans.pack.PackRefund;
import com.pax.pay.trans.pack.PackRefundWallet;
import com.pax.pay.trans.pack.PackReversal;
import com.pax.pay.trans.pack.PackSale;
import com.pax.pay.trans.pack.PackSaleBScanC;
import com.pax.pay.trans.pack.PackSaleCompletionV1;
import com.pax.pay.trans.pack.PackSaleCompletionV2;
import com.pax.pay.trans.pack.PackSaleVoid;
import com.pax.pay.trans.pack.PackScanAlipay;
import com.pax.pay.trans.pack.PackSettle;
import com.pax.pay.trans.pack.PackSettleBScanC;
import com.pax.pay.trans.pack.PackTcAdvice;
import com.pax.pay.trans.pack.PackThaiQrVerifyPaySlip;
import com.pax.pay.trans.pack.PackUPIActTMK;
import com.pax.pay.trans.pack.PackUPILoadRSA;
import com.pax.pay.trans.pack.PackUPILoadTMK;
import com.pax.pay.trans.pack.PackUPILoadTWK;
import com.pax.pay.trans.pack.PackUpdateScriptResult;
import com.pax.pay.trans.pack.PackVerifyBScanC;
import com.pax.pay.trans.pack.PackVoidBScanC;
import com.pax.pay.trans.pack.PackVoidQrAll;
import com.pax.pay.trans.pack.PackWalletBatchUp;
import com.pax.pay.trans.pack.PackWalletGetQR;
import com.pax.pay.trans.pack.PackWalletSale;
import com.pax.pay.trans.pack.PackWalletSettle;
import com.pax.pay.trans.pack.PackWalletVoid;
import com.pax.pay.utils.Utils;

import th.co.bkkps.edc.trans.pack.PackInstalmentAmex;
import th.co.bkkps.edc.trans.pack.PackInstalmentAmexBatchUp;
import th.co.bkkps.edc.trans.pack.PackInstalmentAmexReversal;
import th.co.bkkps.edc.trans.pack.PackInstalmentAmexVoid;
import th.co.bkkps.edc.trans.pack.PackInstalmentBay;
import th.co.bkkps.edc.trans.pack.PackRedeemBay;
import th.co.bkkps.utils.Log;

import static com.pax.pay.utils.Utils.getString;

public enum ETransType {
    /*
     * 管理类
     */

    /**
     * 回响功能
     */
    ECHO("0800", "", "", "990000", "",
            getString(R.string.trans_echo), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_BAY_INSTALLMENT)) {
                return new PackBayEcho(listener);
            }
            return new PackEcho(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    SETTLE("0500", "", "", "920000", "",
            getString(R.string.trans_settle), (byte) 0x00,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_BAY_INSTALLMENT)) {
                return new PackBaySettle(listener);
            }
            return new PackSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },

    BATCH_UP("0320", "", "", "000000", "",
            getString(R.string.trans_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            switch (listener.onGetAcqName()) {
                case Constants.ACQ_BAY_INSTALLMENT:
                    return new PackBayBatchUp(listener);
                case Constants.ACQ_AMEX_EPP:
                    return new PackInstalmentAmexBatchUp(listener);
                default:
                    return new PackBatchUp(listener);
            }
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    REFUND_BAT("0320", "", "", "200000", "00",
            getString(R.string.trans_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackBatchUpNotice(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    SETTLE_END("0500", "", "", "960000", "",
            getString(R.string.trans_settle_end), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_BAY_INSTALLMENT)) {
                return new PackBaySettle(listener);
            }
            return new PackSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    TCADVICE("0320", "", "", "940000", "00",
            getString(R.string.trans_tcadvice), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackTcAdvice(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BPS_VOID("0200", "0400", "", "020000", "00",
            getString(R.string.trans_void), (byte) 0x00,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackSaleVoid(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }
    },
    BPS_QR_SALE_INQUIRY("0100", "", "", "000000", "00",
            getString(R.string.trans_qr_sale), (byte) 0x10,
            false, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSale(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BPS_QR_INQUIRY_ID("0100", "", "", "310000", "00",
            getString(R.string.trans_qr_sale_inquiry), (byte) 0x10,
            false, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSale(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    PROMPT_ADV("0220", "", "", "000000", "00", getString(R.string.trans_qr_advice), (byte) 0x10,
            false, true, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSale(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BPS_QR_READ("0200", "0400", "", "000000", "00",
            Utils.getString(R.string.trans_sale), ETransType.READ_MODE_ALL,
            true, true, true, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackBps(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }
    },
    BPS_SHOWMSG("", "", "", "", "",
            Utils.getString(R.string.trans_print_msg), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BPS_SHOWPARAM("", "", "", "", "",
            Utils.getString(R.string.trans_print_param), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BPS_REPRINT("", "", "", "", "",
            Utils.getString(R.string.trans_reprint), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    TLE_STATUS("", "", "", "", "",
            Utils.getString(R.string.trans_tle_status), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    UPI_STATUS("", "", "", "", "",
            Utils.getString(R.string.trans_upi_status), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    CLEAR_KEY("", "", "", "", "",
            Utils.getString(R.string.trans_clear_key), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    /************************************************
     * 交易类
     ****************************************************/

    SALE("0200", "0400", "", "000000", "00",
            getString(R.string.trans_sale), ETransType.READ_MODE_ALL,
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackSale(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }
    },

    /**********************************************************************************************************/
    VOID("0200", "0400", "", "020000", "00",
            getString(R.string.trans_void), (byte) 0x00,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            switch (listener.onGetAcqName()) {
                case Constants.ACQ_BAY_INSTALLMENT:
                    return new PackBaySaleVoid(listener);
                case Constants.ACQ_AMEX_EPP:
                    return new PackInstalmentAmexVoid(listener);
                default:
                    return new PackSaleVoid(listener);
            }
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            switch (listener.onGetAcqName()) {
                case Constants.ACQ_BAY_INSTALLMENT:
                    return new PackBayReversal(listener);
                case Constants.ACQ_AMEX_EPP:
                    return new PackInstalmentAmexReversal(listener);
                default:
                    return new PackReversal(listener);
            }
        }

    },
    /**********************************************************************************************************/
    //AET-103
    REFUND("0200", "0400", "", "200000", "00",
            getString(R.string.trans_refund), (byte) (SearchMode.SWIPE | SearchMode.INSERT | SearchMode.KEYIN | SearchMode.WAVE),
            true, true, false, true, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRefund(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            //AET-42
            return new PackReversal(listener);
        }

    },
    /**********************************************************************************************************/
    ADJUST("", "", "", "000000", "",
            getString(R.string.trans_tip_adjust), (byte) 0x00,
            false, false, false, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    /**********************************************************************************************************/
    PREAUTH("0100", "0400", "", "300000", "06",
            getString(R.string.trans_preAuth), (byte) (SearchMode.SWIPE | SearchMode.INSERT),
            true, true, false, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackPreAuth(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }

    },
    /**********************************************************************************************************/
    PREAUTHORIZATION("0100", "0400", "", "384000", "06",
            getString(R.string.trans_preauthorize_sale).toUpperCase(), (byte) (SearchMode.SWIPE | SearchMode.INSERT),
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_UP)) {
                // Host V2
                return new PackPreAuthorizationV2(listener);
            } else {
                // Host V1
                return new PackPreAuthorizationV1(listener);
            }
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }

    },
    /**********************************************************************************************************/
    PREAUTHORIZATION_CANCELLATION("0100", "0400", "", "200000", "06",
            getString(R.string.trans_preauthorize_cancellation).toUpperCase(), (byte) (SearchMode.SWIPE | SearchMode.INSERT),
            true, true, false, true, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_UP)) {
                // only support pre-auth cancellation for Host V2
                return new PackPreAuthorizationCancelV2(listener);
            } else {
                // pre-auth cancellation isnot support for host V1
                return null;
            }
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }

    },
    /**********************************************************************************************************/
    SALE_COMPLETION("0220", "0400", "", "004000", "06",
            getString(R.string.trans_salecompletion).toUpperCase(), (byte) (SearchMode.SWIPE | SearchMode.INSERT),
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_UP)) {
                // only support pre-auth cancellation for Host V2
                return new PackSaleCompletionV2(listener);
            } else {
                // pre-auth cancellation isnot support for host V1
                return new PackSaleCompletionV1(listener);
            }
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackReversal(listener);
        }

    },
    /***************************************************************************************************************/
    OFFLINE_TRANS_SEND("0220", "", "", "000000", "00",
            getString(R.string.trans_offline_send), (byte) (SearchMode.SWIPE | SearchMode.INSERT | SearchMode.KEYIN),
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackOfflineTransSend(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    /***************************************************************************************************************/
    OFFLINE_TRANS_SEND_BAT("0220", "", "", "000000", "00",
            getString(R.string.trans_offline_send_bat), (byte) 0x00,
            true, true, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackOfflineBat(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    LOADTMK("0800", "", "", "970000", "",
            getString(R.string.trans_tle_load), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            Log.d("[PackIso8583.getPackager] - acquirer name = %1$s", listener.onGetAcqName());
            if (listener.onGetAcqName().equals(Constants.ACQ_BAY_INSTALLMENT) || listener.onGetAcqName().equals(Constants.ACQ_AYCAP_T1C_HOST)) {
                Log.d("\"[PackIso8583.getPackager] - Call PackBayLoadTMK");
                return new PackBayLoadTMK(listener);
            }
            Log.d("\"[PackIso8583.getPackager] - Call PackKBankLoadTMK");
            return new PackKBankLoadTMK(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    LOADTWK("0800", "", "", "920000", "",
            getString(R.string.trans_tle_logon), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            if (listener.onGetAcqName().equals(Constants.ACQ_BAY_INSTALLMENT) || listener.onGetAcqName().equals(Constants.ACQ_AYCAP_T1C_HOST)) {
                return new PackBayLoadTWK(listener);
            }
            return new PackKBankLoadTWK(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    LOAD_UPI_RSA("0800", "", "", "999999", "",
            getString(R.string.trans_upi_load), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackUPILoadRSA(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    LOAD_UPI_TMK("0800", "", "", "999999", "",
            getString(R.string.trans_upi_load), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackUPILoadTMK(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    ACT_UPI_TMK("0800", "", "", "999999", "",
            getString(R.string.trans_upi_load), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackUPIActTMK(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    LOAD_UPI_TWK("0800", "", "", "920000", "",
            getString(R.string.trans_upi_logon), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackUPILoadTWK(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    /***************************************************************************************************************/
    QR_SALE_WALLET("0200", "0400", "0201", "000000", "00",
            getString(R.string.trans_wallet_sale), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSaleWallet(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackQRSaleWallet(listener);
        }
    },


    QR_VOID_WALLET("0200", "0400", "0220", "020000", "00",
            getString(R.string.trans_wallet_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletVoid(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackWalletVoid(listener);
        }
    },

    /***************************************************************************************************************/
    REFUND_WALLET("0200", "0400", "0220", "200000", "00",
            getString(R.string.trans_qr_refund_wallet), (byte) 0x10, true, true, false, true, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRefundWallet(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRefundWallet(listener);
        }
    },
    SETTLE_WALLET("0500", "", "", "920000", "",
            getString(R.string.trans_wallet_settle), (byte) 0x00,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    BATCH_UP_WALLET("0320", "", "", "000000", "",
            getString(R.string.trans_wallet_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletBatchUp(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },
    SETTLE_END_WALLET("0500", "", "", "960000", "",
            getString(R.string.trans_wallet_settle_end), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },


    /***************************************************************************************************************/
    UPDATE_SCRIPT_RESULT("0620", "", "", "000000", "00",
            getString(R.string.trans_update_script), (byte) 0x00,
            false, true, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackUpdateScriptResult(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }

    },

    /***************************************************************************************************************/
    PROMPTPAY_VOID("0200", "0400", "0220", "020000", "00",
            getString(R.string.trans_qr_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackPromptpayVoid(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackPromptpayVoid(listener);
        }
    },

    /***************************************************************************************************************/
    GET_QR_INFO("0800", "", "", "000000", "00",
            getString(R.string.trans_get_qr_info), (byte) 0x00,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQrInfo(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    /***************************************************************************************************************/
    QR_SALE_ALL_IN_ONE("0100", "0400", "", "000000", "00",
            getString(R.string.trans_qr_sale), (byte) 0x10, false, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSaleAllInOne(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackQRSaleAllInOne(listener);
        }
    },

    /***************************************************************************************************************/
    QR_VOID("0200", "0400", "0220", "020000", "00",
            getString(R.string.trans_qr_void), (byte) 0x10,
            false, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQrVoid(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackQrVoid(listener);
        }
    },

    /***************************************************************************************************************/
    STATUS_INQUIRY_ALL_IN_ONE("0100", "0400", "", "310000", "00",
            getString(R.string.trans_qr_sale_inquiry), (byte) 0x10,
            true, false, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQRSaleAllInOne(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackQRSaleAllInOne(listener);
        }
    },

    /***************************************************************************************************************/
    GET_QR_WALLET("0800", "0400", "", "000000", "00",
            getString(R.string.trans_get_qr_info), (byte) 0x00,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletGetQR(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackWalletGetQR(listener);
        }
    },

    SALE_WALLET("0200", "0400", "0201", "000000", "00",
            getString(R.string.trans_wallet_sale), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackWalletSale(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackWalletSale(listener);
        }
    },

    /**************************
     * Wallet KBANK test
     *************************/
    /***************************************************************************************************************/
    //Promptpay
    GET_QR_KPLUS("0800", " ", " ", "830000", "00",
            getString(R.string.trans_kplus_sale), (byte) 0x10,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQR(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQR(listener);  //*/ return null;
        }
    },

    QR_INQUIRY("0800", "0400", "0800", "800000", "00",
            getString(R.string.trans_kplus_sale), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQR(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQR(listener);
        }
    },

    QR_VERIFY_PAY_SLIP("0800", "0400", "0800", "400000", "00",
            getString(R.string.trans_kplus_verify_pay_slip), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) { return new PackThaiQrVerifyPaySlip(listener); }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQR(listener);
        }
    },

    QR_KPLUS_SETTLE("0500", "", "", "920000", "",
            getString(R.string.trans_kplus_settle), (byte) 0x10,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQrSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    //Wechat
    GET_QR_WECHAT("0800", "0400", " ", "830000", "00",
            getString(R.string.trans_wechat_sale), (byte) 0x10,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRWechat(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRWechat(listener);
        }
    },

    QR_INQUIRY_WECHAT("0800", "0400", "0800", "800000", "00",
            getString(R.string.trans_wechat_sale), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRWechat(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRWechat(listener);
        }
    },

    QR_WECHAT_SETTLE("0500", "", "", "920000", "",
            getString(R.string.trans_wechat_settle), (byte) 0x10,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQrWechatSettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    //Alipay
    GET_QR_ALIPAY("0800", "0400", " ", "830000", "00",
            getString(R.string.trans_alipay_sale), (byte) 0x10,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRAlipay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRAlipay(listener);
        }
    },

    QR_INQUIRY_ALIPAY("0800", "0400", "0800", "800000", "00",
            getString(R.string.trans_alipay_sale), (byte) 0x10,
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRAlipay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRAlipay(listener);
        }
    },

    QR_ALIPAY_SETTLE("0500", "", "", "920000", "",
            getString(R.string.trans_alipay_settle), (byte) 0x10,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQrAlipaySettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    //add by minson
    //B SCAN C
    QR_ALIPAY_SCAN("0200", "0400", "", "000000", "",
            getString(R.string.trans_alipay_scan), (byte) 0x10,
            true, true, true, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackScanAlipay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackScanAlipay(listener);
        }
    },

    QR_WECHAT_SCAN("0200", "0400", "", "000000", "",
            getString(R.string.trans_wechat_scan), (byte) 0x10,
            true, true, true, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackScanWechat(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackScanWechat(listener);
        }
    },
    //add end

    //QR Credit Card
    GET_QR_CREDIT("0800", "0400", " ", "830000", "00",
            getString(R.string.trans_qr_credit), (byte) 0x10,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRCredit(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRCredit(listener);
        }
    },

    QR_INQUIRY_CREDIT("0800", "0400", "0800", "800000", "00",
            getString(R.string.trans_qr_credit), (byte) 0x10,
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetQRCredit(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackGetQRCredit(listener);
        }
    },

    QR_CREDIT_SETTLE("0500", "", "", "920000", "",
            getString(R.string.trans_qr_credit_settle), (byte) 0x10,
            true, true, true, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackQrAlipaySettle(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    QR_VOID_KPLUS("0200", "0400", " ", "020000", "00",
            getString(R.string.trans_kplus_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }
    },

    QR_VOID_ALIPAY("0200", "0400", " ", "020000", "00",
            getString(R.string.trans_alipay_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }
    },

    QR_VOID_WECHAT("0200", "0400", " ", "020000", "00",
            getString(R.string.trans_wechat_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }
    },

    QR_VOID_CREDIT("0200", "0400", " ", "020000", "00",
            getString(R.string.trans_qr_credit_void), (byte) 0x10,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }
    },


    GET_PAN("0200", "0400", " ", "020000", "00",
            getString(R.string.trans_get_pan), (byte) 0x10,
            true, true, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackVoidQrAll(listener);
        }
    },

    GET_T1C_MEMBER_ID("0100", "", " ", "310000", "00",
            getString(R.string.trans_get_t1c_member_id), (byte) 0x10,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackGetT1CMemberID(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_REDEEM_PRODUCT("0200", "0400", "", "004000", "00",
            getString(R.string.trans_kbank_redeem_product), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }
    },

    KBANK_REDEEM_PRODUCT_CREDIT("0200", "0400", "", "004000", "00",
            getString(R.string.trans_kbank_redeem_product_credit), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }
    },

    KBANK_REDEEM_VOUCHER("0200", "0400", "", "004000", "00",
            getString(R.string.trans_kbank_redeem_voucher), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }
    },

    KBANK_REDEEM_VOUCHER_CREDIT("0200", "0400", "", "004000", "00",
            getString(R.string.trans_kbank_redeem_voucher_credit), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }
    },

    KBANK_REDEEM_DISCOUNT("0200", "0400", "", "004000", "00",
            getString(R.string.trans_kbank_redeem_discount), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, false, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }
    },

    KBANK_REDEEM_INQUIRY("0100", "", "", "314000", "00",
            getString(R.string.trans_kbank_redeem_inquiry), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            false, false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSaleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_REDEEM_VOID("0200", "0400", "", "024000", "00",
            getString(R.string.trans_kbank_redeem_void), (byte) 0x00,
            true, false, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemVoidKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackRedeemVoidKbank(listener);
        }
    },

    KBANK_REDEEM_SETTLE("0500", "", "", "920000", "00",
            getString(R.string.trans_kbank_redeem_settle), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSettleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_REDEEM_BATCH_UP("0320", "", "", "", "00",
            getString(R.string.trans_kbank_redeem_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemBatchUpKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_REDEEM_SETTLE_END("0500", "", "", "960000", "00",
            getString(R.string.trans_kbank_redeem_settle_end), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemSettleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_SMART_PAY("0200", "0400", "", "004000", "00",
            getString(R.string.trans_instalment), (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN),
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackInstalmentKbank(listener);
        }
    },

    KBANK_SMART_PAY_VOID("0200", "0400", "", "024000", "00",
            getString(R.string.trans_instalment_void), (byte) 0x00,
            true, true, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentVoidKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackInstalmentVoidKbank(listener);
        }
    },

    KBANK_SMART_PAY_TCADVICE("0320", "", "", "944000", "00",
            getString(R.string.trans_tcadvice), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentTcAdviceKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_SMART_PAY_SETTLE("0500", "", "", "924000", "00",
            getString(R.string.trans_instalment_settle), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentSettleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_SMART_PAY_BATCH_UP("0320", "", "", "", "00",
            getString(R.string.trans_instalment_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentBatchUpKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_SMART_PAY_SETTLE_END("0500", "", "", "964000", "00",
            getString(R.string.trans_instalment_settle_end), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentSettleKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    DOLFIN_INSTALMENT("0200", "0400", "", "210000", "00",
            getString(R.string.trans_dolfin_instalment), (byte) (SearchMode.SP200),
            true, true, false, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) { return new PackInstalmentDolfin(listener); }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) { return null; }
    },

    DOLFIN_INSTALMENT_INQUIRY("0800", "0400", "", "810000", "00",
            getString(R.string.trans_dolfin_instalment), (byte) 0,
            true, false, false, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) { return new PackInstalmentDolfinInquiry(listener); }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) { return null; }
    },

    DOLFIN_INSTALMENT_VOID("0200", "0400", "", "220000", "00",
            getString(R.string.trans_void), (byte) 0x00,
            true, false, false, false, true, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) { return new PackInstalmentDolfinVoid(listener);}

        @Override
        public PackIso8583 getDupPackager(PackListener listener) { return new PackInstalmentDolfinVoid(listener); }
    },

    DOLFIN_INSTALMENT_BATCH_UP("0320", "", "", "", "00",
            getString(R.string.trans_dolfin_instalment_batch_up), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) { return new PackInstalmentBatchUpDolfin(listener); }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    KBANK_DCC_GET_RATE("0800", "", "", "000000", "00",
            getString(R.string.trans_dcc_get_rate), (byte) ETransType.SALE_READ_MODE,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackDccGetRateKbank(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    // VERIFONE-ERM
    /***************************************************************************************************************/
    ERCEIPT_TERMINAL_REGISTRATION("0001", "", "", "", "00",
            getString(R.string.listener_verifone_erm_ereceipt_initial), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptTerminalRegistration(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    /***************************************************************************************************************/
    ERCEIPT_SESSIONKEY_RENEWAL("0801", "", "", "920000", "00",
            getString(R.string.listener_verifone_erm_sessionkey_renewal_processing), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptSessionKeyRenewal(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    ERCEIPT_UPLOAD("0201", "", "", "", "",
            getString(R.string.trans_ereceipt_upload), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptUpload(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    ERCEIPT_UPLOAD_FOR_MULTI_APP("0201", "", "", "", "",
            getString(R.string.trans_ereceipt_upload), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptUploadForMultiApp(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    ERCEIPT_SETTLE_UPLOAD("0501", "", "", "", "",
            getString(R.string.trans_ereceipt_settle_upload), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptSettleUpload(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    ERCEIPT_TERM_ALERT("0801", "", "", "900000", "",
            getString(R.string.trans_ereceipt_term_alert), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptSettleUpload(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    BAY_INSTALMENT("0200", "0400", "", "000000", "00",
            getString(R.string.trans_instalment_bay), ETransType.READ_MODE_ALL,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentBay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackBayReversal(listener);
        }
    },
    BAY_REDEEM_FREEDOM("0200", "0400", "", "000000", "00",
            getString(R.string.trans_instalment_bay), ETransType.READ_MODE_ALL,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemBay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackBayReversal(listener);
        }
    },
    BAY_REDEEM_CATALOGUE("0200", "0400", "", "000000", "00",
            getString(R.string.trans_instalment_bay), ETransType.READ_MODE_ALL,
            true, true, false, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackRedeemBay(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackBayReversal(listener);
        }
    },
    AMEX_INSTALMENT("0200", "0400", "", "000000", "00",
            getString(R.string.trans_sale), ETransType.READ_MODE_ALL,
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInstalmentAmex(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return new PackInstalmentAmexReversal(listener);
        }
    },
    ERM_MULTI_APP_UPLOAD ("", "", "", "", "",
            getString(R.string.trans_erm_multi_app_upload), (byte) 0x00,
            false, false, false, false, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackEReceiptUploadForMultiApp(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    KCHECKID_DUMMY("0000", "0000", "", "000000", "00",
            getString(R.string.trans_kcheckid), ETransType.READ_MODE_ALL,
            false, false, false, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    DUMMY("0000", "0000", "", "000000", "00",
            getString(R.string.trans_kcheckid), ETransType.READ_MODE_ALL,
            false, false, false, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    QR_MYPROMPT_SALE("0200", "0400", "", "010000", "",
                  Utils.getString(R.string.trans_sale), (byte) 0x00,
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackSaleBScanC(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    QR_MYPROMPT_INQUIRY("0800", "", "", "030000", "",
                     Utils.getString(R.string.trans_sale), (byte) 0x00,
            false, true, true, false, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackInquiryBScanC(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    QR_MYPROMPT_VERIFY("0800", "", "", "040000", "",
                    Utils.getString(R.string.trans_sale), (byte) 0x00,
            false, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVerifyBScanC(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },
    QR_MYPROMPT_VOID("0200", "", "", "020000", "",
                  Utils.getString(R.string.trans_void), (byte) 0x00,
            true, true, true, true, false, true) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackVoidBScanC(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },

    SETTLE_MYPROMPT("0500", "", "", "050000", "",
                    Utils.getString(R.string.trans_settle), (byte) 0x00,
            true, true, true, true, false) {
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return new PackSettleBScanC(listener);
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    },    
    DOLFIN_SALE("0100", "0400", "", "770000", "00",
            getString(R.string.trans_sale), (byte) 0x10,
            true, false, false, true, false){
        @Override
        public PackIso8583 getPackager(PackListener listener) {
            return null;
        }

        @Override
        public PackIso8583 getDupPackager(PackListener listener) {
            return null;
        }
    };

    private static final byte SALE_READ_MODE = (byte) (SearchMode.SWIPE | SearchMode.INSERT | SearchMode.WAVE | SearchMode.KEYIN);
    private static final byte READ_MODE_ALL = (byte) (SearchMode.SWIPE | SearchMode.INSERT | SearchMode.WAVE | SearchMode.KEYIN | SearchMode.QR);

    private String msgType;
    private String dupMsgType;
    private String retryChkMsgType;
    private String procCode;
    private String serviceCode;
    private String transName;
    private byte readMode;
    private boolean isDupSendAllowed;
    private boolean isScriptSendAllowed;
    private boolean isAdjustAllowed;
    private boolean isVoidAllowed;
    private boolean isSymbolNegative;
    private boolean requireErmUpload;

    /**
     * @param msgType             ：消息类型码
     * @param dupMsgType          : 冲正消息类型码
     * @param procCode            : 处理码
     * @param serviceCode         ：服务码
     * @param readMode            : read mode
     * @param transName           : 交易名称
     * @param isDupSendAllowed    ：是否冲正上送
     * @param isScriptSendAllowed ：是否脚本结果上送
     * @param isAdjustAllowed     ：is allowed to adjust
     * @param isVoidAllowed       : is allowed to void
     * @param isSymbolNegative    : is symbol negative
     */
    ETransType(String msgType, String dupMsgType, String retryChkMsgType, String procCode, String serviceCode,
               String transName, byte readMode, boolean isDupSendAllowed, boolean isScriptSendAllowed,
               boolean isAdjustAllowed, boolean isVoidAllowed, boolean isSymbolNegative) {
        setTransTypeDetail(msgType, dupMsgType, retryChkMsgType, procCode, serviceCode, transName, readMode, isDupSendAllowed, isScriptSendAllowed, isAdjustAllowed, isVoidAllowed, isSymbolNegative, false);
    }

    ETransType(String msgType, String dupMsgType, String retryChkMsgType, String procCode, String serviceCode,
               String transName, byte readMode, boolean isDupSendAllowed, boolean isScriptSendAllowed,
               boolean isAdjustAllowed, boolean isVoidAllowed, boolean isSymbolNegative, boolean reqErmUpload) {
        setTransTypeDetail(msgType, dupMsgType, retryChkMsgType, procCode, serviceCode, transName, readMode, isDupSendAllowed, isScriptSendAllowed, isAdjustAllowed, isVoidAllowed, isSymbolNegative, reqErmUpload);
    }

    private void setTransTypeDetail(String msgType, String dupMsgType, String retryChkMsgType, String procCode, String serviceCode,
                                    String transName, byte readMode, boolean isDupSendAllowed, boolean isScriptSendAllowed,
                                    boolean isAdjustAllowed, boolean isVoidAllowed, boolean isSymbolNegative, boolean reqErmUpload) {
        this.msgType = msgType;
        this.dupMsgType = dupMsgType;
        this.retryChkMsgType = retryChkMsgType;
        this.procCode = procCode;
        this.serviceCode = serviceCode;
        this.transName = transName;
        this.readMode = readMode;
        this.isDupSendAllowed = isDupSendAllowed;
        this.isScriptSendAllowed = isScriptSendAllowed;
        this.isAdjustAllowed = isAdjustAllowed;
        this.isVoidAllowed = isVoidAllowed;
        this.isSymbolNegative = isSymbolNegative;
        this.requireErmUpload = reqErmUpload;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getDupMsgType() {
        return dupMsgType;
    }

    public String getRetryChkMsgType() {
        return retryChkMsgType;
    }

    public String getProcCode() {
        return procCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getTransName() {
        return transName;
    }

    public byte getReadMode() {
        return readMode;
    }

    public boolean isDupSendAllowed() {
        return isDupSendAllowed;
    }

    public boolean isScriptSendAllowed() {
        return isScriptSendAllowed;
    }

    public boolean isAdjustAllowed() {
        return isAdjustAllowed;
    }

    public boolean isVoidAllowed() {
        return isVoidAllowed;
    }

    public boolean isSymbolNegative() {
        return isSymbolNegative;
    }

    public abstract PackIso8583 getPackager(PackListener listener);

    public abstract PackIso8583 getDupPackager(PackListener listener);

    public boolean getRequireErmUpload() {return requireErmUpload;}
}