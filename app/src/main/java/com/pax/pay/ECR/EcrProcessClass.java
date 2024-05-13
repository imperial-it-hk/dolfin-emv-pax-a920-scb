package com.pax.pay.ECR;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.AlipayQrSaleTrans;
import com.pax.pay.trans.BPSQrCodeSaleTrans;
import com.pax.pay.trans.GetPanTrans;
import com.pax.pay.trans.GetT1CMember;
import com.pax.pay.trans.KplusQrSaleTrans;
import com.pax.pay.trans.QrSaleTrans;
import com.pax.pay.trans.SaleTrans;
import com.pax.pay.trans.SaleVoidTrans;
import com.pax.pay.trans.SettleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.WalletQrSaleTrans;
import com.pax.pay.trans.WalletSaleCBTrans;
import com.pax.pay.trans.WechatQrSaleTrans;
import com.pax.pay.trans.action.ActionTransReport;
import com.pax.pay.trans.action.activity.EcrPaymentSelectActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.uart.BaseL920BMCommClass;
import com.pax.pay.uart.CommConnInterface;
import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.CommManageInterface;
import com.pax.pay.uart.ConnectionInterface;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.NotificationUtils;
import com.pax.pay.utils.Utils;
import com.pax.pay.utils.WakeLockerClass;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import th.co.bkkps.dofinAPI.tran.DolfinSaleCScanBTran;
import th.co.bkkps.dofinAPI.tran.DolfinSaleTran;
import th.co.bkkps.utils.Log;

import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.INSERT;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.KEYIN;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.QR;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.SWIPE;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.WAVE;

public class EcrProcessClass extends Thread {
    public static final String TAG = "EcrProcessClass:";
    public static boolean useLinkPos = true;
    final Semaphore mutex = new Semaphore(0);
    public CommManageClass mCommManage;
    public BaseL920BMCommClass mLinkPosComm = null;
    public HyperCommClass mHyperComm;
    public PosNetCommClass mPosNetComm;
    public ProtoFilterClass mProtoFilter;
    public AlertDialog alertDialog;
    CharSequence saleList[];
    public static Context mActContext;
    private UsbManager mUsbManager;
    private String HostID;
    private String selectSale = null;

    private EcrDismissListener ecrDismissListener;

    public void setEcrDismissListener(EcrDismissListener exListener) {
        ecrDismissListener = exListener;
    }


    public static String getLinkPosProtocolByMerchantName(String merchantName) {
        HashMap<String, String> merchantProtocolMapper = new HashMap<>();
        merchantProtocolMapper.put("Disable", "DISABLE");
        merchantProtocolMapper.put("LemonFarm", "HYPERCOM");
        merchantProtocolMapper.put("Lawson", "HYPERCOM");

        if (merchantProtocolMapper.containsKey(merchantName)) {
            return merchantProtocolMapper.get(merchantName);
        } else {
            // for other merchant name -- ECR command will disabled
            return "DISABLE";
        }
    }


