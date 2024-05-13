/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.*;
import android.provider.Settings;

import androidx.annotation.StringRes;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.core.ATransaction;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintParam;
import com.pax.pay.trans.task.ClearReversalTask;
import com.pax.pay.trans.task.ClearTradeVoucherTask;
import com.pax.pay.trans.transmit.TransOnline;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.List;

import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinClearReversal;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinClearTransaction;
import th.co.bkkps.scbapi.trans.action.ActionScbIppLink;

public class OtherManageFragment extends BasePreferenceFragment {

    private boolean isFirst = FinancialApplication.getController().isFirstRun();
    private boolean isDolfinSuccess;
    private boolean isScbIppSuccess;
    private String selAcq;

    @Override
    protected int getResourceId() {
        return R.xml.other_manage_pref;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int resId = preference.getTitleRes();
        switch (resId) {
            case R.string.om_clearTrade_menu_reversal:
                clearFunc(resId);
                break;
            case R.string.om_clearTrade_menu_trade_voucher:
                clearFunc(resId);
                break;
            case R.string.om_clearTrade_menu_key:
                clearPedKey();
                break;
            case R.string.om_clearTrade_menu_erm_sessionkey:
                clearErmSessionKey();
                break;
            case R.string.om_download_menu_echo_test:
                downloadFunc(resId);
                break;
            case R.string.om_paramPrint_menu_print_aid_para:
            case R.string.om_paramPrint_menu_print_capk_para:
            case R.string.om_paramPrint_menu_print_card_range_list:
                paraPrint(resId);
                break;
            case R.string.go_system_setting_date:
                Device.enableBackKey(true);
                Utils.callSystemSettings(getActivity(), Settings.ACTION_DATE_SETTINGS);
                break;
            case R.string.go_system_settings:
                Device.enableBackKey(true);
                Utils.callSystemSettings(getActivity(), Settings.ACTION_SETTINGS);
                break;
            default:
                return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    private  void clearErmSessionKey () {
        final CustomAlertDialog dialog = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                boolean delResult = FinancialApplication.getEReceiptDataDbHelper().deleteErmSessionKey();
                if(delResult) {
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            DialogUtils.showSuccMessage(getActivity(), "", null,
                                    Constants.SUCCESS_DIALOG_SHOW_TIME);
                        }
                    });
                } else {
                    Device.beepErr();
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
    }

    private void clearPedKey() {
        final CustomAlertDialog dialog = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {

                List<Acquirer> listAcquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
                for (Acquirer acq : listAcquirers) {
                    acq.setTMK(null);
                    acq.setTWK(null);
                    FinancialApplication.getAcqManager().updateAcquirer(acq);
                }

                boolean isDone = Device.eraseKeys();
                if (isDone) {
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            DialogUtils.showSuccMessage(getActivity(), "", null,
                                    Constants.SUCCESS_DIALOG_SHOW_TIME);
                        }
                    });

                } else {
                    Device.beepErr();
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void initPreference() {
        //do nothing
    }

    @Override
    protected boolean onEditTextPreferenceChanged(EditTextPreference preference, Object value, boolean isInitLoading) {
        return false;
    }

    @Override
    protected boolean onRingtonePreferenceChanged(RingtonePreference preference, Object value, boolean isInitLoading) {
        return false;
    }

    @Override
    protected boolean onCheckBoxPreferenceChanged(CheckBoxPreference preference, Object value, boolean isInitLoading) {
        return false;
    }

    @Override
    protected boolean onListPreferenceChanged(ListPreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    @Override
    protected boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    private void clearFunc(@StringRes final int resId) {
        final boolean isDone;

        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        final String[] acqName = new String[acquirers.size() + 1];
        acqName[0] = getString(R.string.acq_all_acquirer);
        for (int i = 0; i < acquirers.size(); i++) {
            acqName[i+1] = acquirers.get(i).getName();
        }

        switch (resId) {
            case R.string.om_clearTrade_menu_reversal:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.acquirer_list));
                builder.setSingleChoiceItems(acqName, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selAcq = acqName[which];
                        dialog.dismiss();
                        clearReversalFunc();
                    }
                });
                builder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
            case R.string.om_clearTrade_menu_trade_voucher:
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());
                mBuilder.setTitle(getString(R.string.acquirer_list));
                mBuilder.setSingleChoiceItems(acqName, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selAcq = acqName[which];
                        dialog.dismiss();
                        clearTradeFunc();
                    }
                });
                mBuilder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mBuilder.create().show();
                break;
            case R.string.om_clearTrade_menu_key:
                CustomAlertDialog dialog2 = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);
                isDone = Device.eraseKeys();
                showResultDialog(isDone, dialog2);
                break;
