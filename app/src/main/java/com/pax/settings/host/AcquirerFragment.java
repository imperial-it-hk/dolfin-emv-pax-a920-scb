/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-10
 * Module Author: xiawh
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings.host;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import th.co.bkkps.utils.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.TextValueWatcher;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.BaseFragment;
import com.pax.settings.NewSpinnerAdapter;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.keyboard.KeyboardUtils;

import java.util.Arrays;
import java.util.List;

public class AcquirerFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener {

    private Acquirer acquirer;

    private NewSpinnerAdapter<Acquirer> adapter;

    private EditText etTerminalId;
    private EditText etMerchantId;
    private EditText etNii;
    private EditText etBatch;
    private EditText etIp;
    private EditText etIp_2nd;
    private EditText etIp_3rd;
    private EditText etIp_4th;
    private EditText etPort;
    private EditText etPort_2nd;
    private EditText etPort_3rd;
    private EditText etPort_4th;
    private EditText etQrTimeout;
    private EditText etHostTimeout;
    private EditText etRetryPromptTimeout;
    private EditText etBillerIdPromptPay;
    private EditText etBillerServicePromptPay;
    private EditText etForceSettleTime;
    private EditText etInstalmentMinAmt;
    private EditText etInstalmentTerms;
    private CheckBox isDefault;
    private CheckBox enableTrickFeed;
    private CheckBox enableKeyIn;
    private CheckBox enableQr;
    private CheckBox enableTle;
    private CheckBox enableUpi;
    private CheckBox enableUploadERM;
    private CheckBox testMode;
    private CheckBox enableTcAdvice;
    private CheckBox enableSmallAmt;
    private CheckBox signatureRequired;
    private TextView txtBillerId;
    private TextView txtInstalmentMinAmt;
    private View vQrTimeoutLayout;
    private View vHostTimeoutLayout;
    private View vRetryTimeoutLayout;
    private View vEnableTrickFeddLayout;
    private View vEnableKeyInLayout;
    private View vEnableQrLayout;
    private View vBillerServiceLayout;
    private View vForceSettleTimeLayout;
    private View vInstalmentTermsLayout;
    private View vSignatureRequiredLayout;
    private EditText etStoreId;
    private TextView txtStoreId;
    private EditText etStoreName;
    private TextView txtStoreName;
    private CheckBox enableControlLimit;
    private CheckBox enableInputPhoneNumber;
    //AET-63
    private boolean isFirst = true;

