package com.pax.pay.trans.action.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;
import com.pax.edc.expandablerecyclerview.ExpandItemAnimator;
import com.pax.edc.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.menu.TleMenuActivity;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import th.co.bkkps.utils.Log;

public class TleBankHostActivity extends BaseActivityWithTickForAction {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_selectacq_layout2;
    }


    private ExpandableRecyclerAdapter<Map<String, String>> acquirerListAdapter;

    RecyclerView mRecyclerView = null;
    LinearLayout mSelectAllLayout = null;
    CheckBox mSelectAllCheck = null;
    TextView mSelectAllTextView = null;
    CheckBox mCheck = null;
    Button mSelect = null;

    @Override
    protected void initViews() {
        mSelectAllLayout = (LinearLayout) findViewById(R.id.select_acq_tle_item_header);
        mSelectAllCheck = (CheckBox) findViewById(R.id.item_select_acq_tle_check);
        mSelectAllTextView = (TextView) findViewById(R.id.expandable_toggle_button);

        if (tleBankList == null || tleBankList.size() < 1) {//Show error msg if no acquirer enable TLE
            mSelectAllCheck.setVisibility(View.GONE);
            mSelectAllTextView.setText(getString(R.string.err_no_support_tle));
            mSelectAllTextView.setVisibility(View.VISIBLE);
        } else if (tleBankList != null && tleBankList.size() < 2) {//Hide Select All Acq if have only 1 acq in the list
            mSelectAllLayout.setVisibility(View.GONE);
        }
        mCheck = (CheckBox) findViewById(R.id.item_select_acq_tle_check);
        mSelect = (Button) findViewById(R.id.select_acq_tle);
        mRecyclerView = (RecyclerView) findViewById(R.id.select_acq_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(acquirerListAdapter);

        confirmButtonChanging();
    }


    @Override
    protected void setListeners() {

        mSelect.setOnClickListener(this);

        mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tickTimer.start();
                //AET-39
                if (isChecked) {
                    for (String tleBankName : tleAcqWithBankNameList.keySet()) {
                        if (!userSelectedAcquirer.contains(tleBankName)) {
                            userSelectedAcquirer.add(tleBankName);
                        }
                    }
                } else {
                    if (userSelectedAcquirer.size() == tleAcqWithBankNameList.size()) {
                        userSelectedAcquirer.clear();
                    }
                }
                confirmButtonChanging();
                acquirerListAdapter.notifyDataSetChanged();
            }
        });

//        mSelect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (userSelectedAcquirer.isEmpty()) {
//                    ToastUtils.showMessage(R.string.err_settle_select_acq);
//                    return;
//                }
//
//                mCheck.setEnabled(false);
//                mSelect.setEnabled(false);
//                mRecyclerView.setEnabled(false);
//
//                finish(new ActionResult(TransResult.SUCC, userSelectedAcquirer));
//            }
//        });
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.select_acq_tle) {
            finish2SelectAcq();
        }
    }

    private void finish2SelectAcq() {
        if (userSelectedAcquirer.isEmpty()) {
            ToastUtils.showMessage(R.string.err_settle_select_acq);
            return;
        }
        finish(new ActionResult(TransResult.SUCC, userSelectedAcquirer));
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    Map<String,Acquirer> tleAcqWithBankNameList = null;
    ArrayList<Map<String, String>> tleBankList;                 // use with adapter
    private ArrayList<String> userSelectedAcquirer = new ArrayList<>();
    final String ACQ_NAME = "acq_name";

    @Override
    protected void loadParam() {

        //bankTleRawList = FinancialApplication.getAcqManager().findTleBankHostByActiveHost();
        // bankTleRawList = FinancialApplication.getAcqManager().findTleBankHostByActiveHost();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String content = bundle.getString("DISP_TITLE","Select Bank Host");
            super.setTitle(content);
        }

        tleAcqWithBankNameList = FinancialApplication.getAcqManager().findTleBankHostByActiveHost();
        //ArrayList<String> utilsTleBankList = Utils.getTLEPrimaryAcquirerList();
        //if (tleAcqWithBankNameList.size() == utilsTleBankList.size()) {
            tleBankList = new ArrayList<>();
            for (String keyName : tleAcqWithBankNameList.keySet()) {
                Map<String, String> map = new HashMap<>();
                map.put(ACQ_NAME, keyName);
                tleBankList.add(map);
            }
        //}


        acquirerListAdapter = new ExpandableRecyclerAdapter<>(this, R.layout.selectacq_item2,
            new ExpandableRecyclerAdapter.ItemViewListener<Map<String, String>>() {
                @Override
                public BaseViewHolder<Map<String, String>> generate(View view) {
                    return new TleBankHostViewHolder(view);
                }
            })
            .setDataBeanList(tleBankList);
    }

    private class TleBankHostViewHolder extends BaseViewHolder<Map<String, String>> {
        private TextView textView;
        private CheckBox checkBox;
        private TextView acqName;
        private TextView merchantName;
        private TextView merchantId;
        private TextView terminalId;
        private TextView batchNo;

        public TleBankHostViewHolder(View itemView) {
            super(itemView, R.id.select_acq_tle_item_header, R.id.expandable);
        }

        @Override
        protected void initView() {
            textView = (TextView) itemView.findViewById(R.id.expandable_toggle_button);
            checkBox = (CheckBox) itemView.findViewById(R.id.item_select_acq_tle_check);
            acqName = (TextView) itemView.findViewById(R.id.settle_acquirer_name);
            merchantName = (TextView) itemView.findViewById(R.id.settle_merchant_name);
            merchantId = (TextView) itemView.findViewById(R.id.settle_merchant_id);
            terminalId = (TextView) itemView.findViewById(R.id.settle_terminal_id);
            batchNo = (TextView) itemView.findViewById(R.id.settle_batch_num);
        }

        @Override
        protected void setListener() {

        }

        @Override
        public void bindView(Map<String, String> dataBean, BaseViewHolder viewHolder, int pos) {
            // set TLE Bank name
            textView.setText(dataBean.get(ACQ_NAME));
            // disable dropdown expand : display acquirer details
            viewHolder.getExpandView().setVisibility(View.GONE);

            String acqName = dataBean.get(ACQ_NAME);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    tickTimer.start();
                    Log.d("SelectAcq", "onCheckedChanged  " + pos);

                    if (isChecked) {
                        if (!userSelectedAcquirer.contains(acqName)) {
                            userSelectedAcquirer.add(acqName);
                        }
                    } else {
                        userSelectedAcquirer.remove(acqName);
                    }

                    confirmButtonChanging();
                    mCheck.setChecked(userSelectedAcquirer.size() == tleAcqWithBankNameList.size());
                }
            });
            if (userSelectedAcquirer != null) {
                checkBox.setChecked(userSelectedAcquirer.contains(acqName));
            }
        }
    }

    private void confirmButtonChanging() {
        boolean enableState = false ;
        if (userSelectedAcquirer!=null && userSelectedAcquirer.size()>0) {
            enableState=true;
        }
        mSelect.setEnabled(enableState);
    }
}