    private CountDownTimer menuTimeout = new CountDownTimer(50000, 1000) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            selectSale = "";
            alertDialog.dismiss();
            mutex.release();
            Log.d("EcrProcess:", "MENU TIMEOUT OCCUR.....");
        }
    };
    private Handler mSaleSelectHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(ActivityStack.getInstance().top());

            TextView myMsg = new TextView(ActivityStack.getInstance().top());
            myMsg.setText(Html.fromHtml("<b>" + "SALE MENU" + "</b>"));
            myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
            myMsg.setTextSize(30);
            myMsg.setTextColor(Color.WHITE);
            myMsg.setBackgroundColor(Color.parseColor("#000066"));
            //set custom title
            mBuilder.setCustomTitle(myMsg)
                    .setItems(saleList, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            selectSale = saleList[which].toString();
                            dialog.dismiss();
                            mutex.release();
                            //clearTradeFunc();
                        }
                    });

            alertDialog = mBuilder.create();
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // TODO Auto-generated method stub
                    dialog.dismiss();
                    mutex.release();
                }
            });
            alertDialog.show();
        }
    };
    public HyperCommInterface HyperCommInterfaceCbk = new HyperCommInterface() {
        @Override
        public int onTransportHeaderRcv(byte[] type, int destAddr, int srcAddr) {
            Log.d("onTransportHeaderRcv : ", "SRC_ADDR = " + srcAddr + ", DEST_ADDR = " + destAddr);
            WakeLockerClass.acquire(mActContext);
            return 0;
        }

        @Override
        public int onPresentationHeaderRcv(byte formatVer, byte reqResIndicator, byte[] transactionCode, byte[] responseCode, byte moreIndicator) {
            Log.d(TAG, "formatVer = " + formatVer +
                    ", reqResIndicator = " + reqResIndicator +
                    ", transactionCode = " + String.format("%C%C", transactionCode[0], transactionCode[1]) +
                    ", responseCode = " + responseCode[0] + responseCode[1] +
                    ", moreIndicator = " + moreIndicator);

            HyperComMsg.instance.reset();

            HyperComMsg.instance.formatVer = formatVer;
            HyperComMsg.instance.reqResIndicator = reqResIndicator;
            HyperComMsg.instance.transactionCode = transactionCode.clone();
            HyperComMsg.instance.responseCode = responseCode.clone();
            HyperComMsg.instance.moreIndicator = moreIndicator;

            if (Arrays.equals(HyperCommClass.TRANSACTION_CODE_TEST_COMMUNICATION_TYPE, HyperComMsg.instance.transactionCode)) {
                Log.d(TAG, "onPreHdrRcv: Test Communication..........................");
                mHyperComm.sendTestCommResponse();
            }

            return 0;
        }

        @Override
        public int onFieldDataRcv(byte[] fieldType, byte[] data) {

            Log.d(TAG, "fieldType = " + String.format("%C%C", fieldType[0], fieldType[1]));
            if (Arrays.equals(HyperCommClass.FIELD_NULL_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_APPROVAL_CODE_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_RESPONSE_TEXT_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_TRANSACTION_DATE_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_TRANSACTION_TIME_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_MERCHANT_NUMBER_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_TERMINAL_ID_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_CARD_NUMBER_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_EXPIRED_DATE_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_TRANSACTION_TYPE, fieldType)) {
                HyperComMsg.instance.data_field_40_amount = new String(data);
            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_TIP_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_CASH_BACK_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_TAX_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_BALANCE_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_AMOUNT_BALN_POS_NEG_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_BATCH_NUMBER_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_TRACE_INVOICE_NUMBER_TYPE, fieldType)) {
                HyperComMsg.instance.data_field_65_invoiceNo = new String(data);
            } else if (Arrays.equals(HyperCommClass.FIELD_MERCHNT_NAME_AND_ADDR_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_MERCHNT_ID_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_CARD_ISSUER_NAME_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_REF_NUMBER_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_CARD_ISSUER_ID_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_ADDITIONAL_1, fieldType)) {
                HyperComMsg.instance.data_field_D6_ref2 = new String(data);
            } else if (Arrays.equals(HyperCommClass.FIELD_ADDITIONAL_2, fieldType)) {
                HyperComMsg.instance.data_field_D7_ref1 = new String(data);
            } else if (Arrays.equals(HyperCommClass.FIELD_ADDITIONAL_3, fieldType)) {
                HyperComMsg.instance.data_field_D8_branchID = new String(data);
            } else if (Arrays.equals(HyperCommClass.FIELD_BATCH_TOTAL_SALES_COUNT_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_BATCH_TOTAL_SALES_AMOUNT_TYPE, fieldType)) {

            } else if (Arrays.equals(HyperCommClass.FIELD_NII_TYPE, fieldType)) {
                HyperComMsg.instance.data_field_HN_nii = new String(data);
            }

            switch (FinancialApplication.getEcrProcess().mHyperComm.getClass().getSimpleName()) {
                case "LemonFarmHyperCommClass":
                    setLemonFarmField(fieldType, data);
                    break;
                case "LawsonHyperCommClass":
                    setLawsonField(fieldType, data);
                    break;
                default:
                    break;
            }

            return 0;
        }


        protected void setLemonFarmField(byte[] fieldType, byte[] data) {
            if (Arrays.equals(LemonFarmHyperCommClass.FIELD_REFERENCE_SALE_ID_R1, fieldType)) {
                HyperComMsg.instance.data_field_R1_ref_saleID = new String(data);
            } else if (Arrays.equals(LemonFarmHyperCommClass.FIELD_QR_TYPE, fieldType)) {
                HyperComMsg.instance.data_field_A1_qr_type = new String(data);
            }
        }

        protected void setLawsonField(byte[] fieldType, byte[] data) {
            if (Arrays.equals(LawsonHyperCommClass.FIELD_MERCHANT_NUMBER, fieldType)) {
                HyperComMsg.instance.data_field_45_merchant_number = new String(data);
            } else if (Arrays.equals(LawsonHyperCommClass.FIELD_REFERENCE_SALE_ID_R0, fieldType)) {
                HyperComMsg.instance.data_field_R0_ref_saleID = new String(data);
            }
//            else if (Arrays.equals(LawsonHyperCommClass.FIELD_PHONE_NUMBER, fieldType)) {
//                HyperComMsg.instance.data_field_R3_phone_number = new String(data);
//            }
        }

        @Override
        public int onRcvCmpt() {

            if (EcrData.instance.isOnProcessing) {
                // for disable process incoming ECR-command during execute ECR-process
                return 0;
            }
            EcrData.instance.isOnProcessing = true;

            if (FinancialApplication.getEcrProcess() != null && FinancialApplication.getEcrProcess().mCommManage != null) {
                FinancialApplication.getEcrProcess().mCommManage.StopReceive();
            }

            Device.beepPrompt();

            if (checkAmtZeroEcrProc(PROTOCOL.HYPERCOM, HyperComMsg.instance.data_field_40_amount, HyperComMsg.instance.transactionCode)) {
                return 0;
            }


            if (mHyperComm instanceof LemonFarmHyperCommClass) {
                // this for dedicate command between Lemonfarm and Existing-HyperComm LinkPos command separately
                processLinkPosCommandLemonFarm();

            } else if (mHyperComm instanceof LawsonHyperCommClass) {
                // this for dedicate command between Lemonfarm and Existing-HyperComm LinkPos command separately
                processLinkPosCommandLawson();

            } else {
                // TRANSACTION_CODE_SALE_ALL_TYPE
                if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_SALE_ALL_TYPE)) {
                    byte mode = SWIPE | INSERT | WAVE | KEYIN /*| QR*/;

                    Acquirer acquirerWallet = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
                    boolean isWalletCscanB = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B);
                    if (acquirerWallet != null && acquirerWallet.isEnable() && !isWalletCscanB) {
                        mode |= QR;
                    }

                    Acquirer acquirerPrompt = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
                    Acquirer acquirerQRC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
                    Acquirer acquirerDolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
                    if ((acquirerPrompt != null && acquirerPrompt.isEnable())
                            || (acquirerQRC != null && acquirerQRC.isEnable())
                        /*||(acquirerDolfin != null && acquirerDolfin.isEnable())*/) {
                        selectSale = "cancel";
                        menuTimeout.start();
                        Message message = Message.obtain();
                        message.what = 1;
                        mSaleSelectHandler.sendMessage(message);
                        Log.d("onDataRcv:", "Mutex Lock, selectSale = " + selectSale);

                        try {
                            mutex.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        menuTimeout.cancel();

                        Log.d("onDataRcv:", "Mutex Un-Lock, selectSale = " + selectSale);
                    } else {
                        selectSale = Constants.ECR_SALE;
                    }

                    if (selectSale.equals(Constants.ECR_SALE)) {
                        SaleTrans mSaleTrans = new SaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount,
                                mode, HyperComMsg.instance.data_field_D8_branchID, true, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                                }
                            }
                        });

                        mSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_ALL_TYPE, 0);
                                }
                            }
                        });

                        mSaleTrans.execute();
                    } else if (selectSale.equals(Constants.ECR_QR_VISA)) {
                        //QR VISA
                        QrSaleTrans mQrSaleTrans = new QrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, "00", new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE, 0);
                                }
                            }
                        });

                        mQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE, 0);
                                }
                            }
                        });

                        mQrSaleTrans.execute();
                    } else if (selectSale.equals(Constants.ECR_QR_SALE)) {
                        //QR Promptpay
                        KplusQrSaleTrans kplusQrSaleTrans = new KplusQrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE, 0);
                                }
                            }
                        });

                        kplusQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE, 0);
                                }
                            }
                        });

                        kplusQrSaleTrans.execute();

                    } else if (selectSale.equals(Constants.ECR_WALLET)) {
                        //Wallet
                        WalletSaleCBTrans mWalletSaleCBTrans = new WalletSaleCBTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.execute();

                    } else if (selectSale.equals(Constants.ECR_DOLFIN)) {
                        DolfinSaleTran mSaleTrans = new DolfinSaleTran(mActContext, HyperComMsg.instance.data_field_40_amount,
                                new ATransaction.TransEndListener() {
                                    @Override
                                    public void onEnd(ActionResult result) {
                                        Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                        if (result.getRet() != TransResult.SUCC) {
                                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_DOLFIN_TYPE, 0);
                                        }
                                    }
                                });

                        mSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_DOLFIN_TYPE, 0);
                                }
                            }
                        });

                        mSaleTrans.execute();
                    } else if(selectSale.equals(Constants.ECR_DOLFIN_C_SCAN_B)) {
                        DolfinSaleCScanBTran mSaleTrans = new DolfinSaleCScanBTran(mActContext, HyperComMsg.instance.data_field_40_amount,
                                new ATransaction.TransEndListener() {
                                    @Override
                                    public void onEnd(ActionResult result) {
                                        Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                        if (result.getRet() != TransResult.SUCC) {
                                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_DOLFIN_TYPE, 0);
                                        }
                                    }
                                });

                        mSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_DOLFIN_TYPE, 0);
                                }
                            }
                        });
                        mSaleTrans.execute();
                    } else {
                        mHyperComm.cancelSaleByUserSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                    }
                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    SaleTrans mSaltrans = new SaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, (byte) (SWIPE | INSERT | WAVE | KEYIN),
                            HyperComMsg.instance.data_field_D8_branchID, true, new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            if (result.getRet() != TransResult.SUCC) {
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                            }
                        }
                    });

                    mSaltrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                            }
                        }
                    });

                    mSaltrans.execute();
                }
                //TRANSACTION_CODE_SALE_QR_TYPE [PromptPay]
                else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    KplusQrSaleTrans kplusQrSaleTrans = new KplusQrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                            if (result.getRet() != TransResult.SUCC) {
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE, 0);
                            }
                        }
                    });

                    kplusQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE, 0);
                            }
                        }
                    });

                    kplusQrSaleTrans.execute();
                }
                //TRANSACTION_CODE_SALE_WALLET_TYPE [WALLET BSC]
                else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    boolean isWalletCscanB = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B);
                    if (isWalletCscanB) {
                        //Wallet
                        WalletSaleCBTrans mWalletSaleCBTrans = new WalletSaleCBTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.execute();
                    } else {
                        //SaleTrans mSaltrans =  new SaleTrans(mActContext, new String(data), QR, true, new ATransaction.TransEndListener(){
                        WalletQrSaleTrans walletQrSaleTrans = new WalletQrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        walletQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                                }
                            }
                        });

                        walletQrSaleTrans.execute();
                    }
                }
                // TRANSACTION_CODE_VOID_TYPE
                else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_VOID_TYPE)) {
                    Log.d(TAG, "Field Type : Trace/Invoice Number");
                    Log.d(TAG, "TRACE_NUM  = " + HyperComMsg.instance.data_field_65_invoiceNo);

                SaleVoidTrans mVoidtrans = new SaleVoidTrans(mActContext, Utils.parseLongSafe(HyperComMsg.instance.data_field_65_invoiceNo,-1), new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            if (result.getRet() != TransResult.SUCC) {
                                HyperComMsg.instance.responseCode = new byte[]{0x00, 0x00};
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_VOID_TYPE, 0);
                                return;
                            }
                        }
                    });

                    mVoidtrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.voidTransactionSendResponse(0);
                            }
                        }
                    });

                    mVoidtrans.execute();
                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_SETTLEMENT_TYPE)) {
                    Log.d(TAG, "Field Type : The host identity");
                    Log.d(TAG, "HOST_ID    = " + HyperComMsg.instance.data_field_HN_nii);

                    HostID = HyperComMsg.instance.data_field_HN_nii;

                    new SettleTrans(mActContext, false, HostID, new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            mHyperComm.settlementTransactionSendResponse(HostID, 0);
                            if (result.getRet() != TransResult.SUCC) {
                                return;
                            }
                            checkParamUpdate(mActContext);
                        }
                    }).execute();
                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    QrSaleTrans mQrSaleTrans = new QrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, "00", new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                            if (result.getRet() != TransResult.SUCC) {
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE, 0);
                            }
                        }
                    });

                    mQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE, 0);
                            }
                        }
                    });

                    mQrSaleTrans.execute();

                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_WECHAT_CSB_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    WechatQrSaleTrans mWechatQrSaleTrans = new WechatQrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                            if (result.getRet() != TransResult.SUCC) {
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_WECHAT_CSB_TYPE, 0);
                            }
                        }
                    });

                    mWechatQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_WECHAT_CSB_TYPE, 0);
                            }
                        }
                    });

                    mWechatQrSaleTrans.execute();
                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_DOLFIN_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);
                    Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
                    if(acq.isEnableCScanBMode()){
                        DolfinSaleCScanBTran dolfinSaleCScanBTran = new DolfinSaleCScanBTran(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret = " + result.getRet());
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_DOLFIN_TYPE,0);
                                }
                            }
                        } );
                        dolfinSaleCScanBTran.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret = " + result.getRet());
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_DOLFIN_TYPE, 0);
                                }
                            }
                        });
                        dolfinSaleCScanBTran.execute();
                    } else {
                        DolfinSaleTran dolfinSaleTran = new DolfinSaleTran(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret = " + result.getRet());
                                if (result.getRet() != TransResult.SUCC) {
                                    mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_DOLFIN_TYPE, 0);
                                }
                            }
                        });

                        dolfinSaleTran.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret = " + result.getRet());
                                if (result.getRet() == TransResult.SUCC) {
                                    mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_DOLFIN_TYPE, 0);
                                }
                            }
                        });

                        dolfinSaleTran.execute();
                    }
                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_ALIPAY_CSB_TYPE)) {
                    Log.d(TAG, "Field Type : Amount Transaction");
                    Log.d(TAG, "Amount     = " + Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100);

                    AlipayQrSaleTrans mAlipayQrSaleTrans = new AlipayQrSaleTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                            if (result.getRet() != TransResult.SUCC) {
                                mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_ALIPAY_CSB_TYPE, 0);
                            }
                        }
                    });

                    mAlipayQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                            if (result.getRet() == TransResult.SUCC) {
                                mHyperComm.saleTransactionSendResponse(HyperCommClass.TRANSACTION_CODE_ALIPAY_CSB_TYPE, 0);
                            }
                        }
                    });

                    mAlipayQrSaleTrans.execute();

                } else if (Arrays.equals(HyperComMsg.instance.transactionCode, HyperCommClass.TRANSACTION_CODE_AUDIT_REPORT_TYPE)) {
                    Log.d(TAG, "Field Type : The host identity");
                    Log.d(TAG, "HOST_ID    = " + HyperComMsg.instance.data_field_HN_nii);

                    HostID = HyperComMsg.instance.data_field_HN_nii;
                    ActionTransReport actionTransReport = new ActionTransReport(new AAction.ActionStartListener() {
                        @Override
                        public void onStart(AAction action) {
                            ((ActionTransReport) action).setParam(mActContext, HostID, true);
                        }
                    });
                    actionTransReport.setEndListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            mHyperComm.auditReportTransactionSendResponse(HostID, 0);
                            ActivityStack.getInstance().popTo(MainActivity.class);
                            TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                            TransContext.getInstance().setCurrentAction(null); //fix leaks
                        }
                    });
                    actionTransReport.execute();
                }
            }
            return 0;
        }
    };
    PosNetCommInterface PosNetCommInterfaceCbk = new PosNetCommInterface() {
        public int onDataRcv(byte[] transactionType, byte[] data, boolean isVatb, byte[] REF1, byte[] REF2, byte[] vatAmount, byte[] taxAllowance, byte[] mercUniqueValue
                , byte[] campaignType, byte[] PosNo_ReceiptNo, byte[] CashierName) {
            WakeLockerClass mWakeLocker = new WakeLockerClass();
            mWakeLocker.acquire(mActContext);

            if (PosNo_ReceiptNo == null) {
                Arrays.fill(EcrData.instance.PosNo_ReceiptNo, (byte) 0x20);
            }


            byte[] amount = new byte[12];

            if (data != null) {
                Utils.SaveArrayCopy(data, 0, amount, 0, amount.length);
            }
            Device.beepPrompt();


            if (checkAmtZeroEcrProc(PROTOCOL.POSNET, new String(amount), transactionType)) {
                return 0;
            }

            if ((Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_SALE_TYPE)) ||
                    (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_SALE_VATB_TYPE))) {
                Log.d("onDataRcv:", "Transaction = " + new String(transactionType));
                Log.d("onDataRcv:", "Amount      = " + new String(amount));

                if (data.length >= 22) {
                    EcrData.instance.PosNo_ReceiptNo = Arrays.copyOfRange(data, 12, 22);
                    EcrData.instance.User_ID = Arrays.copyOfRange(data, 22, 28);
                    EcrData.instance.CashierName = Arrays.copyOfRange(data, 28, 38);
                }

                Acquirer acquirerPrompt = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
                Acquirer acquirerQRC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
                Acquirer acquirerDolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
                if ((acquirerPrompt != null && acquirerPrompt.isEnable())
                        || (acquirerQRC != null && acquirerQRC.isEnable())
                        || (acquirerDolfin != null && acquirerDolfin.isEnable())) {
                    //set default sale transaction
                    selectSale = "cancel";
                    menuTimeout.start();
                    Message message = Message.obtain();
                    message.what = 1;
                    mSaleSelectHandler.sendMessage(message);
                    Log.d("onDataRcv:", "Mutex Lock, selectSale = " + selectSale);

                    try {
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d("onDataRcv:", "Mutex Un-Lock, selectSale = " + selectSale);
                    menuTimeout.cancel();
                } else {
                    selectSale = Constants.ECR_SALE;
                }

                if (selectSale.equals(Constants.ECR_SALE)) {
                    byte mode = SWIPE | INSERT | WAVE | KEYIN /*| QR*/;

                    Acquirer acquirerWallet = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
                    boolean isWalletCscanB = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B);
                    if (acquirerWallet != null && acquirerWallet.isEnable() && !isWalletCscanB) {
                        mode |= QR;
                    }
                    if (selectSale.equals(Constants.ECR_SALE) && isVatb) {

                        SaleTrans emSaleTrans = new SaleTrans(mActContext, new String(amount), mode, null, true, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "VAT-B cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelVATBSendResponse(0);
                                }
                            }
                        }, isVatb, REF1, REF2, vatAmount, taxAllowance, mercUniqueValue, campaignType);

                        //check is vatb, do if cause
                        emSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "VAT-B saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    // Note : VATB use ECR\LinkPoS format same as with SALE response message
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_VATB_TYPE, 0);
                                }
                            }
                        });

                        emSaleTrans.execute();
                    } else if (selectSale.equals(Constants.ECR_SALE) && !isVatb) {
                        SaleTrans emSaleTrans = new SaleTrans(mActContext, new String(amount), mode, null, true, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        });

                        emSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });

                        emSaleTrans.execute();
                    } else if (selectSale.equals(Constants.ECR_QR_VISA)) {
                        QrSaleTrans mQrSaleTrans = new QrSaleTrans(mActContext, new String(amount), "00", new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        });

                        mQrSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });

                        mQrSaleTrans.execute();
                    } else if (selectSale.equals(Constants.ECR_QR_SALE)) {
                        BPSQrCodeSaleTrans mBPSQrCodeSaleTrans = new BPSQrCodeSaleTrans(mActContext, new String(amount), new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        });

                        mBPSQrCodeSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });

                        mBPSQrCodeSaleTrans.execute();

                    } else if (selectSale.equals(Constants.ECR_WALLET)) {
                        //Wallet
                        WalletSaleCBTrans mWalletSaleCBTrans = new WalletSaleCBTrans(mActContext, HyperComMsg.instance.data_field_40_amount, new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });

                        mWalletSaleCBTrans.execute();

                    } else if (selectSale.equals(Constants.ECR_DOLFIN)) {
                        //Dolfin
                        DolfinSaleTran mDolfinSaleTrans = new DolfinSaleTran(mActContext, new String(data).replaceFirst("^0+(?!$)", ""), new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        });

                        mDolfinSaleTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });
                        mDolfinSaleTrans.execute();
                    } else if(selectSale.equals(Constants.ECR_DOLFIN_C_SCAN_B)){
                        //Dolfin
                        DolfinSaleCScanBTran mDolfinSaleCScanBTrans = new DolfinSaleCScanBTran(mActContext,new String(data).replaceFirst("^0+(?!$)", ""), new ATransaction.TransEndListener() {
                            @Override
                            public void onEnd(ActionResult result) {
                                Log.d("onDataRcv:", "cancelSaleSendResponse ret =");
                                if (result.getRet() != TransResult.SUCC) {
                                    mPosNetComm.cancelSaleSendResponse(0);
                                }
                            }
                        } );

                        mDolfinSaleCScanBTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                            @Override
                            public void onEnd(AAction action, ActionResult result) {
                                Log.d("onDataRcv:", "saleTransactionSendResponse ret =");
                                if (result.getRet() == TransResult.SUCC) {
                                    mPosNetComm.saleTransactionSendResponse(PosNetCommClass.TRANSACTION_SALE_TYPE, 0);
                                }
                            }
                        });
                        mDolfinSaleCScanBTrans.execute();

                    } else {
                        mPosNetComm.cancelSaleSendResponse(0);
                    }

                }
            } else if (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_VOID_TYPE)) {
                Log.d("onDataRcv:", "Transaction =" + new String(transactionType));
                Log.d("onDataRcv:", "Tran no.      =" + new String(data));

                SaleVoidTrans mVoidtrans = new SaleVoidTrans(mActContext, Long.valueOf(new String(data)), new ATransaction.TransEndListener() {
                    @Override
                    public void onEnd(ActionResult result) {
                        if (result.getRet() != TransResult.SUCC) {
                            mPosNetComm.cancelVoidSendResponse(0);
                            return;
                        }
                    }
                });

                mVoidtrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                    @Override
                    public void onEnd(AAction action, ActionResult result) {
                        if (result.getRet() == TransResult.SUCC) {
                            mPosNetComm.voidTransactionSendResponse(PosNetCommClass.TRANSACTION_VOID_TYPE, 0);
                        }
                    }
                });

                mVoidtrans.execute();

            } else if (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_SETTLEMENT_TYPE)) {
                Log.d(TAG, "Field Type : The host identity");
                Log.d(TAG, "HOST_ID    = " + new String(data));

                new SettleTrans(mActContext, false, new String(data), new ATransaction.TransEndListener() {
                    @Override
                    public void onEnd(ActionResult result) {
                        //mPosNetComm.settlementTransactionSendResponse(PosNetCommClass.TRANSACTION_SETTLEMENT_TYPE,0);
                        if (result.getRet() != TransResult.SUCC) {
                            return;
                        }
                        checkParamUpdate(mActContext);
                    }
                }).execute();
            } else if (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_GET_PAN_TYPE)) {
                Log.d(TAG, "Field Type : The host identity");
                //Log.d(TAG, "HOST_ID    = " + new String(data));

//                GetPanTrans getPanTrans = new GetPanTrans(mActContext, new ATransaction.TransEndListener() {
//                    @Override
//                    public void onEnd(ActionResult result) {
//                        if (result.getRet() != TransResult.SUCC) {
//                            mPosNetComm.cancelGetPanSendResponse(0);
//                        } else {
//                            mPosNetComm.GetPanTransactionSendResponse(PosNetCommClass.TRANSACTION_GET_PAN_TYPE, 0);
//                        }
//                    }
//                });
//                getPanTrans.execute();

                GetPanTrans getPanTrans = new GetPanTrans(mActContext, null);
                getPanTrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                    @Override
                    public void onEnd(AAction action, ActionResult result) {
                        if (result.getRet() != TransResult.SUCC) {
                            mPosNetComm.cancelGetPanSendResponse(0);
                        } else {
                            mPosNetComm.GetPanTransactionSendResponse(PosNetCommClass.TRANSACTION_GET_PAN_TYPE, 0);
                        }
                    }
                });
                getPanTrans.execute();

            } else if (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_GET_THE_ONE_TYPE)) {
                Log.d(TAG, "Field Type : The host identity");
                boolean isT1CHostActivated = (FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AYCAP_T1C_HOST) != null);
                Log.d(TAG, "HOST AYCAP_T1C enable status : " + isT1CHostActivated);
                if (isT1CHostActivated) {
                    GetT1CMember T1CMemberInquiry = new GetT1CMember(mActContext, null);
                    T1CMemberInquiry.setECRProcReturnListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            if (result.getRet() != TransResult.SUCC) {
                                mPosNetComm.cancelGetT1CSendResponse(0);
                            } else {
                                mPosNetComm.GetTheOneTransactionSendResponse(PosNetCommClass.TRANSACTION_GET_PAN_TYPE, 0);
                            }
                        }
                    });
                    T1CMemberInquiry.execute();
                } else {
                    TransProcessListenerImpl transProcessListener = new TransProcessListenerImpl(mActContext);
                    transProcessListener.onUpdateProgressTitle(Utils.getString(R.string.t1c_inquiry_member_id_ecr_terminated));
                    transProcessListener.onShowErrMessage(Utils.getString(R.string.t1c_inquiry_member_id_host_disabled), Constants.FAILED_DIALOG_SHOW_TIME, false);
                    transProcessListener.onHideProgress();
                }

            }
