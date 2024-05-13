package com.pax.pay.trans.action.activity;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.HyperComMsg;
import com.pax.pay.ECR.HyperCommClass;
import com.pax.pay.TerminalLockCheck;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.AlipayQrSaleTrans;
import com.pax.pay.trans.KplusQrSaleTrans;
import com.pax.pay.trans.QRCreditSaleTrans;
import com.pax.pay.trans.SaleTrans;
import com.pax.pay.trans.WechatQrSaleTrans;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

import java.util.Arrays;
import java.util.List;

//import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.tran.DolfinSaleTran;
import th.co.bkkps.utils.Log;


public class ErcPaymentSelectActivity extends BaseActivityWithTickForAction {
    // For log module
    private String TAG = "LinkPoS:";

    // Local Variable
    private MenuPage menuPage;
    private String  transAmount;
    private String  branchID;
    private boolean autoExecMode;
    private String  cardMode;
    private byte    searchCardMode;
    private boolean includeCardPayment = false;
    private boolean includeQRPayment   = false;
    private String  qrPayMode;                                  // for field A1
    private String  refSaleID;                                  // for field R1
    private boolean isVoidedTrans = false;
    private boolean isNormalTrans = false;
    private boolean qrPosManualInquiryMode = false;
    private String  rejectDefaultRespCode;
    private boolean currKioskMode = false;
    private enum terminalLockState { MODE_BYPASS, MODE_DEFAULT}

    // Listener
    private ATransaction.TransEndListener       transEndListener = null;
    private AAction.ActionEndListener           actionEndListener = null;
    private DialogInterface.OnDismissListener   onDismissListener = null;
    private DialogInterface.OnDismissListener   onDismissVoidTransListener = null;
    private View.OnClickListener                btnOnClickListener = null;
    private DialogInterface.OnDismissListener   onAuditReportPrintDismissListener = null;

    private LinearLayout mLayout = null;
    Button btn_cancel = null;

    // EcrProcessClass
    private HyperCommClass mHyperComm = null;
    private boolean isReturnedToEcrLinkPos = false;
    private boolean isPrintAuditReportMode = false;
    private byte[] returnParam_QRType ;

    // preset - QRType for Lawson
    public static byte[] SEL_PAYMENT_QR_ALP =  new byte[] {0x30, 0x31};       // ALIPAY
    public static byte[] SEL_PAYMENT_QR_WCP =  new byte[] {0x30, 0x32};       // WECHATPAY
    public static byte[] SEL_PAYMENT_QR_TQR =  new byte[] {0x30, 0x33};       // THAI QR
    public static byte[] SEL_PAYMENT_QR_QRC =  new byte[] {0x30, 0x34};       // QR CREDIT

