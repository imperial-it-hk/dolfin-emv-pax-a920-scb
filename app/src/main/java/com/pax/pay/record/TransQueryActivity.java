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

import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.BPSPrintLastTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionInputTransData.EInputType;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.NewSpinnerAdapter;
import com.pax.view.PagerSlidingTabStrip;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import th.co.bkkps.kcheckidAPI.trans.action.ActionDisplayKCheckIDDetailReport;
import th.co.bkkps.kcheckidAPI.trans.action.ActionDisplayKCheckIDSummaryReport;
import th.co.bkkps.utils.Log;

public class TransQueryActivity extends BaseActivityWithTickForAction {

    protected Acquirer acquirer = null;
    protected NewSpinnerAdapter<Acquirer> adapter;
    protected PagerAdapter pagerAdapter;

    protected String navTitle;
    private boolean supportDoTrans = true;
    private boolean isEcrProcess = false;
    private String nii = null;
    private List<Acquirer> listAcquirers;
    private boolean isContinue = true;

    private boolean isKCheckID = false;
    private String targetAcquirerName = "";

    @Override
    protected void onResume() {
        super.onResume();
//        tickTimer.stop(); enable timeout
    }

    @Override
    protected void loadParam() {
        String[] titles = new String[]{getString(R.string.history_detail), getString(R.string.history_total)};
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        isEcrProcess = getIntent().getBooleanExtra(EUIParamKeys.ECR_PROCESS.toString(), false);
        nii = isEcrProcess ? getIntent().getStringExtra(EUIParamKeys.ECR_NII.toString()) : nii;
        supportDoTrans = !isEcrProcess && getIntent().getBooleanExtra(EUIParamKeys.SUPPORT_DO_TRANS.toString(), true);

        targetAcquirerName = getIntent().getStringExtra(EUIParamKeys.ACQUIRER_NAME.toString());
        if (targetAcquirerName != null)
            nii = FinancialApplication.getAcqManager().findAcquirer(targetAcquirerName).getNii();

        listAcquirers = new ArrayList<>(0);
        setListAcqs(listAcquirers);

        if (acquirer == null && !isEcrProcess) {
            for (Acquirer acq : listAcquirers) {
                if (acq.getName().equals(Utils.getString(R.string.acq_all_acquirer))) {//EDCBBLAND-113: set "All Acquirers" as default selected acquirer //FinancialApplication.getAcqManager().getCurAcq().getName()
                    acquirer = acq;
                    break;
                }
            }
        }

        if (acquirer != null) {
            adapter = new NewSpinnerAdapter<>(this);
            adapter.setListInfo(listAcquirers);
            adapter.setOnTextUpdateListener(new NewSpinnerAdapter.OnTextUpdateListener() {
                @Override
                public String onTextUpdate(final List<?> list, int position) {
                    return ((Acquirer) list.get(position)).getName();
                }
            });

            pagerAdapter = new MyAdapter(getSupportFragmentManager(), titles);
        }
    }