//            else if (Arrays.equals(transactionType, PosNetCommClass.TRANSACTION_GET_THE_ONE_TYPE)) {
//                Log.d(TAG, "Field Type : The host identity");
//                //Log.d(TAG, "HOST_ID    = " + new String(data));
//                boolean isT1CHostActivated = (FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AYCAP_T1C_HOST) != null);
//                Log.d(TAG, "HOST AYCAP_T1C enable status : " + isT1CHostActivated);
//                if (isT1CHostActivated) {
//                    new GetPanTrans(mActContext, new ATransaction.TransEndListener() {
//                        @Override
//                        public void onEnd(ActionResult result) {
//                            mPosNetComm.GetTheOneTransactionSendResponse(PosNetCommClass.TRANSACTION_GET_THE_ONE_TYPE, 0);
//                            if (result.getRet() != TransResult.SUCC) {
//                                return;
//                            }
//                            checkParamUpdate(mActContext);
//                        }
//                    }, true).execute();
//                } else {
//                    TransProcessListenerImpl transProcessListener = new TransProcessListenerImpl(mActContext);
//                    transProcessListener.onUpdateProgressTitle("ECR Porcess terminated");
//                    transProcessListener.onShowErrMessage("Inquirt T1C's host disabled\nprocess was terminated", Constants.FAILED_DIALOG_SHOW_TIME, false);
//                    transProcessListener.onHideProgress();
//                }
//            }
            else {
                Log.d(TAG, "transactionType = " + Convert.getInstance().bcdToStr(transactionType));
                //Log.d(TAG, "data = " + Convert.getInstance().bcdToStr(data));
            }
            return 0;
        }


    };

    private void sendCancelEcrLinkPos() {
        if (mHyperComm != null) {
            if ((mHyperComm instanceof LawsonHyperCommClass
                    || mHyperComm instanceof LemonFarmHyperCommClass)) {
                EcrData.instance.RespCode = "ND".getBytes();
            }
            mHyperComm.cancelByHostSendResponse(HyperComMsg.instance.getTransactionCode(), 0);
        }
        resetFlag();
    }

    public void processLinkPosCommandLawson() {
        int ret = EcrProcessResult.ECR_PROCESS_FAILED;
        String title = "SALE MENU";

        // MerchantName
        final String MERC_NAME = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);

        String commandNumb = "";
        try {
            commandNumb = new String(HyperComMsg.instance.transactionCode, "UTF-8");
        } catch (UnsupportedEncodingException ueex) {
            ueex.printStackTrace();
        }

        if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_TYPE)) {
            Log.d(TAG, MERC_NAME + " Field Type : Amount Transaction");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");

            ret = validateEcrField(new String[]{"40", "R0"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            // set transaction amount toECR, use for reply back
            EcrData.instance.transAmount = HyperComMsg.instance.data_field_40_amount.getBytes();

            byte[] mode = new byte[]{((byte) (SWIPE | INSERT | WAVE | KEYIN))};
            Intent intent = new Intent(mActContext, EcrPaymentSelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = getEcrPaymentSelectorBundleForSaleAllCards(title, HyperCommClass.ECR_RESP_NO_DATA, mode);
            intent.putExtras(bundle);
            mActContext.startActivity(intent);
        } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_VOID_TYPE)) {
            Log.d(TAG, MERC_NAME + " Field Type : Trace/Invoice Number");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");
            Log.d(TAG, MERC_NAME + " TRACE_NUM  = " + HyperComMsg.instance.data_field_65_invoiceNo);

            ret = validateEcrField(new String[]{"65", "45"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            SaleVoidTrans mVoidtrans = new SaleVoidTrans(mActContext, Utils.parseLongSafe(HyperComMsg.instance.data_field_65_invoiceNo, -1), new ATransaction.TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    if (result.getRet() != EcrProcessResult.SUCC) {
                        sendCancelEcrLinkPos();
                        return;
                    }
                }
            });

            mVoidtrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                @Override
                public void onEnd(AAction action, ActionResult result) {
                    if (result.getRet() == EcrProcessResult.SUCC) {
                        resetFlag();
                        mHyperComm.voidTransactionSendResponse(0);
                    }
                }
            });

            mVoidtrans.execute();
        } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SETTLEMENT_TYPE)) {
            Log.d(TAG, "Field Type : The host identity");
            Log.d(TAG, "HOST_ID    = " + HyperComMsg.instance.data_field_HN_nii);

            // clear settlement total list
            EcrData.instance.clearSettlementTotalList();
            HostID = HyperComMsg.instance.data_field_HN_nii;

            ret = validateEcrField(new String[]{"HN", "45"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            List<TransData> record = FinancialApplication.getTransDataDbHelper().findEntireTransDataRecords();
            if (record.size() == 0) {
                if (ecrDismissListener != null) {
                    ecrDismissListener.onDismiss(EcrProcessResult.ECR_SETTLEMENT_ZERO_RECORD);
                }
                sendCancelEcrLinkPos();
                return;
            }

            new SettleTrans(mActContext, false, HostID, new ATransaction.TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    resetFlag();
                    mHyperComm.settlementTransactionSendResponse(HostID, 0);
                    if (result.getRet() != EcrProcessResult.SUCC) {
                        return;
                    }
                    checkParamUpdate(mActContext);
                }
            }).execute();
        } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
            Log.d(TAG, MERC_NAME + " Field Type : The host identity");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");

            ret = validateEcrField(new String[]{"40", "R0"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            // set transaction amount toECR, use for reply back
            EcrData.instance.transAmount = HyperComMsg.instance.data_field_40_amount.getBytes();

            Intent intent = new Intent(mActContext, EcrPaymentSelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = getEcrPaymentSelectorBundleForSaleAllQR(title, HyperCommClass.ECR_RESP_NO_DATA, null);
            intent.putExtras(bundle);
            mActContext.startActivity(intent);
        } else {
            if (ecrDismissListener != null) {
                ecrDismissListener.onDismiss(EcrProcessResult.ECR_FOUND_UNSUPPORTED_COMMAND);
            }
            sendCancelEcrLinkPos();
        }
    }

    private Bundle getEcrPaymentSelectorBundleForSaleAllCards(String title, @NonNull byte[] defRejectRespCode, byte[] searchMode) {
        return getEcrPaymentSelectorBundle(title, true, true, defRejectRespCode, searchMode, false, null);
    }

    private Bundle getEcrPaymentSelectorBundleForSaleAllQR(String title, @NonNull byte[] defRejectRespCode, String qrMode) {
        return getEcrPaymentSelectorBundle(title, true, true, defRejectRespCode, null, true, qrMode);
    }

    private Bundle getEcrPaymentSelectorBundle(String title, boolean dispBackButton, @NonNull boolean autoExecuteCommand, byte[] defRejectRespCode, byte[] searchMode, boolean qrPaymentEnabled, String qrMode) {
        Bundle bundle = new Bundle();
        // Default Parameter
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
        bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), dispBackButton);
        bundle.putBoolean(EUIParamKeys.ECR_PROCESS.toString(), true);
        bundle.putBoolean(EUIParamKeys.SUPPORT_E_RECEIPT.toString(), FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE));

        bundle.putString(EUIParamKeys.LINKPOS_STR_TRANS_AMOUNT.toString(), HyperComMsg.instance.data_field_40_amount.replace(" ","0"));
        bundle.putString(EUIParamKeys.LINKPOS_STR_BRANCH_ID.toString(), HyperComMsg.instance.data_field_D8_branchID);

        // Auto Execute during receipt command
        bundle.putBoolean(EUIParamKeys.LINKPOS_BOL_AUTO_EXECUTION_MODE.toString(), autoExecuteCommand);

        // Card Payment Configuration
        bundle.putBoolean(EUIParamKeys.LINKPOS_BOL_INCLUDE_CARD_PAYMENT.toString(), (searchMode != null));
        bundle.putString(EUIParamKeys.LINKPOS_STR_SEARCH_CARD_MODE.toString(), ((searchMode != null) ? Utils.bcd2Str(searchMode) : null));

        // QR Payment Configuration
        bundle.putBoolean(EUIParamKeys.LINKPOS_BOL_INCLUDE_QR_PAYMENT.toString(), qrPaymentEnabled);
        bundle.putString(EUIParamKeys.LINKPOS_STR_QR_PAYMENT_MODE.toString(), qrMode);
        bundle.putBoolean(EUIParamKeys.LINKPOS_BOL_QR_MANUAL_INQUIRY_ENABLED.toString(), ((qrMode != null) ? false : true));

        // ReferenceSaleID
        String tmpSaleReferenceID = null;
        if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            tmpSaleReferenceID = HyperComMsg.instance.data_field_R0_ref_saleID;
        } else if (FinancialApplication.getEcrProcess().mHyperComm instanceof LemonFarmHyperCommClass) {
            tmpSaleReferenceID = HyperComMsg.instance.data_field_R1_ref_saleID;
        }
        bundle.putString(EUIParamKeys.LINKPOS_STR_REFERENCE_SALE_ID.toString(), tmpSaleReferenceID);

        // Default ResponseCode for Rejection
        bundle.putString(EUIParamKeys.LINKPOS_STR_DEFAULT_REJECT_RESPONSE_CODE.toString(), Utils.bcd2Str(defRejectRespCode));

        return bundle;
    }

    private HashMap<String, String> getEcrDataFieldsValueForValidation() {
        HashMap<String, String> ecrDataField = new HashMap<>();
        if (EcrData.instance != null) {
            ecrDataField.put("40", HyperComMsg.instance.data_field_40_amount);
            ecrDataField.put("45", HyperComMsg.instance.data_field_45_merchant_number);
            ecrDataField.put("65", HyperComMsg.instance.data_field_65_invoiceNo);
            ecrDataField.put("A1", HyperComMsg.instance.data_field_A1_qr_type);
            ecrDataField.put("HN", HyperComMsg.instance.data_field_HN_nii);
            ecrDataField.put("R0", HyperComMsg.instance.data_field_R0_ref_saleID);       // for Lawson
            ecrDataField.put("R1", HyperComMsg.instance.data_field_R1_ref_saleID);       // for LemonFarm
        }
        return ecrDataField;
    }

    private HashMap<String, Integer> getEcrExpectedFieldLenForValidation() {
        HashMap<String, Integer> ecrExpectFieldLen = new HashMap<>();
        if (EcrData.instance != null) {
            ecrExpectFieldLen.put("40", EcrData.instance.transAmount.length);
            ecrExpectFieldLen.put("45", EcrData.instance.merchantNumber.length);
            ecrExpectFieldLen.put("65", EcrData.instance.traceInvoice.length);
            ecrExpectFieldLen.put("A1", EcrData.instance.walletType.length);
            ecrExpectFieldLen.put("HN", EcrData.instance.HYPER_COM_HN_NII.length);
            ecrExpectFieldLen.put("R0", EcrData.instance.saleReferenceIDR0.length);       // for Lawson
            ecrExpectFieldLen.put("R1", EcrData.instance.saleReferenceIDR1.length);       // for LemonFarm   // Normally this field just set and save to transData only
        }
        return ecrExpectFieldLen;
    }

    private int validateEcrField(String[] fieldNumber, EcrDismissListener listener) {
        HashMap<String, String> ecrDataField = getEcrDataFieldsValueForValidation();
        HashMap<String, Integer> ecrExpectFieldLen = getEcrExpectedFieldLenForValidation();

        int returnResult = EcrProcessResult.ECR_COMMAND_VALIDATION_FAILED;
        if (!(fieldNumber == null || fieldNumber.length == 0 || ecrDataField.size() != ecrExpectFieldLen.size())) {
            int validateConstraintStr = EcrProcessResult.ECR_COMMAND_VALIDATION_FAILED;
            int validateFormatStr = EcrProcessResult.ECR_COMMAND_VALIDATION_FAILED;
            boolean validationFailed = false;

            for (String fieldNo : fieldNumber) {
                switch (fieldNo) {
                    case "40":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_TRANS_AMOUNT_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_TRANS_AMOUNT_LENGTH;
                        break;
                    case "45":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_MERC_NUMBER_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_MERC_NUMBER_LENGTH;
                        break;
                    case "65":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_TRACE_INVOICE_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_TRACE_INVOICE_LENGTH;
                        break;
                    case "A1":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_QR_TYPE_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_QR_TYPE_LENGTH;
                        break;
                    case "HN":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_HOST_NII_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_HOST_NII_LENGTH;
                        break;
                    case "R0":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_SALE_REF_ID_R0_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_SALE_REF_ID_R0_LENGTH;
                        break;
                    case "R1":
                        validateConstraintStr = EcrProcessResult.ECR_VALIDATE_FAILED_SALE_REF_ID_R1_FORMAT;
                        validateFormatStr = EcrProcessResult.ECR_VALIDATE_FAILED_SALE_REF_ID_R1_LENGTH;
                        break;
                    default:
                        returnResult = EcrProcessResult.ECR_PARAMETER_MISSING;
                        break;
                }

                if ((!ecrDataField.containsKey(fieldNo)) || (ecrDataField.get(fieldNo) == null)) {
                    returnResult = validateConstraintStr;
                    validationFailed = true;
                    break;
                } else if (!ecrExpectFieldLen.containsKey((fieldNo)) || ecrDataField.get(fieldNo).length() != ecrExpectFieldLen.get(fieldNo)) {
                    returnResult = validateFormatStr;
                    validationFailed = true;
                    break;
                } else {
                    if (ecrDataField.get(fieldNo) != null) {
                        if (fieldNo.equals("HN") && (!ecrDataField.get(fieldNo).equals("999"))) {
                            returnResult = EcrProcessResult.ECR_UNSUPPORTED_SPECIFIC_NII;
                            validationFailed = true;
                            break;
                        }
                    }
                }
            }
            if (!validationFailed){
                returnResult = EcrProcessResult.SUCC;
            }
        }

        if (listener != null) {
            listener.onDismiss(returnResult);
        }
        return returnResult;
    }


    public void processLinkPosCommandLemonFarm() {
        // MerchantName
        final String MERC_NAME = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);
        String title = "SALE MENU";
        int ret = EcrProcessResult.ECR_PROCESS_FAILED;
        String commandNumb = "";
        Double amount = 0.00;
        String TraceNumb = "";
        String R1_Field = "";
        String A1_Field = "";
        try {
            commandNumb = new String(HyperComMsg.instance.transactionCode, "UTF-8");
            amount = ((HyperComMsg.instance.data_field_40_amount != null) ? Double.parseDouble(HyperComMsg.instance.data_field_40_amount) / 100 : 0.00);
            R1_Field = ((HyperComMsg.instance.data_field_R1_ref_saleID != null) ? HyperComMsg.instance.data_field_R1_ref_saleID : " Not found");
            A1_Field = ((HyperComMsg.instance.data_field_A1_qr_type != null) ? HyperComMsg.instance.data_field_A1_qr_type : " Not found");
        } catch (UnsupportedEncodingException ueex) {
            ueex.printStackTrace();
        }

        if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_TYPE)) {
            Log.d(TAG, MERC_NAME + " Field Type : Amount Transaction");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");
            Log.d(TAG, MERC_NAME + " Amount     = " + amount);
            Log.d(TAG, MERC_NAME + " Sale Reference ID = " + R1_Field);

            ret = validateEcrField(new String[]{"40", "R1"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            // set transaction amount toECR, use for reply back
            EcrData.instance.transAmount = HyperComMsg.instance.data_field_40_amount.getBytes();

            byte[] mode = new byte[]{((byte) (SWIPE | INSERT | WAVE | KEYIN))};
            Intent intent = new Intent(mActContext, EcrPaymentSelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = getEcrPaymentSelectorBundleForSaleAllCards(title, HyperCommClass.ECR_RESP_NO_DATA, mode);
            intent.putExtras(bundle);
            mActContext.startActivity(intent);
        } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_VOID_TYPE)) {
            Log.d(TAG, MERC_NAME + " Field Type : Trace/Invoice Number");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");
            Log.d(TAG, MERC_NAME + " traceNumber  = " + TraceNumb);

            ret = validateEcrField(new String[]{"65"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            SaleVoidTrans mVoidtrans = new SaleVoidTrans(mActContext, Utils.parseLongSafe(HyperComMsg.instance.data_field_65_invoiceNo, -1), new ATransaction.TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    if (result.getRet() != EcrProcessResult.SUCC) {
                        resetFlag();
                        HyperComMsg.instance.responseCode = new byte[]{0x4E, 0x44};
                        mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_VOID_TYPE, 0);
                        return;
                    }
                }
            });

            mVoidtrans.setECRProcReturnListener(new AAction.ActionEndListener() {
                @Override
                public void onEnd(AAction action, ActionResult result) {
                    if (result.getRet() == EcrProcessResult.SUCC) {
                        resetFlag();
                        mHyperComm.voidTransactionSendResponse(0);
                        return;
                    }
                }
            });

            mVoidtrans.execute();
        } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
            Log.d(TAG, MERC_NAME + " Field Type : The host identity");
            Log.d(TAG, MERC_NAME + " cmd number = " + commandNumb + "'");
            Log.d(TAG, MERC_NAME + " specified QR-Type = " + A1_Field);
            Log.d(TAG, MERC_NAME + " Sale Reference ID = " + R1_Field);

            ret = validateEcrField(new String[]{"40", "R1", "A1"}, ecrDismissListener);
            if (ret != EcrProcessResult.SUCC) {
                sendCancelEcrLinkPos();
                return;
            }

            // set transaction amount toECR, use for reply back
            EcrData.instance.transAmount = HyperComMsg.instance.data_field_40_amount.getBytes();

            Intent intent = new Intent(mActContext, EcrPaymentSelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = getEcrPaymentSelectorBundleForSaleAllQR(title, HyperCommClass.ECR_RESP_NO_DATA, HyperComMsg.instance.data_field_A1_qr_type);
            intent.putExtras(bundle);
            mActContext.startActivity(intent);
        } else {
            if (ecrDismissListener != null) {
                ecrDismissListener.onDismiss(EcrProcessResult.ECR_FOUND_UNSUPPORTED_COMMAND);
            }
        }
    }

    public EcrProcessClass(Context actContext) {
        mActContext = actContext;

        mUsbManager = (UsbManager) mActContext.getSystemService(Context.USB_SERVICE);

//        mLinkPosComm = new BaseL920BMCommClass("LINKPOS", mUsbManager);
//        mLinkPosComm.setParameters(0, 9600, 8, 1);

        mCommManage = new CommManageClass();
        mCommManage.AddConnListener(new CommConnInterface() {
            @Override
            public byte[] Read() {
                return mLinkPosComm.read_blocked(512, 600);
            }

            @Override
            public int Write(byte[] data_buf) {
                mLinkPosComm.write_blocked(data_buf, 1000);
                return 0;
            }

            @Override
            public boolean Connect(ConnectionInterface Listener) {
                return mLinkPosComm.connect(Listener);
            }

            @Override
            public boolean Connect() {
                return mLinkPosComm.connect();
            }

            @Override
            public boolean Disconnect() {
                return mLinkPosComm.disconnect();
            }
        });
        Acquirer acquirerPrompt = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
        Acquirer acquirerQRC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
        Acquirer acquirerWallet = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
        Acquirer acquirerDolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
        boolean isWalletCscanB = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B);
        if (acquirerPrompt != null && acquirerPrompt.isEnable() && acquirerQRC != null && acquirerQRC.isEnable() && acquirerWallet != null && isWalletCscanB) {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_VISA + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_WALLET + "</big>")};
        } else if (acquirerPrompt != null && acquirerPrompt.isEnable() && acquirerQRC != null && acquirerQRC.isEnable()) {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_VISA + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_SALE + "</big>")};
        } else if (acquirerPrompt != null && acquirerPrompt.isEnable()) {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_SALE + "</big>")};
        } else if (acquirerQRC != null && acquirerQRC.isEnable()) {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_QR_VISA + "</big>")};
        } else if (acquirerDolfin != null && acquirerDolfin.isEnable()) {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>"),
                    Html.fromHtml("<big>" + Constants.ECR_DOLFIN + "</big>")};
        } else {
            saleList = new CharSequence[]{Html.fromHtml("<big>" + Constants.ECR_SALE + "</big>")};
        }
    }

    public void onBaseConnect() {
        String linkPOSForMerchant = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);
        String protocol = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL);
        if (linkPOSForMerchant == null || protocol == null){
            Log.d("ProtoFilterClass:", "linkPOSForMerchant or protocol contain null value.");
            return;
        } else {
            if (linkPOSForMerchant.equals("Disable")) {
                Log.d("ProtoFilterClass:", "linkPOSForMerchant :: = " + linkPOSForMerchant);
                return;
            }
        }

        boolean instantHyperCommClassResult = false;
        boolean instantPosnetCommClassResult = false;
        if (protocol.equals("AUTO") || protocol.equals("HYPERCOM")) {
            if (mHyperComm == null) {
                instantHyperCommClassResult = instantLinkPosClass(linkPOSForMerchant, PROTOCOL.HYPERCOM);
                if (!instantHyperCommClassResult) {
                    mHyperComm = new HyperCommClass(mCommManage, mProtoFilter, HyperCommInterfaceCbk);
                }
            }
        }

        if (protocol.equals("AUTO") || protocol.equals("POSNET")) {
            if (mPosNetComm == null) {
                mPosNetComm = new PosNetCommClass(mCommManage, mProtoFilter, PosNetCommInterfaceCbk);
            }
        }


        Log.d(TAG, "<<<<<<<<<<..........onBaseConnect();..........>>>>>>>>>>");

    }