    //Dolfin
    private View vDolfinCScanB;
    private TextView txtDolfinCScanB;
    private TextView txtDolfinEnableCScanB;
    private CheckBox dolfinEnableCScanB;
    private TextView txtDolfinDisplayQrTimeout;
    private EditText etDolfinDisplayQrTimeout;
    private TextView txtDolfinRetryTimes;
    private EditText etDolfinRetryTimes;
    private TextView txtDolfinDelayRetry;
    private EditText etDolfinDelayRetry;


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_acquirer_details;
    }

    @Override
    protected void initData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            String acqName = bundle.getString(EUIParamKeys.ACQUIRER_NAME.toString());
            acquirer = FinancialApplication.getAcqManager().findAcquirer(acqName);
        }

        //List<Acquirer> listAcquirers = FinancialApplication.getAcqManager().findAllAcquirers();
        List<Acquirer> listAcquirers = FinancialApplication.getAcqManager().findEnableAcquirersExcept(Arrays.asList(Constants.ACQ_KCHECKID));

        if(listAcquirers != null && listAcquirers.size() > 0){
            if (acquirer == null) {
                acquirer = listAcquirers.isEmpty() ? new Acquirer() : listAcquirers.get(0);
            }

            adapter = new NewSpinnerAdapter<>(this.context);
            adapter.setListInfo(listAcquirers);
            adapter.setOnTextUpdateListener(new NewSpinnerAdapter.OnTextUpdateListener() {
                @Override
                public String onTextUpdate(final List<?> list, int position) {
                    boolean enabled_val = (((Acquirer) list.get(position)).getName().equals(Constants.ACQ_QR_CREDIT)) ;
                    int visibility_val = (enabled_val) ? View.VISIBLE : View.GONE ;
                    vForceSettleTimeLayout.setEnabled(enabled_val);
                    vForceSettleTimeLayout.setVisibility(visibility_val);

                    return ((Acquirer) list.get(position)).getName();
                }
            });
        }else{
            showErrMsgDialog();
        }
        /*adapter = new NewSpinnerAdapter<>(this.context);
        adapter.setListInfo(listAcquirers);
        adapter.setOnTextUpdateListener(new NewSpinnerAdapter.OnTextUpdateListener() {
            @Override
            public String onTextUpdate(final List<?> list, int position) {
                return ((Acquirer) list.get(position)).getName();
            }
        });*/
    }

    @Override
    protected void initView(View view) {
        Spinner spinner = (Spinner) view.findViewById(R.id.acquirer_list);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                Acquirer newAcquirer = adapter.getListInfo().get(pos);
                if (newAcquirer.getId() != acquirer.getId()) {
                    //AET-36
                    //FinancialApplication.getAcqManager().updateAcquirer(acquirer);
                    onUpdateAcquirer(acquirer);
                    acquirer = newAcquirer;
                    updateItemsValue();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        if (!adapter.getListInfo().isEmpty()) {
            updateItems(view);
            updateItemsValue();
        } else {
            view.findViewById(R.id.acquirer_details).setVisibility(View.GONE);
        }

        spinner = (Spinner) view.findViewById(R.id.acquirer_ssl_type);
        final String[] sslType = getResources().getStringArray(R.array.acq_ssl_type_list_entries);
        String currentType = acquirer.getSslType().toString();
        // init spinner selected index
        for (int i = 0; i < sslType.length; i++) {
            if (TextUtils.equals(sslType[i], currentType)) {
                spinner.setSelection(i);
                break;
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!sslType[pos].equals(acquirer.getSslType().toString())) {
                    //AET-36
                    SysParam.Constant.CommSslType[] sslTypes = SysParam.Constant.CommSslType.values();
                    for (SysParam.Constant.CommSslType i : sslTypes) {
                        if (i.toString().equals(sslType[pos])) {
                            acquirer.setSslType(i);
                            FinancialApplication.getAcqManager().updateAcquirer(acquirer);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    private void updateItems(View view) {
        isDefault = (CheckBox) view.findViewById(R.id.acquirer_is_default);
        isDefault.setOnCheckedChangeListener(AcquirerFragment.this);

        etTerminalId = (EditText) view.findViewById(R.id.terminal_id);
        etTerminalId.addTextChangedListener(new Watcher(R.id.terminal_id));

        etMerchantId = (EditText) view.findViewById(R.id.merchant_id);
        etMerchantId.addTextChangedListener(new Watcher(R.id.merchant_id));

        etNii = (EditText) view.findViewById(R.id.nii_acq);
        etNii.addTextChangedListener(new Watcher(R.id.nii_acq));

        etBatch = (EditText) view.findViewById(R.id.batch_num);
        etBatch.addTextChangedListener(new Watcher(R.id.batch_num));

        etIp = (EditText) view.findViewById(R.id.acq_ip);
        etIp.addTextChangedListener(new Watcher(R.id.acq_ip));
        etIp_2nd = (EditText) view.findViewById(R.id.acq_ip_2nd);
        etIp_2nd.addTextChangedListener(new Watcher(R.id.acq_ip_2nd));
        etIp_3rd = (EditText) view.findViewById(R.id.acq_ip_3rd);
        etIp_3rd.addTextChangedListener(new Watcher(R.id.acq_ip_3rd));
        etIp_4th = (EditText) view.findViewById(R.id.acq_ip_4th);
        etIp_4th.addTextChangedListener(new Watcher(R.id.acq_ip_4th));



        etPort = (EditText) view.findViewById(R.id.acq_ip_port);
        TextValueWatcher<Integer> textValueWatcher = new TextValueWatcher<>(0, 65535);
        textValueWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        textValueWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPort(Integer.parseInt(value));
            }
        });
        etPort.addTextChangedListener(textValueWatcher);
        etPort.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d("silly muhua", "物理按键的Enter, 关闭软键盘");
                        KeyboardUtils.hideSystemKeyboard(getActivity(), etPort);
                        return true;
                    }
                }
                return false;
            }
        });
        etPort_2nd = (EditText) view.findViewById(R.id.acq_ip_port_2nd);
        TextValueWatcher<Integer> textValueWatcher_2nd = new TextValueWatcher<>(0, 65535);
        textValueWatcher_2nd.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        textValueWatcher_2nd.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPortBak1(Integer.parseInt(value));
            }
        });
        etPort_2nd.addTextChangedListener(textValueWatcher_2nd);
        etPort_2nd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d("silly muhua", "物理按键的Enter, 关闭软键盘");
                        KeyboardUtils.hideSystemKeyboard(getActivity(), etPort_2nd);
                        return true;
                    }
                }
                return false;
            }
        });
        etPort_3rd = (EditText) view.findViewById(R.id.acq_ip_port_3rd);
        TextValueWatcher<Integer> textValueWatcher_3rd = new TextValueWatcher<>(0, 65535);
        textValueWatcher_3rd.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        textValueWatcher_3rd.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPortBak2(Integer.parseInt(value));
            }
        });
        etPort_3rd.addTextChangedListener(textValueWatcher_3rd);
        etPort_3rd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d("silly muhua", "物理按键的Enter, 关闭软键盘");
                        KeyboardUtils.hideSystemKeyboard(getActivity(), etPort_3rd);
                        return true;
                    }
                }
                return false;
            }
        });
        etPort_4th = (EditText) view.findViewById(R.id.acq_ip_port_4th);
        TextValueWatcher<Integer> textValueWatcher_4th = new TextValueWatcher<>(0, 65535);
        textValueWatcher_4th.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        textValueWatcher_4th.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPortBak3(Integer.parseInt(value));
            }
        });
        etPort_4th.addTextChangedListener(textValueWatcher_4th);
        etPort_4th.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d("silly muhua", "物理按键的Enter, 关闭软键盘");
                        KeyboardUtils.hideSystemKeyboard(getActivity(), etPort_4th);
                        return true;
                    }
                }
                return false;
            }
        });

        etQrTimeout = (EditText) view.findViewById(R.id.acq_qr_timeout);
        etQrTimeout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etQrTimeout.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> qrTimeoutWatcher = new TextValueWatcher<>(1, 99);
        qrTimeoutWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        qrTimeoutWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPromptQrTimeout(Integer.parseInt(value));
            }
        });
        etQrTimeout.addTextChangedListener(qrTimeoutWatcher);

        etHostTimeout = (EditText) view.findViewById(R.id.acq_host_timeout);
        etHostTimeout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etHostTimeout.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> hostTimeoutWatcher = new TextValueWatcher<>(1, 99);
        hostTimeoutWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        hostTimeoutWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setRecvTimeout(Integer.parseInt(value));
            }
        });
        etHostTimeout.addTextChangedListener(hostTimeoutWatcher);

        etRetryPromptTimeout = (EditText) view.findViewById(R.id.acq_retry_timeout);
        etRetryPromptTimeout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etRetryPromptTimeout.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> retryTimeoutWatcher = new TextValueWatcher<>(1, 99);
        retryTimeoutWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        retryTimeoutWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setPromptRetryTimeout(Integer.parseInt(value));
            }
        });
        etRetryPromptTimeout.addTextChangedListener(retryTimeoutWatcher);

        etForceSettleTime = (EditText) view.findViewById(R.id.acquirer_force_settle_time_textbox);
        etForceSettleTime.addTextChangedListener(new Watcher(R.id.acquirer_force_settle_time_textbox));

        enableTrickFeed = (CheckBox) view.findViewById(R.id.acquirer_disable_trick_feed);
        enableTrickFeed.setOnCheckedChangeListener(AcquirerFragment.this);

        enableKeyIn = (CheckBox) view.findViewById(R.id.acq_support_keyin);
        enableKeyIn.setOnCheckedChangeListener(AcquirerFragment.this);

        enableQr = (CheckBox) view.findViewById(R.id.acq_support_qr);
        enableQr.setOnCheckedChangeListener(AcquirerFragment.this);

        enableTle = (CheckBox) view.findViewById(R.id.acq_support_tle);
        enableTle.setOnCheckedChangeListener(AcquirerFragment.this);

        enableUpi = (CheckBox) view.findViewById(R.id.acq_support_upi);
        enableUpi.setOnCheckedChangeListener(AcquirerFragment.this);

        boolean ErcmMainFlag = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE);
        enableUploadERM = (CheckBox) view.findViewById(R.id.acq_support_erm);
        enableUploadERM.setEnabled(ErcmMainFlag);
        enableUploadERM.setOnCheckedChangeListener(AcquirerFragment.this);



        /*testMode = (CheckBox) view.findViewById(R.id.acq_test_mode);
        testMode.setOnCheckedChangeListener(AcquirerFragment.this);*/

        enableTcAdvice = (CheckBox) view.findViewById(R.id.acq_support_tc_advice);
        enableTcAdvice.setOnCheckedChangeListener(AcquirerFragment.this);

        enableSmallAmt = (CheckBox) view.findViewById(R.id.acq_support_small_amt);
        enableSmallAmt.setOnCheckedChangeListener(AcquirerFragment.this);

        signatureRequired = view.findViewById(R.id.acq_signature_required);
        signatureRequired.setOnCheckedChangeListener(AcquirerFragment.this);

        etBillerIdPromptPay = (EditText) view.findViewById(R.id.biller_id_promptpay);
        etBillerIdPromptPay.addTextChangedListener(new Watcher(R.id.biller_id_promptpay));

        etBillerServicePromptPay = (EditText) view.findViewById(R.id.biller_service_promptpay);
        etBillerServicePromptPay.addTextChangedListener(new Watcher(R.id.biller_service_promptpay));

        etInstalmentMinAmt = (EditText) view.findViewById(R.id.instalment_min_amount);
        etInstalmentMinAmt.addTextChangedListener(new Watcher(R.id.instalment_min_amount));
        txtInstalmentMinAmt= (TextView) view.findViewById(R.id.instalment_min_amount_txt);
        etInstalmentTerms = (EditText) view.findViewById(R.id.instalment_terms);
        etInstalmentTerms.setEnabled(false);

        txtBillerId = (TextView) view.findViewById(R.id.biller_id_txt);

        vQrTimeoutLayout = view.findViewById(R.id.acq_qr_timeout_layout);
        vHostTimeoutLayout = view.findViewById(R.id.acq_host_timeout_layout);
        vRetryTimeoutLayout = view.findViewById(R.id.acq_retry_timeout_layout);
        vEnableTrickFeddLayout = view.findViewById(R.id.acq_disable_trick_feed_layout);
        vEnableKeyInLayout = view.findViewById(R.id.acq_support_keyin_layout);
        vEnableQrLayout = view.findViewById(R.id.acq_support_qr_layout);
        vBillerServiceLayout = view.findViewById(R.id.acq_biller_service_layout);
        vForceSettleTimeLayout =  view.findViewById(R.id.acquirer_force_settle_time_layout);
        vInstalmentTermsLayout = view.findViewById(R.id.instalment_terms_layout);
        vSignatureRequiredLayout = view.findViewById(R.id.acq_signature_required_layout);

        etStoreId = (EditText) view.findViewById(R.id.store_id);
        etStoreId.addTextChangedListener(new Watcher(R.id.store_id));
        txtStoreId  = (TextView) view.findViewById(R.id.store_id_txt);

        etStoreName = (EditText) view.findViewById(R.id.store_name);
        etStoreName.addTextChangedListener(new Watcher(R.id.store_name));
        txtStoreName  = (TextView) view.findViewById(R.id.store_name_txt);

        enableControlLimit = (CheckBox) view.findViewById(R.id.acq_enable_control_limit);
        enableInputPhoneNumber = (CheckBox) view.findViewById(R.id.acq_enable_input_phone_number);
        // set checked
        enableControlLimit.setChecked(acquirer.getEnableControlLimit());
        enableInputPhoneNumber.setChecked(acquirer.getEnablePhoneNumberInput());
        enableInputPhoneNumber.setEnabled(acquirer.getEnableControlLimit());

        enableControlLimit.setOnCheckedChangeListener(AcquirerFragment.this);
        enableInputPhoneNumber.setOnCheckedChangeListener(AcquirerFragment.this);

        //Dolfin
        vDolfinCScanB  = (View) view.findViewById(R.id.acq_c_scan_b_line);
        txtDolfinCScanB  = (TextView) view.findViewById(R.id.acq_c_scan_b);

        txtDolfinEnableCScanB  = (TextView) view.findViewById(R.id.acq_c_scan_b_enable_txt);
        dolfinEnableCScanB = (CheckBox) view.findViewById(R.id.acq_enable_c_scan_b);
        dolfinEnableCScanB.setOnCheckedChangeListener(AcquirerFragment.this);

        txtDolfinDisplayQrTimeout  = (TextView) view.findViewById(R.id.dolfin_display_qr_timeout_txt);
        etDolfinDisplayQrTimeout = (EditText) view.findViewById(R.id.dolfin_display_qr_timeout);
        etDolfinDisplayQrTimeout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etDolfinDisplayQrTimeout.setCursorVisible(true);
                return false;            }
        });
        TextValueWatcher<Integer> timeoutWatcher = new TextValueWatcher<>(1, 99);
        timeoutWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        timeoutWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setCScanBDisplayQrTimeout(Integer.parseInt(value));
                FinancialApplication.getSysParam().set(SysParam.NumberParam.DOLFIN_QR_ON_SCREEN_TIMEOUT, Integer.parseInt(value));
            }
        });
        etDolfinDisplayQrTimeout.addTextChangedListener(timeoutWatcher);


        txtDolfinRetryTimes  = (TextView) view.findViewById(R.id.dolfin_retry_times_txt);
        etDolfinRetryTimes = (EditText) view.findViewById(R.id.dolfin_retry_times);
        etDolfinRetryTimes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etDolfinRetryTimes.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> retryTimesWatcher = new TextValueWatcher<>(1, 99);
        retryTimesWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        retryTimesWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setCScanBRetryTimes(Integer.parseInt(value));
            }
        });
        etDolfinRetryTimes.addTextChangedListener(retryTimesWatcher);

        txtDolfinDelayRetry  = (TextView) view.findViewById(R.id.dolfin_retry_delay_txt);
        etDolfinDelayRetry = (EditText) view.findViewById(R.id.dolfin_retry_delay);
        etDolfinDelayRetry.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etDolfinDelayRetry.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> delayWatcher = new TextValueWatcher<>(1, 99);
        delayWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        delayWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                acquirer.setCScanBDelayRetry(Integer.parseInt(value));
            }
        });
        etDolfinDelayRetry.addTextChangedListener(delayWatcher);
    }

    private void updateItemsValue() {
        if (acquirer == null)
            return;
        isDefault.setChecked(acquirer.getId() == FinancialApplication.getAcqManager().getCurAcq().getId());

        etTerminalId.setText(acquirer.getTerminalId(), BufferType.EDITABLE);

        etMerchantId.setText(acquirer.getMerchantId(), BufferType.EDITABLE);

        etNii.setText(acquirer.getNii(), BufferType.EDITABLE);

        String szBatchNo = Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6);
        etBatch.setText(szBatchNo, BufferType.EDITABLE);

        etIp.setText(acquirer.getIp());
        etIp_2nd.setText(acquirer.getIpBak1());
        etIp_3rd.setText(acquirer.getIpBak2());
        etIp_4th.setText(acquirer.getIpBak3());

        etPort.setText(String.valueOf(acquirer.getPort()));
        etPort_2nd.setText(String.valueOf(acquirer.getPortBak1()));
        etPort_3rd.setText(String.valueOf(acquirer.getPortBak2()));
        etPort_4th.setText(String.valueOf(acquirer.getPortBak3()));

        if (acquirer.getName().equals(Constants.ACQ_QR_CREDIT)) {
            etForceSettleTime.setText(acquirer.getForceSettleTime());
        }

        enableTrickFeed.setChecked(acquirer.isDisableTrickFeed());

        enableKeyIn.setChecked(acquirer.isEnableKeyIn());

        enableQr.setChecked(acquirer.isEnableQR());

        enableTle.setChecked(acquirer.isEnableTle());

        enableUpi.setChecked(acquirer.isEnableUpi());

        enableUploadERM.setChecked(acquirer.isEnableUploadERM());

        //testMode.setChecked(acquirer.isTestMode());

        enableTcAdvice.setChecked(acquirer.isEmvTcAdvice());

        enableSmallAmt.setChecked(acquirer.isEnableSmallAmt());

        if(acquirer.getBillerIdPromptPay() != null && !acquirer.getBillerIdPromptPay().isEmpty()){
            txtBillerId.setVisibility(View.VISIBLE);
            etBillerIdPromptPay.setText(acquirer.getBillerIdPromptPay(), BufferType.EDITABLE);
            etBillerIdPromptPay.setVisibility(View.VISIBLE);
            etBillerServicePromptPay.setText(acquirer.getBillerServiceCode(), BufferType.EDITABLE);
            vBillerServiceLayout.setVisibility(View.VISIBLE);
            etHostTimeout.setText(String.valueOf(acquirer.getRecvTimeout()));
            vHostTimeoutLayout.setVisibility(View.VISIBLE);
            etQrTimeout.setText(String.valueOf(acquirer.getPromptQrTimeout()));
            vQrTimeoutLayout.setVisibility(View.VISIBLE);
            etRetryPromptTimeout.setText(String.valueOf(acquirer.getPromptRetryTimeout()));
            vRetryTimeoutLayout.setVisibility(View.VISIBLE);
            vEnableTrickFeddLayout.setVisibility(View.GONE);
            vEnableKeyInLayout.setVisibility(View.GONE);
            vEnableQrLayout.setVisibility(View.GONE);
        }else{
            txtBillerId.setVisibility(View.GONE);
            etBillerIdPromptPay.setVisibility(View.GONE);
            vBillerServiceLayout.setVisibility(View.GONE);
            vHostTimeoutLayout.setVisibility(View.GONE);
            vQrTimeoutLayout.setVisibility(View.GONE);
            vRetryTimeoutLayout.setVisibility(View.GONE);
            vEnableTrickFeddLayout.setVisibility(View.VISIBLE);
            vEnableKeyInLayout.setVisibility(View.VISIBLE);
            vEnableQrLayout.setVisibility(View.VISIBLE);
        }

        if(acquirer.getInstalmentMinAmt() != null && !acquirer.getInstalmentMinAmt().isEmpty()){
            txtInstalmentMinAmt.setVisibility(View.VISIBLE);
            etInstalmentMinAmt.setVisibility(View.VISIBLE);
            etInstalmentMinAmt.setText(acquirer.getInstalmentMinAmt(), BufferType.EDITABLE);
            vInstalmentTermsLayout.setVisibility(View.VISIBLE);
            etInstalmentTerms.setText(acquirer.getInstalmentTerms());
        }else{
            txtInstalmentMinAmt.setVisibility(View.GONE);
            etInstalmentMinAmt.setVisibility(View.GONE);
            vInstalmentTermsLayout.setVisibility(View.GONE);
        }

        if(acquirer.getStoreId() != null && !acquirer.getStoreId().isEmpty()){
            txtStoreId.setVisibility(View.VISIBLE);
            etStoreId.setVisibility(View.VISIBLE);
            etStoreId.setText(acquirer.getStoreId(), BufferType.EDITABLE);
            txtStoreName.setVisibility(View.VISIBLE);
            etStoreName.setVisibility(View.VISIBLE);
            etStoreName.setText(acquirer.getStoreName(), BufferType.EDITABLE);
        }else{
            etStoreId.setVisibility(View.GONE);
            txtStoreId.setVisibility(View.GONE);
            etStoreName.setVisibility(View.GONE);
            txtStoreName.setVisibility(View.GONE);
        }

        enableInputPhoneNumber.setEnabled(enableControlLimit.isChecked());
        if(acquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN)){
            vDolfinCScanB.setVisibility(View.VISIBLE);
            txtDolfinCScanB.setVisibility(View.VISIBLE);

            txtDolfinEnableCScanB.setVisibility(View.VISIBLE);
            dolfinEnableCScanB.setVisibility(View.VISIBLE);
            dolfinEnableCScanB.setChecked(acquirer.isEnableCScanBMode());

            txtDolfinDisplayQrTimeout.setVisibility(View.VISIBLE);
            etDolfinDisplayQrTimeout.setVisibility(View.VISIBLE);
            etDolfinDisplayQrTimeout.setText(String.valueOf(acquirer.getCScanBDisplayQrTimeout()));


            txtDolfinRetryTimes.setVisibility(View.VISIBLE);
            etDolfinRetryTimes.setVisibility(View.VISIBLE);
            etDolfinRetryTimes.setText(String.valueOf(acquirer.getCScanBRetryTimes()));

            txtDolfinDelayRetry.setVisibility(View.VISIBLE);
            etDolfinDelayRetry.setVisibility(View.VISIBLE);
            etDolfinDelayRetry.setText(String.valueOf(acquirer.getCScanBDelayRetry()));
        } else {

            vDolfinCScanB.setVisibility(View.GONE);
            txtDolfinCScanB.setVisibility(View.GONE);

            txtDolfinEnableCScanB.setVisibility(View.GONE);
            dolfinEnableCScanB.setVisibility(View.GONE);

            txtDolfinDisplayQrTimeout.setVisibility(View.GONE);
            etDolfinDisplayQrTimeout.setVisibility(View.GONE);


            txtDolfinRetryTimes.setVisibility(View.GONE);
            etDolfinRetryTimes.setVisibility(View.GONE);

            txtDolfinDelayRetry.setVisibility(View.GONE);
            etDolfinDelayRetry.setVisibility(View.GONE);
        }

        if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())
                || Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
            signatureRequired.setChecked(acquirer.isSignatureRequired());
            vSignatureRequiredLayout.setVisibility(View.VISIBLE);
        } else {
            vSignatureRequiredLayout.setVisibility(View.GONE);
        }

        //AET-63
        isFirst = false;
    }

    @Override
    public void onClickProtected(View v) {
        //do nothing
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.acquirer_disable_trick_feed:
                acquirer.setDisableTrickFeed(isChecked);
                break;
            case R.id.acq_support_keyin:
                acquirer.setEnableKeyIn(isChecked);
                break;
            case R.id.acq_support_qr:
                acquirer.setEnableQR(isChecked);
                break;
            case R.id.acq_support_tle:
                acquirer.setEnableTle(isChecked);
                break;
            case R.id.acq_support_upi:
                acquirer.setEnableUpi(isChecked);
                break;
            case R.id.acq_support_erm:
                acquirer.setEnableUploadERM(isChecked);
                break;
            case R.id.acq_enable_control_limit:
                acquirer.setEnableControlLimit(isChecked);
                break;
            case R.id.acq_enable_input_phone_number:
                acquirer.setEnablePhoneNumberInput(isChecked);
                break;
            /*case R.id.acq_test_mode:
                acquirer.setTestMode(isChecked);
                break;*/
            case R.id.acq_support_tc_advice:
                acquirer.setEmvTcAdvice(isChecked);
                break;
            case R.id.acq_support_small_amt:
                acquirer.setEnableSmallAmt(isChecked);
                break;
            case R.id.acquirer_is_default:
                onSetDef(isChecked);
                break;
            case R.id.acq_signature_required:
                acquirer.setSignatureRequired(isChecked);
                break;
            case R.id.acq_enable_c_scan_b:
                acquirer.setEnableCScanBMode(isChecked);
                break;
            default:
                break;
        }
    }

    private void onSetDef(boolean isChecked) {
        if (isChecked && acquirer.getId() != FinancialApplication.getAcqManager().getCurAcq().getId()) {
            FinancialApplication.getAcqManager().setCurAcq(acquirer);
            FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, acquirer.getName());
        }
        //AET-63
        isFirst = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        //FinancialApplication.getAcqManager().updateAcquirer(acquirer);
        onUpdateAcquirer(acquirer);
        //AET-36
        String curAcqName = FinancialApplication.getAcqManager().getCurAcq().getName();
        FinancialApplication.getAcqManager().setCurAcq(FinancialApplication.getAcqManager().findAcquirer(curAcqName));
    }

    private class Watcher implements TextWatcher {
        final int id;

        Watcher(int id) {
            this.id = id;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //do nothing
        }

        @Override
        public void afterTextChanged(Editable s) {
            String content = s.toString();
            switch (id) {
                case R.id.terminal_id:
                    acquirer.setTerminalId(content);
                    break;
                case R.id.merchant_id:
                    acquirer.setMerchantId(content);
                    break;
                case R.id.nii_acq:
                    acquirer.setNii(content);
                    break;
                case R.id.batch_num:
                    updateBatchNo(content);
                    break;
                case R.id.acq_ip:     if (Utils.checkIp(content)) { acquirer.setIp(content);} break;
                case R.id.acq_ip_2nd: if (Utils.checkIp(content)) { acquirer.setIpBak1(content);} break;
                case R.id.acq_ip_3rd: if (Utils.checkIp(content)) { acquirer.setIpBak2(content);} break;
                case R.id.acq_ip_4th: if (Utils.checkIp(content)) { acquirer.setIpBak3(content);} break;
                case R.id.acquirer_force_settle_time_textbox: acquirer.setForceSettleTime(content); break;
                case R.id.biller_id_promptpay:
                    acquirer.setBillerIdPromptPay(content);
                    break;
                case R.id.biller_service_promptpay:
                    acquirer.setBillerServiceCode(content);
                    break;
                case R.id.instalment_min_amount:
                    acquirer.setInstalmentMinAmt(content);
                    break;
                case R.id.store_id:
                    if (content != null && !content.isEmpty() ){
                        acquirer.setStoreId(content);
                    }
                    break;
                case R.id.store_name:
                    if (content != null && !content.isEmpty() ){
                        acquirer.setStoreName(content);
                    }
                    break;
                default:
                    break;

            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //do nothing for now
        }

        private void updateBatchNo(String content) {
            //AET-63
            if (!isFirst && !FinancialApplication.getTransDataDbHelper().findAllTransData(acquirer, false).isEmpty()) {
                ToastUtils.showMessage(R.string.has_trans_for_settle);
            } else {
                acquirer.setCurrBatchNo(Integer.parseInt(content));
            }
        }
    }

    private void showErrMsgDialog(){
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
                dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                });
                dialog.setTimeout(3);
                dialog.show();
                dialog.setNormalText("No Enabled Acquirer!!");
                dialog.showCancelButton(false);
                dialog.showConfirmButton(true);
            }
        });
    }

    private void onUpdateAcquirer(Acquirer acquirer) {
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            MerchantProfileManager.INSTANCE.updateTerminalIDAndMerchantID(acquirer.getName(), acquirer.getTerminalId(), acquirer.getMerchantId());
        }
    }

}