    private void setListAcqs(List<Acquirer> listAcquirers) {
        List<Acquirer> acquirerList = AcqManager.findEnableAcquirers();//EDCBBLAND-509: Show only enabled acquirer in dropdown list
        if (nii != null) {
            for (Acquirer acq : acquirerList) {
                if (nii.contains(acq.getNii()) && !acq.getName().equals(Constants.ACQ_DOLFIN)) {
                    Log.d("Acquirer:","RequestNII[" + nii + "]:Match--->[Name=" + acq.getName()+", NII="+ acq.getNii()+"]");
                    listAcquirers.add(acq);
                    acquirer = acq;
                }
            }
        } else {
            for (Acquirer acq : acquirerList) {
                if (acq.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                    acquirerList.remove(acq);
                    break;
                }
            }
            listAcquirers.add(new Acquirer(0, Utils.getString(R.string.acq_all_acquirer)));//EDCBBLAND-113: create dummy acquirer for show "All Acquirers" in dropdown list
            listAcquirers.addAll(acquirerList);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_trans_query_layout;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }


    protected ViewPager pager = null;
    protected PagerSlidingTabStrip tabs = null;

    @Override
    protected void initViews() {
        pager = (ViewPager) findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);

        Spinner spinner = (Spinner) findViewById(R.id.trans_history_acq_list);

        if (adapter.getCount() > 1) {
            spinner.setVisibility(View.VISIBLE);
            spinner.setAdapter(adapter);

            spinner.setSelection(adapter.getListInfo().indexOf(acquirer));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int pos, long id) {
                    Acquirer newAcquirer = adapter.getListInfo().get(pos);
                    if (newAcquirer.getId() != acquirer.getId()) {
                        acquirer = newAcquirer;
                        pagerAdapter.notifyDataSetChanged();

                        handleUserSelectionOn3rdApp(acquirer, tabs.getCurrentPosition());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Another interface callback
                }
            });
        }
        pager.setAdapter(pagerAdapter);
        tabs.setViewPager(pager);

        if (isEcrProcess) {
            FinancialApplication.getApp().runInBackground(new Runnable() {
                @Override
                public void run() {
                    Iterator<Acquirer> iterator = listAcquirers.iterator();
                    isContinue = true;
                    while (iterator.hasNext()) {
                        Acquirer acq = iterator.next();
                        int result = Printer.printAuditReport(TransQueryActivity.this, acq);
                        if (result != TransResult.SUCC) {
                            isContinue = false;
                            DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    isContinue = true;
                                }
                            };
                            DialogUtils.showErrMessage(TransQueryActivity.this,
                                    getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                    onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                        }
                        while(!isContinue) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!iterator.hasNext()) finish(new ActionResult(TransResult.SUCC, null));
                    }
                }
            });
        }
    }

    private void handleUserSelectionOn3rdApp(Acquirer acquirer, int currPos) {
        isKCheckID = acquirer.getName().equals(Constants.ACQ_KCHECKID);
        if (isKCheckID) {
            if (currPos == 0) {
                openDialogForDetailReport();
            } else {
                openDialogForSummaryReport();
            }
        }
    }

    @Override
    public void finish(ActionResult result) {
        if (isEcrProcess && FinancialApplication.getEcrProcess() != null) {
            if (result.getRet() == TransResult.SUCC) {
                long saleTotalNum = 0, saleTotalAmt = 0;
                for (Acquirer acq : listAcquirers) {
                    TransTotal total = FinancialApplication.getTransTotalDbHelper().calcTotal(acq,false);
                    saleTotalNum += total.getSaleTotalNum();
                    saleTotalAmt += total.getSaleTotalAmt();
                }
                EcrData.instance.nBatchTotalSalesCount = saleTotalNum;
                EcrData.instance.nBatchTotalSalesAmount = saleTotalAmt;
            }
            if (acquirer != null) EcrData.instance.setEcrData(acquirer, nii, result);
        }

        super.finish(result, true);
    }

    @Override
    protected void setListeners() {
        //do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.query_action, menu);

        if (!supportDoTrans) {
            menu.removeItem(R.id.history_menu_print_trans_last);
            menu.removeItem(R.id.history_menu_print_trans_detail);
            menu.removeItem(R.id.history_menu_print_trans_total);
            menu.removeItem(R.id.history_menu_print_last_total);
            menu.removeItem(R.id.history_menu_print_audit_report);
        }
        menu.removeItem(R.id.history_menu_print_audit_report);//hide audit report

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onTimerFinish() {
        ActivityStack.getInstance().popTo(MainActivity.class);
    }

    private class MyAdapter extends FragmentPagerAdapter {
        private String[] titles;

        MyAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            this.titles = titles;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new TransDetailFragment();
                case 1:
                    return new TransTotalFragment();
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            switch (position) {
                case 0:
                    TransDetailFragment f1 = (TransDetailFragment) super.instantiateItem(container, position);
                    f1.setAcquirerName(acquirer.getName());
                    f1.setSupportDoTrans(supportDoTrans);
                    return f1;
                case 1:
                    TransTotalFragment f2 = (TransTotalFragment) super.instantiateItem(container, position);
                    f2.setAcquirerName(acquirer.getName());
                    return f2;
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    @Override
    public void onClickProtected(View v) {
        // do nothing
    }

    @Override
    protected boolean onKeyBackDown() {
        finish();
        ActivityStack.getInstance().popTo(MainActivity.class);
        return true;
    }

    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(100);
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                ActivityStack.getInstance().popTo(MainActivity.class);
                return true;
            case R.id.history_menu_search:
                if (isKCheckID) {
                    DialogUtils.showErrMessage(TransQueryActivity.this,
                            getString(R.string.dialog_print), getString(R.string.err_feature_not_allow) + " " + acquirer.getName(),
                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                    return false;
                } else {
                    queryTransRecordByTransNo();
                    return true;
                }
            case R.id.history_menu_print_trans_last:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if (isKCheckID) {
                                DialogUtils.showErrMessage(TransQueryActivity.this,
                                        getString(R.string.dialog_print), getString(R.string.err_feature_not_allow) + " " + acquirer.getName(),
                                        null, Constants.FAILED_DIALOG_SHOW_TIME);
                            } else {
                                Log.i("Print Last");
                        	new BPSPrintLastTrans(TransQueryActivity.this, null).execute();
                            }
                        } catch (Exception e){
                            Log.e("Print Last", e);
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            case R.id.history_menu_print_trans_detail:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        //AET-112
                        try {
                            int result = 0;
                            if (isKCheckID) {
                                openDialogForDetailReport();
                            } else {
                                Log.i("Print detail");
                                result = Printer.printDetailReport(TransQueryActivity.this, acquirer);
                            }

                            if (result != TransResult.SUCC) {
                                if (result == TransResult.ERR_NO_TRANS) {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                } else {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_printing_process),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                }
                            }
                        } catch (Exception e){
                            Log.e("Print detail", e);
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            case R.id.history_menu_print_trans_total:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int result = 0;
                            if (isKCheckID) {
                                openDialogForSummaryReport();
                            }
                            else {
                                Log.i("Print Summary");
                                result = Printer.printSummaryReport(TransQueryActivity.this, acquirer);
                            }

                            if (result != TransResult.SUCC) {
                                if (result == TransResult.ERR_NO_TRANS) {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                } else {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_printing_process),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                }
                            }
                        } catch (Exception e){
                            Log.e("Print Summary", e);
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            case R.id.history_menu_print_last_total:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isKCheckID) {
                                DialogUtils.showErrMessage(TransQueryActivity.this,
                                        getString(R.string.dialog_print), getString(R.string.err_feature_not_allow) + " " + acquirer.getName(),
                                        null, Constants.FAILED_DIALOG_SHOW_TIME);
                            } else {
                                Log.i("Print Last Batch");
                                int result = Printer.printLastBatch(TransQueryActivity.this, acquirer, TransContext.getInstance().getCurrentAction());
                                if (result != TransResult.SUCC) {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                }
                            }
                        } catch (Exception e){
                            Log.e("Print Last Batch", e);
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            case R.id.history_menu_print_audit_report:
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        //AET-112
                        try{
                            if (isKCheckID) {
                                DialogUtils.showErrMessage(TransQueryActivity.this,
                                        getString(R.string.dialog_print), getString(R.string.err_feature_not_allow) + " " + acquirer.getName(),
                                        null, Constants.FAILED_DIALOG_SHOW_TIME);
                            } else {
                                Log.i("Print Audit");
                                int result = Printer.printAuditReport(TransQueryActivity.this, acquirer);
                                if (result != TransResult.SUCC) {
                                    DialogUtils.showErrMessage(TransQueryActivity.this,
                                            getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                                }
                            }
                        } catch (Exception e){
                            Log.e("Print Audit", e);
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    private void openDialogForDetailReport() {
        final ActionDisplayKCheckIDDetailReport dispDetailReportAction = new ActionDisplayKCheckIDDetailReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionDisplayKCheckIDDetailReport) action).setParam(TransQueryActivity.this);
            }
        });
        dispDetailReportAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                action.setFinished(true);
                setResult(TransResult.SUCC);
                finish();
            }
        });
        dispDetailReportAction.execute();
    }

    private void openDialogForSummaryReport() {
        final ActionDisplayKCheckIDSummaryReport dispSummaryReportAction = new ActionDisplayKCheckIDSummaryReport(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionDisplayKCheckIDSummaryReport) action).setParam(TransQueryActivity.this);
            }
        });
        dispSummaryReportAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                action.setFinished(true);
                setResult(TransResult.SUCC);
                finish();
            }
        });
        dispSummaryReportAction.execute();
        finish();
    }

    private void queryTransRecordByTransNo() {
        final ActionInputTransData inputTransDataAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                String promptMsg = getString(R.string.prompt_input_transno);
            /*if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                promptMsg = getString(R.string.prompt_input_stanno);
            }*/
                ((ActionInputTransData) action).setParam(TransQueryActivity.this,
                        getString(R.string.menu_history)).setInputLine(promptMsg,
                        EInputType.NUM, 6, false);
            }

        });

        inputTransDataAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {

                if (result.getRet() != TransResult.SUCC) {
                    if (result.getRet() == TransResult.ERR_TIMEOUT) {
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    } else {
                        ActivityStack.getInstance().pop();
                    }
                    return;
                }

                String content = (String) result.getData();
                if (content == null || content.isEmpty()) {
                    ToastUtils.showMessage(R.string.please_input_again);
                    inputTransDataAction.setFinished(false);
                    return;
                }
                long transNo = Utils.parseLongSafe(content, -1);
                TransData transData;
//                if(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
//                    transData = FinancialApplication.getTransDataDbHelper().findTransDataByStanNo(transNo, false);
//                } else{
                transData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(transNo, false);
//                }

                if (transData == null) {
                    ToastUtils.showMessage(R.string.err_no_orig_trans);
                    inputTransDataAction.setFinished(false);
                    return;
                }

                final LinkedHashMap<String, String> map = prepareValuesForDisp(transData);

                ActionDispTransDetail dispTransDetailAction = new ActionDispTransDetail(
                        new AAction.ActionStartListener() {

                            @Override
                            public void onStart(AAction action) {

                                ((ActionDispTransDetail) action).setParam(TransQueryActivity.this,
                                        getString(R.string.menu_history), map);

                            }
                        });
                dispTransDetailAction.setEndListener(new AAction.ActionEndListener() {

                    @Override
                    public void onEnd(AAction action, ActionResult result) {
                        ActivityStack.getInstance().popTo(TransQueryActivity.this);
                    }
                });

                dispTransDetailAction.execute();
            }
        });

        inputTransDataAction.execute();
    }

    private LinkedHashMap<String, String> prepareValuesForDisp(TransData transData) {

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        ETransType transType = transData.getTransType();
        String amount;
        if (transType.isSymbolNegative()) {
            amount = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            amount = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }

        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        long stanNo = transData.getTransState() == TransData.ETransStatus.VOIDED ? transData.getVoidStanNo() : transData.getStanNo();

        map.put(getString(R.string.history_detail_type), transType.getTransName());
        map.put(getString(R.string.history_detail_amount), amount);
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), transData.getRefNo());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(stanNo, 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(transData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }
}