//    public void onBaseConnect() {
//
//        RunProcess();
//
//        if (mProtoFilter == null) {
//            mProtoFilter = new ProtoFilterClass(mCommManage);
//
//            mCommManage.AddReceiveListener(new CommManageInterface() {
//                public int onReceive(byte[] data, int len) {
//
//                    String strType = "";
//                    if (data == null || data.length == 0) {
//                        Log.d(TAG, "data == null");
//                        return 0;
//                    }
//
//                    try {
//                        if (data[0] == 6) {
//                            Log.d(TAG, "delete 06");
//                            data = Arrays.copyOfRange(data, 1, data.length);
//                            Log.d("ProtoFilterClass:", "buf2 = " + Convert.getInstance().bcdToStr(data));
//                        }
//                        if (data.length == 0) {
//                            Log.d(TAG, "data was not found after ACK was received.");
//                            return 0;
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                    PROTOCOL_RESULT type = mProtoFilter.protoIdentifier(data);
//
//                    if (type == PROTOCOL_RESULT.HYPERCOM) {
//                        strType = "HYPERCOM";
//                    } else if (type == PROTOCOL_RESULT.POSNET) {
//                        strType = "POSNET";
//                    } else if (type == PROTOCOL_RESULT.UNKNOWN) {
//                        strType = "UNKNOWN";
//                        mCommManage.StartReceive();
//                    } else if (type == PROTOCOL_RESULT.DISABLE) {
//                        strType = "DISABLE";
//                    }
//
//                    Log.d("ProtoFilterClass:", "protoIdentifier = " + strType);
//
//                    return 0;
//                }
//            });
//        }
//
//
//        String linkPOSForMerchant = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME);
//        String protocol = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL);
//        if (linkPOSForMerchant == null || protocol == null){
//            Log.d("ProtoFilterClass:", "linkPOSForMerchant or protocol contain null value.");
//            return;
//        } else {
//            if (linkPOSForMerchant.equals("Disable")) {
//                Log.d("ProtoFilterClass:", "linkPOSForMerchant :: = " + linkPOSForMerchant);
//                return;
//            }
//        }
//
//
//
//        boolean instantHyperCommClassResult = false;
//        boolean instantPosnetCommClassResult = false;
//        if (protocol.equals("AUTO") || protocol.equals("HYPERCOM")) {
//            if (mHyperComm == null) {
//                instantHyperCommClassResult = instantLinkPosClass(linkPOSForMerchant, PROTOCOL.HYPERCOM);
//                if (!instantHyperCommClassResult) {
//                    mHyperComm = new HyperCommClass(mCommManage, mProtoFilter, HyperCommInterfaceCbk);
//                }
//            }
//            if (mHyperComm != null) {
//                mHyperComm.onBaseConnect();
//            }
//        }
//
//        if (protocol.equals("AUTO") || protocol.equals("POSNET")) {
//            if (mPosNetComm == null) {
//                mPosNetComm = new PosNetCommClass(mCommManage, mProtoFilter, PosNetCommInterfaceCbk);
//            }
//            if (mPosNetComm != null) {
//                mPosNetComm.onBaseConnect();
//            }
//        }
//
//
//        Log.d(TAG, "<<<<<<<<<<..........onBaseConnect();..........>>>>>>>>>>");
//
//    }//todo linkpos_cz

    private boolean instantLinkPosClass(String mercName, PROTOCOL linkPosProtocol) {
        String packageName = "com.pax.pay.ECR";
        String protocol = (linkPosProtocol == PROTOCOL.HYPERCOM) ? "HyperCommClass" : "PosNetCommClass";
        String linkPOSFullClassName = String.format("%1$s.%2$s%3$s", packageName, mercName, protocol);
        try {
            Class linkPOSClass = Class.forName(linkPOSFullClassName);

            //linkPOSClass.getConstructor(CommManageClass.class, ProtoFilterClass.class, interfaceClass);
            if (linkPosProtocol == PROTOCOL.HYPERCOM) {
                mHyperComm = (HyperCommClass) linkPOSClass.newInstance();
            } else {
                mPosNetComm = (PosNetCommClass) linkPOSClass.newInstance();
            }
            Log.d(TAG, String.format("%s : create successfully.", linkPOSFullClassName));
            return true;
        } catch (Exception ex) {
            Log.d(TAG, String.format("Cannot find Ecr subclass : %s", linkPOSFullClassName));
        }
        return false;
    }

