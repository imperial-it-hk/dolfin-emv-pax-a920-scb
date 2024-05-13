package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.text.InputFilter;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.ToastUtils;
import com.pax.view.keyboard.CustomKeyboardEditText;

/**
 * Created by WITSUTA A on 4/20/2018.
 */

public class InputTransIDActivity extends BaseActivityWithTickForAction {

    private String prompt;
    private String navTitle;
    private ActionInputTransData.EInputType inputType;
    private int maxLen;
    private int minLen;
    private boolean isPaddingZero;

    private CustomKeyboardEditText mEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEditText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEditText.setText("");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_input_transid;
    }

    @Override
    protected void loadParam() {
        prompt = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        inputType = (ActionInputTransData.EInputType) getIntent().getSerializableExtra(EUIParamKeys.INPUT_TYPE.toString());
        maxLen = getIntent().getIntExtra(EUIParamKeys.INPUT_MAX_LEN.toString(), 12);
        minLen = getIntent().getIntExtra(EUIParamKeys.INPUT_MIN_LEN.toString(), 0);
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        isPaddingZero = getIntent().getBooleanExtra(EUIParamKeys.INPUT_PADDING_ZERO.toString(), true);
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        TextView promptText = (TextView) findViewById(R.id.prompt_tran_id);
        promptText.setText(prompt);

    }

    private void setEditText() {
        if (ActionInputTransData.EInputType.TRANSID == inputType) {
            setEditTextNum();
        }
        if (mEditText != null) {
            mEditText.setOnEditorActionListener(new EditorActionListener() {
                @Override
                protected void onKeyOk() {
                    quickClickProtection.stop();
                    String content = process();
                    if (!onConfirmResult(content)) {
                        return;
                    }
                    finish(new ActionResult(TransResult.SUCC, content));
                }

                @Override
                protected void onKeyCancel() {
                    finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                }
            });
        }
    }

    // 数字
    private void setEditTextNum() {
        mEditText = (CustomKeyboardEditText) findViewById(R.id.input_trans_id);
        mEditText.requestFocus();
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
    }

    @Override
    protected void setListeners() {

    }



    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    private boolean onConfirmResult(String content) {
        if (ActionInputTransData.EInputType.TRANSID == inputType && minLen > 0 && (content == null || content.isEmpty())) {
            ToastUtils.showMessage(R.string.please_input_again);
            return false;
        }
        return true;
    }

    /**
     * 输入数值检查
     */
    private String process() {
        String content = mEditText.getText().toString().trim();

        if (content.isEmpty()) {
            return null;
        }

        if (ActionInputTransData.EInputType.TRANSID == inputType) {
            if (content.length() >= minLen && content.length() <= maxLen) {
                if (isPaddingZero) {
                    content = Component.getPaddedString(content, maxLen, '0');
                }
            } else {
                return null;
            }
        }
        return content;
    }
}
