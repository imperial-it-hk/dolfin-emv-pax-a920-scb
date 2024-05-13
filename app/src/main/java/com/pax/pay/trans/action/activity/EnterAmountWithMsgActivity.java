package com.pax.pay.trans.action.activity;

import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.Utils;
import com.pax.view.keyboard.CustomKeyboardEditText;

/**
 * Created by WITSUTA A on 5/14/2018.
 */

public class EnterAmountWithMsgActivity extends BaseActivityWithTickForAction {


    private TextView textHeader;
    private CustomKeyboardEditText editAmount;
    private String MsgHeader;
    private String title;
    private EnterAmountTextWatcher amountWatcher = null;


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        editAmount.requestFocus();
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        MsgHeader = getIntent().getStringExtra(Utils.getString(R.string.wallet_text_header));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_amount_with_msg;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        textHeader = (TextView) findViewById(R.id.text_header);
        if(MsgHeader != null && !MsgHeader.isEmpty()){
            textHeader.setText(MsgHeader);
        }
        editAmount = (CustomKeyboardEditText) findViewById(R.id.amount_edit);
        editAmount.setText(CurrencyConverter.convert(0L)); //AET-64
        editAmount.requestFocus();
    }

    @Override
    protected void setListeners() {
        amountWatcher = new EnterAmountTextWatcher();
        editAmount.addTextChangedListener(amountWatcher);
        if (amountWatcher != null)
            amountWatcher.setAmount(CurrencyConverter.parse(editAmount.getText().toString().trim()), 0L);
        editAmount.setOnEditorActionListener(new EnterAmountEditorActionListener());
    }


    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    private class EnterAmountEditorActionListener extends EditorActionListener {
        @Override
        public void onKeyCancel() {
            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null)); // AET-64
        }

        @Override
        public void onKeyOk() {
            finish(new ActionResult(TransResult.SUCC,
                    CurrencyConverter.parse(editAmount.getText().toString().trim()))
            );
        }


    }
}