//    private boolean instantLinkPosClass(String mercName, PROTOCOL linkPosProtocol) {
//        String packageName = "com.pax.pay.ECR";
//        String protocol = (linkPosProtocol == PROTOCOL.HYPERCOM) ? "HyperCommClass" : "PosNetCommClass";
//        String linkPOSFullClassName = String.format("%1$s.%2$s%3$s", packageName, mercName, protocol);
//        try {
//            Class linkPOSClass = Class.forName(linkPOSFullClassName);
//
//            //linkPOSClass.getConstructor(CommManageClass.class, ProtoFilterClass.class, interfaceClass);
//            if (linkPosProtocol == PROTOCOL.HYPERCOM) {
//                mHyperComm = (HyperCommClass) linkPOSClass.newInstance();
//                mHyperComm.initial(mCommManage, mProtoFilter, HyperCommInterfaceCbk);
//                mHyperComm.onBaseConnect();
//                mHyperComm.AddReceiveListener(FinancialApplication.getEcrProcess().mProtoFilter);
//            } else {
//                mPosNetComm = (PosNetCommClass) linkPOSClass.newInstance();
//                mPosNetComm.initial(mCommManage, mProtoFilter, PosNetCommInterfaceCbk);
//                mPosNetComm.onBaseConnect();
//                mPosNetComm.AddReceiveListener(FinancialApplication.getEcrProcess().mProtoFilter);
//            }
//            Log.d(TAG, String.format("%s : create successfully.", linkPOSFullClassName));
//            return true;
//        } catch (Exception ex) {
//            Log.d(TAG, String.format("Cannot find Ecr subclass : %s", linkPOSFullClassName));
//        }
//        return false;
//    }//todo linkpos_cz


    public void RemoveConnListener() {

        mLinkPosComm.disconnect();

        if (mHyperComm != null) {
            mHyperComm.onBaseDisconnect();
        }

        if (mPosNetComm != null) {
            mPosNetComm.onBaseDisconnect();
        }

        Log.d(TAG, "<<<<<<<<<<..........onBaseDisconnect..........>>>>>>>>>>");
    }

    public void RunProcess() {
        mCommManage.StartThread();
    }

    public void StopProcess() {
        if (mCommManage != null) {
            mCommManage.StopThread();
            mCommManage = null;
            mLinkPosComm = null;
            Log.d(TAG, "<<<<<<<<<<..........Stoped..........>>>>>>>>>>");
        }
    }

    private boolean checkAmtZeroEcrProc(PROTOCOL protocol, String amt, byte[] transType) {

        boolean returnStatus = false;
        switch (protocol) {
            case HYPERCOM:
                if (amt != null && Double.parseDouble(amt) <= 0.00
                        && !Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_VOID_TYPE)
                        && !Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SETTLEMENT_TYPE)) {
                    DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.dialog_not_allowed_zero_amt), null, Constants.FAILED_DIALOG_SHOW_TIME);
                    returnStatus = true;
                } else {
                    if (Utils.parseLongSafe(amt, 0) > FinancialApplication.getSysParam().getEDCMaxAmt()) {
                        DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.err_amount_exceed_max_limit), null, Constants.FAILED_DIALOG_SHOW_TIME);
                        returnStatus = true;
                    }
                }

                if (returnStatus) {
                    if (FinancialApplication.getEcrProcess().mHyperComm instanceof LemonFarmHyperCommClass
                                 || FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {

                        sendCancelEcrLinkPos();

                    } else {
                        if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SALE_ALL_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                        } else if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_CREDIT_TYPE, 0);
                        } else if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SALE_RABBIT_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_RABBIT_TYPE, 0);
                        } else if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_QR_TYPE, 0);
                        } else if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_SALE_WALLET_TYPE, 0);
                        } else if (Arrays.equals(transType, HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE)) {
                            mHyperComm.cancelByHostSendResponse(HyperCommClass.TRANSACTION_CODE_QR_VISA_TYPE, 0);
                        }
                    }
                }

                break;
            case POSNET:
                if (Arrays.equals(transType, PosNetCommClass.TRANSACTION_SALE_TYPE)
                        && amt != null && Double.parseDouble(amt) <= 0.00
                ) {
                    DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.dialog_not_allowed_zero_amt), null, Constants.FAILED_DIALOG_SHOW_TIME);
                    returnStatus = true;
                } else {
                    if (Utils.parseLongSafe(amt, 0) > FinancialApplication.getSysParam().getEDCMaxAmt()) {
                        DialogUtils.showErrMessage(ActivityStack.getInstance().top(), Utils.getString(R.string.menu_sale), Utils.getString(R.string.err_amount_exceed_max_limit), null, Constants.FAILED_DIALOG_SHOW_TIME);
                        returnStatus = true;
                    }
                }

                if (returnStatus) {
                    mPosNetComm.cancelSaleSendResponse(0);
                    resetFlag();
                }

                break;
        }
        return returnStatus;
    }

    private void checkParamUpdate(Context context) {
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_UPDATE_PARAM)) {
            boolean paramResult = FinancialApplication.getDownloadManager().handleSuccess(context);

            if (paramResult) {
                NotificationUtils.cancelNotification(context);
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
                NotificationUtils.makeNotification(context, context.getString(R.string.notif_param_complete), context.getString(R.string.notif_param_success), false);
            } else {
                NotificationUtils.cancelNotification(context);
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
                NotificationUtils.makeNotification(context, context.getString(R.string.notif_param_fail), context.getString(R.string.notif_param_call_bank), false);
            }
        }
    }

    public enum PROTOCOL {
        HYPERCOM, POSNET, AUTO, AUTO_HYPERCOM_IND, AUTO_POSNET_IND, DISABLE
    }

    public enum PROTOCOL_RESULT {
        HYPERCOM, POSNET, UNKNOWN, DISABLE
    }

    public void resetFlag() {
        // unblock for edc able to receive-data
        EcrData.instance.isOnProcessing = false;
    }


}