    // AcquirerList
    Acquirer acqKBANK    = null;
    Acquirer acqTPN      = null;
    Acquirer acqAMEX     = null;
    Acquirer acqKPLUS    = null;
    Acquirer acqAlipay   = null;
    Acquirer acqWechat   = null;
    Acquirer acqQrCredit = null;
    Acquirer acqDolFin   = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switchLockScreenState(terminalLockState.MODE_BYPASS);
    }

    private void switchLockScreenState(terminalLockState state) {
        if (state == terminalLockState.MODE_BYPASS) {
            // turn off kiosk mode
            currKioskMode = TerminalLockCheck.getInstance().getKioskMode();
            TerminalLockCheck.getInstance().setKioskMode(false);
        } else {
            TerminalLockCheck.getInstance().setKioskMode(currKioskMode);
        }
    }

    @Override
    protected int getLayoutId() { return R.layout.activity_ecr_payment_selector; }

    @Override
    protected String getTitleString() {
        if (Arrays.equals(HyperComMsg.instance.getTransactionCode(),new byte[] {'9','1'})) {
            isPrintAuditReportMode=true;
            return "PRINT AUDIT REPORT";
        } else {
            return "LINKPOS : SALE MENU";
        }
        //return (Arrays.equals(HyperComMsg.instance.getTransactionCode(),new byte[] {'9','1'})) ? "LINKPOS : PRINT AUDIT REPORT"  :"LINKPOS : SALE MENU";
    }

    private void setSearchCardMode (String exCardMode){
        if(exCardMode != null) {
            if(exCardMode != "") {
                searchCardMode = Utils.str2Bcd(exCardMode)[0];
                return;
            }
        }

        searchCardMode = ActionSearchCard.SearchMode.SWIPE | ActionSearchCard.SearchMode.INSERT
                       | ActionSearchCard.SearchMode.KEYIN | ActionSearchCard.SearchMode.WAVE;
    }

    private void setDefaultRejectRespCode(String exRejectDefaultRespCode) {
        if(exRejectDefaultRespCode != null) {
            if(exRejectDefaultRespCode != "") {
                ECR_RESP_EDC_REJECT = Utils.str2Bcd(exRejectDefaultRespCode);
            }
        }

        if(ECR_RESP_EDC_REJECT == null) {
            ECR_RESP_EDC_REJECT = HyperCommClass.ECR_RESP_USER_CANCEL;
        }
    }

    @Override
    protected void loadParam() {
        transAmount             = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_TRANS_AMOUNT.toString());
        branchID                = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_BRANCH_ID.toString());
        autoExecMode            = getIntent().getBooleanExtra(EUIParamKeys.LINKPOS_BOL_AUTO_EXECUTION_MODE.toString(),false);
        cardMode                = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_SEARCH_CARD_MODE.toString());
        includeCardPayment      = getIntent().getBooleanExtra(EUIParamKeys.LINKPOS_BOL_INCLUDE_CARD_PAYMENT.toString(),false);
        includeQRPayment        = getIntent().getBooleanExtra(EUIParamKeys.LINKPOS_BOL_INCLUDE_QR_PAYMENT.toString(),false);
        qrPayMode               = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_QR_PAYMENT_MODE.toString());                              // field A1
        refSaleID               = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_REFERENCE_SALE_ID.toString());                            // field R0 or R1 (received from POS)
        qrPosManualInquiryMode  = getIntent().getBooleanExtra(EUIParamKeys.LINKPOS_BOL_QR_MANUAL_INQUIRY_ENABLED.toString(),false);
        rejectDefaultRespCode   = getIntent().getStringExtra(EUIParamKeys.LINKPOS_STR_DEFAULT_REJECT_RESPONSE_CODE.toString());

        // set extra SearchMode & DefualtRejectionRespCode
        setSearchCardMode(cardMode);
        setDefaultRejectRespCode(rejectDefaultRespCode);

        Log.d(TAG, "==================================================================================");
        Log.d(TAG, "                   Parameter passed through this activity                         ");
        Log.d(TAG, "==================================================================================");
        Log.d(TAG, " 1. TransAmount = " + transAmount);
        Log.d(TAG, " 2. branchID = " + branchID);
        Log.d(TAG, " 3. includeCardPayment = " + includeCardPayment);
        Log.d(TAG, " 4. includeQRPayment = " + includeQRPayment);
        Log.d(TAG, " 5. qrPayMode = " + qrPayMode);
        Log.d(TAG, " 6. refSaleID = " + refSaleID);
        Log.d(TAG, " 7. cardMode = " + cardMode);
        Log.d(TAG, " 8. autoExecMode = " + autoExecMode + "        ** for accept direct separated command");
        Log.d(TAG, " 9. qrPosManualInquiryMode = " + qrPosManualInquiryMode);
        Log.d(TAG, "10. rejectDefaultRespCode = " + rejectDefaultRespCode);
        Log.d(TAG, "==================================================================================");
    }

    @Override
    protected void initViews( ) {
        String SessionID = Device.getTime(Constants.TIME_PATTERN_TRANS2);
        Log.d(TAG, " EcrPaymentSelectionActivity SessionID = " + SessionID);
        TAG += "SESSIONID : " + SessionID ;

        if (FinancialApplication.getEcrProcess() != null) {
            mHyperComm = FinancialApplication.getEcrProcess().mHyperComm;
            Log.d(TAG, "HyperCommClass has been set");
        } else {
            mHyperComm = null;
            Log.d(TAG, "instant of HyperCommClass was not found");
        }

        // Assign acquirer data
        acqKBANK    = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KBANK);
        acqTPN      = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_UP);
        acqAMEX     = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
        acqKPLUS    = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_KPLUS);
        acqAlipay   = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ALIPAY);
        acqWechat   = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_WECHAT);
        acqQrCredit = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_QR_CREDIT);
        //acqDolFin   = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_DOLFIN);

        // Display Amount
        TextView tvAmount = (TextView) findViewById(R.id.amount_edit);
        tvAmount.setText(CurrencyConverter.convert(Utils.parseLongSafe(transAmount,0)));

        TextView tvLabel = (TextView) findViewById(R.id.txv_amount_label) ;

        // LinearLayout
        mLayout = (LinearLayout) findViewById(R.id.ll_gallery);

        // CancelButton
        btn_cancel = (Button) findViewById(R.id.btn_cancel);

        if(isPrintAuditReportMode) {
            tvAmount.setVisibility(View.INVISIBLE);
            mLayout.setVisibility(View.INVISIBLE);
            btn_cancel.setVisibility(View.INVISIBLE);
            tvLabel.setVisibility(View.INVISIBLE);
        }
    }



    private MenuPage createMenu() {
        Log.d(TAG, "Create MenuPage -- Begin");
        MenuPage.Builder builder = new MenuPage.Builder(ErcPaymentSelectActivity.this, 9, 3);
        if (includeCardPayment) {
            if(acqKBANK !=null || acqTPN != null || acqAMEX != null) {
                builder.addTransItem("SALE", R.drawable.app_sale, getCreditDebitCardSaleTrans());
                Log.d(TAG, "process as binding-menu-mode : Credit/Debit SaleTransaction");
            }
        }

        if(includeQRPayment) {
            if(acqAlipay !=null)    {
                builder.addTransItem("Alipay", R.drawable.icon_alipay, getAlipayTrans());
                Log.d(TAG, "process as binding-menu-mode : Alipay SaleTransaction");
            }
            if(acqWechat !=null)    {
                builder.addTransItem("Wechat", R.drawable.icon_wechatpay, getWechatTrans());
                Log.d(TAG, "process as binding-menu-mode : ThaiQR SaleTransaction");
            }
            if(acqKPLUS !=null)     {
                builder.addTransItem("THAI QR", R.drawable.icon_thaiqr, getKPlusTrans());
                Log.d(TAG, "process as binding-menu-mode : ThaiQR SaleTransaction");
            }
            if(acqQrCredit !=null)  {
                builder.addTransItem(getString(R.string.menu_qr_credit_sale), R.drawable.app_sale, getQrCreditTrans());
                Log.d(TAG, "process as binding-menu-mode : QRCredit SaleTransaction");
            }
            //if(acqDolFin !=null)    {
                //builder.addTransItem("Dolfin", R.drawable.dolfin_icon, getDolfinTrans());
                //Log.d(TAG, "process as binding-menu-mode : Dolfin SaleTransaction");
            //}
        }

        Log.d(TAG, "Create MenuPage -- End");
        return  builder.create();
    }

    byte[] ECR_RESP_EDC_REJECT = null;

    @Override
    protected void setListeners() {
        transEndListener =  new ATransaction.TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                Log.d(TAG, "cancelSaleSendResponse ret ="  + result.getRet());
                if (result.getRet() != TransResult.SUCC) {
                    if (mHyperComm != null) {
                        returnRejectResponse(ECR_RESP_EDC_REJECT);
                        isReturnedToEcrLinkPos=true;
                    }
                }
                finish(new ActionResult(result.getRet(), null));
                finish();
            }
        };

        actionEndListener =  new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                Log.d(TAG, "saleTransactionSendResponse ret =" + result.getRet());
                if (result.getRet() == TransResult.SUCC) {
                    if (Arrays.equals(HyperComMsg.instance.getTransactionCode(), new byte[] {'7','2'})) {
                        try {
                            if (result.getData() != null) {
                                if (result.getRet() == TransResult.SUCC ) {
                                    if(result.getData().toString().equals("QR-INQUIRY")) {
                                        returnSaleResponse();
                                    } else {
                                        returnRejectResponse(ECR_RESP_EDC_REJECT);
                                    }
                                }
                            }
                        } catch (Exception ex) {

                        }
                        isReturnedToEcrLinkPos=true;
                    } else {
                        returnSaleResponse();
                        isReturnedToEcrLinkPos=true;
                    }
                } else if ( result.getRet() != TransResult.SUCC) {
                    if (Arrays.equals(HyperComMsg.instance.getTransactionCode(), new byte[] {'7','1'})) {
                        returnSaleResponse();
                        isReturnedToEcrLinkPos=true;
                    }
                }

                if(result.getRet() == TransResult.SUCC) {
                    setResult(result.getRet());
                    finish();
                }
            }
        };

        onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                returnRejectResponse(ECR_RESP_EDC_REJECT);
                setResult(TransResult.ERR_USER_CANCEL,null);
                finish();
            }
        };

        onDismissVoidTransListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                returnRejectResponse(ECR_RESP_EDC_REJECT);
                setResult(TransResult.ERR_ECR_DUPLICATE_SALE_REFERENCE_ID,null);
                finish();
            }
        };

        onAuditReportPrintDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                returnRejectResponse(ECR_RESP_EDC_REJECT);
                setResult(TransResult.ERR_NO_TRANS,null);
                finish();
            }
        };


        btnOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Activity was closed by user manual press on CANCEL button");
                btn_cancel.setEnabled(false);
                returnRejectResponse(ECR_RESP_EDC_REJECT);
                isReturnedToEcrLinkPos=true;
                setResult(TransResult.ERR_USER_CANCEL,null);
                finish();
            }
        };
        btn_cancel.setOnClickListener(btnOnClickListener);

        try {
            menuPage = createMenu();
            mLayout.addView(menuPage);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "UI - Create menuPage failed.");
        }

        if(autoExecMode) {
            Log.d(TAG, "ENABLED : auto-execution-direct-command mode");
            autoOpenTrans();
        } else {
            Log.d(TAG, "DISABLED : auto-execution-direct-command mode");
        }
    }

    int printcount = 0;
    private void printAuditReport(String localHostNII) {
        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                printcount = 0;
                int ret = -1;
                for (Acquirer acquirer : acquirerList) {
                    try {
                        ret = -1;
                        if (localHostNII.equals("999")) {
                            ret = Printer.printAuditReport(ErcPaymentSelectActivity.this, acquirer);
                        } else {
                            if(acquirer.getNii().equals(localHostNII) && acquirer.isEnable()) {
                                ret = Printer.printAuditReport(ErcPaymentSelectActivity.this, acquirer);
                            }
                        }

                        if(ret == TransResult.SUCC) {
                            printcount+=1;
                        }

                    } catch (Exception ex) {

                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EcrData.instance.singleHostNII = HyperComMsg.instance.data_field_HN_nii.getBytes();
                        if (printcount > 0) {
                            returnAuditReportResponse();
                            Device.beepOk();
                            finish(new ActionResult(TransResult.SUCC,null));
                            finish();
                        } else {
                            DialogUtils.showErrMessage(ErcPaymentSelectActivity.this, Utils.getString(R.string.linkpos_ecr_audit_report_printing),"Error : No Record ", onAuditReportPrintDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                        }
                    }
                });
            }
        });



    }


    //private boolean search_RefSaleID_transData (String refSaleID) {
    private boolean search_RefSaleID_transData (String refSaleID) {
        try {
            Log.d(TAG, "searchReferenceSaleId -- Begin");
            isVoidedTrans = false;
            isNormalTrans = false;

            boolean processNext =false;
            if (refSaleID != null) {
                if (refSaleID.length() >0 ) {
                    processNext = true;
                }
            }

            if(processNext) {
                TransData dbLastRecord = FinancialApplication.getTransDataDbHelper().findLatestSaleVoidTransData();
                lastTransDataRecord = dbLastRecord;
                if (dbLastRecord !=null) {
                    if( dbLastRecord.getReversalStatus().equals(TransData.ReversalStatus.NORMAL)) {
                        if(dbLastRecord.getReferenceSaleID().equals(refSaleID)) {
                            Log.d(TAG, "send Re-call-transaction back to POS for ReferenceSaleID = " + refSaleID);
                            Component.setTransDataInstance(dbLastRecord);
                            EcrData.instance.setEcrData(dbLastRecord, FinancialApplication.getSysParam(), dbLastRecord.getAcquirer(), new ActionResult(TransResult.SUCC,null));
                            Log.d(TAG, "searchReferenceSaleId -- End");
                            return true;
                        }
                    }
                } else {
                    Log.d(TAG, "List of TransData was empty");
                }
            }
        } catch (Exception ex ) {
           ex.printStackTrace();
            Log.d(TAG, "unable to retrieve transdata from database");
        }
        Log.d(TAG, "searchReferenceSaleId -- End");
        return false;
    }

    private TransData lastTransDataRecord = null;
    private void autoOpenTrans() {
        if(search_RefSaleID_transData(refSaleID)) {
            Log.d(TAG, "returnSaleResponse-Automatically--Begin");
            if (lastTransDataRecord.getTransState() == TransData.ETransStatus.VOIDED) {
                DialogUtils.showErrMessage(this, "", Utils.getString(R.string.linkpos_ecr_error_duplicate_sale_ref_id), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            } else if (lastTransDataRecord.getReversalStatus() != TransData.ReversalStatus.NORMAL) {
                DialogUtils.showErrMessage(this, "", Utils.getString(R.string.linkpos_ecr_error_last_trans_contain_reversal), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            } else {
                returnSaleResponse();
                setResult(TransResult.SUCC,null);
                finish();
            }
            isReturnedToEcrLinkPos=true;
            Log.d(TAG, "returnSaleResponse-Automatically--End");
            return;
        }

        if (includeCardPayment && ! includeQRPayment) {
            Log.d(TAG, "process as executor mode : Credit/Debit SaleTransaction");
            tickTimer.stop();                                                                       // disable timer
            getCreditDebitCardSaleTrans(true);
        } else if ( ! includeCardPayment && includeQRPayment) {
            if(! includeCardPayment && includeQRPayment) {
                if (qrPayMode != null ){
                    tickTimer.stop();                                                               // disable timer
                    switch (qrPayMode) {
                        case "01" :
                            Log.d(TAG, "process as executor mode : Alipay SaleTransaction");
                            getAlipayTrans(true);
                            break;
                        case "02" :
                            Log.d(TAG, "process as executor mode : WechatPay SaleTransaction");
                            getWechatTrans(true);
                            break;
                        case "03" :
                            Log.d(TAG, "process as executor mode : ThaiQR SaleTransaction");
                            getKPlusTrans(true);
                            break;
                        case "04" :
                            Log.d(TAG, "process as executor mode : QRCredit SaleTransaction");
                            getQrCreditTrans(true);
                            break;
                        //case "05" : getDolfinTrans(true); break;                                  // disable command A1 to support Dolfin direct inquiry
                        default : {
                            Log.d(TAG, "unknown QRTypr (R1) Mode = '" + qrPayMode + "'");
                            DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_r1_unknown_qr_type),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                            break;
                        }
                    }
                }
            }
        } else {
            if(Arrays.equals(HyperComMsg.instance.getTransactionCode(), new byte[] {'9','1'})) {
                printAuditReport(HyperComMsg.instance.data_field_HN_nii);
            }
        }
    }

    private SaleTrans getCreditDebitCardSaleTrans(){ return getCreditDebitCardSaleTrans(false); }
    private SaleTrans getCreditDebitCardSaleTrans(boolean isExecuteNeeded) {
        if(acqKBANK != null || acqAMEX != null || acqTPN != null) {
            SaleTrans creditdebitSaleTrans = new SaleTrans(this, transAmount, searchCardMode, branchID, true, refSaleID, transEndListener);
            creditdebitSaleTrans.setECRProcReturnListener(actionEndListener);
            if (isExecuteNeeded) {
                creditdebitSaleTrans.execute();
            }
            return creditdebitSaleTrans;
        } else {
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_unsupport_card_payment),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
            return null;
        }
    }

    private DolfinSaleTran getDolfinTrans(){return getDolfinTrans(false);}
    private DolfinSaleTran getDolfinTrans(boolean isExecuteNeeded) {
        try {
            if(acqDolFin !=null) {
                if (DolfinApi.getInstance() != null) {
                    if(DolfinApi.getInstance().getDolfinServiceBinded()) {
                        DolfinSaleTran dolfinSaleTrans = new DolfinSaleTran(this,transAmount.replaceFirst("^0+(?!$)", ""), refSaleID, transEndListener);
                        dolfinSaleTrans.setECRProcReturnListener(actionEndListener);
                        if (isExecuteNeeded) {
                            dolfinSaleTrans.execute();
                        }
                        return dolfinSaleTrans;
                    } else {
                        // service not bind
                        if (isExecuteNeeded) {
                            DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_unsupport_card_payment),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                        }
                    }
                } else {
                    // dolfin api not available
                    if (isExecuteNeeded) {
                        DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_dolfin_api_not_found),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                    }
                }
            } else {
                if (isExecuteNeeded) {
                    DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_dolfin_acquirer_disable),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_dolfin_service_failed),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
        }
        return null;
    }

    private QRCreditSaleTrans getQrCreditTrans(){
        return getQrCreditTrans(false);
    }
    private QRCreditSaleTrans getQrCreditTrans(boolean isExecuteNeeded) {
        if(acqQrCredit !=null) {
            QRCreditSaleTrans qrCreditSaleTrans = new QRCreditSaleTrans(this, transAmount, refSaleID, qrPosManualInquiryMode, transEndListener);
            qrCreditSaleTrans.setECRProcReturnListener(actionEndListener);
            if (isExecuteNeeded) {
                qrCreditSaleTrans.execute();
            }
            return qrCreditSaleTrans;
        } else {
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_qrcredit_acquirer_disable),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
            return null;
        }
    }

    private AlipayQrSaleTrans getAlipayTrans(){
        return getAlipayTrans(false);
    }
    private AlipayQrSaleTrans getAlipayTrans(boolean isExecuteNeeded) {
        if(acqAlipay !=null) {
            AlipayQrSaleTrans alipaySaleTrans = new AlipayQrSaleTrans(this, transAmount, refSaleID, qrPosManualInquiryMode, transEndListener);
            alipaySaleTrans.setECRProcReturnListener(actionEndListener);
            if (isExecuteNeeded) {
                alipaySaleTrans.execute();
            }
            return alipaySaleTrans;
        } else {
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_alipay_acquirer_disable),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
            return null;
        }
    }

    private WechatQrSaleTrans getWechatTrans(){
        return getWechatTrans(false);
    }
    private WechatQrSaleTrans getWechatTrans(boolean isExecuteNeeded) {
        if(acqWechat !=null) {
            WechatQrSaleTrans wechatSaleTrans = new WechatQrSaleTrans(this, transAmount, refSaleID, qrPosManualInquiryMode, transEndListener);
            wechatSaleTrans.setECRProcReturnListener(actionEndListener);
            if (isExecuteNeeded) {
                wechatSaleTrans.execute();
            }
            return wechatSaleTrans;
        } else {
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_wechat_acquirer_disable),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
            return null;
        }
    }

    private KplusQrSaleTrans getKPlusTrans(){
        return getKPlusTrans(false);
    }
    private KplusQrSaleTrans getKPlusTrans(boolean isExecuteNeeded) {
        if(acqKPLUS !=null) {
            KplusQrSaleTrans kplusSaleTrans = new KplusQrSaleTrans(this, transAmount, refSaleID, qrPosManualInquiryMode, transEndListener);
            kplusSaleTrans.setECRProcReturnListener(actionEndListener);
            if (isExecuteNeeded) {
                kplusSaleTrans.execute();
            }
            return kplusSaleTrans;
        } else {
            if (isExecuteNeeded) {
                DialogUtils.showErrMessage(this, Utils.getString(R.string.linkpos_ecr_thaiqr_acquirer_disable),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
            }
            return null;
        }
    }

    @Override
    public void finish(ActionResult result) {
        // on user press NavBar-BackKey
        if(result.getRet() == TransResult.ERR_USER_CANCEL && (! isReturnedToEcrLinkPos) && EcrData.instance.isOnProcessing) {
            returnRejectResponse(ECR_RESP_EDC_REJECT);
        }
        super.finish(result);
    }

    @Override
    protected void onDestroy() {
        //Device.beepErr();
        super.onDestroy();
            switchLockScreenState(terminalLockState.MODE_DEFAULT);
    }

    @Override
    protected boolean onKeyBackDown() {
        Log.d(TAG, "Activity was closed by user press BackKey/NavBackKey manually");
        setResult(TransResult.ERR_USER_CANCEL,null);
        finish();
        return super.onKeyBackDown();
    }

    @Override
    protected void onTimerTick(long timeleft) {
        btn_cancel.setText(String.format("CANCEL\n(Automatic cancel in  %s Sec.)", timeleft));
        super.onTimerTick(timeleft);
    }

    @Override
    protected void onTimerFinish() {
        Log.d(TAG, "Activity was closed by timer automatically");
        Device.beepErr();
        DialogUtils.showErrMessage(this, Utils.getString(R.string.err_timeout),"", onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
        returnRejectResponse(ECR_RESP_EDC_REJECT);
        super.onTimerFinish();
    }


    private void TransactionCodeSwap(@NonNull byte[] newTransCode) {
        if(newTransCode.length == 2 ) {
            Log.d(TAG, String.format("TransCode has been change from  %s  to  %s ", String.valueOf(HyperComMsg.instance.getTransactionCode()), String.valueOf(newTransCode)) );
            HyperComMsg.instance.setTransactionCode(newTransCode);
        }
    }




    private void returnRejectResponse(byte[] respCode) {
        if (mHyperComm != null) {
            try {
                if(HyperComMsg.instance.getTransactionCode() !=null) {
                    //if( ! isReturnedToEcrLinkPos) {
                    EcrData.instance.RespCode = respCode;
                    mHyperComm.cancelByHostSendResponse(HyperComMsg.instance.getTransactionCode(), 0);
                    Log.d(TAG, " <------ [REJECT]     data was sent to POS -- Successful");
                    //} else {
                    //    Log.d(TAG, " <------ [REJECT]     data has already sent.");
                    //}
                } else {
                    Log.d(TAG, " cannot get original transaction code from HyperComMsg Class");
                }
            } catch (Exception ex){
                ex.printStackTrace();
                Log.d(TAG, "Error during send [REJECT] back to PoS");
            }
        } else {
            Log.d(TAG, "mHypercom = null ");
        }
        EcrData.instance.isOnProcessing=false;
    }

    private void returnSaleResponse () {
        if (mHyperComm != null) {
            try {
                if(HyperComMsg.instance.getTransactionCode() !=null) {
                    mHyperComm.saleTransactionSendResponse(HyperComMsg.instance.getTransactionCode(), 0);
                    Log.d(TAG, " <------ [SALE RESPONSE]     data was sent back to POS -- Successful");
                } else {
                    Log.d(TAG, " cannot get original transaction code from HyperComMsg Class");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error during send [SALE RESPONSE] back to PoS");
            }
        } else {
            Log.d(TAG, "mHypercom = null ");
        }
        EcrData.instance.isOnProcessing=false;
    }

    private void returnVoidResponse () {
        if (mHyperComm != null) {
            try {
                mHyperComm.voidTransactionSendResponse( 0);
                Log.d(TAG, " <------ [VOID RESPONSE]     data was sent back to POS -- Successful");
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error during send [VOID RESPONSE] back to PoS");
            }
        } else {
            Log.d(TAG, "mHypercom = null ");
        }
        EcrData.instance.isOnProcessing=false;
    }

    private void returnAuditReportResponse() {
        if (mHyperComm != null) {
            try {
                mHyperComm.auditReportTransactionSendResponse( HyperComMsg.instance.data_field_HN_nii,0);
                Log.d(TAG, " <------ [AUDIT-REPORT RESPONSE]     data was sent back to POS -- Successful");
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error during send [AUDIT-REPORT RESPONSE] back to PoS");
            }
        } else {
            Log.d(TAG, "mHypercom = null ");
        }
        EcrData.instance.isOnProcessing=false;

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == 4 || event.getKeyCode()==66 || event.getKeyCode()==67) {
            return false;
        } else {
            return true;
        }
    }
}
