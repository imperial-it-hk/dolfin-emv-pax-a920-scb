package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;
import com.pax.edc.expandablerecyclerview.ExpandItemAnimator;
import com.pax.edc.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectTleAcqActivity extends BaseActivityWithTickForAction {

    private static final String ACQ_NAME = "acq_name";

    private CheckBox mCheck;
    private Button mSelect;
    private RecyclerView mRecyclerView;

    private ExpandableRecyclerAdapter<Map<String, String>> acquirerListAdapter;

    private List<String> checkedAcqs;

    private CheckBox lastCheckedCb = null;
    private LinearLayout mSelectAllLayout;
    ArrayList<Map<String, String>> myListArray;
    List<Acquirer> acqList;

    private CheckBox mSelectAllCheck;
    private TextView mSelectAllTextView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_selectacq_layout2;
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            checkedAcqs = bundle.getStringArrayList(Constants.ACQUIRER_NAME);
        }

        if (checkedAcqs == null)
            checkedAcqs = new ArrayList<>();

        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        myListArray = new ArrayList<>();
        String content = getIntent().getStringExtra(EUIParamKeys.CONTENT.toString());
        for (Acquirer i : acquirers) {
            Map<String, String> map = new HashMap<>();
            if(content.contains("TLE")){
                if(i.isEnableTle()){
                    map.put(ACQ_NAME, i.getName());
                    myListArray.add(map);
                }
            }else if(content.contains("UPI")){
                if(i.getName().equalsIgnoreCase(Constants.ACQ_UP)){
                    map.put(ACQ_NAME, i.getName());
                    myListArray.add(map);
                }
            }else{
                map.put(ACQ_NAME, i.getName());
                myListArray.add(map);
            }
        }

        List<String> acquirerName = new ArrayList<>();
        for (Map<String, String> map : myListArray) {
            ArrayList<String> acqs = new ArrayList<>(map.values());
            acquirerName.addAll(acqs);
        }
        acqList = FinancialApplication.getAcqManager().findAcquirer(acquirerName);

        if(myListArray.size() == 1 ){//auto select if only have 1 acq in the list
            checkedAcqs.add(myListArray.get(0).get(ACQ_NAME));
        }

        acquirerListAdapter = new ExpandableRecyclerAdapter<>(SelectTleAcqActivity.this, R.layout.selectacq_item2,
                new ExpandableRecyclerAdapter.ItemViewListener<Map<String, String>>() {
                    @Override
                    public BaseViewHolder<Map<String, String>> generate(View view) {
                        return new AcqTleViewHolder(view);
                    }
                })
                .setDataBeanList(myListArray);
    }

    @Override
    protected String getTitleString() {
        return getString(R.string.tle_select_acquirer);
    }

    @Override
    protected void initViews() {
        mSelectAllLayout = (LinearLayout) findViewById(R.id.select_acq_tle_item_header);
        mSelectAllCheck = (CheckBox) findViewById(R.id.item_select_acq_tle_check);
        mSelectAllTextView = (TextView) findViewById(R.id.expandable_toggle_button);

        if(myListArray == null || myListArray.size() < 1){//Show error msg if no acquirer enable TLE
            mSelectAllCheck.setVisibility(View.GONE);
            mSelectAllTextView.setText(getString(R.string.err_no_support_tle));
            mSelectAllTextView.setVisibility(View.VISIBLE);
        }else if(myListArray != null && myListArray.size() < 2){//Hide Select All Acq if have only 1 acq in the list
            mSelectAllLayout.setVisibility(View.GONE);
        }
        mCheck = (CheckBox) findViewById(R.id.item_select_acq_tle_check);
        mSelect = (Button) findViewById(R.id.select_acq_tle);
        mRecyclerView = (RecyclerView) findViewById(R.id.select_acq_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(acquirerListAdapter);
        mRecyclerView.setItemAnimator(new ExpandItemAnimator());

        confirmBtnChange();
    }

    @Override
    protected void setListeners() {
        mSelect.setOnClickListener(this);

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                tickTimer.start();
                return false;
            }
        });

        mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tickTimer.start();
                //AET-39
                if (isChecked) {
                    for (Acquirer acquirer : acqList) {
                        if (!checkedAcqs.contains(acquirer.getName())) {
                            checkedAcqs.add(acquirer.getName());
                        }
                    }
                } else {
                    if (checkedAcqs.size() == acqList.size()) {
                        checkedAcqs.clear();
                    }
                }
                confirmBtnChange();
                acquirerListAdapter.notifyDataSetChanged();
            }
        });

        //AET-39
        if (checkedAcqs != null && checkedAcqs.size() == acqList.size()) {
            mCheck.setChecked(true);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.select_acq_tle) {
            finish2SelectAcq();
        }
    }

    private void finish2SelectAcq() {
        if (checkedAcqs.isEmpty()) {
            ToastUtils.showMessage(R.string.err_settle_select_acq);
            return;
        }
        finish(new ActionResult(TransResult.SUCC, checkedAcqs));
    }

    private class AcqTleViewHolder extends BaseViewHolder<Map<String, String>> {

        private TextView textView;
        private CheckBox checkBox;
        private TextView acqName;
        private TextView merchantName;
        private TextView merchantId;
        private TextView terminalId;
        private TextView batchNo;

        public AcqTleViewHolder(View itemView) {
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
        public void bindView(final Map<String, String> dataBean, final BaseViewHolder viewHolder, final int pos) {
            textView.setText(dataBean.get(ACQ_NAME));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    tickTimer.start();
                    Log.d("SelectAcq", "onCheckedChanged  " + pos);
                    if (isChecked) {
                        if (!checkedAcqs.contains(dataBean.get(ACQ_NAME)))


                            checkedAcqs.add(dataBean.get(ACQ_NAME));
                    } else {
                        checkedAcqs.remove(dataBean.get(ACQ_NAME));

                    }
                    confirmBtnChange();
                    //AET-39
                    mCheck.setChecked(checkedAcqs.size() == acqList.size());
                }
            });

            //AET-39
            checkBox.setChecked(checkedAcqs.contains(dataBean.get(ACQ_NAME)));

            if (viewHolder.getExpandView().getVisibility() == View.VISIBLE) {
                updateValueTable(pos);
            }
        }

        private void updateValueTable(final int position) {
            String acquirerName = findAcquirer(position);
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName);
            TransTotal total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer);

            acqName.setText(acquirer.getName());
            merchantName.setText(getString(R.string.settle_merchant_name));
            merchantId.setText(acquirer.getMerchantId());
            terminalId.setText(acquirer.getTerminalId());
            batchNo.setText(String.valueOf(acquirer.getCurrBatchNo()));
        }

        private String findAcquirer(int position) {
            return acquirerListAdapter.getDataBeanList().get(position).get(ACQ_NAME);
        }
    }
    // AET-114
    private void confirmBtnChange() {
        mSelect.setEnabled(!checkedAcqs.isEmpty());
    }
}