//            case R.string.om_clearTrade_menu_erm_sessionkey:
//                CustomAlertDialog dialog2 = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);
//                isDone = Device.eraseKeys();
//                showResultDialog(isDone, dialog2);
//                break;
            default:
                isDone = false;
                showResultDialog(isDone, DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1));
                break;

        }
    }

    private void clearReversalFunc() {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtils.showConfirmDialog(getActivity(), getString(R.string.receipt_host_colon) + selAcq + "\n" + getString(R.string.dialog_confirm_clear_reversal), new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        alertDialog.dismiss();
                    }
                }, new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        alertDialog.dismiss();
                        if (selAcq != null) {
                            final CustomAlertDialog dialog = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);
                            boolean isDone;
                            if (selAcq.equals(getString(R.string.acq_all_acquirer))) {
                                isDone = FinancialApplication.getTransDataDbHelper().deleteAllDupRecord();
                                if(isDone){
                                    Acquirer acqDolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
                                    Acquirer acqScbIpp = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
                                    if(acqDolfin!= null && acqDolfin.isEnable() && DolfinApi.getInstance().getDolfinServiceBinded()){
                                        isDone = processDolfinClearReversal();
                                    }
//                                    if(isDone) {
//                                        if(acqScbIpp != null) {
//                                            isDone = processScbIppClearReversal();
//                                        }
//                                    }

                                }
                            } else {
                                Acquirer selAcquirer = FinancialApplication.getAcqManager().findAcquirer(selAcq);
                                if (selAcquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN) && DolfinApi.getInstance().getDolfinServiceBinded()) {
                                    isDone = processDolfinClearReversal();
                                }
                                //else if (selAcquirer.getName().equalsIgnoreCase(Constants.ACQ_SCB_IPP) ) {
                                    //isDone = processScbIppClearReversal();
                                //}
                                else {
                                        isDone = FinancialApplication.getTransDataDbHelper().deleteDupRecord(selAcquirer);
                                }
                            }
                            showResultDialog(isDone, dialog);
                        }
                    }
                });
            }
        });
    }

    private void clearTradeFunc() {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtils.showConfirmDialog(getActivity(), getString(R.string.receipt_host_colon) + selAcq + "\n" + getString(R.string.dialog_confirm_clear_trade), new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                    alertDialog.dismiss();
                    }
                }, new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                    alertDialog.dismiss();
                    if (selAcq != null) {
                        final CustomAlertDialog dialog = DialogUtils.showProcessingMessage(getActivity(), getActivity().getString(R.string.wait_process), -1);

                        Component.setSettleStatus(Controller.Constant.WORKED, selAcq);

                        boolean isDone;
                        if (selAcq.equals(getString(R.string.acq_all_acquirer))) {
                            isDone = FinancialApplication.getTransDataDbHelper().deleteAllTransData();
//                            FinancialApplication.getTransTotalDbHelper().deleteAllTransTotal();
                            if(isDone){
                                Acquirer acqDolfin = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
                                if(acqDolfin!= null && acqDolfin.isEnable() && DolfinApi.getInstance().getDolfinServiceBinded()){
                                    isDone = processDolfinClearTransaction();
                                }
                            }
                        } else {
                            Acquirer selAcquirer = FinancialApplication.getAcqManager().findAcquirer(selAcq);
                            isDone = FinancialApplication.getTransDataDbHelper().deleteAllTransData(selAcquirer, true);
                            if(isDone && selAcquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN) && DolfinApi.getInstance().getDolfinServiceBinded()){
                                isDone = processDolfinClearTransaction();
                            }
//                            FinancialApplication.getTransTotalDbHelper().deleteAllTransTotal(selAcquirer);
                        }
                        FinancialApplication.getController().set(Controller.CLEAR_LOG, Controller.Constant.NO);
                        showResultDialog(isDone, dialog);
                    }
                    }
                });
            }
        });
    }

    private void downloadFunc(@StringRes final int resId) {
        if (isFirst) { //AET-269
            ToastUtils.showMessage(R.string.wait_2_init_device);
            return;
        }
        FinancialApplication.getApp().runInBackground(new Runnable() {

            @Override
            public void run() {
                int ret = -1;
                TransProcessListener listenerImpl = new TransProcessListenerImpl(getActivity());

                if (R.string.om_download_menu_echo_test == resId) {
                    ret = new TransOnline().echo(listenerImpl);
                }

                listenerImpl.onHideProgress();

                if (ret == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(getActivity(), "", null,
                            Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else if (ret != TransResult.ERR_ABORTED && ret != TransResult.ERR_HOST_REJECT) {
                    listenerImpl.onShowErrMessage(TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                // ERR_ABORTED AND ERR_HOST_REJECT 之前已提示错误信息， 此处不需要再提示
            }
        });

    }

    //AET-133
    private void paraPrint(@StringRes final int resId) {
        FinancialApplication.getApp().runInBackground(new Runnable() {

            @Override
            public void run() {

                ReceiptPrintParam receiptPrintParam = new ReceiptPrintParam();
                switch (resId) {
                    case R.string.om_paramPrint_menu_print_aid_para:
                        receiptPrintParam.print(ReceiptPrintParam.AID, new PrintListenerImpl(getActivity()));
                        break;
                    case R.string.om_paramPrint_menu_print_capk_para:
                        receiptPrintParam.print(ReceiptPrintParam.CAPK, new PrintListenerImpl(getActivity()));
                        break;
                    case R.string.om_paramPrint_menu_print_card_range_list:
                        receiptPrintParam.print(ReceiptPrintParam.CARD_RANGE, new PrintListenerImpl(getActivity()));
                        break;
                    default:
                        break;
                }
                Device.beepOk();
            }
        });
    }

    private void showResultDialog (boolean isDone, CustomAlertDialog dialog) {
        final boolean isSucc = isDone;
        final CustomAlertDialog alertDialog = dialog;

        FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
            @Override
            public void run() {
                alertDialog.dismiss();

                if (isSucc) {
                    DialogUtils.showSuccMessage(getActivity(), "", null,
                            Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    Device.beepErr();
                }
            }
        }, 3000);
    }

    @Override
    public void onResume() {
        Device.enableBackKey(false);
        super.onResume();
    }
    
    private ATransaction.TransEndListener genTransEndListener(final CustomAlertDialog dialog) {
        return result -> {
            if (dialog != null) {
                FinancialApplication.getApp().runOnUiThreadDelay(() -> {
                    dialog.dismiss();
                    if (result.getRet() == TransResult.SUCC) {
                        DialogUtils.showSuccMessage(getActivity(), "", null,
                                Constants.SUCCESS_DIALOG_SHOW_TIME);
                    } else {
                        Device.beepErr();
                    }
                }, 3000);
            }
        };
    }
    

    private boolean processDolfinClearTransaction(){
        ActionDolfinClearTransaction actionClearTransaction = new ActionDolfinClearTransaction(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/}
        });
        actionClearTransaction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                isDolfinSuccess = result.getData() != null && (Integer) result.getData() == 0;
                ActivityStack.getInstance().popTo(getActivity());
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
            }
        });
        actionClearTransaction.execute();
        return isDolfinSuccess;
    }

    private boolean processDolfinClearReversal(){
        ActionDolfinClearReversal actionClearReversal = new ActionDolfinClearReversal(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {/*do nothing*/}
        });
        actionClearReversal.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                isDolfinSuccess = result.getData() != null && (Integer) result.getData() == 0;
                ActivityStack.getInstance().popTo(getActivity());
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
            }
        });
        actionClearReversal.execute();
        return isDolfinSuccess;
    }

    private boolean processScbIppClearReversal(){
        ActionScbIppLink actionScbIppLink = new ActionScbIppLink(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionScbIppLink) action).setParam(getActivity(), ActionScbIppLink.scbIppLinkType.CLEAR_REVERSAL, Constants.ACQ_SCB_IPP);
            }
        });
        actionScbIppLink.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                isScbIppSuccess =  (result.getRet() == TransResult.SUCC);
            }
        });
        actionScbIppLink.execute();

        return isScbIppSuccess;
    }
}
