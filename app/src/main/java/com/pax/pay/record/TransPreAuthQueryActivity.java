package com.pax.pay.record;

import android.annotation.SuppressLint;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
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
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.NewSpinnerAdapter;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import th.co.bkkps.utils.Log;

public class TransPreAuthQueryActivity extends TransQueryActivity {

    private boolean supportDoTrans = true;

    @Override
    protected void loadParam() {
        String[] titles = new String[]{getString(R.string.history_detail), getString(R.string.history_total)};
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        supportDoTrans = getIntent().getBooleanExtra(EUIParamKeys.SUPPORT_DO_TRANS.toString(), true);

        List<Acquirer> listAcquirers = new ArrayList<>(0);
        setListAcqs(listAcquirers);

        if (acquirer == null) {
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
            adapter.setOnTextUpdateListener((list, position) -> ((Acquirer) list.get(position)).getName());

            pagerAdapter = new TransPreAuthQueryActivity.MyAdapter(getSupportFragmentManager(), titles);
        }
    }

    private void setListAcqs(List<Acquirer> listAcquirers) {
        List<String> acqs = Arrays.asList(Constants.ACQ_KBANK, Constants.ACQ_KBANK_BDMS, Constants.ACQ_UP);
        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findAcquirer(acqs);
        listAcquirers.add(new Acquirer(0, Utils.getString(R.string.acq_all_acquirer)));//EDCBBLAND-113: create dummy acquirer for show "All Acquirers" in dropdown list
        listAcquirers.addAll(acquirerList);
    }

    @Override
    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.query_action, menu);

        if (!supportDoTrans) {
            menu.removeItem(R.id.history_menu_print_trans_detail);
        }
        menu.removeItem(R.id.history_menu_print_trans_last);//hide print last trans
        menu.removeItem(R.id.history_menu_print_audit_report);//hide audit report
        menu.removeItem(R.id.history_menu_print_trans_total);//hide summary report
        menu.removeItem(R.id.history_menu_print_last_total);//hide last settle report

        return true;
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
                    return new TransPreAuthDetailFragment();
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
                    TransPreAuthDetailFragment f1 = (TransPreAuthDetailFragment) super.instantiateItem(container, position);
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(100);
                finish();
                ActivityStack.getInstance().popTo(MainActivity.class);
                return true;
            case R.id.history_menu_search:
                queryTransRecordByTransNo();
                return true;
            case R.id.history_menu_print_trans_last:
                FinancialApplication.getApp().runInBackground(() -> {
                    try{
                        Log.i("Print Last");
                        new BPSPrintLastTrans(TransPreAuthQueryActivity.this, null).execute();
                    } catch (Exception e){
                        Log.e("Print Last", e);
                        e.printStackTrace();
                    }
                });
                return true;
            case R.id.history_menu_print_trans_detail:
                FinancialApplication.getApp().runInBackground(() -> {
                    //AET-112
                    try {
                        Log.i("Print detail");
                        int result = Printer.printPreAuthDetailReport(TransPreAuthQueryActivity.this, acquirer);
                        if (result != TransResult.SUCC) {
                            if (result == TransResult.ERR_NO_TRANS) {
                                DialogUtils.showErrMessage(TransPreAuthQueryActivity.this,
                                        getString(R.string.dialog_print), getString(R.string.err_no_trans),
                                        null, Constants.FAILED_DIALOG_SHOW_TIME);
                            } else {
                                DialogUtils.showErrMessage(TransPreAuthQueryActivity.this,
                                        getString(R.string.dialog_print), getString(R.string.err_printing_process),
                                        null, Constants.FAILED_DIALOG_SHOW_TIME);
                            }
                        }
                    } catch (Exception e){
                        Log.e("Print detail", e);
                        e.printStackTrace();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    private void queryTransRecordByTransNo() {
        final ActionInputTransData inputTransDataAction = new ActionInputTransData(action -> {
            String promptMsg = getString(R.string.prompt_input_transno);
        /*if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
            promptMsg = getString(R.string.prompt_input_stanno);
        }*/
            ((ActionInputTransData) action).setParam(TransPreAuthQueryActivity.this,
                    getString(R.string.menu_history)).setInputLine(promptMsg,
                    ActionInputTransData.EInputType.NUM, 6, false);
        });

        inputTransDataAction.setEndListener((action, result) -> {

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
                    action12 -> ((ActionDispTransDetail) action12).setParam(TransPreAuthQueryActivity.this,
                            getString(R.string.menu_history), map));
            dispTransDetailAction.setEndListener(
                    (action1, result1) -> ActivityStack.getInstance().popTo(TransPreAuthQueryActivity.this)
            );

            dispTransDetailAction.execute();
        });

        inputTransDataAction.execute();
    }

    private LinkedHashMap<String, String> prepareValuesForDisp(TransData transData) {

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        ETransType transType = transData.getTransType();
        String amount;
        if (transType.isSymbolNegative()) {
            amount = CurrencyConverter.convert(-Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
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
