/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-28
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.pax.device.Device;

public class EnterAmountTextWatcher implements TextWatcher {

    private boolean mEditing;
    private long mPre = 0L;
    private String mPreStr;
    private boolean mIsForward = true;
    private OnTipListener fListener;

    private long mBaseAmount = 0L;

    private long maxValue = 999999999999L;

    private EditText editText;

    public EnterAmountTextWatcher() {
        mEditing = false;
    }

    public EnterAmountTextWatcher(long baseAmount, long initTipAmount) {
        mEditing = false;

        setAmount(baseAmount, initTipAmount);
    }

    public EnterAmountTextWatcher(EditText editText) {
        this.editText = editText;
    }

    public void setAmount(long baseAmount, long tipAmount) {
        mBaseAmount = baseAmount;
        mPre = tipAmount;
        if (fListener != null) {
            fListener.onUpdateTipListener(mBaseAmount, mPre);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (!mEditing) {
            mIsForward = (after >= count);
            mPreStr = s.toString();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!mEditing) {
            mEditing = true;
            String edit = s.toString().trim();
            long curr = mIsForward ? doForward(edit, mBaseAmount) : doBackward(edit, mBaseAmount);

            String str = CurrencyConverter.convert(curr);
            mPre = curr - mBaseAmount;
            updateEditable(s, str);
            mEditing = false;
        }
    }

    private void updateEditable(Editable s, String str) {
        try {
            s.replace(0, s.length(), str);
            if (fListener != null) {
                fListener.onUpdateTipListener(mBaseAmount, mPre);
            }
            if (editText != null) {
                editText.setText(str);
                editText.setSelection(str.length());
            }
        } catch (NumberFormatException nfe) {
            s.clear();
            mPre = 0L;
        }
    }

    private long doForward(String edit, long currAmount) {
        long curr = currAmount;
        long lastDigit = 0L;
        int time = 0;
        if (!edit.isEmpty() && !mPreStr.isEmpty()) {
            int start = edit.indexOf(mPreStr);
            if (start != -1) {
                start += mPreStr.length();
                time = edit.length() - start;
            } else {
                start = 0;
            }
            if (time > 0) {
                try {
                    lastDigit = Long.parseLong(edit.substring(start).replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    time = 0;
                }
            }
        }

        time = (int) Math.pow(10, time);

        if ((curr + mPre * time + lastDigit > maxValue) ||
                (fListener != null && currAmount >= 0 && !fListener.onVerifyTipListener(currAmount, mPre * time + lastDigit))) { //AET-21
            Device.beepErr();
            curr += mPre;
        } else {
            curr += mPre * time + lastDigit;
        }

        return curr;
    }

    private long doBackward(String edit, long currAmount) {
        long curr = currAmount;
        if (0 == edit.length()) {
            mPre = 0L;
        }
        curr += mPre / 10;
        return curr;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public void setOnTipListener(OnTipListener listener) {
        this.fListener = listener;
    }

    public interface OnTipListener {
        void onUpdateTipListener(long baseAmount, long tipAmount);

        boolean onVerifyTipListener(long baseAmount, long tipAmount);
    }
}
