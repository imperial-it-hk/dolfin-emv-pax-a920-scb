package com.pax.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.Utils;

public class EditTextAmountPreference extends EditTextPreferenceFix {
    private EnterAmountTextWatcher amountTextWatcher;

    public EditTextAmountPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextAmountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextAmountPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final EditText editText = getEditText();
//        editText.setText(CurrencyConverter.convert(Utils.parseLongSafe(getText(), 0)));

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                editText.setCursorVisible(true);
                return false;
            }
        });

        editText.removeTextChangedListener(amountTextWatcher);
        amountTextWatcher = new EnterAmountTextWatcher(editText);
        amountTextWatcher.setMaxValue(999999999999L);
        amountTextWatcher.setOnTipListener(new EnterAmountTextWatcher.OnTipListener() {
            @Override
            public void onUpdateTipListener(long baseAmount, long tipAmount) {
                setText(String.valueOf(tipAmount));
            }

            @Override
            public boolean onVerifyTipListener(long baseAmount, long tipAmount) {
                return true;
            }
        });
        editText.addTextChangedListener(amountTextWatcher);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
//        final EditText editText = getEditText();
//        editText.setText(CurrencyConverter.convert(Utils.parseLongSafe(getText(), 0)));
    }


}
